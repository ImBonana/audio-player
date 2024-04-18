package de.maxhenkel.audioplayer;

import de.maxhenkel.voicechat.api.Entity;
import de.maxhenkel.voicechat.api.Player;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import de.maxhenkel.voicechat.api.audiochannel.EntityAudioChannel;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

public class PlayerManager {

    private final Map<UUID, PlayerReference> players;
    private final ExecutorService executor;

    public PlayerManager() {
        this.players = new ConcurrentHashMap<>();
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "AudioPlayerThread");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Nullable
    public UUID playLocational(VoicechatServerApi api, ServerWorld level, Vec3d pos, UUID sound, @Nullable ServerPlayerEntity p, float distance, @Nullable String category, int maxLengthSeconds) {
        return playLocational(api, level, pos, sound, p, distance, category, maxLengthSeconds, false);
    }

    @Nullable
    public UUID playLocational(VoicechatServerApi api, ServerWorld level, Vec3d pos, UUID sound, @Nullable ServerPlayerEntity p, float distance, @Nullable String category, int maxLengthSeconds, boolean byCommand) {
        UUID channelID = UUID.randomUUID();
        LocationalAudioChannel channel = api.createLocationalAudioChannel(channelID, api.fromServerLevel(level), api.createPosition(pos.x, pos.y, pos.z));
        if (channel == null) {
            return null;
        }
        if (category != null) {
            channel.setCategory(category);
        }
        channel.setDistance(distance);
        api.getPlayersInRange(api.fromServerLevel(level), channel.getLocation(), distance + 1F, serverPlayer -> {
            VoicechatConnection connection = api.getConnectionOf(serverPlayer);
            if (connection != null) {
                return connection.isDisabled();
            }
            return true;
        }).stream().map(Player::getPlayer).map(ServerPlayerEntity.class::cast).forEach(player -> {
            player.sendMessage(Text.literal("You need to enable voice chat to hear custom audio"), true);
        });

        AtomicBoolean stopped = new AtomicBoolean();
        AtomicReference<de.maxhenkel.voicechat.api.audiochannel.AudioPlayer> player = new AtomicReference<>();

        players.put(channelID, new PlayerReference(() -> {
            synchronized (stopped) {
                stopped.set(true);
                de.maxhenkel.voicechat.api.audiochannel.AudioPlayer audioPlayer = player.get();
                if (audioPlayer != null) {
                    audioPlayer.stopPlaying();
                }
            }
        }, player, sound, byCommand));

        executor.execute(() -> {
            de.maxhenkel.voicechat.api.audiochannel.AudioPlayer audioPlayer = playChannel(api, channel, level, sound, p, maxLengthSeconds);
            if (audioPlayer == null) {
                players.remove(channelID);
                return;
            }
            audioPlayer.setOnStopped(() -> {
                players.remove(channelID);
            });
            synchronized (stopped) {
                if (!stopped.get()) {
                    player.set(audioPlayer);
                } else {
                    audioPlayer.stopPlaying();
                }
            }
        });
        return channelID;
    }

    public UUID playOnEntity(VoicechatServerApi api, ServerWorld level, Entity source, UUID sound, @Nullable String category, ServerPlayerEntity p, int maxLengthSeconds, float distance) {
        return playOnEntity(api, level, source, sound, category, distance, p, maxLengthSeconds, false);
    }

    public UUID playOnEntity(VoicechatServerApi api, ServerWorld level, Entity source, UUID sound, @Nullable String category, float distance, ServerPlayerEntity p, int maxLengthSeconds, boolean byCommand) {
        UUID channelID = UUID.randomUUID();
        EntityAudioChannel channel = api.createEntityAudioChannel(channelID, source);
        if (channel == null) {
            return null;
        }
        if (category != null) {
            channel.setCategory(category);
        }
        channel.setDistance(distance);
        api.getPlayersInRange(api.fromServerLevel(level), channel.getEntity().getPosition(), distance + 1F, serverPlayer -> {
            VoicechatConnection connection = api.getConnectionOf(serverPlayer);
            if (connection != null) {
                return connection.isDisabled();
            }
            return true;
        }).stream().map(Player::getPlayer).map(ServerPlayerEntity.class::cast).forEach(player -> {
            player.sendMessage(Text.literal("You need to enable voice chat to hear custom audio"), true);
        });

        AtomicBoolean stopped = new AtomicBoolean();
        AtomicReference<de.maxhenkel.voicechat.api.audiochannel.AudioPlayer> player = new AtomicReference<>();

        players.put(channelID, new PlayerReference(() -> {
            synchronized (stopped) {
                stopped.set(true);
                de.maxhenkel.voicechat.api.audiochannel.AudioPlayer audioPlayer = player.get();
                if (audioPlayer != null) {
                    audioPlayer.stopPlaying();
                }
            }
        }, player, sound, byCommand));

        executor.execute(() -> {
            de.maxhenkel.voicechat.api.audiochannel.AudioPlayer audioPlayer = playChannel(api, channel, level, sound, p, maxLengthSeconds);
            if (audioPlayer == null) {
                players.remove(channelID);
                return;
            }
            audioPlayer.setOnStopped(() -> {
                players.remove(channelID);
            });
            synchronized (stopped) {
                if (!stopped.get()) {
                    player.set(audioPlayer);
                } else {
                    audioPlayer.stopPlaying();
                }
            }
        });
        return channelID;
    }

