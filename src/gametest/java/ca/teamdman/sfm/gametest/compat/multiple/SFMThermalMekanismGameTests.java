package ca.teamdman.sfm.gametest.compat.multiple;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.common.blockentity.ManagerBlockEntity;
import ca.teamdman.sfm.common.logging.TranslatableLogEvent;
import ca.teamdman.sfm.common.program.LabelPositionHolder;
import ca.teamdman.sfm.common.registry.SFMBlocks;
import ca.teamdman.sfm.common.registry.SFMItems;
import ca.teamdman.sfm.gametest.SFMGameTestBase;
import cofh.thermal.expansion.block.entity.machine.MachineInsolatorTile;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tile.TileEntityBin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.Level;

import java.util.ArrayDeque;
import java.util.Objects;

@GameTestHolder(SFM.MOD_ID)
@PrefixGameTestTemplate(false)
public class SFMThermalMekanismGameTests extends SFMGameTestBase {
    @GameTest(template = "3x4x3")
    public static void resource_loss_regression(GameTestHelper helper) {
        var managerPos = new BlockPos(1, 2, 1);
        var buttonPos = managerPos.offset(Direction.NORTH.getNormal());
        var leftPos = new BlockPos(2, 2, 1);
        var rightPos = new BlockPos(0, 2, 1);

        // place and fill the chests
        helper.setBlock(leftPos, MekanismBlocks.CREATIVE_BIN.getBlock());
        TileEntityBin bin = (TileEntityBin) helper.getBlockEntity(leftPos);
        assert bin != null;
        bin.setStackInSlot(0, new ItemStack(Items.WHEAT_SEEDS, Integer.MAX_VALUE));

        var phytoBlock = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("thermal", "machine_insolator"));
        assert phytoBlock != null;
        helper.setBlock(rightPos, phytoBlock);
        MachineInsolatorTile phyto = (MachineInsolatorTile) helper.getBlockEntity(rightPos);
        assert phyto != null;
        phyto.getItemInv().set(0, new ItemStack(Items.WHEAT_SEEDS, 63));

        // create the manager block and add the disk
        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());
        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(managerPos);
        assert manager != null;
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));

        // create the program
        var program = """
                    NAME "move on pulse"
                
                    EVERY REDSTONE PULSE DO
                        INPUT FROM left BOTTOM SIDE
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
        manager.setLogLevel(Level.ERROR);
        assertTrue(manager.logger.getLogLevel() == Level.ERROR, "Log level should be trace");

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            ItemStack phytoInputStack = phyto.getItemInv().get(0);
            assertTrue(phytoInputStack.getItem() == Items.WHEAT_SEEDS, "Item should be wheat seeds");
            assertTrue(phytoInputStack.getCount() == 64, "Item should be 64 wheat seeds");

            ArrayDeque<TranslatableLogEvent> logs = manager.logger.getLogs();
            int foundErrors = 0;
            for (TranslatableLogEvent log : logs) {
                if (log.level() == Level.ERROR) {
                    foundErrors++;
                    SFM.LOGGER.warn("Found error when should be none in resource_loss_regression test: {}", log.contents());
                }
            }
            assertTrue(foundErrors == 0, "No errors should be found in logs, found " + foundErrors);
        });

        // create the button
        helper.setBlock(buttonPos, Blocks.STONE_BUTTON);
        // push the button
        helper.pressButton(buttonPos);
    }

}
