package ca.teamdman.sfm.common.block;

import net.minecraft.world.level.block.Block;

public interface IFacadableBlock {
    Block getNonFacadeBlock();
    Block getFacadeBlock();
}
