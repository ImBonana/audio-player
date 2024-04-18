package de.maxhenkel.audioplayer.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.maxhenkel.admiral.annotations.*;
import de.maxhenkel.audioplayer.CustomSound;
import de.maxhenkel.audioplayer.FileNameManager;
import de.maxhenkel.audioplayer.PlayerType;
import de.maxhenkel.configbuilder.entry.ConfigEntry;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.GoatHornItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

@Command("audioplayer")
public class ApplyCommands {

    @RequiresPermission("audioplayer.apply")
    @Command("apply")
    public void apply(CommandContext<ServerCommandSource> context, @Name("file_name") String fileName, @OptionalArgument @Name("range") @Min("1") Float range, @OptionalArgument @Name("custom_name") String customName) throws CommandSyntaxException {
        UUID id = getId(context, fileName);
        if (id == null) {
            return;
        }
        apply(context, new CustomSound(id, range, false), customName);
    }

    @RequiresPermission("audioplayer.apply")
    @Command("apply")
    public void apply(CommandContext<ServerCommandSource> context, @Name("file_name") String fileName, @OptionalArgument @Name("custom_name") String customName) throws CommandSyntaxException {
        UUID id = getId(context, fileName);
        if (id == null) {
            return;
        }
        apply(context, new CustomSound(id, null, false), customName);
    }

    // The apply commands for UUIDs must be below the ones with file names, so that the file name does not overwrite the UUID argument

    @RequiresPermission("audioplayer.apply")
    @Command("apply")
    @Command("musicdisc")
    @Command("goathorn")
    public void apply(CommandContext<ServerCommandSource> context, @Name("sound_id") UUID sound, @OptionalArgument @Name("range") @Min("1") Float range, @OptionalArgument @Name("custom_name") String customName) throws CommandSyntaxException {
        apply(context, new CustomSound(sound, range, false), customName);
    }

    @RequiresPermission("audioplayer.apply")
    @Command("apply")
    @Command("musicdisc")
    @Command("goathorn")
    public void apply(CommandContext<ServerCommandSource> context, @Name("sound_id") UUID sound, @OptionalArgument @Name("custom_name") String customName) throws CommandSyntaxException {
        apply(context, new CustomSound(sound, null, false), customName);
    }

    @Nullable
    private static UUID getId(CommandContext<ServerCommandSource> context, String fileName) {
        try {
            return UUID.fromString(fileName);
        } catch (Exception ignored) {
        }

        Optional<FileNameManager> optionalFileNameManager = FileNameManager.instance();
        if (optionalFileNameManager.isEmpty()) {
            context.getSource().sendError(Text.literal("An internal error occurred"));
            return null;
        }

        FileNameManager fileNameManager = optionalFileNameManager.get();
        UUID audioId = fileNameManager.getAudioId(fileName);

        if (audioId == null) {
            context.getSource().sendError(Text.literal("No audio with name '%s' found or more than one found".formatted(fileName)));
            return null;
        }
        return audioId;
    }

    private static void apply(CommandContext<ServerCommandSource> context, CustomSound sound, @Nullable String customName) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        ItemStack itemInHand = player.getStackInHand(Hand.MAIN_HAND);

        if (isShulkerBox(itemInHand)) {
            applyShulker(context, sound, customName);
            return;
        }

        PlayerType type = PlayerType.fromItemStack(itemInHand);
        if (type == null) {
            sendInvalidHandItemMessage(context, itemInHand);
            return;
        }
        apply(context, itemInHand, type, sound, customName);
    }

    @RequiresPermission("audioplayer.set_static")
    @Command("setstatic")
    public void setStatic(CommandContext<ServerCommandSource> context, @Name("enabled") Optional<Boolean> enabled) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        ItemStack itemInHand = player.getStackInHand(Hand.MAIN_HAND);

        PlayerType playerType = PlayerType.fromItemStack(itemInHand);

        if (playerType == null) {
            sendInvalidHandItemMessage(context, itemInHand);
            return;
        }
        CustomSound customSound = CustomSound.of(itemInHand);
        if (customSound == null) {
            context.getSource().sendError(Text.literal("This item does not have custom audio"));
            return;
        }

        CustomSound newSound = customSound.asStatic(enabled.orElse(true));
        newSound.saveToItemIgnoreLore(itemInHand);

        context.getSource().sendFeedback(() -> Text.literal((enabled.orElse(true) ? "Enabled" : "Disabled") + " static audio"), false);
    }

    private static void applyShulker(CommandContext<ServerCommandSource> context, CustomSound sound, @Nullable String customName) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        ItemStack itemInHand = player.getStackInHand(Hand.MAIN_HAND);
        if (isShulkerBox(itemInHand)) {
            processShulker(context, itemInHand, sound, customName);
            return;
        }
        context.getSource().sendError(Text.literal("You don't have a shulker box in your main hand"));
    }

    private static void processShulker(CommandContext<ServerCommandSource> context, ItemStack shulkerItem, CustomSound sound, @Nullable String customName) throws CommandSyntaxException {
        NbtList shulkerContents = shulkerItem.getOrCreateSubNbt(BlockItem.BLOCK_ENTITY_TAG_KEY).getList(ShulkerBoxBlockEntity.ITEMS_KEY, NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < shulkerContents.size(); i++) {
            NbtCompound currentItem = shulkerContents.getCompound(i);
            ItemStack itemStack = ItemStack.fromNbt(currentItem);
            PlayerType playerType = PlayerType.fromItemStack(itemStack);
            if (playerType == null) {
                continue;
            }
            apply(context, itemStack, playerType, sound, customName);
            currentItem.put("tag", itemStack.getOrCreateNbt());
        }
        context.getSource().sendFeedback(() -> Text.literal("Successfully updated contents"), false);
    }

    private static void apply(CommandContext<ServerCommandSource> context, ItemStack stack, PlayerType type, CustomSound customSound, @Nullable String customName) throws CommandSyntaxException {
        checkRange(type.getMaxRange(), customSound.getRange().orElse(null));
        if (!type.isValid(stack)) {
            return;
        }
        customSound.saveToItem(stack, customName);
        NbtCompound tag = stack.getOrCreateNbt();

        if (stack.getItem() instanceof GoatHornItem) {
            tag.putString("instrument", "");
        } else {
            tag.remove("instrument");
        }

        if (stack.getItem() instanceof BlockItem) {
            NbtCompound blockEntityTag = stack.getOrCreateSubNbt(BlockItem.BLOCK_ENTITY_TAG_KEY);
            customSound.saveToNbt(blockEntityTag);
        }

        context.getSource().sendFeedback(() -> Text.literal("Successfully updated ").append(stack.getName()), false);
    }

    private static void checkRange(ConfigEntry<Float> maxRange, @Nullable Float range) throws CommandSyntaxException {
        if (range == null) {
            return;
        }
        if (range > maxRange.get()) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.floatTooHigh().create(range, maxRange.get());
        }
    }

    public static boolean isShulkerBox(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockitem && blockitem.getBlock() instanceof ShulkerBoxBlock;
    }

    private static void sendInvalidHandItemMessage(CommandContext<ServerCommandSource> context, ItemStack invalidItem) {
        if (invalidItem.isEmpty()) {
            context.getSource().sendError(Text.literal("You don't have an item in your main hand"));
            return;
        }
        context.getSource().sendError(Text.literal("The item in your main hand can not have custom audio"));
    }

}
