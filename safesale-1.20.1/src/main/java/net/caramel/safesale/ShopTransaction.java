package net.caramel.safesale;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public final class ShopTransaction {
	private ShopTransaction() {
	}

	public static boolean purchase(ServerPlayerEntity buyer, ShopRecord record, int chestSlot) {
		ServerWorld world = buyer.getServerWorld();
		if (!world.getRegistryKey().equals(record.worldKey())) {
			return false;
		}

		BlockEntity blockEntity = world.getBlockEntity(record.pos());
		if (!(blockEntity instanceof ChestBlockEntity chest)) {
			ShopStorage.get(world).removeShop(record.worldKey(), record.pos());
			return false;
		}

		return ShopInventoryOps.purchaseSlot(buyer.getInventory(), chest, record, chestSlot);
	}
}
