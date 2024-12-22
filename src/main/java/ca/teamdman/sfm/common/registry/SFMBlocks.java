package ca.teamdman.sfm.common.registry;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.common.block.*;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;


public class SFMBlocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, SFM.MOD_ID);
    public static final RegistryObject<Block> MANAGER_BLOCK = BLOCKS.register("manager", ManagerBlock::new);
    public static final RegistryObject<Block> TUNNELLED_MANAGER_BLOCK = BLOCKS.register("tunnelled_manager", TunnelledManagerBlock::new);
    public static final RegistryObject<Block> PRINTING_PRESS_BLOCK = BLOCKS.register(
            "printing_press",
            PrintingPressBlock::new
    );
    public static final RegistryObject<Block> WATER_TANK_BLOCK = BLOCKS.register("water_tank", WaterTankBlock::new);
    public static final RegistryObject<Block> CABLE_BLOCK = BLOCKS.register("cable", CableBlock::new);
    public static final RegistryObject<Block> CABLE_FACADE_BLOCK = BLOCKS.register("cable_facade", CableFacadeBlock::new);
    public static final RegistryObject<Block> FANCY_CABLE_BLOCK = BLOCKS.register("fancy_cable", FancyCableBlock::new);
    public static final RegistryObject<Block> FANCY_CABLE_FACADE_BLOCK = BLOCKS.register("fancy_cable_facade", FancyCableFacadeBlock::new);
    public static final RegistryObject<Block> BATTERY_BLOCK = BLOCKS.register("battery", BatteryBlock::new);
    public static final RegistryObject<Block> TEST_BARREL_BLOCK = BLOCKS.register("test_barrel", TestBarrelBlock::new);
    public static final RegistryObject<Block> TEST_BARREL_TANK_BLOCK = BLOCKS.register("test_barrel_tank", TestBarrelTankBlock::new);


    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }

}
