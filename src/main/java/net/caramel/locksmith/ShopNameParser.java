package net.caramel.locksmith;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;

public final class ShopNameParser {
	private ShopNameParser() {
	}

	public static String formatShopName(int priceAmount, Item priceItem) {
		return priceAmount + " " + formatItemName(priceItem);
	}

	public static Optional<ParsedPriceName> parsePriceName(String name) {
		int firstSpace = name.indexOf(' ');
		if (firstSpace <= 0 || firstSpace == name.length() - 1) {
			return Optional.empty();
		}

		int amount;
		try {
			amount = Integer.parseInt(name.substring(0, firstSpace));
		} catch (NumberFormatException exception) {
			return Optional.empty();
		}

		if (amount < 1 || amount > 64) {
			return Optional.empty();
		}

		String remainingName = name.substring(firstSpace + 1).toLowerCase(Locale.ROOT);
		return BuiltInRegistries.ITEM.stream()
				.sorted(Comparator.comparingInt(item -> -formatItemName(item).length()))
				.filter(item -> startsWithItemName(remainingName, formatItemName(item)))
				.findFirst()
				.map(item -> new ParsedPriceName(item, amount));
	}

	private static String formatItemName(Item item) {
		return item.getDefaultInstance().getItemName().getString().toLowerCase(Locale.ROOT);
	}

	private static boolean startsWithItemName(String value, String itemName) {
		return value.equals(itemName) || value.startsWith(itemName + " ");
	}

	public record ParsedPriceName(Item priceItem, int priceAmount) {
	}
}
