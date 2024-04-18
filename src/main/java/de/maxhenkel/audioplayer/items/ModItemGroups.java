package de.maxhenkel.audioplayer.items;

import de.maxhenkel.audioplayer.AudioPlayer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroups {
    public static final ItemGroup MAIN_GROUP = Registry.register(Registries.ITEM_GROUP,
            new Identifier(AudioPlayer.MOD_ID, "boombox"),
            FabricItemGroup.builder().displayName(Text.translatable("itemgroup.boombox"))
                    .icon(() -> new ItemStack(ModItems.BOOMBOX)).entries(ModItems::addItemsToIngredientTabItemGroup).build());

    public static void registerItemGroups() {
        AudioPlayer.LOGGER.info("Registering Item Groups for " + AudioPlayer.MOD_ID);
    }
}
