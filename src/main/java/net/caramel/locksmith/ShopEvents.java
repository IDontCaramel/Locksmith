package net.caramel.locksmith;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class ShopEvents {
	private ShopEvents() {
	}

	public static void register() {
		UseBlockCallback.EVENT.register(ShopEvents::onUseBlock);
		PlayerBlockBreakEvents.BEFORE.register(ShopEvents::beforeBlockBreak);
	}

	public static ShopPlacementResult createShopFromPlacedChest(ServerPlayer player, ServerLevel world, ItemStack stack, BlockPos pos) {
		if (!world.getBlockState(pos).is(Blocks.CHEST)) {
			return ShopPlacementResult.IGNORED;
		}

		if (!stack.has(DataComponents.CUSTOM_NAME)) {
			return ShopPlacementResult.IGNORED;
		}

		String chestName = stack.getHoverName().getString();
		return ShopItemNbt.readPreparedShop(stack, world.dimension(), pos)
				.map(record -> {
					if (!chestName.startsWith(ShopNameParser.formatShopName(record.priceAmount(), record.priceItem()))) {
						return ShopPlacementResult.INVALID_NAME;
					}

					return createShopIfAllowed(world, pos, record);
				})
				.or(() -> ShopNameParser.parsePriceName(chestName)
						.map(parsed -> new ShopRecord(player.getUUID(), world.dimension(), pos.immutable(), parsed.priceItem(), parsed.priceAmount()))
						.map(record -> createShopIfAllowed(world, pos, record)))
				.orElse(ShopPlacementResult.IGNORED);
	}

	private static ShopPlacementResult createShopIfAllowed(ServerLevel world, BlockPos pos, ShopRecord record) {
		if (!RegionChecks.isShopPlacementAllowed(world, pos)) {
			return ShopPlacementResult.OUTSIDE_REGION;
		}

		ShopStorage.get(world).putShop(record);
		return ShopPlacementResult.CREATED;
	}

	public enum ShopPlacementResult {
		IGNORED,
		CREATED,
		INVALID_NAME,
		OUTSIDE_REGION
	}

	private static InteractionResult onUseBlock(Player player, Level world, InteractionHand hand, BlockHitResult hitResult) {
		if (!(world instanceof ServerLevel serverWorld) || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.PASS;
		}

		return handleShopUse(serverPlayer, serverWorld, hitResult.getBlockPos());
	}

	private static InteractionResult handleShopUse(ServerPlayer player, ServerLevel world, BlockPos pos) {
		return ShopStorage.get(world).getShop(world.dimension(), pos)
				.map(record -> {
					if (hasOwnerAccess(record, player)) {
						return InteractionResult.PASS;
					}

					BlockEntity blockEntity = world.getBlockEntity(pos);
					if (!(blockEntity instanceof ChestBlockEntity)) {
						ShopStorage.get(world).removeShop(world.dimension(), pos);
						player.sendSystemMessage(Component.literal("This shop is no longer valid"));
						return InteractionResult.FAIL;
					}

					ShopScreens.openBuyScreen(player, record);
					return InteractionResult.SUCCESS;
				})
				.orElse(InteractionResult.PASS);
	}

	private static boolean hasOwnerAccess(ShopRecord record, ServerPlayer player) {
		return record.isOwner(player.getUUID()) || AdminShopAccess.isEnabled(player.getUUID());
	}

	private static boolean beforeBlockBreak(Level world, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		if (!(world instanceof ServerLevel serverWorld) || !(player instanceof ServerPlayer serverPlayer)) {
			return true;
		}

		return ShopStorage.get(serverWorld).getShop(serverWorld.dimension(), pos)
				.map(record -> {
					if (hasOwnerAccess(record, serverPlayer)) {
						ShopStorage.get(serverWorld).removeShop(serverWorld.dimension(), pos);
						return true;
					}

					serverPlayer.sendSystemMessage(Component.literal("Only the shop owner can break this chest"));
					return false;
				})
				.orElse(true);
	}
}
