package ca.teamdman.sfm.common.config;

import ca.teamdman.sfm.SFM;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Set;

public class SFMConfigTracker {
    private static final HashMap<IConfigSpec<?>, Path> configPaths = new HashMap<>();

    public static @Nullable Path getPathForConfig(IConfigSpec<?> spec) {
        return configPaths.get(spec);
    }

    static @Nullable ModConfig getServerModConfig() {
        Set<ModConfig> modConfigs = ConfigTracker.INSTANCE.configSets().get(ModConfig.Type.SERVER);
        for (ModConfig modConfig : modConfigs) {
            // .equals() doesn't work here
            if (modConfig.getSpec() == SFMConfig.SERVER_SPEC) {
                return modConfig;
            }
        }
        return null;
    }

    static @Nullable ModConfig getClientModConfig() {
        Set<ModConfig> modConfigs = ConfigTracker.INSTANCE.configSets().get(ModConfig.Type.CLIENT);
        for (ModConfig modConfig : modConfigs) {
            // .equals() doesn't work here
            if (modConfig.getSpec() == SFMConfig.CLIENT_SPEC) {
                return modConfig;
            }
        }
        return null;
    }

    @Mod.EventBusSubscriber(modid = SFM.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModConfigEventListeners {
        /**
         * Tracks when configs are loaded
         * <p>
         * See {@link ConfigTracker#openConfig(ModConfig, Path)}
         */
        @SuppressWarnings("JavadocReference")
        @SubscribeEvent
        public static void onConfigLoaded(ModConfigEvent.Loading event) {
            handleConfigEvent(event);
        }

        @SubscribeEvent
        public static void onConfigReloaded(ModConfigEvent.Reloading event) {
            handleConfigEvent(event);
        }

        private static void handleConfigEvent(ModConfigEvent event) {
            ModConfig modConfig = event.getConfig();
            if (modConfig.getModId().equals(SFM.MOD_ID)) {
                IConfigSpec<?> spec = modConfig.getSpec();
                FileConfig fileConfig = getFileConfig(spec);
                if (fileConfig != null) {
                    configPaths.put(spec, fileConfig.getNioPath());
                }
            }
        }

        private static @Nullable FileConfig getFileConfig(IConfigSpec<?> configSpec) {
            Config config = getChildConfig(configSpec);
            if (config instanceof FileConfig fileConfig) {
                return fileConfig;
            }
            return null;
        }

        private static @Nullable Config getChildConfig(IConfigSpec<?> configSpec) {
            if (configSpec instanceof ForgeConfigSpec forgeConfigSpec) {
                try {
                    Field childConfigField = forgeConfigSpec.getClass().getDeclaredField("childConfig");
                    childConfigField.setAccessible(true);
                    return (Config) childConfigField.get(forgeConfigSpec);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    SFM.LOGGER.error("Failed to extract childConfig field", e);
                    return null;
                }
            }
            return null;
        }
    }

    @Mod.EventBusSubscriber(modid = SFM.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class GameConfigEventListeners {
        /**
         * Tracks when configs are unloaded
         * <p>
         * See {@link ConfigTracker#unloadConfigs(ModConfig.Type, Path)}
         * which is called by {@link ServerLifecycleHooks#handleServerStopped(MinecraftServer)}
         */
        @SubscribeEvent
        public static void onServerStopped(ServerStoppedEvent event) {
            configPaths.entrySet().removeIf(entry -> entry.getKey() == SFMConfig.SERVER_SPEC);
        }
    }
}
