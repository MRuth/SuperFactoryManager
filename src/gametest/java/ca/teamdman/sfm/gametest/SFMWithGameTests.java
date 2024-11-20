package ca.teamdman.sfm.gametest;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.common.blockentity.ManagerBlockEntity;
import ca.teamdman.sfm.common.program.LabelPositionHolder;
import ca.teamdman.sfm.common.registry.SFMBlocks;
import ca.teamdman.sfm.common.registry.SFMItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Map;

@SuppressWarnings({
        "RedundantSuppression",
        "DataFlowIssue",
        "deprecation",
        "OptionalGetWithoutIsPresent",
        "DuplicatedCode",
})
@GameTestHolder(SFM.MOD_ID)
@PrefixGameTestTemplate(false)
public class SFMWithGameTests extends SFMGameTestBase {
    @GameTest(template = "3x2x1")
    public static void move_with_tag_mineable(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 0), SFMBlocks.MANAGER_BLOCK.get());
        BlockPos rightPos = new BlockPos(0, 2, 0);
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        BlockPos leftPos = new BlockPos(2, 2, 0);
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());

        var rightChest = getItemHandler(helper, rightPos);
        var leftChest = getItemHandler(helper, leftPos);

        ItemStack enchantedDirtStack = new ItemStack(Items.DIRT, 64);
        EnchantmentHelper.setEnchantments(Map.of(Enchantments.SHARPNESS, 100), enchantedDirtStack);
        leftChest.insertItem(0, enchantedDirtStack, false);
        leftChest.insertItem(1, new ItemStack(Items.DIRT, 64), false);
        leftChest.insertItem(2, new ItemStack(Items.STONE, 64), false);

        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 0));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           INPUT WITH TAG minecraft:mineable/shovel FROM a
                                           OUTPUT TO b
                                       END
                                   """.stripTrailing().stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .add("b", helper.absolutePos(rightPos))
                .save(manager.getDisk());

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            assertTrue(count(leftChest, Items.DIRT) == 0, "dirt should depart");
            assertTrue(count(leftChest, Items.STONE) == 64, "stone should remain");
            assertTrue(count(rightChest, Items.DIRT) == 64 * 2, "dirt should arrive");
            assertTrue(count(rightChest, Items.STONE) == 0, "stone should not arrive");
        });
    }

    @GameTest(template = "3x4x3")
    public static void move_with_tag_ingots(GameTestHelper helper) {
        BlockPos managerPos = new BlockPos(1, 2, 0);
        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());
        BlockPos leftPos = managerPos.east();
        helper.setBlock(leftPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        BlockPos rightPos = managerPos.west();
        helper.setBlock(rightPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        BlockPos topPos = managerPos.above();
        helper.setBlock(topPos, SFMBlocks.TEST_BARREL_BLOCK.get());

        var rightChest = getItemHandler(helper, rightPos);
        var leftChest = getItemHandler(helper, leftPos);
        var topChest = getItemHandler(helper, topPos);

        leftChest.insertItem(0, new ItemStack(Items.DIRT, 64), false);
        leftChest.insertItem(1, new ItemStack(Items.DIRT, 64), false);
        leftChest.insertItem(2, new ItemStack(Items.STONE, 64), false);
        leftChest.insertItem(3, new ItemStack(Items.IRON_INGOT, 64), false);
        leftChest.insertItem(4, new ItemStack(Items.GOLD_INGOT, 64), false);
        leftChest.insertItem(5, new ItemStack(Items.GOLD_NUGGET, 64), false);
        leftChest.insertItem(6, new ItemStack(Items.CHEST, 64), false);

        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(managerPos);
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                       EVERY 20 TICKS DO
                                           INPUT minecraft: FROM left
                                           OUTPUT WITH TAG ingots OR tag chests EXCEPT iron_ingot TO right
                                           OUTPUT WITHOUT TAG ingots OR TAG nuggets TO "top"
                                       END
                                   """.stripTrailing().stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("left", helper.absolutePos(leftPos))
                .add("right", helper.absolutePos(rightPos))
                .add("top", helper.absolutePos(topPos))
                .save(manager.getDisk());

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            assertTrue(count(leftChest, null) == 128, "stuff should depart");
            assertTrue(count(leftChest, Items.GOLD_NUGGET) == 64, "gold nuggets should remain");
            assertTrue(count(leftChest, Items.IRON_INGOT) == 64, "iron ingot should remain");
            assertTrue(count(rightChest, Items.GOLD_INGOT) == 64, "gold ingot should arrive");
            assertTrue(count(rightChest, Items.CHEST) == 64, "chests should arrive");
            assertTrue(count(topChest, Items.DIRT) == 64 * 2, "dirt should arrive");
            assertTrue(count(topChest, Items.STONE) == 64, "stone should arrive");
        });
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
