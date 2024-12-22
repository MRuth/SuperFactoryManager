package ca.teamdman.sfm.common.blockentity;

import ca.teamdman.sfm.common.registry.SFMBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class CableFacadeBlockEntity extends FacadeBlockEntity {
    public CableFacadeBlockEntity(BlockPos pos, BlockState state) {
        super(SFMBlockEntities.CABLE_FACADE_BLOCK_ENTITY.get(), pos, state);
    }
}
