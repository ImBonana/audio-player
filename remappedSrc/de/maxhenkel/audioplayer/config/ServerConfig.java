package de.maxhenkel.audioplayer.config;

import de.maxhenkel.configbuilder.ConfigBuilder;
import de.maxhenkel.configbuilder.entry.ConfigEntry;

public class ServerConfig {

    public final ConfigEntry<String> filebinUrl;
    public final ConfigEntry<Long> maxUploadSize;
    public final ConfigEntry<Float> tapeRange;
    public final ConfigEntry<Float> maxTapeRange;
    public final ConfigEntry<Boolean> allowWavUpload;
    public final ConfigEntry<Boolean> allowMp3Upload;
    public final ConfigEntry<Integer> maxTapeDuration;
    public final ConfigEntry<Integer> cacheSize;
    public final ConfigEntry<Boolean> allowStaticAudio;

    public ServerConfig(ConfigBuilder builder) {
        filebinUrl = builder.stringEntry(
                "filebin_url",
                "https://filebin.net/",
                "The URL of the Filebin service that the mod should use"
        );
        maxUploadSize = builder.longEntry(
                "max_upload_size",
                1000L * 1000L * 20L,
                1L,
                (long) Integer.MAX_VALUE,
                "The maximum allowed size of an uploaded file in bytes"
        );
        tapeRange = builder.floatEntry(
                "tape_range",
                65F,
                1F,
                (float) Integer.MAX_VALUE,
                "The range of tape in blocks"
        );
        maxTapeRange = builder.floatEntry(
                "max_tape_range",
                256F,
                1F,
                (float) Integer.MAX_VALUE,
                "The maximum allowed range of a tape in blocks"
        );
        allowWavUpload = builder.booleanEntry(
                "allow_wav_upload",
                true,
                "Whether users should be able to upload .wav files",
                "Note that .wav files are not compressed and can be very large",
                "Playing .wav files may result in more RAM usage"
        );
        allowMp3Upload = builder.booleanEntry(
                "allow_mp3_upload",
                true,
                "Whether users should be able to upload .mp3 files",
                "Note that .mp3 files require Simple Voice Chats mp3 decoder",
                "Playing .mp3 files can be slightly more CPU intensive"
        );
        maxTapeDuration = builder.integerEntry(
                "max_tape_duration",
                60 * 5,
                1,
                Integer.MAX_VALUE,
                "The maximum allowed duration of a tape in seconds"
        );
        cacheSize = builder.integerEntry(
                "cache_size",
                16,
                0,
                Integer.MAX_VALUE,
                "The maximum amount of audio files that are cached in memory",
                "Setting this to 0 will disable the cache",
                "A higher value will result in less disk reads, but more RAM usage"
        );
        allowStaticAudio = builder.booleanEntry(
                "allow_static_audio",
                true,
                "Static audio does not have directionality or falloff (volume does not decrease with distance)",
                "The /audioplayer setstatic [enabled] command can be used when this is set to true",
                "If this config option is disabled, static audio is completely disabled and will play as if the option wouldn't be set"
        );
    }

}
