package ca.teamdman.sfm.common.block;

import ca.teamdman.sfm.common.registry.SFMBlockEntities;
import ca.teamdman.sfm.common.registry.SFMBlocks;
import ca.teamdman.sfm.common.util.FacadeType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import static ca.teamdman.sfm.common.block.CableFacadeBlock.FACADE_TYPE_PROP;

public class FancyCableFacadeBlock extends FancyCableBlock implements EntityBlock, IFacadableBlock {
    public FancyCableFacadeBlock() {
        super();
        registerDefaultState(defaultBlockState().setValue(FACADE_TYPE_PROP, FacadeType.TRANSLUCENT));
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
    public VoxelShape getOcclusionShape(
            BlockState pState,
            BlockGetter pLevel,
            BlockPos pPos
    ) {
        return super.getOcclusionShape(pState, pLevel, pPos);
        // Translucent blocks should have no occlusion
//        return pState.getValue(FACADE_TYPE_PROP) == FacadeType.TRANSLUCENT ?
//               Shapes.empty() :
//               Shapes.block();
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
    public boolean propagatesSkylightDown(
            BlockState pState,
            BlockGetter pLevel,
            BlockPos pPos
    ) {
        return pState.getValue(FACADE_TYPE_PROP) == FacadeType.TRANSLUCENT;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACADE_TYPE_PROP);
    }
}
