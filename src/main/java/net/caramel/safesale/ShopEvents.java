package net.caramel.safesale;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class ShopEvents {
	private ShopEvents() {
	}

	public static void register() {
		UseBlockCallback.EVENT.register(ShopEvents::onUseBlock);
		PlayerBlockBreakEvents.BEFORE.register(ShopEvents::beforeBlockBreak);
	}

	public static ShopPlacementResult createShopFromPlacedChest(ServerPlayerEntity player, ServerWorld world, ItemStack stack, BlockPos pos) {
		if (!world.getBlockState(pos).isOf(Blocks.CHEST)) {
			return ShopPlacementResult.IGNORED;
		}

		if (!stack.hasCustomName()) {
			return ShopPlacementResult.IGNORED;
		}

		String chestName = stack.getName().getString();
		return ShopItemNbt.readPreparedShop(stack, world.getRegistryKey(), pos)
				.map(record -> {
					if (!chestName.startsWith(ShopNameParser.formatShopName(record.priceAmount(), record.priceItem()))) {
						return ShopPlacementResult.INVALID_NAME;
					}

					return createShopIfAllowed(world, pos, record);
				})
				.or(() -> ShopNameParser.parsePriceName(chestName)
						.map(parsed -> new ShopRecord(player.getUuid(), world.getRegistryKey(), pos.toImmutable(), parsed.priceItem(), parsed.priceAmount()))
						.map(record -> createShopIfAllowed(world, pos, record)))
				.orElse(ShopPlacementResult.IGNORED);
	}

	private static ShopPlacementResult createShopIfAllowed(ServerWorld world, BlockPos pos, ShopRecord record) {
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

	private static ActionResult onUseBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
		if (!(world instanceof ServerWorld serverWorld) || !(player instanceof ServerPlayerEntity serverPlayer)) {
			return ActionResult.PASS;
		}

		return handleShopUse(serverPlayer, serverWorld, hitResult.getBlockPos());
	}

	private static ActionResult handleShopUse(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
		return ShopStorage.get(world).getShop(world.getRegistryKey(), pos)
				.map(record -> {
					if (hasOwnerAccess(record, player)) {
						return ActionResult.PASS;
					}

					BlockEntity blockEntity = world.getBlockEntity(pos);
					if (!(blockEntity instanceof ChestBlockEntity)) {
						ShopStorage.get(world).removeShop(world.getRegistryKey(), pos);
						player.sendMessage(Text.literal("This shop is no longer valid"), false);
						return ActionResult.FAIL;
					}

					ShopScreens.openBuyScreen(player, record);
					return ActionResult.SUCCESS;
				})
				.orElse(ActionResult.PASS);
	}

	private static boolean hasOwnerAccess(ShopRecord record, ServerPlayerEntity player) {
		return record.isOwner(player.getUuid()) || AdminShopAccess.isEnabled(player.getUuid());
	}

	private static boolean beforeBlockBreak(World world, PlayerEntity player, BlockPos pos, net.minecraft.block.BlockState state, BlockEntity blockEntity) {
		if (!(world instanceof ServerWorld serverWorld) || !(player instanceof ServerPlayerEntity serverPlayer)) {
			return true;
		}

		return ShopStorage.get(serverWorld).getShop(serverWorld.getRegistryKey(), pos)
				.map(record -> {
					if (hasOwnerAccess(record, serverPlayer)) {
						ShopStorage.get(serverWorld).removeShop(serverWorld.getRegistryKey(), pos);
						return true;
					}

					serverPlayer.sendMessage(Text.literal("Only the shop owner can break this chest"), false);
					return false;
				})
				.orElse(true);
	}
}
