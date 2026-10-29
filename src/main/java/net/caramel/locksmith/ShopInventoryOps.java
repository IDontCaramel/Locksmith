package net.caramel.locksmith;

import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

final class ShopInventoryOps {
	private ShopInventoryOps() {
	}

	static boolean purchaseSlot(Container buyerInventory, Container chestInventory, ShopRecord record, int chestSlot) {
		if (chestSlot < 0 || chestSlot >= chestInventory.getContainerSize()) {
			return false;
		}

		ItemStack soldStack = chestInventory.getItem(chestSlot).copy();
		if (soldStack.isEmpty()) {
			return false;
		}

		ItemStack priceStack = new ItemStack(record.priceItem(), record.priceAmount());
		if (countItem(buyerInventory, record.priceItem()) < record.priceAmount()) {
			return false;
		}

		if (!canInsert(buyerInventory, soldStack.copy())) {
			return false;
		}

		removeItem(buyerInventory, record.priceItem(), record.priceAmount());
		chestInventory.setItem(chestSlot, priceStack);
		insertItem(buyerInventory, soldStack);
		chestInventory.setChanged();
		buyerInventory.setChanged();
		return true;
	}

	private static int countItem(Container inventory, Item item) {
		int count = 0;
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.is(item)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private static boolean canInsert(Container inventory, ItemStack stack) {
		int remaining = stack.getCount();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack existing = inventory.getItem(slot);
			if (existing.isEmpty()) {
				remaining -= Math.min(stack.getMaxStackSize(), remaining);
			} else if (ItemStack.isSameItemSameComponents(existing, stack)) {
				remaining -= Math.min(existing.getMaxStackSize() - existing.getCount(), remaining);
			}

			if (remaining <= 0) {
				return true;
			}
		}
		return false;
	}

	private static void removeItem(Container inventory, Item item, int amount) {
		int remaining = amount;
		for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!stack.is(item)) {
				continue;
			}

			int removed = Math.min(stack.getCount(), remaining);
			stack.shrink(removed);
			remaining -= removed;
		}
	}

	private static void insertItem(Container inventory, ItemStack stack) {
		for (int slot = 0; slot < inventory.getContainerSize() && !stack.isEmpty(); slot++) {
			ItemStack existing = inventory.getItem(slot);
			if (ItemStack.isSameItemSameComponents(existing, stack)) {
				int inserted = Math.min(existing.getMaxStackSize() - existing.getCount(), stack.getCount());
				existing.grow(inserted);
				stack.shrink(inserted);
			}
		}

		for (int slot = 0; slot < inventory.getContainerSize() && !stack.isEmpty(); slot++) {
			if (inventory.getItem(slot).isEmpty()) {
				ItemStack inserted = stack.copy();
				inserted.setCount(Math.min(inserted.getMaxStackSize(), stack.getCount()));
				inventory.setItem(slot, inserted);
				stack.shrink(inserted.getCount());
			}
		}
	}
}
