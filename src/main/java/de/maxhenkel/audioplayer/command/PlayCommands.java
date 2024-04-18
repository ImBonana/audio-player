package de.maxhenkel.audioplayer.command;

import com.mojang.brigadier.context.CommandContext;
import de.maxhenkel.admiral.annotations.Command;
import de.maxhenkel.admiral.annotations.Min;
import de.maxhenkel.admiral.annotations.Name;
import de.maxhenkel.admiral.annotations.RequiresPermission;
import de.maxhenkel.audioplayer.PlayerManager;
import de.maxhenkel.audioplayer.Plugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

@Command("audioplayer")
public class PlayCommands {

    @RequiresPermission("audioplayer.play_command")
    @Command("play")
    public void play(CommandContext<ServerCommandSource> context, @Name("sound") UUID sound, @Name("location") Vec3d location, @Name("range") @Min("0") float range) {
        @Nullable ServerPlayerEntity player = context.getSource().getPlayer();
        VoicechatServerApi api = Plugin.voicechatServerApi;
        if (api == null) {
            return;
        }
        PlayerManager.instance().playLocational(
                api,
                context.getSource().getWorld(),
                location,
                sound,
                player,
                range,
                null,
                Integer.MAX_VALUE,
                true
        );
        context.getSource().sendFeedback(() -> Text.literal("Successfully played %s".formatted(sound)), false);
    }

    @RequiresPermission("audioplayer.play_command")
    @Command("stop")
    private static int stop(CommandContext<ServerCommandSource> context, @Name("sound") UUID sound) {
        UUID channelID = PlayerManager.instance().findChannelID(sound, true);

        if (channelID != null) {
            PlayerManager.instance().stop(channelID);
            context.getSource().sendFeedback(() -> Text.literal("Successfully stopped %s".formatted(sound)), false);
            return 1;
        } else {
            context.getSource().sendError(Text.literal("Failed to stop, could not find sound with ID %s".formatted(sound)));
        }
        return 0;
    }

}
