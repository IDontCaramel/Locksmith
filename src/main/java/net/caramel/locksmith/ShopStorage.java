package net.caramel.locksmith;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ShopStorage extends SavedData {
	private static final SavedDataType<ShopStorage> TYPE = new SavedDataType<>(
			Locksmith.id("shops"),
			ShopStorage::new,
			CompoundTag.CODEC.xmap(ShopStorage::fromNbt, ShopStorage::writeNbt),
			null
	);
	private static final String SHOPS = "Shops";
	private static final String WORLD = "World";
	private static final String POS = "Pos";
	private static final String OWNER = "Owner";
	private static final String PRICE_ITEM = "PriceItem";
	private static final String PRICE_AMOUNT = "PriceAmount";

	private final Map<Key, ShopRecord> shops = new HashMap<>();

	public static ShopStorage get(ServerLevel world) {
		return world.getServer().getDataStorage().computeIfAbsent(TYPE);
	}

	private static ShopStorage fromNbt(CompoundTag nbt) {
		ShopStorage storage = new ShopStorage();
		nbt.getListOrEmpty(SHOPS).compoundStream()
				.forEach(tag -> readRecord(tag).ifPresent(record -> storage.shops.put(Key.of(record.worldKey(), record.pos()), record)));
		return storage;
	}

	public Optional<ShopRecord> getShop(ResourceKey<Level> worldKey, BlockPos pos) {
		return Optional.ofNullable(shops.get(Key.of(worldKey, pos)));
	}

	public void putShop(ShopRecord record) {
		shops.put(Key.of(record.worldKey(), record.pos()), record);
		setDirty();
	}

	public void removeShop(ResourceKey<Level> worldKey, BlockPos pos) {
		if (shops.remove(Key.of(worldKey, pos)) != null) {
			setDirty();
		}
	}

	private CompoundTag writeNbt() {
		ListTag list = new ListTag();
		for (ShopRecord record : shops.values()) {
			list.add(writeRecord(record));
		}
		CompoundTag nbt = new CompoundTag();
		nbt.put(SHOPS, list);
		return nbt;
	}

	private static CompoundTag writeRecord(ShopRecord record) {
		CompoundTag nbt = new CompoundTag();
		nbt.store(WORLD, Level.RESOURCE_KEY_CODEC, record.worldKey());
		nbt.putLong(POS, record.pos().asLong());
		nbt.store(OWNER, UUIDUtil.CODEC, record.ownerUuid());
		nbt.store(PRICE_ITEM, BuiltInRegistries.ITEM.byNameCodec(), record.priceItem());
		nbt.putInt(PRICE_AMOUNT, record.priceAmount());
		return nbt;
	}

	private static Optional<ShopRecord> readRecord(CompoundTag nbt) {
		Optional<ResourceKey<Level>> worldKey = nbt.read(WORLD, Level.RESOURCE_KEY_CODEC);
		Optional<Long> pos = nbt.getLong(POS);
		Optional<UUID> ownerUuid = nbt.read(OWNER, UUIDUtil.CODEC);
		Optional<Item> priceItem = nbt.read(PRICE_ITEM, BuiltInRegistries.ITEM.byNameCodec());
		Optional<Integer> priceAmount = nbt.getInt(PRICE_AMOUNT).filter(amount -> amount >= 1 && amount <= 64);
		if (worldKey.isEmpty() || pos.isEmpty() || ownerUuid.isEmpty() || priceItem.isEmpty() || priceAmount.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(new ShopRecord(ownerUuid.get(), worldKey.get(), BlockPos.of(pos.get()), priceItem.get(), priceAmount.get()));
	}

	private record Key(ResourceKey<Level> worldKey, BlockPos pos) {
		static Key of(ResourceKey<Level> worldKey, BlockPos pos) {
			return new Key(worldKey, pos.immutable());
		}
	}
}
