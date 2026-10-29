package net.caramel.locksmith;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class RegionStorage extends SavedData {
	private static final SavedDataType<RegionStorage> TYPE = new SavedDataType<>(
			Locksmith.id("regions"),
			RegionStorage::new,
			CompoundTag.CODEC.xmap(RegionStorage::fromNbt, RegionStorage::writeNbt),
			null
	);
	private static final String ENFORCED = "Enforced";
	private static final String REGIONS = "Regions";
	private static final String SPAWN_RADIUS = "SpawnRadius";
	private static final String NAME = "Name";
	private static final String WORLD = "World";
	private static final String MIN_POS = "MinPos";
	private static final String MAX_POS = "MaxPos";
	private static final int DEFAULT_SPAWN_RADIUS = 100;

	private final Map<String, RegionRecord> regions = new LinkedHashMap<>();
	private boolean enforced = true;
	private int spawnRadius = DEFAULT_SPAWN_RADIUS;

	public static RegionStorage get(ServerLevel world) {
		return world.getServer().getDataStorage().computeIfAbsent(TYPE);
	}

	private static RegionStorage fromNbt(CompoundTag nbt) {
		RegionStorage storage = new RegionStorage();
		nbt.getBoolean(ENFORCED).ifPresent(value -> storage.enforced = value);
		nbt.getInt(SPAWN_RADIUS).ifPresent(radius -> storage.spawnRadius = Math.max(0, radius));
		nbt.getListOrEmpty(REGIONS).compoundStream()
				.forEach(tag -> readRecord(tag).ifPresent(record -> storage.regions.put(record.name(), record)));
		return storage;
	}

	public boolean putRegion(RegionRecord record) {
		if (regions.containsKey(record.name())) {
			return false;
		}

		regions.put(record.name(), record);
		setDirty();
		return true;
	}

	public boolean removeRegion(String name) {
		if (regions.remove(name) == null) {
			return false;
		}

		setDirty();
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

	public boolean isEnforced() {
		return enforced;
	}

	public void setEnforced(boolean enforced) {
		this.enforced = enforced;
		setDirty();
	}

	public int spawnRadius() {
		return spawnRadius;
	}

	public void setSpawnRadius(int spawnRadius) {
		this.spawnRadius = Math.max(0, spawnRadius);
		setDirty();
	}

	private CompoundTag writeNbt() {
		CompoundTag nbt = new CompoundTag();
		nbt.putBoolean(ENFORCED, enforced);
		nbt.putInt(SPAWN_RADIUS, spawnRadius);

		ListTag list = new ListTag();
		for (RegionRecord region : regions.values()) {
			list.add(writeRecord(region));
		}
		nbt.put(REGIONS, list);
		return nbt;
	}

	private static CompoundTag writeRecord(RegionRecord record) {
		CompoundTag nbt = new CompoundTag();
		nbt.putString(NAME, record.name());
		nbt.store(WORLD, Level.RESOURCE_KEY_CODEC, record.worldKey());
		nbt.putLong(MIN_POS, record.minPos().asLong());
		nbt.putLong(MAX_POS, record.maxPos().asLong());
		return nbt;
	}

	private static Optional<RegionRecord> readRecord(CompoundTag nbt) {
		Optional<String> name = nbt.getString(NAME).filter(value -> !value.isBlank());
		Optional<ResourceKey<Level>> worldKey = nbt.read(WORLD, Level.RESOURCE_KEY_CODEC);
		Optional<Long> minPos = nbt.getLong(MIN_POS);
		Optional<Long> maxPos = nbt.getLong(MAX_POS);
		if (name.isEmpty() || worldKey.isEmpty() || minPos.isEmpty() || maxPos.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(RegionRecord.create(name.get(), worldKey.get(), BlockPos.of(minPos.get()), BlockPos.of(maxPos.get())));
	}
}
