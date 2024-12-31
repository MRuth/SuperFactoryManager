package ca.teamdman.sfm.common.facade;

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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A plan for how to update the blocks in the world and sometimes their facade data.
 * <p>
 * If the render block is a cable and the world block is a cable, and they are different, then the world block should be changed to the render block without affecting facades.
 * If the render block is a cable and the world block is a cable, and they are the same, then the world block should be changed to the non-facade block, removing the facade.
 * <p>
 * Consider: If some full-block facades are set to different render blocks and the user updates the entire network to the fancy cable, then the facades must be preserved in all their uniqueness.
 * <p>
 * A warning should occur when sum(unique(facade data, block state)) > 1.
 */
public class FacadePlanner {
    public static @Nullable IFacadePlan getFacadePlan(
            Player player,
            Level level,
            ServerboundFacadePacket msg,
            boolean computeWarnings
    ) {
        // preconditions
        BlockPos hitPos = msg.hitResult().getBlockPos();
        if (!level.isLoaded(hitPos)) return null;
        BlockState hitBlockState = level.getBlockState(hitPos);
        Block hitBlock = hitBlockState.getBlock();
        if (!(hitBlock instanceof IFacadableBlock hitFacadable)) return null;
        Item paintItem = msg.paintStack().getItem();

        boolean paintingWithAir = paintItem == Items.AIR;
        if (paintingWithAir) {
            return new ClearFacadesFacadePlan(
                    hitFacadable.getNonFacadeBlock(),
                    getPositions(level, msg, hitPos, hitBlock),
                    computeWarnings ? getFacadePlanWarning(level, msg, Set.of(hitPos), null) : null
            );
        }

        @Nullable Block renderBlock = Block.byItem(paintItem);
        if (renderBlock == Blocks.AIR) return null;

        if (renderBlock instanceof IFacadableBlock guh) {
            if (level.getBlockEntity(hitPos) instanceof IFacadeBlockEntity<?> hitFacadeBlockEntity) {
                // Change facade type
                IFacadeBlockEntity.FacadeData hitFacadeData = hitFacadeBlockEntity.getFacadeData();
                if (hitFacadeData != null && hitBlockState.hasProperty(FacadeType.FACADE_TYPE_PROPERTY)) {
                    return new ChangeWorldBlockFacadePlan(
                            guh.getFacadeBlock(),
                            getPositions(level, msg, hitPos, hitBlock),
                            computeWarnings ? getFacadePlanWarning(
                                    level,
                                    msg,
                                    Set.of(hitPos),
                                    hitFacadeData.getRenderBlockState()
                            ) : null
                    );
                }
            }
            return new ClearFacadesFacadePlan(
                    guh.getFacadeBlock(),
                    getPositions(level, msg, hitPos, hitBlock),
                    computeWarnings ? getFacadePlanWarning(level, msg, Set.of(hitPos), null) : null
            );
        }

        // Apply facade
        BlockState renderBlockState = Objects.requireNonNullElse(
                renderBlock.getStateForPlacement(new InPlaceBlockPlaceContext(
                        player,
                        msg.paintHand(),
                        msg.paintStack(),
                        msg.hitResult()
                )),
                renderBlock.defaultBlockState()
        );
        FacadeType facadeType = renderBlockState.isSolidRender(level, hitPos)
                                ? FacadeType.OPAQUE
                                : FacadeType.TRANSLUCENT;
        return new ApplyFacadesFacadePlan(
                hitFacadable.getFacadeBlock(),
                renderBlockState,
                facadeType,
                msg.hitResult().getDirection(),
                getPositions(level, msg, hitPos, hitBlock),
                computeWarnings ? getFacadePlanWarning(level, msg, Set.of(hitPos), renderBlockState) : null
        );
    }

    private static @NotNull Set<BlockPos> getPositions(
            Level level,
            ServerboundFacadePacket msg,
            BlockPos hitPos,
            Block hitBlock
    ) {
        return switch (msg.spreadLogic()) {
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
    }

    private static @Nullable FacadePlanWarning getFacadePlanWarning(
            Level level,
            ServerboundFacadePacket msg,
            Set<BlockPos> positions,
            @Nullable BlockState renderBlockState
    ) {
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
                    return new FacadePlanWarning(
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
                            if (offsetFacadeData != null && offsetFacadeData.getRenderBlockState().equals(
                                    renderBlockState)) {
                                susTouches++;
                                break;
                            }
                        }
                    }
                }
                if (susTouches > 0) {
                    return new FacadePlanWarning(
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
        return null;
    }
}
