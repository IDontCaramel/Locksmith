package net.caramel.safesale;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Locale;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class LocksmithCommands {
	private LocksmithCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher, registryAccess));
	}

	private static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
		LiteralCommandNode<ServerCommandSource> locksmith = dispatcher.register(literal("locksmith")
				.then(literal("sell")
						.then(argument("sellAmount", integer(1, 64))
								.then(argument("sellItem", ItemStackArgumentType.itemStack(registryAccess))
										.then(argument("priceAmount", integer(1, 64))
												.then(argument("priceItem", ItemStackArgumentType.itemStack(registryAccess))
														.executes(LocksmithCommands::executeSell)))))));

		dispatcher.register(literal("ls").redirect(locksmith));
	}

	private static int executeSell(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
		int sellAmount = getInteger(context, "sellAmount");
		ItemStackArgument sellItem = ItemStackArgumentType.getItemStackArgument(context, "sellItem");
		int priceAmount = getInteger(context, "priceAmount");
		ItemStackArgument priceItem = ItemStackArgumentType.getItemStackArgument(context, "priceItem");

		String chestName = formatChestName(sellAmount, sellItem, priceAmount, priceItem);
		if (!prepareSellChest(player, chestName, sellItem.getItem(), sellAmount, priceItem.getItem(), priceAmount)) {
			return 0;
		}

		sendPrivate(player, "Prepared sell chest: " + chestName);
		return 1;
	}

	private static boolean prepareSellChest(ServerPlayerEntity player, String chestName, Item sellItem, int sellAmount, Item priceItem, int priceAmount) {
		ItemStack heldStack = player.getMainHandStack();
		if (!heldStack.isOf(Items.CHEST)) {
			sendPrivate(player, "You must hold a normal chest to prepare a sell chest.");
			return false;
		}

		ItemStack renamedChest = new ItemStack(Items.CHEST);
		renamedChest.setCustomName(Text.literal(chestName));
		ShopItemNbt.writePreparedShop(renamedChest, player.getUuid(), sellItem, sellAmount, priceItem, priceAmount);

		if (heldStack.getCount() == 1) {
			player.getInventory().setStack(player.getInventory().selectedSlot, renamedChest);
			return true;
		}

		int emptySlot = player.getInventory().getEmptySlot();
		if (emptySlot == -1) {
			sendPrivate(player, "You need a free inventory slot to prepare one chest from a stack.");
			return false;
		}

		heldStack.decrement(1);
		player.getInventory().setStack(emptySlot, renamedChest);
		return true;
	}

	private static String formatChestName(int sellAmount, ItemStackArgument sellItem, int priceAmount, ItemStackArgument priceItem) {
		return sellAmount + " " + formatItemName(sellItem.getItem()) + " - " + priceAmount + " " + formatItemName(priceItem.getItem());
	}

	private static String formatItemName(Item item) {
		return item.getName().getString().toLowerCase(Locale.ROOT);
	}

	private static void sendPrivate(ServerPlayerEntity player, String message) {
		player.sendMessage(Text.literal(message), false);
	}
}
