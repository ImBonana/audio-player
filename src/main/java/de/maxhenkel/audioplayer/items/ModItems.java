package de.maxhenkel.audioplayer.items;

import de.maxhenkel.audioplayer.AudioPlayer;
import de.maxhenkel.audioplayer.items.custom.BoomboxItem;
import de.maxhenkel.audioplayer.items.custom.RecordTapeItem;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class ModItems {
    private static final List<Item> modGroupItems = new ArrayList<>();

    public static final Item RECORD_TAPE = registerItem("record_tape", new RecordTapeItem(new FabricItemSettings().maxCount(1)));
    public static final Item BOOMBOX = registerItem("boombox", new BoomboxItem(new FabricItemSettings().maxCount(1)));

    private static Item registerItem(String name, Item item) {
        return registerItem(name, item, true);
    }

    private static Item registerItem(String name, Item item, boolean addToGroupTab) {
        Item registered = Registry.register(Registries.ITEM, new Identifier(AudioPlayer.MOD_ID, name), item);
        if(addToGroupTab) addItemToCategory(registered);
        return registered;
    }

    public static void registerModItems() {
        AudioPlayer.LOGGER.info("Registering Mod Items for " + AudioPlayer.MOD_ID);
    }

    public static void addItemToCategory(Item item) {
        modGroupItems.add(item);
    }

    public static void addItemsToIngredientTabItemGroup(ItemGroup.DisplayContext displayContext, ItemGroup.Entries entries) {
        for(Item item : modGroupItems) {
            entries.add(item);
        }
    }
}
