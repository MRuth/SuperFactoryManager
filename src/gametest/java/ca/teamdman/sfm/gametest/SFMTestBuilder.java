package ca.teamdman.sfm.gametest;

import ca.teamdman.sfm.common.blockentity.ManagerBlockEntity;
import ca.teamdman.sfm.common.program.LabelPositionHolder;
import ca.teamdman.sfm.common.registry.SFMBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class SFMTestBuilder extends SFMGameTestBase {
    protected final GameTestHelper helper;
    protected @Nullable ManagerBlockEntity manager;
    protected Map<String, IItemHandler> chests = new HashMap<>();
    protected Map<String, BlockPos> positions = new HashMap<>();
    protected LabelPositionHolder labelHolder = LabelPositionHolder.empty();
    protected @Nullable String program;
    protected List<Runnable> assertions = new ArrayList<>();

    public SFMTestBuilder(GameTestHelper helper) {
        this.helper = helper;
    }

    public SFMTestBuilder setProgram(String program) {
        // need to strip to ensure no indent despite java multiline string usage
        this.program = program.stripTrailing().stripIndent();
        return this;
    }

    public SFMTestBuilder fillChest(String name, int slot, ItemStack stack) {
        IItemHandler chest = chests.get(name);
        if (chest == null) {
            throw new IllegalArgumentException("Chest not found: " + name);
        }
        chest.insertItem(slot, stack, false);
        return this;
    }

    public SFMTestBuilder fillChest(String name, List<ItemStack> stacks) {
        IItemHandler chest = chests.get(name);
        if (chest == null) {
            throw new IllegalArgumentException("Chest not found: " + name);
        }
        for (int i = 0; i < stacks.size(); i++) {
            chest.insertItem(i, stacks.get(i), false);
        }
        return this;
    }

    public SFMTestBuilder assertContains(String name, @Nullable Item item, int expectedCount) {
        assertions.add(() -> {
            IItemHandler chest = chests.get(name);
            if (chest == null) {
                throw new IllegalArgumentException("Chest not found: " + name);
            }
            int actualCount = count(chest, item);
            assertTrue(
                    actualCount == expectedCount,
                    String.format("Expected %d of %s in chest %s, but found %d",
                                  expectedCount, item == null ? "any item" : item.getDescriptionId(), name, actualCount)
            );
        });
        return this;
    }

    public void run() {
        setupManager();
        assert manager != null;
        labelHolder.save(Objects.requireNonNull(manager.getDisk()));
        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            for (Runnable assertion : assertions) {
                assertion.run();
            }
        });
    }

    protected abstract void setupManager();

    protected void addChest(String name, BlockPos pos) {
        helper.setBlock(pos, SFMBlocks.TEST_BARREL_BLOCK.get());
        IItemHandler chest = getItemHandler(helper, pos);
        chests.put(name, chest);
        positions.put(name, pos);
        labelHolder.add(name, helper.absolutePos(pos));
    }
}
