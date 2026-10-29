package net.caramel.locksmith;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

public final class ShopTransaction {
	private ShopTransaction() {
	}

	public static boolean purchase(ServerPlayer buyer, ShopRecord record, int chestSlot) {
		ServerLevel world = buyer.level();
		if (!world.dimension().equals(record.worldKey())) {
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
