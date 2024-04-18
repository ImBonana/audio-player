package de.maxhenkel.audioplayer.util;

import de.maxhenkel.audioplayer.CustomSound;
import net.minecraft.item.ItemStack;

import java.util.UUID;

public interface ICanUseBoombox {
    boolean audio_player$play(ItemStack itemStack, CustomSound sound);
    void audio_player$stop();
    UUID audio_player$getChannelId();
}
