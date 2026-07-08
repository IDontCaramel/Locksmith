package net.caramel.safesale;

import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.UUID;

public record ShopRecord(
		UUID ownerUuid,
		RegistryKey<World> worldKey,
		BlockPos pos,
		Item priceItem,
		int priceAmount
) {
	public boolean isOwner(UUID playerUuid) {
		return ownerUuid.equals(playerUuid);
	}
}
