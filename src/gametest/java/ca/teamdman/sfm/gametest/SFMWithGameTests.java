package ca.teamdman.sfm.gametest;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.common.registry.SFMResourceTypes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.gametest.GameTestHolder;

import java.util.Arrays;
import java.util.function.BiFunction;

@SuppressWarnings({
        "RedundantSuppression",
        "DataFlowIssue",
        "deprecation",
        "OptionalGetWithoutIsPresent",
        "DuplicatedCode",
})
@GameTestHolder(SFM.MOD_ID)
public class SFMWithGameTests extends SFMGameTestBase {
    /// Some tests assume that some items have certain tags.
    ///
    /// To avoid problems between versions, we will validate those assumptions here.
    @GameTest(template = "1x2x1")
    public static void validate_tags(GameTestHelper helper) {
        BiFunction<Item, String, Boolean> hasTag = (item, findTag) -> SFMResourceTypes.ITEM
                .get()
                .getTagsForStack(new ItemStack(item))
                .anyMatch(tag -> tag.toString().equals(findTag));

        // Assert mineable tags
        assert hasTag.apply(Items.DIRT, "minecraft:mineable/shovel");
        assert hasTag.apply(Items.STONE, "minecraft:mineable/pickaxe");
        assert hasTag.apply(Items.IRON_INGOT, "c:ingots");
        assert hasTag.apply(Items.GOLD_INGOT, "c:ingots");
        assert hasTag.apply(Items.GOLD_NUGGET, "c:nuggets");
        assert hasTag.apply(Items.CHEST, "c:chests");
    }

    @GameTest(template = "3x2x1")
    public static void move_with_tag_mineable(GameTestHelper helper) {
        new LeftRightManagerTest(helper)
                .setProgram("""
                                    EVERY 20 TICKS DO
                                        INPUT WITH TAG minecraft:mineable/shovel FROM a
                                        OUTPUT TO b
                                    END
                                    """)
                .fillChest("left", Arrays.asList(
                        enchant(new ItemStack(Items.DIRT, 64), Enchantments.SHARPNESS, 100),
                        new ItemStack(Items.DIRT, 64),
                        new ItemStack(Items.STONE, 64)
                ))
                .assertContains("left", Items.DIRT, 0)
                .assertContains("left", Items.STONE, 64)
                .assertContains("right", Items.DIRT, 128)
                .assertContains("right", Items.STONE, 0)
                .run();
    }

    @GameTest(template = "3x4x3")
    public static void move_with_tag_ingots(GameTestHelper helper) {
        new LeftRightTopManagerTest(helper)
                .setProgram("""
                                    EVERY 20 TICKS DO
                                        INPUT WITH TAG ingots OR tag chests EXCEPT iron_ingot TO b
                                        OUTPUT WITHOUT TAG ingots OR TAG nuggets TO "top"
                                    END
                                    """)
                .fillChest("left", Arrays.asList(
                        new ItemStack(Items.DIRT, 64),
                        new ItemStack(Items.DIRT, 64),
                        new ItemStack(Items.STONE, 64),
                        new ItemStack(Items.IRON_INGOT, 64),
                        new ItemStack(Items.GOLD_INGOT, 64),
                        new ItemStack(Items.GOLD_NUGGET, 64),
                        new ItemStack(Items.CHEST, 64)
                ))
                .assertContains("left", null, 128) // stuff should depart
                .assertContains("left", Items.GOLD_NUGGET, 64) // gold nuggets should remain
                .assertContains("left", Items.IRON_INGOT, 64) // iron ingot should remain
                .assertContains("right", Items.GOLD_INGOT, 64) // gold ingot should arrive
                .assertContains("right", Items.CHEST, 64) // chests should arrive
                .assertContains("top", Items.DIRT, 128) // dirt should arrive
                .assertContains("top", Items.STONE, 64) // stone should arrive
                .run();
    }
    @GameTest(template = "3x2x1")
    public static void move_with_tag_disjunction(GameTestHelper helper) {
        new LeftRightManagerTest(helper)
                .setProgram("""
            EVERY 20 TICKS DO
                INPUT WITH (TAG minecraft:mineable/shovel OR TAG minecraft:mineable/pickaxe) FROM left
                OUTPUT TO right
            END
            """)
                .fillChest("left", Arrays.asList(
                        new ItemStack(Items.DIRT, 64),       // Has tag 'minecraft:mineable/shovel'
                        new ItemStack(Items.STONE, 64),      // Has tag 'minecraft:mineable/pickaxe'
                        new ItemStack(Items.GRASS_BLOCK, 64) // No relevant tag
                ))
                // Assertions for 'left' chest
                .assertContains("left", Items.DIRT, 0)          // Should have moved
                .assertContains("left", Items.STONE, 0)         // Should have moved
                .assertContains("left", Items.GRASS_BLOCK, 64)  // Should remain
                // Assertions for 'right' chest
                .assertContains("right", Items.DIRT, 64)        // Should have arrived
                .assertContains("right", Items.STONE, 64)       // Should have arrived
                .assertContains("right", Items.GRASS_BLOCK, 0)  // Should not be there
                .run();
    }

