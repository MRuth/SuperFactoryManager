package ca.teamdman.sfm.common.registry;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.common.capabilityprovidermapper.BlockEntityCapabilityProviderMapper;
import ca.teamdman.sfm.common.capabilityprovidermapper.CapabilityProviderMapper;
import ca.teamdman.sfm.common.capabilityprovidermapper.CauldronCapabilityProviderMapper;
import ca.teamdman.sfm.common.capabilityprovidermapper.ae2.InterfaceCapabilityProviderMapper;
import ca.teamdman.sfm.common.compat.SFMModCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.CapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Supplier;

public class SFMCapabilityProviderMappers {
    public static final  ResourceLocation                                   REGISTRY_ID      = new ResourceLocation(
            SFM.MOD_ID,
            "capability_provider_mappers"
    );
    private static final DeferredRegister<CapabilityProviderMapper>         MAPPERS          = DeferredRegister.create(
            REGISTRY_ID,
            SFM.MOD_ID
    );
    public static final  Supplier<IForgeRegistry<CapabilityProviderMapper>> DEFERRED_MAPPERS = MAPPERS.makeRegistry(() -> new RegistryBuilder<CapabilityProviderMapper>().setName(
            REGISTRY_ID));

    @SuppressWarnings("unused")
    public static final RegistryObject<BlockEntityCapabilityProviderMapper> BLOCK_ENTITY_MAPPER = MAPPERS.register(
            "block_entity",
            BlockEntityCapabilityProviderMapper::new
    );

    @SuppressWarnings("unused")
    public static final RegistryObject<CauldronCapabilityProviderMapper> CAULDRON_MAPPER = MAPPERS.register(
            "cauldron",
            CauldronCapabilityProviderMapper::new
    );

    public static void register(IEventBus bus) {
        MAPPERS.register(bus);
    }

    static {
        if (SFMModCompat.isAE2Loaded()) {
            MAPPERS.register("ae2/interface", InterfaceCapabilityProviderMapper::new);
        }
    }

    /**
     * Find a {@link CapabilityProvider} as provided by the registered capability provider mappers.
     * If multiple {@link CapabilityProviderMapper}s match, the first one is returned.
     */
    @SuppressWarnings("UnstableApiUsage") // for the javadoc lol
    public static @Nullable ICapabilityProvider discoverCapabilityProvider(
            Level level,
            BlockPos pos
    ) {
        if (!level.isLoaded(pos)) return null;

        Collection<CapabilityProviderMapper> mappers = DEFERRED_MAPPERS.get().getValues();
        CapabilityProviderMapper beMapper = null;
        for (CapabilityProviderMapper mapper : mappers) {
            if (mapper instanceof BlockEntityCapabilityProviderMapper) {
                beMapper = mapper;
                continue;
            }
            ICapabilityProvider capabilityProvider = mapper.getProviderFor(level, pos);
            if (capabilityProvider != null) {
                return capabilityProvider;
            }
        }

        return beMapper != null ? beMapper.getProviderFor(level, pos) : null;
    }
}
