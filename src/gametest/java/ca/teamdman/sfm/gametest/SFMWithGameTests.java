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
import java.util.function.BiConsumer;

@SuppressWarnings({
        "RedundantSuppression",
        "DataFlowIssue",
        "deprecation",
        "OptionalGetWithoutIsPresent",
        "DuplicatedCode",
        "ArraysAsListWithZeroOrOneArgument"
})
@GameTestHolder(SFM.MOD_ID)
public class SFMWithGameTests extends SFMGameTestBase {
    /// Some tests assume that some items have certain tags.
    ///
    /// To avoid problems between versions, we will validate those assumptions here.
    @GameTest(template = "1x1x1")
    public static void validate_tags(GameTestHelper helper) {
        BiConsumer<Item, String> assertTag = (item, findTag) -> {
            boolean hasTag = SFMResourceTypes.ITEM
                    .get()
                    .getTagsForStack(new ItemStack(item))
                    .anyMatch(tag -> tag.toString().equals(findTag) || !findTag.contains(":") && tag
                            .getPath()
                            .equals(findTag));
            assertTrue(hasTag, "Item " + item + " should have tag " + findTag);
        };

        // Assert mineable tags
        assertTag.accept(Items.DIRT, "minecraft:mineable/shovel");
        assertTag.accept(Items.STONE, "minecraft:mineable/pickaxe");
        assertTag.accept(Items.OAK_PLANKS, "minecraft:planks");
        assertTag.accept(Items.OAK_PLANKS, "minecraft:mineable/axe");
        assertTag.accept(Items.IRON_INGOT, "ingots");
        assertTag.accept(Items.GOLD_INGOT, "ingots");
        assertTag.accept(Items.GOLD_NUGGET, "nuggets");
        assertTag.accept(Items.CHEST, "chests");
        helper.succeed();
    }

    @GameTest(template = "3x2x1")
    public static void move_with_tag_mineable(GameTestHelper helper) {
        new LeftRightManagerTest(helper)
                .setProgram("""
                                    EVERY 20 TICKS DO
                                        INPUT WITH TAG minecraft:mineable/shovel FROM left
                                        OUTPUT TO right
                                    END
                                    """)
                .preContents("left", Arrays.asList(
                        enchant(new ItemStack(Items.DIRT, 64), Enchantments.SHARPNESS, 100), // Slot 0
                        new ItemStack(Items.DIRT, 64),                                       // Slot 1
                        new ItemStack(Items.STONE, 64)                                       // Slot 2
                ))
                .postContents("left", Arrays.asList(
                        ItemStack.EMPTY,                    // Slot 0 (Dirt moved)
                        ItemStack.EMPTY,                    // Slot 1 (Dirt moved)
                        new ItemStack(Items.STONE, 64)      // Slot 2 (Stone remains)
                ))
                .postContents("right", Arrays.asList(
                        enchant(new ItemStack(Items.DIRT, 64), Enchantments.SHARPNESS, 100), // Slot 0
                        new ItemStack(Items.DIRT, 64)                                        // Slot 1
                        // The rest are empty by default
                ))
                .run();
    }

    @GameTest(template = "3x2x1")
    public static void move_with_tag_ingots(GameTestHelper helper) {
        new LeftRightManagerTest(helper)
                .setProgram("""
                                    EVERY 20 TICKS DO
                                        INPUT WITH TAG ingots FROM left
                                        OUTPUT TO right
                                    END
                                    """)
                .preContents("left", Arrays.asList(
                        new ItemStack(Items.DIRT, 64),
                        new ItemStack(Items.DIRT, 64),
                        new ItemStack(Items.IRON_INGOT, 64),
                        new ItemStack(Items.GOLD_INGOT, 64),
                        new ItemStack(Items.GOLD_NUGGET, 64),
                        new ItemStack(Items.CHEST, 64)
                ))
                .postContents("left", Arrays.asList(
                        new ItemStack(Items.DIRT, 64),
                        new ItemStack(Items.DIRT, 64),
                        ItemStack.EMPTY,
                        ItemStack.EMPTY,
                        new ItemStack(Items.GOLD_NUGGET, 64),
                        new ItemStack(Items.CHEST, 64)
                ))
                .postContents("right", Arrays.asList(
                        new ItemStack(Items.IRON_INGOT, 64),
                        new ItemStack(Items.GOLD_INGOT, 64)
                ))
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
                .preContents("left", Arrays.asList(
                        new ItemStack(Items.DIRT, 64),       // Slot 0
                        new ItemStack(Items.STONE, 64),      // Slot 1
                        new ItemStack(Items.OAK_PLANKS, 64)  // Slot 2
                ))
                .postContents("left", Arrays.asList(
                        ItemStack.EMPTY,                    // Slot 0 (Dirt moved)
                        ItemStack.EMPTY,                    // Slot 1 (Stone moved)
                        new ItemStack(Items.OAK_PLANKS, 64) // Slot 2 (Planks remain)
                ))
                .postContents("right", Arrays.asList(
                        new ItemStack(Items.DIRT, 64),      // Slot 0
                        new ItemStack(Items.STONE, 64)      // Slot 1
                        // The rest are empty
                ))
                .run();
    }

