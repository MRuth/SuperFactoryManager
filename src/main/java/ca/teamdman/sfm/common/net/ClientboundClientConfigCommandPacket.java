package ca.teamdman.sfm.common.net;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.client.ClientScreenHelpers;
import ca.teamdman.sfm.common.command.ConfigCommandBehaviour;
import ca.teamdman.sfm.common.config.SFMConfig;
import ca.teamdman.sfm.common.config.SFMConfigReadWriter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;

public record ClientboundClientConfigCommandPacket(
        ConfigCommandBehaviour requestingEditMode
) implements SFMPacket {
    public static class Daddy implements SFMPacketDaddy<ClientboundClientConfigCommandPacket> {
        @Override
        public PacketDirection getPacketDirection() {
            return PacketDirection.CLIENTBOUND;
        }

        @Override
        public void encode(
                ClientboundClientConfigCommandPacket msg,
                FriendlyByteBuf friendlyByteBuf
        ) {
            friendlyByteBuf.writeEnum(msg.requestingEditMode());
        }

        @Override
        public ClientboundClientConfigCommandPacket decode(FriendlyByteBuf friendlyByteBuf) {
            return new ClientboundClientConfigCommandPacket(
                    friendlyByteBuf.readEnum(ConfigCommandBehaviour.class)
            );
        }

        @Override
        public void handle(
                ClientboundClientConfigCommandPacket msg,
                SFMPacketHandlingContext context
        ) {
            String configTomlString = SFMConfigReadWriter.getConfigToml(SFMConfig.CLIENT_SPEC);
            if (configTomlString == null) {
                SFM.LOGGER.error("Unable to get client config");
                return;
            }
            configTomlString = configTomlString.replaceAll("\r", "");
            switch (msg.requestingEditMode()) {
                case SHOW -> ClientScreenHelpers.showProgramEditScreen(configTomlString);
                case EDIT -> ClientScreenHelpers.showProgramEditScreen(
                        configTomlString,
                        Daddy::handleNewClientConfig
                );
            }
        }

        @Override
        public Class<ClientboundClientConfigCommandPacket> getPacketClass() {
            return ClientboundClientConfigCommandPacket.class;
        }

        public static void handleNewClientConfig(String newConfigToml) {
            SFMConfigReadWriter.ConfigSyncResult configSyncResult = SFMConfigReadWriter.updateClientConfig(newConfigToml);
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                player.sendSystemMessage(configSyncResult.component());
            }
        }
    }
}
