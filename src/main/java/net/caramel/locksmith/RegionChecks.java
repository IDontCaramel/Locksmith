package net.caramel.locksmith;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelData;

public final class RegionChecks {
	private RegionChecks() {
	}

	public static boolean isShopPlacementAllowed(ServerLevel world, BlockPos pos) {
		RegionStorage storage = RegionStorage.get(world);
		if (!storage.isEnforced()) {
			return true;
		}
		for (RegionRecord region : storage.regions()) {
			if (region.contains(world.dimension(), pos)) {
				return true;
			}
		}

		return isInsideSpawnRadius(world, pos, storage.spawnRadius());
	}

	private static boolean isInsideSpawnRadius(ServerLevel world, BlockPos pos, int spawnRadius) {
		LevelData.RespawnData spawn = world.getRespawnData();
		if (!world.dimension().equals(spawn.dimension()) || spawnRadius <= 0) {
			return false;
		}

		BlockPos spawnPos = spawn.pos();
		return Math.abs(pos.getX() - spawnPos.getX()) <= spawnRadius
				&& Math.abs(pos.getZ() - spawnPos.getZ()) <= spawnRadius;
	}
}
