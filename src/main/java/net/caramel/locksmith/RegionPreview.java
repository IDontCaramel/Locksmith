package net.caramel.locksmith;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class RegionPreview {
	private static final DustParticleOptions POS1_PARTICLE = new DustParticleOptions(0xFF1A1A, 1.2F);
	private static final DustParticleOptions POS2_PARTICLE = new DustParticleOptions(0x1A4DFF, 1.2F);
	private static final DustParticleOptions OUTLINE_PARTICLE = new DustParticleOptions(0x33FF33, 1.0F);
	private static final int REDRAW_INTERVAL_TICKS = 10;
	private static final int NAMED_PREVIEW_TICKS = 20 * 10;
	private static final int MAX_PARTICLES_PER_DRAW = 300;
	private static final Map<UUID, ActivePreview> ACTIVE_PREVIEWS = new HashMap<>();
	private static long serverTicks;

	private RegionPreview() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(RegionPreview::onEndServerTick);
	}

	public static boolean showSelectionUntilConfirmed(ServerPlayer player, RegionSelections.Selection selection) {
		ActivePreview preview = ActivePreview.selection(player.getUUID(), selection);
		if (!preview.canShowTo(player)) {
			return false;
		}

		ACTIVE_PREVIEWS.put(player.getUUID(), preview);
		draw(player, preview);
		return true;
	}

	public static boolean showSelectionForTenSeconds(ServerPlayer player, RegionSelections.Selection selection) {
		ActivePreview preview = ActivePreview.selection(player.getUUID(), selection).withExpiry(serverTicks + NAMED_PREVIEW_TICKS);
		if (!preview.canShowTo(player)) {
			return false;
		}

		ACTIVE_PREVIEWS.put(player.getUUID(), preview);
		draw(player, preview);
		return true;
	}

	public static boolean showRegionForTenSeconds(ServerPlayer player, RegionRecord region) {
		ActivePreview preview = ActivePreview.region(player.getUUID(), region, serverTicks + NAMED_PREVIEW_TICKS);
		if (!preview.canShowTo(player)) {
			return false;
		}

		ACTIVE_PREVIEWS.put(player.getUUID(), preview);
		draw(player, preview);
		return true;
	}

	public static void clear(ServerPlayer player) {
		ACTIVE_PREVIEWS.remove(player.getUUID());
	}

	private static void onEndServerTick(MinecraftServer server) {
		serverTicks++;
		if (serverTicks % REDRAW_INTERVAL_TICKS != 0) {
			return;
		}

		Iterator<Map.Entry<UUID, ActivePreview>> iterator = ACTIVE_PREVIEWS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, ActivePreview> entry = iterator.next();
			ActivePreview preview = entry.getValue();
			if (preview.isExpired(serverTicks)) {
				iterator.remove();
				continue;
			}

			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null || !preview.canShowTo(player)) {
				iterator.remove();
				continue;
			}

			draw(player, preview);
		}
	}

	private static void draw(ServerPlayer player, ActivePreview preview) {
		if (preview.pos1() != null) {
			showMarker(player, preview.pos1(), POS1_PARTICLE);
		}

		if (preview.pos2() != null) {
			showMarker(player, preview.pos2(), POS2_PARTICLE);
		}

		if (preview.region() != null) {
			showCuboid(player, preview.region());
		}
	}

	private static void showMarker(ServerPlayer player, BlockPos pos, DustParticleOptions particle) {
		ServerLevel world = player.level();
		for (int y = 0; y <= 1; y++) {
			spawnParticle(world, player, particle, pos.getX() + 0.5D, pos.getY() + y, pos.getZ() + 0.5D);
		}
	}

	private static void showCuboid(ServerPlayer player, RegionRecord region) {
		BlockPos min = region.minPos();
		BlockPos max = region.maxPos();
		int stepBlocks = stepBlocks(region);
		drawLine(player, min.getX(), min.getY(), min.getZ(), max.getX(), min.getY(), min.getZ(), stepBlocks);
		drawLine(player, min.getX(), min.getY(), max.getZ(), max.getX(), min.getY(), max.getZ(), stepBlocks);
		drawLine(player, min.getX(), max.getY(), min.getZ(), max.getX(), max.getY(), min.getZ(), stepBlocks);
		drawLine(player, min.getX(), max.getY(), max.getZ(), max.getX(), max.getY(), max.getZ(), stepBlocks);

		drawLine(player, min.getX(), min.getY(), min.getZ(), min.getX(), min.getY(), max.getZ(), stepBlocks);
		drawLine(player, max.getX(), min.getY(), min.getZ(), max.getX(), min.getY(), max.getZ(), stepBlocks);
		drawLine(player, min.getX(), max.getY(), min.getZ(), min.getX(), max.getY(), max.getZ(), stepBlocks);
		drawLine(player, max.getX(), max.getY(), min.getZ(), max.getX(), max.getY(), max.getZ(), stepBlocks);

		drawLine(player, min.getX(), min.getY(), min.getZ(), min.getX(), max.getY(), min.getZ(), stepBlocks);
		drawLine(player, max.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), min.getZ(), stepBlocks);
		drawLine(player, min.getX(), min.getY(), max.getZ(), min.getX(), max.getY(), max.getZ(), stepBlocks);
		drawLine(player, max.getX(), min.getY(), max.getZ(), max.getX(), max.getY(), max.getZ(), stepBlocks);
	}

	private static int stepBlocks(RegionRecord region) {
		BlockPos min = region.minPos();
		BlockPos max = region.maxPos();
		int totalEdgeLength = 4 * (Math.abs(max.getX() - min.getX()) + Math.abs(max.getY() - min.getY()) + Math.abs(max.getZ() - min.getZ()));
		return Math.max(2, (int) Math.ceil((double) totalEdgeLength / MAX_PARTICLES_PER_DRAW));
	}

	private static void drawLine(ServerPlayer player, int startX, int startY, int startZ, int endX, int endY, int endZ, int stepBlocks) {
		int distance = Math.max(Math.max(Math.abs(endX - startX), Math.abs(endY - startY)), Math.abs(endZ - startZ));
		int steps = Math.max(1, distance / stepBlocks);
		for (int i = 0; i <= steps; i++) {
			double progress = (double) i / steps;
			double x = startX + (endX - startX) * progress + 0.5D;
			double y = startY + (endY - startY) * progress + 0.5D;
			double z = startZ + (endZ - startZ) * progress + 0.5D;
			spawnParticle(player.level(), player, OUTLINE_PARTICLE, x, y, z);
		}
	}

	private static void spawnParticle(ServerLevel world, ServerPlayer player, DustParticleOptions particle, double x, double y, double z) {
		world.sendParticles(player, particle, true, false, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
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
