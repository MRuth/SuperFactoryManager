package ca.teamdman.sfm.common.registry;


import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.common.blockentity.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

@SuppressWarnings("DataFlowIssue")
public final class SFMBlockEntities {

    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(
            ForgeRegistries.BLOCK_ENTITY_TYPES,
            SFM.MOD_ID
    );

    public static void register(IEventBus bus) {
        BLOCK_ENTITY_TYPES.register(bus);
    }

    public static final RegistryObject<BlockEntityType<ManagerBlockEntity>> MANAGER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "manager",
            () -> BlockEntityType.Builder
                    .of(ManagerBlockEntity::new, SFMBlocks.MANAGER_BLOCK.get())
                    .build(null)
    );
    public static final RegistryObject<BlockEntityType<TunnelledManagerBlockEntity>> TUNNELLED_MANAGER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "tunnelled_manager",
            () -> BlockEntityType.Builder
                    .of(TunnelledManagerBlockEntity::new, SFMBlocks.TUNNELLED_MANAGER_BLOCK.get())
                    .build(null)
    );
    public static final Supplier<BlockEntityType<CableFacadeBlockEntity>> CABLE_FACADE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "cable_facade",
            () -> BlockEntityType.Builder
                    .of(CableFacadeBlockEntity::new, SFMBlocks.CABLE_FACADE_BLOCK.get())
                    .build(null)
    );
    public static final Supplier<BlockEntityType<FancyCableFacadeBlockEntity>> FANCY_CABLE_FACADE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "fancy_cable_facade",
            () -> BlockEntityType.Builder
                    .of(FancyCableFacadeBlockEntity::new, SFMBlocks.FANCY_CABLE_FACADE_BLOCK.get())
                    .build(null)
    );
    public static final RegistryObject<BlockEntityType<PrintingPressBlockEntity>> PRINTING_PRESS_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "printing_press",
            () -> BlockEntityType.Builder
                    .of(PrintingPressBlockEntity::new, SFMBlocks.PRINTING_PRESS_BLOCK.get())
                    .build(null)
    );

    public static final RegistryObject<BlockEntityType<WaterTankBlockEntity>> WATER_TANK_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "water_tank",
            () -> BlockEntityType.Builder
                    .of(WaterTankBlockEntity::new, SFMBlocks.WATER_TANK_BLOCK.get())
                    .build(null)
    );

    public static final RegistryObject<BlockEntityType<BatteryBlockEntity>> BATTERY_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "battery",
            () -> BlockEntityType.Builder
                    .of(BatteryBlockEntity::new, SFMBlocks.BATTERY_BLOCK.get())
                    .build(null)
    );

    public static final Supplier<BlockEntityType<TestBarrelBlockEntity>> TEST_BARREL_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "test_barrel",
            () -> BlockEntityType.Builder
                    .of(TestBarrelBlockEntity::new, SFMBlocks.TEST_BARREL_BLOCK.get())
                    .build(null)
    );

    public static final Supplier<BlockEntityType<TestBarrelTankBlockEntity>> TEST_BARREL_TANK_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "test_barrel_tank",
            () -> BlockEntityType.Builder
                    .of(TestBarrelTankBlockEntity::new, SFMBlocks.TEST_BARREL_BLOCK.get())
                    .build(null)
    );


}
