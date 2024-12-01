package ca.teamdman.sfm.gametest;

import ca.teamdman.sfm.common.blockentity.ManagerBlockEntity;
import ca.teamdman.sfm.common.program.LabelPositionHolder;
import ca.teamdman.sfm.common.registry.SFMBlocks;
import ca.teamdman.sfm.common.registry.SFMItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class SFMTestBuilder extends SFMGameTestBase {
    protected final GameTestHelper helper;
    protected @Nullable ManagerBlockEntity manager;
    protected Map<String, IItemHandler> chests = new HashMap<>();
    protected Map<String, BlockPos> positions = new HashMap<>();
    protected LabelPositionHolder labelHolder = LabelPositionHolder.empty();
    protected @Nullable String program;
    protected List<Runnable> postConditions = new ArrayList<>();
    protected List<Runnable> preConditions = new ArrayList<>();

    public SFMTestBuilder(GameTestHelper helper) {
        this.helper = helper;
    }

    public SFMTestBuilder setProgram(String program) {
        // need to strip to ensure no indent despite java multiline string usage
        this.program = program.stripTrailing().stripIndent();
        return this;
    }

    public SFMTestBuilder preContents(
            String name,
            List<ItemStack> stacks
    ) {
        preConditions.add(() -> {
            IItemHandler chest = chests.get(name);
            if (chest == null) {
                throw new IllegalArgumentException("Chest not found: " + name);
            }
            for (int i = 0; i < stacks.size(); i++) {
                chest.insertItem(i, stacks.get(i), false);
            }
        });
        return this;
    }

    public SFMTestBuilder postContents(
            String name,
            List<ItemStack> expected
    ) {
        postConditions.add(() -> {
            IItemHandler chest = chests.get(name);
            if (chest == null) {
                throw new IllegalArgumentException("Chest not found: " + name);
            }
            for (int i = 0; i < chest.getSlots(); i++) {
                ItemStack expectedStack = i < expected.size() ? expected.get(i) : ItemStack.EMPTY;
                ItemStack actualStack = chest.getStackInSlot(i);
                assertTrue(
                        expectedStack.isEmpty() && actualStack.isEmpty() || ItemStack.isSame(
                                expectedStack,
                                actualStack
                        ),
                        String.format("Expected %s in chest %s slot %d, but found %s",
                                      expectedStack, name, i, actualStack
                        )
                );
            }
        });
        return this;
    }

    public void run() {
        setupStructure(BlockPos.ZERO);
        for (Runnable preCondition : preConditions) {
            preCondition.run();
        }
        assert manager != null;
        labelHolder.save(Objects.requireNonNull(manager.getDisk()));
        assertManagerDidThingWithoutLagging(
                helper,
                manager,
                () -> {
                    // first, assertions as normal
                    {
                        for (Runnable assertion : postConditions) {
                            assertion.run();
                        }
                    }

                    // second, move an item and ensure that the assertions fail somewhere
                    {
                        // shuffle the candidate chests
                        ArrayList<Map.Entry<String, IItemHandler>> chests = new ArrayList<>(this.chests.entrySet());
                        Collections.shuffle(chests);

                        // find a chest with something we can take
                        Map.Entry<String, IItemHandler> source = null;
                        for (int i = 0; i < chests.size(); i++) {
                            IItemHandler chest = chests.get(i).getValue();
                            if (count(chest, null) > 0) {
                                source = chests.remove(i);
                                break;
                            }
                        }
                        assertTrue(
                                source != null,
                                "Chaos failed to find an item to move?? What is this test doing that there's no items??"
                        );

                        // find a chest to move it to
                        Map.Entry<String, IItemHandler> dest = chests.remove(0);

                        // perform the chaos
                        ItemStack taken = ItemStack.EMPTY;
                        int takenSlot = -1;
                        for (int slot = 0; slot < source.getValue().getSlots(); slot++) {
                            taken = source.getValue().extractItem(slot, 1, false);
                            if (!taken.isEmpty()) {
                                takenSlot = slot;
                                ItemStack remainder = ItemHandlerHelper.insertItem(dest.getValue(), taken, false);
                                assertTrue(
                                        remainder.isEmpty(),
                                        "Chaos failed to insert the taken item, took "
                                        + taken
                                        + " from "
                                        + source.getKey()
                                        + " slot "
                                        + takenSlot
                                        + " to "
                                        + dest.getKey()
                                );
                                break;
                            }
                        }
                        assertTrue(
                                !taken.isEmpty(),
                                "Chaos failed to take an item from "
                                + source.getKey()
                                + " slot "
                                + takenSlot
                        );

                        // assert that the assertions fail
                        boolean tripped = false;
                        for (Runnable assertion : postConditions) {
                            try {
                                assertion.run();
                            } catch (GameTestAssertException e) {
                                tripped = true;
                                break;
                            }
                        }
                        assertTrue(
                                tripped,
                                "Assertions did not fail after chaos, moved "
                                + taken
                                + " from "
                                + source.getKey()
                                + " slot "
                                + takenSlot
                                + " to "
                                + dest.getKey()
                        );
                    }
                },
                helper::succeed
        );
    }

    protected void addChest(
            String name,
            BlockPos pos
    ) {
        helper.setBlock(pos, SFMBlocks.TEST_BARREL_BLOCK.get());
        IItemHandler chest = getItemHandler(helper, pos);
        chests.put(name, chest);
        positions.put(name, pos);
        labelHolder.add(name, helper.absolutePos(pos));
    }

    protected abstract void setupStructure(BlockPos offset);

    protected void setupManager(BlockPos offset) {
        BlockPos managerPos = new BlockPos(1, 2, 0).offset(offset);
        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());
        manager = (ManagerBlockEntity) helper.getBlockEntity(managerPos);
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram(program.stripTrailing().stripIndent());
    }
}
