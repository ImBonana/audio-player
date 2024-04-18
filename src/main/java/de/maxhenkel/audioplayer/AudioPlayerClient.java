package de.maxhenkel.audioplayer;

import de.maxhenkel.audioplayer.network.ModPackets;
import de.maxhenkel.audioplayer.screens.ModScreenHandlers;
import de.maxhenkel.audioplayer.screens.boombox.BoomboxScreen;
import de.maxhenkel.audioplayer.util.ModKeyBinds;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class AudioPlayerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModKeyBinds.register();
        ModPackets.registerS2CPackets();

        HandledScreens.register(ModScreenHandlers.BOOMBOX_SCREEN_HANDLER, BoomboxScreen::new);
    }
}
