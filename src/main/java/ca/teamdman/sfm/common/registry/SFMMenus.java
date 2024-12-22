package ca.teamdman.sfm.common.registry;


import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.client.ClientRaycastHelpers;
import ca.teamdman.sfm.common.blockentity.ManagerBlockEntity;
import ca.teamdman.sfm.common.blockentity.TestBarrelTankBlockEntity;
import ca.teamdman.sfm.common.containermenu.ManagerContainerMenu;
import ca.teamdman.sfm.common.containermenu.TestBarrelTankContainerMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SFMMenus {
    private static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(
            ForgeRegistries.MENU_TYPES,
            SFM.MOD_ID
    );

    public static void register(IEventBus bus) {
        MENU_TYPES.register(bus);
    }

    public static final RegistryObject<MenuType<ManagerContainerMenu>> MANAGER_MENU = MENU_TYPES.register(
            "manager",
            () -> IForgeMenuType.create(
                    new IContainerFactory<>() {
                        @Override
                        public ManagerContainerMenu create(
                                int windowId,
                                Inventory inv,
                                FriendlyByteBuf data
                        ) {
                            return new ManagerContainerMenu(
                                    windowId,
                                    inv,
                                    data
                            );
                        }

                        @Override
                        public ManagerContainerMenu create(
                                int windowId,
                                Inventory inv
                        ) {
                            return DistExecutor.unsafeRunForDist(
                                    () -> () -> {
                                        BlockEntity be = ClientRaycastHelpers.getLookBlockEntity();
                                        if (!(be instanceof ManagerBlockEntity mbe))
                                            return IContainerFactory.super.create(windowId, inv);
                                        return new ManagerContainerMenu(windowId, inv, mbe);
                                    },
                                    () -> () -> IContainerFactory.super.create(
                                            windowId,
                                            inv
                                    )
                            );
                        }
                    })
    );

    public static final RegistryObject<MenuType<TestBarrelTankContainerMenu>> TEST_BARREL_TANK_MENU = MENU_TYPES.register(
            "test_barrel_tank",
            () -> IForgeMenuType.create(
                    new IContainerFactory<>() {
                        @Override
                        public TestBarrelTankContainerMenu create(
                                int windowId,
                                Inventory inv,
                                FriendlyByteBuf data
                        ) {
                            return new TestBarrelTankContainerMenu(
                                    windowId,
                                    inv,
                                    data
                            );
                        }

                        @Override
                        public TestBarrelTankContainerMenu create(
                                int windowId,
                                Inventory inv
                        ) {
                            return DistExecutor.unsafeRunForDist(
                                    () -> () -> {
                                        BlockEntity unchecked = ClientRaycastHelpers.getLookBlockEntity();
                                        if (!(unchecked instanceof TestBarrelTankBlockEntity blockEntity))
                                            return IContainerFactory.super.create(windowId, inv);
                                        return new TestBarrelTankContainerMenu(windowId, inv, blockEntity);
                                    },
                                    () -> () -> IContainerFactory.super.create(
                                            windowId,
                                            inv
                                    )
                            );
                        }
                    })
    );


}
