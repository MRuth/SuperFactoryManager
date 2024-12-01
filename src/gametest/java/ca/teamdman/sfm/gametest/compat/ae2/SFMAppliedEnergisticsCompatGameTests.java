package ca.teamdman.sfm.gametest.compat.ae2;

import appeng.blockentity.misc.InscriberBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.common.blockentity.ManagerBlockEntity;
import ca.teamdman.sfm.common.program.LabelPositionHolder;
import ca.teamdman.sfm.common.registry.SFMBlocks;
import ca.teamdman.sfm.common.registry.SFMItems;
import ca.teamdman.sfm.gametest.SFMGameTestBase;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;

import java.util.stream.Stream;

@SuppressWarnings({"OptionalGetWithoutIsPresent", "DataFlowIssue"})
@GameTestHolder(SFM.MOD_ID)
public class SFMAppliedEnergisticsCompatGameTests extends SFMGameTestBase {

    @GameTest(template = "7x3x3", timeoutTicks = 20 * 20)
    public static void ae2_inscribers(GameTestHelper helper) {
        var managerPos = new BlockPos(0, 2, 1);

        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());
        for (int i = 0; i < 6; i++) {
            helper.setBlock(new BlockPos(i + 1, 2, 1), SFMBlocks.CABLE_BLOCK.get());
        }

        var siliconPos1 = new BlockPos(4, 3, 1);
        var siliconPos2 = new BlockPos(5, 3, 1);
        var siliconPos3 = new BlockPos(6, 3, 1);
        var logicPos = new BlockPos(1, 2, 0);
        var engineeringPos = new BlockPos(2, 2, 0);
        var calculationPos = new BlockPos(3, 2, 0);
        var lastPos1 = new BlockPos(1, 3, 1);
        var lastPos2 = new BlockPos(2, 3, 1);
        var lastPos3 = new BlockPos(3, 3, 1);
        helper.setBlock(siliconPos1, AEBlocks.INSCRIBER.block());
        helper.setBlock(siliconPos2, AEBlocks.INSCRIBER.block());
        helper.setBlock(siliconPos3, AEBlocks.INSCRIBER.block());
        helper.setBlock(logicPos, AEBlocks.INSCRIBER.block());
        helper.setBlock(engineeringPos, AEBlocks.INSCRIBER.block());
        helper.setBlock(calculationPos, AEBlocks.INSCRIBER.block());
        helper.setBlock(lastPos1, AEBlocks.INSCRIBER.block());
        helper.setBlock(lastPos2, AEBlocks.INSCRIBER.block());
        helper.setBlock(lastPos3, AEBlocks.INSCRIBER.block());
        var silicon1 = ((InscriberBlockEntity) helper.getBlockEntity(siliconPos1));
        var silicon2 = ((InscriberBlockEntity) helper.getBlockEntity(siliconPos2));
        var silicon3 = ((InscriberBlockEntity) helper.getBlockEntity(siliconPos3));
        var logic = ((InscriberBlockEntity) helper.getBlockEntity(logicPos));
        var engineering = ((InscriberBlockEntity) helper.getBlockEntity(engineeringPos));
        var calculation = ((InscriberBlockEntity) helper.getBlockEntity(calculationPos));
        var last1 = ((InscriberBlockEntity) helper.getBlockEntity(lastPos1));
        var last2 = ((InscriberBlockEntity) helper.getBlockEntity(lastPos2));
        var last3 = ((InscriberBlockEntity) helper.getBlockEntity(lastPos3));
        silicon1
                .getCapability(ForgeCapabilities.ITEM_HANDLER)
                .resolve()
                .get()
                .insertItem(0, new ItemStack(AEItems.SILICON_PRESS), false);
        silicon2
                .getCapability(ForgeCapabilities.ITEM_HANDLER)
                .resolve()
                .get()
                .insertItem(0, new ItemStack(AEItems.SILICON_PRESS), false);
        silicon3
                .getCapability(ForgeCapabilities.ITEM_HANDLER)
                .resolve()
                .get()
                .insertItem(0, new ItemStack(AEItems.SILICON_PRESS), false);
        engineering
                .getCapability(ForgeCapabilities.ITEM_HANDLER)
                .resolve()
                .get()
                .insertItem(0, new ItemStack(AEItems.ENGINEERING_PROCESSOR_PRESS), false);
        calculation
                .getCapability(ForgeCapabilities.ITEM_HANDLER)
                .resolve()
                .get()
                .insertItem(0, new ItemStack(AEItems.CALCULATION_PROCESSOR_PRESS), false);
        logic
                .getCapability(ForgeCapabilities.ITEM_HANDLER)
                .resolve()
                .get()
                .insertItem(0, new ItemStack(AEItems.LOGIC_PROCESSOR_PRESS), false);

        Stream
                .of(silicon1, silicon2, silicon3, logic, engineering, calculation, last1, last2, last3)
                .map(InscriberBlockEntity::getUpgrades)
                .forEach(upgradeInventory -> {
                    for (int slot = 0; slot < upgradeInventory.size(); slot++) {
                        upgradeInventory.insertItem(slot, new ItemStack(AEItems.SPEED_CARD), false);
                    }
                });

