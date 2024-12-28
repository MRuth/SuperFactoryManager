package ca.teamdman.sfm.common.net;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.common.block.CableFacadeBlock;
import ca.teamdman.sfm.common.block.IFacadableBlock;
import ca.teamdman.sfm.common.block.ManagerBlock;
import ca.teamdman.sfm.common.blockentity.CableFacadeBlockEntity;
import ca.teamdman.sfm.common.blockentity.FacadeBlockEntity;
import ca.teamdman.sfm.common.cablenetwork.CableNetwork;
import ca.teamdman.sfm.common.localization.LocalizationKeys;
import ca.teamdman.sfm.common.util.FacadeType;
import ca.teamdman.sfm.common.util.InPlaceBlockPlaceContext;
import ca.teamdman.sfm.common.util.SFMDirections;
import ca.teamdman.sfm.common.util.SFMStreamUtils;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public record ServerboundFacadePacket(
        BlockHitResult hitResult,
        SpreadLogic spreadLogic,
        ItemStack paintStack,
        InteractionHand paintHand
) implements SFMPacket {
    public static @Nullable ServerboundFacadePacket.FacadePlan getFacadePlan(
            Player player,
            Level level,
            ServerboundFacadePacket msg,
            boolean computeWarnings
    ) {
        BlockPos hitPos = msg.hitResult.getBlockPos();
        if (!level.isLoaded(hitPos)) return null;
        BlockState hitBlockState = level.getBlockState(hitPos);
        Block hitBlock = hitBlockState.getBlock();
        if (!(hitBlock instanceof IFacadableBlock hitFacadable)) return null;

        BlockState worldBlockState;
        BlockState renderBlockState;
        BlockPlaceContext blockPlaceContext = new InPlaceBlockPlaceContext(
                player,
                msg.paintHand(),
                msg.paintStack(),
                msg.hitResult()
        );
        Item paintItem = msg.paintStack.getItem();
        if (paintItem == Items.AIR) {
            // Clear facade
            worldBlockState = hitFacadable.getNonFacadeBlock().getStateForPlacement(blockPlaceContext);
            assert worldBlockState != null;
            renderBlockState = null;
        } else {
            @Nullable Block renderBlock = Block.byItem(paintItem);
            if (renderBlock == Blocks.AIR) return null;
            if (renderBlock instanceof IFacadableBlock guh && guh.getNonFacadeBlock() == renderBlock) {
                // Set to a cable block
                worldBlockState = guh.getNonFacadeBlock().getStateForPlacement(blockPlaceContext);
                assert worldBlockState != null;
                renderBlockState = null;
            } else {
                // Set to a facade
                renderBlockState = Objects.requireNonNullElse(
                        renderBlock.getStateForPlacement(blockPlaceContext),
                        renderBlock.defaultBlockState()
                );
                worldBlockState = hitFacadable.getFacadeBlock().getStateForPlacement(blockPlaceContext);
                assert worldBlockState != null;
                FacadeType facadeType = renderBlockState.isSolidRender(level, hitPos)
                                        ? FacadeType.OPAQUE
                                        : FacadeType.TRANSLUCENT;
                worldBlockState.setValue(CableFacadeBlock.FACADE_TYPE_PROP, facadeType);
            }
        }

        Set<BlockPos> positions = switch (msg.spreadLogic) {
            case SINGLE -> Set.of(hitPos);
            case NETWORK -> CableNetwork.discoverCables(level, hitPos).collect(Collectors.toSet());
            case NETWORK_GLOBAL_SAME_PAINT -> {
                if (level.getBlockEntity(hitPos) instanceof FacadeBlockEntity startFacadeBlockEntity) {
                    // the start block is a facade
                    Block existingPaintBlock = startFacadeBlockEntity.getFacadeState().getBlock();
                    yield CableNetwork.discoverCables(level, hitPos)
                            // we only want painted blocks that match
                            .filter(cablePos -> {
                                if (level.getBlockEntity(cablePos) instanceof FacadeBlockEntity otherFacadeBlockEntity) {
                                    return otherFacadeBlockEntity.getFacadeState().getBlock() == existingPaintBlock;
                                } else {
                                    return false;
                                }
                            }).collect(Collectors.toSet());
                } else {
                    // the start block is not a facade
                    yield CableNetwork.discoverCables(level, hitPos)
                            // we only want unpainted blocks
                            .filter(cablePos -> !(level.getBlockEntity(cablePos) instanceof FacadeBlockEntity))
                            .collect(Collectors.toSet());
                }
            }
            case NETWORK_CONTIGUOUS_SAME_PAINT -> {
                Set<BlockPos> cablePositions = CableNetwork
                        .discoverCables(level, hitPos)
                        .collect(Collectors.toSet());

                if (level.getBlockEntity(hitPos) instanceof FacadeBlockEntity startFacadeBlockEntity) {
                    // the start block is a facade
                    Block existingPaintBlock = startFacadeBlockEntity.getFacadeState().getBlock();
                    yield SFMStreamUtils.<BlockPos, BlockPos>getRecursiveStream(
                            (current, next, results) -> {
                                results.accept(current);
                                SFMStreamUtils.get3DNeighboursIncludingKittyCorner(current)
                                        .filter(neighbour -> {
                                            if (!cablePositions.contains(neighbour)) {
                                                return false;
                                            }
                                            if (level.getBlockEntity(neighbour) instanceof FacadeBlockEntity otherCableFacadeBlockEntity) {
                                                Block neighbourPaintBlock = otherCableFacadeBlockEntity
                                                        .getFacadeState()
                                                        .getBlock();
                                                return neighbourPaintBlock == existingPaintBlock;
                                            } else {
                                                return false;
                                            }
                                        })
                                        .forEach(next);
                            },
                            hitPos
                    ).collect(Collectors.toSet());
                } else {
                    // the start block is not a facade
                    yield SFMStreamUtils.<BlockPos, BlockPos>getRecursiveStream(
                            (current, next, results) -> {
                                results.accept(current);
                                SFMStreamUtils.get3DNeighboursIncludingKittyCorner(current)
                                        .filter(neighbour -> {
                                            if (!cablePositions.contains(neighbour)) {
                                                return false;
                                            }
                                            Block neighbourBlock = level.getBlockState(neighbour).getBlock();
                                            // we assume that non-facade blocks are distinct from facade ones here
                                            return neighbourBlock == hitBlock;
                                        })
                                        .forEach(next);
                            },
                            hitPos
                    ).collect(Collectors.toSet());
                }
            }
        };

        FacadePlanWarning facadePlanWarning = null;
        if (computeWarnings) {
            switch (msg.spreadLogic()) {
                case NETWORK -> {
                    // Confirm if:
                    // - There exists two cable blocks with different facade status
                    // Do not confirm if:
                    // - All the cable blocks are the same facade status
                    Object2IntOpenHashMap<BlockState> clobbering = new Object2IntOpenHashMap<>();
                    for (BlockPos blockPos : positions) {
                        if (level.getBlockEntity(blockPos) instanceof CableFacadeBlockEntity facadeBlockEntity) {
                            clobbering.merge(
                                    facadeBlockEntity.getFacadeState(),
                                    1,
                                    Integer::sum
                            );
                        } else {
                            BlockState blockState = level.getBlockState(blockPos);
                            if (blockState.getBlock() instanceof ManagerBlock) continue;
                            clobbering.merge(
                                    blockState,
                                    1,
                                    Integer::sum
                            );
                        }
                    }
                    int clobberingUniqueStateCount = clobbering.keySet().size();
                    if (clobberingUniqueStateCount > 1) {
                        facadePlanWarning = new FacadePlanWarning(
                                LocalizationKeys.FACADE_APPLY_NETWORK_CONFIRM_SCREEN_TITLE.getComponent(),
                                LocalizationKeys.FACADE_APPLY_NETWORK_CONFIRM_SCREEN_MESSAGE.getComponent(
                                        clobberingUniqueStateCount,
                                        clobbering.values().intStream().sum()
                                ),
                                LocalizationKeys.FACADE_APPLY_NETWORK_CONFIRM_SCREEN_YES_BUTTON.getComponent(),
                                LocalizationKeys.FACADE_APPLY_NETWORK_CONFIRM_SCREEN_NO_BUTTON.getComponent()
                        );
                    }
                }
                case NETWORK_GLOBAL_SAME_PAINT, NETWORK_CONTIGUOUS_SAME_PAINT -> {
                    // Confirm if the placement of this new facade will touch existing facades of the new type
                    // So like AAABBBAAA -> AAAAAAAAA should warn
                    // but     AAABBBAAA -> AAACCCAAA should not warn
                    // Get paint block state
                    int susTouches = 0;
                    for (BlockPos checkPos : positions) {
                        BlockState checkState = level.getBlockState(checkPos);
                        if (checkState.getBlock() instanceof ManagerBlock) continue;
                        // Skip if this is already the desired state
                        if (level.getBlockEntity(checkPos) instanceof FacadeBlockEntity facadeBlockEntity
                            && facadeBlockEntity.getFacadeState().equals(renderBlockState)) {
                            continue;
                        }
                        // Increment if neighbour already in the new state
                        for (Direction direction : SFMDirections.DIRECTIONS) {
                            BlockPos offset = checkPos.relative(direction);
                            if (level.getBlockEntity(offset) instanceof FacadeBlockEntity facadeBlockEntity
                                && facadeBlockEntity.getFacadeState().equals(renderBlockState)) {
                                susTouches++;
                                break;
                            }
                        }
                    }
                    if (susTouches > 0) {
                        facadePlanWarning = new FacadePlanWarning(
                                LocalizationKeys.FACADE_APPLY_SUS_NEIGHBOURS_CONFIRM_SCREEN_TITLE.getComponent(),
                                LocalizationKeys.FACADE_APPLY_SUS_NEIGHBOURS_CONFIRM_SCREEN_MESSAGE.getComponent(
                                        susTouches,
                                        susTouches
                                ),
                                LocalizationKeys.FACADE_APPLY_SUS_NEIGHBOURS_CONFIRM_SCREEN_YES_BUTTON.getComponent(),
                                LocalizationKeys.FACADE_APPLY_SUS_NEIGHBOURS_CONFIRM_SCREEN_NO_BUTTON.getComponent()
                        );
                    }
                }
            }
        }

        return new FacadePlan(
                worldBlockState,
                renderBlockState,
                positions,
                facadePlanWarning
        );
    }

    public static void handle(
            ServerboundFacadePacket msg,
            Player sender
    ) {
        Level level = sender.level;
        FacadePlan facadePlan = getFacadePlan(sender, level, msg, false);
        // todo: convert below to method that takes the paint plan to avoid recomputing it when doing eager apply
        if (facadePlan == null) {
            return;
        }
        Consumer<BlockPos> painter;
        if (facadePlan.renderBlockState == null) {
            // we are clearing the facade
            painter = pos -> level.setBlock(
                    pos,
                    facadePlan.worldBlockState,
                    Block.UPDATE_IMMEDIATE | Block.UPDATE_CLIENTS
            );
        } else {
            // we are setting a facade
            painter = pos -> {
                level.setBlock(pos, facadePlan.worldBlockState, Block.UPDATE_IMMEDIATE | Block.UPDATE_CLIENTS);
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof FacadeBlockEntity facadeBlockEntity) {
                    facadeBlockEntity.setFacadeState(facadePlan.renderBlockState);
                } else {
                    SFM.LOGGER.warn("Block entity {} at {} is not a facade block entity", pos, blockEntity);
                }
            };
        }
        facadePlan.positions.forEach(painter);
    }

    public enum SpreadLogic {
        SINGLE,
        NETWORK,
        NETWORK_GLOBAL_SAME_PAINT,
        NETWORK_CONTIGUOUS_SAME_PAINT;

        public static SpreadLogic fromParts(
                boolean isCtrlKeyDown,
                boolean isAltKeyDown
        ) {
            if (isCtrlKeyDown && isAltKeyDown) {
                return NETWORK;
            }
            if (isAltKeyDown) {
                return NETWORK_GLOBAL_SAME_PAINT;
            }
            if (isCtrlKeyDown) {
                return NETWORK_CONTIGUOUS_SAME_PAINT;
            }
            return SINGLE;
        }
    }

    public record FacadePlan(
            BlockState worldBlockState,
            @Nullable BlockState renderBlockState,
            Set<BlockPos> positions,
            @Nullable FacadePlanWarning warning
    ) {
    }

    public record FacadePlanWarning(
            MutableComponent confirmTitle,
            MutableComponent confirmMessage,
            MutableComponent confirmYes,
            MutableComponent confirmNo
    ) {
    }

    public static class Daddy implements SFMPacketDaddy<ServerboundFacadePacket> {
        @Override
        public void encode(
                ServerboundFacadePacket msg,
                FriendlyByteBuf buf
        ) {
            buf.writeBlockHitResult(msg.hitResult);
            buf.writeEnum(msg.spreadLogic);
            buf.writeItem(msg.paintStack);
            buf.writeEnum(msg.paintHand);
        }

        @Override
        public ServerboundFacadePacket decode(FriendlyByteBuf buf) {
            return new ServerboundFacadePacket(
                    buf.readBlockHitResult(),
                    buf.readEnum(SpreadLogic.class),
                    buf.readItem(),
                    buf.readEnum(InteractionHand.class)
            );
        }

        @Override
        public void handle(
                ServerboundFacadePacket msg,
                SFMPacketHandlingContext context
        ) {
            Player sender = context.sender();
            if (sender == null) return;
            ServerboundFacadePacket.handle(msg, sender);
        }

        @Override
        public Class<ServerboundFacadePacket> getPacketClass() {
            return ServerboundFacadePacket.class;
        }
    }
}
