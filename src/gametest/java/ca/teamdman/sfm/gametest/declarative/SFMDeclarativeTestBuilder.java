package ca.teamdman.sfm.gametest.declarative;

import ca.teamdman.sfm.common.blockentity.ManagerBlockEntity;
import ca.teamdman.sfm.common.program.LabelPositionHolder;
import ca.teamdman.sfm.common.registry.SFMItems;
import ca.teamdman.sfm.gametest.SFMGameTestBase;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class SFMDeclarativeTestBuilder extends SFMGameTestBase {
    private final GameTestHelper helper;
    private final SFMTestSpec spec;
    private @Nullable ManagerBlockEntity manager;

    public SFMDeclarativeTestBuilder(GameTestHelper helper, SFMTestSpec spec) {
        this.helper = helper;
        this.spec = spec;
    }

    public void run() {
        // 1) place blocks
        placeBlocks();

        // 2) place manager + disk if there's a program
        setupManager();

        // 3) run preconditions (if any)
        runPreconditions();

        // 4) label the blocks for the program
        labelBlocks();

        // 5) run the actual manager logic (including chaos test)
        runManagerTest();
    }

    private void placeBlocks() {
        for (TestBlockDef<?> def : spec.blocks()) {
            placeBlock(def);
        }
    }

    private <T extends BlockEntity> void placeBlock(TestBlockDef<T> def) {
        BlockPos worldPos = def.relativePos().offset(spec.offset());
        helper.setBlock(worldPos, def.block());
        // If the block has a block entity config
        if (def.blockEntityConfigurer() != null) {
            BlockEntity be = helper.getBlockEntity(worldPos);
            //noinspection unchecked
            Objects.requireNonNull(def.blockEntityConfigurer()).accept((T) be);
        }
    }

    private void setupManager() {
        // By default, maybe you always place the manager at (1,2,0)? Or from the spec offset + whatever
        // For demonstration, let's just do the same approach as your old code:
        BlockPos managerPos = new BlockPos(1, 2, 0).offset(spec.offset());
        helper.setBlock(managerPos, ca.teamdman.sfm.common.registry.SFMBlocks.MANAGER_BLOCK.get());
        this.manager = (ManagerBlockEntity) helper.getBlockEntity(managerPos);

        // If we have a program, set the disk’s program
        if (manager != null && spec.program() != null) {
            manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
            manager.setProgram(spec.program());
        }
    }

    private void runPreconditions() {
        for (String line : spec.preconditions()) {
            // parse and evaluate that line, e.g. “a BOTTOM SIDE HAS EQ 1000 fe::"
            // see "compiling a DSL" below for a rough idea
            parseDslAndEvaluate(line);
        }
    }

    private void runManagerTest() {
        // Reuse your existing “run” method from SFMTestBuilder logic
        // or replicate its chaos-shuffle approach
        // e.g.:
        if (manager == null) {
            helper.fail("No manager found!");
            return;
        }

        // Save label info to the manager’s disk
        LabelPositionHolder labelHolder = LabelPositionHolder.empty();
        for (TestBlockDef<?> def : spec.blocks()) {
            // Actually add the label to the holder
            BlockPos absolutePos = helper.absolutePos(def.relativePos().offset(spec.offset()));
            labelHolder.add(def.label(), absolutePos);
        }
        labelHolder.save(Objects.requireNonNull(manager.getDisk()));

        // Then do your “assertManagerDidThingWithoutLagging” or “succeedIfManagerDidThingWithoutLagging”
        // plus the chaos item move if you want

        // For demonstration, skip the chaos logic here or only do it if wanted:
        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            // 6) run postconditions
            runPostconditions();
        });
    }

    private void runPostconditions() {
        for (String line : spec.postconditions()) {
            parseDslAndEvaluate(line);
        }
    }

    private void parseDslAndEvaluate(String line) {
        // This is where you’d parse lines like:
        //   “a BOTTOM SIDE HAS EQ 1000 fe::”
        // Or something like: “b BOTTOM SIDE HAS EQ 0 fe::”

        // In the simplest approach:
        //   1) tokenize the line
        //   2) interpret the bits (label, side, expression, resource type, etc.)
        //   3) run an assertion in the game world

        // Because this can get elaborate, you might want to create a separate DSL parser class.
        // For now, we just do something super naive:
        if (line.contains("HAS EQ")) {
            // ...
        }
        // etc.
    }

    private void labelBlocks() {
        // Optionally do it here if you want labeling before or after preconditions
    }
}
