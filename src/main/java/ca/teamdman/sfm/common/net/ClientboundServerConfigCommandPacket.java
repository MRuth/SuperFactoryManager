package ca.teamdman.sfm.common.net;

import ca.teamdman.sfm.client.ClientScreenHelpers;
import ca.teamdman.sfm.common.command.ConfigCommandBehaviourInput;
import ca.teamdman.sfm.common.registry.SFMPackets;
import net.minecraft.network.FriendlyByteBuf;

public record ClientboundServerConfigCommandPacket(
        String configToml,
        ConfigCommandBehaviourInput requestingEditMode
) implements SFMPacket {
    public static final int MAX_LENGTH = 20480;

    public static class Daddy implements SFMPacketDaddy<ClientboundServerConfigCommandPacket> {
        @Override
        public PacketDirection getPacketDirection() {
            return PacketDirection.CLIENTBOUND;
        }
        @Override
        public void encode(
                ClientboundServerConfigCommandPacket msg,
                FriendlyByteBuf friendlyByteBuf
        ) {
            friendlyByteBuf.writeUtf(msg.configToml(), MAX_LENGTH);
            friendlyByteBuf.writeEnum(msg.requestingEditMode());
        }

        @Override
        public ClientboundServerConfigCommandPacket decode(FriendlyByteBuf friendlyByteBuf) {
            return new ClientboundServerConfigCommandPacket(
                    friendlyByteBuf.readUtf(MAX_LENGTH),
                    friendlyByteBuf.readEnum(ConfigCommandBehaviourInput.class)
            );
        }

        @Override
        public void handle(
                ClientboundServerConfigCommandPacket msg,
                SFMPacketHandlingContext context
        ) {
            String configTomlString = msg.configToml();
            configTomlString = configTomlString.replaceAll("\r", "");
            switch (msg.requestingEditMode()) {
                case SHOW -> ClientScreenHelpers.showProgramEditScreen(configTomlString);
                case EDIT -> ClientScreenHelpers.showProgramEditScreen(
                        configTomlString,
                        (newContent) -> SFMPackets.sendToServer(new ServerboundServerConfigUpdatePacket(newContent))
                );
            }
        }

        @Override
        public Class<ClientboundServerConfigCommandPacket> getPacketClass() {
            return ClientboundServerConfigCommandPacket.class;
        }
    }
}
