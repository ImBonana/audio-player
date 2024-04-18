package de.maxhenkel.audioplayer;

import de.maxhenkel.voicechat.api.Player;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.StaticAudioChannel;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import javax.annotation.Nullable;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

// TODO Move this to the voice chat API
public class StaticAudioPlayer implements de.maxhenkel.voicechat.api.audiochannel.AudioPlayer, Runnable {

    private final Thread playbackThread;
    private final AudioSupplier audio;
    private final VoicechatServerApi api;
    private final String category;
    private final Vec3d pos;
    private final ServerWorld level;
    private final float distance;
    private final OpusEncoder encoder;

    private static final long FRAME_SIZE_NS = 20_000_000;
    public static final int SAMPLE_RATE = 48000;
    public static final int FRAME_SIZE = (SAMPLE_RATE / 1000) * 20;

    private final ConcurrentHashMap<UUID, StaticAudioChannel> audioChannels;
    private boolean started;
    @Nullable
    private Runnable onStopped;

    public StaticAudioPlayer(short[] audio, VoicechatServerApi api, String category, Vec3d pos, UUID playerID, ServerWorld level, float distance) {
        this.playbackThread = new Thread(this);
        this.audio = new AudioSupplier(audio);
        this.api = api;
        this.category = category;
        this.pos = pos;
        this.audioChannels = new ConcurrentHashMap<>();
        this.encoder = api.createEncoder();
        this.playbackThread.setDaemon(true);
        this.playbackThread.setName("StaticAudioPlayer-%s".formatted(playerID));
        this.level = level;
        this.distance = distance;
    }

    public static StaticAudioPlayer create(VoicechatServerApi api, ServerWorld level, UUID sound, ServerPlayerEntity p, int maxLengthSeconds, String category, Vec3d pos, UUID playerID, float distance) {
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

            StaticAudioPlayer instance = new StaticAudioPlayer(audio, api, category, pos, playerID, level, distance);
            instance.startPlaying();
            return instance;
        } catch (Exception e) {
            AudioPlayer.LOGGER.error("Failed to play audio", e);
            if (p != null) {
                p.sendMessage(Text.literal("Failed to play audio: %s".formatted(e.getMessage())).formatted(Formatting.DARK_RED), true);
            }
            return null;
        }
    }

    @Override
    public void startPlaying() {
        if (started) {
            return;
        }
        this.playbackThread.start();
        started = true;
    }

    @Override
    public void stopPlaying() {
        this.playbackThread.interrupt();
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public boolean isPlaying() {
        return playbackThread.isAlive();
    }

    @Override
    public boolean isStopped() {
        return started && !playbackThread.isAlive();
    }

    @Override
    public void setOnStopped(Runnable onStopped) {
        this.onStopped = onStopped;
    }

    @Override
    public void run() {
        int framePosition = 0;

        ScheduledFuture<?> nearbyPlayersTask = AudioPlayer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> {
            List<ServerPlayerEntity> players = api.getPlayersInRange(api.fromServerLevel(this.level), api.createPosition(pos.x, pos.y, pos.z), distance + 1F, serverPlayer -> {
                VoicechatConnection connection = api.getConnectionOf(serverPlayer);
                if (connection != null) {
                    // TODO Either document in the api that this helper is square distance, or provide a spherical version (or both?)
                    Vec3d playerPos = ((ServerPlayerEntity) serverPlayer.getPlayer()).getLerpedPos(0.0F);
                    return !connection.isDisabled() && pos.distanceTo(playerPos) <= distance;
                }
                return false;
            }).stream().map(Player::getPlayer).map(ServerPlayerEntity.class::cast).toList();

            for (ServerPlayerEntity player : players) {
                this.audioChannels.computeIfAbsent(player.getUuid(), uuid -> {
                    StaticAudioChannel audioChannel = api.createStaticAudioChannel(UUID.randomUUID(), api.fromServerLevel(this.level), api.getConnectionOf(api.fromServerPlayer(player)));
                    audioChannel.setCategory(this.category);
                    return audioChannel;
                });
            }

            List<UUID> uuids = players.stream().map(ServerPlayerEntity::getUuid).toList();

            for (UUID uuid : this.audioChannels.keySet()) {
                if (!uuids.contains(uuid)) {
                    StaticAudioChannel toRemove = this.audioChannels.remove(uuid);
                    toRemove.flush();
                }
            }
        }, 0L, 100L, TimeUnit.MILLISECONDS);

        long startTime = System.nanoTime();

        short[] frame;

        while ((frame = this.audio.get()) != null) {
            if (frame.length != FRAME_SIZE) {
                AudioPlayer.LOGGER.error("Got invalid audio frame size {}!={}", frame.length, FRAME_SIZE);
                break;
            }
            byte[] encoded = encoder.encode(frame);
            for (StaticAudioChannel audioChannel : this.audioChannels.values()) {
                audioChannel.send(encoded);
            }
            framePosition++;
            long waitTimestamp = startTime + framePosition * FRAME_SIZE_NS;

            long waitNanos = waitTimestamp - System.nanoTime();

            try {
                if (waitNanos > 0L) {
                    Thread.sleep(waitNanos / 1_000_000L, (int) (waitNanos % 1_000_000));
                }
            } catch (InterruptedException e) {
                break;
            }
        }

        encoder.close();
        nearbyPlayersTask.cancel(true);

        for (StaticAudioChannel audioChannel : this.audioChannels.values()) {
            audioChannel.flush();
        }

        if (onStopped != null) {
            onStopped.run();
        }
    }

    public class AudioSupplier implements Supplier<short[]> {

        private final short[] audioData;
        private final short[] frame;
        private int framePosition;

        public AudioSupplier(short[] audioData) {
            this.audioData = audioData;
            this.frame = new short[FRAME_SIZE];
        }

        @Override
        public short[] get() {
            if (framePosition >= audioData.length) {
                return null;
            }

            Arrays.fill(frame, (short) 0);
            System.arraycopy(audioData, framePosition, frame, 0, Math.min(frame.length, audioData.length - framePosition));
            framePosition += frame.length;
            return frame;
        }
    }
}
