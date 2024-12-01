package ca.teamdman.sfm.gametest.compat.thermal;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.common.blockentity.ManagerBlockEntity;
import ca.teamdman.sfm.common.program.LabelPositionHolder;
import ca.teamdman.sfm.common.registry.SFMBlocks;
import ca.teamdman.sfm.common.registry.SFMItems;
import ca.teamdman.sfm.common.registry.SFMResourceTypes;
import ca.teamdman.sfm.gametest.SFMGameTestBase;
import cofh.thermal.expansion.block.entity.machine.MachineFurnaceTile;
import cofh.thermal.expansion.block.entity.machine.MachineInsolatorTile;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tile.TileEntityFluidTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;

@SuppressWarnings({"DataFlowIssue"})
@GameTestHolder(SFM.MOD_ID)
public class SFMThermalCompatGameTests extends SFMGameTestBase {

    @GameTest(template = "25x3x25", timeoutTicks = 20 * 20)
    public static void thermal_furnace_array(GameTestHelper helper) {
        // designate positions
        var furnacePositions = new ArrayList<BlockPos>();
        var resultChestPositions = new ArrayList<BlockPos>();
        var ingredientChestPositions = new ArrayList<BlockPos>();
        var managerPos = new BlockPos(0, 2, 0);
        var powerPos = new BlockPos(1, 2, 0);

        // set up power
        helper.setBlock(powerPos, MekanismBlocks.ULTIMATE_ENERGY_CUBE.getBlock());
        helper
                .getBlockEntity(powerPos)
                .getCapability(ForgeCapabilities.ENERGY, Direction.UP)
                .ifPresent(energy -> energy.receiveEnergy(Integer.MAX_VALUE, false));

        // set up furnaces
        var furnaceBlock = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("thermal", "machine_furnace"));
        for (int x = 0; x < 25; x++) {
            for (int z = 1; z < 25; z++) {
                helper.setBlock(new BlockPos(x, 2, z), SFMBlocks.CABLE_BLOCK.get());
                helper.setBlock(new BlockPos(x, 3, z), furnaceBlock);
                furnacePositions.add(new BlockPos(x, 3, z));
                var furnace = (MachineFurnaceTile) helper.getBlockEntity(new BlockPos(x, 3, z));
                furnace.setSideConfig(Direction.UP, MachineFurnaceTile.SideConfig.SIDE_INPUT);
                furnace.setSideConfig(Direction.DOWN, MachineFurnaceTile.SideConfig.SIDE_OUTPUT);
            }
        }

        // set up destinations
        for (int i = 2; i <= 3; i++) {
            BlockPos pos = new BlockPos(i, 2, 0);
            helper.setBlock(pos, SFMBlocks.TEST_BARREL_BLOCK.get());
            resultChestPositions.add(pos);
        }

        // set up ingredients
        for (int i = 5; i <= 6; i++) {
            BlockPos pos = new BlockPos(i, 2, 0);
            helper.setBlock(pos, SFMBlocks.TEST_BARREL_BLOCK.get());
            ingredientChestPositions.add(pos);
            for (int slot = 0; slot < 27; slot++) {
                getItemHandler(helper, pos).insertItem(slot, new ItemStack(Items.CHICKEN, 64), false);
            }
        }

