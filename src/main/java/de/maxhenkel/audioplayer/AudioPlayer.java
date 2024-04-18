package de.maxhenkel.audioplayer;

import de.maxhenkel.admiral.MinecraftAdmiral;
import de.maxhenkel.audioplayer.command.ApplyCommands;
import de.maxhenkel.audioplayer.command.PlayCommands;
import de.maxhenkel.audioplayer.command.UploadCommands;
import de.maxhenkel.audioplayer.command.UtilityCommands;
import de.maxhenkel.audioplayer.config.ServerConfig;
import de.maxhenkel.audioplayer.items.ModItemGroups;
import de.maxhenkel.audioplayer.items.ModItems;
import de.maxhenkel.audioplayer.network.ModPackets;
import de.maxhenkel.audioplayer.screens.ModScreenHandlers;
import de.maxhenkel.audioplayer.util.ModKeyBinds;
import de.maxhenkel.configbuilder.ConfigBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;

public class AudioPlayer implements ModInitializer {

    public static final String MOD_ID = "audioplayer";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static ServerConfig SERVER_CONFIG;

    public static AudioCache AUDIO_CACHE;

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                MinecraftAdmiral.builder(dispatcher, registryAccess).addCommandClasses(
                    UploadCommands.class,
                    ApplyCommands.class,
                    UtilityCommands.class,
                    PlayCommands.class
            ).setPermissionManager(AudioPlayerPermissionManager.INSTANCE).build());

        FileNameManager.init();

        SERVER_CONFIG = ConfigBuilder.builder(ServerConfig::new).path(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID).resolve("audioplayer-server.properties")).build();

        try {
            Files.createDirectories(AudioManager.getUploadFolder());
        } catch (IOException e) {
            LOGGER.warn("Failed to create upload folder", e);
        }

        AUDIO_CACHE = new AudioCache(SERVER_CONFIG.cacheSize.get());

        ModPackets.registerC2SPackets();

        ModItems.registerModItems();
        ModItemGroups.registerItemGroups();

        ModScreenHandlers.registerScreenHandlers();
    }
}
