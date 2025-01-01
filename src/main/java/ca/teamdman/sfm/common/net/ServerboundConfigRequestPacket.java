package ca.teamdman.sfm.common.net;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.common.command.ConfigCommandBehaviour;
import ca.teamdman.sfm.common.config.SFMConfig;
import ca.teamdman.sfm.common.config.SFMConfigReadWriter;
import ca.teamdman.sfm.common.registry.SFMPackets;
import net.minecraft.commands.Commands;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public record ServerboundConfigRequestPacket(
        ConfigCommandBehaviour requestingEditMode
) implements SFMPacket {
    public static class Daddy implements SFMPacketDaddy<ServerboundConfigRequestPacket> {
        @Override
        public PacketDirection getPacketDirection() {
            return PacketDirection.SERVERBOUND;
        }

        @Override
        public void encode(
                ServerboundConfigRequestPacket msg,
                FriendlyByteBuf friendlyByteBuf
        ) {
            friendlyByteBuf.writeEnum(msg.requestingEditMode());
        }

        @Override
        public ServerboundConfigRequestPacket decode(FriendlyByteBuf friendlyByteBuf) {
            return new ServerboundConfigRequestPacket(friendlyByteBuf.readEnum(ConfigCommandBehaviour.class));
        }

        @Override
        public void handle(
                ServerboundConfigRequestPacket msg,
                SFMPacketHandlingContext context
        ) {
            ServerPlayer player = context.sender();
            if (player == null) {
                SFM.LOGGER.error("Received {} from null player", this.getPacketClass().getName());
                return;
            }
            if (!player.hasPermissions(Commands.LEVEL_OWNERS)
                && msg.requestingEditMode() == ConfigCommandBehaviour.EDIT) {
                SFM.LOGGER.warn(
                        "Player {} tried to request server config for editing but does not have the necessary permissions, this should never happen o-o",
                        player.getName().getString()
                );
                return;
            }
            String configToml = SFMConfigReadWriter.getConfigToml(SFMConfig.SERVER_SPEC);
            if (configToml == null) {
                SFM.LOGGER.warn("Unable to get server config for player {}", player.getName().getString());
                player.sendSystemMessage(SFMConfigReadWriter.ConfigSyncResult.INTERNAL_FAILURE.component());
                return;
            }
            configToml = configToml.replaceAll("(?m)^#", "--");
            configToml = configToml.replaceAll("\r", "");
            SFM.LOGGER.info("Sending config to player: {}", player.getName().getString());
            SFMPackets.sendToPlayer(
                    () -> player,
                    new ClientboundServerConfigCommandPacket(configToml, msg.requestingEditMode())
            );
        }

        @Override
        public Class<ServerboundConfigRequestPacket> getPacketClass() {
            return ServerboundConfigRequestPacket.class;
        }
    }
}
