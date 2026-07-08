package net.caramel.safesale;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ShopNameParser {
	private static Map<String, Item> itemsByDisplayName;

	private ShopNameParser() {
	}

	public static Optional<ParsedShopName> parse(String name) {
		String[] parts = name.split(" - ", -1);
		if (parts.length != 2) {
			return Optional.empty();
		}

		Optional<AmountAndItem> sell = parseAmountAndItem(parts[0]);
		Optional<AmountAndItem> price = parseAmountAndItem(parts[1]);
		if (sell.isEmpty() || price.isEmpty()) {
			return Optional.empty();
		}

		AmountAndItem sellValue = sell.get();
		AmountAndItem priceValue = price.get();
		return Optional.of(new ParsedShopName(sellValue.item(), sellValue.amount(), priceValue.item(), priceValue.amount()));
	}

	public static String formatShopName(int sellAmount, Item sellItem, int priceAmount, Item priceItem) {
		return sellAmount + " " + formatItemName(sellItem) + " - " + priceAmount + " " + formatItemName(priceItem);
	}

	private static Optional<AmountAndItem> parseAmountAndItem(String value) {
		int firstSpace = value.indexOf(' ');
		if (firstSpace <= 0 || firstSpace == value.length() - 1) {
			return Optional.empty();
		}

		int amount;
		try {
			amount = Integer.parseInt(value.substring(0, firstSpace));
		} catch (NumberFormatException exception) {
			return Optional.empty();
		}

		if (amount < 1 || amount > 64) {
			return Optional.empty();
		}

		Item item = itemsByDisplayName().get(value.substring(firstSpace + 1));
		if (item == null) {
			return Optional.empty();
		}

		return Optional.of(new AmountAndItem(amount, item));
	}

	private static Map<String, Item> itemsByDisplayName() {
		if (itemsByDisplayName == null) {
			Map<String, Item> items = new HashMap<>();
			for (Item item : Registries.ITEM) {
				items.putIfAbsent(formatItemName(item), item);
			}
			itemsByDisplayName = items;
		}
		return itemsByDisplayName;
	}

	private static String formatItemName(Item item) {
		return item.getName().getString().toLowerCase(Locale.ROOT);
	}

	private record AmountAndItem(int amount, Item item) {
	}

	public record ParsedShopName(Item sellItem, int sellAmount, Item priceItem, int priceAmount) {
	}
}
