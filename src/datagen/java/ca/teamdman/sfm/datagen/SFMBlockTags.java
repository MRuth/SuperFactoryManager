package ca.teamdman.sfm.datagen;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.common.registry.SFMBlocks;
import ca.teamdman.sfm.datagen.version_plumbing.MCVersionAgnosticBlockTagsDataGen;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.data.event.GatherDataEvent;

public class SFMBlockTags extends MCVersionAgnosticBlockTagsDataGen {
    public SFMBlockTags(GatherDataEvent event) {
        super(event, SFM.MOD_ID);
    }

    @Override
    protected void addBlockTags() {
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(SFMBlocks.CABLE_BLOCK.get())
                .add(SFMBlocks.CABLE_FACADE_BLOCK.get())
                .add(SFMBlocks.FANCY_CABLE_BLOCK.get())
                .add(SFMBlocks.MANAGER_BLOCK.get())
                .add(SFMBlocks.TUNNELLED_MANAGER_BLOCK.get())
                .add(SFMBlocks.PRINTING_PRESS_BLOCK.get());
        tag(BlockTags.MINEABLE_WITH_AXE)
                .add(SFMBlocks.PRINTING_PRESS_BLOCK.get());
    }

    @Override
    public String getName() {
        return "SuperFactoryManager Tags";
    }
}
