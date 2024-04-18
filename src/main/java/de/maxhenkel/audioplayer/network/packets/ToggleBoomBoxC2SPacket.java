package de.maxhenkel.audioplayer.network.packets;

import de.maxhenkel.audioplayer.CustomSound;
import de.maxhenkel.audioplayer.PlayerManager;
import de.maxhenkel.audioplayer.items.custom.BoomboxItem;
import de.maxhenkel.audioplayer.screens.boombox.BoomboxInventory;
import de.maxhenkel.audioplayer.util.ICanUseBoombox;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.TypedActionResult;

import java.util.UUID;

public class ToggleBoomBoxC2SPacket {
    public static void receive(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        server.execute(() -> {
            ItemStack stack = player.getEquippedStack(EquipmentSlot.HEAD);
            if(stack.getItem() instanceof BoomboxItem) {
                BoomboxInventory boomboxInventory = new BoomboxInventory(stack);

                CustomSound customSound = CustomSound.of(boomboxInventory.getTape());

                if (customSound == null) return;
                PlayerManager playerManager = PlayerManager.instance();

                ICanUseBoombox iCanUseBoombox = (ICanUseBoombox) player;

                UUID channel = iCanUseBoombox.audio_player$getChannelId();

                if(channel == null || !playerManager.isPlaying(channel)) {
                    iCanUseBoombox.audio_player$play(stack, customSound);
                    return;
                }

                iCanUseBoombox.audio_player$stop();
            }
        });
    }
}
