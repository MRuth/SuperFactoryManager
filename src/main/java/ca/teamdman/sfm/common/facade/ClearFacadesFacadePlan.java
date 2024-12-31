package ca.teamdman.sfm.common.facade;

import ca.teamdman.sfm.common.block.IFacadableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public record ClearFacadesFacadePlan(
        IFacadableBlock worldBlock,
        Set<BlockPos> positions,
        @Nullable FacadePlanWarning warning
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
}
