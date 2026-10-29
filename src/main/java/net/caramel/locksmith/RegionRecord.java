package net.caramel.locksmith;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record RegionRecord(String name, ResourceKey<Level> worldKey, BlockPos minPos, BlockPos maxPos) {
	public static RegionRecord create(String name, ResourceKey<Level> worldKey, BlockPos first, BlockPos second) {
		BlockPos min = new BlockPos(
				Math.min(first.getX(), second.getX()),
				Math.min(first.getY(), second.getY()),
				Math.min(first.getZ(), second.getZ())
		);
		BlockPos max = new BlockPos(
				Math.max(first.getX(), second.getX()),
				Math.max(first.getY(), second.getY()),
				Math.max(first.getZ(), second.getZ())
		);
		return new RegionRecord(name, worldKey, min.immutable(), max.immutable());
	}

	public boolean contains(ResourceKey<Level> worldKey, BlockPos pos) {
		return this.worldKey.equals(worldKey)
				&& pos.getX() >= minPos.getX()
				&& pos.getX() <= maxPos.getX()
				&& pos.getY() >= minPos.getY()
				&& pos.getY() <= maxPos.getY()
				&& pos.getZ() >= minPos.getZ()
				&& pos.getZ() <= maxPos.getZ();
	}
}
