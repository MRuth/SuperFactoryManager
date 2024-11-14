package ca.teamdman.sfm.common.config;

import ca.teamdman.sfm.SFM;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;


/*
2024-11-12
- SFM currently uses COMMON when it seems like it should be SERVER
- SERVER configs are automatically sent to clients
- Search discord for "send config" and "ConfigTracker" to find discussions

- SFM currently sends a packet and receives a packet to display the server config, this should be replaced with showing the config synced from the server using built-in behaviour
- SFM wants to send the updated config TOML but the handler is stubbed. Config needs to be updated from toml, saved, and resent to clients.

MehVahdJukaar — 03/19/2021 7:25 PM
https://discord.com/channels/313125603924639766/725850371834118214/822611868275310592
so I've managed to sync the common config file by sending to the client its data and then
using CONFIG.setConfig(TomlFormat.instance().createParser().parse(data)) like it's done in
ConfigTracker class. However I would like to be able to load the original client side config
file (still common) back up in case I want to edit it. How can I do that?

sleepy sci, on graveyard duty — 03/19/2021 7:42 PM
https://discord.com/channels/313125603924639766/725850371834118214/822615931510718514
the common config is meant for config settings which do not impact any game logic, but would be useful to store/have on both sides (and which can be separate)
server config is for server-controlled values
client config is for client only player-controlled values
common is anything else

sleepy sci, on graveyard duty — 03/19/2021 7:42 PM
https://discord.com/channels/313125603924639766/725850371834118214/822616037417549835
data defined by the server that affects client-side ...
then it should be server config

 */
public class SFMConfig {
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final Server SERVER;
    public static final SFMConfig.Client CLIENT;

    static {
        final Pair<Server, ForgeConfigSpec> serverSpecPair = new ForgeConfigSpec.Builder().configure(Server::new);
        SERVER_SPEC = serverSpecPair.getRight();
        SERVER = serverSpecPair.getLeft();
        final Pair<SFMConfig.Client, ForgeConfigSpec> clientSpecPair = new ForgeConfigSpec.Builder().configure(SFMConfig.Client::new);
        CLIENT_SPEC = clientSpecPair.getRight();
        CLIENT = clientSpecPair.getLeft();
    }

    /**
     * Get a config value in a way that doesn't fail when running tests
     */
    public static <T> T getOrDefault(ForgeConfigSpec.ConfigValue<T> configValue) {
        try {
            return configValue.get();
        } catch (Exception e) {
            return configValue.getDefault();
        }
    }

    public static void register(ModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, SFMConfig.SERVER_SPEC);
        context.registerConfig(ModConfig.Type.CLIENT, SFMConfig.CLIENT_SPEC);
    }

    @Mod.EventBusSubscriber(modid = SFM.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class Server {
        public final ForgeConfigSpec.BooleanValue disableProgramExecution;
        public final ForgeConfigSpec.IntValue timerTriggerMinimumIntervalInTicks;
        public final ForgeConfigSpec.IntValue timerTriggerMinimumIntervalInTicksWhenOnlyForgeEnergyIO;
        public final ForgeConfigSpec.IntValue maxIfStatementsInTriggerBeforeSimulationIsntAllowed;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> disallowedResourceTypesForTransfer;
        /**
         * This is used by managers to detect when the config has changed.
         * When the manager cached var differs from this, the manager will rebuild its program.
         */
        private int revision = 0;

        Server(ForgeConfigSpec.Builder builder) {
            builder.comment("This config is shown to clients, don't put anything secret in here");
            disableProgramExecution = builder
                    .comment("Prevents factory managers from compiling and running code (for emergencies)")
                    .define("disableProgramExecution", false);

            timerTriggerMinimumIntervalInTicks = builder
                    .defineInRange("timerTriggerMinimumIntervalInTicks", 20, 1, Integer.MAX_VALUE);

            timerTriggerMinimumIntervalInTicksWhenOnlyForgeEnergyIO = builder
                    .defineInRange(
                            "timerTriggerMinimumIntervalInTicksWhenOnlyForgeEnergyIOStatementsPresent",
                            1,
                            1,
                            Integer.MAX_VALUE
                    );

            maxIfStatementsInTriggerBeforeSimulationIsntAllowed = builder
                    .comment(
                            "The number of scenarios to check is 2^n where n is the number of if statements in a trigger")
                    .defineInRange("maxIfStatementsInTriggerBeforeSimulationIsntAllowed", 10, 0, Integer.MAX_VALUE);

            disallowedResourceTypesForTransfer = builder
                    .comment("What resource types should SFM not be allowed to move")
                    .defineListAllowEmpty(
                            List.of("disallowedResourceTypesForTransfer"),
                            List::of,
                            String.class::isInstance
                    );
        }

        public int getRevision() {
            return revision;
        }

        @SubscribeEvent
        public static void onConfigChanged(ModConfigEvent.Reloading event) {
            if (event.getConfig().getSpec() == SERVER_SPEC) {
                SFMConfig.SERVER.revision++;
                SFM.LOGGER.info("SFM config reloaded, now on revision {}", SFMConfig.SERVER.revision);
            }
        }
    }

    public static class Client {
        public final ForgeConfigSpec.BooleanValue showLineNumbers;

        Client(ForgeConfigSpec.Builder builder) {
            showLineNumbers = builder.define("showLineNumbers", false);
        }
    }
}