    @GameTest(template = "3x2x1")
    public static void move_with_tag_negation(GameTestHelper helper) {
        new LeftRightManagerTest(helper)
                .setProgram("""
            EVERY 20 TICKS DO
                INPUT WITH NOT TAG c:ingots FROM left
                OUTPUT TO right
            END
            """)
                .fillChest("left", Arrays.asList(
                        new ItemStack(Items.DIRT, 64),        // Does not have 'c:ingots'
                        new ItemStack(Items.STONE, 64),       // Does not have 'c:ingots'
                        new ItemStack(Items.IRON_INGOT, 64)   // Has 'c:ingots'
                ))
                // Assertions for 'left' chest
                .assertContains("left", Items.DIRT, 0)           // Should have moved
                .assertContains("left", Items.STONE, 0)          // Should have moved
                .assertContains("left", Items.IRON_INGOT, 64)    // Should remain
                // Assertions for 'right' chest
                .assertContains("right", Items.DIRT, 64)         // Should have arrived
                .assertContains("right", Items.STONE, 64)        // Should have arrived
                .assertContains("right", Items.IRON_INGOT, 0)    // Should not be there
                .run();
    }
    @GameTest(template = "3x2x1")
    public static void move_with_tag_conjunction(GameTestHelper helper) {
        new LeftRightManagerTest(helper)
                .setProgram("""
            EVERY 20 TICKS DO
                INPUT WITH NOT (TAG c:ingots OR TAG c:nuggets) FROM left
                OUTPUT TO right
            END
            """)
                .fillChest("left", Arrays.asList(
                        new ItemStack(Items.DIRT, 64),          // Does not have 'c:ingots' or 'c:nuggets'
                        new ItemStack(Items.STONE, 64),         // Does not have 'c:ingots' or 'c:nuggets'
                        new ItemStack(Items.GOLD_INGOT, 64),    // Has 'c:ingots'
                        new ItemStack(Items.GOLD_NUGGET, 64)    // Has 'c:nuggets'
                ))
                // Assertions for 'left' chest
                .assertContains("left", Items.DIRT, 0)           // Should have moved
                .assertContains("left", Items.STONE, 0)          // Should have moved
                .assertContains("left", Items.GOLD_INGOT, 64)    // Should remain
                .assertContains("left", Items.GOLD_NUGGET, 64)   // Should remain
                // Assertions for 'right' chest
                .assertContains("right", Items.DIRT, 64)         // Should have arrived
                .assertContains("right", Items.STONE, 64)        // Should have arrived
                .assertContains("right", Items.GOLD_INGOT, 0)    // Should not be there
                .assertContains("right", Items.GOLD_NUGGET, 0)   // Should not be there
                .run();
    }
    @GameTest(template = "3x2x1")
    public static void move_with_complex_withClause(GameTestHelper helper) {
        new LeftRightManagerTest(helper)
                .setProgram("""
            EVERY 20 TICKS DO
                INPUT WITH (TAG minecraft:mineable/shovel OR TAG minecraft:mineable/pickaxe) AND NOT TAG c:ingots FROM left
                OUTPUT TO right
            END
            """)
                .fillChest("left", Arrays.asList(
                        new ItemStack(Items.DIRT, 64),         // Has 'minecraft:mineable/shovel', not 'c:ingots'
                        new ItemStack(Items.STONE, 64),        // Has 'minecraft:mineable/pickaxe', not 'c:ingots'
                        new ItemStack(Items.IRON_INGOT, 64),   // Has 'c:ingots'
                        new ItemStack(Items.GOLD_INGOT, 64),   // Has 'c:ingots'
                        new ItemStack(Items.GRASS_BLOCK, 64)   // No relevant tags
                ))
                // Assertions for 'left' chest
                .assertContains("left", Items.DIRT, 0)          // Should have moved
                .assertContains("left", Items.STONE, 0)         // Should have moved
                .assertContains("left", Items.IRON_INGOT, 64)   // Should remain
                .assertContains("left", Items.GOLD_INGOT, 64)   // Should remain
                .assertContains("left", Items.GRASS_BLOCK, 64)  // Should remain
                // Assertions for 'right' chest
                .assertContains("right", Items.DIRT, 64)        // Should have arrived
                .assertContains("right", Items.STONE, 64)       // Should have arrived
                .assertContains("right", Items.IRON_INGOT, 0)   // Should not be there
                .assertContains("right", Items.GOLD_INGOT, 0)   // Should not be there
                .assertContains("right", Items.GRASS_BLOCK, 0)  // Should not be there
                .run();
    }
    @GameTest(template = "3x2x1")
    public static void move_with_nested_withClause(GameTestHelper helper) {
        new LeftRightManagerTest(helper)
                .setProgram("""
            EVERY 20 TICKS DO
                INPUT WITH ((TAG c:chests) OR (TAG c:ingots AND NOT TAG c:nuggets)) FROM left
                OUTPUT TO right
            END
            """)
                .fillChest("left", Arrays.asList(
                        new ItemStack(Items.CHEST, 64),         // Has 'c:chests'
                        new ItemStack(Items.IRON_INGOT, 64),    // Has 'c:ingots', not 'c:nuggets'
                        new ItemStack(Items.GOLD_INGOT, 64),    // Has 'c:ingots', not 'c:nuggets'
                        new ItemStack(Items.GOLD_NUGGET, 64),   // Has 'c:nuggets'
                        new ItemStack(Items.DIRT, 64)           // No relevant tags
                ))
                // Assertions for 'left' chest
                .assertContains("left", Items.CHEST, 0)          // Should have moved
                .assertContains("left", Items.IRON_INGOT, 0)     // Should have moved
                .assertContains("left", Items.GOLD_INGOT, 0)     // Should have moved
                .assertContains("left", Items.GOLD_NUGGET, 64)   // Should remain
                .assertContains("left", Items.DIRT, 64)          // Should remain
                // Assertions for 'right' chest
                .assertContains("right", Items.CHEST, 64)        // Should have arrived
                .assertContains("right", Items.IRON_INGOT, 64)   // Should have arrived
                .assertContains("right", Items.GOLD_INGOT, 64)   // Should have arrived
                .assertContains("right", Items.GOLD_NUGGET, 0)   // Should not be there
                .assertContains("right", Items.DIRT, 0)          // Should not be there
                .run();
    }


//    @GameTest(template = "3x2x1")
//    public static void move_with_enchantments(GameTestHelper helper) {
//        helper.setBlock(new BlockPos(1, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
//        BlockPos rightPos = new BlockPos(0, 2, 0);
//        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
//        BlockPos leftPos = new BlockPos(2, 2, 0);
//        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());
//
//        var rightChest = getItemHandler(helper, rightPos);
//        var leftChest = getItemHandler(helper, leftPos);
//
//        ItemStack enchantedDirtStack = new ItemStack(Items.DIRT, 64);
//        EnchantmentHelper.setEnchantments(Map.of(Enchantments.SHARPNESS, 100), enchantedDirtStack);
//        leftChest.insertItem(0, enchantedDirtStack, false);
//        leftChest.insertItem(1, new ItemStack(Items.DIRT, 64), false);
//        leftChest.insertItem(2, new ItemStack(Items.STONE, 64), false);
//
//        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 0));
//        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
//        manager.setProgram("""
//                                       EVERY 20 TICKS DO
//                                           INPUT WITH DATA enchantments FROM a
//                                           OUTPUT TO b
//                                       END
//                                   """.stripTrailing().stripIndent());
//
//        // set the labels
//        LabelPositionHolder.empty()
//                .add("a", helper.absolutePos(leftPos))
//                .add("b", helper.absolutePos(rightPos))
//                .save(manager.getDisk());
//
//        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
//            assertTrue(count(leftChest, Items.DIRT) == 64, "dirt should depart");
//            assertTrue(count(leftChest, Items.STONE) == 64, "stone should remain");
//            assertTrue(count(rightChest, Items.DIRT) == 64, "dirt should arrive");
//            assertTrue(count(rightChest, Items.STONE) == 0, "stone should not arrive");
//        });
//    }
}
