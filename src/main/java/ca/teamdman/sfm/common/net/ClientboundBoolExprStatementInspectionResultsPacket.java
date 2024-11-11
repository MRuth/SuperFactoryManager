package ca.teamdman.sfm.common.net;

import ca.teamdman.sfm.client.ClientStuff;
import net.minecraft.network.FriendlyByteBuf;

public record ClientboundBoolExprStatementInspectionResultsPacket(
        String results
) implements SFMPacket {
    public static final int MAX_RESULTS_LENGTH = 2048;

    public static class Daddy implements SFMPacketDaddy<ClientboundBoolExprStatementInspectionResultsPacket> {
        @Override
        public Class<ClientboundBoolExprStatementInspectionResultsPacket> getPacketClass() {
            return ClientboundBoolExprStatementInspectionResultsPacket.class;
        }

        @Override
        public void encode(
                ClientboundBoolExprStatementInspectionResultsPacket msg,
                FriendlyByteBuf friendlyByteBuf
        ) {
            friendlyByteBuf.writeUtf(msg.results(), MAX_RESULTS_LENGTH);
        }

        @Override
        public ClientboundBoolExprStatementInspectionResultsPacket decode(FriendlyByteBuf friendlyByteBuf) {
            return new ClientboundBoolExprStatementInspectionResultsPacket(
                    friendlyByteBuf.readUtf(MAX_RESULTS_LENGTH)
            );
        }

        @Override
        public void handle(
                ClientboundBoolExprStatementInspectionResultsPacket msg,
                SFMPacketHandlingContext context
        ) {
            ClientStuff.showProgramEditScreen(msg.results);
        }
    }
}
