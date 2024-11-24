package ca.teamdman.sfm.gametest.compat.industrialforegoing;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.common.blockentity.ManagerBlockEntity;
import ca.teamdman.sfm.common.program.LabelPositionHolder;
import ca.teamdman.sfm.common.registry.SFMBlocks;
import ca.teamdman.sfm.common.registry.SFMItems;
import ca.teamdman.sfm.gametest.SFMGameTestBase;
import com.buuz135.industrial.module.ModuleCore;
import com.buuz135.industrial.module.ModuleTransportStorage;
import com.buuz135.industrial.utils.BlockUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@SuppressWarnings({"DuplicatedCode", "DataFlowIssue"})
@GameTestHolder(SFM.MOD_ID)
@PrefixGameTestTemplate(false)
public class SFMIndustrialForegoingCompatGameTests extends SFMGameTestBase {
    @GameTest(template = "3x2x1")
    public static void industrialforegoing_blackhole_empty(GameTestHelper helper) {
        // designate positions
        var leftPos = new BlockPos(2, 2, 0);
        var rightPos = new BlockPos(0, 2, 0);
        var managerPos = new BlockPos(1, 2, 0);

        // set up the world
        helper.setBlock(leftPos, ModuleTransportStorage.BLACK_HOLE_UNIT_SUPREME.getLeft().get());
        var left = helper.getBlockEntity(leftPos).getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
        helper.setBlock(rightPos, ModuleTransportStorage.BLACK_HOLE_UNIT_SUPREME.getLeft().get());
        var right = helper.getBlockEntity(rightPos).getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());
        var manager = ((ManagerBlockEntity) helper.getBlockEntity(managerPos));

        // set up the program
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                   EVERY 20 TICKS DO
                                     INPUT FROM a NORTH SIDE
                                     OUTPUT TO b TOP SIDE
                                   END
                                   """.stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .add("b", helper.absolutePos(rightPos))
                .save(manager.getDisk());

        int fullCount = BlockUtils.getStackAmountByRarity(ModuleCore.SUPREME_RARITY);
        assertTrue(fullCount > 0, "expected full count to be greater than 0");
        assertTrue(left.insertItem(0, new ItemStack(Items.COAL, 64), false).isEmpty(), "couldn't prep left");
        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            assertTrue(left.getStackInSlot(0).isEmpty(), "Contents did not depart properly");
            assertTrue(right.getStackInSlot(0).getCount() == 64, "Contents did not arrive");
        });
    }


    @GameTest(template = "3x2x1")
    public static void industrialforegoing_blackhole_some(GameTestHelper helper) {
        // designate positions
        var leftPos = new BlockPos(2, 2, 0);
        var rightPos = new BlockPos(0, 2, 0);
        var managerPos = new BlockPos(1, 2, 0);

        // set up the world
        helper.setBlock(leftPos, ModuleTransportStorage.BLACK_HOLE_UNIT_SUPREME.getLeft().get());
        var left = helper.getBlockEntity(leftPos).getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
        helper.setBlock(rightPos, ModuleTransportStorage.BLACK_HOLE_UNIT_SUPREME.getLeft().get());
        var right = helper.getBlockEntity(rightPos).getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());
        var manager = ((ManagerBlockEntity) helper.getBlockEntity(managerPos));

        // set up the program
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                   EVERY 20 TICKS DO
                                     INPUT FROM a NORTH SIDE
                                     OUTPUT TO b TOP SIDE
                                   END
                                   """.stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .add("b", helper.absolutePos(rightPos))
                .save(manager.getDisk());

        // we need to insert a normal stack last for the rendering to work in IF
        assertTrue(left.insertItem(0, new ItemStack(Items.COAL, 5000 - 64), false).isEmpty(), "couldn't prep left");
        assertTrue(left.insertItem(0, new ItemStack(Items.COAL, 64), false).isEmpty(), "couldn't prep left");
        assertTrue(right.insertItem(0, new ItemStack(Items.COAL, 5000 - 64), false).isEmpty(), "couldn't prep left");
        assertTrue(right.insertItem(0, new ItemStack(Items.COAL, 64), false).isEmpty(), "couldn't prep right");

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            assertTrue(left.getStackInSlot(0).getCount() == 5_000 - 64, "Contents did not depart properly");
            assertTrue(right.getStackInSlot(0).getCount() == 5_000 + 64, "Contents did not arrive");
            
        });
    }


    @GameTest(template = "3x2x1")
    public static void industrialforegoing_blackhole_full(GameTestHelper helper) {
        // designate positions
        var leftPos = new BlockPos(2, 2, 0);
        var rightPos = new BlockPos(0, 2, 0);
        var managerPos = new BlockPos(1, 2, 0);

        // set up the world
        helper.setBlock(leftPos, ModuleTransportStorage.BLACK_HOLE_UNIT_SUPREME.getLeft().get());
        var left = helper.getBlockEntity(leftPos).getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
        helper.setBlock(rightPos, ModuleTransportStorage.BLACK_HOLE_UNIT_SUPREME.getLeft().get());
        var right = helper.getBlockEntity(rightPos).getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());
        var manager = ((ManagerBlockEntity) helper.getBlockEntity(managerPos));

        // set up the program
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                   EVERY 20 TICKS DO
                                     INPUT FROM a NORTH SIDE
                                     OUTPUT TO b TOP SIDE
                                   END
                                   """.stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .add("b", helper.absolutePos(rightPos))
                .save(manager.getDisk());

        int fullCount = BlockUtils.getStackAmountByRarity(ModuleCore.SUPREME_RARITY);
        assertTrue(fullCount > 0, "expected full count to be greater than 0");
        assertTrue(left.insertItem(0, new ItemStack(Items.COAL, 64), false).isEmpty(), "couldn't prep left");
        assertTrue(left.insertItem(0, new ItemStack(Items.COAL, 1), false).isEmpty(), "couldn't prep left");
        assertTrue(
                right.insertItem(0, new ItemStack(Items.COAL, fullCount - 2), false).isEmpty(),
                "couldn't prep right"
        );
        assertTrue(right.insertItem(0, new ItemStack(Items.COAL, 1), false).isEmpty(), "couldn't prep right");

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            // black hole units have voiding=true by default
            // the final insertion operation that causes the thing to fill will cause the items that don't fit to be voided
            // this test should cause a stack of 64 to be used to insert even though only 1 is needed to finish filling
            assertTrue(left.getStackInSlot(0).getCount() == 1, "Contents did not depart properly");
            assertTrue(right.getStackInSlot(0).getCount() == fullCount, "Contents did not arrive");
        });
    }

}
