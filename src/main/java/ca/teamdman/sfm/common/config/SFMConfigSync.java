package ca.teamdman.sfm.common.config;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.common.localization.LocalizationKeys;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.network.ConfigSync;
import net.minecraftforge.network.HandshakeHandler;
import net.minecraftforge.network.HandshakeMessages;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Supplier;

public class SFMConfigSync {
    /**
     * SERVER configs are synced at login to servers, which can serve as inspiration for how we should update the configs on our own.
     * <p>
     * <p>
     * The {@link ConfigSync#syncConfigs(boolean)} method provides a list of {@link HandshakeMessages.S2CConfigData}
     * Those packets are registered in {@link net.minecraftforge.network.NetworkInitialization#getHandshakeChannel()}
     * Those packets have handler {@link HandshakeHandler#handleConfigSync(HandshakeMessages.S2CConfigData, Supplier)}
     * which calls {@link ConfigSync#receiveSyncedConfig(HandshakeMessages.S2CConfigData, Supplier)}
     * which calls {@link ModConfig#acceptSyncedConfig(byte[])}
     * <p>
     * So we just need to call {@link ModConfig#acceptSyncedConfig(byte[])}
     * with the results of {@link Files#readAllBytes(Path)} on the updated config file.
     * <p>
     * We can pass the bytes without writing the file ourselves
     * Then we call {@link ModConfig#save()} to write the file back to disk.
     */
    public static ConfigSyncResult updateAndSyncServerConfig(String newConfigToml) {
        SFM.LOGGER.debug("Received config for update and sync:\n{}", newConfigToml);
        CommentedConfig config = TomlFormat.instance().createParser().parse(newConfigToml);
        if (!SFMConfig.SERVER_SPEC.isCorrect(config)) {
            SFM.LOGGER.error("Received invalid config from player");
            return ConfigSyncResult.INVALID_CONFIG;
        }
        ModConfig modConfig = getServerConfig();
        if (modConfig == null) {
            SFM.LOGGER.error("Failed to find server config");
            return ConfigSyncResult.INTERNAL_FAILURE;
        }
        SFM.LOGGER.info("Updated server config");
        modConfig.acceptSyncedConfig(newConfigToml.getBytes());
        SFM.LOGGER.info("Distributing new server config to players");
        SFM.LOGGER.warn("TODO: implement this lol");
        return ConfigSyncResult.SUCCESS;
    }

    private static @Nullable ModConfig getServerConfig() {
        Set<ModConfig> modConfigs = ConfigTracker.INSTANCE.configSets().get(ModConfig.Type.SERVER);
        for (ModConfig modConfig : modConfigs) {
            if (modConfig.getModId().equals(SFM.MOD_ID)) {
                return modConfig;
            }
        }
        return null;
    }

    public enum ConfigSyncResult {
        SUCCESS,
        INVALID_CONFIG,
        INTERNAL_FAILURE;

        public Component component() {
            return switch (this) {
                case SUCCESS -> LocalizationKeys.CONFIG_UPDATE_AND_SYNC_RESULT_SUCCESS.getComponent();
                case INVALID_CONFIG -> LocalizationKeys.CONFIG_UPDATE_AND_SYNC_RESULT_INVALID_CONFIG.getComponent();
                case INTERNAL_FAILURE -> LocalizationKeys.CONFIG_UPDATE_AND_SYNC_RESULT_INTERNAL_FAILURE.getComponent();
            };
        }
    }
}
