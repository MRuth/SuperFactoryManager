package ca.teamdman.sfm.common.net;

import ca.teamdman.sfm.client.ClientScreenHelpers;
import net.minecraft.network.FriendlyByteBuf;

public record ClientboundShowChangelogPacket(
) implements SFMPacket {

    public static class Daddy implements SFMPacketDaddy<ClientboundShowChangelogPacket> {
        @Override
        public void encode(
                ClientboundShowChangelogPacket msg,
                FriendlyByteBuf friendlyByteBuf
        ) {
        }

        @Override
        public ClientboundShowChangelogPacket decode(FriendlyByteBuf friendlyByteBuf) {
            return new ClientboundShowChangelogPacket(
            );
        }

        @Override
        public void handle(
                ClientboundShowChangelogPacket msg,
                SFMPacketHandlingContext context
        ) {
            ClientScreenHelpers.showChangelog();
        }

        @Override
        public Class<ClientboundShowChangelogPacket> getPacketClass() {
            return ClientboundShowChangelogPacket.class;
        }
    }
}
