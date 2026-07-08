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

	public static void createShopFromPlacedChest(ServerPlayerEntity player, ServerWorld world, ItemStack stack, BlockPos pos) {
		if (!world.getBlockState(pos).isOf(Blocks.CHEST) || !stack.hasCustomName()) {
			return;
		}

		String chestName = stack.getName().getString();
		ShopItemNbt.readPreparedShop(stack, world.getRegistryKey(), pos)
				.filter(record -> chestName.startsWith(ShopNameParser.formatShopName(record.priceAmount(), record.priceItem())))
				.or(() -> ShopNameParser.parsePriceName(chestName)
						.map(parsed -> new ShopRecord(player.getUuid(), world.getRegistryKey(), pos.toImmutable(), parsed.priceItem(), parsed.priceAmount())))
				.ifPresent(record -> {
					ShopStorage.get(world).putShop(record);
					player.sendMessage(Text.literal("Shop chest created"), false);
				});
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
					if (record.isOwner(player.getUuid())) {
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

	private static boolean beforeBlockBreak(World world, PlayerEntity player, BlockPos pos, net.minecraft.block.BlockState state, BlockEntity blockEntity) {
		if (!(world instanceof ServerWorld serverWorld) || !(player instanceof ServerPlayerEntity serverPlayer)) {
			return true;
		}

		return ShopStorage.get(serverWorld).getShop(serverWorld.getRegistryKey(), pos)
				.map(record -> {
					if (record.isOwner(serverPlayer.getUuid())) {
						ShopStorage.get(serverWorld).removeShop(serverWorld.getRegistryKey(), pos);
						return true;
					}

					serverPlayer.sendMessage(Text.literal("Only the shop owner can break this chest"), false);
					return false;
				})
				.orElse(true);
	}
}
