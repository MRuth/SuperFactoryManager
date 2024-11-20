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
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public record ServerboundNetworkToolUsePacket(
        BlockPos blockPosition,
        Direction blockFace
) implements SFMPacket {
    public static class Daddy implements SFMPacketDaddy<ServerboundNetworkToolUsePacket> {
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
                    for (var cap : SFMModCompat.getCapabilitiesUnsafe()) {
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


                payload.append("---- exports ----\n");
                int len = payload.length();
                //noinspection unchecked,rawtypes
                SFMResourceTypes.DEFERRED_TYPES
                        .get()
                        .getEntries()
                        .forEach(entry -> payload.append(ServerboundContainerExportsInspectionRequestPacket.buildInspectionResults(
                                (ResourceKey) entry.getKey(),
                                entry.getValue(),
                                level,
                                pos,
                                msg.blockFace
                        )));
                if (payload.length() == len) {
                    payload.append("No exports found");
                }
                payload.append("\n");


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
