package ca.teamdman.sfm.common.facade;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public interface IFacadePlan {
    void apply(Level level);
    Set<BlockPos> positions();
    @Nullable FacadePlanWarning warning();
}
