package net.caramel.safesale;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.UUID;

public final class ShopItemNbt {
	private static final String ROOT = SafeSale.MOD_ID;
	private static final String OWNER = "Owner";
	private static final String SELL_ITEM = "SellItem";
	private static final String SELL_AMOUNT = "SellAmount";
	private static final String PRICE_ITEM = "PriceItem";
	private static final String PRICE_AMOUNT = "PriceAmount";

	private ShopItemNbt() {
	}

	public static void writePreparedShop(ItemStack stack, UUID ownerUuid, Item sellItem, int sellAmount, Item priceItem, int priceAmount) {
		NbtCompound root = stack.getOrCreateNbt();
		NbtCompound shop = new NbtCompound();
		shop.putUuid(OWNER, ownerUuid);
		shop.putString(SELL_ITEM, Registries.ITEM.getId(sellItem).toString());
		shop.putInt(SELL_AMOUNT, sellAmount);
		shop.putString(PRICE_ITEM, Registries.ITEM.getId(priceItem).toString());
		shop.putInt(PRICE_AMOUNT, priceAmount);
		root.put(ROOT, shop);
	}

	public static Optional<ShopRecord> readPreparedShop(ItemStack stack, RegistryKey<World> worldKey, BlockPos pos) {
		NbtCompound root = stack.getNbt();
		if (root == null || !root.contains(ROOT, NbtCompound.COMPOUND_TYPE)) {
			return Optional.empty();
		}

		NbtCompound shop = root.getCompound(ROOT);
		if (!shop.containsUuid(OWNER) || !shop.contains(SELL_ITEM) || !shop.contains(SELL_AMOUNT) || !shop.contains(PRICE_ITEM) || !shop.contains(PRICE_AMOUNT)) {
			return Optional.empty();
		}

		Item sellItem = Registries.ITEM.get(new Identifier(shop.getString(SELL_ITEM)));
		Item priceItem = Registries.ITEM.get(new Identifier(shop.getString(PRICE_ITEM)));
		int sellAmount = shop.getInt(SELL_AMOUNT);
		int priceAmount = shop.getInt(PRICE_AMOUNT);
		if (sellAmount < 1 || sellAmount > 64 || priceAmount < 1 || priceAmount > 64) {
			return Optional.empty();
		}

		return Optional.of(new ShopRecord(shop.getUuid(OWNER), worldKey, pos.toImmutable(), sellItem, sellAmount, priceItem, priceAmount));
	}
}
