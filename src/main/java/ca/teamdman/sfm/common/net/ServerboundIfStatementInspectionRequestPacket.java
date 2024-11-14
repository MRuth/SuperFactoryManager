package ca.teamdman.sfm.common.net;

import ca.teamdman.sfm.common.program.ProgramContext;
import ca.teamdman.sfm.common.program.SimulateExploreAllPathsProgramBehaviour;
import ca.teamdman.sfm.common.registry.SFMPackets;
import ca.teamdman.sfm.common.util.SFMUtils;
import ca.teamdman.sfml.ast.IfStatement;
import ca.teamdman.sfml.ast.Program;
import net.minecraft.network.FriendlyByteBuf;

public record ServerboundIfStatementInspectionRequestPacket(
        String programString,
        int inputNodeIndex
) implements SFMPacket {
    public static class Daddy implements SFMPacketDaddy<ServerboundIfStatementInspectionRequestPacket> {
        @Override
        public void encode(
                ServerboundIfStatementInspectionRequestPacket msg,
                FriendlyByteBuf friendlyByteBuf
        ) {
            friendlyByteBuf.writeUtf(msg.programString, Program.MAX_PROGRAM_LENGTH);
            friendlyByteBuf.writeInt(msg.inputNodeIndex());
        }

        @Override
        public ServerboundIfStatementInspectionRequestPacket decode(FriendlyByteBuf friendlyByteBuf) {
            return new ServerboundIfStatementInspectionRequestPacket(
                    friendlyByteBuf.readUtf(Program.MAX_PROGRAM_LENGTH),
                    friendlyByteBuf.readInt()
            );
        }

        @Override
        public void handle(
                ServerboundIfStatementInspectionRequestPacket msg,
                SFMPacketHandlingContext context
        ) {
            context.compileAndThen(
                    msg.programString,
                    (program, player, managerBlockEntity) -> program.builder()
                            .getNodeAtIndex(msg.inputNodeIndex)
                            .filter(IfStatement.class::isInstance)
                            .map(IfStatement.class::cast)
                            .ifPresent(ifStatement -> {
                                StringBuilder payload = new StringBuilder();
                                payload
                                        .append(ifStatement.toStringCondensed())
                                        .append("\n-- peek results --\n");
                                ProgramContext programContext = new ProgramContext(
                                        program,
                                        managerBlockEntity,
                                        new SimulateExploreAllPathsProgramBehaviour()
                                );
                                boolean result = ifStatement.condition().test(programContext);
                                payload.append(result ? "TRUE" : "FALSE");

                                SFMPackets.sendToPlayer(() -> player, new ClientboundIfStatementInspectionResultsPacket(
                                        SFMUtils.truncate(
                                                payload.toString(),
                                                ClientboundIfStatementInspectionResultsPacket.MAX_RESULTS_LENGTH
                                        )));
                            })
            );
        }

        @Override
        public Class<ServerboundIfStatementInspectionRequestPacket> getPacketClass() {
            return ServerboundIfStatementInspectionRequestPacket.class;
        }
    }
}
