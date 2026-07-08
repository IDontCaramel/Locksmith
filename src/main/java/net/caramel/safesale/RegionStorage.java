package net.caramel.safesale;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class RegionStorage extends PersistentState {
	private static final String STATE_ID = SafeSale.MOD_ID + "_regions";
	private static final String REGIONS = "Regions";
	private static final String SPAWN_RADIUS = "SpawnRadius";
	private static final String NAME = "Name";
	private static final String WORLD = "World";
	private static final String MIN_POS = "MinPos";
	private static final String MAX_POS = "MaxPos";
	private static final int DEFAULT_SPAWN_RADIUS = 100;

	private final Map<String, RegionRecord> regions = new LinkedHashMap<>();
	private int spawnRadius = DEFAULT_SPAWN_RADIUS;

	public static RegionStorage get(ServerWorld world) {
		return world.getServer().getOverworld().getPersistentStateManager().getOrCreate(RegionStorage::fromNbt, RegionStorage::new, STATE_ID);
	}

	public static RegionStorage fromNbt(NbtCompound nbt) {
		RegionStorage storage = new RegionStorage();
		if (nbt.contains(SPAWN_RADIUS)) {
			storage.spawnRadius = Math.max(0, nbt.getInt(SPAWN_RADIUS));
		}

		NbtList list = nbt.getList(REGIONS, NbtCompound.COMPOUND_TYPE);
		for (int i = 0; i < list.size(); i++) {
			readRecord(list.getCompound(i)).ifPresent(record -> storage.regions.put(record.name(), record));
		}
		return storage;
	}

	public boolean putRegion(RegionRecord record) {
		if (regions.containsKey(record.name())) {
			return false;
		}

		regions.put(record.name(), record);
		markDirty();
		return true;
	}

	public boolean removeRegion(String name) {
		if (regions.remove(name) == null) {
			return false;
		}

		markDirty();
		return true;
	}

	public boolean hasRegion(String name) {
		return regions.containsKey(name);
	}

	public Optional<RegionRecord> getRegion(String name) {
		return Optional.ofNullable(regions.get(name));
	}

	public Collection<RegionRecord> regions() {
		return regions.values();
	}

	public int spawnRadius() {
		return spawnRadius;
	}

	public void setSpawnRadius(int spawnRadius) {
		this.spawnRadius = Math.max(0, spawnRadius);
		markDirty();
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt) {
		nbt.putInt(SPAWN_RADIUS, spawnRadius);

		NbtList list = new NbtList();
		for (RegionRecord region : regions.values()) {
			list.add(writeRecord(region));
		}
		nbt.put(REGIONS, list);
		return nbt;
	}

	private static NbtCompound writeRecord(RegionRecord record) {
		NbtCompound nbt = new NbtCompound();
		nbt.putString(NAME, record.name());
		nbt.putString(WORLD, record.worldKey().getValue().toString());
		nbt.putLong(MIN_POS, record.minPos().asLong());
		nbt.putLong(MAX_POS, record.maxPos().asLong());
		return nbt;
	}

	private static Optional<RegionRecord> readRecord(NbtCompound nbt) {
		if (!nbt.contains(NAME) || !nbt.contains(WORLD) || !nbt.contains(MIN_POS) || !nbt.contains(MAX_POS)) {
			return Optional.empty();
		}

		String name = nbt.getString(NAME);
		if (name.isBlank()) {
			return Optional.empty();
		}

		RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, new Identifier(nbt.getString(WORLD)));
		return Optional.of(RegionRecord.create(name, worldKey, BlockPos.fromLong(nbt.getLong(MIN_POS)), BlockPos.fromLong(nbt.getLong(MAX_POS))));
	}
}
