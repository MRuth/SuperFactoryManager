package ca.teamdman.sfm.common.net;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.common.config.ConfigExporter;
import ca.teamdman.sfm.common.config.SFMConfig;
import ca.teamdman.sfm.common.net.ClientboundConfigResponsePacket.ConfigResponseUsage;
import ca.teamdman.sfm.common.registry.SFMPackets;
import net.minecraft.commands.Commands;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public record ServerboundConfigRequestPacket(
        ConfigResponseUsage requestingEditMode
) implements SFMPacket {
    public static class Daddy implements SFMPacketDaddy<ServerboundConfigRequestPacket> {
        @Override
        public void encode(
                ServerboundConfigRequestPacket msg,
                FriendlyByteBuf friendlyByteBuf
        ) {
            friendlyByteBuf.writeEnum(msg.requestingEditMode());
        }

        @Override
        public ServerboundConfigRequestPacket decode(FriendlyByteBuf friendlyByteBuf) {
            return new ServerboundConfigRequestPacket(friendlyByteBuf.readEnum(ConfigResponseUsage.class));
        }

        @Override
        public void handle(
                ServerboundConfigRequestPacket msg,
                SFMPacketHandlingContext context
        ) {
            ServerPlayer player = context.sender();
            if (player == null) {
                SFM.LOGGER.error("Received ServerboundConfigRequestPacket from null player");
                return;
            }
            if (!player.hasPermissions(Commands.LEVEL_OWNERS)
                && msg.requestingEditMode() == ConfigResponseUsage.EDIT) {
                SFM.LOGGER.warn(
                        "Player {} tried to request server config for editing but does not have the necessary permissions, this should never happen o-o",
                        player.getName().getString()
                );
                return;
            }
            String configToml = ConfigExporter.getConfigToml(SFMConfig.SERVER_SPEC);
            configToml = configToml.replaceAll("(?m)^#", "--");
            configToml = configToml.replaceAll("\r", "");
            SFM.LOGGER.info("Sending config to player: {}", player.getName().getString());
            SFMPackets.sendToPlayer(
                    () -> player,
                    new ClientboundConfigResponsePacket(configToml, msg.requestingEditMode())
            );
        }

        @Override
        public Class<ServerboundConfigRequestPacket> getPacketClass() {
            return ServerboundConfigRequestPacket.class;
        }
    }
}
