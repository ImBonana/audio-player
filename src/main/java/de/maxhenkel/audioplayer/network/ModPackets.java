package de.maxhenkel.audioplayer.network;

import de.maxhenkel.audioplayer.AudioPlayer;
import de.maxhenkel.audioplayer.network.packets.ToggleBoomBoxC2SPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

public class ModPackets {
    public static final Identifier TOGGLE_BOOMBOX = new Identifier(AudioPlayer.MOD_ID, "toggle_boombox");

    public static void registerC2SPackets() {
        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_BOOMBOX, ToggleBoomBoxC2SPacket::receive);
    }

    public static void registerS2CPackets() {

    }
}
