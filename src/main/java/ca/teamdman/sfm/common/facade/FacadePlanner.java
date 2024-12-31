package ca.teamdman.sfm.common.facade;

import ca.teamdman.sfm.common.block.IFacadableBlock;
import ca.teamdman.sfm.common.blockentity.IFacadeBlockEntity;
import ca.teamdman.sfm.common.cablenetwork.CableNetwork;
import ca.teamdman.sfm.common.net.ServerboundFacadePacket;
import ca.teamdman.sfm.common.util.InPlaceBlockPlaceContext;
import ca.teamdman.sfm.common.util.SFMStreamUtils;
import net.minecraft.core.BlockPos;
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
            ServerboundFacadePacket msg
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
                    getPositions(level, msg, hitPos, hitBlock)
            );
        }

        @Nullable Block renderBlock = Block.byItem(paintItem);
        if (renderBlock == Blocks.AIR) return null;

        if (renderBlock instanceof IFacadableBlock guh) {
            if (level.getBlockEntity(hitPos) instanceof IFacadeBlockEntity hitFacadeBlockEntity) {
                // Change facade type
                FacadeData hitFacadeData = hitFacadeBlockEntity.getFacadeData();
                if (hitFacadeData != null
                    && hitBlockState.hasProperty(FacadeTransparency.FACADE_TRANSPARENCY_PROPERTY)) {
                    return new ChangeWorldBlockFacadePlan(
                            guh.getFacadeBlock(),
                            getPositions(level, msg, hitPos, hitBlock)
                    );
                }
            }
            return new ClearFacadesFacadePlan(
                    guh.getFacadeBlock(),
                    getPositions(level, msg, hitPos, hitBlock)
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
        FacadeTransparency facadeTransparency = renderBlockState.isSolidRender(level, hitPos)
                                                ? FacadeTransparency.OPAQUE
                                                : FacadeTransparency.TRANSLUCENT;
        return new ApplyFacadesFacadePlan(
                hitFacadable.getFacadeBlock(),
                new FacadeData(
                        renderBlockState,
                        msg.hitResult().getDirection(),
                        FacadeTextureMode.FILL
                ),
                facadeTransparency,
                getPositions(level, msg, hitPos, hitBlock)
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
                if (level.getBlockEntity(hitPos) instanceof IFacadeBlockEntity startFacadeBlockEntity) {
                    // the start block is a facade
                    FacadeData existingFacadeData = startFacadeBlockEntity.getFacadeData();
                    Class<?> existingFacadeBlockEntityClass = startFacadeBlockEntity.getClass();
                    yield CableNetwork.discoverCables(level, hitPos)
                            // we only want matches with the same (world,render) type
                            .filter(cablePos -> {
                                if (
                                        level.getBlockEntity(cablePos) instanceof IFacadeBlockEntity otherFacadeBlockEntity
                                        && otherFacadeBlockEntity.getClass().equals(existingFacadeBlockEntityClass)
                                ) {
                                    return Objects.equals(otherFacadeBlockEntity.getFacadeData(), existingFacadeData);
                                } else {
                                    return false;
                                }
                            }).collect(Collectors.toSet());
                } else {
                    // the start block is not a facade
                    yield CableNetwork.discoverCables(level, hitPos)
                            // must match start block
                            .filter(checkPos -> level.getBlockState(checkPos).getBlock() == hitBlock)
                            // must not have a facade set
                            .filter(checkPos -> !(level.getBlockEntity(checkPos) instanceof IFacadeBlockEntity))
                            .collect(Collectors.toSet());
                }
            }
            case NETWORK_CONTIGUOUS_SAME_PAINT -> {
                Set<BlockPos> cablePositions = CableNetwork
                        .discoverCables(level, hitPos)
                        .collect(Collectors.toSet());

                if (level.getBlockEntity(hitPos) instanceof IFacadeBlockEntity startFacadeBlockEntity) {
                    // the start block is a facade
                    Class<?> existingFacadeBlockEntityClass = startFacadeBlockEntity.getClass();
                    FacadeData existingFacadeData = startFacadeBlockEntity.getFacadeData();
                    yield SFMStreamUtils.<BlockPos, BlockPos>getRecursiveStream(
                            (current, next, results) -> {
                                results.accept(current);
                                SFMStreamUtils.get3DNeighboursIncludingKittyCorner(current)
                                        .filter(neighbour -> {
                                            if (!cablePositions.contains(neighbour)) {
                                                return false;
                                            }
                                            if (
                                                    level.getBlockEntity(neighbour) instanceof IFacadeBlockEntity otherCableFacadeBlockEntity
                                                    && otherCableFacadeBlockEntity.getClass().equals(existingFacadeBlockEntityClass)
                                            ) {
                                                FacadeData otherFacadeData = otherCableFacadeBlockEntity.getFacadeData();
                                                return Objects.equals(otherFacadeData, existingFacadeData);
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
}
