package ca.teamdman.sfm.common.registry;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.common.compat.SFMMekanismCompat;
import ca.teamdman.sfm.common.compat.SFMModCompat;
import ca.teamdman.sfm.common.resourcetype.FluidResourceType;
import ca.teamdman.sfm.common.resourcetype.ForgeEnergyResourceType;
import ca.teamdman.sfm.common.resourcetype.ItemResourceType;
import ca.teamdman.sfm.common.resourcetype.ResourceType;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;
import java.util.stream.Stream;

public class SFMResourceTypes {
    public static final ResourceLocation REGISTRY_ID = new ResourceLocation(SFM.MOD_ID, "resource_type");
    private static final DeferredRegister<ResourceType<?, ?, ?>> TYPES = DeferredRegister.create(
            REGISTRY_ID,
            SFM.MOD_ID
    );
    public static final Supplier<IForgeRegistry<ResourceType<?, ?, ?>>> DEFERRED_TYPES = TYPES.makeRegistry(
            () -> new RegistryBuilder<ResourceType<?, ?, ?>>().setName(
                    REGISTRY_ID));
    public static final RegistryObject<ResourceType<ItemStack, Item, IItemHandler>> ITEM = TYPES.register(
            "item",
            ItemResourceType::new
    );
    public static final RegistryObject<ResourceType<FluidStack, Fluid, IFluidHandler>> FLUID = TYPES.register(
            "fluid",
            FluidResourceType::new
    );
    public static final RegistryObject<ResourceType<Integer, Class<Integer>, IEnergyStorage>> FORGE_ENERGY = TYPES.register(
            "forge_energy",
            ForgeEnergyResourceType::new
    );
    private static final Int2ObjectArrayMap<ResourceType<?, ?, ?>> DEFERRED_TYPES_BY_ID = new Int2ObjectArrayMap<>();

    static {
        if (SFMModCompat.isMekanismLoaded()) {
            SFMMekanismCompat.registerResourceTypes(TYPES);
        }
    }

    public static int getResourceTypeCount() {
        return TYPES.getEntries().size();
    }

    public static @Nullable ResourceType<?, ?, ?> fastLookup(
            String resourceTypeNamespace,
            String resourceTypeName
    ) {
        return DEFERRED_TYPES_BY_ID.computeIfAbsent(
                resourceTypeNamespace.hashCode() ^ resourceTypeName.hashCode(),
                i -> DEFERRED_TYPES.get().getValue(new ResourceLocation(resourceTypeNamespace, resourceTypeName))
        );
    }

    public static Stream<Capability<?>> getCapabilities() {
        return TYPES.getEntries().stream().map(RegistryObject::get).map(resourceType -> resourceType.CAPABILITY_KIND);
    }

    public static void register(IEventBus bus) {
        TYPES.register(bus);
    }

    /* TODO: add support for new resource types
     * - mekanism heat
     * - botania mana
     * - ars nouveau source
     * - flux plugs
     * - PNC pressure
     * - PNC heat
     * - nature's aura aura
     * - create rotation
     */
}
