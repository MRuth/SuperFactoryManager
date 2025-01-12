package ca.teamdman.sfm.datagen;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.common.registry.SFMBlocks;
import ca.teamdman.sfm.datagen.version_plumbing.MCVersionAgnosticLootTablesDataGen;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.registries.RegistryObject;

import java.util.Set;

public class SFMLootTables extends MCVersionAgnosticLootTablesDataGen {

    public SFMLootTables(GatherDataEvent event) {
        super(event, SFM.MOD_ID);
    }

    @Override
    protected void populate(BlockLootWriter writer) {
        writer.dropSelf(SFMBlocks.MANAGER_BLOCK);
        writer.dropSelf(SFMBlocks.TUNNELLED_MANAGER_BLOCK);
        writer.dropSelf(SFMBlocks.CABLE_BLOCK);
        writer.dropOther(SFMBlocks.CABLE_FACADE_BLOCK, SFMBlocks.CABLE_BLOCK);
        writer.dropSelf(SFMBlocks.FANCY_CABLE_BLOCK);
        writer.dropOther(SFMBlocks.FANCY_CABLE_FACADE_BLOCK, SFMBlocks.FANCY_CABLE_BLOCK);
        writer.dropSelf(SFMBlocks.PRINTING_PRESS_BLOCK);
        writer.dropSelf(SFMBlocks.WATER_TANK_BLOCK);
    }

    @Override
    protected Set<? extends RegistryObject<Block>> getExpectedBlocks() {
        return SFMBlocks.getBlocks();
    }
}
