package de.maxhenkel.audioplayer;

import de.maxhenkel.audioplayer.items.custom.RecordTapeItem;
import de.maxhenkel.configbuilder.entry.ConfigEntry;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public enum PlayerType {

    TAPE(
            AudioPlayer.SERVER_CONFIG.tapeRange,
            AudioPlayer.SERVER_CONFIG.maxTapeRange,
            AudioPlayer.SERVER_CONFIG.maxTapeDuration,
            Plugin.TAPE_CATEGORY,
            itemStack -> itemStack.getItem() instanceof RecordTapeItem
    );

    private final ConfigEntry<Float> defaultRange;
    private final ConfigEntry<Float> maxRange;
    private final ConfigEntry<Integer> maxDuration;
    private final String category;
    private final Predicate<ItemStack> validator;

    PlayerType(ConfigEntry<Float> defaultRange, ConfigEntry<Float> maxRange, ConfigEntry<Integer> maxDuration, String category, Predicate<ItemStack> validator) {
        this.defaultRange = defaultRange;
        this.maxRange = maxRange;
        this.maxDuration = maxDuration;
        this.category = category;
        this.validator = validator;
    }

    public ConfigEntry<Float> getDefaultRange() {
        return defaultRange;
    }

    public ConfigEntry<Float> getMaxRange() {
        return maxRange;
    }

    public ConfigEntry<Integer> getMaxDuration() {
        return maxDuration;
    }

    public String getCategory() {
        return category;
    }

    public Predicate<ItemStack> getValidator() {
        return validator;
    }

    public boolean isValid(ItemStack itemStack) {
        return validator.test(itemStack);
    }

    @Nullable
    public static PlayerType fromItemStack(ItemStack itemStack) {
        for (PlayerType type : values()) {
            if (type.getValidator().test(itemStack)) {
                return type;
            }
        }
        return null;
    }

}
