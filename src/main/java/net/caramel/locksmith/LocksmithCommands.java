package net.caramel.locksmith;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.BoolArgumentType.getBool;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class LocksmithCommands {
	private static final SuggestionProvider<CommandSourceStack> REGION_NAME_SUGGESTIONS = (context, builder) -> SharedSuggestionProvider.suggest(
			RegionStorage.get(context.getSource().getLevel()).regions().stream().map(RegionRecord::name), builder);

	private LocksmithCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher, registryAccess));
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
		LiteralCommandNode<CommandSourceStack> root = dispatcher.register(literal("locksmith")
				.then(literal("admin")
						.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
						.executes(LocksmithCommands::executeAdmin))
				.then(literal("sell")
						.then(argument("priceAmount", integer(1, 64))
								.then(argument("priceItem", ItemArgument.item(registryAccess))
										.executes(LocksmithCommands::executeSell))))
				.then(literal("region")
						.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
						.then(literal("pos1")
								.executes(context -> executeSetRegionPos(context, true)))
						.then(literal("pos2")
								.executes(context -> executeSetRegionPos(context, false)))
						.then(literal("enforce")
								.then(argument("enabled", bool())
										.executes(LocksmithCommands::executeSetRegionEnforcement)))
						.then(literal("confirm")
								.then(argument("region_name", word())
										.executes(LocksmithCommands::executeConfirmRegion)))
						.then(literal("list")
								.executes(LocksmithCommands::executeListRegions))
						.then(literal("delete")
								.then(argument("region_name", word())
										.suggests(REGION_NAME_SUGGESTIONS)
										.executes(LocksmithCommands::executeDeleteRegion)))
						.then(literal("preview")
								.executes(LocksmithCommands::executePreviewSelection)
								.then(argument("region_name", word())
										.suggests(REGION_NAME_SUGGESTIONS)
										.executes(LocksmithCommands::executePreviewSavedRegion)))
						.then(literal("spawnradius")
								.then(argument("radius", integer(0))
										.executes(LocksmithCommands::executeSetSpawnRadius)))));
		dispatcher.register(literal("ls").redirect(root));
	}

	private static int executeAdmin(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		boolean enabled = AdminShopAccess.toggle(player.getUUID());
		sendPrivate(player, "shop acces " + enabled);
		return enabled ? 1 : 0;
	}

	private static int executeSell(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		int priceAmount = getInteger(context, "priceAmount");
		ItemInput priceItem = ItemArgument.getItem(context, "priceItem");

		String chestName = formatChestName(priceAmount, priceItem);
		if (!prepareSellChest(player, chestName, priceItem.item().value(), priceAmount)) {
			return 0;
		}

		sendPrivate(player, "Prepared sell chest: " + chestName);
		return 1;
	}

	private static boolean prepareSellChest(ServerPlayer player, String chestName, Item priceItem, int priceAmount) {
		ItemStack heldStack = player.getMainHandItem();
		if (!heldStack.is(Items.CHEST)) {
			sendPrivate(player, "You must hold a normal chest to prepare a sell chest");
			return false;
		}

		ItemStack renamedChest = new ItemStack(Items.CHEST);
		renamedChest.set(DataComponents.CUSTOM_NAME, Component.literal(chestName));
		ShopItemNbt.writePreparedShop(renamedChest, player.getUUID(), priceItem, priceAmount);

		if (heldStack.getCount() == 1) {
			player.getInventory().setItem(player.getInventory().getSelectedSlot(), renamedChest);
			return true;
		}

		int emptySlot = player.getInventory().getFreeSlot();
		if (emptySlot == -1) {
			sendPrivate(player, "You need a free inventory slot to prepare one chest from a stack");
			return false;
		}

		heldStack.shrink(1);
		player.getInventory().setItem(emptySlot, renamedChest);
		return true;
	}

	private static int executeSetRegionPos(CommandContext<CommandSourceStack> context, boolean pos1) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		RegionSelections.Selection selection = pos1 ? RegionSelections.setPos1(player) : RegionSelections.setPos2(player);

		sendPrivate(player, (pos1 ? "Region pos1 set to " : "Region pos2 set to ") + formatPos(player.blockPosition()) + " in " + player.level().dimension().identifier());
		RegionPreview.showSelectionUntilConfirmed(player, selection);
		if (selection.isSameWorld()) {
			sendPrivate(player, "type /locksmith region confirm region_name to create the region");
		} else if (selection.hasBothPositions()) {
			sendPrivate(player, "Region positions must be in the same dimension");
		}
		return 1;
	}

	private static int executeConfirmRegion(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		String name = getString(context, "region_name");
		RegionSelections.Selection selection = RegionSelections.get(player.getUUID()).orElse(null);
		if (selection == null || !selection.hasBothPositions()) {
			sendPrivate(player, "Set both region positions first");
			return 0;
		}

		if (!selection.isSameWorld()) {
			sendPrivate(player, "Region positions must be in the same dimension");
			return 0;
		}

		ServerLevel world = player.level();
		RegionStorage storage = RegionStorage.get(world);
		if (!storage.putRegion(RegionRecord.create(name, selection.pos1WorldKey(), selection.pos1(), selection.pos2()))) {
			sendPrivate(player, "A region named " + name + " already exists");
			return 0;
		}

		sendPrivate(player, "Region " + name + " created");
		RegionPreview.clear(player);
		return 1;
	}

	private static int executeListRegions(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		RegionStorage storage = RegionStorage.get(source.getLevel());

		source.sendSuccess(() -> Component.literal("Region enforcement: " + storage.isEnforced()), false);
		source.sendSuccess(() -> Component.literal("Spawn radius: " + storage.spawnRadius()), false);
		if (storage.regions().isEmpty()) {
			source.sendSuccess(() -> Component.literal("No custom shop regions"), false);
			return 1;
		}

		for (RegionRecord region : storage.regions()) {
			source.sendSuccess(() -> Component.literal(region.name() + " " + region.worldKey().identifier() + " " + formatPos(region.minPos()) + " to " + formatPos(region.maxPos())), false);
		}
		return storage.regions().size();
	}

	private static int executeDeleteRegion(CommandContext<CommandSourceStack> context) {
		String name = getString(context, "region_name");
		RegionStorage storage = RegionStorage.get(context.getSource().getLevel());
		if (!storage.removeRegion(name)) {
			context.getSource().sendSuccess(() -> Component.literal("No region named " + name), false);
			return 0;
		}

		context.getSource().sendSuccess(() -> Component.literal("Region " + name + " deleted"), false);
		return 1;
	}

	private static int executeSetRegionEnforcement(CommandContext<CommandSourceStack> context) {
		boolean enabled = getBool(context, "enabled");
		RegionStorage.get(context.getSource().getLevel()).setEnforced(enabled);
		context.getSource().sendSuccess(() -> Component.literal("Region enforcement set to " + enabled), false);
		return 1;
	}

	private static int executeSetSpawnRadius(CommandContext<CommandSourceStack> context) {
		int radius = getInteger(context, "radius");
		RegionStorage storage = RegionStorage.get(context.getSource().getLevel());
		storage.setSpawnRadius(radius);
		context.getSource().sendSuccess(() -> Component.literal("Spawn radius set to " + radius), false);
		return 1;
	}

	private static int executePreviewSelection(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		RegionSelections.Selection selection = RegionSelections.get(player.getUUID()).orElse(null);
		if (selection == null || !RegionPreview.showSelectionForTenSeconds(player, selection)) {
			sendPrivate(player, "Set a region position in this dimension first");
			return 0;
		}

		sendPrivate(player, "Showing region preview");
		return 1;
	}

	private static int executePreviewSavedRegion(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		String name = getString(context, "region_name");
		RegionRecord region = RegionStorage.get(player.level()).getRegion(name).orElse(null);
		if (region == null) {
			sendPrivate(player, "No region named " + name);
			return 0;
		}

		if (!RegionPreview.showRegionForTenSeconds(player, region)) {
			sendPrivate(player, "Region " + name + " is in " + region.worldKey().identifier());
			return 0;
		}

		sendPrivate(player, "Showing region " + name + " for 10 seconds");
		return 1;
	}

	private static String formatChestName(int priceAmount, ItemInput priceItem) {
		return ShopNameParser.formatShopName(priceAmount, priceItem.item().value());
	}

	private static String formatPos(BlockPos pos) {
		return pos.getX() + " " + pos.getY() + " " + pos.getZ();
	}

	private static void sendPrivate(ServerPlayer player, String message) {
		player.sendSystemMessage(Component.literal(message));
	}
}
