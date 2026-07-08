package net.caramel.safesale;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class RegionPreview {
	private static final DustParticleEffect POS1_PARTICLE = new DustParticleEffect(new Vector3f(1.0F, 0.1F, 0.1F), 1.2F);
	private static final DustParticleEffect POS2_PARTICLE = new DustParticleEffect(new Vector3f(0.1F, 0.3F, 1.0F), 1.2F);
	private static final DustParticleEffect OUTLINE_PARTICLE = new DustParticleEffect(new Vector3f(0.2F, 1.0F, 0.2F), 1.0F);
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

	public static boolean showSelectionUntilConfirmed(ServerPlayerEntity player, RegionSelections.Selection selection) {
		ActivePreview preview = ActivePreview.selection(player.getUuid(), selection);
		if (!preview.canShowTo(player)) {
			return false;
		}

		ACTIVE_PREVIEWS.put(player.getUuid(), preview);
		draw(player, preview);
		return true;
	}

	public static boolean showSelectionForTenSeconds(ServerPlayerEntity player, RegionSelections.Selection selection) {
		ActivePreview preview = ActivePreview.selection(player.getUuid(), selection).withExpiry(serverTicks + NAMED_PREVIEW_TICKS);
		if (!preview.canShowTo(player)) {
			return false;
		}

		ACTIVE_PREVIEWS.put(player.getUuid(), preview);
		draw(player, preview);
		return true;
	}

	public static boolean showRegionForTenSeconds(ServerPlayerEntity player, RegionRecord region) {
		ActivePreview preview = ActivePreview.region(player.getUuid(), region, serverTicks + NAMED_PREVIEW_TICKS);
		if (!preview.canShowTo(player)) {
			return false;
		}

		ACTIVE_PREVIEWS.put(player.getUuid(), preview);
		draw(player, preview);
		return true;
	}

	public static void clear(ServerPlayerEntity player) {
		ACTIVE_PREVIEWS.remove(player.getUuid());
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

			ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
			if (player == null || !preview.canShowTo(player)) {
				iterator.remove();
				continue;
			}

			draw(player, preview);
		}
	}

	private static void draw(ServerPlayerEntity player, ActivePreview preview) {
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

	private static void showMarker(ServerPlayerEntity player, BlockPos pos, DustParticleEffect particle) {
		ServerWorld world = player.getServerWorld();
		for (int y = 0; y <= 1; y++) {
			spawnParticle(world, player, particle, pos.getX() + 0.5D, pos.getY() + y, pos.getZ() + 0.5D);
		}
	}

	private static void showCuboid(ServerPlayerEntity player, RegionRecord region) {
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

	private static void drawLine(ServerPlayerEntity player, int startX, int startY, int startZ, int endX, int endY, int endZ, int stepBlocks) {
		int distance = Math.max(Math.max(Math.abs(endX - startX), Math.abs(endY - startY)), Math.abs(endZ - startZ));
		int steps = Math.max(1, distance / stepBlocks);
		for (int i = 0; i <= steps; i++) {
			double progress = (double) i / steps;
			double x = startX + (endX - startX) * progress + 0.5D;
			double y = startY + (endY - startY) * progress + 0.5D;
			double z = startZ + (endZ - startZ) * progress + 0.5D;
			spawnParticle(player.getServerWorld(), player, OUTLINE_PARTICLE, x, y, z);
		}
	}

	private static void spawnParticle(ServerWorld world, ServerPlayerEntity player, DustParticleEffect particle, double x, double y, double z) {
		world.spawnParticles(player, particle, true, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
	}

	private record ActivePreview(UUID ownerUuid, net.minecraft.registry.RegistryKey<net.minecraft.world.World> worldKey, BlockPos pos1, BlockPos pos2, RegionRecord region, long expiresAtTick) {
		static ActivePreview selection(UUID ownerUuid, RegionSelections.Selection selection) {
			RegionRecord region = null;
			if (selection.isSameWorld()) {
				region = RegionRecord.create("preview", selection.pos1WorldKey(), selection.pos1(), selection.pos2());
			}
			net.minecraft.registry.RegistryKey<net.minecraft.world.World> worldKey = selection.pos1WorldKey() != null ? selection.pos1WorldKey() : selection.pos2WorldKey();
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

		boolean canShowTo(ServerPlayerEntity player) {
			return ownerUuid.equals(player.getUuid()) && worldKey != null && worldKey.equals(player.getWorld().getRegistryKey());
		}
	}
}
