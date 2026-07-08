package net.caramel.safesale.mixin;

import net.caramel.safesale.ShopEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
	@Inject(method = "place", at = @At("RETURN"))
	private void safesale$afterPlace(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> cir) {
		if (!cir.getReturnValue().isAccepted()) {
			return;
		}

		World world = context.getWorld();
		PlayerEntity player = context.getPlayer();
		if (world instanceof ServerWorld serverWorld && player instanceof ServerPlayerEntity serverPlayer) {
			ShopEvents.ShopPlacementResult result = ShopEvents.createShopFromPlacedChest(serverPlayer, serverWorld, context.getStack(), context.getBlockPos());
			switch (result) {
				case CREATED -> serverPlayer.sendMessage(Text.literal("Shop created"), false);
				case INVALID_NAME -> serverPlayer.sendMessage(Text.literal("Shop couldnt be created check the name of the item"), false);
				case IGNORED -> {
				}
			}
		}
	}
}
