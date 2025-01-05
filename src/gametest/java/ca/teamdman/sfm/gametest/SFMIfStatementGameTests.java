package ca.teamdman.sfm.gametest;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.common.blockentity.ManagerBlockEntity;
import ca.teamdman.sfm.common.item.DiskItem;
import ca.teamdman.sfm.common.localization.LocalizationKeys;
import ca.teamdman.sfm.common.net.ServerboundOutputInspectionRequestPacket;
import ca.teamdman.sfm.common.program.LabelPositionHolder;
import ca.teamdman.sfm.common.program.ProgramContext;
import ca.teamdman.sfm.common.program.linting.GatherWarningsProgramBehaviour;
import ca.teamdman.sfm.common.registry.SFMBlocks;
import ca.teamdman.sfm.common.registry.SFMItems;
import ca.teamdman.sfml.ast.OutputStatement;
import ca.teamdman.sfml.ast.Program;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({
        "RedundantSuppression",
        "DataFlowIssue",
        "deprecation",
        "OptionalGetWithoutIsPresent",
        "DuplicatedCode",
})
@GameTestHolder(SFM.MOD_ID)
public class SFMIfStatementGameTests extends SFMGameTestBase {

    @GameTest(template = "3x2x1")
    public static void comparison_gt(GameTestHelper helper) {
        var leftPos = new BlockPos(2, 2, 0);
        var rightPos = new BlockPos(0, 2, 0);
        var managerPos = new BlockPos(1, 2, 0);
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());
        var left = (Container) helper.getBlockEntity(leftPos);
        var right = (Container) helper.getBlockEntity(rightPos);
        var manager = (ManagerBlockEntity) helper.getBlockEntity(managerPos);
        left.setItem(0, new ItemStack(Items.DIAMOND, 64));
        left.setItem(1, new ItemStack(Items.DIAMOND, 64));
        left.setItem(2, new ItemStack(Items.IRON_INGOT, 12));
        right.setItem(0, new ItemStack(Items.STICK, 13));
        right.setItem(1, new ItemStack(Items.STICK, 64));
        right.setItem(2, new ItemStack(Items.DIRT, 1));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                   NAME "comparison_gt test"
                                   EVERY 20 TICKS DO
                                       IF left HAS GT 100 diamond THEN
                                           -- should happen
                                           INPUT diamond FROM left
                                           OUTPUT diamond TO right
                                       END
                                       IF left HAS GT 300 iron_ingot THEN
                                           -- should not happen
                                           INPUT iron_ingot FROM left
                                           OUTPUT iron_ingot TO right
                                       END
                                       IF right HAS > 10 stick THEN
                                           -- should happen
                                           INPUT stick FROM right
                                           OUTPUT stick TO left
                                       END
                                       if right has > 0 dirt then
                                           -- should happen
                                           input dirt from right
                                           output dirt to left
                                       end
                                   END
                                   """.stripTrailing().stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("left", helper.absolutePos(leftPos))
                .add("right", helper.absolutePos(rightPos))
                .save(manager.getDisk());

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            int leftDiamondCount = count(left, Items.DIAMOND);
            int leftIronCount = count(left, Items.IRON_INGOT);
            int leftStickCount = count(left, Items.STICK);
            int leftDirtCount = count(left, Items.DIRT);
            int rightDiamondCount = count(right, Items.DIAMOND);
            int rightIronCount = count(right, Items.IRON_INGOT);
            int rightStickCount = count(right, Items.STICK);
            int rightDirtCount = count(right, Items.DIRT);
            // the diamonds should have moved from left to right
            assertTrue(leftDiamondCount == 0, "left should have no diamonds");
            assertTrue(rightDiamondCount == 64 * 2, "right should have 100 diamonds");
            // the iron should have stayed in left
            assertTrue(leftIronCount == 12, "left should have 12 iron ingots");
            assertTrue(rightIronCount == 0, "right should have no iron ingots");
            // the sticks should have moved from right to left
            assertTrue(rightStickCount == 0, "right should have no sticks");
            assertTrue(leftStickCount == 77, "left should have 77 sticks");
            // the dirt should have moved from right to left
            assertTrue(rightDirtCount == 0, "right should have no dirt");
            assertTrue(leftDirtCount == 1, "left should have 1 dirt");
        });
    }

    @GameTest(template = "3x2x1")
    public static void comparison_ge(GameTestHelper helper) {
        var leftPos = new BlockPos(2, 2, 0);
        var rightPos = new BlockPos(0, 2, 0);
        var managerPos = new BlockPos(1, 2, 0);
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());
        var left = (Container) helper.getBlockEntity(leftPos);
        var right = (Container) helper.getBlockEntity(rightPos);
        var manager = (ManagerBlockEntity) helper.getBlockEntity(managerPos);
        left.setItem(0, new ItemStack(Items.DIAMOND, 64));
        left.setItem(1, new ItemStack(Items.DIAMOND, 64));
        left.setItem(2, new ItemStack(Items.IRON_INGOT, 12));
        right.setItem(0, new ItemStack(Items.STICK, 13));
        right.setItem(1, new ItemStack(Items.STICK, 64));
        right.setItem(2, new ItemStack(Items.DIRT, 1));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                   NAME "comparison_ge test"
                                   EVERY 20 TICKS DO
                                       IF left HAS GE 129 diamond THEN
                                           -- should not happen
                                           INPUT diamond FROM left
                                           OUTPUT diamond TO right
                                       END
                                       IF left HAS GE 12 iron_ingot THEN
                                           -- should happen
                                           INPUT iron_ingot FROM left
                                           OUTPUT iron_ingot TO right
                                       END
                                       IF right HAS >= 13 stick THEN
                                           -- should happen
                                           INPUT stick FROM right
                                           OUTPUT stick TO left
                                       END
                                       if right has >= 1 dirt then
                                           -- should happen
                                           input dirt from right
                                           output dirt to left
                                       end
                                   END
                                   """.stripTrailing().stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("left", helper.absolutePos(leftPos))
                .add("right", helper.absolutePos(rightPos))
                .save(manager.getDisk());

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            int leftDiamondCount = count(left, Items.DIAMOND);
            int leftIronCount = count(left, Items.IRON_INGOT);
            int leftStickCount = count(left, Items.STICK);
            int leftDirtCount = count(left, Items.DIRT);
            int rightDiamondCount = count(right, Items.DIAMOND);
            int rightIronCount = count(right, Items.IRON_INGOT);
            int rightStickCount = count(right, Items.STICK);
            int rightDirtCount = count(right, Items.DIRT);
            // the diamonds should have moved from left to right
            assertTrue(leftDiamondCount == 64 * 2, "left should have 128 diamonds");
            assertTrue(rightDiamondCount == 0, "right should have no diamonds");
            // the iron should have moved from left to right
            assertTrue(leftIronCount == 0, "left should have no iron ingots");
            assertTrue(rightIronCount == 12, "right should have 12 iron ingots");
            // the sticks should have moved from right to left
            assertTrue(rightStickCount == 0, "right should have no sticks");
            assertTrue(leftStickCount == 77, "left should have 77 sticks");
            // the dirt should have moved from right to left
            assertTrue(rightDirtCount == 0, "right should have no dirt");
            assertTrue(leftDirtCount == 1, "left should have 1 dirt");
        });
    }


    @GameTest(template = "3x2x1")
    public static void comparison_eq(GameTestHelper helper) {
        var leftPos = new BlockPos(2, 2, 0);
        var rightPos = new BlockPos(0, 2, 0);
        var managerPos = new BlockPos(1, 2, 0);
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());
        var left = (Container) helper.getBlockEntity(leftPos);
        var right = (Container) helper.getBlockEntity(rightPos);
        var manager = (ManagerBlockEntity) helper.getBlockEntity(managerPos);
        left.setItem(0, new ItemStack(Items.DIAMOND, 64));
        left.setItem(1, new ItemStack(Items.DIAMOND, 64));
        left.setItem(2, new ItemStack(Items.IRON_INGOT, 12));
        right.setItem(0, new ItemStack(Items.STICK, 13));
        right.setItem(1, new ItemStack(Items.STICK, 64));
        right.setItem(2, new ItemStack(Items.DIRT, 1));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                   NAME "comparison_eq test"
                                   EVERY 20 TICKS DO
                                       IF left HAS eq 129 diamond THEN
                                           -- should not happen
                                           INPUT diamond FROM left
                                           OUTPUT diamond TO right
                                       END
                                       IF left HAS = 12 iron_ingot THEN
                                           -- should happen
                                           INPUT iron_ingot FROM left
                                           OUTPUT iron_ingot TO right
                                       END
                                       IF right HAS eq 77 stick THEN
                                           -- should happen
                                           INPUT stick FROM right
                                           OUTPUT stick TO left
                                       END
                                       if right has = 1 dirt then
                                           -- should happen
                                           input dirt from right
                                           output dirt to left
                                       end
                                   END
                                   """.stripTrailing().stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("left", helper.absolutePos(leftPos))
                .add("right", helper.absolutePos(rightPos))
                .save(manager.getDisk());

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            int leftDiamondCount = count(left, Items.DIAMOND);
            int leftIronCount = count(left, Items.IRON_INGOT);
            int leftStickCount = count(left, Items.STICK);
            int leftDirtCount = count(left, Items.DIRT);
            int rightDiamondCount = count(right, Items.DIAMOND);
            int rightIronCount = count(right, Items.IRON_INGOT);
            int rightStickCount = count(right, Items.STICK);
            int rightDirtCount = count(right, Items.DIRT);
            // the diamonds should have moved from left to right
            assertTrue(leftDiamondCount == 64 * 2, "left should have 128 diamonds");
            assertTrue(rightDiamondCount == 0, "right should have no diamonds");
            // the iron should have moved from left to right
            assertTrue(leftIronCount == 0, "left should have no iron ingots");
            assertTrue(rightIronCount == 12, "right should have 12 iron ingots");
            // the sticks should have moved from right to left
            assertTrue(rightStickCount == 0, "right should have no sticks");
            assertTrue(leftStickCount == 77, "left should have 77 sticks");
            // the dirt should have moved from right to left
            assertTrue(rightDirtCount == 0, "right should have no dirt");
            assertTrue(leftDirtCount == 1, "left should have 1 dirt");
        });
    }


    @GameTest(template = "3x2x1")
    public static void comparison_lt(GameTestHelper helper) {
        var leftPos = new BlockPos(2, 2, 0);
        var rightPos = new BlockPos(0, 2, 0);
        var managerPos = new BlockPos(1, 2, 0);
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());
        var left = (Container) helper.getBlockEntity(leftPos);
        var right = (Container) helper.getBlockEntity(rightPos);
        var manager = (ManagerBlockEntity) helper.getBlockEntity(managerPos);
        left.setItem(0, new ItemStack(Items.DIAMOND, 64));
        left.setItem(1, new ItemStack(Items.DIAMOND, 64));
        left.setItem(2, new ItemStack(Items.IRON_INGOT, 12));
        right.setItem(0, new ItemStack(Items.STICK, 13));
        right.setItem(1, new ItemStack(Items.STICK, 64));
        right.setItem(2, new ItemStack(Items.DIRT, 1));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                   NAME "comparison_lt test"
                                   EVERY 20 TICKS DO
                                       IF left HAS lt 10 diamond THEN
                                           -- should not happen
                                           INPUT diamond FROM left
                                           OUTPUT diamond TO right
                                       END
                                       IF left HAS < 200 iron_ingot THEN
                                           -- should happen
                                           INPUT iron_ingot FROM left
                                           OUTPUT iron_ingot TO right
                                       END
                                       IF right HAS < 78 stick THEN
                                           -- should happen
                                           INPUT stick FROM right
                                           OUTPUT stick TO left
                                       END
                                       if right has < 3 dirt then
                                           -- should happen
                                           input dirt from right
                                           output dirt to left
                                       end
                                   END
                                   """.stripTrailing().stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("left", helper.absolutePos(leftPos))
                .add("right", helper.absolutePos(rightPos))
                .save(manager.getDisk());

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            int leftDiamondCount = count(left, Items.DIAMOND);
            int leftIronCount = count(left, Items.IRON_INGOT);
            int leftStickCount = count(left, Items.STICK);
            int leftDirtCount = count(left, Items.DIRT);
            int rightDiamondCount = count(right, Items.DIAMOND);
            int rightIronCount = count(right, Items.IRON_INGOT);
            int rightStickCount = count(right, Items.STICK);
            int rightDirtCount = count(right, Items.DIRT);
            // the diamonds should have moved from left to right
            assertTrue(leftDiamondCount == 64 * 2, "left should have 128 diamonds");
            assertTrue(rightDiamondCount == 0, "right should have no diamonds");
            // the iron should have moved from left to right
            assertTrue(leftIronCount == 0, "left should have no iron ingots");
            assertTrue(rightIronCount == 12, "right should have 12 iron ingots");
            // the sticks should have moved from right to left
            assertTrue(rightStickCount == 0, "right should have no sticks");
            assertTrue(leftStickCount == 77, "left should have 77 sticks");
            // the dirt should have moved from right to left
            assertTrue(rightDirtCount == 0, "right should have no dirt");
            assertTrue(leftDirtCount == 1, "left should have 1 dirt");
        });
    }


    @GameTest(template = "3x2x1")
    public static void comparison_le(GameTestHelper helper) {
        var leftPos = new BlockPos(2, 2, 0);
        var rightPos = new BlockPos(0, 2, 0);
        var managerPos = new BlockPos(1, 2, 0);
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());
        var left = (Container) helper.getBlockEntity(leftPos);
        var right = (Container) helper.getBlockEntity(rightPos);
        var manager = (ManagerBlockEntity) helper.getBlockEntity(managerPos);
        left.setItem(0, new ItemStack(Items.DIAMOND, 64));
        left.setItem(1, new ItemStack(Items.DIAMOND, 64));
        left.setItem(2, new ItemStack(Items.IRON_INGOT, 12));
        right.setItem(0, new ItemStack(Items.STICK, 13));
        right.setItem(1, new ItemStack(Items.STICK, 64));
        right.setItem(2, new ItemStack(Items.DIRT, 1));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                   NAME "comparison_le test"
                                   EVERY 20 TICKS DO
                                       IF left HAS le 10 diamond THEN
                                           -- should not happen
                                           INPUT diamond FROM left
                                           OUTPUT diamond TO right
                                       END
                                       IF left HAS <= 12 iron_ingot THEN
                                           -- should happen
                                           INPUT iron_ingot FROM left
                                           OUTPUT iron_ingot TO right
                                       END
                                       IF right HAS le 77 stick THEN
                                           -- should happen
                                           INPUT stick FROM right
                                           OUTPUT stick TO left
                                       END
                                       if right has <= 1 dirt then
                                           -- should happen
                                           input dirt from right
                                           output dirt to left
                                       end
                                   END
                                   """.stripTrailing().stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("left", helper.absolutePos(leftPos))
                .add("right", helper.absolutePos(rightPos))
                .save(manager.getDisk());

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            int leftDiamondCount = count(left, Items.DIAMOND);
            int leftIronCount = count(left, Items.IRON_INGOT);
            int leftStickCount = count(left, Items.STICK);
            int leftDirtCount = count(left, Items.DIRT);
            int rightDiamondCount = count(right, Items.DIAMOND);
            int rightIronCount = count(right, Items.IRON_INGOT);
            int rightStickCount = count(right, Items.STICK);
            int rightDirtCount = count(right, Items.DIRT);
            // the diamonds should have moved from left to right
            assertTrue(leftDiamondCount == 64 * 2, "left should have 128 diamonds");
            assertTrue(rightDiamondCount == 0, "right should have no diamonds");
            // the iron should have moved from left to right
            assertTrue(leftIronCount == 0, "left should have no iron ingots");
            assertTrue(rightIronCount == 12, "right should have 12 iron ingots");
            // the sticks should have moved from right to left
            assertTrue(rightStickCount == 0, "right should have no sticks");
            assertTrue(leftStickCount == 77, "left should have 77 sticks");
            // the dirt should have moved from right to left
            assertTrue(rightDirtCount == 0, "right should have no dirt");
            assertTrue(leftDirtCount == 1, "left should have 1 dirt");
        });
    }

    @GameTest(template = "3x2x1")
    public static void forget_1(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        BlockPos rightPos = new BlockPos(0, 2, 0);
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        BlockPos leftPos = new BlockPos(2, 2, 0);
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());

        var rightChest = getItemHandler(helper, rightPos);
        var leftChest = getItemHandler(helper, leftPos);

        leftChest.insertItem(0, new ItemStack(Blocks.DIRT, 64), false);

        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           INPUT FROM a
                                           IF a has gt 0 dirt THEN
                                               FORGET a
                                           END
                                           OUTPUT TO b -- nothing happens :D
                                       END
                                   """.stripTrailing().stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .add("b", helper.absolutePos(rightPos))
                .save(manager.getDisk());

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            assertTrue(leftChest.getStackInSlot(0).getCount() == 64, "Dirt should not depart");
            assertTrue(rightChest.getStackInSlot(0).isEmpty(), "Dirt should not arrive");

        });
    }


    @GameTest(template = "3x2x1")
    public static void forget_2(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        BlockPos rightPos = new BlockPos(0, 2, 0);
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        BlockPos leftPos = new BlockPos(2, 2, 0);
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());

        var rightChest = getItemHandler(helper, rightPos);
        var leftChest = getItemHandler(helper, leftPos);

        leftChest.insertItem(0, new ItemStack(Blocks.DIRT, 64), false);

        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           INPUT FROM a,b,c
                                           IF a has gt 0 dirt THEN
                                               FORGET
                                           END
                                           OUTPUT TO dest -- nothing happens :D
                                       END
                                   """.stripTrailing().stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .add("b", helper.absolutePos(leftPos))
                .add("c", helper.absolutePos(leftPos))
                .add("dest", helper.absolutePos(rightPos))
                .save(manager.getDisk());

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            assertTrue(leftChest.getStackInSlot(0).getCount() == 64, "Dirt should not depart");
            assertTrue(rightChest.getStackInSlot(0).isEmpty(), "Dirt should not arrive");

        });
    }


    @GameTest(template = "3x2x1")
    public static void forget_slot(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        BlockPos rightPos = new BlockPos(0, 2, 0);
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        BlockPos leftPos = new BlockPos(2, 2, 0);
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());

        var rightChest = getItemHandler(helper, rightPos);
        var leftChest = getItemHandler(helper, leftPos);

        leftChest.insertItem(0, new ItemStack(Blocks.DIRT, 64), false);
        leftChest.insertItem(1, new ItemStack(Items.IRON_INGOT, 64), false);

        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           INPUT FROM a SLOTS 1-26 -- iron ingot
                                           INPUT FROM c SLOTS 0    -- dirt blocks
                                           IF a SLOTS 0 has gt 0 dirt THEN
                                               FORGET a -- forgets the iron ingot
                                           END
                                           OUTPUT TO b -- will move the dirt block, the next tick the ingots
                                       END
                                   """.stripTrailing().stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .add("c", helper.absolutePos(leftPos))
                .add("b", helper.absolutePos(rightPos))
                .save(manager.getDisk());

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            assertTrue(leftChest.getStackInSlot(0).isEmpty(), "Dirt should depart");
            assertTrue(leftChest.getStackInSlot(1).getCount() == 64, "Iron ingots should not depart");
            assertTrue(rightChest.getStackInSlot(0).getCount() == 64, "Dirt should arrive in size");
            assertTrue(rightChest.getStackInSlot(0).getItem() == Items.DIRT, "Dirt should arrive in type");

        });
    }

    @GameTest(template = "3x2x1", batch = "linting")
    public static void count_execution_paths_conditional_1(GameTestHelper helper) {
        // place inventories
        helper.setBlock(new BlockPos(1, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        BlockPos rightPos = new BlockPos(0, 2, 0);
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        BlockPos leftPos = new BlockPos(2, 2, 0);
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());

        // place manager
        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));

        // set the labels
        LabelPositionHolder labelPositionHolder = LabelPositionHolder.empty()
                .add("left", helper.absolutePos(leftPos))
                .add("right", helper.absolutePos(rightPos))
                .save(manager.getDisk());

        // load the program
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           IF left HAS gt 0 stone THEN
                                               INPUT FROM left
                                           END
                                           OUTPUT TO right
                                       END
                                   """.stripTrailing().stripIndent());
        assertManagerRunning(manager);
        var program = manager.getProgram();

        // ensure no warnings
        var warnings = DiskItem.getWarnings(manager.getDisk());
        assertTrue(warnings.isEmpty(), "expected 0 warning, got " + warnings.size());

        // count the execution paths
        GatherWarningsProgramBehaviour simulation = new GatherWarningsProgramBehaviour(warnings::addAll);
        program.tick(ProgramContext.createSimulationContext(
                program,
                labelPositionHolder,
                0,
                simulation
        ));

        List<Integer> expectedPathSizes = new ArrayList<>(List.of(1, 2));
        assertTrue(
                simulation.getSeenPaths().size() == expectedPathSizes.size(),
                "expected " + expectedPathSizes.size() + " execution paths, got " + simulation.getSeenPaths().size()
        );
        int[] actualPathIOSizes = simulation.getSeenIOStatementCountForEachPath();
        // don't assume the order, just that each path size has occurred the specified number of times
        for (int i = 0; i < actualPathIOSizes.length; i++) {
            int pathSize = actualPathIOSizes[i];
            if (!expectedPathSizes.remove((Integer) pathSize)) {
                helper.fail("unexpected path size " + pathSize + " at index " + i + " of " + simulation
                        .getSeenPaths()
                        .size() + " paths");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "3x2x1", batch = "linting")
    public static void count_execution_paths_conditional_1b(GameTestHelper helper) {
        // place inventories
        helper.setBlock(new BlockPos(1, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        BlockPos rightPos = new BlockPos(0, 2, 0);
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        BlockPos leftPos = new BlockPos(2, 2, 0);
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());

        // place manager
        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));

        // set the labels
        LabelPositionHolder.empty()
                .add("left", helper.absolutePos(leftPos))
                .save(manager.getDisk());

        // load the program
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           IF left HAS gt 0 stone THEN
                                               INPUT FROM left
                                           END
                                       END
                                   """.stripTrailing().stripIndent());
        assertManagerRunning(manager);

        // assert expected warnings
        var warnings = DiskItem.getWarnings(manager.getDisk());
        assertTrue(warnings.size() == 1, "expected 1 warning, got " + warnings.size());
        assertTrue(warnings
                           .get(0)
                           .getKey()
                           .equals(LocalizationKeys.PROGRAM_WARNING_UNUSED_INPUT_LABEL // should be unused input
                                           .key()
                                           .get()), "expected output without matching input warning");
        helper.succeed();
    }

    @GameTest(template = "3x2x1", batch = "linting")
    public static void count_execution_paths_conditional_2(GameTestHelper helper) {
        // place inventories
        helper.setBlock(new BlockPos(1, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        BlockPos rightPos = new BlockPos(0, 2, 0);
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        BlockPos leftPos = new BlockPos(2, 2, 0);
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());

        // place manager
        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));

        // set the labels
        LabelPositionHolder labelPositionHolder = LabelPositionHolder.empty()
                .add("left1", helper.absolutePos(leftPos))
                .add("left2", helper.absolutePos(leftPos))
                .add("right", helper.absolutePos(rightPos))
                .save(manager.getDisk());

        // load the program
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           IF left2 HAS gt 0 stone THEN
                                               INPUT FROM left1
                                           END
                                           IF left1 HAS gt 0 stone THEN
                                               INPUT FROM left2
                                           END
                                           OUTPUT TO right
                                       END
                                   """.stripTrailing().stripIndent());
        assertManagerRunning(manager);
        var program = manager.getProgram();

        // ensure no warnings
        var warnings = DiskItem.getWarnings(manager.getDisk());
        assertTrue(warnings.isEmpty(), "expected 0 warning, got " + warnings.size());

        // count the execution paths
        GatherWarningsProgramBehaviour simulation = new GatherWarningsProgramBehaviour(warnings::addAll);
        program.tick(ProgramContext.createSimulationContext(
                program,
                labelPositionHolder,
                0,
                simulation
        ));
        List<Integer> expectedPathSizes = new ArrayList<>(List.of(1, 2, 2, 3));
        assertTrue(
                simulation.getSeenPaths().size() == expectedPathSizes.size(),
                "expected " + expectedPathSizes.size() + " execution paths, got " + simulation.getSeenPaths().size()
        );
        int[] actualPathIOSizes = simulation.getSeenIOStatementCountForEachPath();
        // don't assume the order, just that each path size has occurred the specified number of times
        for (int i = 0; i < actualPathIOSizes.length; i++) {
            int pathSize = actualPathIOSizes[i];
            if (!expectedPathSizes.remove((Integer) pathSize)) {
                helper.fail("unexpected path size " + pathSize + " at index " + i + " of " + simulation
                        .getSeenPaths()
                        .size() + " paths");
            }
        }
        helper.succeed();
    }


    @GameTest(template = "3x2x1", batch = "linting")
    public static void conditional_output_inspection(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        BlockPos rightPos = new BlockPos(0, 2, 0);
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        BlockPos leftPos = new BlockPos(2, 2, 0);
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());

        var rightChest = getItemHandler(helper, rightPos);
        var leftChest = getItemHandler(helper, leftPos);

        leftChest.insertItem(0, new ItemStack(Blocks.DIRT, 64), false);

        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));


        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .add("b", helper.absolutePos(rightPos))
                .save(manager.getDisk());

        // set the program
        String code = """
                    EVERY 20 TICKS DO
                        IF a HAS = 64 dirt THEN
                            INPUT RETAIN 32 FROM a
                        END
                        OUTPUT TO b
                    END
                """.stripTrailing().stripIndent();
        manager.setProgram(code);
        assertManagerRunning(manager);

        // compile new program for inspection
        Program program = compile(code);


        OutputStatement outputStatement = (OutputStatement) program
                .triggers()
                .get(0)
                .getBlock()
                .getStatements()
                .get(1);

        String inspectionResults = ServerboundOutputInspectionRequestPacket.getOutputStatementInspectionResultsString(
                manager,
                program,
                outputStatement
        );

        //noinspection TrailingWhitespacesInTextBlock
        String expected = """
                OUTPUT TO b
                -- predictions may differ from actual execution results
                -- POSSIBILITY 0 -- all false
                OVERALL a HAS = 64 dirt -- false
                
                    -- predicted inputs:
                    none
                    -- predicted outputs:
                    none
                -- POSSIBILITY 1 -- all true
                OVERALL a HAS = 64 dirt -- true
                
                    -- predicted inputs:
                    INPUT 32 minecraft:dirt FROM a SLOTS 0
                    -- predicted outputs:
                    OUTPUT 32 minecraft:dirt TO b
                """.stripLeading().stripIndent().stripTrailing();
        if (!inspectionResults.equals(expected)) {
            System.out.println("Received results:");
            System.out.println(inspectionResults);
            System.out.println("Expected:");
            System.out.println(expected);

            // get the position of the difference and show it
            for (int i = 0; i < inspectionResults.length(); i++) {
                if (inspectionResults.charAt(i) != expected.charAt(i)) {
                    System.out.println("Difference at position "
                                       + i
                                       + ":"
                                       + inspectionResults.charAt(i)
                                       + " vs "
                                       + expected.charAt(i));
                    break;
                }
            }

            helper.fail("inspection didn't match results");
        }

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            assertTrue(leftChest.getStackInSlot(0).getCount() == 32, "Dirt did not depart");
            assertTrue(rightChest.getStackInSlot(0).getCount() == 32, "Dirt did not arrive");
        });
    }


    @GameTest(template = "3x4x3")
    public static void move_if_powered(GameTestHelper helper) {
        BlockPos managerPos = new BlockPos(1, 2, 1);
        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());
        BlockPos leftPos = managerPos.east();
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        BlockPos rightPos = managerPos.west();
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        BlockPos topPos = managerPos.above();
        helper.setBlock(topPos, SFMBlocks.TEST_BARREL_BLOCK.get());

        BlockPos leverPos = managerPos.north();
        helper.setBlock(leverPos, Blocks.LEVER);
        helper.pullLever(leverPos);

        var rightChest = getItemHandler(helper, rightPos);
        var leftChest = getItemHandler(helper, leftPos);

        leftChest.insertItem(0, new ItemStack(Items.DIRT, 64), false);
        leftChest.insertItem(1, new ItemStack(Items.DIRT, 64), false);
        leftChest.insertItem(2, new ItemStack(Items.STONE, 64), false);
        leftChest.insertItem(3, new ItemStack(Items.IRON_INGOT, 64), false);
        leftChest.insertItem(4, new ItemStack(Items.GOLD_INGOT, 64), false);
        leftChest.insertItem(5, new ItemStack(Items.GOLD_NUGGET, 64), false);

        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(managerPos);
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           IF redstone GT 0 THEN
                                                INPUT FROM left
                                                OUTPUT TO right
                                           END
                                       END
                                   """.stripTrailing().stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("left", helper.absolutePos(leftPos))
                .add("right", helper.absolutePos(rightPos))
                .save(manager.getDisk());

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            assertTrue(count(leftChest, null) == 0, "everything should depart");
            assertTrue(count(rightChest, Items.GOLD_NUGGET) == 64, "gold nuggets should arrive");
            assertTrue(count(rightChest, Items.IRON_INGOT) == 64, "iron ingots should arrive");
            assertTrue(count(rightChest, Items.GOLD_INGOT) == 64, "gold ingots should arrive");
            assertTrue(count(rightChest, Items.DIRT) == 64 * 2, "dirt should arrive");
            assertTrue(count(rightChest, Items.STONE) == 64, "stone should arrive");
        });
    }

    @GameTest(template = "3x2x1")
    public static void has_or(GameTestHelper helper) {
        var leftPos = new BlockPos(2, 2, 0);
        var rightPos = new BlockPos(0, 2, 0);
        var managerPos = new BlockPos(1, 2, 0);
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());
        var left = (Container) helper.getBlockEntity(leftPos);
        var right = (Container) helper.getBlockEntity(rightPos);
        var manager = (ManagerBlockEntity) helper.getBlockEntity(managerPos);
        left.setItem(0, new ItemStack(Items.DIAMOND, 64));
        left.setItem(1, new ItemStack(Items.DIAMOND, 64));
        left.setItem(2, new ItemStack(Items.IRON_INGOT, 12));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                   NAME "has_or test"
                                   EVERY 20 TICKS DO
                                       IF left HAS LT 150 diamond OR iron_ingot THEN
                                            INPUT FROM left
                                            OUTPUT TO right
                                       END
                                   END
                                   """.stripTrailing().stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("left", helper.absolutePos(leftPos))
                .add("right", helper.absolutePos(rightPos))
                .save(manager.getDisk());

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            // left should be empty
            assertTrue(count(left, Items.DIAMOND) == 0, "left should have no diamonds");
            assertTrue(count(left, Items.IRON_INGOT) == 0, "left should have no iron ingots");
            // right should have all the items
            assertTrue(count(right, Items.DIAMOND) == 64 * 2, "right should have 128 diamonds");
            assertTrue(count(right, Items.IRON_INGOT) == 12, "right should have 12 iron ingots");
        });
    }
}