        var powerPos1 = new BlockPos(0, 3, 1);
        helper.setBlock(powerPos1, AEBlocks.CREATIVE_ENERGY_CELL.block());
        var powerPos2 = new BlockPos(4, 2, 0);
        helper.setBlock(powerPos2, AEBlocks.CREATIVE_ENERGY_CELL.block());

        var materialsPos = new BlockPos(6, 2, 0);
        var resultsPos = new BlockPos(5, 2, 0);
        helper.setBlock(materialsPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        helper.setBlock(resultsPos, SFMBlocks.TEST_BARREL_BLOCK.get());
        //noinspection DataFlowIssue,OptionalGetWithoutIsPresent
        var materials = helper
                .getBlockEntity(materialsPos)
                .getCapability(ForgeCapabilities.ITEM_HANDLER)
                .resolve()
                .get();
        //noinspection DataFlowIssue,OptionalGetWithoutIsPresent
        var results = helper.getBlockEntity(resultsPos).getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().get();
        materials.insertItem(0, new ItemStack(Items.REDSTONE, 64), false);
        materials.insertItem(1, new ItemStack(Items.REDSTONE, 64), false);
        materials.insertItem(2, new ItemStack(Items.REDSTONE, 64), false);
        materials.insertItem(3, new ItemStack(Items.DIAMOND, 64), false);
        materials.insertItem(4, new ItemStack(Items.GOLD_INGOT, 64), false);
        materials.insertItem(5, new ItemStack(AEItems.CERTUS_QUARTZ_CRYSTAL, 64), false);
        materials.insertItem(6, new ItemStack(AEItems.SILICON, 64), false);
        materials.insertItem(7, new ItemStack(AEItems.SILICON, 64), false);
        materials.insertItem(8, new ItemStack(AEItems.SILICON, 64), false);

        // put signs on them lol
        helper.setBlock(
                materialsPos.offset(0, 1, 0),
                Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, 8)
        );
        ((SignBlockEntity) helper.getBlockEntity(materialsPos.offset(0, 1, 0))).setMessage(
                0,
                Component.literal("input")
        );
        helper.setBlock(
                resultsPos.offset(0, 1, 0),
                Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, 8)
        );
        ((SignBlockEntity) helper.getBlockEntity(resultsPos.offset(0, 1, 0))).setMessage(
                0,
                Component.literal("output")
        );

        var manager = ((ManagerBlockEntity) helper.getBlockEntity(managerPos));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        LabelPositionHolder.empty()
                .add("silicon", helper.absolutePos(siliconPos1))
                .add("silicon", helper.absolutePos(siliconPos2))
                .add("silicon", helper.absolutePos(siliconPos3))
                .add("logic", helper.absolutePos(logicPos))
                .add("engineering", helper.absolutePos(engineeringPos))
                .add("calculation", helper.absolutePos(calculationPos))
                .add("last", helper.absolutePos(lastPos1))
                .add("last", helper.absolutePos(lastPos2))
                .add("last", helper.absolutePos(lastPos3))
                .add("materials", helper.absolutePos(materialsPos))
                .add("results", helper.absolutePos(resultsPos))
                .save(manager.getDisk());

        manager.setProgram("""
                                   EVERY 20 TICKS DO
                                       INPUT FROM materials
                                       OUTPUT gold_ingot TO logic SLOTS 2
                                       OUTPUT diamond TO engineering SLOTS 2
                                       OUTPUT certus_quartz_crystal TO calculation SLOTS 2
                                       OUTPUT silicon TO silicon SLOTS 2
                                       OUTPUT redstone TO last SLOTS 2
                                       OUTPUT printed_silicon TO last SLOTS 1
                                       OUTPUT printed_calculation_processor, printed_engineering_processor, printed_logic_processor TO last SLOTS 0

                                       FORGET
                                       INPUT FROM logic, engineering, calculation, silicon west side
                                       output to materials

                                       FORGET
                                       INPUT FROM last west SIDE
                                       OUTPUT TO results
                                   END
                                   """.stripTrailing().stripIndent());
        helper.succeedWhen(() -> {
            boolean hasCalculation = count(results, AEItems.CALCULATION_PROCESSOR.asItem().asItem()) > 0;
            boolean hasEngineering = count(results, AEItems.ENGINEERING_PROCESSOR.asItem().asItem()) > 0;
            boolean hasLogic = count(results, AEItems.LOGIC_PROCESSOR.asItem().asItem()) > 0;
            if (hasCalculation && hasEngineering && hasLogic) {
                helper.succeed();
            } else {
                helper.fail("Missing processors: " + (hasCalculation ? "" : "calculation ") + (
                        hasEngineering
                        ? ""
                        : "engineering "
                ) + (hasLogic ? "" : "logic"));
            }
        });
    }
}
