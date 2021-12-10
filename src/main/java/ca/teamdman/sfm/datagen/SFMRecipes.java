package ca.teamdman.sfm.datagen;

import ca.teamdman.sfm.common.registry.SFMBlocks;
import ca.teamdman.sfm.common.registry.SFMItems;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.Tags;

import java.util.function.Consumer;

public class SFMRecipes extends RecipeProvider {

    public SFMRecipes(DataGenerator generatorIn) {
        super(generatorIn);
    }

    @Override
    protected void buildCraftingRecipes(Consumer<FinishedRecipe> consumer) {
        ShapedRecipeBuilder
                .shaped(SFMBlocks.CABLE_BLOCK.get(), 16)
                .define('D', Tags.Items.DYES_BLACK)
                .define('G', Items.LIGHT_WEIGHTED_PRESSURE_PLATE)
                .define('C', Tags.Items.CHESTS)
                .define('B', Items.IRON_BARS)
                .pattern("DGD")
                .pattern("BCB")
                .pattern("DGD")
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .unlockedBy("has_chest", has(Tags.Items.CHESTS))
                .save(consumer);

        ShapedRecipeBuilder
                .shaped(SFMBlocks.MANAGER_BLOCK.get())
                .define('A', Tags.Items.CHESTS)
                .define('B', SFMBlocks.CABLE_BLOCK.get())
                .define('C', Items.REPEATER)
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .unlockedBy("has_chest", has(Tags.Items.CHESTS))
                .pattern("ABA")
                .pattern("BCB")
                .pattern("ABA")
                .save(consumer);

        ShapedRecipeBuilder
                .shaped(SFMItems.LABEL_GUN_ITEM.get())
                .define('S', Items.STICK)
                .define('B', Items.INK_SAC)
                .define('L', Items.LAPIS_LAZULI)
                .define('C', Items.OAK_SIGN)
                .unlockedBy("has_ink", has(Items.INK_SAC))
                .pattern(" LC")
                .pattern(" SB")
                .pattern("S  ")
                .save(consumer);
    }
}