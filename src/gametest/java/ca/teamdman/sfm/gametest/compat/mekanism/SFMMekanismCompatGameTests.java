package ca.teamdman.sfm.gametest.compat.mekanism;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.gametest.SFMGameTestBase;
import ca.teamdman.sfm.common.blockentity.ManagerBlockEntity;
import ca.teamdman.sfm.common.program.LabelPositionHolder;
import ca.teamdman.sfm.common.registry.SFMBlocks;
import ca.teamdman.sfm.common.registry.SFMItems;
import mekanism.api.chemical.infuse.InfusionStack;
import mekanism.api.math.FloatingLong;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.registries.MekanismInfuseTypes;
import mekanism.common.tier.BinTier;
import mekanism.common.tier.ChemicalTankTier;
import mekanism.common.tier.EnergyCubeTier;
import mekanism.common.tile.TileEntityBin;
import mekanism.common.tile.TileEntityChemicalTank;
import mekanism.common.tile.TileEntityEnergyCube;
import mekanism.common.tile.multiblock.TileEntityInductionPort;
import mekanism.common.util.UnitDisplayUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SuppressWarnings({"OptionalGetWithoutIsPresent", "DuplicatedCode", "DataFlowIssue"})
@GameTestHolder(SFM.MOD_ID)
@PrefixGameTestTemplate(false)
public class SFMMekanismCompatGameTests extends SFMGameTestBase {
    @GameTest(template = "3x2x1")
    public static void mek_chemtank_infusion_empty(GameTestHelper helper) {
        // designate positions
        var leftPos = new BlockPos(2, 2, 0);
        var rightPos = new BlockPos(0, 2, 0);
        var managerPos = new BlockPos(1, 2, 0);

        // set up the world
        helper.setBlock(leftPos, MekanismBlocks.ULTIMATE_CHEMICAL_TANK.getBlock());
        var leftTank = ((TileEntityChemicalTank) helper.getBlockEntity(leftPos));
        helper.setBlock(rightPos, MekanismBlocks.ULTIMATE_CHEMICAL_TANK.getBlock());
        var rightTank = ((TileEntityChemicalTank) helper.getBlockEntity(rightPos));
        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());
        var manager = ((ManagerBlockEntity) helper.getBlockEntity(managerPos));

        // set up the program
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                   EVERY 20 TICKS DO
                                      INPUT infusion:*:* FROM a NORTH SIDE -- mek can extract from front by default
                                      OUTPUT infusion:*:* TO b TOP SIDE -- mek can insert to top by default
                                   END
                                   """.stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .add("b", helper.absolutePos(rightPos))
                .save(manager.getDisk());


        // ensure it can move into an empty tank
        leftTank.getInfusionTank().setStack(new InfusionStack(MekanismInfuseTypes.REDSTONE.get(), 1_000_000L));
        rightTank.getInfusionTank().setStack(InfusionStack.EMPTY);
        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            assertTrue(leftTank.getInfusionTank().getStack().isEmpty(), "Contents did not depart");
            assertTrue(rightTank.getInfusionTank().getStack().getAmount() == 1_000_000L, "Contents did not arrive");
        });
    }

    @GameTest(template = "3x2x1")
    public static void mek_chemtank_infusion_some(GameTestHelper helper) {
        // designate positions
        var leftPos = new BlockPos(2, 2, 0);
        var rightPos = new BlockPos(0, 2, 0);
        var managerPos = new BlockPos(1, 2, 0);

        // set up the world
        helper.setBlock(leftPos, MekanismBlocks.ULTIMATE_CHEMICAL_TANK.getBlock());
        var leftTank = ((TileEntityChemicalTank) helper.getBlockEntity(leftPos));
        helper.setBlock(rightPos, MekanismBlocks.ULTIMATE_CHEMICAL_TANK.getBlock());
        var rightTank = ((TileEntityChemicalTank) helper.getBlockEntity(rightPos));
        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());
        var manager = ((ManagerBlockEntity) helper.getBlockEntity(managerPos));

        // set up the program
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                   EVERY 20 TICKS DO
                                      INPUT infusion:*:* FROM a NORTH SIDE -- mek can extract from front by default
                                      OUTPUT infusion:*:* TO b TOP SIDE -- mek can insert to top by default
                                   END
                                   """.stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .add("b", helper.absolutePos(rightPos))
                .save(manager.getDisk());


