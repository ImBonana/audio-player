package de.maxhenkel.audioplayer.command;

import com.mojang.brigadier.context.CommandContext;
import de.maxhenkel.admiral.annotations.Command;
import de.maxhenkel.admiral.annotations.Name;
import de.maxhenkel.admiral.annotations.RequiresPermission;
import de.maxhenkel.audioplayer.AudioManager;
import de.maxhenkel.audioplayer.AudioPlayer;
import de.maxhenkel.audioplayer.Filebin;
import net.minecraft.network.chat.*;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.net.UnknownHostException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Command("audioplayer")
public class UploadCommands {

    public static final Pattern SOUND_FILE_PATTERN = Pattern.compile("^[a-z0-9_ -]+.((wav)|(mp3))$", Pattern.CASE_INSENSITIVE);

    @RequiresPermission("audioplayer.upload")
    @Command
    public void audioPlayer(CommandContext<ServerCommandSource> context) {
        context.getSource().sendFeedback(() ->
                        Text.literal("Upload audio via Filebin ")
                                .append(Text.literal("here").styled(style -> {
                                    return style
                                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/audioplayer upload"))
                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to show more")));
                                }).formatted(Formatting.GREEN))
                                .append(".")
                , false);
        context.getSource().sendFeedback(() ->
                        Text.literal("Upload audio with access to the servers file system ")
                                .append(Text.literal("here").styled(style -> {
                                    return style
                                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/audioplayer serverfile"))
                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to show more")));
                                }).formatted(Formatting.GREEN))
                                .append(".")
                , false);
        context.getSource().sendFeedback(() ->
                        Text.literal("Upload audio from a URL ")
                                .append(Text.literal("here").styled(style -> {
                                    return style
                                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/audioplayer url"))
                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to show more")));
                                }).formatted(Formatting.GREEN))
                                .append(".")
                , false);
    }

