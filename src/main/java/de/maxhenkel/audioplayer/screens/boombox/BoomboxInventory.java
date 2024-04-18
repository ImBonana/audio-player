package de.maxhenkel.audioplayer.screens.boombox;

import de.maxhenkel.audioplayer.items.ItemInventory;
import net.minecraft.item.ItemStack;

public class BoomboxInventory extends ItemInventory {
    public BoomboxInventory(ItemStack item) {
        super(item, 1);
    }

    public ItemStack getTape() {
        return this.getStack(0);
    }
}
