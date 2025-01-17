package ca.teamdman.sfm.common.net;

import ca.teamdman.sfm.common.cablenetwork.CableNetwork;
import ca.teamdman.sfm.common.cablenetwork.CableNetworkManager;
import ca.teamdman.sfm.common.compat.SFMModCompat;
import ca.teamdman.sfm.common.registry.SFMPackets;
import ca.teamdman.sfm.common.registry.SFMResourceTypes;
import ca.teamdman.sfm.common.util.SFMDirections;
import ca.teamdman.sfml.ast.DirectionQualifier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public record ServerboundNetworkToolUsePacket(
        BlockPos blockPosition,
        Direction blockFace
) implements SFMPacket {
    public static class Daddy implements SFMPacketDaddy<ServerboundNetworkToolUsePacket> {
        @Override
        public PacketDirection getPacketDirection() {
            return PacketDirection.SERVERBOUND;
        }
        @Override
        public void encode(
                ServerboundNetworkToolUsePacket msg,
                FriendlyByteBuf friendlyByteBuf
        ) {
            friendlyByteBuf.writeBlockPos(msg.blockPosition);
            friendlyByteBuf.writeEnum(msg.blockFace);
        }

        @Override
        public ServerboundNetworkToolUsePacket decode(FriendlyByteBuf friendlyByteBuf) {
            return new ServerboundNetworkToolUsePacket(
                    friendlyByteBuf.readBlockPos(),
                    friendlyByteBuf.readEnum(Direction.class)
            );
        }

        @Override
        public void handle(
                ServerboundNetworkToolUsePacket msg,
                SFMPacketHandlingContext context
        ) {
            {
                // we don't know if the player has the program edit screen open from a manager or a disk in hand
                ServerPlayer player = context.sender();
                if (player == null) return;
                Level level = player.getLevel();
                BlockPos pos = msg.blockPosition();
                if (!level.isLoaded(pos)) return;
                StringBuilder payload = new StringBuilder()
                        .append("---- block position ----\n")
                        .append(pos)
                        .append("\n---- block state ----\n");
                BlockState state = level.getBlockState(pos);
                payload.append(state).append("\n");

                List<CableNetwork> foundNetworks = new ArrayList<>();
                for (Direction direction : SFMDirections.DIRECTIONS) {
                    BlockPos cablePosition = pos.relative(direction);
                    CableNetworkManager
                            .getOrRegisterNetworkFromCablePosition(level, cablePosition)
                            .ifPresent(foundNetworks::add);
                }
                payload.append("---- cable networks ----\n");
                if (foundNetworks.isEmpty()) {
                    payload.append("No networks found\n");
                } else {
                    for (CableNetwork network : foundNetworks) {
                        payload.append(network).append("\n");
                    }
                }

                BlockEntity entity = level.getBlockEntity(pos);
                if (entity != null) {
                    if (!FMLEnvironment.production) {
                        payload.append("---- (dev only) block entity ----\n");
                        payload.append(entity).append("\n");
                    }
                    payload.append("---- capability directions ----\n");
                    for (var cap : (Iterable<Capability<?>>) SFMResourceTypes.getCapabilities()::iterator) {
                        String directions = DirectionQualifier.EVERY_DIRECTION
                                .stream()
                                .filter(dir -> entity.getCapability(cap, dir).isPresent())
                                .map(dir -> dir == null ? "NULL DIRECTION" : DirectionQualifier.directionToString(dir))
                                .collect(Collectors.joining(", ", "[", "]"));
                        if (!directions.equals("[]")) {
                            payload
                                    .append(cap.getName())
                                    .append("\n")
                                    .append(directions)
                                    .append("\n");
                        }
                    }
                }

                Direction[] directions = new Direction[SFMDirections.DIRECTIONS.length + 1];
                directions[0] = msg.blockFace;
                directions[1] = null;
                int assignmentIndex = 2;
                for (Direction direction : SFMDirections.DIRECTIONS) {
                    if (direction == msg.blockFace) continue;
                    directions[assignmentIndex++] = direction;
                }

                String[] messages = new String[directions.length];
                messages[0] = String.format("---- exports for selected face: %s ----", msg.blockFace);
                for (int i = 1; i < directions.length; i++) {
                    messages[i] = String.format("---- exports for face: %s ----", directions[i]);
                }
                for (int i = 0; i < directions.length; i++) {
                    int index = i;
                    payload.append(messages[i]).append("\n");
                    MutableBoolean foundExports = new MutableBoolean(false);
                    //noinspection unchecked,rawtypes
                    SFMResourceTypes.DEFERRED_TYPES
                            .get()
                            .getEntries()
                            .stream()
                            .map(entry -> ServerboundContainerExportsInspectionRequestPacket.buildInspectionResults(
                                    (ResourceKey) entry.getKey(),
                                    entry.getValue(),
                                    level,
                                    pos,
                                    directions[index]
                            ))
                            .filter(s -> !s.isBlank())
                            .forEach(results -> {
                                foundExports.setTrue();
                                payload.append(results).append("\n");
                            });
                    if (foundExports.isFalse()) {
                        payload.append("No exports found");
                    }
                    payload.append("\n");
                }

                if (entity != null) {
                    if (player.hasPermissions(2)) {
                        payload.append("---- (op only) nbt data ----\n");
                        payload.append(entity.serializeNBT()).append("\n");
                    }
                }


                SFMPackets.sendToPlayer(() -> player, new ClientboundInputInspectionResultsPacket(
                        SFMPacketDaddy.truncate(
                                payload.toString(),
                                ClientboundInputInspectionResultsPacket.MAX_RESULTS_LENGTH
                        )));
            }
        }

        @Override
        public Class<ServerboundNetworkToolUsePacket> getPacketClass() {
            return ServerboundNetworkToolUsePacket.class;
        }
    }
}
