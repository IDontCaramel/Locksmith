package net.caramel.locksmith;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class RegionSelections {
	private static final Map<UUID, Selection> SELECTIONS = new HashMap<>();

	private RegionSelections() {
	}

	public static Selection setPos1(ServerPlayer player) {
		Selection previous = SELECTIONS.getOrDefault(player.getUUID(), Selection.empty());
		Selection next = new Selection(selectedPos(player), player.level().dimension(), previous.pos2(), previous.pos2WorldKey());
		SELECTIONS.put(player.getUUID(), next);
		return next;
	}

	public static Selection setPos2(ServerPlayer player) {
		Selection previous = SELECTIONS.getOrDefault(player.getUUID(), Selection.empty());
		Selection next = new Selection(previous.pos1(), previous.pos1WorldKey(), selectedPos(player), player.level().dimension());
		SELECTIONS.put(player.getUUID(), next);
		return next;
	}

	public static Optional<Selection> get(UUID playerUuid) {
		return Optional.ofNullable(SELECTIONS.get(playerUuid));
	}

	private static BlockPos selectedPos(ServerPlayer player) {
		return player.blockPosition().immutable();
	}

	public record Selection(BlockPos pos1, ResourceKey<Level> pos1WorldKey, BlockPos pos2, ResourceKey<Level> pos2WorldKey) {
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
