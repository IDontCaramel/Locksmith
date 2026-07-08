package net.caramel.safesale;

import net.minecraft.item.Item;

import java.util.Locale;

public final class ShopNameParser {
	private ShopNameParser() {
	}

	public static String formatShopName(int priceAmount, Item priceItem) {
		return priceAmount + " " + formatItemName(priceItem);
	}

	private static String formatItemName(Item item) {
		return item.getName().getString().toLowerCase(Locale.ROOT);
	}
}