    @GameTest(template = "3x2x1")
    public static void move_with_tag_negation(GameTestHelper helper) {
        new LeftRightManagerTest(helper)
                .setProgram("""
                                    EVERY 20 TICKS DO
                                        INPUT WITH NOT TAG minecraft:mineable/pickaxe FROM left
                                        OUTPUT TO right
                                    END
                                    """)
                .preContents("left", Arrays.asList(
                        new ItemStack(Items.DIRT, 64),       // Slot 0
                        new ItemStack(Items.STONE, 64),      // Slot 1
                        new ItemStack(Items.OAK_PLANKS, 64)  // Slot 2
                ))
                .postContents("left", Arrays.asList(
                        ItemStack.EMPTY,                    // Slot 0 (Dirt moved)
                        new ItemStack(Items.STONE, 64),     // Slot 1 (Stone remains)
                        ItemStack.EMPTY                     // Slot 2 (Planks moved)
                ))
                .postContents("right", Arrays.asList(
                        new ItemStack(Items.DIRT, 64),      // Slot 0
                        new ItemStack(Items.OAK_PLANKS, 64) // Slot 1
                ))
                .run();
    }

    @GameTest(template = "3x2x1")
    public static void move_with_tag_conjunction(GameTestHelper helper) {
        new LeftRightManagerTest(helper)
                .setProgram("""
                                    EVERY 20 TICKS DO
                                        INPUT WITH TAG minecraft:planks AND TAG minecraft:mineable/axe FROM left
                                        OUTPUT TO right
                                    END
                                    """)
                .preContents("left", Arrays.asList(
                        new ItemStack(Items.DIRT, 64),      // Slot 0
                        new ItemStack(Items.STONE, 64),     // Slot 1
                        new ItemStack(Items.OAK_PLANKS, 64) // Slot 2
                ))
                .postContents("left", Arrays.asList(
                        new ItemStack(Items.DIRT, 64),     // Slot 0 (Dirt remains)
                        new ItemStack(Items.STONE, 64),    // Slot 1 (Stone remains)
                        ItemStack.EMPTY                    // Slot 2 (Planks moved)
                ))
                .postContents("right", Arrays.asList(
                        new ItemStack(Items.OAK_PLANKS, 64) // Slot 0
                ))
                .run();
    }

    @GameTest(template = "3x2x1")
    public static void move_with_complex_withClause(GameTestHelper helper) {
        new LeftRightManagerTest(helper)
                .setProgram("""
                                    EVERY 20 TICKS DO
                                        INPUT WITH (TAG minecraft:planks OR (TAG minecraft:mineable/shovel AND NOT TAG minecraft:mineable/axe)) FROM left
                                        OUTPUT TO right
                                    END
                                    """)
                .preContents("left", Arrays.asList(
                        new ItemStack(Items.DIRT, 64),      // Slot 0
                        new ItemStack(Items.STONE, 64),     // Slot 1
                        new ItemStack(Items.OAK_PLANKS, 64) // Slot 2
                ))
                .postContents("left", Arrays.asList(
                        ItemStack.EMPTY,                   // Slot 0 (Dirt moved)
                        new ItemStack(Items.STONE, 64),    // Slot 1 (Stone remains)
                        ItemStack.EMPTY                    // Slot 2 (Planks moved)
                ))
                .postContents("right", Arrays.asList(
                        new ItemStack(Items.DIRT, 64),      // Slot 0
                        new ItemStack(Items.OAK_PLANKS, 64) // Slot 1
                ))
                .run();
    }

    @GameTest(template = "3x2x1")
    public static void move_with_nested_withClause(GameTestHelper helper) {
        new LeftRightManagerTest(helper)
                .setProgram("""
                                    EVERY 20 TICKS DO
                                        INPUT WITH ((TAG minecraft:mineable/shovel AND NOT TAG minecraft:mineable/pickaxe) OR (TAG minecraft:planks AND TAG minecraft:mineable/axe)) FROM left
                                        OUTPUT TO right
                                    END
                                    """)
                .preContents("left", Arrays.asList(
                        new ItemStack(Items.DIRT, 64),      // Slot 0
                        new ItemStack(Items.STONE, 64),     // Slot 1
                        new ItemStack(Items.OAK_PLANKS, 64) // Slot 2
                ))
                .postContents("left", Arrays.asList(
                        ItemStack.EMPTY,                   // Slot 0 (Dirt moved)
                        new ItemStack(Items.STONE, 64),    // Slot 1 (Stone remains)
                        ItemStack.EMPTY                    // Slot 2 (Planks moved)
                ))
                .postContents("right", Arrays.asList(
                        new ItemStack(Items.DIRT, 64),      // Slot 0
                        new ItemStack(Items.OAK_PLANKS, 64) // Slot 1
                ))
                .run();
    }

    @GameTest(template = "3x2x1")
    public static void move_with_not_and_or_combination(GameTestHelper helper) {
        new LeftRightManagerTest(helper)
                .setProgram("""
                                    EVERY 20 TICKS DO
                                        INPUT WITH NOT TAG minecraft:mineable/shovel AND TAG minecraft:mineable/axe OR TAG minecraft:planks FROM left
                                        OUTPUT TO right
                                    END
                                    """)
                .preContents("left", Arrays.asList(
                        new ItemStack(Items.DIRT, 64),      // Slot 0
                        new ItemStack(Items.OAK_PLANKS, 64),// Slot 1
                        new ItemStack(Items.STONE, 64)      // Slot 2
                ))
                .postContents("left", Arrays.asList(
                        new ItemStack(Items.DIRT, 64),     // Slot 0 (Dirt remains)
                        ItemStack.EMPTY,                   // Slot 1 (Planks moved)
                        new ItemStack(Items.STONE, 64)     // Slot 2 (Stone remains)
                ))
                .postContents("right", Arrays.asList(
                        new ItemStack(Items.OAK_PLANKS, 64) // Slot 0
                ))
                .run();
    }
}
