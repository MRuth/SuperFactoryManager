package ca.teamdman.sfm.gametest.declarative;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class TestBlockDef<T extends BlockEntity> {
    private final String label;
    private final BlockPos relativePos;
    private final Block block;

    // Optional: A reference to a function that will configure the block entity after placement
    private final @Nullable Consumer<T> blockEntityConfigurer;

    public TestBlockDef(
            String label,
            BlockPos relativePos,
            Block block,
            @Nullable Consumer<T> blockEntityConfigurer
    ) {
        this.label = label;
        this.relativePos = relativePos;
        this.block = block;
        this.blockEntityConfigurer = blockEntityConfigurer;
    }

    public String label() {
        return label;
    }

    public BlockPos relativePos() {
        return relativePos;
    }

    public Block block() {
        return block;
    }

    public @Nullable Consumer<T> blockEntityConfigurer() {
        return blockEntityConfigurer;
    }

    /**
     * Helper static methods for convenience
     */
    public static <U extends BlockEntity> TestBlockDef<U> of(
            String label,
            BlockPos relativePos,
            Block block,
            @Nullable Consumer<U> blockEntityConfigurer
    ) {
        return new TestBlockDef<>(label, relativePos, block, blockEntityConfigurer);
    }

    public static TestBlockDef<?> ofNoBlockEntity(
            String label,
            BlockPos relativePos,
            Block block
    ) {
        return new TestBlockDef<>(label, relativePos, block, null);
    }
}
