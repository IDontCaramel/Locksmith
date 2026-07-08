package net.caramel.safesale;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class RegionSelections {
	private static final Map<UUID, Selection> SELECTIONS = new HashMap<>();

	private RegionSelections() {
	}

	public static Selection setPos1(ServerPlayerEntity player) {
		Selection previous = SELECTIONS.getOrDefault(player.getUuid(), Selection.empty());
		Selection next = new Selection(selectedPos(player), player.getWorld().getRegistryKey(), previous.pos2(), previous.pos2WorldKey());
		SELECTIONS.put(player.getUuid(), next);
		return next;
	}

	public static Selection setPos2(ServerPlayerEntity player) {
		Selection previous = SELECTIONS.getOrDefault(player.getUuid(), Selection.empty());
		Selection next = new Selection(previous.pos1(), previous.pos1WorldKey(), selectedPos(player), player.getWorld().getRegistryKey());
		SELECTIONS.put(player.getUuid(), next);
		return next;
	}

	public static Optional<Selection> get(UUID playerUuid) {
		return Optional.ofNullable(SELECTIONS.get(playerUuid));
	}

	private static BlockPos selectedPos(ServerPlayerEntity player) {
		return player.getBlockPos().toImmutable();
	}

	public record Selection(BlockPos pos1, RegistryKey<World> pos1WorldKey, BlockPos pos2, RegistryKey<World> pos2WorldKey) {
		static Selection empty() {
			return new Selection(null, null, null, null);
		}

		boolean hasBothPositions() {
			return pos1 != null && pos1WorldKey != null && pos2 != null && pos2WorldKey != null;
		}

		boolean isSameWorld() {
			return hasBothPositions() && pos1WorldKey.equals(pos2WorldKey);
		}
	}
}
