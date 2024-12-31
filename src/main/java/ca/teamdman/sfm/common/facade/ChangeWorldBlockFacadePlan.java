package ca.teamdman.sfm.common.facade;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.common.block.IFacadableBlock;
import ca.teamdman.sfm.common.blockentity.IFacadeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public record ChangeWorldBlockFacadePlan(
        IFacadableBlock worldBlock,
        Set<BlockPos> positions,
        @Nullable FacadePlanWarning warning
) implements IFacadePlan {
    @Override
    public void apply(Level level) {
        this.positions().forEach(pos -> {
            if (level.getBlockEntity(pos) instanceof IFacadeBlockEntity<?> oldFacadeBlockEntity) {
                // this position already has a facade

                // get the old state
                BlockState oldState = level.getBlockState(pos);
                IFacadeBlockEntity.FacadeData facadeData = oldFacadeBlockEntity.getFacadeData();

                // if the old state is valid, we can set the new world block and restore the facade
                if (facadeData != null && oldState.hasProperty(FacadeType.FACADE_TYPE_PROPERTY)) {
                    level.setBlock(
                            pos,
                            this.worldBlock().getFacadeBlock().getStateForPlacementByFacadePlan(
                                    level,
                                    pos,
                                    oldState.getValue(FacadeType.FACADE_TYPE_PROPERTY)
                            ),
                            Block.UPDATE_IMMEDIATE | Block.UPDATE_CLIENTS
                    );
                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    if (blockEntity instanceof IFacadeBlockEntity<?> facadeBlockEntity) {
                        facadeBlockEntity.updateFacadeData(
                                facadeData.getRenderBlockState(),
                                facadeData.getRenderHitDirection()
                        );
                    } else {
                        SFM.LOGGER.warn("Block entity {} at {} is not a facade block entity", pos, blockEntity);
                    }
                }
            } else {
                // there was no old facade, just set the new world block
                level.setBlock(
                        pos,
                        this.worldBlock().getNonFacadeBlock().getStateForPlacementByFacadePlan(
                                level,
                                pos,
                                null
                        ),
                        Block.UPDATE_IMMEDIATE | Block.UPDATE_CLIENTS
                );
            }
        });
    }
}
