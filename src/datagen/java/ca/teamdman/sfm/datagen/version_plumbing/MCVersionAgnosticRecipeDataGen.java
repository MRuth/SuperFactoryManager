package ca.teamdman.sfm.datagen.version_plumbing;

import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.data.event.GatherDataEvent;

@SuppressWarnings("SameParameterValue")
public class MCVersionAgnosticRecipeDataGen extends RecipeProvider {
    public MCVersionAgnosticRecipeDataGen(GatherDataEvent event, String modId) {
        super(event.getGenerator());
    }

    protected ShapedRecipeBuilder beginShaped(ItemLike result, int count) {
        return new ShapedRecipeBuilder(result, count);
    }

    protected ShapelessRecipeBuilder beginShapeless(ItemLike result, int count) {
        return new ShapelessRecipeBuilder(result, count);
    }
}
