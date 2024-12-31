package ca.teamdman.sfm.common.block;

import ca.teamdman.sfm.common.facade.FacadeType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface IFacadableBlock {
    IFacadableBlock getNonFacadeBlock();

    IFacadableBlock getFacadeBlock();

    BlockState getStateForPlacementByFacadePlan(
            LevelAccessor level,
            BlockPos pos,
            @Nullable FacadeType facadeType
    );
}
