package ca.teamdman.sfm.common.facade;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.common.block.IFacadableBlock;
import ca.teamdman.sfm.common.blockentity.IFacadeBlockEntity;
import ca.teamdman.sfm.common.localization.LocalizationKeys;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public record ApplyFacadesFacadePlan(
        FacadeData facadeData,
        FacadeTransparency facadeTransparency,
        Set<BlockPos> positions
) implements IFacadePlan {
    @Override
    public void apply(Level level) {
        this.positions().forEach(pos -> {
            BlockState blockState = level.getBlockState(pos);
            Block block = blockState.getBlock();
            if (block instanceof IFacadableBlock facadableBlock) {
                BlockState nextBlockState = facadableBlock.getFacadeBlock().getStateForPlacementByFacadePlan(
                        level,
                        pos,
                        this.facadeTransparency()
                );
                level.setBlock(pos, nextBlockState, Block.UPDATE_IMMEDIATE | Block.UPDATE_CLIENTS);
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof IFacadeBlockEntity facadeBlockEntity) {
                    facadeBlockEntity.updateFacadeData(this.facadeData());
                } else {
                    SFM.LOGGER.warn("Block entity {} at {} is not a facade block entity", pos, blockEntity);
                }
            } else {
                SFM.LOGGER.warn("Block {} at {} is not a facadable block", block, pos);
            }
        });
    }

    @Override
    public @Nullable FacadePlanWarning computeWarning(
            Level level
    ) {
        FacadePlanAnalysisResult analysisResult = FacadePlanAnalysisResult.analyze(level, positions);
        if (analysisResult.shouldWarn()) {
            return FacadePlanWarning.of(
                    LocalizationKeys.FACADE_CONFIRM_APPLY_SCREEN_TITLE.getComponent(),
                    LocalizationKeys.FACADE_CONFIRM_APPLY_SCREEN_MESSAGE.getComponent(
                            analysisResult.facadeDataToCount().size(),
                            analysisResult.countAffected()
                    )
            );
        }
        return null;
    }
}
