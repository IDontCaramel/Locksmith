package net.caramel.safesale;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class SafeSaleCommands {
	private static final SuggestionProvider<ServerCommandSource> REGION_NAME_SUGGESTIONS = (context, builder) -> CommandSource.suggestMatching(
			RegionStorage.get(context.getSource().getWorld()).regions().stream().map(RegionRecord::name), builder);

	private SafeSaleCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher, registryAccess));
	}

	private static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
		dispatcher.register(literal("safesale")
				.then(literal("admin")
						.requires(source -> source.hasPermissionLevel(2))
						.executes(SafeSaleCommands::executeAdmin))
				.then(literal("sell")
						.then(argument("priceAmount", integer(1, 64))
								.then(argument("priceItem", ItemStackArgumentType.itemStack(registryAccess))
										.executes(SafeSaleCommands::executeSell))))
				.then(literal("region")
						.requires(source -> source.hasPermissionLevel(2))
						.then(literal("set")
								.then(literal("pos1")
										.executes(context -> executeSetRegionPos(context, true)))
								.then(literal("pos2")
										.executes(context -> executeSetRegionPos(context, false))))
						.then(literal("confirm")
								.then(argument("region_name", word())
										.executes(SafeSaleCommands::executeConfirmRegion)))
						.then(literal("list")
								.executes(SafeSaleCommands::executeListRegions))
						.then(literal("delete")
								.then(argument("region_name", word())
										.suggests(REGION_NAME_SUGGESTIONS)
										.executes(SafeSaleCommands::executeDeleteRegion)))
						.then(literal("preview")
								.executes(SafeSaleCommands::executePreviewSelection)
								.then(argument("region_name", word())
										.suggests(REGION_NAME_SUGGESTIONS)
										.executes(SafeSaleCommands::executePreviewSavedRegion)))
						.then(literal("spawnradius")
								.then(argument("radius", integer(0))
										.executes(SafeSaleCommands::executeSetSpawnRadius)))));
	}

	private static int executeAdmin(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
		boolean enabled = AdminShopAccess.toggle(player.getUuid());
		sendPrivate(player, "shop acces " + enabled);
		return enabled ? 1 : 0;
	}

	private static int executeSell(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
		int priceAmount = getInteger(context, "priceAmount");
		ItemStackArgument priceItem = ItemStackArgumentType.getItemStackArgument(context, "priceItem");

		String chestName = formatChestName(priceAmount, priceItem);
		if (!prepareSellChest(player, chestName, priceItem.getItem(), priceAmount)) {
			return 0;
		}

		sendPrivate(player, "Prepared sell chest: " + chestName);
		return 1;
	}

	private static boolean prepareSellChest(ServerPlayerEntity player, String chestName, Item priceItem, int priceAmount) {
		ItemStack heldStack = player.getMainHandStack();
		if (!heldStack.isOf(Items.CHEST)) {
			sendPrivate(player, "You must hold a normal chest to prepare a sell chest");
			return false;
		}

		ItemStack renamedChest = new ItemStack(Items.CHEST);
		renamedChest.setCustomName(Text.literal(chestName));
		ShopItemNbt.writePreparedShop(renamedChest, player.getUuid(), priceItem, priceAmount);

		if (heldStack.getCount() == 1) {
			player.getInventory().setStack(player.getInventory().selectedSlot, renamedChest);
			return true;
		}

		int emptySlot = player.getInventory().getEmptySlot();
		if (emptySlot == -1) {
			sendPrivate(player, "You need a free inventory slot to prepare one chest from a stack");
			return false;
		}

		heldStack.decrement(1);
		player.getInventory().setStack(emptySlot, renamedChest);
		return true;
	}

	private static int executeSetRegionPos(CommandContext<ServerCommandSource> context, boolean pos1) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
		RegionSelections.Selection selection = pos1 ? RegionSelections.setPos1(player) : RegionSelections.setPos2(player);

		sendPrivate(player, (pos1 ? "Region pos1 set to " : "Region pos2 set to ") + formatPos(player.getBlockPos()) + " in " + player.getWorld().getRegistryKey().getValue());
		RegionPreview.showSelectionUntilConfirmed(player, selection);
		if (selection.isSameWorld()) {
			sendPrivate(player, "type /safesale region confirm region_name to create the region");
		} else if (selection.hasBothPositions()) {
			sendPrivate(player, "Region positions must be in the same dimension");
		}
		return 1;
	}

	private static int executeConfirmRegion(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
		String name = getString(context, "region_name");
		RegionSelections.Selection selection = RegionSelections.get(player.getUuid()).orElse(null);
		if (selection == null || !selection.hasBothPositions()) {
			sendPrivate(player, "Set both region positions first");
			return 0;
		}

		if (!selection.isSameWorld()) {
			sendPrivate(player, "Region positions must be in the same dimension");
			return 0;
		}

		ServerWorld world = player.getServerWorld();
		RegionStorage storage = RegionStorage.get(world);
		if (!storage.putRegion(RegionRecord.create(name, selection.pos1WorldKey(), selection.pos1(), selection.pos2()))) {
			sendPrivate(player, "A region named " + name + " already exists");
			return 0;
		}

		sendPrivate(player, "Region " + name + " created");
		RegionPreview.clear(player);
		return 1;
	}

	private static int executeListRegions(CommandContext<ServerCommandSource> context) {
		ServerCommandSource source = context.getSource();
		RegionStorage storage = RegionStorage.get(source.getWorld());

		source.sendFeedback(() -> Text.literal("Spawn radius: " + storage.spawnRadius()), false);
		if (storage.regions().isEmpty()) {
			source.sendFeedback(() -> Text.literal("No custom shop regions"), false);
			return 1;
		}

		for (RegionRecord region : storage.regions()) {
			source.sendFeedback(() -> Text.literal(region.name() + " " + region.worldKey().getValue() + " " + formatPos(region.minPos()) + " to " + formatPos(region.maxPos())), false);
		}
		return storage.regions().size();
	}

	private static int executeDeleteRegion(CommandContext<ServerCommandSource> context) {
		String name = getString(context, "region_name");
		RegionStorage storage = RegionStorage.get(context.getSource().getWorld());
		if (!storage.removeRegion(name)) {
			context.getSource().sendFeedback(() -> Text.literal("No region named " + name), false);
			return 0;
		}

		context.getSource().sendFeedback(() -> Text.literal("Region " + name + " deleted"), false);
		return 1;
	}

	private static int executeSetSpawnRadius(CommandContext<ServerCommandSource> context) {
		int radius = getInteger(context, "radius");
		RegionStorage storage = RegionStorage.get(context.getSource().getWorld());
		storage.setSpawnRadius(radius);
		context.getSource().sendFeedback(() -> Text.literal("Spawn radius set to " + radius), false);
		return 1;
	}

	private static int executePreviewSelection(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
		RegionSelections.Selection selection = RegionSelections.get(player.getUuid()).orElse(null);
		if (selection == null || !RegionPreview.showSelectionForTenSeconds(player, selection)) {
			sendPrivate(player, "Set a region position in this dimension first");
			return 0;
		}

		sendPrivate(player, "Showing region preview");
		return 1;
	}

	private static int executePreviewSavedRegion(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
		String name = getString(context, "region_name");
		RegionRecord region = RegionStorage.get(player.getServerWorld()).getRegion(name).orElse(null);
		if (region == null) {
			sendPrivate(player, "No region named " + name);
			return 0;
		}

		if (!RegionPreview.showRegionForTenSeconds(player, region)) {
			sendPrivate(player, "Region " + name + " is in " + region.worldKey().getValue());
			return 0;
		}

		sendPrivate(player, "Showing region " + name + " for 10 seconds");
		return 1;
	}

	private static String formatChestName(int priceAmount, ItemStackArgument priceItem) {
		return ShopNameParser.formatShopName(priceAmount, priceItem.getItem());
	}

	private static String formatPos(BlockPos pos) {
		return pos.getX() + " " + pos.getY() + " " + pos.getZ();
	}

	private static void sendPrivate(ServerPlayerEntity player, String message) {
		player.sendMessage(Text.literal(message), false);
	}
}
