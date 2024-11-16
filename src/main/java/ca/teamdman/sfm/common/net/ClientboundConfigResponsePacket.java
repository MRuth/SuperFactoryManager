package ca.teamdman.sfm.common.net;

import ca.teamdman.sfm.client.ClientStuff;
import ca.teamdman.sfm.common.registry.SFMPackets;
import net.minecraft.network.FriendlyByteBuf;

public record ClientboundConfigResponsePacket(
        String configToml,
        ConfigResponseUsage requestingEditMode
) implements SFMPacket {
    public static final int MAX_LENGTH = 20480;

    public enum ConfigResponseUsage {
        SHOW,
        EDIT
    }

    public static class Daddy implements SFMPacketDaddy<ClientboundConfigResponsePacket> {
        @Override
        public void encode(
                ClientboundConfigResponsePacket msg,
                FriendlyByteBuf friendlyByteBuf
        ) {
            friendlyByteBuf.writeUtf(msg.configToml(), MAX_LENGTH);
            friendlyByteBuf.writeEnum(msg.requestingEditMode());
        }

        @Override
        public ClientboundConfigResponsePacket decode(FriendlyByteBuf friendlyByteBuf) {
            return new ClientboundConfigResponsePacket(
                    friendlyByteBuf.readUtf(MAX_LENGTH),
                    friendlyByteBuf.readEnum(ConfigResponseUsage.class)
            );
        }

        @Override
        public void handle(
                ClientboundConfigResponsePacket msg,
                SFMPacketHandlingContext context
        ) {
            String configTomlString = msg.configToml();
            configTomlString = configTomlString.replaceAll("\r", "");
            switch (msg.requestingEditMode()) {
                case SHOW -> ClientStuff.showProgramEditScreen(configTomlString);
                case EDIT -> ClientStuff.showProgramEditScreen(
                        configTomlString,
                        (newContent) -> SFMPackets.sendToServer(new ServerboundConfigUpdatePacket(newContent))
                );
            }
        }

        @Override
        public Class<ClientboundConfigResponsePacket> getPacketClass() {
            return ClientboundConfigResponsePacket.class;
        }
    }
}
