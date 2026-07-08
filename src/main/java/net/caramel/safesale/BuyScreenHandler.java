package net.caramel.safesale;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public final class BuyScreenHandler extends GenericContainerScreenHandler {
	private static final int CHEST_ROWS = 3;
	private static final int CHEST_SLOTS = 27;

	private final ServerPlayerEntity player;
	private final ShopRecord record;
	private final SimpleInventory displayInventory;

	public BuyScreenHandler(int syncId, ServerPlayerEntity player, ShopRecord record) {
		this(syncId, player, record, new SimpleInventory(CHEST_SLOTS));
	}

	private BuyScreenHandler(int syncId, ServerPlayerEntity player, ShopRecord record, SimpleInventory displayInventory) {
		super(ScreenHandlerType.GENERIC_9X3, syncId, player.getInventory(), displayInventory, CHEST_ROWS);
		this.player = player;
		this.record = record;
		this.displayInventory = displayInventory;
		refreshDisplay();
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int slot) {
		return ItemStack.EMPTY;
	}

	@Override
	public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
		if (isShopDisplaySlot(slotIndex) && player instanceof ServerPlayerEntity) {
			ShopTransaction.purchase(this.player, record, slotIndex);
		} else {
			super.onSlotClick(slotIndex, button, actionType, player);
		}
		refreshDisplay();
	}

	static boolean isShopDisplaySlot(int slotIndex) {
		return slotIndex >= 0 && slotIndex < CHEST_SLOTS;
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return player.getUuid().equals(this.player.getUuid());
	}

	private void refreshDisplay() {
		for (int slot = 0; slot < CHEST_SLOTS; slot++) {
			displayInventory.setStack(slot, ItemStack.EMPTY);
		}

		ChestBlockEntity chest = getChest();
		if (chest == null) {
			sendContentUpdates();
			return;
		}

		for (int slot = 0; slot < CHEST_SLOTS && slot < chest.size(); slot++) {
			displayInventory.setStack(slot, chest.getStack(slot).copy());
		}
		sendContentUpdates();
	}

	private ChestBlockEntity getChest() {
		ServerWorld world = player.getServerWorld();
		if (!world.getRegistryKey().equals(record.worldKey())) {
			return null;
		}

		BlockEntity blockEntity = world.getBlockEntity(record.pos());
		if (blockEntity instanceof ChestBlockEntity chest) {
			return chest;
		}
		return null;
	}
}