        // ensure it can move when there's already some in the destination
        leftTank.getInfusionTank().setStack(new InfusionStack(MekanismInfuseTypes.REDSTONE.get(), 1_000_000L));
        rightTank.getInfusionTank().setStack(new InfusionStack(MekanismInfuseTypes.REDSTONE.get(), 1_000_000L));
        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            assertTrue(leftTank.getInfusionTank().getStack().isEmpty(), "Contents did not depart");
            assertTrue(rightTank.getInfusionTank().getStack().getAmount() == 2_000_000L, "Contents did not arrive");
        });
    }

    @GameTest(template = "3x2x1")
    public static void mek_chemtank_infusion_full(GameTestHelper helper) {
        // designate positions
        var leftPos = new BlockPos(2, 2, 0);
        var rightPos = new BlockPos(0, 2, 0);
        var managerPos = new BlockPos(1, 2, 0);

        // set up the world
        helper.setBlock(leftPos, MekanismBlocks.ULTIMATE_CHEMICAL_TANK.getBlock());
        var leftTank = ((TileEntityChemicalTank) helper.getBlockEntity(leftPos));
        helper.setBlock(rightPos, MekanismBlocks.ULTIMATE_CHEMICAL_TANK.getBlock());
        var rightTank = ((TileEntityChemicalTank) helper.getBlockEntity(rightPos));
        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());
        var manager = ((ManagerBlockEntity) helper.getBlockEntity(managerPos));

        // set up the program
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                   EVERY 20 TICKS DO
                                     INPUT infusion:*:* FROM a NORTH SIDE -- mek can extract from front by default
                                     OUTPUT infusion:*:* TO b TOP SIDE -- mek can insert to top by default
                                   END
                                   """.stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .add("b", helper.absolutePos(rightPos))
                .save(manager.getDisk());

        // ensure it can move into a nearly full tank
        leftTank.getInfusionTank().setStack(new InfusionStack(MekanismInfuseTypes.REDSTONE.get(), 2_000_000L));
        rightTank
                .getInfusionTank()
                .setStack(new InfusionStack(
                        MekanismInfuseTypes.REDSTONE.get(),
                        ChemicalTankTier.ULTIMATE.getStorage() - 1_000_000L
                ));
        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            assertTrue(leftTank.getInfusionTank().getStack().getAmount() == 1_000_000L, "Contents did not depart");
            assertTrue(
                    rightTank.getInfusionTank().getStack().getAmount() == ChemicalTankTier.ULTIMATE.getStorage(),
                    "Contents did not arrive"
            );
        });
    }


    @GameTest(template = "3x2x1")
    public static void mek_bin_empty(GameTestHelper helper) {
        // designate positions
        var leftPos = new BlockPos(2, 2, 0);
        var rightPos = new BlockPos(0, 2, 0);
        var managerPos = new BlockPos(1, 2, 0);

        // set up the world
        helper.setBlock(leftPos, MekanismBlocks.ULTIMATE_BIN.getBlock());
        var left = ((TileEntityBin) helper.getBlockEntity(leftPos));
        helper.setBlock(rightPos, MekanismBlocks.ULTIMATE_BIN.getBlock());
        var right = ((TileEntityBin) helper.getBlockEntity(rightPos));
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

        left.getBinSlot().setStack(new ItemStack(Items.COAL, BinTier.ULTIMATE.getStorage()));
        right.getBinSlot().setEmpty();
        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            assertTrue(left.getBinSlot().getCount() == BinTier.ULTIMATE.getStorage() - 64, "Contents did not depart");
            assertTrue(right.getBinSlot().getCount() == 64, "Contents did not arrive");
            assertTrue(right.getBinSlot().getStack().getItem() == Items.COAL, "Contents wrong type");
            
        });
    }


    @GameTest(template = "3x2x1")
    public static void mek_bin_some(GameTestHelper helper) {
        // designate positions
        var leftPos = new BlockPos(2, 2, 0);
        var rightPos = new BlockPos(0, 2, 0);
        var managerPos = new BlockPos(1, 2, 0);

        // set up the world
        helper.setBlock(leftPos, MekanismBlocks.ULTIMATE_BIN.getBlock());
        var left = ((TileEntityBin) helper.getBlockEntity(leftPos));
        helper.setBlock(rightPos, MekanismBlocks.ULTIMATE_BIN.getBlock());
        var right = ((TileEntityBin) helper.getBlockEntity(rightPos));
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

        left.getBinSlot().setStack(new ItemStack(Items.DIAMOND, 100));
        right.getBinSlot().setStack(new ItemStack(Items.DIAMOND, 100));
        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            assertTrue(left.getBinSlot().getCount() == 100 - 64, "Contents did not depart");
            assertTrue(right.getBinSlot().getCount() == 100 + 64, "Contents did not arrive");
            assertTrue(right.getBinSlot().getStack().getItem() == Items.DIAMOND, "Contents wrong type");
            
        });
    }


    @GameTest(template = "3x2x1")
    public static void mek_bin_full(GameTestHelper helper) {
        // designate positions
        var leftPos = new BlockPos(2, 2, 0);
        var rightPos = new BlockPos(0, 2, 0);
        var managerPos = new BlockPos(1, 2, 0);

        // set up the world
        helper.setBlock(leftPos, MekanismBlocks.ULTIMATE_BIN.getBlock());
        var left = ((TileEntityBin) helper.getBlockEntity(leftPos));
        helper.setBlock(rightPos, MekanismBlocks.ULTIMATE_BIN.getBlock());
        var right = ((TileEntityBin) helper.getBlockEntity(rightPos));
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

        left.getBinSlot().setStack(new ItemStack(Items.STICK, BinTier.ULTIMATE.getStorage()));
        right.getBinSlot().setStack(new ItemStack(Items.STICK, BinTier.ULTIMATE.getStorage() - 32));
        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            assertTrue(left.getBinSlot().getCount() == BinTier.ULTIMATE.getStorage() - 32, "Contents did not depart");
            assertTrue(right.getBinSlot().getCount() == BinTier.ULTIMATE.getStorage(), "Contents did not arrive");
            assertTrue(right.getBinSlot().getStack().getItem() == Items.STICK, "Contents wrong type");
            
        });
    }


    @GameTest(template = "3x2x1")
    public static void mek_energy_empty(GameTestHelper helper) {
        // designate positions
        var leftPos = new BlockPos(2, 2, 0);
        var rightPos = new BlockPos(0, 2, 0);
        var managerPos = new BlockPos(1, 2, 0);

        // set up the world
        helper.setBlock(leftPos, MekanismBlocks.ULTIMATE_ENERGY_CUBE.getBlock());
        var left = ((TileEntityEnergyCube) helper.getBlockEntity(leftPos));
        helper.setBlock(rightPos, MekanismBlocks.ULTIMATE_ENERGY_CUBE.getBlock());
        var right = ((TileEntityEnergyCube) helper.getBlockEntity(rightPos));
        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());
        var manager = ((ManagerBlockEntity) helper.getBlockEntity(managerPos));

        // set up the program
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                   EVERY 20 TICKS DO
                                     INPUT forge_energy:forge:energy FROM a NORTH SIDE
                                     OUTPUT forge_energy:forge:energy TO b TOP SIDE
                                   END
                                   """.stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .add("b", helper.absolutePos(rightPos))
                .save(manager.getDisk());

        left.setEnergy(0, EnergyCubeTier.ULTIMATE.getMaxEnergy());
        right.setEnergy(0, FloatingLong.ZERO);
        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            assertTrue(left.getEnergy(0).equals(FloatingLong.ZERO), "Contents did not depart");
            assertTrue(right.getEnergy(0).equals(EnergyCubeTier.ULTIMATE.getMaxEnergy()), "Contents did not arrive");
            
        });
    }


    @GameTest(template = "3x2x1")
    public static void mek_energy_some(GameTestHelper helper) {
        // designate positions
        var leftPos = new BlockPos(2, 2, 0);
        var rightPos = new BlockPos(0, 2, 0);
        var managerPos = new BlockPos(1, 2, 0);

        // set up the world
        helper.setBlock(leftPos, MekanismBlocks.ULTIMATE_ENERGY_CUBE.getBlock());
        var left = ((TileEntityEnergyCube) helper.getBlockEntity(leftPos));
        helper.setBlock(rightPos, MekanismBlocks.ULTIMATE_ENERGY_CUBE.getBlock());
        var right = ((TileEntityEnergyCube) helper.getBlockEntity(rightPos));
        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());
        var manager = ((ManagerBlockEntity) helper.getBlockEntity(managerPos));

        // set up the program
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                   EVERY 20 TICKS DO
                                     INPUT forge_energy:forge:energy FROM a NORTH SIDE
                                     OUTPUT forge_energy:forge:energy TO b TOP SIDE
                                   END
                                   """.stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .add("b", helper.absolutePos(rightPos))
                .save(manager.getDisk());

        left.setEnergy(0, FloatingLong.create(1_000));
        right.setEnergy(0, FloatingLong.create(1_000));
        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            assertTrue(left.getEnergy(0).equals(FloatingLong.ZERO), "Contents did not depart");
            assertTrue(right.getEnergy(0).equals(FloatingLong.create(2_000)), "Contents did not arrive");
            
        });
    }


    @GameTest(template = "3x2x1")
    public static void mek_energy_full(GameTestHelper helper) {
        // designate positions
        var leftPos = new BlockPos(2, 2, 0);
        var rightPos = new BlockPos(0, 2, 0);
        var managerPos = new BlockPos(1, 2, 0);

        // set up the world
        helper.setBlock(leftPos, MekanismBlocks.ULTIMATE_ENERGY_CUBE.getBlock());
        var left = ((TileEntityEnergyCube) helper.getBlockEntity(leftPos));
        helper.setBlock(rightPos, MekanismBlocks.ULTIMATE_ENERGY_CUBE.getBlock());
        var right = ((TileEntityEnergyCube) helper.getBlockEntity(rightPos));
        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());
        var manager = ((ManagerBlockEntity) helper.getBlockEntity(managerPos));

        // set up the program
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                   EVERY 20 TICKS DO
                                     INPUT forge_energy:forge:energy FROM a NORTH SIDE
                                     OUTPUT forge_energy:forge:energy TO b TOP SIDE
                                   END
                                   """.stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .add("b", helper.absolutePos(rightPos))
                .save(manager.getDisk());

        left.setEnergy(0, EnergyCubeTier.ULTIMATE.getMaxEnergy());
        right.setEnergy(0, EnergyCubeTier.ULTIMATE.getMaxEnergy().subtract(1_000));
        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            assertTrue(
                    left.getEnergy(0).equals(EnergyCubeTier.ULTIMATE.getMaxEnergy().subtract(1_000)),
                    "Contents did not depart"
            );
            assertTrue(right.getEnergy(0).equals(EnergyCubeTier.ULTIMATE.getMaxEnergy()), "Contents did not arrive");
            
        });
    }


    @GameTest(template = "3x2x1")
    public static void mek_energy_one(GameTestHelper helper) {
        // designate positions
        var leftPos = new BlockPos(2, 2, 0);
        var rightPos = new BlockPos(0, 2, 0);
        var managerPos = new BlockPos(1, 2, 0);

        // set up the world
        helper.setBlock(leftPos, MekanismBlocks.ULTIMATE_ENERGY_CUBE.getBlock());
        var left = ((TileEntityEnergyCube) helper.getBlockEntity(leftPos));
        helper.setBlock(rightPos, MekanismBlocks.ULTIMATE_ENERGY_CUBE.getBlock());
        var right = ((TileEntityEnergyCube) helper.getBlockEntity(rightPos));
        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());
        var manager = ((ManagerBlockEntity) helper.getBlockEntity(managerPos));

        // set up the program
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                   EVERY 20 TICKS DO
                                     INPUT 1 forge_energy:forge:energy FROM a NORTH SIDE
                                     OUTPUT forge_energy:forge:energy TO b TOP SIDE
                                   END
                                   """.stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(leftPos))
                .add("b", helper.absolutePos(rightPos))
                .save(manager.getDisk());

        left.setEnergy(0, FloatingLong.create(100));
        right.setEnergy(0, FloatingLong.ZERO);
        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            assertTrue(
                    left
                            .getEnergy(0)
                            .equals(FloatingLong
                                            .create(100)
                                            .subtract(UnitDisplayUtils.EnergyUnit.FORGE_ENERGY.convertFrom(1))),
                    "Contents did not depart"
            );
            assertTrue(
                    right.getEnergy(0).equals(UnitDisplayUtils.EnergyUnit.FORGE_ENERGY.convertFrom(1)),
                    "Contents did not arrive"
            );
            
        });
    }


    @GameTest(template = "25x3x25")
    public static void many_lava_cauldrons(GameTestHelper helper) {
        // designate positions
        var sourceBlocks = new ArrayList<BlockPos>();
        var destBlocks = new ArrayList<BlockPos>();
        var managerPos = new BlockPos(0, 2, 0);

        // set up cauldrons
        for (int x = 0; x < 25; x++) {
            for (int z = 1; z < 25; z++) {
                helper.setBlock(new BlockPos(x, 2, z), SFMBlocks.CABLE_BLOCK.get());
                helper.setBlock(new BlockPos(x, 3, z), Blocks.LAVA_CAULDRON);
                sourceBlocks.add(new BlockPos(x, 3, z));
            }
        }

        // set up tanks
        for (int i = 1; i < 25; i++) {
            BlockPos tankPos = new BlockPos(i, 2, 0);
            helper.setBlock(tankPos, MekanismBlocks.BASIC_FLUID_TANK.getBlock());
            destBlocks.add(tankPos);
        }

        // set up the manager
        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());
        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(managerPos);
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));

        // create the program
        var program = """
                    NAME "many inventory lag test"

                    EVERY 20 TICKS DO
                        INPUT fluid:*:* FROM source
                        OUTPUT fluid:*:* TO dest TOP SIDE
                    END
                """;

        // set the labels
        LabelPositionHolder.empty()
                .addAll("source", sourceBlocks.stream().map(helper::absolutePos).toList())
                .addAll("dest", destBlocks.stream().map(helper::absolutePos).toList())
                .save(manager.getDisk());

        // load the program
        manager.setProgram(program);
        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            sourceBlocks.forEach(pos -> helper.assertBlock(
                    pos,
                    Blocks.CAULDRON::equals,
                    () -> "Cauldron did not empty"
            ));
            int found = destBlocks
                    .stream()
                    .map(helper::getBlockEntity)
                    .map(be -> be.getCapability(ForgeCapabilities.FLUID_HANDLER))
                    .map(x -> x.orElse(null))
                    .peek(Objects::requireNonNull)
                    .map(x -> x.getFluidInTank(0))
                    .mapToInt(FluidStack::getAmount)
                    .sum();
            assertTrue(found == 1000 * 25 * 24, "Not all fluids were moved (found " + found + ")");
            

        });
    }

    @GameTest(template = "3x4x3")
    public static void multi_fluid(GameTestHelper helper) {
        var a1Pos = new BlockPos(2, 2, 1);
        var a2Pos = new BlockPos(1, 2, 0);
        var b1Pos = new BlockPos(1, 2, 2);
        var b2Pos = new BlockPos(0, 2, 1);
        var managerPos = new BlockPos(1, 2, 1);
        helper.setBlock(a1Pos, MekanismBlocks.BASIC_FLUID_TANK.getBlock());
        helper.setBlock(a2Pos, MekanismBlocks.BASIC_FLUID_TANK.getBlock());
        helper.setBlock(b1Pos, MekanismBlocks.BASIC_FLUID_TANK.getBlock());
        helper.setBlock(b2Pos, MekanismBlocks.BASIC_FLUID_TANK.getBlock());
        var a1 = helper
                .getBlockEntity(a1Pos)
                .getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH)
                .orElse(null);
        var a2 = helper
                .getBlockEntity(a2Pos)
                .getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH)
                .orElse(null);
        var b1 = helper
                .getBlockEntity(b1Pos)
                .getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH)
                .orElse(null);
        var b2 = helper
                .getBlockEntity(b2Pos)
                .getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH)
                .orElse(null);

        a1.fill(new FluidStack(Fluids.WATER, 3000), IFluidHandler.FluidAction.EXECUTE);
        a2.fill(new FluidStack(Fluids.LAVA, 3000), IFluidHandler.FluidAction.EXECUTE);

        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());
        var manager = ((ManagerBlockEntity) helper.getBlockEntity(managerPos));
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));
        manager.setProgram("""
                                   EVERY 20 TICKS DO
                                     INPUT fluid:: FROM a NORTH SIDE
                                     OUTPUT fluid::lava, fluid::water TO b TOP SIDE
                                   END
                                   """.stripIndent());

        // set the labels
        LabelPositionHolder.empty()
                .add("a", helper.absolutePos(a1Pos))
                .add("a", helper.absolutePos(a2Pos))
                .add("b", helper.absolutePos(b1Pos))
                .add("b", helper.absolutePos(b2Pos))
                .save(manager.getDisk());

        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            assertTrue(a1.getFluidInTank(0).isEmpty(), "a1 did not empty");
            assertTrue(a2.getFluidInTank(0).isEmpty(), "a2 did not empty");
            assertTrue(b1.getFluidInTank(0).getFluid() == Fluids.WATER, "b1 did not fill with water");
            assertTrue(b2.getFluidInTank(0).getFluid() == Fluids.LAVA, "b2 did not fill with lava");
        });
    }


    @GameTest(template = "25x3x25")
    public static void mek_induction(GameTestHelper helper) {
        // designate positions
        var managerPos = new BlockPos(1, 3, 0);
        var powerCubePos = new BlockPos(1, 2, 0);
        var inductionBeginPos = new BlockPos(0, 2, 1);
        var inductionInput = new BlockPos(1, 3, 1);

        // set up induction matrix
        for (int x = 0; x < 18; x++) {
            for (int z = 0; z < 18; z++) {
                for (int y = 0; y < 18; y++) {
                    //noinspection ExtractMethodRecommender
                    boolean isOutside = x == 0 || x == 17 || z == 0 || z == 17 || y == 0 || y == 17;
                    Block block;
                    if (isOutside) {
                        block = MekanismBlocks.INDUCTION_CASING.getBlock();
                    } else {
                        if (y == 1) {
                            block = MekanismBlocks.ULTIMATE_INDUCTION_CELL.getBlock();
                        } else {
                            block = MekanismBlocks.ULTIMATE_INDUCTION_PROVIDER.getBlock();
                        }
                    }
                    helper.setBlock(inductionBeginPos.offset(x, y, z), block);
                }
            }
        }
        helper.setBlock(inductionInput, MekanismBlocks.INDUCTION_PORT.getBlock());
        var inductionPort = (TileEntityInductionPort) helper.getBlockEntity(inductionInput);

        // set up the energy source
        helper.setBlock(powerCubePos, MekanismBlocks.CREATIVE_ENERGY_CUBE.getBlock());

        TileEntityEnergyCube powerCube = (TileEntityEnergyCube) helper.getBlockEntity(powerCubePos);
        powerCube.setEnergy(0, EnergyCubeTier.CREATIVE.getMaxEnergy());

        // set up the manager
        helper.setBlock(managerPos, SFMBlocks.MANAGER_BLOCK.get());
        ManagerBlockEntity manager = (ManagerBlockEntity) helper.getBlockEntity(managerPos);
        manager.setItem(0, new ItemStack(SFMItems.DISK_ITEM.get()));

        // create the program
        long incr = 10_000_000_000L;
        var startingAmount = FloatingLong.create(0L);
        var program = """
                    NAME "induction matrix test"
                    EVERY 20 TICKS DO
                        INPUT %d mekanism_energy:: FROM source NORTH SIDE
                        OUTPUT mekanism_energy:: TO dest NORTH SIDE
                    END
                """.formatted(incr);

        // set the labels
        LabelPositionHolder.empty()
                .addAll("source", List.of(helper.absolutePos(powerCubePos)))
                .addAll("dest", List.of(helper.absolutePos(inductionInput)))
                .save(manager.getDisk());

        // we can't prefill since we can't wait a delay AND use succeedIfManagerDidThing
        // pre-fill the matrix by a little bit
        // we want to make sure SFM doesn't have problems inserting beyond MAX_INT
//        var startingAmount = FloatingLong.create(Integer.MAX_VALUE + incr);
//            inductionPort.insertEnergy(startingAmount, Action.EXECUTE);

        // launch the program
        manager.setProgram(program);
        succeedIfManagerDidThingWithoutLagging(helper, manager, () -> {
            if (!inductionPort.getMultiblock().isFormed()) {
                throw new GameTestAssertException("Induction matrix did not form");
            }

            var expected = startingAmount.add(incr);
            FloatingLong energy = inductionPort.getEnergy(0);
            boolean success = energy.equals(expected);
            assertTrue(
                    success,
                    "Expected energy did not match"
            );
            
        });
    }
}
