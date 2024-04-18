package de.maxhenkel.audioplayer.datagen;

import de.maxhenkel.audioplayer.AudioPlayer;
import de.maxhenkel.audioplayer.items.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.util.Identifier;

public class ModRecipeProvider extends FabricRecipeProvider {
    public ModRecipeProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generate(RecipeExporter exporter) {
        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.BOOMBOX, 1)
                .pattern("III")
                .pattern("IJI")
                .pattern("III")
                .input('I', Items.IRON_INGOT)
                .input('J', Items.JUKEBOX)
                .criterion(hasItem(Items.JUKEBOX), conditionsFromItem(Items.JUKEBOX))
                .offerTo(exporter, new Identifier(AudioPlayer.MOD_ID, "boombox"));

        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.RECORD_TAPE, 1)
                .pattern("   ")
                .pattern("IRI")
                .pattern("   ")
                .input('I', Items.IRON_INGOT)
                .input('R', Items.REDSTONE)
                .criterion(hasItem(Items.REDSTONE), conditionsFromItem(Items.REDSTONE))
                .criterion(hasItem(Items.IRON_INGOT), conditionsFromItem(Items.IRON_INGOT))
                .offerTo(exporter, new Identifier(AudioPlayer.MOD_ID, "record_tape"));
    }
}
