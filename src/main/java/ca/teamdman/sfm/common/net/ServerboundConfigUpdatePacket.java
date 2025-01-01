package ca.teamdman.sfm.common.net;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.common.config.SFMConfigReadWriter;
import net.minecraft.commands.Commands;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.HandshakeMessages;

public record ServerboundConfigUpdatePacket(
        String newConfig
) implements SFMPacket {
    /**
     * Value chosen to match {@link HandshakeMessages.S2CConfigData#decode(FriendlyByteBuf)}
     */
    public static final int MAX_CONFIG_LENGTH = 32767;
    public static class Daddy implements SFMPacketDaddy<ServerboundConfigUpdatePacket> {
        @Override
        public PacketDirection getPacketDirection() {
            return PacketDirection.SERVERBOUND;
        }
        @Override
        public void encode(
                ServerboundConfigUpdatePacket msg,
                FriendlyByteBuf friendlyByteBuf
        ) {
            friendlyByteBuf.writeUtf(msg.newConfig, MAX_CONFIG_LENGTH);
        }

        @Override
        public ServerboundConfigUpdatePacket decode(FriendlyByteBuf friendlyByteBuf) {
            return new ServerboundConfigUpdatePacket(friendlyByteBuf.readUtf(MAX_CONFIG_LENGTH));
        }

        @Override
        public void handle(
                ServerboundConfigUpdatePacket msg,
                SFMPacketHandlingContext context
        ) {
            ServerPlayer player = context.sender();
            if (player == null) {
                SFM.LOGGER.error("Received ServerboundConfigRequestPacket from null player");
                return;
            }
            if (!player.hasPermissions(Commands.LEVEL_OWNERS)) {
                SFM.LOGGER.fatal(
                        "Player {} tried to WRITE server config but does not have the necessary permissions, this should never happen o-o",
                        player.getName().getString()
                );
                return;
            }
            SFMConfigReadWriter.ConfigSyncResult result = SFMConfigReadWriter.updateAndSyncServerConfig(msg.newConfig);
            player.sendSystemMessage(result.component());
        }

        @Override
        public Class<ServerboundConfigUpdatePacket> getPacketClass() {
            return ServerboundConfigUpdatePacket.class;
        }
    }
}
