package ca.teamdman.sfm.common.net;

import ca.teamdman.sfm.client.ClientScreenHelpers;
import net.minecraft.network.FriendlyByteBuf;

public record ClientboundInputInspectionResultsPacket(
        String results
) implements SFMPacket {
    public static final int MAX_RESULTS_LENGTH = 20480;

    public static class Daddy implements SFMPacketDaddy<ClientboundInputInspectionResultsPacket> {
        @Override
        public void encode(
                ClientboundInputInspectionResultsPacket msg,
                FriendlyByteBuf friendlyByteBuf
        ) {
            friendlyByteBuf.writeUtf(msg.results(), MAX_RESULTS_LENGTH);
        }

        @Override
        public ClientboundInputInspectionResultsPacket decode(FriendlyByteBuf friendlyByteBuf) {
            return new ClientboundInputInspectionResultsPacket(
                    friendlyByteBuf.readUtf(MAX_RESULTS_LENGTH)
            );
        }

        @Override
        public void handle(
                ClientboundInputInspectionResultsPacket msg,
                SFMPacketHandlingContext context
        ) {
            ClientScreenHelpers.showProgramEditScreen(msg.results());
        }

        @Override
        public Class<ClientboundInputInspectionResultsPacket> getPacketClass() {
            return ClientboundInputInspectionResultsPacket.class;
        }
    }

}
