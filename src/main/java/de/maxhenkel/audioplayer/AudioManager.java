package de.maxhenkel.audioplayer;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.UUID;

public class AudioManager {

    public static final WorldSavePath AUDIO_DATA = new WorldSavePath("audio_player_data");

    public static short[] getSound(MinecraftServer server, UUID id) throws Exception {
        return AudioPlayer.AUDIO_CACHE.get(id, () -> AudioConverter.convert(getExistingSoundFile(server, id)));
    }

    public static Path getSoundFile(MinecraftServer server, UUID id, String extension) {
        return getAudioDataFolder(server).resolve(id.toString() + "." + extension);
    }

    public static Path getAudioDataFolder(MinecraftServer server) {
        return server.getSavePath(AUDIO_DATA);
    }

    public static Path getExistingSoundFile(MinecraftServer server, UUID id) throws FileNotFoundException {
        Path file = getSoundFile(server, id, AudioConverter.AudioType.MP3.getExtension());
        if (Files.exists(file)) {
            return file;
        }
        file = getSoundFile(server, id, AudioConverter.AudioType.WAV.getExtension());
        if (Files.exists(file)) {
            return file;
        }
        throw new FileNotFoundException("Audio does not exist");
    }

    public static Path getUploadFolder() {
        return FabricLoader.getInstance().getGameDir().resolve("audioplayer_uploads");
    }

    public static void saveSound(MinecraftServer server, UUID id, String url) throws UnsupportedAudioFileException, IOException {
        byte[] data = download(new URL(url), AudioPlayer.SERVER_CONFIG.maxUploadSize.get());

        AudioConverter.AudioType audioType = AudioConverter.getAudioType(data);
        checkExtensionAllowed(audioType);

        Path soundFile = getSoundFile(server, id, audioType.getExtension());
        if (Files.exists(soundFile)) {
            throw new FileAlreadyExistsException("This audio already exists");
        }
        Files.createDirectories(soundFile.getParent());

        try (OutputStream outputStream = Files.newOutputStream(soundFile)) {
            IOUtils.write(data, outputStream);
        }

        FileNameManager.instance().ifPresent(mgr -> mgr.addFileName(id, FileNameManager.getFileNameFromUrl(url)));
    }

    public static void saveSound(MinecraftServer server, UUID id, Path file) throws UnsupportedAudioFileException, IOException {
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            throw new NoSuchFileException("The file %s does not exist".formatted(file.toString()));
        }

        long size = Files.size(file);
        if (size > AudioPlayer.SERVER_CONFIG.maxUploadSize.get()) {
            throw new IOException("Maximum file size exceeded (%sMB>%sMB)".formatted(Math.round((float) size / 1_000_000F), Math.round(AudioPlayer.SERVER_CONFIG.maxUploadSize.get().floatValue() / 1_000_000F)));
        }

        AudioConverter.AudioType audioType = AudioConverter.getAudioType(file);
        checkExtensionAllowed(audioType);

        Path soundFile = getSoundFile(server, id, audioType.getExtension());
        if (Files.exists(soundFile)) {
            throw new FileAlreadyExistsException("This audio already exists");
        }
        Files.createDirectories(soundFile.getParent());

        Files.move(file, soundFile);
        FileNameManager.instance().ifPresent(mgr -> mgr.addFileName(id, FileNameManager.getFileNameFromPath(file)));
    }

    public static void checkExtensionAllowed(@Nullable AudioConverter.AudioType audioType) throws UnsupportedAudioFileException {
        if (audioType == null) {
            throw new UnsupportedAudioFileException("Unsupported audio format");
        }
        if (audioType.equals(AudioConverter.AudioType.MP3)) {
            if (!AudioPlayer.SERVER_CONFIG.allowMp3Upload.get()) {
                throw new UnsupportedAudioFileException("Uploading mp3 files is not allowed on this server");
            }
        }
        if (audioType.equals(AudioConverter.AudioType.WAV)) {
            if (!AudioPlayer.SERVER_CONFIG.allowWavUpload.get()) {
                throw new UnsupportedAudioFileException("Uploading wav files is not allowed on this server");
            }
        }
    }

    private static byte[] download(URL url, long limit) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        BufferedInputStream bis = new BufferedInputStream(url.openStream());

        int nRead;
        byte[] data = new byte[32768];

        while ((nRead = bis.read(data, 0, data.length)) != -1) {
            bos.write(data, 0, nRead);
            if (bos.size() > limit) {
                bis.close();
                throw new IOException("Maximum file size of %sMB exceeded".formatted((int) (((float) limit) / 1_000_000F)));
            }
        }
        bis.close();
        return bos.toByteArray();
    }

    public static float getLengthSeconds(short[] audio) {
        return (float) audio.length / AudioConverter.FORMAT.getSampleRate();
    }

}
