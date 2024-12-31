package ca.teamdman.sfm.common.facade;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.common.block.IFacadableBlock;
import ca.teamdman.sfm.common.blockentity.IFacadeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public record ApplyFacadesFacadePlan(
        IFacadableBlock worldBlock,
        BlockState renderBlockState,
        FacadeType facadeType,
        Direction renderHitDirection,
        Set<BlockPos> positions,
        @Nullable FacadePlanWarning warning
) implements IFacadePlan {
    @Override
    public void apply(Level level) {
        this.positions().forEach(pos ->{
            level.setBlock(
                    pos,
                    this.worldBlock().getStateForPlacementByFacadePlan(
                            level,
                            pos,
                            this.facadeType()
                    ),
                    Block.UPDATE_IMMEDIATE | Block.UPDATE_CLIENTS
            );
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof IFacadeBlockEntity<?> facadeBlockEntity) {
                facadeBlockEntity.updateFacadeData(this.renderBlockState(), this.renderHitDirection());
            } else {
                SFM.LOGGER.warn("Block entity {} at {} is not a facade block entity", pos, blockEntity);
            }
        });
    }
}
