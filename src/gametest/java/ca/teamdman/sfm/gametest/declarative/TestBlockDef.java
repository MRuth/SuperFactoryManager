package ca.teamdman.sfm.gametest.declarative;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public record TestBlockDef<T extends BlockEntity>(
        String label,
        BlockPos posRelativeToManager,
        Block block,
        @Nullable Consumer<T> blockEntityConfigurer
) {
    public static <U extends BlockEntity> TestBlockDef<U> of(
            String label,
            BlockPos posRelativeToManager,
            Block block,
            @Nullable Consumer<U> blockEntityConfigurer
    ) {
        return new TestBlockDef<>(label, posRelativeToManager, block, blockEntityConfigurer);
    }

    public static TestBlockDef<?> of(
            String label,
            BlockPos posRelativeToManager,
            Block block
    ) {
        return new TestBlockDef<>(label, posRelativeToManager, block, null);
    }
}
