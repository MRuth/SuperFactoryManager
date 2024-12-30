package ca.teamdman.sfm.common.facade;

import ca.teamdman.sfm.common.block.CableFacadeBlock;
import ca.teamdman.sfm.common.block.IFacadableBlock;
import ca.teamdman.sfm.common.block.ManagerBlock;
import ca.teamdman.sfm.common.blockentity.IFacadeBlockEntity;
import ca.teamdman.sfm.common.cablenetwork.CableNetwork;
import ca.teamdman.sfm.common.localization.LocalizationKeys;
import ca.teamdman.sfm.common.net.ServerboundFacadePacket;
import ca.teamdman.sfm.common.util.InPlaceBlockPlaceContext;
import ca.teamdman.sfm.common.util.SFMDirections;
import ca.teamdman.sfm.common.util.SFMStreamUtils;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class FacadePlanner {
    public static @Nullable FacadePlan getFacadePlan(
            Player player,
            Level level,
            ServerboundFacadePacket msg,
            boolean computeWarnings
    ) {
        BlockPos hitPos = msg.hitResult().getBlockPos();
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
        Item paintItem = msg.paintStack().getItem();
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

        Set<BlockPos> positions = switch (msg.spreadLogic()) {
            case SINGLE -> Set.of(hitPos);
            case NETWORK -> CableNetwork.discoverCables(level, hitPos).collect(Collectors.toSet());
            case NETWORK_GLOBAL_SAME_PAINT -> {
                if (level.getBlockEntity(hitPos) instanceof IFacadeBlockEntity<?> startFacadeBlockEntity) {
                    // the start block is a facade

                    Block existingRenderBlock;
                    IFacadeBlockEntity.FacadeData existingFacadeData = startFacadeBlockEntity.getFacadeData();
                    if (existingFacadeData != null) {
                        existingRenderBlock = existingFacadeData.getRenderBlockState().getBlock();
                    } else {
                        existingRenderBlock = null;
                    }

                    yield CableNetwork.discoverCables(level, hitPos)
                            // we only want painted blocks that match
                            .filter(cablePos -> {
                                if (level.getBlockEntity(cablePos) instanceof IFacadeBlockEntity<?> otherFacadeBlockEntity) {
                                    IFacadeBlockEntity.FacadeData otherFacadeData = otherFacadeBlockEntity.getFacadeData();
                                    Block otherRenderBlock = null;
                                    if (otherFacadeData != null) {
                                        otherRenderBlock = otherFacadeData.getRenderBlockState().getBlock();
                                    }
                                    return otherRenderBlock == existingRenderBlock;
                                } else {
                                    return false;
                                }
                            }).collect(Collectors.toSet());
                } else {
                    // the start block is not a facade
                    yield CableNetwork.discoverCables(level, hitPos)
                            // we only want unpainted blocks
                            .filter(cablePos -> !(level.getBlockEntity(cablePos) instanceof IFacadeBlockEntity<?>))
                            .collect(Collectors.toSet());
                }
            }
            case NETWORK_CONTIGUOUS_SAME_PAINT -> {
                Set<BlockPos> cablePositions = CableNetwork
                        .discoverCables(level, hitPos)
                        .collect(Collectors.toSet());

                if (level.getBlockEntity(hitPos) instanceof IFacadeBlockEntity<?> startFacadeBlockEntity) {
                    // the start block is a facade

                    IFacadeBlockEntity.FacadeData existingFacadeData = startFacadeBlockEntity.getFacadeData();
                    Block existingRenderBlock;
                    if (existingFacadeData != null) {
                        existingRenderBlock = existingFacadeData.getRenderBlockState().getBlock();
                    } else {
                        existingRenderBlock = null;
                    }

                    yield SFMStreamUtils.<BlockPos, BlockPos>getRecursiveStream(
                            (current, next, results) -> {
                                results.accept(current);
                                SFMStreamUtils.get3DNeighboursIncludingKittyCorner(current)
                                        .filter(neighbour -> {
                                            if (!cablePositions.contains(neighbour)) {
                                                return false;
                                            }
                                            if (level.getBlockEntity(neighbour) instanceof IFacadeBlockEntity<?> otherCableFacadeBlockEntity) {
                                                IFacadeBlockEntity.FacadeData otherFacadeData = otherCableFacadeBlockEntity.getFacadeData();
                                                Block neighbourPaintBlock = null;
                                                if (otherFacadeData != null) {
                                                    neighbourPaintBlock = otherFacadeData
                                                            .getRenderBlockState()
                                                            .getBlock();
                                                }
                                                return neighbourPaintBlock == existingRenderBlock;
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
                    for (BlockPos spreadPos : positions) {
                        if (level.getBlockEntity(spreadPos) instanceof IFacadeBlockEntity<?> spreadFacadeBlockEntity) {
                            IFacadeBlockEntity.FacadeData spreadFacadeData = spreadFacadeBlockEntity.getFacadeData();
                            clobbering.merge(
                                    spreadFacadeData == null ? null : spreadFacadeData.getRenderBlockState(),
                                    1,
                                    Integer::sum
                            );
                        } else {
                            BlockState spreadBlockState = level.getBlockState(spreadPos);
                            if (spreadBlockState.getBlock() instanceof ManagerBlock) continue;
                            clobbering.merge(
                                    spreadBlockState,
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
                    for (BlockPos spreadPos : positions) {
                        BlockState checkState = level.getBlockState(spreadPos);
                        if (checkState.getBlock() instanceof ManagerBlock) continue;
                        // Skip if this is already the desired state
                        if (level.getBlockEntity(spreadPos) instanceof IFacadeBlockEntity<?> spreadFacadeBlockEntity) {
                            IFacadeBlockEntity.FacadeData spreadFacadeData = spreadFacadeBlockEntity.getFacadeData();
                            if (spreadFacadeData != null && spreadFacadeData
                                    .getRenderBlockState()
                                    .equals(renderBlockState)) {
                                continue;
                            }
                        }
                        // Increment if neighbour already in the new state
                        for (Direction direction : SFMDirections.DIRECTIONS) {
                            BlockPos offset = spreadPos.relative(direction);
                            if (level.getBlockEntity(offset) instanceof IFacadeBlockEntity<?> offsetFacadeBlockEntity) {
                                IFacadeBlockEntity.FacadeData offsetFacadeData = offsetFacadeBlockEntity.getFacadeData();
                                if (offsetFacadeData != null && offsetFacadeData.getRenderBlockState().equals(renderBlockState)) {
                                    susTouches++;
                                    break;
                                }
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
                msg.hitResult().getDirection(),
                facadePlanWarning
        );
    }
}
