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

import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

@Command("audioplayer")
public class ApplyCommands {

    @Command("apply")
    public void apply(CommandContext<ServerCommandSource> context, @Name("file_name") String fileName, @OptionalArgument @Name("custom_name") String customName) throws CommandSyntaxException {
        UUID id = getId(context, fileName);
        if (id == null) {
            return;
        }
        apply(context, new CustomSound(id, null, false), customName);
    }

    @Command("apply")
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

        PlayerType type = PlayerType.fromItemStack(itemInHand);
        if (type == null) {
            sendInvalidHandItemMessage(context, itemInHand);
            return;
        }
        apply(context, itemInHand, type, sound, customName);
    }

    private static void apply(CommandContext<ServerCommandSource> context, ItemStack stack, PlayerType type, CustomSound customSound, @Nullable String customName) throws CommandSyntaxException {
        checkRange(type.getMaxRange(), customSound.getRange().orElse(null));
        if (!type.isValid(stack)) {
            return;
        }
        customSound.saveToItem(stack, customName != null ? customName : "Custom Sound");
        NbtCompound tag = stack.getOrCreateNbt();

        tag.remove("instrument");

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

    private static void sendInvalidHandItemMessage(CommandContext<ServerCommandSource> context, ItemStack invalidItem) {
        if (invalidItem.isEmpty()) {
            context.getSource().sendError(Text.literal("You don't have an item in your main hand"));
            return;
        }
        context.getSource().sendError(Text.literal("The item in your main hand can not have custom audio"));
    }

}
