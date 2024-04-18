package de.maxhenkel.audioplayer.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.maxhenkel.admiral.annotations.Command;
import de.maxhenkel.audioplayer.CustomSound;
import de.maxhenkel.audioplayer.FileNameManager;
import de.maxhenkel.audioplayer.PlayerType;
import java.util.Optional;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

@Command("audioplayer")
public class UtilityCommands {

    @Command("clear")
    public void clear(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        ItemStack itemInHand = player.getStackInHand(Hand.MAIN_HAND);

        PlayerType playerType = PlayerType.fromItemStack(itemInHand);
        if (playerType == null) {
            context.getSource().sendError(Text.literal("Invalid item"));
            return;
        }

        if (!itemInHand.hasNbt()) {
            context.getSource().sendError(Text.literal("Item does not contain NBT data"));
            return;
        }

        if (!CustomSound.clearItem(itemInHand)) {
            context.getSource().sendError(Text.literal("Item does not have custom audio"));
            return;
        }

        NbtCompound tag = itemInHand.getNbt();
        if (tag == null) {
            return;
        }

        tag.remove(ItemStack.DISPLAY_KEY);
        tag.remove("HideFlags");

        context.getSource().sendFeedback(() -> Text.literal("Successfully cleared item"), false);
    }

    @Command("id")
    public void id(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        CustomSound customSound = getHeldSound(context);
        if (customSound == null) {
            return;
        }
        context.getSource().sendFeedback(() -> UploadCommands.sendUUIDMessage(customSound.getSoundId(), Text.literal("Successfully extracted sound ID.")), false);
    }

    @Command("name")
    public void name(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        CustomSound customSound = getHeldSound(context);
        if (customSound == null) {
            return;
        }
        Optional<FileNameManager> optionalMgr = FileNameManager.instance();

        if (optionalMgr.isEmpty()) {
            context.getSource().sendError(Text.literal("An internal error occurred"));
            return;
        }

        FileNameManager mgr = optionalMgr.get();
        String fileName = mgr.getFileName(customSound.getSoundId());
        if (fileName == null) {
            context.getSource().sendError(Text.literal("Custom audio does not have an associated file name"));
            return;
        }

        context.getSource().sendFeedback(() -> Text.literal("Audio file name: ").append(Text.literal(fileName).styled(style -> style
                .withColor(Formatting.GREEN)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to copy")))
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, fileName)))), false);
    }

    private static CustomSound getHeldSound(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        ItemStack itemInHand = player.getStackInHand(Hand.MAIN_HAND);

        PlayerType playerType = PlayerType.fromItemStack(itemInHand);

        if (playerType == null) {
            context.getSource().sendError(Text.literal("Invalid item"));
            return null;
        }

        CustomSound customSound = CustomSound.of(itemInHand);
        if (customSound == null) {
            context.getSource().sendError(Text.literal("Item does not have custom audio"));
            return null;
        }

        return customSound;
    }

}
