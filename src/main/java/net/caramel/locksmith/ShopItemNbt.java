package net.caramel.locksmith;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

public final class ShopItemNbt {
	private static final String ROOT = Locksmith.MOD_ID;
	private static final String OWNER = "Owner";
	private static final String PRICE_ITEM = "PriceItem";
	private static final String PRICE_AMOUNT = "PriceAmount";

	private ShopItemNbt() {
	}

	public static void writePreparedShop(ItemStack stack, UUID ownerUuid, Item priceItem, int priceAmount) {
		CompoundTag shop = new CompoundTag();
		shop.store(OWNER, UUIDUtil.CODEC, ownerUuid);
		shop.store(PRICE_ITEM, BuiltInRegistries.ITEM.byNameCodec(), priceItem);
		shop.putInt(PRICE_AMOUNT, priceAmount);
		CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> root.put(ROOT, shop));
	}

	public static Optional<ShopRecord> readPreparedShop(ItemStack stack, ResourceKey<Level> worldKey, BlockPos pos) {
		Optional<CompoundTag> shop = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(ROOT);
		if (shop.isEmpty()) {
			return Optional.empty();
		}

		Optional<UUID> ownerUuid = shop.get().read(OWNER, UUIDUtil.CODEC);
		Optional<Item> priceItem = shop.get().read(PRICE_ITEM, BuiltInRegistries.ITEM.byNameCodec());
		Optional<Integer> priceAmount = shop.get().getInt(PRICE_AMOUNT).filter(amount -> amount >= 1 && amount <= 64);
		if (ownerUuid.isEmpty() || priceItem.isEmpty() || priceAmount.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(new ShopRecord(ownerUuid.get(), worldKey, pos.immutable(), priceItem.get(), priceAmount.get()));
	}
}
