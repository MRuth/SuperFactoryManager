package ca.teamdman.sfm.common.net;

import ca.teamdman.sfm.common.program.ProgramContext;
import ca.teamdman.sfm.common.program.SimulateExploreAllPathsProgramBehaviour;
import ca.teamdman.sfm.common.registry.SFMPackets;
import ca.teamdman.sfm.common.util.SFMUtils;
import ca.teamdman.sfml.ast.BoolExpr;
import ca.teamdman.sfml.ast.Program;
import net.minecraft.network.FriendlyByteBuf;

public record ServerboundBoolExprStatementInspectionRequestPacket(
        String programString,
        int inputNodeIndex
) implements SFMPacket {
    public static class Daddy implements SFMPacketDaddy<ServerboundBoolExprStatementInspectionRequestPacket> {
        @Override
        public void encode(
                ServerboundBoolExprStatementInspectionRequestPacket msg,
                FriendlyByteBuf friendlyByteBuf
        ) {
            friendlyByteBuf.writeUtf(msg.programString, Program.MAX_PROGRAM_LENGTH);
            friendlyByteBuf.writeInt(msg.inputNodeIndex());
        }

        @Override
        public ServerboundBoolExprStatementInspectionRequestPacket decode(FriendlyByteBuf friendlyByteBuf) {
            return new ServerboundBoolExprStatementInspectionRequestPacket(
                    friendlyByteBuf.readUtf(Program.MAX_PROGRAM_LENGTH),
                    friendlyByteBuf.readInt()
            );
        }

        @Override
        public void handle(
                ServerboundBoolExprStatementInspectionRequestPacket msg,
                SFMPacketHandlingContext context
        ) {
            context.compileAndThen(
                    msg.programString,
                    (program, player, managerBlockEntity) ->
                            program.builder()
                                    .getNodeAtIndex(msg.inputNodeIndex)
                                    .filter(BoolExpr.class::isInstance)
                                    .map(BoolExpr.class::cast)
                                    .ifPresent(expr -> {
                                        StringBuilder payload = new StringBuilder();
                                        payload
                                                .append(expr.toStringPretty())
                                                .append("\n-- peek results --\n");
                                        ProgramContext programContext = new ProgramContext(
                                                program,
                                                managerBlockEntity,
                                                new SimulateExploreAllPathsProgramBehaviour()
                                        );
                                        boolean result = expr.test(programContext);
                                        payload.append(result ? "TRUE" : "FALSE");

                                        SFMPackets.sendToPlayer(
                                                () -> player,
                                                new ClientboundBoolExprStatementInspectionResultsPacket(
                                                        SFMUtils.truncate(
                                                                payload.toString(),
                                                                ClientboundBoolExprStatementInspectionResultsPacket.MAX_RESULTS_LENGTH
                                                        ))
                                        );
                                    })
            );
        }

        @Override
        public Class<ServerboundBoolExprStatementInspectionRequestPacket> getPacketClass() {
            return ServerboundBoolExprStatementInspectionRequestPacket.class;
        }
    }
}
