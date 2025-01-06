package ca.teamdman.sfm.gametest;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.common.item.DiskItem;
import ca.teamdman.sfm.common.localization.LocalizationKeys;
import ca.teamdman.sfm.common.registry.SFMBlocks;
import ca.teamdman.sfm.common.registry.SFMItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;

@GameTestHolder(SFM.MOD_ID)
public class SFMProgramLinterGameTests extends SFMGameTestBase {
    @GameTest(template = "3x2x1")
    public static void mekanism_null_io_direction(GameTestHelper helper) {
        helper.succeed();
//        new SFMChestTestBuilder(helper)
//                .addChest();
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
}
