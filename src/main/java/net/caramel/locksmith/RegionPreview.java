package net.caramel.locksmith;

import com.mojang.math.Transformation;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class RegionPreview {
	private static final float BEAM_WIDTH = 0.06F;
	private static final int NAMED_PREVIEW_TICKS = 20 * 10;
	private static final Map<UUID, RenderedPreview> ACTIVE_PREVIEWS = new HashMap<>();
	private static long serverTicks;

	private RegionPreview() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(RegionPreview::onEndServerTick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> clear(handler.player));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			ACTIVE_PREVIEWS.clear();
			serverTicks = 0L;
		});
	}

	public static boolean showSelectionUntilConfirmed(ServerPlayer player, RegionSelections.Selection selection) {
		ActivePreview preview = ActivePreview.selection(player.getUUID(), selection);
		if (!preview.canShowTo(player)) {
			return false;
		}

		show(player, preview);
		return true;
	}

	public static boolean showSelectionForTenSeconds(ServerPlayer player, RegionSelections.Selection selection) {
		ActivePreview preview = ActivePreview.selection(player.getUUID(), selection).withExpiry(serverTicks + NAMED_PREVIEW_TICKS);
		if (!preview.canShowTo(player)) {
			return false;
		}

		show(player, preview);
		return true;
	}

	public static boolean showRegionForTenSeconds(ServerPlayer player, RegionRecord region) {
		ActivePreview preview = ActivePreview.region(player.getUUID(), region, serverTicks + NAMED_PREVIEW_TICKS);
		if (!preview.canShowTo(player)) {
			return false;
		}

		show(player, preview);
		return true;
	}

	public static void clear(ServerPlayer player) {
		RenderedPreview rendered = ACTIVE_PREVIEWS.remove(player.getUUID());
		if (rendered != null) {
			rendered.remove();
		}
	}

	private static void onEndServerTick(MinecraftServer server) {
		serverTicks++;
		Iterator<RenderedPreview> iterator = ACTIVE_PREVIEWS.values().iterator();
		while (iterator.hasNext()) {
			RenderedPreview rendered = iterator.next();
			ServerPlayer player = server.getPlayerList().getPlayer(rendered.player().getUUID());
			if (player != rendered.player() || rendered.preview().isExpired(serverTicks)
					|| !rendered.preview().canShowTo(player)) {
				rendered.remove();
				iterator.remove();
			}
		}
	}

	private static void show(ServerPlayer player, ActivePreview preview) {
		clear(player);
		List<Integer> entityIds = new ArrayList<>();
		if (preview.region() != null) {
			showCuboid(player, preview.region(), entityIds);
		}
		if (preview.pos1() != null) {
			showMarker(player, preview.pos1(), Blocks.CONCRETE.red(), entityIds);
		}
		if (preview.pos2() != null) {
			showMarker(player, preview.pos2(), Blocks.CONCRETE.blue(), entityIds);
		}
		ACTIVE_PREVIEWS.put(player.getUUID(), new RenderedPreview(player, preview,
				entityIds.stream().mapToInt(Integer::intValue).toArray()));
	}

	private static void showMarker(ServerPlayer player, BlockPos pos, Block block, List<Integer> entityIds) {
		showBeam(player, pos.getX() + 0.4D, pos.getY() + 0.5D, pos.getZ() + 0.4D,
				0.2F, 1.0F, 0.2F, block, entityIds);
	}

	private static void showCuboid(ServerPlayer player, RegionRecord region, List<Integer> entityIds) {
		BlockPos min = region.minPos();
		BlockPos max = region.maxPos();
		double[] xs = {min.getX(), (double) max.getX() + 1};
		double[] ys = {min.getY(), (double) max.getY() + 1};
		double[] zs = {min.getZ(), (double) max.getZ() + 1};
		float width = (float) (xs[1] - xs[0]);
		float height = (float) (ys[1] - ys[0]);
		float depth = (float) (zs[1] - zs[0]);
		double half = BEAM_WIDTH / 2.0D;
		for (double y : ys) {
			for (double z : zs) {
				showBeam(player, xs[0] - half, y - half, z - half,
						width + BEAM_WIDTH, BEAM_WIDTH, BEAM_WIDTH, Blocks.CONCRETE.lime(), entityIds);
			}
		}
		for (double x : xs) {
			for (double z : zs) {
				showBeam(player, x - half, ys[0] - half, z - half,
						BEAM_WIDTH, height + BEAM_WIDTH, BEAM_WIDTH, Blocks.CONCRETE.lime(), entityIds);
			}
			for (double y : ys) {
				showBeam(player, x - half, y - half, zs[0] - half,
						BEAM_WIDTH, BEAM_WIDTH, depth + BEAM_WIDTH, Blocks.CONCRETE.lime(), entityIds);
			}
		}
	}

	private static void showBeam(ServerPlayer player, double x, double y, double z,
			float width, float height, float depth, Block block, List<Integer> entityIds) {
		// Allocate a normal entity ID, but never add this display to the server world.
		Display.BlockDisplay display = new Display.BlockDisplay(EntityTypes.BLOCK_DISPLAY, player.level());
		display.setPos(x, y, z);
		display.setBlockState(block.defaultBlockState());
		display.setTransformation(new Transformation(null, null, new Vector3f(width, height, depth), null));
		display.setBrightnessOverride(new Brightness(15, 15));
		display.setViewRange(16.0F);
		player.connection.send(new ClientboundAddEntityPacket(display.getId(), display.getUUID(),
				x, y, z, 0, 0, EntityTypes.BLOCK_DISPLAY, 0, Vec3.ZERO, 0));
		var data = display.getEntityData().getNonDefaultValues();
		if (data != null) {
			player.connection.send(new ClientboundSetEntityDataPacket(display.getId(), data));
		}
		entityIds.add(display.getId());
	}

	private record RenderedPreview(ServerPlayer player, ActivePreview preview, int[] entityIds) {
		void remove() {
			player.connection.send(new ClientboundRemoveEntitiesPacket(entityIds));
		}
	}

	private record ActivePreview(UUID ownerUuid, ResourceKey<Level> worldKey, BlockPos pos1, BlockPos pos2, RegionRecord region, long expiresAtTick) {
		static ActivePreview selection(UUID ownerUuid, RegionSelections.Selection selection) {
			RegionRecord region = null;
			if (selection.isSameWorld()) {
				region = RegionRecord.create("preview", selection.pos1WorldKey(), selection.pos1(), selection.pos2());
			}
			ResourceKey<Level> worldKey = selection.pos1WorldKey() != null ? selection.pos1WorldKey() : selection.pos2WorldKey();
			return new ActivePreview(ownerUuid, worldKey, selection.pos1(), selection.pos2(), region, -1L);
		}

		static ActivePreview region(UUID ownerUuid, RegionRecord region, long expiresAtTick) {
			return new ActivePreview(ownerUuid, region.worldKey(), null, null, region, expiresAtTick);
		}

		ActivePreview withExpiry(long expiresAtTick) {
			return new ActivePreview(ownerUuid, worldKey, pos1, pos2, region, expiresAtTick);
		}

		boolean isExpired(long currentTick) {
			return expiresAtTick >= 0L && currentTick >= expiresAtTick;
		}

		boolean canShowTo(ServerPlayer player) {
			return ownerUuid.equals(player.getUUID()) && worldKey != null && worldKey.equals(player.level().dimension());
		}
	}
}
