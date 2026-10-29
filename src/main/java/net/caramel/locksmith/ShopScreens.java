package net.caramel.locksmith;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

public final class ShopScreens {
	private ShopScreens() {
	}

	public static void register() {
	}

	public static void openBuyScreen(ServerPlayer player, ShopRecord record) {
		player.openMenu(new SimpleMenuProvider(
				(syncId, playerInventory, playerEntity) -> new BuyScreenHandler(syncId, player, record),
				Component.literal("Price: " + ShopNameParser.formatShopName(record.priceAmount(), record.priceItem()))
		));
	}
}
