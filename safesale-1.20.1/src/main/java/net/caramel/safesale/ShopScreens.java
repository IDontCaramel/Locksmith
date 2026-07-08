package net.caramel.safesale;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;

public final class ShopScreens {
	private ShopScreens() {
	}

	public static void register() {
	}

	public static void openBuyScreen(ServerPlayerEntity player, ShopRecord record) {
		player.openHandledScreen(new NamedScreenHandlerFactory() {
			@Override
			public Text getDisplayName() {
				return Text.literal(record.sellAmount() + " for " + record.priceAmount());
			}

			@Override
			public ScreenHandler createMenu(int syncId, net.minecraft.entity.player.PlayerInventory playerInventory, net.minecraft.entity.player.PlayerEntity playerEntity) {
				return new BuyScreenHandler(syncId, player, record);
			}
		});
	}
}
