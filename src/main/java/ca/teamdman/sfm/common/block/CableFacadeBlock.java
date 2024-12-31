package ca.teamdman.sfm.common.block;

import ca.teamdman.sfm.common.facade.FacadeTransparency;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;


public class CableFacadeBlock extends CableBlock implements EntityBlock, IFacadableBlock {
    public CableFacadeBlock() {
        super();
        registerDefaultState(getStateDefinition().any().setValue(FacadeTransparency.FACADE_TRANSPARENCY_PROPERTY, FacadeTransparency.OPAQUE));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(
            BlockPos blockPos,
            BlockState blockState
    ) {
        return SFMBlockEntities.CABLE_FACADE_BLOCK_ENTITY.get().create(blockPos, blockState);
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getOcclusionShape(
            BlockState pState,
            BlockGetter pLevel,
            BlockPos pPos
    ) {
        // Translucent blocks should have no occlusion
        return pState.getValue(FacadeTransparency.FACADE_TRANSPARENCY_PROPERTY) == FacadeTransparency.TRANSLUCENT ?
               Shapes.empty() :
               Shapes.block();
    }

    @SuppressWarnings("deprecation")
    @Override
    public ItemStack getCloneItemStack(
            BlockGetter pLevel,
            BlockPos pPos,
            BlockState pState
    ) {
        return new ItemStack(SFMBlocks.CABLE_BLOCK.get());
    }

    @Override
    public boolean propagatesSkylightDown(
            BlockState pState,
            BlockGetter pLevel,
            BlockPos pPos
    ) {
        return pState.getValue(FacadeTransparency.FACADE_TRANSPARENCY_PROPERTY) == FacadeTransparency.TRANSLUCENT;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FacadeTransparency.FACADE_TRANSPARENCY_PROPERTY);
    }

    @Override
    public BlockState getStateForPlacementByFacadePlan(
            LevelAccessor level,
            BlockPos pos,
            @Nullable FacadeTransparency facadeTransparency
    ) {
        BlockState blockState = super.getStateForPlacementByFacadePlan(level, pos, facadeTransparency);
        if (facadeTransparency == null) {
            return blockState;
        }
        return blockState.setValue(FacadeTransparency.FACADE_TRANSPARENCY_PROPERTY, facadeTransparency);
    }
}
