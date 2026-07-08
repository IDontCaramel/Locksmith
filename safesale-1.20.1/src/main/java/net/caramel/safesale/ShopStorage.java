package net.caramel.safesale;

import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ShopStorage extends PersistentState {
	private static final String STATE_ID = SafeSale.MOD_ID + "_shops";
	private static final String SHOPS = "Shops";
	private static final String WORLD = "World";
	private static final String POS = "Pos";
	private static final String OWNER = "Owner";
	private static final String SELL_ITEM = "SellItem";
	private static final String SELL_AMOUNT = "SellAmount";
	private static final String PRICE_ITEM = "PriceItem";
	private static final String PRICE_AMOUNT = "PriceAmount";

	private final Map<Key, ShopRecord> shops = new HashMap<>();

	public static ShopStorage get(ServerWorld world) {
		return world.getServer().getOverworld().getPersistentStateManager().getOrCreate(ShopStorage::fromNbt, ShopStorage::new, STATE_ID);
	}

	public static ShopStorage fromNbt(NbtCompound nbt) {
		ShopStorage storage = new ShopStorage();
		NbtList list = nbt.getList(SHOPS, NbtCompound.COMPOUND_TYPE);
		for (int i = 0; i < list.size(); i++) {
			readRecord(list.getCompound(i)).ifPresent(record -> storage.shops.put(Key.of(record.worldKey(), record.pos()), record));
		}
		return storage;
	}

	public Optional<ShopRecord> getShop(RegistryKey<World> worldKey, BlockPos pos) {
		return Optional.ofNullable(shops.get(Key.of(worldKey, pos)));
	}

	public void putShop(ShopRecord record) {
		shops.put(Key.of(record.worldKey(), record.pos()), record);
		markDirty();
	}

	public void removeShop(RegistryKey<World> worldKey, BlockPos pos) {
		if (shops.remove(Key.of(worldKey, pos)) != null) {
			markDirty();
		}
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt) {
		NbtList list = new NbtList();
		for (ShopRecord record : shops.values()) {
			list.add(writeRecord(record));
		}
		nbt.put(SHOPS, list);
		return nbt;
	}

	private static NbtCompound writeRecord(ShopRecord record) {
		NbtCompound nbt = new NbtCompound();
		nbt.putString(WORLD, record.worldKey().getValue().toString());
		nbt.putLong(POS, record.pos().asLong());
		nbt.putUuid(OWNER, record.ownerUuid());
		nbt.putString(SELL_ITEM, Registries.ITEM.getId(record.sellItem()).toString());
		nbt.putInt(SELL_AMOUNT, record.sellAmount());
		nbt.putString(PRICE_ITEM, Registries.ITEM.getId(record.priceItem()).toString());
		nbt.putInt(PRICE_AMOUNT, record.priceAmount());
		return nbt;
	}

	private static Optional<ShopRecord> readRecord(NbtCompound nbt) {
		if (!nbt.contains(WORLD) || !nbt.contains(POS) || !nbt.containsUuid(OWNER) || !nbt.contains(SELL_ITEM) || !nbt.contains(SELL_AMOUNT) || !nbt.contains(PRICE_ITEM) || !nbt.contains(PRICE_AMOUNT)) {
			return Optional.empty();
		}

		RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, new Identifier(nbt.getString(WORLD)));
		BlockPos pos = BlockPos.fromLong(nbt.getLong(POS));
		UUID ownerUuid = nbt.getUuid(OWNER);
		Item sellItem = Registries.ITEM.get(new Identifier(nbt.getString(SELL_ITEM)));
		Item priceItem = Registries.ITEM.get(new Identifier(nbt.getString(PRICE_ITEM)));
		int sellAmount = nbt.getInt(SELL_AMOUNT);
		int priceAmount = nbt.getInt(PRICE_AMOUNT);
		if (sellAmount < 1 || sellAmount > 64 || priceAmount < 1 || priceAmount > 64) {
			return Optional.empty();
		}

		return Optional.of(new ShopRecord(ownerUuid, worldKey, pos, sellItem, sellAmount, priceItem, priceAmount));
	}

	private record Key(RegistryKey<World> worldKey, BlockPos pos) {
		static Key of(RegistryKey<World> worldKey, BlockPos pos) {
			return new Key(worldKey, pos.toImmutable());
		}
	}
}
