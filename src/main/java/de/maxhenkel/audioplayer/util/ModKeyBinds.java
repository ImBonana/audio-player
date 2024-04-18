package de.maxhenkel.audioplayer.util;

import de.maxhenkel.audioplayer.network.ModPackets;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class ModKeyBinds {
    public static final String KEY_CATEGORY_MAIN = "key.category.audioplayer.main";

    public static final String KEY_TOGGLE_BOOMBOX = "key.audioplayer.toggle_boombox";

    public static KeyBinding toggleBoomBoxKey;

    public static void registerKeyInput() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if(toggleBoomBoxKey.wasPressed()) {
                ClientPlayNetworking.send(ModPackets.TOGGLE_BOOMBOX, PacketByteBufs.create());
            }
        });
    }

    public static void register() {
        toggleBoomBoxKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(KEY_TOGGLE_BOOMBOX, InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Z, KEY_CATEGORY_MAIN));

        registerKeyInput();
    }
}
