package net.caramel.safesale;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public record RegionRecord(String name, RegistryKey<World> worldKey, BlockPos minPos, BlockPos maxPos) {
	public static RegionRecord create(String name, RegistryKey<World> worldKey, BlockPos first, BlockPos second) {
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
		return new RegionRecord(name, worldKey, min.toImmutable(), max.toImmutable());
	}

	public boolean contains(RegistryKey<World> worldKey, BlockPos pos) {
		return this.worldKey.equals(worldKey)
				&& pos.getX() >= minPos.getX()
				&& pos.getX() <= maxPos.getX()
				&& pos.getY() >= minPos.getY()
				&& pos.getY() <= maxPos.getY()
				&& pos.getZ() >= minPos.getZ()
				&& pos.getZ() <= maxPos.getZ();
	}
}
