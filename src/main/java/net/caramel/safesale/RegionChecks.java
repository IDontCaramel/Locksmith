package net.caramel.safesale;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class RegionChecks {
	private RegionChecks() {
	}

	public static boolean isShopPlacementAllowed(ServerWorld world, BlockPos pos) {
		RegionStorage storage = RegionStorage.get(world);
		for (RegionRecord region : storage.regions()) {
			if (region.contains(world.getRegistryKey(), pos)) {
				return true;
			}
		}

		return isInsideOverworldSpawnRadius(world, pos, storage.spawnRadius());
	}

	private static boolean isInsideOverworldSpawnRadius(ServerWorld world, BlockPos pos, int spawnRadius) {
		if (!world.getRegistryKey().equals(World.OVERWORLD) || spawnRadius <= 0) {
			return false;
		}

		BlockPos spawnPos = world.getSpawnPos();
		return Math.abs(pos.getX() - spawnPos.getX()) <= spawnRadius
				&& Math.abs(pos.getZ() - spawnPos.getZ()) <= spawnRadius;
	}
}
