package ca.teamdman.sfm.common.block;

import ca.teamdman.sfm.common.facade.FacadeType;
import ca.teamdman.sfm.common.registry.SFMBlockEntities;
import ca.teamdman.sfm.common.registry.SFMBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jetbrains.annotations.Nullable;

public class FancyCableFacadeBlock extends FancyCableBlock implements EntityBlock, IFacadableBlock {
    public FancyCableFacadeBlock() {
        super();
        registerDefaultState(defaultBlockState().setValue(FacadeType.FACADE_TYPE_PROPERTY, FacadeType.TRANSLUCENT));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(
            BlockPos blockPos,
            BlockState blockState
    ) {
        return SFMBlockEntities.FANCY_CABLE_FACADE_BLOCK_ENTITY.get().create(blockPos, blockState);
    }

    @SuppressWarnings("deprecation")
    @Override
    public ItemStack getCloneItemStack(
            BlockGetter pLevel,
            BlockPos pPos,
            BlockState pState
    ) {
        return new ItemStack(SFMBlocks.FANCY_CABLE_BLOCK.get());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FacadeType.FACADE_TYPE_PROPERTY);
    }

    @Override
    public BlockState getStateForPlacementByFacadePlan(
            LevelAccessor level,
            BlockPos pos,
            @Nullable FacadeType facadeType
    ) {
        BlockState state = super.getStateForPlacementByFacadePlan(level, pos, facadeType);
        if (facadeType == null) {
            return state;
        }
        return state.setValue(FacadeType.FACADE_TYPE_PROPERTY, facadeType);
    }
}
