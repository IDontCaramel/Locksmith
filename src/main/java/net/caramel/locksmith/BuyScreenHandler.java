package net.caramel.locksmith;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

public final class BuyScreenHandler extends ChestMenu {
	private static final int CHEST_ROWS = 3;
	private static final int CHEST_SLOTS = 27;

	private final ServerPlayer player;
	private final ShopRecord record;
	private final SimpleContainer displayInventory;

	public BuyScreenHandler(int syncId, ServerPlayer player, ShopRecord record) {
		this(syncId, player, record, new SimpleContainer(CHEST_SLOTS));
	}

	private BuyScreenHandler(int syncId, ServerPlayer player, ShopRecord record, SimpleContainer displayInventory) {
		super(MenuType.GENERIC_9x3, syncId, player.getInventory(), displayInventory, CHEST_ROWS);
		this.player = player;
		this.record = record;
		this.displayInventory = displayInventory;
		refreshDisplay();
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slot) {
		return ItemStack.EMPTY;
	}

	@Override
	public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
		if (isShopDisplaySlot(slotIndex) && player instanceof ServerPlayer) {
			ShopTransaction.purchase(this.player, record, slotIndex);
		} else {
			super.clicked(slotIndex, button, input, player);
		}
		refreshDisplay();
	}

	static boolean isShopDisplaySlot(int slotIndex) {
		return slotIndex >= 0 && slotIndex < CHEST_SLOTS;
	}

	@Override
	public boolean stillValid(Player player) {
		return player.getUUID().equals(this.player.getUUID());
	}

	private void refreshDisplay() {
		for (int slot = 0; slot < CHEST_SLOTS; slot++) {
			displayInventory.setItem(slot, ItemStack.EMPTY);
		}

		ChestBlockEntity chest = getChest();
		if (chest == null) {
			broadcastChanges();
			return;
		}

		for (int slot = 0; slot < CHEST_SLOTS && slot < chest.getContainerSize(); slot++) {
			displayInventory.setItem(slot, chest.getItem(slot).copy());
		}
		broadcastChanges();
	}

	private ChestBlockEntity getChest() {
		ServerLevel world = player.level();
		if (!world.dimension().equals(record.worldKey())) {
			return null;
		}

		BlockEntity blockEntity = world.getBlockEntity(record.pos());
		if (blockEntity instanceof ChestBlockEntity chest) {
			return chest;
		}
		return null;
	}
}