    @RequiresPermission("audioplayer.upload")
    @Command("upload")
    @Command("filebin")
    public void filebin(CommandContext<ServerCommandSource> context) {
        UUID uuid = UUID.randomUUID();
        String uploadURL = Filebin.getBin(uuid);

        MutableText msg = Text.literal("Click ")
                .append(Text.literal("this link")
                        .styled(style -> {
                            return style
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, uploadURL))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to open")));
                        })
                        .formatted(Formatting.GREEN)
                )
                .append(" and upload your sound as ")
                .append(Text.literal("mp3").formatted(Formatting.GRAY))
                .append(" or ")
                .append(Text.literal("wav").formatted(Formatting.GRAY))
                .append(".\n")
                .append("Once you have uploaded the file, click ")
                .append(Text.literal("here")
                        .styled(style -> {
                            return style
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/audioplayer filebin " + uuid))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to confirm upload")));
                        })
                        .formatted(Formatting.GREEN)
                )
                .append(".");

        context.getSource().sendFeedback(() -> msg, false);
    }

    @RequiresPermission("audioplayer.upload")
    @Command("filebin")
    public void filebinUpload(CommandContext<ServerCommandSource> context, @Name("id") UUID sound) {
        new Thread(() -> {
            try {
                context.getSource().sendFeedback(() -> Text.literal("Downloading sound, please wait..."), false);
                Filebin.downloadSound(context.getSource().getServer(), sound);
                context.getSource().sendFeedback(() -> sendUUIDMessage(sound, Text.literal("Successfully downloaded sound.")), false);
            } catch (Exception e) {
                AudioPlayer.LOGGER.warn("{} failed to download a sound: {}", context.getSource().getName(), e.getMessage());
                context.getSource().sendError(Text.literal("Failed to download sound: %s".formatted(e.getMessage())));
            }
        }).start();
    }

    @RequiresPermission("audioplayer.upload")
    @Command("url")
    public void url(CommandContext<ServerCommandSource> context) {
        context.getSource().sendFeedback(() ->
                        Text.literal("If you have a direct link to a ")
                                .append(Text.literal(".mp3").formatted(Formatting.GRAY))
                                .append(" or ")
                                .append(Text.literal(".wav").formatted(Formatting.GRAY))
                                .append(" file, enter the following command: ")
                                .append(Text.literal("/audioplayer url <link-to-your-file>").formatted(Formatting.GRAY).styled(style -> {
                                    return style
                                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/audioplayer url "))
                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to fill in the command")));
                                }))
                                .append(".")
                , false);
    }

    @RequiresPermission("audioplayer.upload")
    @Command("url")
    public void urlUpload(CommandContext<ServerCommandSource> context, @Name("url") String url) {
        UUID sound = UUID.randomUUID();
        new Thread(() -> {
            try {
                context.getSource().sendFeedback(() -> Text.literal("Downloading sound, please wait..."), false);
                AudioManager.saveSound(context.getSource().getServer(), sound, url);
                context.getSource().sendFeedback(() -> sendUUIDMessage(sound, Text.literal("Successfully downloaded sound.")), false);
            } catch (UnknownHostException e) {
                AudioPlayer.LOGGER.warn("{} failed to download a sound: {}", context.getSource().getName(), e.toString());
                context.getSource().sendError(Text.literal("Failed to download sound: Unknown host"));
            } catch (UnsupportedAudioFileException e) {
                AudioPlayer.LOGGER.warn("{} failed to download a sound: {}", context.getSource().getName(), e.toString());
                context.getSource().sendError(Text.literal("Failed to download sound: Invalid file format"));
            } catch (Exception e) {
                AudioPlayer.LOGGER.warn("{} failed to download a sound: {}", context.getSource().getName(), e.toString());
                context.getSource().sendError(Text.literal("Failed to download sound: %s".formatted(e.getMessage())));
            }
        }).start();
    }

    @RequiresPermission("audioplayer.upload")
    @Command("serverfile")
    public void serverFile(CommandContext<ServerCommandSource> context) {
        context.getSource().sendFeedback(() ->
                        Text.literal("Upload a ")
                                .append(Text.literal(".mp3").formatted(Formatting.GRAY))
                                .append(" or ")
                                .append(Text.literal(".wav").formatted(Formatting.GRAY))
                                .append(" file to ")
                                .append(Text.literal(AudioManager.getUploadFolder().toAbsolutePath().toString()).formatted(Formatting.GRAY))
                                .append(" on the server and run the command ")
                                .append(Text.literal("/audioplayer serverfile \"yourfile.mp3\"").formatted(Formatting.GRAY).styled(style -> {
                                    return style
                                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/audioplayer serverfile "))
                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to fill in the command")));
                                }))
                                .append(".")
                , false);
    }

    @RequiresPermission("audioplayer.upload")
    @Command("serverfile")
    public void serverFileUpload(CommandContext<ServerCommandSource> context, @Name("filename") String fileName) {
        Matcher matcher = SOUND_FILE_PATTERN.matcher(fileName);
        if (!matcher.matches()) {
            context.getSource().sendError(Text.literal("Invalid file name! Valid characters are ")
                    .append(Text.literal("A-Z").formatted(Formatting.GRAY))
                    .append(", ")
                    .append(Text.literal("0-9").formatted(Formatting.GRAY))
                    .append(", ")
                    .append(Text.literal("_").formatted(Formatting.GRAY))
                    .append(" and ")
                    .append(Text.literal("-").formatted(Formatting.GRAY))
                    .append(". The name must also end in ")
                    .append(Text.literal(".mp3").formatted(Formatting.GRAY))
                    .append(" or ")
                    .append(Text.literal(".wav").formatted(Formatting.GRAY))
                    .append(".")
            );
            return;
        }
        UUID uuid = UUID.randomUUID();
        new Thread(() -> {
            Path file = AudioManager.getUploadFolder().resolve(fileName);
            try {
                AudioManager.saveSound(context.getSource().getServer(), uuid, file);
                context.getSource().sendFeedback(() -> sendUUIDMessage(uuid, Text.literal("Successfully copied sound.")), false);
                context.getSource().sendFeedback(() -> Text.literal("Deleted temporary file ").append(Text.literal(fileName).formatted(Formatting.GRAY)).append("."), false);
            } catch (NoSuchFileException e) {
                context.getSource().sendError(Text.literal("Could not find file ").append(Text.literal(fileName).formatted(Formatting.GRAY)).append("."));
            } catch (Exception e) {
                AudioPlayer.LOGGER.warn("{} failed to copy a sound: {}", context.getSource().getName(), e.getMessage());
                context.getSource().sendError(Text.literal("Failed to copy sound: %s".formatted(e.getMessage())));
            }
        }).start();
    }

    public static MutableText sendUUIDMessage(UUID soundID, MutableText component) {
        return component.append(" ")
                .append(Texts.bracketed(Text.literal("Copy ID"))
                        .styled(style -> {
                            return style
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, soundID.toString()))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Copy sound ID")));
                        })
                        .formatted(Formatting.GREEN)
                )
                .append(" ")
                .append(Texts.bracketed(Text.literal("Put on item"))
                        .styled(style -> {
                            return style
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/audioplayer apply %s".formatted(soundID.toString())))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Put the sound on an item")));
                        })
                        .formatted(Formatting.GREEN)
                );
    }

}
