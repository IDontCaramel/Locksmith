package net.caramel.safesale;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

final class ShopInventoryOps {
	private ShopInventoryOps() {
	}

	static boolean purchaseSlot(Inventory buyerInventory, Inventory chestInventory, ShopRecord record, int chestSlot) {
		if (!isExactSaleSlot(chestInventory, record, chestSlot)) {
			return false;
		}

		ItemStack soldStack = chestInventory.getStack(chestSlot).copy();
		ItemStack priceStack = new ItemStack(record.priceItem(), record.priceAmount());
		if (countItem(buyerInventory, record.priceItem()) < record.priceAmount()) {
			return false;
		}

		if (!canInsert(buyerInventory, soldStack.copy())) {
			return false;
		}

		removeItem(buyerInventory, record.priceItem(), record.priceAmount());
		chestInventory.setStack(chestSlot, priceStack);
		insertItem(buyerInventory, soldStack);
		chestInventory.markDirty();
		buyerInventory.markDirty();
		return true;
	}

	static boolean isExactSaleSlot(Inventory chestInventory, ShopRecord record, int chestSlot) {
		if (chestSlot < 0 || chestSlot >= chestInventory.size()) {
			return false;
		}

		ItemStack stack = chestInventory.getStack(chestSlot);
		return stack.isOf(record.sellItem()) && stack.getCount() == record.sellAmount();
	}

	private static int countItem(Inventory inventory, Item item) {
		int count = 0;
		for (int slot = 0; slot < inventory.size(); slot++) {
			ItemStack stack = inventory.getStack(slot);
			if (stack.isOf(item)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private static boolean canInsert(Inventory inventory, ItemStack stack) {
		int remaining = stack.getCount();
		for (int slot = 0; slot < inventory.size(); slot++) {
			ItemStack existing = inventory.getStack(slot);
			if (existing.isEmpty()) {
				remaining -= Math.min(stack.getMaxCount(), remaining);
			} else if (ItemStack.canCombine(existing, stack)) {
				remaining -= Math.min(existing.getMaxCount() - existing.getCount(), remaining);
			}

			if (remaining <= 0) {
				return true;
			}
		}
		return false;
	}

	private static void removeItem(Inventory inventory, Item item, int amount) {
		int remaining = amount;
		for (int slot = 0; slot < inventory.size() && remaining > 0; slot++) {
			ItemStack stack = inventory.getStack(slot);
			if (!stack.isOf(item)) {
				continue;
			}

			int removed = Math.min(stack.getCount(), remaining);
			stack.decrement(removed);
			remaining -= removed;
		}
	}

	private static void insertItem(Inventory inventory, ItemStack stack) {
		for (int slot = 0; slot < inventory.size() && !stack.isEmpty(); slot++) {
			ItemStack existing = inventory.getStack(slot);
			if (ItemStack.canCombine(existing, stack)) {
				int inserted = Math.min(existing.getMaxCount() - existing.getCount(), stack.getCount());
				existing.increment(inserted);
				stack.decrement(inserted);
			}
		}

		for (int slot = 0; slot < inventory.size() && !stack.isEmpty(); slot++) {
			if (inventory.getStack(slot).isEmpty()) {
				ItemStack inserted = stack.copy();
				inserted.setCount(Math.min(inserted.getMaxCount(), stack.getCount()));
				inventory.setStack(slot, inserted);
				stack.decrement(inserted.getCount());
			}
		}
	}
}
