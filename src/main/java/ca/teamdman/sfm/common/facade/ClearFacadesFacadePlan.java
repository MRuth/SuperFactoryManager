package ca.teamdman.sfm.common.facade;

import ca.teamdman.sfm.common.block.IFacadableBlock;
import ca.teamdman.sfm.common.localization.LocalizationKeys;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public record ClearFacadesFacadePlan(
        IFacadableBlock worldBlock,
        Set<BlockPos> positions
) implements IFacadePlan {
    @Override
    public void apply(Level level) {
        this.positions().forEach(pos -> level.setBlock(
                pos,
                this.worldBlock().getStateForPlacementByFacadePlan(
                        level,
                        pos,
                        null
                ),
                Block.UPDATE_IMMEDIATE | Block.UPDATE_CLIENTS
        ));
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public @Nullable FacadePlanWarning computeWarning(
            Level level
    ) {
        FacadePlanAnalysisResult analysisResult = FacadePlanAnalysisResult.analyze(level, positions);
        if (analysisResult.shouldWarn()) {
            return FacadePlanWarning.of(
                    LocalizationKeys.FACADE_CONFIRM_CLEAR_SCREEN_TITLE.getComponent(),
                    LocalizationKeys.FACADE_CONFIRM_CLEAR_SCREEN_MESSAGE.getComponent(
                            analysisResult.facadeDataToCount().size(),
                            analysisResult.countAffected()
                    )
            );
        }
        return null;
    }
}
