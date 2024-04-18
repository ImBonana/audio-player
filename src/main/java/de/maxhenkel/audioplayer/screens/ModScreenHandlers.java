package de.maxhenkel.audioplayer.screens;

import de.maxhenkel.audioplayer.AudioPlayer;
import de.maxhenkel.audioplayer.screens.boombox.BoomboxScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class ModScreenHandlers {
    public static final ScreenHandlerType<BoomboxScreenHandler> BOOMBOX_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, new Identifier(AudioPlayer.MOD_ID, "boombox"), new ExtendedScreenHandlerType<>(BoomboxScreenHandler::new));

    public static void registerScreenHandlers() {
        AudioPlayer.LOGGER.info("Registering Screen Handlers for " + AudioPlayer.MOD_ID);
    }
}
