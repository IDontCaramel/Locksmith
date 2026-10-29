package net.caramel.locksmith.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.caramel.locksmith.ShopEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
	@WrapMethod(method = "place")
	private InteractionResult locksmith$afterPlace(BlockPlaceContext context, Operation<InteractionResult> original) {
		// Placing consumes the stack, and an empty stack no longer exposes its components.
		ItemStack placedStack = context.getItemInHand().copy();
		InteractionResult result = original.call(context);
		if (!result.consumesAction()) {
			return result;
		}

		if (context.getLevel() instanceof ServerLevel serverWorld && context.getPlayer() instanceof ServerPlayer serverPlayer) {
			ShopEvents.ShopPlacementResult placement = ShopEvents.createShopFromPlacedChest(serverPlayer, serverWorld, placedStack, context.getClickedPos());
			switch (placement) {
				case CREATED -> serverPlayer.sendSystemMessage(Component.literal("Shop created"));
				case INVALID_NAME -> serverPlayer.sendSystemMessage(Component.literal("Shop couldnt be created check the name of the item"));
				case OUTSIDE_REGION -> serverPlayer.sendSystemMessage(Component.literal("chest shop is outside of shop region"));
				case IGNORED -> {
				}
			}
		}
		return result;
	}
}
