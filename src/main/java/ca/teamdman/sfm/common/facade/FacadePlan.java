package ca.teamdman.sfm.common.facade;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.common.blockentity.IFacadeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Consumer;

public record FacadePlan(
        BlockState worldBlockState,
        @Nullable BlockState renderBlockState,
        Set<BlockPos> positions,
        Direction direction,
        @Nullable FacadePlanWarning warning
) {
    public void apply(Level level) {
        Consumer<BlockPos> painter;
        if (this.renderBlockState() == null) {
            // we are clearing the facade
            painter = pos -> level.setBlock(
                    pos,
                    this.worldBlockState(),
                    Block.UPDATE_IMMEDIATE | Block.UPDATE_CLIENTS
            );
        } else {
            // we are setting a facade
            painter = pos -> {
                level.setBlock(pos, this.worldBlockState(), Block.UPDATE_IMMEDIATE | Block.UPDATE_CLIENTS);
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof IFacadeBlockEntity<?> facadeBlockEntity) {
                    facadeBlockEntity.updateFacadeData(this.renderBlockState(), this.direction());
                } else {
                    SFM.LOGGER.warn("Block entity {} at {} is not a facade block entity", pos, blockEntity);
                }
            };
        }
        this.positions().forEach(painter);
    }
}
