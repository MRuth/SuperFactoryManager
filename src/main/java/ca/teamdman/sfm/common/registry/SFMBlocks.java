package ca.teamdman.sfm.common.registry;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.common.block.*;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


public class SFMBlocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, SFM.MOD_ID);
    public static final RegistryObject<ManagerBlock> MANAGER_BLOCK = BLOCKS.register("manager", ManagerBlock::new);
    public static final RegistryObject<TunnelledManagerBlock> TUNNELLED_MANAGER_BLOCK = BLOCKS.register(
            "tunnelled_manager",
            TunnelledManagerBlock::new
    );
    public static final RegistryObject<PrintingPressBlock> PRINTING_PRESS_BLOCK = BLOCKS.register(
            "printing_press",
            PrintingPressBlock::new
    );
    public static final RegistryObject<WaterTankBlock> WATER_TANK_BLOCK = BLOCKS.register(
            "water_tank",
            WaterTankBlock::new
    );
    public static final RegistryObject<CableBlock> CABLE_BLOCK = BLOCKS.register("cable", CableBlock::new);
    public static final RegistryObject<CableFacadeBlock> CABLE_FACADE_BLOCK = BLOCKS.register(
            "cable_facade",
            CableFacadeBlock::new
    );
    public static final RegistryObject<FancyCableBlock> FANCY_CABLE_BLOCK = BLOCKS.register(
            "fancy_cable",
            FancyCableBlock::new
    );
    public static final RegistryObject<FancyCableFacadeBlock> FANCY_CABLE_FACADE_BLOCK = BLOCKS.register(
            "fancy_cable_facade",
            FancyCableFacadeBlock::new
    );
    public static final RegistryObject<BatteryBlock> BATTERY_BLOCK = BLOCKS.register("battery", BatteryBlock::new);
    public static final RegistryObject<TestBarrelBlock> TEST_BARREL_BLOCK = BLOCKS.register(
            "test_barrel",
            TestBarrelBlock::new
    );
    public static final RegistryObject<TestBarrelTankBlock> TEST_BARREL_TANK_BLOCK = BLOCKS.register(
            "test_barrel_tank",
            TestBarrelTankBlock::new
    );

    public static Set<? extends RegistryObject<Block>> getBlocks() {
        return new HashSet<>(BLOCKS.getEntries());
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }

}
