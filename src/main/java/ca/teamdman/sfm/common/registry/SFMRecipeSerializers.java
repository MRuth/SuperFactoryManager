package ca.teamdman.sfm.common.registry;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.common.recipe.DiskResetRecipe;
import ca.teamdman.sfm.common.recipe.LabelGunResetRecipe;
import ca.teamdman.sfm.common.recipe.PrintingPressRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleRecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SFMRecipeSerializers {
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(
            ForgeRegistries.RECIPE_SERIALIZERS,
            SFM.MOD_ID
    );

    public static final RegistryObject<RecipeSerializer<PrintingPressRecipe>> PRINTING_PRESS = RECIPE_SERIALIZERS.register(
            "printing_press",
            PrintingPressRecipe.Serializer::new
    );

    public static final RegistryObject<SimpleRecipeSerializer<DiskResetRecipe>> DISK_RESET = RECIPE_SERIALIZERS.register(
            "disk_reset",
            () -> new SimpleRecipeSerializer<>(DiskResetRecipe::new)
    );
    public static final RegistryObject<SimpleRecipeSerializer<LabelGunResetRecipe>> LABEL_GUN_RESET = RECIPE_SERIALIZERS.register(
            "label_gun_reset",
            () -> new SimpleRecipeSerializer<>(LabelGunResetRecipe::new)
    );

    public static void register(IEventBus bus) {
        RECIPE_SERIALIZERS.register(bus);
    }
}
