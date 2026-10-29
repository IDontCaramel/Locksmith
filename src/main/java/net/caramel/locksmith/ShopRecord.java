package net.caramel.locksmith;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import java.util.UUID;

public record ShopRecord(
		UUID ownerUuid,
		ResourceKey<Level> worldKey,
		BlockPos pos,
		Item priceItem,
		int priceAmount
) {
	public boolean isOwner(UUID playerUuid) {
		return ownerUuid.equals(playerUuid);
	}
}
