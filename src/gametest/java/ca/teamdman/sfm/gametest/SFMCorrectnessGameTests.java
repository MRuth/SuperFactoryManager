package ca.teamdman.sfm.gametest;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.common.blockentity.ManagerBlockEntity;
import ca.teamdman.sfm.common.blockentity.PrintingPressBlockEntity;
import ca.teamdman.sfm.common.cablenetwork.CableNetwork;
import ca.teamdman.sfm.common.cablenetwork.CableNetworkManager;
import ca.teamdman.sfm.common.item.DiskItem;
import ca.teamdman.sfm.common.item.FormItem;
import ca.teamdman.sfm.common.localization.LocalizationKeys;
import ca.teamdman.sfm.common.program.GatherWarningsProgramBehaviour;
import ca.teamdman.sfm.common.program.LabelPositionHolder;
import ca.teamdman.sfm.common.program.ProgramContext;
import ca.teamdman.sfm.common.registry.SFMBlocks;
import ca.teamdman.sfm.common.registry.SFMItems;
import ca.teamdman.sfm.common.util.SFMDirections;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.apache.logging.log4j.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

// https://github.dev/CompactMods/CompactMachines
// https://github.com/SocketMods/BaseDefense/blob/3b3cb4af26f4553c3438417cbb95f0d3fb707751/build.gradle#L74
// https://github.com/sinkillerj/ProjectE/blob/mc1.16.x/build.gradle#L54
// https://github.com/mekanism/Mekanism/blob/1.16.x/build.gradle
// https://github.com/TwistedGate/ImmersivePetroleum/blob/1.16.5/build.gradle#L107
// https://github.com/MinecraftForge/MinecraftForge/blob/d7b137d1446377bfd1958f8a0e24f63819b81bfc/src/test/java/net/minecraftforge/debug/misc/GameTestTest.java#L155
// https://docs.minecraftforge.net/en/1.19.x/misc/gametest/
// https://github.com/MinecraftForge/MinecraftForge/blob/1.19.x/src/test/java/net/minecraftforge/debug/misc/GameTestTest.java#LL101-L116C6
// https://github.com/XFactHD/FramedBlocks/blob/1.19.4/src/main/java/xfacthd/framedblocks/api/test/TestUtils.java#L65-L87
@SuppressWarnings({"DataFlowIssue", "deprecation", "OptionalGetWithoutIsPresent"})
@GameTestHolder(SFM.MOD_ID)
@PrefixGameTestTemplate(false)
public class SFMCorrectnessGameTests extends SFMGameTestBase {
    /**
     * Ensure that the manager state gets updated as the disk is inserted and the program is set
     */
    @GameTest(template = "1x2x1")
    public static void manager_state_update(GameTestHelper helper) {
        helper.setBlock(new BlockPos(0, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(0, 2, 0));
        assertTrue(manager.getState() == ManagerBlockEntity.State.NO_DISK, "Manager did not start with no disk");
        assertTrue(manager.getDisk() == null, "Manager did not start with no disk");
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        assertTrue(manager.getState() == ManagerBlockEntity.State.NO_PROGRAM, "Disk did not start with no program");
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           INPUT FROM a
                                           OUTPUT TO b
                                       END
                                   """.stripTrailing().stripIndent());
        assertManagerRunning(manager);
        helper.succeed();
    }

    @GameTest(template = "3x2x1")
    public static void move_1_stack(GameTestHelper helper) {
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
                                           OUTPUT TO b
                                       END
                                   """.stripTrailing().stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .add("b", helper.absolutePos(rightPos))
                .save(Objects.requireNonNull(manager.getDisk()));

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            assertTrue(leftChest.getStackInSlot(0).isEmpty(), "Dirt did not move");
            assertTrue(rightChest.getStackInSlot(0).getCount() == 64, "Dirt did not move");
        });
    }

    @GameTest(template = "3x2x1")
    public static void move_full_chest(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        BlockPos rightPos = new BlockPos(0, 2, 0);
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        BlockPos leftPos = new BlockPos(2, 2, 0);
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());


        var leftChest = getItemHandler(helper, leftPos);

        var rightChest = getItemHandler(helper, rightPos);

        for (int i = 0; i < leftChest.getSlots(); i++) {
            leftChest.insertItem(i, new ItemStack(Blocks.DIRT, 64), false);
        }

        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           INPUT FROM a
                                           OUTPUT TO b
                                       END
                                   """.stripTrailing().stripIndent());
        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .add("b", helper.absolutePos(rightPos))
                .save(Objects.requireNonNull(manager.getDisk()));

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            assertTrue(
                    IntStream.range(0, leftChest.getSlots()).allMatch(slot -> leftChest.getStackInSlot(slot).isEmpty()),
                    "Dirt did not leave"
            );
            int count = rightChest.getSlots() * 64;
            int total = 0;
            for (int i = 0; i < rightChest.getSlots(); i++) {
                ItemStack x = rightChest.getStackInSlot(i);
                if (x.is(Items.DIRT)) {
                    total += rightChest.getStackInSlot(i).getCount();
                }
            }
            assertTrue(total == count, "Dirt did not arrive");
        });
    }


    @GameTest(template = "3x4x3")
    public static void many_outputs(GameTestHelper helper) {
        BlockPos managerPos = new BlockPos(1, 2, 1);
        BlockPos sourcePos = new BlockPos(1, 3, 1);
        BlockPos dest1Pos = new BlockPos(2, 2, 1);
        BlockPos dest2Pos = new BlockPos(0, 2, 1);

        // set up inventories
        helper.setBlock(sourcePos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(dest1Pos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(dest2Pos, SFMBlocks.TEST_BARREL_BLOCK.get());


        var sourceInv = getItemHandler(helper, sourcePos);

        var dest1Inv = getItemHandler(helper, dest1Pos);

        var dest2Inv = getItemHandler(helper, dest2Pos);

        for (int i = 0; i < sourceInv.getSlots(); i++) {
            sourceInv.insertItem(i, new ItemStack(Blocks.DIRT, 64), false);
        }

        // set up manager
        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());
        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(managerPos);
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           INPUT FROM source
                                           OUTPUT 64 dirt TO EACH dest
                                       END
                                   """.stripTrailing().stripIndent());
        // set the labels
        LabelPositionHolder.empty()
                .add("source", helper.absolutePos(sourcePos))
                .add("dest", helper.absolutePos(dest1Pos))
                .add("dest", helper.absolutePos(dest2Pos))
                .save(Objects.requireNonNull(manager.getDisk()));

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            int found = IntStream
                    .range(0, sourceInv.getSlots())
                    .mapToObj(sourceInv::getStackInSlot)
                    .mapToInt(ItemStack::getCount)
                    .sum();
            assertTrue(found == 64 * (sourceInv.getSlots() - 2), "Dirt did not leave (found " + found + " (" + (
                    found > 64 ? found / 64 + "x stacks + " + found % 64 : found
            ) + " dirt))");
            int total;
            total = 0;
            for (int i = 0; i < dest1Inv.getSlots(); i++) {
                ItemStack x = dest1Inv.getStackInSlot(i);
                if (x.is(Items.DIRT)) {
                    total += dest1Inv.getStackInSlot(i).getCount();
                }
            }
            assertTrue(total == 64, "Dirt did not arrive properly 1");
            total = 0;
            for (int i = 0; i < dest2Inv.getSlots(); i++) {
                ItemStack x = dest2Inv.getStackInSlot(i);
                if (x.is(Items.DIRT)) {
                    total += dest2Inv.getStackInSlot(i).getCount();
                }
            }
            assertTrue(total == 64, "Dirt did not arrive properly 2");
        });
    }

    @GameTest(template = "3x2x1")
    public static void retain_5(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        BlockPos leftPos = new BlockPos(2, 2, 0);
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        BlockPos rightPos = new BlockPos(0, 2, 0);
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());

        var rightChest = getItemHandler(helper, rightPos);
        var leftChest = getItemHandler(helper, leftPos);

        leftChest.insertItem(0, new ItemStack(Blocks.DIRT, 64), false);

        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                   EVERY 20 TICKS DO
                                      INPUT RETAIN 5 FROM a
                                      OUTPUT TO b
                                   END
                                   """.stripTrailing().stripIndent());
        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .add("b", helper.absolutePos(rightPos))
                .save(Objects.requireNonNull(manager.getDisk()));

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            assertTrue(leftChest.getStackInSlot(0).getCount() == 5, "Dirt did not move");
            assertTrue(rightChest.getStackInSlot(0).getCount() == 64 - 5, "Dirt did not move");
        });
    }

    @GameTest(template = "3x2x1")
    public static void move_multiple_item_names(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        BlockPos rightPos = new BlockPos(0, 2, 0);
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        BlockPos leftPos = new BlockPos(2, 2, 0);
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());

        var leftChest = getItemHandler(helper, leftPos);
        var rightChest = getItemHandler(helper, rightPos);

        leftChest.insertItem(0, new ItemStack(Items.IRON_INGOT, 64), false);
        leftChest.insertItem(1, new ItemStack(Items.STONE, 64), false);

        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           INPUT
                                               RETAIN 5 iron_ingot,
                                               RETAIN 3 stone
                                           FROM a TOP SIDE
                                   
                                           OUTPUT
                                               2 iron_ingot,
                                               RETAIN 10 stone
                                           TO b
                                       END
                                   """.stripTrailing().stripIndent());
        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .add("b", helper.absolutePos(rightPos))
                .save(Objects.requireNonNull(manager.getDisk()));

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            assertTrue(leftChest.getStackInSlot(0).getCount() == 64 - 2, "Iron ingots did not retain");
            assertTrue(leftChest.getStackInSlot(1).getCount() == 64 - 10, "Stone did not retain");
            assertTrue(rightChest.getStackInSlot(0).getCount() == 2, "Iron ingots did not move");
            assertTrue(rightChest.getStackInSlot(1).getCount() == 10, "Stone did not move");
        });
    }

    /**
     * Ensure that cauldrons can be treated as water fluid holders
     */
    @GameTest(template = "3x2x1")
    public static void move_cauldron_water(GameTestHelper helper) {
        // fill in the blocks needed for the test
        helper.setBlock(new BlockPos(1, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        BlockPos left = new BlockPos(2, 2, 0);
        helper.setBlock(left, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        BlockPos right = new BlockPos(0, 2, 0);
        helper.setBlock(right, Blocks.CAULDRON);

        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(left))
                .add("b", helper.absolutePos(right))
                .save(Objects.requireNonNull(manager.getDisk()));

        // load the program
        manager.setProgram("""
                                       NAME "cauldron water test"
                                   
                                       EVERY 20 TICKS DO
                                           INPUT fluid:minecraft:water FROM a
                                           OUTPUT fluid:*:* TO b
                                       END
                                   """.stripTrailing().stripIndent());

        assertManagerRunning(manager);
        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            helper.assertBlock(left, b -> b == Blocks.CAULDRON, "cauldron didn't empty");
            helper.assertBlockState(
                    right,
                    s -> s.getBlock() == Blocks.WATER_CAULDRON
                         && s.getValue(LayeredCauldronBlock.LEVEL) == 3,
                    () -> "cauldron didn't fill"
            );

        });
    }

    /**
     * Ensure that a cauldrons can be treated as a lava fluid holder
     */
    @GameTest(template = "3x2x1")
    public static void move_cauldron_lava(GameTestHelper helper) {
        // fill in the blocks needed for the test
        helper.setBlock(new BlockPos(1, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        BlockPos left = new BlockPos(2, 2, 0);
        helper.setBlock(left, Blocks.LAVA_CAULDRON.defaultBlockState());
        BlockPos right = new BlockPos(0, 2, 0);
        helper.setBlock(right, Blocks.CAULDRON);

        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(left))
                .add("b", helper.absolutePos(right))
                .save(Objects.requireNonNull(manager.getDisk()));

        // load the program
        manager.setProgram("""
                                       NAME "cauldron lava test"
                                   
                                       EVERY 20 TICKS DO
                                           INPUT fluid:minecraft:lava FROM a
                                           OUTPUT fluid:*:* TO b
                                       END
                                   """.stripTrailing().stripIndent());

        assertManagerRunning(manager);
        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            helper.assertBlock(left, b -> b == Blocks.CAULDRON, "cauldron didn't empty");
            helper.assertBlockState(right, s -> s.getBlock() == Blocks.LAVA_CAULDRON, () -> "cauldron didn't fill");

        });
    }

    @GameTest(template = "25x4x25")
    public static void cable_spiral(GameTestHelper helper) {
        BlockPos start = new BlockPos(0, 2, 0);
        BlockPos end = new BlockPos(12, 2, 12);

        var len = 24;
        var dir = Direction.EAST;
        var current = start;
        while (len > 0) {
            // fill len blocks
            for (int i = 0; i < len; i++) {
                helper.setBlock(current, SFMBlocks.CABLE_BLOCK.get());
                current = current.relative(dir);
            }
            // turn right
            dir = dir.getClockWise();
            len -= 1;
        }

        // fill in the blocks needed for the test
        helper.setBlock(new BlockPos(1, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        helper.setBlock(start, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(end, SFMBlocks.TEST_BARREL_BLOCK.get());

        // add some items
        Container startChest = (Container) helper.getBlockEntity(start);
        startChest.setItem(0, new ItemStack(Items.IRON_INGOT, 64));
        Container endChest = (Container) helper.getBlockEntity(end);


        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(start))
                .add("b", helper.absolutePos(end))
                .save(Objects.requireNonNull(manager.getDisk()));

        // load the program
        manager.setProgram("""
                                       NAME "long cable test"
                                   
                                       EVERY 20 TICKS DO
                                           INPUT FROM a
                                           OUTPUT TO b
                                       END
                                   """.stripTrailing().stripIndent());

        assertManagerRunning(manager);
        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            // ensure item arrived
            assertTrue(endChest.getItem(0).getCount() == 64, "Items did not move");
            // ensure item left
            assertTrue(startChest.getItem(0).isEmpty(), "Items did not leave");

        });
    }


    @GameTest(template = "3x4x3")
    public static void regression_crash_type_mixing(GameTestHelper helper) {
        // fill in the blocks needed for the test
        BlockPos managerPos = new BlockPos(1, 2, 1);
        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());

        BlockPos left = new BlockPos(2, 2, 1);
        helper.setBlock(left, SFMBlocks.TEST_BARREL_BLOCK.get());
        // add sticks to the chest
        Container chest = (Container) helper.getBlockEntity(left);
        chest.setItem(0, new ItemStack(Items.STICK, 64));

        BlockPos right = new BlockPos(0, 2, 1);
        helper.setBlock(right, SFMBlocks.TEST_BARREL_BLOCK.get());

        BlockPos front = new BlockPos(1, 2, 2);
        helper.setBlock(front, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));

        BlockPos back = new BlockPos(1, 2, 0);
        helper.setBlock(back, Blocks.CAULDRON);

        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(managerPos);
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(left))
                .add("a", helper.absolutePos(front))
                .add("b", helper.absolutePos(right))
                .add("b", helper.absolutePos(back))
                .save(Objects.requireNonNull(manager.getDisk()));

        // load the program
        manager.setProgram("""
                                       NAME "water crash test"
                                   
                                       every 20 ticks do
                                           INPUT  item:minecraft:stick, fluid:minecraft:water FROM a
                                           OUTPUT stick, fluid:minecraft:water TO b
                                       end
                                   """.stripTrailing().stripIndent());

        assertManagerRunning(manager);
        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            helper.assertBlock(front, b -> b == Blocks.CAULDRON, "cauldron didn't empty");
            helper.assertBlockState(
                    back,
                    s -> s.getBlock() == Blocks.WATER_CAULDRON
                         && s.getValue(LayeredCauldronBlock.LEVEL) == 3,
                    () -> "cauldron didn't fill"
            );
            // ensure sticks departed
            assertTrue(chest.getItem(0).getCount() == 0, "Items did not move");
            // ensure sticks arrived
            Container rightChest = (Container) helper.getBlockEntity(right);
            assertTrue(rightChest.getItem(0).getCount() == 64, "Items did not move");


        });
    }

    @GameTest(template = "25x4x25") // start with empty platform
    public static void cable_network_formation(GameTestHelper helper) {
        // create a row of cables
        for (int i = 0; i < 10; i++) {
            helper.setBlock(new BlockPos(i, 2, 0), SFMBlocks.CABLE_BLOCK.get());
        }

        var net = CableNetworkManager
                .getOrRegisterNetworkFromCablePosition(helper.getLevel(), helper.absolutePos(new BlockPos(0, 2, 0)))
                .get();
        // those cables should all be on the same network
        for (int i = 0; i < 10; i++) {
            assertTrue(CableNetworkManager
                               .getOrRegisterNetworkFromCablePosition(
                                       helper.getLevel(),
                                       helper.absolutePos(new BlockPos(i, 2, 0))
                               )
                               .get() == net, "Line of ten should be on same network");
        }

        // the network should only contain those cables
        assertTrue(net.getCableCount() == 10, "Network size should be ten");

        // break a block in the middle of the cable
        helper.setBlock(new BlockPos(5, 2, 0), Blocks.AIR);
        // the network should split
        net = CableNetworkManager
                .getOrRegisterNetworkFromCablePosition(helper.getLevel(), helper.absolutePos(new BlockPos(0, 2, 0)))
                .get();
        // now we have a network of 5 cables and a network of 4 cables
        for (int i = 0; i < 5; i++) {
            assertTrue(CableNetworkManager
                               .getOrRegisterNetworkFromCablePosition(
                                       helper.getLevel(),
                                       helper.absolutePos(new BlockPos(i, 2, 0))
                               )
                               .get() == net, "Row of five should be same network after splitting");
        }
        var old = net;
        net = CableNetworkManager
                .getOrRegisterNetworkFromCablePosition(helper.getLevel(), helper.absolutePos(new BlockPos(6, 2, 0)))
                .get();
        assertTrue(old != net, "Networks should be distinct after splitting");
        for (int i = 6; i < 10; i++) {
            assertTrue(CableNetworkManager
                               .getOrRegisterNetworkFromCablePosition(
                                       helper.getLevel(),
                                       helper.absolutePos(new BlockPos(i, 2, 0))
                               )
                               .get() == net, "Remaining row should be same network after splitting");
        }

        // repair the cable
        helper.setBlock(new BlockPos(5, 2, 0), SFMBlocks.CABLE_BLOCK.get());
        // the network should merge
        net = CableNetworkManager
                .getOrRegisterNetworkFromCablePosition(helper.getLevel(), helper.absolutePos(new BlockPos(0, 2, 0)))
                .get();
        for (int i = 0; i < 10; i++) {
            assertTrue(CableNetworkManager
                               .getOrRegisterNetworkFromCablePosition(
                                       helper.getLevel(),
                                       helper.absolutePos(new BlockPos(i, 2, 0))
                               )
                               .get() == net, "Networks should merge to same network after repairing");
        }

        // add cables in the corner
        helper.setBlock(new BlockPos(0, 2, 1), SFMBlocks.CABLE_BLOCK.get());
        helper.setBlock(new BlockPos(1, 2, 1), SFMBlocks.CABLE_BLOCK.get());
        assertTrue(CableNetworkManager
                           .getOrRegisterNetworkFromCablePosition(
                                   helper.getLevel(),
                                   helper.absolutePos(new BlockPos(0, 2, 0))
                           )
                           .get()
                           .getCableCount() == 12, "Network should grow to twelve after adding two cables");

        // punch out the corner, the network should shrink by 1
        helper.setBlock(new BlockPos(1, 2, 1), Blocks.AIR);
        assertTrue(CableNetworkManager
                           .getOrRegisterNetworkFromCablePosition(
                                   helper.getLevel(),
                                   helper.absolutePos(new BlockPos(0, 2, 0))
                           )
                           .get()
                           .getCableCount() == 11, "Network should shrink to eleven after removing a cable");


        // create a new network in a plus shape
        helper.setBlock(new BlockPos(15, 2, 15), SFMBlocks.CABLE_BLOCK.get());
        for (Direction value : SFMDirections.DIRECTIONS) {
            helper.setBlock(new BlockPos(15, 2, 15).relative(value), SFMBlocks.CABLE_BLOCK.get());
        }
        // should all be on the same network
        net = CableNetworkManager
                .getOrRegisterNetworkFromCablePosition(helper.getLevel(), helper.absolutePos(new BlockPos(15, 2, 15)))
                .get();
        for (Direction value : SFMDirections.DIRECTIONS) {
            assertTrue(CableNetworkManager
                               .getOrRegisterNetworkFromCablePosition(
                                       helper.getLevel(),
                                       helper.absolutePos(new BlockPos(15, 2, 15).relative(value))
                               )
                               .get()
                       == net, "Plus cables should all be on the same network");
        }

        // break the block in the middle
        helper.setBlock(new BlockPos(15, 2, 15), Blocks.AIR);
        // the network should split
        assertTrue(CableNetworkManager
                           .getOrRegisterNetworkFromCablePosition(
                                   helper.getLevel(),
                                   helper.absolutePos(new BlockPos(15, 2, 15))
                           )
                           .isEmpty(), "Network should not be present where the cable was removed from");
        var networks = new ArrayList<CableNetwork>();
        for (Direction value : SFMDirections.DIRECTIONS) {
            networks.add(CableNetworkManager
                                 .getOrRegisterNetworkFromCablePosition(
                                         helper.getLevel(),
                                         helper.absolutePos(new BlockPos(15, 2, 15).relative(value))
                                 )
                                 .get());
        }
        // make sure all the networks are different
        for (CableNetwork network : networks) {
            assertTrue(
                    networks.stream().filter(n -> n == network).count() == 1,
                    "Broken plus networks should be distinct"
            );
        }

        // add the block back
        helper.setBlock(new BlockPos(15, 2, 15), SFMBlocks.CABLE_BLOCK.get());
        // the network should merge
        net = CableNetworkManager
                .getOrRegisterNetworkFromCablePosition(helper.getLevel(), helper.absolutePos(new BlockPos(15, 2, 15)))
                .get();
        for (Direction value : SFMDirections.DIRECTIONS) {
            assertTrue(CableNetworkManager
                               .getOrRegisterNetworkFromCablePosition(
                                       helper.getLevel(),
                                       helper.absolutePos(new BlockPos(15, 2, 15).relative(value))
                               )
                               .get()
                       == net, "Plus networks did not merge after repairing");
        }

        // let's also test having cables in more than just a straight line
        // we want corners with multiple cables adjacent

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                helper.setBlock(new BlockPos(7 + i, 2, 7 + j), SFMBlocks.CABLE_BLOCK.get());
            }
        }
        // make sure it's all in a single network
        assertTrue(CableNetworkManager
                           .getOrRegisterNetworkFromCablePosition(
                                   helper.getLevel(),
                                   helper.absolutePos(new BlockPos(7, 2, 7))
                           )
                           .get()
                           .getCableCount() == 25, "Network cable count should be 25");
        // cut a line through it
        for (int i = 0; i < 5; i++) {
            helper.setBlock(new BlockPos(7 + i, 2, 9), Blocks.AIR);
        }

        // make sure the network disappeared where it was cut
        assertTrue(CableNetworkManager
                           .getOrRegisterNetworkFromCablePosition(
                                   helper.getLevel(),
                                   helper.absolutePos(new BlockPos(7, 2, 9))
                           )
                           .isEmpty(), "Network should not be present where the cable was removed from");
        // make sure new network of 10 is formed
        assertTrue(CableNetworkManager
                           .getOrRegisterNetworkFromCablePosition(
                                   helper.getLevel(),
                                   helper.absolutePos(new BlockPos(7, 2, 8))
                           )
                           .get()
                           .getCableCount() == 10, "New network should be size ten");
        // make sure new network of 10 is formed
        assertTrue(CableNetworkManager
                           .getOrRegisterNetworkFromCablePosition(
                                   helper.getLevel(),
                                   helper.absolutePos(new BlockPos(7, 2, 11))
                           )
                           .get()
                           .getCableCount() == 10, "Other new network should be size ten");
        // make sure the new networks are distinct
        assertTrue(CableNetworkManager
                           .getOrRegisterNetworkFromCablePosition(
                                   helper.getLevel(),
                                   helper.absolutePos(new BlockPos(7, 2, 8))
                           )
                           .get() != CableNetworkManager
                           .getOrRegisterNetworkFromCablePosition(
                                   helper.getLevel(),
                                   helper.absolutePos(new BlockPos(7, 2, 11))
                           )
                           .get(), "New networks should be distinct");


        helper.succeed();
    }

    @GameTest(template = "3x2x1")
    public static void cable_network_rebuilding(GameTestHelper helper) {
        helper.setBlock(new BlockPos(0, 2, 0), SFMBlocks.CABLE_BLOCK.get());
        helper.setBlock(new BlockPos(1, 2, 0), SFMBlocks.CABLE_BLOCK.get());
        helper.setBlock(new BlockPos(2, 2, 0), SFMBlocks.CABLE_BLOCK.get());
        var network = CableNetworkManager.getOrRegisterNetworkFromCablePosition(
                helper.getLevel(),
                helper.absolutePos(new BlockPos(0, 2, 0))
        );
        assertTrue(network.isPresent(), "Network should be built");
        CableNetworkManager.unregisterNetworkForTestingPurposes(network.get());
        network = CableNetworkManager.getOrRegisterNetworkFromCablePosition(
                helper.getLevel(),
                helper.absolutePos(new BlockPos(0, 2, 0))
        );
        assertTrue(network.isPresent(), "Network should be rebuilt after clearing");
        assertTrue(network.get().getCableCount() == 3, "Network rebuilding should discover 3 cables");
        helper.succeed();
    }


    @GameTest(template = "3x2x1") // start with empty platform
    public static void CauldronLavaMovement(GameTestHelper helper) {
        // fill in the blocks needed for the test
        helper.setBlock(new BlockPos(1, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        BlockPos left = new BlockPos(2, 2, 0);
        helper.setBlock(left, Blocks.LAVA_CAULDRON);
        BlockPos right = new BlockPos(0, 2, 0);
        helper.setBlock(right, Blocks.CAULDRON);

        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));

        // create the program
        var program = """
                    NAME "cauldron water test"
                
                    EVERY 20 TICKS DO
                        INPUT fluid:minecraft:lava FROM a
                        OUTPUT fluid:*:* TO b
                    END
                """;

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(left))
                .add("b", helper.absolutePos(right))
                .save(Objects.requireNonNull(manager.getDisk()));

        // load the program
        manager.setProgram(program);

        assertManagerRunning(manager);
        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            helper.assertBlock(left, b -> b == Blocks.CAULDRON, "cauldron didn't empty");
            helper.assertBlockState(right, s -> s.getBlock() == Blocks.LAVA_CAULDRON, () -> "cauldron didn't fill");

        });
    }

    @GameTest(template = "3x2x1")
    public static void move_slots(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        BlockPos rightPos = new BlockPos(0, 2, 0);
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        BlockPos leftPos = new BlockPos(2, 2, 0);
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());

        var rightChest = getItemHandler(helper, rightPos);
        var leftChest = getItemHandler(helper, leftPos);

        leftChest.insertItem(0, new ItemStack(Items.DIAMOND, 5), false);
        leftChest.insertItem(1, new ItemStack(Items.DIAMOND, 5), false);
        leftChest.insertItem(3, new ItemStack(Items.DIAMOND, 5), false);
        leftChest.insertItem(4, new ItemStack(Items.DIAMOND, 5), false);
        leftChest.insertItem(5, new ItemStack(Items.DIAMOND, 5), false);

        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           INPUT FROM a TOP SIDE SLOTS 0,1,3-4,5
                                           OUTPUT TO a SLOTS 2
                                       END
                                   """.stripTrailing().stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .add("b", helper.absolutePos(rightPos))
                .save(Objects.requireNonNull(manager.getDisk()));

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            assertTrue(leftChest.getStackInSlot(0).isEmpty(), "slot 0 did not leave");
            assertTrue(leftChest.getStackInSlot(1).isEmpty(), "slot 1 did not leave");
            assertTrue(leftChest.getStackInSlot(3).isEmpty(), "slot 3 did not leave");
            assertTrue(leftChest.getStackInSlot(4).isEmpty(), "slot 4 did not leave");
            assertTrue(leftChest.getStackInSlot(5).isEmpty(), "slot 5 did not leave");
            assertTrue(leftChest.getStackInSlot(2).getCount() == 25, "Items did not transfer to slot 2");
            assertTrue(IntStream
                               .range(0, rightChest.getSlots())
                               .allMatch(slot -> rightChest.getStackInSlot(slot).isEmpty()), "Chest b is not empty");

        });
    }


    @GameTest(template = "3x4x3")
    public static void printing_press_clone_program(GameTestHelper helper) {
        var printingPos = new BlockPos(1, 2, 1);
        var pistonPos = new BlockPos(1, 4, 1);
        var woodPos = new BlockPos(0, 4, 1);
        var buttonPos = new BlockPos(0, 4, 0);
        var chestPos = new BlockPos(0, 2, 1);

        helper.setBlock(printingPos, SFMBlocks.PRINTING_PRESS_BLOCK.get());
        helper.setBlock(pistonPos, Blocks.PISTON.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.DOWN));
        helper.setBlock(woodPos, Blocks.OAK_PLANKS);
        helper.setBlock(buttonPos, Blocks.STONE_BUTTON);
        helper.setBlock(chestPos, SFMBlocks.TEST_BARREL_BLOCK.get());

        var printingPress = (PrintingPressBlockEntity) helper.getBlockEntity(printingPos);
        Player player = helper.makeMockPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BLACK_DYE));
        BlockState pressState = helper.getBlockState(printingPos);
        pressState.getBlock().use(
                pressState,
                helper.getLevel(),
                helper.absolutePos(printingPos),
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(
                        new Vec3(0.5, 0.5, 0.5),
                        Direction.UP,
                        helper.absolutePos(printingPos),
                        false
                )
        );
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(SFMItems.DISK_ITEM.get()));
        pressState.getBlock().use(
                pressState,
                helper.getLevel(),
                helper.absolutePos(printingPos),
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(
                        new Vec3(0.5, 0.5, 0.5),
                        Direction.UP,
                        helper.absolutePos(printingPos),
                        false
                )
        );
        var disk = new ItemStack(SFMItems.DISK_ITEM.get());
        DiskItem.setProgram(disk, """
                    EVERY 20 TICKS DO
                        INPUT FROM a TOP SIDE SLOTS 0,1,3-4,5
                        OUTPUT TO a SLOTS 2
                    END
                """.stripTrailing().stripIndent());
        player.setItemInHand(InteractionHand.MAIN_HAND, FormItem.getForm(disk));
        pressState.getBlock().use(
                pressState,
                helper.getLevel(),
                helper.absolutePos(printingPos),
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(
                        new Vec3(0.5, 0.5, 0.5),
                        Direction.UP,
                        helper.absolutePos(printingPos),
                        false
                )
        );

        BlockState buttonState = helper.getBlockState(buttonPos);
        buttonState.getBlock().use(
                buttonState,
                helper.getLevel(),
                helper.absolutePos(buttonPos),
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(
                        new Vec3(0.5, 0.5, 0.5),
                        Direction.UP,
                        helper.absolutePos(printingPos),
                        false
                )
        );

        helper.runAfterDelay(5, () -> {
            pressState.getBlock().use(
                    pressState,
                    helper.getLevel(),
                    helper.absolutePos(printingPos),
                    player,
                    InteractionHand.MAIN_HAND,
                    new BlockHitResult(
                            new Vec3(0.5, 0.5, 0.5),
                            Direction.UP,
                            helper.absolutePos(printingPos),
                            false
                    )
            );
            ItemStack held = player.getMainHandItem();
            if (held.is(SFMItems.DISK_ITEM.get()) && DiskItem.getProgram(held).equals(DiskItem.getProgram(disk))) {
                var chest = getItemHandler(helper, chestPos);
                chest.insertItem(0, held, false);
                assertTrue(printingPress.getInk().isEmpty(), "Ink was not consumed");
                assertTrue(printingPress.getPaper().isEmpty(), "Paper was not consumed");
                assertTrue(!printingPress.getForm().isEmpty(), "Form should not be consumed");
                helper.succeed();
            } else {
                helper.fail("Disk was not cloned");
            }
        });
    }

    @GameTest(template = "1x2x1")
    public static void printing_press_insertion_extraction(GameTestHelper helper) {
        var pos = new BlockPos(0, 2, 0);
        helper.setBlock(pos, SFMBlocks.PRINTING_PRESS_BLOCK.get());
        var printingPress = (PrintingPressBlockEntity) helper.getBlockEntity(pos);
        var player = helper.makeMockPlayer();
        // put black dye in player hand
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BLACK_DYE, 23));
        // right click on printing press
        BlockState pressState = helper.getBlockState(pos);
        pressState.getBlock().use(
                pressState,
                helper.getLevel(),
                helper.absolutePos(pos),
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(
                        new Vec3(0.5, 0.5, 0.5),
                        Direction.UP,
                        helper.absolutePos(pos),
                        false
                )
        );
        // assert the ink was inserted
        assertTrue(!printingPress.getInk().isEmpty(), "Ink was not inserted");
        assertTrue(player.getMainHandItem().isEmpty(), "Ink was not taken from hand");
        // put book in player hand
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOOK));
        // right click on printing press
        pressState.getBlock().use(
                pressState,
                helper.getLevel(),
                helper.absolutePos(pos),
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(
                        new Vec3(0.5, 0.5, 0.5),
                        Direction.UP,
                        helper.absolutePos(pos),
                        false
                )
        );
        // assert the book was inserted
        assertTrue(!printingPress.getPaper().isEmpty(), "Paper was not inserted");
        assertTrue(player.getMainHandItem().isEmpty(), "Paper was not taken from hand");
        // put form in player hand
        var form = FormItem.getForm(new ItemStack(Items.WRITTEN_BOOK));
        player.setItemInHand(InteractionHand.MAIN_HAND, form.copy());
        // right click on printing press
        pressState.getBlock().use(
                pressState,
                helper.getLevel(),
                helper.absolutePos(pos),
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(
                        new Vec3(0.5, 0.5, 0.5),
                        Direction.UP,
                        helper.absolutePos(pos),
                        false
                )
        );
        // assert the form was inserted
        assertTrue(!printingPress.getForm().isEmpty(), "Form was not inserted");
        assertTrue(player.getMainHandItem().isEmpty(), "Form was not taken from hand");

        // pull out item
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        // right click on printing press
        pressState.getBlock().use(
                pressState,
                helper.getLevel(),
                helper.absolutePos(pos),
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(
                        new Vec3(0.5, 0.5, 0.5),
                        Direction.UP,
                        helper.absolutePos(pos),
                        false
                )
        );
        // assert the paper was extracted
        assertTrue(printingPress.getPaper().isEmpty(), "Paper was not extracted");
        assertTrue(!player.getMainHandItem().isEmpty(), "Paper was not given to player");
        assertTrue(player.getMainHandItem().is(Items.BOOK), "Paper doesn't match");
        assertTrue(player.getMainHandItem().getCount() == 1, "Paper wrong count");

        // pull out an item
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        // right click on printing press
        pressState.getBlock().use(
                pressState,
                helper.getLevel(),
                helper.absolutePos(pos),
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(
                        new Vec3(0.5, 0.5, 0.5),
                        Direction.UP,
                        helper.absolutePos(pos),
                        false
                )
        );
        // assert the form was extracted
        assertTrue(printingPress.getForm().isEmpty(), "Form was not extracted");
        assertTrue(!player.getMainHandItem().isEmpty(), "Form was not given to player");
        assertTrue(ItemStack.isSameItemSameTags(player.getMainHandItem(), form), "Form doesn't match");
        // pull out item
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        // right click on printing press
        pressState.getBlock().use(
                pressState,
                helper.getLevel(),
                helper.absolutePos(pos),
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(
                        new Vec3(0.5, 0.5, 0.5),
                        Direction.UP,
                        helper.absolutePos(pos),
                        false
                )
        );
        // assert the ink was extracted
        assertTrue(printingPress.getInk().isEmpty(), "Ink was not extracted");
        assertTrue(!player.getMainHandItem().isEmpty(), "Ink was not given to player");
        assertTrue(player.getMainHandItem().is(Items.BLACK_DYE), "Ink doesn't match");
        assertTrue(player.getMainHandItem().getCount() == 23, "Ink wrong count");
        // try to pull out another item
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        // right click on printing press
        pressState.getBlock().use(
                pressState,
                helper.getLevel(),
                helper.absolutePos(pos),
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(
                        new Vec3(0.5, 0.5, 0.5),
                        Direction.UP,
                        helper.absolutePos(pos),
                        false
                )
        );
        // assert nothing was extracted
        assertTrue(player.getMainHandItem().isEmpty(), "Nothing should have been extracted");
        helper.succeed();
    }

    @GameTest(template = "3x4x3")
    public static void printing_press_clone_enchantment(GameTestHelper helper) {
        var printingPos = new BlockPos(1, 2, 1);
        var pistonPos = new BlockPos(1, 4, 1);
        var woodPos = new BlockPos(0, 4, 1);
        var buttonPos = new BlockPos(0, 4, 0);
        var chestPos = new BlockPos(0, 2, 1);

        helper.setBlock(printingPos, SFMBlocks.PRINTING_PRESS_BLOCK.get());
        helper.setBlock(pistonPos, Blocks.PISTON.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.DOWN));
        helper.setBlock(woodPos, Blocks.OAK_PLANKS);
        helper.setBlock(buttonPos, Blocks.STONE_BUTTON);
        helper.setBlock(chestPos, SFMBlocks.TEST_BARREL_BLOCK.get());

        var printingPress = (PrintingPressBlockEntity) helper.getBlockEntity(printingPos);
        Player player = helper.makeMockPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(SFMItems.EXPERIENCE_GOOP_ITEM.get(), 10));
        BlockState pressState = helper.getBlockState(printingPos);
        pressState.getBlock().use(
                pressState,
                helper.getLevel(),
                helper.absolutePos(printingPos),
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(
                        new Vec3(0.5, 0.5, 0.5),
                        Direction.UP,
                        helper.absolutePos(printingPos),
                        false
                )
        );
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOOK));
        pressState.getBlock().use(
                pressState,
                helper.getLevel(),
                helper.absolutePos(printingPos),
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(
                        new Vec3(0.5, 0.5, 0.5),
                        Direction.UP,
                        helper.absolutePos(printingPos),
                        false
                )
        );
        ItemStack reference = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(
                Enchantments.SHARPNESS,
                3
        ));
        player.setItemInHand(InteractionHand.MAIN_HAND, FormItem.getForm(reference));
        pressState.getBlock().use(
                pressState,
                helper.getLevel(),
                helper.absolutePos(printingPos),
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(
                        new Vec3(0.5, 0.5, 0.5),
                        Direction.UP,
                        helper.absolutePos(printingPos),
                        false
                )
        );

        BlockState buttonState = helper.getBlockState(buttonPos);
        buttonState.getBlock().use(
                buttonState,
                helper.getLevel(),
                helper.absolutePos(buttonPos),
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(
                        new Vec3(0.5, 0.5, 0.5),
                        Direction.UP,
                        helper.absolutePos(printingPos),
                        false
                )
        );

        helper.runAfterDelay(5, () -> {
            pressState.getBlock().use(
                    pressState,
                    helper.getLevel(),
                    helper.absolutePos(printingPos),
                    player,
                    InteractionHand.MAIN_HAND,
                    new BlockHitResult(
                            new Vec3(0.5, 0.5, 0.5),
                            Direction.UP,
                            helper.absolutePos(printingPos),
                            false
                    )
            );
            ItemStack held = player.getMainHandItem();
            if (ItemStack.isSameItemSameTags(held, reference)) {
                var chest = getItemHandler(helper, chestPos);
                chest.insertItem(0, held, false);
                assertTrue(printingPress.getInk().getCount() == 9, "Ink was not consumed properly");
                assertTrue(printingPress.getPaper().isEmpty(), "Paper was not consumed");
                assertTrue(!printingPress.getForm().isEmpty(), "Form should not be consumed");
                helper.succeed();
            } else {
                helper.fail("cloned item wasnt same");
            }
        });
    }

    @GameTest(template = "3x4x3")
    public static void falling_anvil_program_form(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 1), Blocks.IRON_BLOCK);
        var pos = helper.absoluteVec(new Vec3(1.5, 3.5, 1.5));
        ItemStack disk = new ItemStack(SFMItems.DISK_ITEM.get());
        DiskItem.setProgram(disk, """
                    NAME "falling anvil test"
                    EVERY 20 TICKS DO
                        INPUT FROM a TOP SIDE SLOTS 0,1,3-4,5
                        OUTPUT TO a SLOTS 2
                    END
                """.stripTrailing().stripIndent());
        helper
                .getLevel()
                .addFreshEntity(new ItemEntity(
                        helper.getLevel(),
                        pos.x, pos.y, pos.z,
                        disk,
                        0, 0, 0
                ));
        helper.setBlock(new BlockPos(1, 4, 1), Blocks.ANVIL);
        helper.runAfterDelay(20, () -> {
            List<ItemEntity> found = helper
                    .getLevel()
                    .getEntitiesOfClass(
                            ItemEntity.class,
                            new AABB(helper.absolutePos(new BlockPos(1, 4, 1))).inflate(3)
                    );
            if (found.stream().anyMatch(e -> ItemStack.isSameItemSameTags(e.getItem(), FormItem.getForm(disk)))) {
                helper.succeed();
            } else {
                helper.fail("no form found");
            }
        });
    }

    @GameTest(template = "3x4x3")
    public static void falling_anvil_enchantment_form(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 1), Blocks.IRON_BLOCK);
        var pos = helper.absoluteVec(new Vec3(1.5, 3.5, 1.5));
        ItemStack reference = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(
                Enchantments.SHARPNESS,
                3
        ));
        helper
                .getLevel()
                .addFreshEntity(new ItemEntity(
                        helper.getLevel(),
                        pos.x, pos.y, pos.z,
                        reference,
                        0, 0, 0
                ));
        helper.setBlock(new BlockPos(1, 4, 1), Blocks.ANVIL);
        helper.runAfterDelay(20, () -> {
            List<ItemEntity> found = helper
                    .getLevel()
                    .getEntitiesOfClass(
                            ItemEntity.class,
                            new AABB(helper.absolutePos(new BlockPos(1, 4, 1))).inflate(3)
                    );
            if (found.stream().anyMatch(e -> ItemStack.isSameItemSameTags(e.getItem(), FormItem.getForm(reference)))) {
                helper.succeed();
            } else {
                helper.fail("no form found");
            }
        });
    }

    @GameTest(template = "3x4x3")
    public static void falling_anvil_disenchant(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 1), Blocks.OBSIDIAN);
        var pos = helper.absoluteVec(new Vec3(1.5, 3.5, 1.5));
        helper
                .getLevel()
                .addFreshEntity(new ItemEntity(
                        helper.getLevel(),
                        pos.x, pos.y, pos.z,
                        new ItemStack(Items.BOOK, 16),
                        0, 0, 0
                ));
        var axe = new ItemStack(Items.GOLDEN_AXE);
        axe.enchant(Enchantments.BLOCK_EFFICIENCY, 3);
        axe.enchant(Enchantments.SHARPNESS, 2);
        helper.getLevel().addFreshEntity(new ItemEntity(
                helper.getLevel(),
                pos.x, pos.y, pos.z,
                axe,
                0, 0, 0
        ));
        helper.setBlock(new BlockPos(1, 4, 1), Blocks.ANVIL);
        helper.runAfterDelay(20, () -> {
            List<ItemEntity> found = helper
                    .getLevel()
                    .getEntitiesOfClass(
                            ItemEntity.class,
                            new AABB(helper.absolutePos(new BlockPos(1, 4, 1))).inflate(3)
                    );
            boolean foundDisenchantedAxe = found
                    .stream()
                    .anyMatch(e -> ItemStack.isSameItemSameTags(e.getItem(), new ItemStack(Items.GOLDEN_AXE)));
            boolean foundEfficiencyBook = found
                    .stream()
                    .anyMatch(e -> ItemStack.isSameItemSameTags(
                            e.getItem(),
                            EnchantedBookItem.createForEnchantment(new EnchantmentInstance(
                                    Enchantments.BLOCK_EFFICIENCY,
                                    3
                            ))
                    ));
            boolean foundSharpnessBook = found
                    .stream()
                    .anyMatch(e -> ItemStack.isSameItemSameTags(
                            e.getItem(),
                            EnchantedBookItem.createForEnchantment(new EnchantmentInstance(Enchantments.SHARPNESS, 2))
                    ));
            boolean foundRemainingBooks = found
                                                  .stream()
                                                  .filter(e -> e.getItem().is(Items.BOOK))
                                                  .mapToInt(e -> e.getItem().getCount())
                                                  .sum() == 16 - 2;
            if (foundDisenchantedAxe && foundEfficiencyBook && foundSharpnessBook && foundRemainingBooks) {
                helper.succeed();
            } else {
                helper.fail("disenchant failed");
            }
        });
    }

    @GameTest(template = "3x4x3")
    public static void falling_anvil_xp_shard(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 1), Blocks.OBSIDIAN);
        var pos = helper.absoluteVec(new Vec3(1.5, 3.5, 1.5));
        helper
                .getLevel()
                .addFreshEntity(new ItemEntity(
                        helper.getLevel(),
                        pos.x, pos.y, pos.z,
                        EnchantedBookItem.createForEnchantment(new EnchantmentInstance(
                                Enchantments.SHARPNESS,
                                3
                        )),
                        0, 0, 0
                ));
        helper.setBlock(new BlockPos(1, 4, 1), Blocks.ANVIL);
        helper.runAfterDelay(20, () -> {
            List<ItemEntity> found = helper
                    .getLevel()
                    .getEntitiesOfClass(
                            ItemEntity.class,
                            new AABB(helper.absolutePos(new BlockPos(1, 4, 1))).inflate(3)
                    );
            assertTrue(found.size() == 1, "should only be one item");
            assertTrue(found.get(0).getItem().is(SFMItems.EXPERIENCE_SHARD_ITEM.get()), "should be an xp shard");
            assertTrue(found.get(0).getItem().getCount() == 1, "should only be one");
            helper.succeed();
        });
    }

    @GameTest(template = "3x4x3")
    public static void falling_anvil_xp_shard_many(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 1), Blocks.OBSIDIAN);
        var pos = helper.absoluteVec(new Vec3(1.5, 3.5, 1.5));
        ItemStack enchBook = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(
                Enchantments.SHARPNESS,
                3
        ));
        for (int i = 0; i < 10; i++) {
            helper
                    .getLevel()
                    .addFreshEntity(new ItemEntity(
                            helper.getLevel(),
                            pos.x, pos.y, pos.z,
                            enchBook,
                            0, 0, 0
                    ));
        }
        helper.setBlock(new BlockPos(1, 4, 1), Blocks.ANVIL);
        helper.runAfterDelay(20, () -> {
            List<ItemEntity> found = helper
                    .getLevel()
                    .getEntitiesOfClass(
                            ItemEntity.class,
                            new AABB(helper.absolutePos(new BlockPos(1, 4, 1))).inflate(3)
                    );
            assertTrue(
                    found.stream().allMatch(e -> e.getItem().is(SFMItems.EXPERIENCE_SHARD_ITEM.get())),
                    "should only be xp shards"
            );
            assertTrue(found.stream().mapToInt(e -> e.getItem().getCount()).sum() == 10, "bad count");
            helper.succeed();
        });
    }

    @GameTest(template = "1x2x1")
    public static void disk_item_clientside_regression(GameTestHelper helper) {
        var stack = new ItemStack(SFMItems.DISK_ITEM.get());
        stack.getDisplayName();
        stack.getHoverName();
        stack.getItem().getName(stack);
        stack.getItem().appendHoverText(stack, helper.getLevel(), new ArrayList<>(), TooltipFlag.Default.NORMAL);
        Vec3 pos = helper.absoluteVec(new Vec3(0.5, 2, 0.5));
        ItemEntity itemEntity = new ItemEntity(helper.getLevel(), pos.x, pos.y, pos.z, stack, 0, 0, 0);
        helper.getLevel().addFreshEntity(itemEntity);
        helper.succeed();
    }

    @GameTest(template = "1x2x1")
    public static void program_crlf_line_endings_conversion(GameTestHelper helper) {
        var managerPos = new BlockPos(0, 2, 0);
        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());
        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(managerPos);
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        String program = """
                NAME "line endings test"
                EVERY 20 TICKS DO
                    INPUT FROM a
                    OUTPUT TO b
                END
                """.stripTrailing().stripIndent();
        String programWithWindowsLineEndings = program.replaceAll("\n", "\r\n");
        manager.setProgram(programWithWindowsLineEndings);
        var programString = manager.getProgramString();
        if (programString.equals(program)) {
            helper.succeed();
        } else {
            helper.fail(String.format(
                    "program string was not converted correctly: %s",
                    programString
            ));
        }
    }


    @GameTest(template = "3x2x1")
    public static void pattern_cache_regression_1(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        BlockPos rightPos = new BlockPos(0, 2, 0);
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        BlockPos leftPos = new BlockPos(2, 2, 0);
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());

        var rightChest = getItemHandler(helper, rightPos);
        var leftChest = getItemHandler(helper, leftPos);

        leftChest.insertItem(0, new ItemStack(Blocks.IRON_BLOCK, 64), false);

        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           -- pattern caching behaviour should not short circuit this to match all
                                           -- since underscores wouldn't be matched by this
                                           INPUT "[a-z]*" FROM a
                                           OUTPUT TO b
                                       END
                                   """.stripTrailing().stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .add("b", helper.absolutePos(rightPos))
                .save(Objects.requireNonNull(manager.getDisk()));

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            assertTrue(leftChest.getStackInSlot(0).getCount() == 64, "should not depart");
            assertTrue(rightChest.getStackInSlot(0).isEmpty(), "should not arrive");

        });
    }

    @GameTest(template = "3x2x1")
    public static void pattern_cache_regression_2(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        BlockPos rightPos = new BlockPos(0, 2, 0);
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        BlockPos leftPos = new BlockPos(2, 2, 0);
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());

        var rightChest = getItemHandler(helper, rightPos);
        var leftChest = getItemHandler(helper, leftPos);

        leftChest.insertItem(0, new ItemStack(Blocks.IRON_BLOCK, 64), false);

        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           -- pattern caching behaviour should not short circuit this to match all
                                           -- since underscores wouldn't be matched by this
                                           INPUT "[a-zA-Z]*" FROM a
                                           OUTPUT TO b
                                       END
                                   """.stripTrailing().stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .add("b", helper.absolutePos(rightPos))
                .save(Objects.requireNonNull(manager.getDisk()));

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            assertTrue(leftChest.getStackInSlot(0).getCount() == 64, "should not depart");
            assertTrue(rightChest.getStackInSlot(0).isEmpty(), "should not arrive");

        });
    }

    @GameTest(template = "3x2x1")
    public static void pattern_cache_regression_3(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        BlockPos rightPos = new BlockPos(0, 2, 0);
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        BlockPos leftPos = new BlockPos(2, 2, 0);
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());

        var rightChest = getItemHandler(helper, rightPos);
        var leftChest = getItemHandler(helper, leftPos);

        leftChest.insertItem(0, new ItemStack(Blocks.IRON_BLOCK, 64), false);

        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           -- pattern caching behaviour should not short circuit this to match all
                                           -- since underscores wouldn't be matched by this
                                           INPUT "[a-z0-9]*" FROM a
                                           OUTPUT TO b
                                       END
                                   """.stripTrailing().stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .add("b", helper.absolutePos(rightPos))
                .save(Objects.requireNonNull(manager.getDisk()));

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            assertTrue(leftChest.getStackInSlot(0).getCount() == 64, "should not depart");
            assertTrue(rightChest.getStackInSlot(0).isEmpty(), "should not arrive");

        });
    }

    @GameTest(template = "3x2x1")
    public static void each_src_quantity(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        BlockPos rightPos = new BlockPos(0, 2, 0);
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        BlockPos leftPos = new BlockPos(2, 2, 0);
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());

        var rightChest = getItemHandler(helper, rightPos);
        var leftChest = getItemHandler(helper, leftPos);

        leftChest.insertItem(0, new ItemStack(Items.IRON_INGOT, 64), false);
        leftChest.insertItem(1, new ItemStack(Items.GOLD_INGOT, 64), false);
        leftChest.insertItem(2, new ItemStack(Items.NETHERITE_INGOT, 64), false);

        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           INPUT 2 EACH *ingot* FROM a
                                           OUTPUT TO b
                                       END
                                   """.stripTrailing().stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .add("b", helper.absolutePos(rightPos))
                .save(Objects.requireNonNull(manager.getDisk()));

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            // left should have 62 of each ingot
            assertTrue(count(leftChest, Items.IRON_INGOT) == 62, "Iron did not move");
            assertTrue(count(leftChest, Items.GOLD_INGOT) == 62, "Gold did not move");
            assertTrue(count(leftChest, Items.NETHERITE_INGOT) == 62, "Netherite did not move");
            // right should have 2 of each ingot
            assertTrue(count(rightChest, Items.IRON_INGOT) == 2, "Iron did not arrive");
            assertTrue(count(rightChest, Items.GOLD_INGOT) == 2, "Gold did not arrive");
            assertTrue(count(rightChest, Items.NETHERITE_INGOT) == 2, "Netherite did not arrive");

        });
    }


    @GameTest(template = "3x2x1")
    public static void each_src_quantity_retain(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        BlockPos rightPos = new BlockPos(0, 2, 0);
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        BlockPos leftPos = new BlockPos(2, 2, 0);
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());

        var rightChest = getItemHandler(helper, rightPos);
        var leftChest = getItemHandler(helper, leftPos);

        leftChest.insertItem(0, new ItemStack(Items.IRON_INGOT, 2), false);
        leftChest.insertItem(1, new ItemStack(Items.GOLD_INGOT, 2), false);

        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           -- move 2 of each ingot type
                                           -- keep 2 ingots total
                                           INPUT 2 EACH RETAIN 2 *ingot* FROM a
                                           OUTPUT TO b
                                       END
                                   """.stripTrailing().stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .add("b", helper.absolutePos(rightPos))
                .save(Objects.requireNonNull(manager.getDisk()));

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            // two of the four ingots should have moved
            // for now we assume that gold will move since it is in the higher slot
            assertTrue(count(leftChest, Items.IRON_INGOT) == 2, "Iron moved");
            assertTrue(count(leftChest, Items.GOLD_INGOT) == 0, "Gold did not move");
            assertTrue(count(rightChest, Items.IRON_INGOT) == 0, "Iron arrive");
            assertTrue(count(rightChest, Items.GOLD_INGOT) == 2, "Gold did not arrive");

        });
    }


    @GameTest(template = "3x2x1")
    public static void each_src_quantity_each_retain(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        BlockPos rightPos = new BlockPos(0, 2, 0);
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        BlockPos leftPos = new BlockPos(2, 2, 0);
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());

        var rightChest = getItemHandler(helper, rightPos);
        var leftChest = getItemHandler(helper, leftPos);

        leftChest.insertItem(0, new ItemStack(Items.IRON_INGOT, 2), false);
        leftChest.insertItem(1, new ItemStack(Items.GOLD_INGOT, 2), false);

        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           -- move 2 of each ingot type
                                           -- keep 2 ingots of each type
                                           INPUT 2 EACH RETAIN 2 EACH *ingot* FROM a
                                           OUTPUT TO b
                                       END
                                   """.stripTrailing().stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .add("b", helper.absolutePos(rightPos))
                .save(Objects.requireNonNull(manager.getDisk()));

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            // two of the four ingots should have moved
            // for now we assume that gold will move since it is in the higher slot
            assertTrue(count(leftChest, Items.IRON_INGOT) == 2, "Iron moved");
            assertTrue(count(leftChest, Items.GOLD_INGOT) == 2, "Gold did not move");
            assertTrue(count(rightChest, Items.IRON_INGOT) == 0, "Iron arrive");
            assertTrue(count(rightChest, Items.GOLD_INGOT) == 0, "Gold arrived");

        });
    }


    @GameTest(template = "3x2x1")
    public static void each_src_retain(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        BlockPos rightPos = new BlockPos(0, 2, 0);
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        BlockPos leftPos = new BlockPos(2, 2, 0);
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());

        var rightChest = getItemHandler(helper, rightPos);
        var leftChest = getItemHandler(helper, leftPos);

        leftChest.insertItem(0, new ItemStack(Items.IRON_INGOT, 64), false);
        leftChest.insertItem(1, new ItemStack(Items.GOLD_INGOT, 64), false);
        leftChest.insertItem(2, new ItemStack(Items.NETHERITE_INGOT, 64), false);

        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           -- move all ingots
                                           -- keep 2 of each ingot type
                                           INPUT RETAIN 2 EACH *ingot* FROM a
                                           OUTPUT TO b
                                       END
                                   """.stripTrailing().stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .add("b", helper.absolutePos(rightPos))
                .save(Objects.requireNonNull(manager.getDisk()));

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            // left should have 2 of each ingot
            assertTrue(count(leftChest, Items.IRON_INGOT) == 2, "Iron did not move");
            assertTrue(count(leftChest, Items.GOLD_INGOT) == 2, "Gold did not move");
            assertTrue(count(leftChest, Items.NETHERITE_INGOT) == 2, "Netherite did not move");
            // right should have 62 of each ingot
            assertTrue(count(rightChest, Items.IRON_INGOT) == 62, "Iron did not arrive");
            assertTrue(count(rightChest, Items.GOLD_INGOT) == 62, "Gold did not arrive");
            assertTrue(count(rightChest, Items.NETHERITE_INGOT) == 62, "Netherite did not arrive");

        });
    }


    @GameTest(template = "3x2x1")
    public static void each_dest_quantity(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        BlockPos rightPos = new BlockPos(0, 2, 0);
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        BlockPos leftPos = new BlockPos(2, 2, 0);
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());

        var rightChest = getItemHandler(helper, rightPos);
        var leftChest = getItemHandler(helper, leftPos);

        leftChest.insertItem(0, new ItemStack(Items.IRON_INGOT, 64), false);
        leftChest.insertItem(1, new ItemStack(Items.GOLD_INGOT, 64), false);
        leftChest.insertItem(2, new ItemStack(Items.NETHERITE_INGOT, 64), false);

        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           INPUT FROM a
                                           OUTPUT 2 each *ingot* TO b
                                       END
                                   """.stripTrailing().stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .add("b", helper.absolutePos(rightPos))
                .save(Objects.requireNonNull(manager.getDisk()));

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            // left should have 62 of each ingot
            assertTrue(count(leftChest, Items.IRON_INGOT) == 62, "Iron did not move");
            assertTrue(count(leftChest, Items.GOLD_INGOT) == 62, "Gold did not move");
            assertTrue(count(leftChest, Items.NETHERITE_INGOT) == 62, "Netherite did not move");
            // right should have 2 of each ingot
            assertTrue(count(rightChest, Items.IRON_INGOT) == 2, "Iron did not arrive");
            assertTrue(count(rightChest, Items.GOLD_INGOT) == 2, "Gold did not arrive");
            assertTrue(count(rightChest, Items.NETHERITE_INGOT) == 2, "Netherite did not arrive");

        });
    }


    @GameTest(template = "3x2x1")
    public static void each_dest_quantity_retain(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        BlockPos rightPos = new BlockPos(0, 2, 0);
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        BlockPos leftPos = new BlockPos(2, 2, 0);
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());

        var rightChest = getItemHandler(helper, rightPos);
        var leftChest = getItemHandler(helper, leftPos);

        leftChest.insertItem(0, new ItemStack(Items.IRON_INGOT, 2), false);
        leftChest.insertItem(1, new ItemStack(Items.GOLD_INGOT, 2), false);

        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           INPUT FROM a
                                           OUTPUT 2 EACH RETAIN 3 *ingot* TO b
                                       END
                                   """.stripTrailing().stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .add("b", helper.absolutePos(rightPos))
                .save(Objects.requireNonNull(manager.getDisk()));

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            // two of the four ingots should have moved
            // for now we assume that gold will move since it is in the higher slot
            assertTrue(count(leftChest, Items.IRON_INGOT) == 0, "Iron moved");
            assertTrue(count(leftChest, Items.GOLD_INGOT) == 1, "Gold did not move");
            assertTrue(count(rightChest, Items.IRON_INGOT) == 2, "Iron arrive");
            assertTrue(count(rightChest, Items.GOLD_INGOT) == 1, "Gold did not arrive");

        });
    }


    @GameTest(template = "3x2x1")
    public static void each_dest_quantity_each_retain(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        BlockPos rightPos = new BlockPos(0, 2, 0);
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        BlockPos leftPos = new BlockPos(2, 2, 0);
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());

        var rightChest = getItemHandler(helper, rightPos);
        var leftChest = getItemHandler(helper, leftPos);

        leftChest.insertItem(0, new ItemStack(Items.IRON_INGOT, 8), false);
        leftChest.insertItem(1, new ItemStack(Items.GOLD_INGOT, 8), false);

        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           INPUT FROM a
                                           OUTPUT 4 EACH RETAIN 2 EACH *ingot* TO b
                                       END
                                   """.stripTrailing().stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .add("b", helper.absolutePos(rightPos))
                .save(Objects.requireNonNull(manager.getDisk()));

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            // two of the four ingots should have moved
            // for now we assume that gold will move since it is in the higher slot
            assertTrue(count(leftChest, Items.IRON_INGOT) == 6, "Iron depart fail");
            assertTrue(count(leftChest, Items.GOLD_INGOT) == 6, "Gold depart fail");
            assertTrue(count(rightChest, Items.IRON_INGOT) == 2, "Iron arrive fail");
            assertTrue(count(rightChest, Items.GOLD_INGOT) == 2, "Gold arrive fail");

        });
    }


    @GameTest(template = "3x2x1")
    public static void each_dest_retain(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        BlockPos rightPos = new BlockPos(0, 2, 0);
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        BlockPos leftPos = new BlockPos(2, 2, 0);
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());

        var rightChest = getItemHandler(helper, rightPos);
        var leftChest = getItemHandler(helper, leftPos);

        leftChest.insertItem(0, new ItemStack(Items.IRON_INGOT, 64), false);
        leftChest.insertItem(1, new ItemStack(Items.GOLD_INGOT, 64), false);
        leftChest.insertItem(2, new ItemStack(Items.NETHERITE_INGOT, 64), false);

        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           INPUT FROM a
                                           OUTPUT RETAIN 2 EACH *ingot* TO b
                                       END
                                   """.stripTrailing().stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .add("b", helper.absolutePos(rightPos))
                .save(Objects.requireNonNull(manager.getDisk()));

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            // left should have 2 of each ingot
            assertTrue(count(leftChest, Items.IRON_INGOT) == 62, "Iron did not move");
            assertTrue(count(leftChest, Items.GOLD_INGOT) == 62, "Gold did not move");
            assertTrue(count(leftChest, Items.NETHERITE_INGOT) == 62, "Netherite did not move");
            // right should have 62 of each ingot
            assertTrue(count(rightChest, Items.IRON_INGOT) == 2, "Iron did not arrive");
            assertTrue(count(rightChest, Items.GOLD_INGOT) == 2, "Gold did not arrive");
            assertTrue(count(rightChest, Items.NETHERITE_INGOT) == 2, "Netherite did not arrive");

        });
    }


    @GameTest(template = "3x2x1")
    public static void sfm_v4_12_0_changelog(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        BlockPos rightPos = new BlockPos(0, 2, 0);
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        BlockPos leftPos = new BlockPos(2, 2, 0);
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());

        var rightChest = getItemHandler(helper, rightPos);
        var leftChest = getItemHandler(helper, leftPos);

        Item[] items = new Item[]{
                Items.NETHERITE_INGOT,
                Items.NETHERITE_INGOT,
                Items.GOLD_INGOT,
                Items.GOLD_INGOT,
                Items.COPPER_INGOT,
                Items.COPPER_INGOT,
                Items.SANDSTONE,
                Items.STONE,
                Items.COBBLESTONE,
                Items.OAK_LOG,
                Items.DARK_OAK_LOG,
                Items.ACACIA_LOG,
                };
        for (int i = 0; i < items.length; i++) {
            leftChest.insertItem(i, new ItemStack(items[i], 64), false);
        }

        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                       NAME "SFM 4.12.0 change overview"
                                       EVERY 20 TICKS DO
                                           INPUT fluid:: FROM a
                                           INPUT 1 *log FROM a
                                           INPUT EXCEPT *log FROM a
                                           OUTPUT
                                               1 EACH minecraft:*ingot,
                                               1 EACH RETAIN 12 EACH minecraft:*stone,
                                               *log,
                                           EXCEPT cobblestone, iron_ingot,
                                           TO b
                                       END
                                   """.stripTrailing().stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .add("b", helper.absolutePos(rightPos))
                .save(Objects.requireNonNull(manager.getDisk()));

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            Item[] expected = new Item[]{
                    Items.NETHERITE_INGOT,
                    Items.GOLD_INGOT,
                    Items.COPPER_INGOT,
                    Items.SANDSTONE,
                    Items.STONE,
                    Items.OAK_LOG,
                    };
            int[] found = new int[expected.length];
            slots:
            for (int i = 0; i < rightChest.getSlots(); i++) {
                ItemStack stack = rightChest.getStackInSlot(i);
                if (stack.isEmpty()) continue;
                Item item = stack.getItem();
                for (int j = 0; j < expected.length; j++) {
                    if (item == expected[j]) {
                        found[j] += stack.getCount();
                        continue slots;
                    }
                }
                assertTrue(false, "Unexpected item in chest: " + item);
            }
            for (int i = 0; i < found.length; i++) {
                assertTrue(found[i] == 1, "Expected " + expected[i] + " to be 1, but was " + found[i]);
            }

        });
    }


    @GameTest(template = "3x2x1")
    public static void forget_input_count_state(GameTestHelper helper) {
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
                                           INPUT 10 FROM a,b
                                           OUTPUT 1 to z
                                           FORGET b
                                           OUTPUT to z
                                       END
                                   """.stripTrailing().stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .add("b", helper.absolutePos(leftPos))
                .add("z", helper.absolutePos(rightPos))
                .save(Objects.requireNonNull(manager.getDisk()));

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            assertTrue(leftChest.getStackInSlot(0).getCount() == 64 - 10, "did not remain");
            assertTrue(rightChest.getStackInSlot(0).getCount() == 10, "did not arrive");

        });
    }

    @GameTest(template = "3x2x1")
    public static void reorder_1(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        BlockPos rightPos = new BlockPos(0, 2, 0);
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        BlockPos leftPos = new BlockPos(2, 2, 0);
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());

        var rightChest = getItemHandler(helper, rightPos);
        var leftChest = getItemHandler(helper, leftPos);

        leftChest.insertItem(0, new ItemStack(Items.IRON_INGOT, 64), false);

        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           FROM a
                                           INPUT iron_ingot
                                   
                                           TO b OUTPUT
                                       END
                                   """.stripTrailing().stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .add("b", helper.absolutePos(rightPos))
                .save(Objects.requireNonNull(manager.getDisk()));

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            assertTrue(leftChest.getStackInSlot(0).isEmpty(), "should depart");
            assertTrue(rightChest.getStackInSlot(0).getCount() == 64, "should arrive in size");
            assertTrue(rightChest.getStackInSlot(0).getItem() == Items.IRON_INGOT, "should arrive in type");

        });
    }

    @GameTest(template = "3x2x1")
    public static void reorder_2(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        BlockPos rightPos = new BlockPos(0, 2, 0);
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        BlockPos leftPos = new BlockPos(2, 2, 0);
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());

        var rightChest = getItemHandler(helper, rightPos);
        var leftChest = getItemHandler(helper, leftPos);

        leftChest.insertItem(0, new ItemStack(Items.IRON_INGOT, 64), false);

        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           FROM a
                                           INPUT iron_ingot
                                   
                                           OUTPUT TO b
                                       END
                                   """.stripTrailing().stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .add("b", helper.absolutePos(rightPos))
                .save(Objects.requireNonNull(manager.getDisk()));

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            assertTrue(leftChest.getStackInSlot(0).isEmpty(), "should depart");
            assertTrue(rightChest.getStackInSlot(0).getCount() == 64, "should arrive in size");
            assertTrue(rightChest.getStackInSlot(0).getItem() == Items.IRON_INGOT, "should arrive in type");

        });
    }

    @GameTest(template = "3x2x1")
    public static void reorder_3(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        BlockPos rightPos = new BlockPos(0, 2, 0);
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        BlockPos leftPos = new BlockPos(2, 2, 0);
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());

        var rightChest = getItemHandler(helper, rightPos);
        var leftChest = getItemHandler(helper, leftPos);

        leftChest.insertItem(0, new ItemStack(Items.IRON_INGOT, 64), false);

        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           INPUT FROM a
                                   
                                           TO b
                                           OUTPUT iron_ingot
                                       END
                                   """.stripTrailing().stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .add("b", helper.absolutePos(rightPos))
                .save(Objects.requireNonNull(manager.getDisk()));

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            assertTrue(leftChest.getStackInSlot(0).isEmpty(), "should depart");
            assertTrue(rightChest.getStackInSlot(0).getCount() == 64, "should arrive in size");
            assertTrue(rightChest.getStackInSlot(0).getItem() == Items.IRON_INGOT, "should arrive in type");

        });
    }


    @GameTest(template = "3x4x3")
    public static void round_robin_by_block_1(GameTestHelper helper) {
        BlockPos managerPos = new BlockPos(1, 2, 1);
        BlockPos sourcePos = new BlockPos(1, 3, 1);
        BlockPos dest1Pos = new BlockPos(2, 2, 1);
        BlockPos dest2Pos = new BlockPos(0, 2, 1);

        // set up inventories
        helper.setBlock(sourcePos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(dest1Pos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(dest2Pos, SFMBlocks.TEST_BARREL_BLOCK.get());


        var sourceInv = getItemHandler(helper, sourcePos);

        var dest1Inv = getItemHandler(helper, dest1Pos);

        var dest2Inv = getItemHandler(helper, dest2Pos);

        for (int i = 0; i < sourceInv.getSlots(); i++) {
            sourceInv.insertItem(i, new ItemStack(Blocks.DIRT, 64), false);
        }

        // set up manager
        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());
        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(managerPos);
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           INPUT FROM source
                                           OUTPUT 128 dirt TO dest ROUND ROBIN BY BLOCK
                                       END
                                   """.stripTrailing().stripIndent());
        // set the labels
        LabelPositionHolder.empty()
                .add("source", helper.absolutePos(sourcePos))
                .add("dest", helper.absolutePos(dest1Pos))
                .add("dest", helper.absolutePos(dest2Pos))
                .save(Objects.requireNonNull(manager.getDisk()));

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            assertTrue(count(sourceInv, Items.DIRT) == 64 * (27 - 2), "source count bad");
            int count1 = count(dest1Inv, Items.DIRT);
            int count2 = count(dest2Inv, Items.DIRT);
            assertTrue(count1 == 128 && count2 == 0 || count1 == 0 && count2 == 128, "first tick arrival count bad");


        });
    }


    @GameTest(template = "3x4x3")
    public static void round_robin_by_block_2(GameTestHelper helper) {
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                helper.setBlock(x, 1, z, SFMBlocks.CABLE_BLOCK.get());
            }
        }
        BlockPos managerPos = new BlockPos(0, 2, 2);
        BlockPos sourcePos = new BlockPos(2, 2, 0);
        BlockPos a1Pos = new BlockPos(0, 2, 0);
        BlockPos a2Pos = new BlockPos(0, 2, 1);
        BlockPos b1Pos = new BlockPos(1, 2, 2);
        BlockPos b2Pos = new BlockPos(2, 2, 2);

        // set up inventories
        helper.setBlock(sourcePos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(a1Pos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(a2Pos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(b1Pos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(b2Pos, SFMBlocks.TEST_BARREL_BLOCK.get());


        var sourceInv = getItemHandler(helper, sourcePos);

        var a1 = getItemHandler(helper, a1Pos);
        var a2 = getItemHandler(helper, a2Pos);
        var b1 = getItemHandler(helper, b1Pos);
        var b2 = getItemHandler(helper, b2Pos);

        for (int i = 0; i < sourceInv.getSlots(); i++) {
            sourceInv.insertItem(i, new ItemStack(Blocks.DIRT, 64), false);
        }

        // set up manager
        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());
        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(managerPos);
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           INPUT FROM source
                                           OUTPUT 128 dirt TO EACH a,b ROUND ROBIN BY BLOCK
                                       END
                                   """.stripTrailing().stripIndent());
        // set the labels
        LabelPositionHolder.empty()
                .add("source", helper.absolutePos(sourcePos))
                .add("a", helper.absolutePos(a1Pos))
                .add("a", helper.absolutePos(a2Pos))
                .add("b", helper.absolutePos(b1Pos))
                .add("b", helper.absolutePos(b2Pos))
                .save(Objects.requireNonNull(manager.getDisk()));

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            assertTrue(count(sourceInv, Items.DIRT) == 64 * (27 - 2), "source count bad");
            int a1Count = count(a1, Items.DIRT);
            int a2Count = count(a2, Items.DIRT);
            int b1Count = count(b1, Items.DIRT);
            int b2Count = count(b2, Items.DIRT);
            // only one of a1, a2, b1, b2 must be 128, rest must be zero
            boolean good = (a1Count == 128 && a2Count == 0 && b1Count == 0 && b2Count == 0) ||
                           (a1Count == 0 && a2Count == 128 && b1Count == 0 && b2Count == 0) ||
                           (a1Count == 0 && a2Count == 0 && b1Count == 128 && b2Count == 0) ||
                           (a1Count == 0 && a2Count == 0 && b1Count == 0 && b2Count == 128);
            assertTrue(good, "first tick arrival count bad");

        });
    }

    @GameTest(template = "3x2x1")
    public static void round_robin_no_blocks_crash_regression(GameTestHelper helper) {
        BlockPos leftPos = new BlockPos(2, 2, 0);
        BlockPos managerPos = new BlockPos(1, 2, 0);
        BlockPos rightPos = new BlockPos(0, 2, 0);

        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());

        var leftChest = getItemHandler(helper, leftPos);
        leftChest.insertItem(0, new ItemStack(Blocks.DIRT, 64), false);

        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(managerPos);
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           INPUT FROM d,e ROUND ROBIN BY BLOCK
                                           OUTPUT TO f,g,h ROUND ROBIN BY LABEL
                                       END
                                   """.stripTrailing().stripIndent());

        // set labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .save(Objects.requireNonNull(manager.getDisk()));

        // it should not crash
        succeedIfManagerDidThingWithoutLagging(helper, manager, helper::succeed);
    }

    @GameTest(template = "3x4x3")
    public static void round_robin_by_label(GameTestHelper helper) {
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                helper.setBlock(x, 1, z, SFMBlocks.CABLE_BLOCK.get());
            }
        }
        BlockPos managerPos = new BlockPos(0, 2, 2);
        BlockPos sourcePos = new BlockPos(2, 2, 0);
        BlockPos a1Pos = new BlockPos(0, 2, 0);
        BlockPos a2Pos = new BlockPos(0, 2, 1);
        BlockPos b1Pos = new BlockPos(1, 2, 2);
        BlockPos b2Pos = new BlockPos(2, 2, 2);

        // set up inventories
        helper.setBlock(sourcePos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(a1Pos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(a2Pos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(b1Pos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(b2Pos, SFMBlocks.TEST_BARREL_BLOCK.get());


        var sourceInv = getItemHandler(helper, sourcePos);

        var a1 = getItemHandler(helper, a1Pos);
        var a2 = getItemHandler(helper, a2Pos);
        var b1 = getItemHandler(helper, b1Pos);
        var b2 = getItemHandler(helper, b2Pos);

        for (int i = 0; i < sourceInv.getSlots(); i++) {
            sourceInv.insertItem(i, new ItemStack(Blocks.DIRT, 64), false);
        }

        // set up manager
        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());
        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(managerPos);
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           INPUT FROM source
                                           OUTPUT 128 dirt TO EACH a,b ROUND ROBIN BY LABEL
                                       END
                                   """.stripTrailing().stripIndent());
        // set the labels
        LabelPositionHolder.empty()
                .add("source", helper.absolutePos(sourcePos))
                .add("a", helper.absolutePos(a1Pos))
                .add("a", helper.absolutePos(a2Pos))
                .add("b", helper.absolutePos(b1Pos))
                .add("b", helper.absolutePos(b2Pos))
                .save(Objects.requireNonNull(manager.getDisk()));

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            assertTrue(count(sourceInv, Items.DIRT) == 64 * (27 - 4), "source count bad");
            // we make no guarantees about which one ticks first
            // we guarantee only one of a or b receives on the first tick
            boolean condition1 = count(a1, Items.DIRT) == 128 && count(a2, Items.DIRT) == 128
                                 && count(b1, Items.DIRT) == 0 && count(b2, Items.DIRT) == 0;
            boolean condition2 = count(b1, Items.DIRT) == 128 && count(b2, Items.DIRT) == 128
                                 && count(a1, Items.DIRT) == 0 && count(a2, Items.DIRT) == 0;
            assertTrue(condition1 || condition2, "Arrival counts bad");
        });
    }


    @GameTest(template = "3x2x1")
    public static void wireless_regression(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        BlockPos rightPos = new BlockPos(0, 2, 0);
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        BlockPos leftPos = new BlockPos(2, 1, 0);
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());

        var rightChest = getItemHandler(helper, rightPos);
        var leftChest = getItemHandler(helper, leftPos);

        leftChest.insertItem(0, new ItemStack(Blocks.DIRT, 64), false);

        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           INPUT FROM a
                                           OUTPUT TO b
                                       END
                                   """.stripTrailing().stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .add("b", helper.absolutePos(rightPos))
                .save(Objects.requireNonNull(manager.getDisk()));

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            assertTrue(!leftChest.getStackInSlot(0).isEmpty(), "Dirt should not move");
            assertTrue(rightChest.getStackInSlot(0).getCount() != 64, "Dirt should not move");

        });
    }

    @GameTest(template = "3x2x1")
    public static void multi_io_limits(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        BlockPos rightPos = new BlockPos(0, 2, 0);
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        BlockPos leftPos = new BlockPos(2, 2, 0);
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());

        var rightChest = getItemHandler(helper, rightPos);
        var leftChest = getItemHandler(helper, leftPos);

        leftChest.insertItem(0, new ItemStack(Blocks.DIRT, 64), false);
        leftChest.insertItem(1, new ItemStack(Blocks.DIRT, 64), false);

        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           INPUT 64 FROM a
                                           OUTPUT RETAIN 63 TO b slots 0
                                           OUTPUT TO b slots 1-99
                                       END
                                   """.stripTrailing().stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .add("b", helper.absolutePos(rightPos))
                .save(Objects.requireNonNull(manager.getDisk()));

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            assertTrue(leftChest.getStackInSlot(0).isEmpty(), "Dirt slot 0 must move");
            assertTrue(leftChest.getStackInSlot(1).getCount() == 64, "Dirt slot 1 must not move");
            assertTrue(rightChest.getStackInSlot(0).getCount() == 63, "Dirt slot 0 must arrive");
            assertTrue(rightChest.getStackInSlot(1).getCount() == 1, "Dirt slot 1 must arrive");
            assertTrue(rightChest.getStackInSlot(2).isEmpty(), "Dirt slot 2 must not arrive");

        });
    }

    @GameTest(template = "3x4x3")
    public static void move_on_pulse(GameTestHelper helper) {
        var managerPos = new BlockPos(1, 2, 1);
        var buttonPos = managerPos.offset(Direction.NORTH.getNormal());
        var leftPos = new BlockPos(2, 2, 1);
        var rightPos = new BlockPos(0, 2, 1);

        // place and fill the chests
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        var left = (BarrelBlockEntity) helper.getBlockEntity(leftPos);
        var right = (BarrelBlockEntity) helper.getBlockEntity(rightPos);
        left.setItem(0, new ItemStack(Items.IRON_INGOT, 64));

        // create the manager block and add the disk
        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());
        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(managerPos);
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));

        // create the program
        var program = """
                    NAME "move on pulse"
                
                    EVERY REDSTONE PULSE DO
                        INPUT FROM left
                        OUTPUT TO right
                    END
                """.stripTrailing().stripIndent();

        // set the labels
        LabelPositionHolder.empty()
                .add("left", helper.absolutePos(leftPos))
                .add("right", helper.absolutePos(rightPos))
                .save(Objects.requireNonNull(manager.getDisk()));

        // load the program
        manager.setProgram(program);
        manager.setLogLevel(Level.TRACE);
        assertTrue(manager.logger.getLogLevel() == Level.TRACE, "Log level should be trace");

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            assertTrue(left.getItem(0).isEmpty(), "Iron should depart");
            assertTrue(right.getItem(0).getCount() == 64, "Iron should arrive");
        });

        // create the button
        helper.setBlock(buttonPos, Blocks.STONE_BUTTON);
        // push the button
        helper.pressButton(buttonPos);
    }

    @GameTest(template = "3x2x1", batch = "linting")
    public static void count_execution_paths_1(GameTestHelper helper) {
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
                .save(Objects.requireNonNull(manager.getDisk()));

        // load the program
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           INPUT FROM left
                                           OUTPUT TO right
                                       END
                                   """.stripTrailing().stripIndent());
        assertManagerRunning(manager);
        var program = manager.getProgram();

        // ensure no warnings
        var warnings = DiskItem.getWarnings(Objects.requireNonNull(manager.getDisk()));
        assertTrue(warnings.isEmpty(), "expected 0 warning, got " + warnings.size());

        // count the execution paths
        GatherWarningsProgramBehaviour simulation = new GatherWarningsProgramBehaviour(warnings::addAll);
        program.tick(ProgramContext.createSimulationContext(
                program,
                labelPositionHolder,
                0,
                simulation
        ));
        assertTrue(simulation.getSeenPaths().size() == 1, "expected single execution path");
        assertTrue(simulation.getSeenPaths().get(0).history().size() == 2, "expected two elements in execution path");
        helper.succeed();
    }

    @GameTest(template = "3x2x1", batch = "linting")
    public static void count_execution_paths_2(GameTestHelper helper) {
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
                .save(Objects.requireNonNull(manager.getDisk()));

        // load the program
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           INPUT FROM left
                                           OUTPUT TO right
                                       END
                                       EVERY 20 TICKS DO
                                           INPUT FROM left
                                           OUTPUT TO right
                                           OUTPUT TO right
                                       END
                                   """.stripTrailing().stripIndent());
        assertManagerRunning(manager);
        var program = manager.getProgram();

        // ensure no warnings
        var warnings = DiskItem.getWarnings(Objects.requireNonNull(manager.getDisk()));
        assertTrue(warnings.isEmpty(), "expected 0 warning, got " + warnings.size());

        // count the execution paths
        GatherWarningsProgramBehaviour simulation = new GatherWarningsProgramBehaviour(warnings::addAll);
        program.tick(ProgramContext.createSimulationContext(
                program,
                labelPositionHolder,
                0,
                simulation
        ));
        assertTrue(simulation.getSeenPaths().size() == 2, "expected single execution path");
        assertTrue(simulation.getSeenPaths().get(0).history().size() == 2, "expected two elements in execution path");
        assertTrue(simulation.getSeenPaths().get(1).history().size() == 3, "expected two elements in execution path");
        helper.succeed();
    }

    @GameTest(template = "3x2x1", batch = "linting")
    public static void count_execution_paths_3(GameTestHelper helper) {
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
                .save(Objects.requireNonNull(manager.getDisk()));

        // load the program
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           INPUT FROM left
                                           OUTPUT TO right
                                       END
                                       EVERY 20 TICKS DO
                                           INPUT FROM left
                                           INPUT FROM left
                                           OUTPUT TO right
                                           OUTPUT TO right
                                       END
                                       EVERY 20 TICKS DO
                                           INPUT FROM left
                                           INPUT FROM left
                                           OUTPUT TO right
                                       END
                                   """.stripTrailing().stripIndent());
        assertManagerRunning(manager);
        var program = manager.getProgram();

        // ensure no warnings
        var warnings = DiskItem.getWarnings(Objects.requireNonNull(manager.getDisk()));
        assertTrue(warnings.isEmpty(), "expected 0 warning, got " + warnings.size());

        // count the execution paths
        GatherWarningsProgramBehaviour simulation = new GatherWarningsProgramBehaviour(warnings::addAll);
        program.tick(ProgramContext.createSimulationContext(
                program,
                labelPositionHolder,
                0,
                simulation
        ));
        assertTrue(simulation.getSeenPaths().size() == 3, "expected single execution path");
        assertTrue(simulation.getSeenPaths().get(0).history().size() == 2, "expected two elements in execution path");
        assertTrue(simulation.getSeenPaths().get(1).history().size() == 4, "expected two elements in execution path");
        assertTrue(simulation.getSeenPaths().get(2).history().size() == 3, "expected two elements in execution path");
        helper.succeed();
    }


    @GameTest(template = "3x2x1", batch = "linting")
    public static void unused_io_warning_output_label_not_presnet_in_input(GameTestHelper helper) {
        // place inventories
        helper.setBlock(new BlockPos(1, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        BlockPos rightPos = new BlockPos(0, 2, 0);
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        BlockPos leftPos = new BlockPos(2, 2, 0);
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());

        // place manager
        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        LabelPositionHolder.empty()
                .add("bruh", helper.absolutePos(leftPos))
                .save(Objects.requireNonNull(manager.getDisk()));
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           OUTPUT TO bruh
                                       END
                                   """.stripTrailing().stripIndent());
        assertManagerRunning(manager);

        // assert expected warnings
        var warnings = DiskItem.getWarnings(Objects.requireNonNull(manager.getDisk()));
        assertTrue(warnings.size() == 1, "expected 1 warning, got " + warnings.size());
        assertTrue(warnings
                           .get(0)
                           .getKey()
                           .equals(LocalizationKeys.PROGRAM_WARNING_OUTPUT_RESOURCE_TYPE_NOT_FOUND_IN_INPUTS
                                           .key()
                                           .get()), "expected output without matching input warning");
        helper.succeed();
    }


    @GameTest(template = "3x2x1", batch = "linting")
    public static void unused_io_warning_input_label_not_present_in_output(GameTestHelper helper) {
        // place inventories
        helper.setBlock(new BlockPos(1, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        BlockPos rightPos = new BlockPos(0, 2, 0);
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        BlockPos leftPos = new BlockPos(2, 2, 0);
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());

        // place manager
        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        LabelPositionHolder.empty()
                .add("left", helper.absolutePos(leftPos))
                .save(Objects.requireNonNull(manager.getDisk()));
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           INPUT FROM left
                                       END
                                   """.stripTrailing().stripIndent());
        assertManagerRunning(manager);

        // assert expected warnings
        var warnings = DiskItem.getWarnings(Objects.requireNonNull(manager.getDisk()));
        assertTrue(warnings.size() == 1, "expected 1 warning, got " + warnings.size());
        assertTrue(warnings
                           .get(0)
                           .getKey()
                           .equals(LocalizationKeys.PROGRAM_WARNING_UNUSED_INPUT_LABEL // should be unused input
                                           .key()
                                           .get()), "expected output without matching input warning");
        helper.succeed();
    }


    @GameTest(template = "1x2x1")
    public static void disk_name(GameTestHelper helper) {
        BlockPos chestPos = new BlockPos(0, 2, 0);
        helper.setBlock(chestPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        var chest = getItemHandler(helper, chestPos);

        {
            ItemStack disk = new ItemStack(SFMItems.DISK_ITEM.get());
            String programString = """
                    NAME "bruh"
                    EVERY 20 TICKS DO
                    END
                    """;
            DiskItem.setProgram(disk, programString);
            DiskItem.compileAndUpdateErrorsAndWarnings(disk, null);
            chest.insertItem(0, disk, false);
            assertTrue(DiskItem.getProgramName(disk).equals("bruh"), "program name should be bruh for disk 1");
            assertTrue(DiskItem.getWarnings(disk).isEmpty(), "there should be no warnings on disk 1");
            assertTrue(DiskItem.getErrors(disk).isEmpty(), "there should be no errors on disk 1");
            assertTrue(disk.getHoverName().getString().equals("bruh"), "display name should be \"bruh\" for disk 1");
        }
        {
            ItemStack disk = new ItemStack(SFMItems.DISK_ITEM.get());
            String programString = """
                    EVERY 20 TICKS DO
                    END
                    """;
            DiskItem.setProgram(disk, programString);
            DiskItem.compileAndUpdateErrorsAndWarnings(disk, null);
            chest.insertItem(1, disk, false);
            assertTrue(DiskItem.getProgramName(disk).isEmpty(), "program name should be empty for disk 2");
            assertTrue(DiskItem.getWarnings(disk).isEmpty(), "there should be no warnings on disk 2");
            assertTrue(DiskItem.getErrors(disk).isEmpty(), "there should be no errors on disk 2");
            assertTrue(
                    disk.getHoverName().contains(LocalizationKeys.DISK_ITEM.getComponent()),
                    "display name should be default for disk 2"
            );
        }
        helper.succeed();
    }

    @GameTest(template = "7x3x3")
    public static void regression_input_retain_b_shared_shared(GameTestHelper helper) {
        BlockPos managerPos = new BlockPos(1, 2, 1);
        BlockPos aPos = new BlockPos(2, 2, 1);
        BlockPos b1Pos = new BlockPos(4, 2, 1);
        BlockPos b2Pos = new BlockPos(5, 2, 1);
        BlockPos b3Pos = new BlockPos(6, 2, 1);

        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());
        helper.setBlock(aPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(b1Pos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(b2Pos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(b3Pos, SFMBlocks.TEST_BARREL_BLOCK.get());

        for (int i = 0; i < 6; i++) {
            helper.setBlock(new BlockPos(1 + i, 2, 2), SFMBlocks.CABLE_BLOCK.get());
        }

        var a = getItemHandler(helper, aPos);
        var b1 = getItemHandler(helper, b1Pos);
        var b2 = getItemHandler(helper, b2Pos);
        var b3 = getItemHandler(helper, b3Pos);

        for (int i = 0; i < 5; i++) {
            b1.insertItem(i, new ItemStack(Items.DIRT, 64), false);
            b2.insertItem(i, new ItemStack(Items.DIRT, 64), false);
            b3.insertItem(i, new ItemStack(Items.DIRT, 64), false);
        }

        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(managerPos);
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           INPUT RETAIN 5 FROM b
                                           OUTPUT TO a
                                       END
                                   """.stripTrailing().stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(aPos))
                .add("b", helper.absolutePos(b1Pos))
                .add("b", helper.absolutePos(b2Pos))
                .add("b", helper.absolutePos(b3Pos))
                .save(Objects.requireNonNull(manager.getDisk()));

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            // There should be exactly 5 dirt across all b
            // The rest should be in a
            assertTrue(count(a, Items.DIRT) == 64 * 3 * 5 - 5, "dirt should arrive in a");
            int bDirt = count(b1, Items.DIRT) + count(b2, Items.DIRT) + count(b3, Items.DIRT);
            assertTrue(bDirt == 5, "dirt should depart from b");
        });
    }

    @GameTest(template = "7x3x3")
    public static void regression_input_retain_b_shared_expanded(GameTestHelper helper) {
        BlockPos managerPos = new BlockPos(1, 2, 1);
        BlockPos aPos = new BlockPos(2, 2, 1);
        BlockPos b1Pos = new BlockPos(4, 2, 1);
        BlockPos b2Pos = new BlockPos(5, 2, 1);
        BlockPos b3Pos = new BlockPos(6, 2, 1);

        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());
        helper.setBlock(aPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(b1Pos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(b2Pos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(b3Pos, SFMBlocks.TEST_BARREL_BLOCK.get());

        for (int i = 0; i < 6; i++) {
            helper.setBlock(new BlockPos(1 + i, 2, 2), SFMBlocks.CABLE_BLOCK.get());
        }

        var a = getItemHandler(helper, aPos);
        var b1 = getItemHandler(helper, b1Pos);
        var b2 = getItemHandler(helper, b2Pos);
        var b3 = getItemHandler(helper, b3Pos);

        for (int i = 0; i < 5; i++) {
            b1.insertItem(i, new ItemStack(Items.DIRT, 64), false);
            b2.insertItem(i, new ItemStack(Items.DIRT, 64), false);
            b3.insertItem(i, new ItemStack(Items.DIRT, 64), false);
        }

        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(managerPos);
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           INPUT RETAIN 5 EACH FROM b
                                           OUTPUT TO a
                                       END
                                   """.stripTrailing().stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(aPos))
                .add("b", helper.absolutePos(b1Pos))
                .add("b", helper.absolutePos(b2Pos))
                .add("b", helper.absolutePos(b3Pos))
                .save(Objects.requireNonNull(manager.getDisk()));

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            // There should be exactly 5 dirt across all b
            // The rest should be in a
            assertTrue(count(a, Items.DIRT) == 64 * 3 * 5 - 5, "dirt should arrive in a");
            int bDirt = count(b1, Items.DIRT) + count(b2, Items.DIRT) + count(b3, Items.DIRT);
            assertTrue(bDirt == 5, "dirt should depart from b");
        });
    }


    @GameTest(template = "7x3x3")
    public static void regression_input_retain_b_expanded_shared(GameTestHelper helper) {
        BlockPos managerPos = new BlockPos(1, 2, 1);
        BlockPos aPos = new BlockPos(2, 2, 1);
        BlockPos b1Pos = new BlockPos(4, 2, 1);
        BlockPos b2Pos = new BlockPos(5, 2, 1);
        BlockPos b3Pos = new BlockPos(6, 2, 1);

        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());
        helper.setBlock(aPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(b1Pos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(b2Pos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(b3Pos, SFMBlocks.TEST_BARREL_BLOCK.get());

        for (int i = 0; i < 6; i++) {
            helper.setBlock(new BlockPos(1 + i, 2, 2), SFMBlocks.CABLE_BLOCK.get());
        }

        var a = getItemHandler(helper, aPos);
        var b1 = getItemHandler(helper, b1Pos);
        var b2 = getItemHandler(helper, b2Pos);
        var b3 = getItemHandler(helper, b3Pos);

        for (int i = 0; i < 5; i++) {
            b1.insertItem(i, new ItemStack(Items.DIRT, 64), false);
            b2.insertItem(i, new ItemStack(Items.DIRT, 64), false);
            b3.insertItem(i, new ItemStack(Items.DIRT, 64), false);
        }

        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(managerPos);
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           INPUT 9999 EACH RETAIN 5 FROM b
                                           OUTPUT TO a
                                       END
                                   """.stripTrailing().stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(aPos))
                .add("b", helper.absolutePos(b1Pos))
                .add("b", helper.absolutePos(b2Pos))
                .add("b", helper.absolutePos(b3Pos))
                .save(Objects.requireNonNull(manager.getDisk()));

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            // There should be exactly 5 dirt across all b
            // The rest should be in a
            assertTrue(count(a, Items.DIRT) == 64 * 3 * 5 - 5, "dirt should arrive in a");
            int bDirt = count(b1, Items.DIRT) + count(b2, Items.DIRT) + count(b3, Items.DIRT);
            assertTrue(bDirt == 5, "dirt should depart from b");
        });
    }


    @GameTest(template = "7x3x3")
    public static void regression_input_retain_b_expanded_expanded(GameTestHelper helper) {
        BlockPos managerPos = new BlockPos(1, 2, 1);
        BlockPos aPos = new BlockPos(2, 2, 1);
        BlockPos b1Pos = new BlockPos(4, 2, 1);
        BlockPos b2Pos = new BlockPos(5, 2, 1);
        BlockPos b3Pos = new BlockPos(6, 2, 1);

        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());
        helper.setBlock(aPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(b1Pos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(b2Pos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(b3Pos, SFMBlocks.TEST_BARREL_BLOCK.get());

        for (int i = 0; i < 6; i++) {
            helper.setBlock(new BlockPos(1 + i, 2, 2), SFMBlocks.CABLE_BLOCK.get());
        }

        var a = getItemHandler(helper, aPos);
        var b1 = getItemHandler(helper, b1Pos);
        var b2 = getItemHandler(helper, b2Pos);
        var b3 = getItemHandler(helper, b3Pos);

        for (int i = 0; i < 5; i++) {
            b1.insertItem(i, new ItemStack(Items.DIRT, 64), false);
            b2.insertItem(i, new ItemStack(Items.DIRT, 64), false);
            b3.insertItem(i, new ItemStack(Items.DIRT, 64), false);
        }

        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(managerPos);
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           INPUT 9999 EACH RETAIN 5 EACH FROM b
                                           OUTPUT TO a
                                       END
                                   """.stripTrailing().stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(aPos))
                .add("b", helper.absolutePos(b1Pos))
                .add("b", helper.absolutePos(b2Pos))
                .add("b", helper.absolutePos(b3Pos))
                .save(Objects.requireNonNull(manager.getDisk()));

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            // There should be exactly 5 dirt across all b
            // The rest should be in a
            assertTrue(count(a, Items.DIRT) == 64 * 3 * 5 - 5, "dirt should arrive in a");
            int bDirt = count(b1, Items.DIRT) + count(b2, Items.DIRT) + count(b3, Items.DIRT);
            assertTrue(bDirt == 5, "dirt should depart from b");
        });
    }


    @GameTest(template = "3x2x1")
    public static void move_using_or(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        BlockPos rightPos = new BlockPos(0, 2, 0);
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        BlockPos leftPos = new BlockPos(2, 2, 0);
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());

        var rightChest = getItemHandler(helper, rightPos);
        var leftChest = getItemHandler(helper, leftPos);

        leftChest.insertItem(0, new ItemStack(Blocks.DIRT, 64), false);
        leftChest.insertItem(1, new ItemStack(Blocks.STONE, 64), false);
        leftChest.insertItem(2, new ItemStack(Blocks.COBBLESTONE, 64), false);
        leftChest.insertItem(3, new ItemStack(Blocks.COBBLESTONE, 64), false);

        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           INPUT
                                               5 stone or dirt,
                                               cobblestone FROM a
                                           OUTPUT TO b
                                       END
                                   """.stripTrailing().stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .add("b", helper.absolutePos(rightPos))
                .save(Objects.requireNonNull(manager.getDisk()));

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            // count of stone + dirt in left must be 64*2-5
            int leftStoneDirt = count(leftChest, Items.STONE) + count(leftChest, Items.DIRT);
            assertTrue(leftStoneDirt == 64 * 2 - 5, "stone and dirt should depart");
            // count of stone + dirt in right must be 5
            int rightStoneDirt = count(rightChest, Items.STONE) + count(rightChest, Items.DIRT);
            assertTrue(rightStoneDirt == 5, "stone and dirt should arrive");
            // left cobblestone count = 0
            assertTrue(count(leftChest, Items.COBBLESTONE) == 0, "no cobblestone should remain");
            // right cobblestone count = 64*2
            assertTrue(count(rightChest, Items.COBBLESTONE) == 64 * 2, "cobblestone should arrive");
        });
    }

    @GameTest(template = "3x2x1")
    public static void move_using_each_or(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        BlockPos rightPos = new BlockPos(0, 2, 0);
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        BlockPos leftPos = new BlockPos(2, 2, 0);
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());

        var rightChest = getItemHandler(helper, rightPos);
        var leftChest = getItemHandler(helper, leftPos);

        leftChest.insertItem(0, new ItemStack(Blocks.DIRT, 64), false);
        leftChest.insertItem(1, new ItemStack(Blocks.STONE, 64), false);
        leftChest.insertItem(2, new ItemStack(Blocks.COBBLESTONE, 64), false);
        leftChest.insertItem(3, new ItemStack(Blocks.COBBLESTONE, 64), false);

        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           INPUT
                                               5 each stone or dirt,
                                               cobblestone FROM a
                                           OUTPUT TO b
                                       END
                                   """.stripTrailing().stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .add("b", helper.absolutePos(rightPos))
                .save(Objects.requireNonNull(manager.getDisk()));

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            // left dirt count = 64-5
            assertTrue(count(leftChest, Items.DIRT) == 64 - 5, "dirt should depart");
            // left stone count = 64-5
            assertTrue(count(leftChest, Items.STONE) == 64 - 5, "stone should depart");
            // right dirt count = 5
            assertTrue(count(rightChest, Items.DIRT) == 5, "dirt should arrive");
            // right stone count = 5
            assertTrue(count(rightChest, Items.STONE) == 5, "stone should arrive");
            // left cobblestone count = 0
            assertTrue(count(leftChest, Items.COBBLESTONE) == 0, "no cobblestone should remain");
            // right cobblestone count = 64*2
            assertTrue(count(rightChest, Items.COBBLESTONE) == 64 * 2, "cobblestone should arrive");
        });
    }

    @GameTest(template = "7x3x3")
    public static void tunnel_furnace(GameTestHelper helper) {
        BlockPos hopperPos = new BlockPos(0, 4, 0);
        helper.setBlock(hopperPos, Blocks.HOPPER);
        BlockPos managerPos = new BlockPos(0, 3, 0);
        helper.setBlock(managerPos, SFMBlocks.TUNNELLED_MANAGER_BLOCK.get());
        BlockPos barrelPos = new BlockPos(0, 2, 0);
        helper.setBlock(barrelPos, SFMBlocks.TEST_BARREL_BLOCK.get());

        var hopper = getItemHandler(helper, hopperPos);
        var barrel = getItemHandler(helper, barrelPos);

        hopper.insertItem(0, new ItemStack(Blocks.DIRT, 1), false);

        helper.runAfterDelay(8, () -> {
            assertTrue(hopper.getStackInSlot(0).isEmpty(), "Dirt did not move");
            assertTrue(barrel.getStackInSlot(0).getCount() == 1, "Dirt did not move");
        });


        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(managerPos);
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           INPUT FROM barrel
                                           OUTPUT TO hopper
                                       END
                                   """.stripTrailing().stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("barrel", helper.absolutePos(barrelPos))
                .add("hopper", helper.absolutePos(hopperPos))
                .save(Objects.requireNonNull(manager.getDisk()));

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            assertTrue(hopper.getStackInSlot(0).getCount() == 1, "Dirt did not move");
            assertTrue(barrel.getStackInSlot(0).isEmpty(), "Dirt did not move");
        });
    }
}
