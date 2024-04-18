package de.maxhenkel.audioplayer;

import de.maxhenkel.configbuilder.entry.ConfigEntry;
import javax.annotation.Nullable;
import net.minecraft.block.SkullBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.Optional;
import java.util.UUID;

public class CustomSound {

    public static final String CUSTOM_SOUND = "CustomSound";
    public static final String CUSTOM_SOUND_RANGE = "CustomSoundRange";
    public static final String CUSTOM_SOUND_STATIC = "IsStaticCustomSound";

    public static final String DEFAULT_HEAD_LORE = "Has custom audio";

    protected UUID soundId;
    @Nullable
    protected Float range;
    protected boolean staticSound;

    public CustomSound(UUID soundId, @Nullable Float range, boolean staticSound) {
        this.soundId = soundId;
        this.range = range;
        this.staticSound = staticSound;
    }

    @Nullable
    public static CustomSound of(ItemStack item) {
        NbtCompound tag = item.getNbt();
        if (tag == null) {
            return null;
        }
        return of(tag);
    }

    @Nullable
    public static CustomSound of(NbtCompound tag) {
        UUID soundId;
        if (tag.contains(CUSTOM_SOUND)) {
            soundId = tag.getUuid(CUSTOM_SOUND);
        } else {
            return null;
        }
        Float range = null;
        if (tag.contains(CUSTOM_SOUND_RANGE)) {
            range = tag.getFloat(CUSTOM_SOUND_RANGE);
        }
        boolean staticSound = false;
        if (tag.contains(CUSTOM_SOUND_STATIC)) {
            staticSound = tag.getBoolean(CUSTOM_SOUND_STATIC);
        }
        return new CustomSound(soundId, range, staticSound);
    }

    public UUID getSoundId() {
        return soundId;
    }

    public Optional<Float> getRange() {
        return Optional.ofNullable(range);
    }

    public float getRange(PlayerType playerType) {
        return getRangeOrDefault(playerType.getDefaultRange(), playerType.getMaxRange());
    }

    public float getRangeOrDefault(ConfigEntry<Float> defaultRange, ConfigEntry<Float> maxRange) {
        if (range == null) {
            return defaultRange.get();
        } else if (range > maxRange.get()) {
            return maxRange.get();
        } else {
            return range;
        }
    }

    public boolean isStaticSound() {
        return staticSound;
    }

    public void saveToNbt(NbtCompound tag) {
        if (soundId != null) {
            tag.putUuid(CUSTOM_SOUND, soundId);
        } else {
            tag.remove(CUSTOM_SOUND);
        }
        if (range != null) {
            tag.putFloat(CUSTOM_SOUND_RANGE, range);
        } else {
            tag.remove(CUSTOM_SOUND_RANGE);
        }
        if (staticSound) {
            tag.putBoolean(CUSTOM_SOUND_STATIC, true);
        } else {
            tag.remove(CUSTOM_SOUND_STATIC);
        }
    }

    public void saveToItemIgnoreLore(ItemStack stack) {
        saveToItem(stack, null, false);
    }

    public void saveToItem(ItemStack stack) {
        saveToItem(stack, null);
    }

    public void saveToItem(ItemStack stack, @Nullable String loreString) {
        saveToItem(stack, loreString, true);
    }

    private void saveToItem(ItemStack stack, @Nullable String loreString, boolean applyLore) {
        NbtCompound tag = stack.getOrCreateNbt();
        saveToNbt(tag);
        NbtList lore = new NbtList();
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof SkullBlock) {
            NbtCompound blockEntityTag = stack.getOrCreateSubNbt(BlockItem.BLOCK_ENTITY_TAG_KEY);
            saveToNbt(blockEntityTag);
            if (loreString == null) {
                lore.add(0, NbtString.of(Text.Serialization.toJsonString(Text.literal(DEFAULT_HEAD_LORE).styled(style -> style.withItalic(false)).formatted(Formatting.GRAY))));
            }
        }

        if (loreString != null) {
            lore.add(0, NbtString.of(Text.Serialization.toJsonString(Text.literal(loreString).styled(style -> style.withItalic(false)).formatted(Formatting.GRAY))));
        }

        NbtCompound display = new NbtCompound();
        display.put(ItemStack.LORE_KEY, lore);
        if (applyLore) {
            tag.put(ItemStack.DISPLAY_KEY, display);
        }

        tag.putInt("HideFlags", ItemStack.TooltipSection.ADDITIONAL.getFlag());
    }

    public CustomSound asStatic(boolean staticSound) {
        return new CustomSound(soundId, range, staticSound);
    }

    public static boolean clearItem(ItemStack stack) {
        NbtCompound tag = stack.getNbt();
        if (tag == null) {
            return false;
        }
        if (!tag.contains(CUSTOM_SOUND)) {
            return false;
        }
        tag.remove(CUSTOM_SOUND);
        tag.remove(CUSTOM_SOUND_RANGE);
        tag.remove(CUSTOM_SOUND_STATIC);
        if (stack.getItem() instanceof BlockItem) {
            NbtCompound blockEntityTag = stack.getOrCreateSubNbt(BlockItem.BLOCK_ENTITY_TAG_KEY);
            blockEntityTag.remove(CUSTOM_SOUND);
            blockEntityTag.remove(CUSTOM_SOUND_RANGE);
            blockEntityTag.remove(CUSTOM_SOUND_STATIC);
        }
        return true;
    }

}