    @Nullable
    public UUID playStatic(VoicechatServerApi api, ServerWorld level, Vec3d pos, UUID sound, @Nullable ServerPlayerEntity p, float distance, @Nullable String category, int maxLengthSeconds) {
        return playStatic(api, level, pos, sound, p, distance, category, maxLengthSeconds, false);
    }

    @Nullable
    public UUID playStatic(VoicechatServerApi api, ServerWorld level, Vec3d pos, UUID sound, @Nullable ServerPlayerEntity p, float distance, @Nullable String category, int maxLengthSeconds, boolean byCommand) {
        UUID channelID = UUID.randomUUID();

        api.getPlayersInRange(api.fromServerLevel(level), api.createPosition(pos.x, pos.y, pos.z), distance + 1F, serverPlayer -> {
            VoicechatConnection connection = api.getConnectionOf(serverPlayer);
            if (connection != null) {
                return connection.isDisabled();
            }
            return true;
        }).stream().map(Player::getPlayer).map(ServerPlayerEntity.class::cast).forEach(player -> {
            player.sendMessage(Text.literal("You need to enable voice chat to hear custom audio"), true);
        });

        StaticAudioPlayer staticAudioPlayer = StaticAudioPlayer.create(api, level, sound, p, maxLengthSeconds, category, pos, channelID, distance);

        AtomicBoolean stopped = new AtomicBoolean();
        AtomicReference<de.maxhenkel.voicechat.api.audiochannel.AudioPlayer> player = new AtomicReference<>();

        players.put(channelID, new PlayerReference(() -> {
            synchronized (stopped) {
                stopped.set(true);
                de.maxhenkel.voicechat.api.audiochannel.AudioPlayer audioPlayer = player.get();
                if (audioPlayer != null) {
                    audioPlayer.stopPlaying();
                }
            }
        }, player, sound, byCommand));

        executor.execute(() -> {
            if (staticAudioPlayer == null) {
                players.remove(channelID);
                return;
            }
            staticAudioPlayer.setOnStopped(() -> {
                players.remove(channelID);
            });
            synchronized (stopped) {
                if (!stopped.get()) {
                    player.set(staticAudioPlayer);
                } else {
                    staticAudioPlayer.stopPlaying();
                }
            }
        });
        return channelID;
    }


    @Nullable
    private de.maxhenkel.voicechat.api.audiochannel.AudioPlayer playChannel(VoicechatServerApi api, AudioChannel channel, ServerWorld level, UUID sound, ServerPlayerEntity p, int maxLengthSeconds) {
        try {
            short[] audio = AudioManager.getSound(level.getServer(), sound);

            if (AudioManager.getLengthSeconds(audio) > maxLengthSeconds) {
                if (p != null) {
                    p.sendMessage(Text.literal("Audio is too long to play").formatted(Formatting.DARK_RED), true);
                } else {
                    AudioPlayer.LOGGER.error("Audio {} was too long to play", sound);
                }
                return null;
            }

            de.maxhenkel.voicechat.api.audiochannel.AudioPlayer player = api.createAudioPlayer(channel, api.createEncoder(), audio);
            player.startPlaying();
            return player;
        } catch (Exception e) {
            AudioPlayer.LOGGER.error("Failed to play audio", e);
            if (p != null) {
                p.sendMessage(Text.literal("Failed to play audio: %s".formatted(e.getMessage())).formatted(Formatting.DARK_RED), true);
            }
            return null;
        }
    }

    public void stop(UUID channelID) {
        PlayerReference player = players.get(channelID);
        if (player != null) {
            player.onStop.stop();
        }
        players.remove(channelID);
    }

    public boolean isPlaying(UUID channelID) {
        PlayerReference player = players.get(channelID);
        if (player == null) {
            return false;
        }
        de.maxhenkel.voicechat.api.audiochannel.AudioPlayer p = player.player.get();
        if (p == null) {
            return true;
        }
        return p.isPlaying();
    }

    private static PlayerManager instance;

    public static PlayerManager instance() {
        if (instance == null) {
            instance = new PlayerManager();
        }
        return instance;
    }

    private interface Stoppable {
        void stop();
    }
    
    private record PlayerReference(Stoppable onStop,
                                   AtomicReference<de.maxhenkel.voicechat.api.audiochannel.AudioPlayer> player, UUID sound, boolean byCommand) {
    }

    @Nullable
    public UUID findChannelID(UUID sound, boolean onlyByCommand) {
        for (Map.Entry<UUID, PlayerReference> entry : players.entrySet()) {
            if (entry.getValue().sound.equals(sound) && (entry.getValue().byCommand || !onlyByCommand)) {
                return entry.getKey();
            }
        }
        return null;
    }

}
