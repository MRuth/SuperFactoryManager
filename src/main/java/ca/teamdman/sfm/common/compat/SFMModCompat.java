package ca.teamdman.sfm.common.compat;

import ca.teamdman.sfm.common.registry.SFMResourceTypes;
import ca.teamdman.sfm.common.resourcetype.GasResourceType;
import ca.teamdman.sfm.common.resourcetype.InfuseResourceType;
import ca.teamdman.sfm.common.resourcetype.PigmentResourceType;
import ca.teamdman.sfm.common.resourcetype.SlurryResourceType;
import ca.teamdman.sfm.common.util.NotStored;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class SFMModCompat {
    public static boolean isMekanismLoaded() {
        return isModLoaded("mekanism");
    }

    public static boolean isAE2Loaded() {
        return isModLoaded("ae2");
    }

    public static boolean isModLoaded(String modid) {
        return ModList.get().getModContainerById(modid).isPresent();
    }

    public static boolean isMekanismBlock(
            Level level,
            @NotStored BlockPos pos
    ) {
        Block block = level.getBlockState(pos).getBlock();
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);
        assert blockId != null;
        return blockId.getNamespace().equals("mekanism");
    }
}