        // set up the manager
        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());
        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(managerPos);
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));

        // create the program
        var program = """
                    NAME "thermal furnace array test"
                    EVERY 5 TICKS DO
                        INPUT forge_energy:forge:energy FROM power NORTH SIDE
                        OUTPUT forge_energy:forge:energy TO furnaces
                    END
                    EVERY 20 TICKS DO
                        INPUT FROM ingredients
                        OUTPUT RETAIN 2 TO EACH furnaces TOP SIDE
                    FORGET
                        INPUT FROM furnaces BOTTOM SIDE
                        OUTPUT TO results TOP SIDE
                    END
                """;

        // set the labels
        LabelPositionHolder.empty()
                .addAll("furnaces", furnacePositions.stream().map(helper::absolutePos).toList())
                .addAll("ingredients", ingredientChestPositions.stream().map(helper::absolutePos).toList())
                .addAll("results", resultChestPositions.stream().map(helper::absolutePos).toList())
                .add("power", helper.absolutePos(powerPos))
                .save(manager.getDisk());

        // load the program
        manager.setProgram(program.stripIndent());
        helper.succeedWhen(() -> {
            // the result chests must be full of cooked chicken
            for (BlockPos resultChestPosition : resultChestPositions) {
                boolean hasEnoughChicken = count(getItemHandler(helper, resultChestPosition), Items.COOKED_CHICKEN)
                                           >= 64 * 27;
                if (!hasEnoughChicken) {
                    helper.fail("Not enough cooked chicken in chest at " + resultChestPosition);
                }
            }
        });
    }


    @SuppressWarnings("DuplicatedCode")
    @GameTest(template = "25x3x25", timeoutTicks = 20 * 20)
    public static void thermal_phyto_array(GameTestHelper helper) {
        // designate positions
        var phytoPositions = new ArrayList<BlockPos>();
        var resultChestPositions = new ArrayList<BlockPos>();
        var seedChestPositions = new ArrayList<BlockPos>();
        var managerPos = new BlockPos(0, 2, 0);
        var powerPos = new BlockPos(1, 2, 0);
        var waterPos = new BlockPos(2, 2, 0);

        // set up power
        helper.setBlock(powerPos, MekanismBlocks.ULTIMATE_ENERGY_CUBE.getBlock());
        helper
                .getBlockEntity(powerPos)
                .getCapability(ForgeCapabilities.ENERGY, Direction.UP)
                .ifPresent(energy -> energy.receiveEnergy(Integer.MAX_VALUE, false));

        // set up water
        helper.setBlock(waterPos, MekanismBlocks.CREATIVE_FLUID_TANK.getBlock());
        TileEntityFluidTank tank = (TileEntityFluidTank) helper.getBlockEntity(waterPos);
        tank.setFluidInTank(0, new FluidStack(Fluids.WATER, Integer.MAX_VALUE));

        // set up phytos
        var phytoBlock = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("thermal", "machine_insolator"));
        SFM.LOGGER.debug("Setting up phytos with block {}", phytoBlock);
        for (int x = 0; x < 25; x++) {
            for (int z = 1; z < 25; z++) {
                BlockPos cableBelow = new BlockPos(x, 2, z);
                helper.setBlock(cableBelow, SFMBlocks.CABLE_BLOCK.get());
                BlockPos phytoPos = new BlockPos(x, 3, z);
                helper.setBlock(phytoPos, phytoBlock);
                phytoPositions.add(phytoPos);
                var phyto = (MachineInsolatorTile) helper.getBlockEntity(phytoPos);
                phyto.setSideConfig(Direction.UP, MachineInsolatorTile.SideConfig.SIDE_INPUT);
                phyto.setSideConfig(Direction.DOWN, MachineInsolatorTile.SideConfig.SIDE_OUTPUT);
            }
        }

        // set up destinations
        SFM.LOGGER.debug("Setting up destinations");
        for (int i = 3; i <= 4; i++) {
            BlockPos pos = new BlockPos(i, 2, 0);
            helper.setBlock(pos, SFMBlocks.TEST_BARREL_BLOCK.get());
            resultChestPositions.add(pos);
        }

        // set up ingredients
        //noinspection NonStrictComparisonCanBeEquality
        for (int i = 6; i <= 6; i++) {
            BlockPos pos = new BlockPos(i, 2, 0);
            helper.setBlock(pos, SFMBlocks.TEST_BARREL_BLOCK.get());
            seedChestPositions.add(pos);
            var items = new Item[]{
                    Items.BEETROOT_SEEDS,
                    Items.MELON_SEEDS,
                    Items.PUMPKIN_SEEDS,
                    Items.WHEAT_SEEDS,
                    SFMResourceTypes.ITEM.get().getRegistry().getValue(new ResourceLocation("thermal", "phytogro")),
                    SFMResourceTypes.ITEM.get().getRegistry().getValue(new ResourceLocation("thermal", "phytogro")),
                    SFMResourceTypes.ITEM.get().getRegistry().getValue(new ResourceLocation("thermal", "phytogro")),
                    SFMResourceTypes.ITEM.get().getRegistry().getValue(new ResourceLocation("thermal", "phytogro")),
                    SFMResourceTypes.ITEM.get().getRegistry().getValue(new ResourceLocation("thermal", "phytogro")),
            };
            int slot = 0;
            for (Item item : items) {
                getItemHandler(helper, pos).insertItem(slot++, new ItemStack(item, 64), false);
            }
        }

        // set up the manager
        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());
        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(managerPos);
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));

        // create the program
        var program = """
                    NAME "thermal phyto array test"
                    EVERY 20 TICKS DO
                        INPUT *seed* FROM seeds
                        OUTPUT RETAIN 2 EACH *seed* TO phytos TOP SIDE SLOTS 0
                    FORGET
                        INPUT FROM seeds
                        OUTPUT RETAIN 2 phytogro TO EACH phytos TOP SIDE SLOTS 1
                    FORGET
                        INPUT fluid:: FROM water TOP SIDE
                        OUTPUT fluid:: TO phytos TOP SIDE
                    FORGET
                        INPUT FROM phytos BOTTOM SIDE SLOTS 2-5
                        OUTPUT TO results TOP SIDE
                    FORGET
                        INPUT forge_energy:forge:energy FROM power NORTH SIDE
                        OUTPUT forge_energy:forge:energy TO phytos
                    END
                """.stripTrailing().stripIndent();

        // set the labels
        LabelPositionHolder.empty()
                .addAll("phytos", phytoPositions.stream().map(helper::absolutePos).toList())
                .addAll("seeds", seedChestPositions.stream().map(helper::absolutePos).toList())
                .addAll("results", resultChestPositions.stream().map(helper::absolutePos).toList())
                .add("power", helper.absolutePos(powerPos))
                .add("water", helper.absolutePos(waterPos))
                .save(manager.getDisk());

        // load the program
        manager.setProgram(program.stripIndent());
        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
        });
    }
}
