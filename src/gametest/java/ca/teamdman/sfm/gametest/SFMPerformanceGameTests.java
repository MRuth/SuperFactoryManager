package ca.teamdman.sfm.gametest;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.common.blockentity.ManagerBlockEntity;
import ca.teamdman.sfm.common.item.DiskItem;
import ca.teamdman.sfm.common.program.LabelPositionHolder;
import ca.teamdman.sfm.common.registry.SFMBlocks;
import ca.teamdman.sfm.common.registry.SFMItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings({"DataFlowIssue", "DuplicatedCode", "DefaultAnnotationParam"})
@GameTestHolder(SFM.MOD_ID)
@PrefixGameTestTemplate(value=true)
public class SFMPerformanceGameTests extends SFMGameTestBase {
    @GameTest(template = "25x3x25", batch = "laggy")
    public static void move_many_inventories(GameTestHelper helper) {
        // fill the platform with cables and barrels
        var sourceBlocks = new ArrayList<BlockPos>();
        var destBlocks = new ArrayList<BlockPos>();
        for (int x = 0; x < 25; x++) {
//            for (int z = 0; z < 25; z++) {
            for (int z = 0; z < 24; z++) {
                helper.setBlock(new BlockPos(x, 2, z), SFMBlocks.CABLE_BLOCK.get());
                helper.setBlock(new BlockPos(x, 3, z), SFMBlocks.TEST_BARREL_BLOCK.get());
                if (z % 2 == 0) {
                    sourceBlocks.add(new BlockPos(x, 3, z));
                    // fill the source chests with ingots
                    BarrelBlockEntity barrel = (BarrelBlockEntity) helper.getBlockEntity(new BlockPos(x, 3, z));
                    for (int i = 0; i < barrel.getContainerSize(); i++) {
                        barrel.setItem(i, new ItemStack(Items.IRON_INGOT, 64));
                    }
                } else {
                    destBlocks.add(new BlockPos(x, 3, z));
                }
            }
        }

        // fill in the blocks needed for the test
        helper.setBlock(new BlockPos(0, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(0, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));

        // create the program
        var program = """
                    NAME "many inventory lag test"
                
                    EVERY 20 TICKS DO
                        INPUT FROM a
                        OUTPUT TO b
                    END
                """.stripTrailing().stripIndent();

        // set the labels
        LabelPositionHolder.empty()
                .addAll("a", sourceBlocks.stream().map(helper::absolutePos).toList())
                .addAll("b", destBlocks.stream().map(helper::absolutePos).toList())
                .save(manager.getDisk());

        // load the program
        manager.setProgram(program);
        assertTrue(
                manager.getState() == ManagerBlockEntity.State.RUNNING,
                "Program did not start running " + DiskItem.getErrors(manager.getDisk())
        );

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            // ensure all the source chests are empty
            sourceBlocks.forEach(pos -> {
                BarrelBlockEntity barrel = (BarrelBlockEntity) helper.getBlockEntity(pos);
                for (int i = 0; i < barrel.getContainerSize(); i++) {
                    ItemStack found = barrel.getItem(i);
                    assertTrue(
                            found.isEmpty(),
                            "Items did not leave, pos=" + helper.absolutePos(pos) + " i=" + i + " found=" + found
                    );
                }
            });
            // ensure all the dest chests are full
            destBlocks.forEach(pos -> {
                BarrelBlockEntity barrel = (BarrelBlockEntity) helper.getBlockEntity(pos);
                for (int i = 0; i < barrel.getContainerSize(); i++) {
                    assertTrue(barrel.getItem(i).getCount() == 64, "Items did not arrive");
                }
            });


        });
    }

    @GameTest(template = "25x3x25", batch = "laggy")
    public static void move_many_full(GameTestHelper helper) {
        // fill the platform with cables and barrels
        var sourceBlocks = new ArrayList<BlockPos>();
        var destBlocks = new ArrayList<BlockPos>();
        for (int x = 0; x < 25; x++) {
//            for (int z = 0; z < 25; z++) {
            for (int z = 0; z < 24; z++) {
                helper.setBlock(new BlockPos(x, 2, z), SFMBlocks.CABLE_BLOCK.get());
                helper.setBlock(new BlockPos(x, 3, z), SFMBlocks.TEST_BARREL_BLOCK.get());
                if (z % 2 == 0) {
                    sourceBlocks.add(new BlockPos(x, 3, z));
                } else {
                    destBlocks.add(new BlockPos(x, 3, z));
                }

                // fill the source chests with ingots
                BarrelBlockEntity barrel = (BarrelBlockEntity) helper.getBlockEntity(new BlockPos(x, 3, z));
                for (int i = 0; i < barrel.getContainerSize(); i++) {
                    barrel.setItem(i, new ItemStack(Items.IRON_INGOT, 64));
                }
            }
        }

        // fill in the blocks needed for the test
        helper.setBlock(new BlockPos(0, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(0, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));

        // create the program
        var program = """
                    NAME "move many full"
                
                    EVERY 20 TICKS DO
                        INPUT FROM a
                        OUTPUT TO b
                    END
                """.stripTrailing().stripIndent();

        // set the labels
        LabelPositionHolder.empty()
                .addAll("a", sourceBlocks.stream().map(helper::absolutePos).toList())
                .addAll("b", destBlocks.stream().map(helper::absolutePos).toList())
                .save(manager.getDisk());

        // load the program
        manager.setProgram(program);
        assertTrue(
                manager.getState() == ManagerBlockEntity.State.RUNNING,
                "Program did not start running " + DiskItem.getErrors(manager.getDisk())
        );

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            // ensure all the source chests are full
            sourceBlocks.forEach(pos -> {
                BarrelBlockEntity barrel = (BarrelBlockEntity) helper.getBlockEntity(pos);
                for (int i = 0; i < barrel.getContainerSize(); i++) {
                    assertTrue(barrel.getItem(i).getCount() == 64, "Items did not stay");
                }
            });
            // ensure all the dest chests are full
            destBlocks.forEach(pos -> {
                BarrelBlockEntity barrel = (BarrelBlockEntity) helper.getBlockEntity(pos);
                for (int i = 0; i < barrel.getContainerSize(); i++) {
                    assertTrue(barrel.getItem(i).getCount() == 64, "Items did not arrive");
                }
            });


        });
    }

    /**
     * Creates many inventories.
     * Half of them will be full, the other half will be empty.
     * The half that is full will have three different items, one type per row:
     * - iron ingots
     * - gold ingots
     * - diamonds
     * The program should use a regular expression to match only ingots in the form *:*_ingot
     */
    @GameTest(template = "25x3x25", batch = "laggy")
    public static void move_many_regex(GameTestHelper helper) {
        // fill the platform with cables and barrels
        var sourceBlocks = new ArrayList<BlockPos>();
        var destBlocks = new ArrayList<BlockPos>();

        var ironIngots = new AtomicInteger(0);
        var goldIngots = new AtomicInteger(0);
        var diamonds = new AtomicInteger(0);

        for (int x = 0; x < 25; x++) {
            for (int z = 0; z < 24; z++) { // make sure we have an even number to split
                // place a cable below
                helper.setBlock(new BlockPos(x, 2, z), SFMBlocks.CABLE_BLOCK.get());
                // place the barrel on top
                helper.setBlock(new BlockPos(x, 3, z), SFMBlocks.TEST_BARREL_BLOCK.get());
                if (z % 2 == 0) {
                    sourceBlocks.add(new BlockPos(x, 3, z));
                    BarrelBlockEntity barrel = (BarrelBlockEntity) helper.getBlockEntity(new BlockPos(x, 3, z));
                    for (int i = 0; i < barrel.getContainerSize(); i++) {
                        if (i % 3 == 0) {
                            barrel.setItem(i, new ItemStack(Items.IRON_INGOT, 64));
                            ironIngots.addAndGet(64);
                        } else if (i % 3 == 1) {
                            barrel.setItem(i, new ItemStack(Items.GOLD_INGOT, 64));
                            goldIngots.addAndGet(64);
                        } else {
                            barrel.setItem(i, new ItemStack(Items.DIAMOND, 64));
                            diamonds.addAndGet(64);
                        }
                    }
                } else {
                    destBlocks.add(new BlockPos(x, 3, z));
                }
            }
        }

        // create the manager block and add the disk
        helper.setBlock(new BlockPos(0, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(0, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));

        // create the program
        var program = """
                    NAME "move many regex"
                
                    EVERY 20 TICKS DO
                        INPUT *:*_ingot FROM a
                        OUTPUT TO b
                    END
                """.stripTrailing().stripIndent();

        // set the labels
        LabelPositionHolder.empty()
                .addAll("a", sourceBlocks.stream().map(helper::absolutePos).toList())
                .addAll("b", destBlocks.stream().map(helper::absolutePos).toList())
                .save(manager.getDisk());

        // load the program
        manager.setProgram(program);
        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            // ensure the source chests only have the non-ingot items
            sourceBlocks.forEach(pos -> {
                BarrelBlockEntity barrel = (BarrelBlockEntity) helper.getBlockEntity(pos);
                for (int i = 0; i < barrel.getContainerSize(); i++) {
                    if (i % 3 == 0) {
                        assertTrue(barrel.getItem(i).isEmpty(), "Items did not depart");
                    } else if (i % 3 == 1) {
                        assertTrue(barrel.getItem(i).isEmpty(), "Items did not depart");
                    } else {
                        assertTrue(barrel.getItem(i).getItem() == Items.DIAMOND, "Non-matching didn't stay");
                    }
                }
            });
            // ensure the destination chests only have the ingot items
            int diamondStart = diamonds.get();
            destBlocks.forEach(pos -> {
                BarrelBlockEntity barrel = (BarrelBlockEntity) helper.getBlockEntity(pos);
                for (int i = 0; i < barrel.getContainerSize(); i++) {
                    Item item = barrel.getItem(i).getItem();
                    if (item == Items.IRON_INGOT) {
                        ironIngots.addAndGet(-barrel.getItem(i).getCount());
                    } else if (item == Items.GOLD_INGOT) {
                        goldIngots.addAndGet(-barrel.getItem(i).getCount());
                    } else if (item == Items.DIAMOND) {
                        diamonds.addAndGet(-barrel.getItem(i).getCount());
                    }
                }
            });
            assertTrue(ironIngots.get() == 0, "Iron ingots did not arrive");
            assertTrue(goldIngots.get() == 0, "Gold ingots did not arrive");
            assertTrue(diamonds.get() == diamondStart, "Diamonds did not stay");
        });
    }

    /**
     * Creates four chests: a, b, c, d
     * a: 9x64 iron ingots
     * b: 9x64 gold ingots
     * c: 9x64 diamonds
     * d: 26x64 cobblestone, 1x64 copper ingot
     * <p>
     * Every 20 ticks, the program should:
     * - move all ingots from a to b, b to c, c to d, d to a
     * <p>
     * The program should use a regular expression to match only ingots in the form *:*_ingot
     */
    @GameTest(template = "3x4x3", batch = "laggy")
    public static void move_regex_circle(GameTestHelper helper) {
        var managerPos = new BlockPos(1, 2, 1);
        var aPos = new BlockPos(1, 2, 0);
        var bPos = new BlockPos(2, 2, 1);
        var cPos = new BlockPos(1, 2, 2);
        var dPos = new BlockPos(0, 2, 1);

        // place and fill the chests
        helper.setBlock(aPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(bPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(cPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(dPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        var a = (BarrelBlockEntity) helper.getBlockEntity(aPos);
        var b = (BarrelBlockEntity) helper.getBlockEntity(bPos);
        var c = (BarrelBlockEntity) helper.getBlockEntity(cPos);
        var d = (BarrelBlockEntity) helper.getBlockEntity(dPos);
        for (int i = 0; i < 27; i++) {
            if (i < 9) {
                a.setItem(i, new ItemStack(Items.IRON_INGOT, 64));
                b.setItem(i, new ItemStack(Items.GOLD_INGOT, 64));
                c.setItem(i, new ItemStack(Items.DIAMOND, 64));
            }
            d.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        }
        d.setItem(26, new ItemStack(Items.COPPER_INGOT, 64));

        // create the manager block and add the disk
        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());
        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(managerPos);
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));

        // create the program
        var program = """
                    NAME "move regex circle"
                
                    EVERY 20 TICKS DO
                        INPUT *:*_ingot FROM a
                        OUTPUT TO b
                    END
                    EVERY 20 TICKS DO
                        INPUT *:*_ingot FROM b
                        OUTPUT TO c
                    END
                    EVERY 20 TICKS DO
                        INPUT *:*_ingot FROM c
                        OUTPUT TO d
                    END
                    EVERY 20 TICKS DO
                        INPUT *:*_ingot FROM d
                        OUTPUT TO a
                    END
                """.stripTrailing().stripIndent();

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(aPos))
                .add("b", helper.absolutePos(bPos))
                .add("c", helper.absolutePos(cPos))
                .add("d", helper.absolutePos(dPos))
                .save(manager.getDisk());

        // load the program
        manager.setProgram(program);

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {

        });
    }

    /**
     * In FTB Skies, I was generating resources using sieves and gravel.
     * This would give a bunch of raw metals, which I would then smelt into ingots.
     * Those ingots would be moved into gold barrels; very large inventories; and I had a lot of them.
     * Then I had a crafting station with its own adjacent gold barrel.
     * I wrote a program to keep that gold barrel stocked with essential ingredients.
     */
    @GameTest(template = "25x4x25")
    public static void gather_supplies(GameTestHelper helper) {
        var items = new Item[]{
                Items.GOLD_INGOT,
                Items.GOLD_BLOCK,
                Items.IRON_INGOT,
                Items.IRON_BLOCK,
                Items.DIAMOND,
                Items.DIAMOND_BLOCK,
                Items.EMERALD,
                Items.EMERALD_BLOCK,
                Items.LAPIS_LAZULI,
                Items.LAPIS_BLOCK,
                Items.REDSTONE,
                Items.REDSTONE_BLOCK,
                Items.COAL,
                Items.COAL_BLOCK,
                Items.NETHERITE_INGOT,
                Items.NETHERITE_BLOCK,
                Items.TORCH,
                Items.BUCKET,
                Items.CHEST,
                Items.CRAFTING_TABLE,
                Items.FURNACE,
                Items.COBBLESTONE,
                Items.OAK_LOG,
                Items.OAK_PLANKS
        };

        // create a bunch of storage inventories
        List<BlockPos> storage = new ArrayList<>();
        int itemIndex = 0;
        for (int i = 0; i < 24; i++) {
            for (int j = 3; j < 25; j++) {
                for (int k = 0; k < 3; k++) {
                    BlockPos pos = new BlockPos(i, k + 2, j);
                    if (j == 3) {
                        if (k == 0) {
                            helper.setBlock(pos, SFMBlocks.CABLE_BLOCK.get());
                        }
                    } else {
                        if (i % 3 == 0 || i % 3 == 2) {
                            helper.setBlock(pos, SFMBlocks.TEST_BARREL_BLOCK.get());
                            // fill the barrel with some items
                            BarrelBlockEntity barrel = (BarrelBlockEntity) helper.getBlockEntity(pos);
                            for (int slot = 0; slot < barrel.getContainerSize(); slot++) {
                                barrel.setItem(slot, new ItemStack(items[itemIndex++ % items.length], 64));
                            }

                            storage.add(pos);
                        } else {
                            helper.setBlock(pos, SFMBlocks.CABLE_BLOCK.get());
                        }
                    }
                }
            }
        }

        // add the crafting station
        helper.setBlock(new BlockPos(0, 2, 1), Blocks.CRAFTING_TABLE);
        helper.setBlock(new BlockPos(0, 2, 0), SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(new BlockPos(1, 2, 0), SFMBlocks.CABLE_BLOCK.get());
        helper.setBlock(new BlockPos(1, 2, 1), SFMBlocks.CABLE_BLOCK.get());
        helper.setBlock(new BlockPos(1, 2, 2), SFMBlocks.CABLE_BLOCK.get());

        // add the manager
        helper.setBlock(new BlockPos(2, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        var manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(2, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));

        // create the program
        var program = """
                NAME "gather supplies"
                
                EVERY 20 TICKS DO
                   INPUT
                       retain 64 gold_ingot,
                       retain 64 gold_block,
                       retain 64 iron_ingot,
                       retain 64 iron_block,
                       retain 64 diamond,
                       retain 64 diamond_block,
                       retain 64 emerald,
                       retain 64 emerald_block,
                       retain 64 lapis_lazuli,
                       retain 64 lapis_block,
                       retain 64 "redstone",
                       retain 64 redstone_block,
                       retain 64 coal,
                       retain 64 coal_block,
                       retain 64 netherite_ingot,
                       retain 64 netherite_block,
                       retain 64 torch,
                       retain 16 bucket,
                       retain 64 chest,
                       retain 64 crafting_table,
                       retain 64 furnace,
                       retain 64 cobblestone,
                       retain 64 *:*_log,
                       retain 64 *:*_planks
                    FROM chest
                    INPUT EXCEPT
                       gold_ingot,
                       gold_block,
                       iron_ingot,
                       iron_block,
                       diamond,
                       diamond_block,
                       emerald,
                       emerald_block,
                       lapis_lazuli,
                       lapis_block,
                       "redstone",
                       redstone_block,
                       coal,
                       coal_block,
                       netherite_ingot,
                       netherite_block,
                       torch,
                       bucket,
                       chest,
                       crafting_table,
                       furnace,
                       cobblestone,
                       *:*_log,
                       *:*_planks
                   FROM chest
                   OUTPUT TO storage
                END
                
                EVERY 20 TICKS DO
                   INPUT FROM storage
                   OUTPUT
                       retain 64 gold_ingot,
                       retain 64 gold_block,
                       retain 64 iron_ingot,
                       retain 64 iron_block,
                       retain 64 diamond,
                       retain 64 diamond_block,
                       retain 64 emerald,
                       retain 64 emerald_block,
                       retain 64 lapis_lazuli,
                       retain 64 lapis_block,
                       retain 64 "redstone",
                       retain 64 redstone_block,
                       retain 64 coal,
                       retain 64 coal_block,
                       retain 64 netherite_ingot,
                       retain 64 netherite_block,
                       retain 64 torch,
                       retain 16 bucket,
                       retain 64 chest,
                       retain 64 crafting_table,
                       retain 64 furnace,
                       retain 64 cobblestone,
                       retain 64 *:*_log,
                       retain 64 *:*_planks
                   TO chest
                END
                """.stripTrailing().stripIndent();

        // set the labels
        LabelPositionHolder.empty()
                .addAll("storage", storage.stream().map(helper::absolutePos).toList())
                .add("chest", helper.absolutePos(new BlockPos(0, 2, 0)))
                .save(manager.getDisk());

        // load the program
        manager.setProgram(program);

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            // the inventory should be stocked with a stack of each item
            BarrelBlockEntity barrel = (BarrelBlockEntity) helper.getBlockEntity(new BlockPos(0, 2, 0));
            for (Item item : items) {
                for (int slot = 0; slot < barrel.getContainerSize(); slot++) {
                    ItemStack stack = barrel.getItem(slot);
                    if (stack.getItem() == item) {
                        assertTrue(
                                stack.getCount() == stack.getMaxStackSize(),
                                "Item " + item + " is not fully stocked"
                        );
                    }
                }
            }
        });
    }
}
