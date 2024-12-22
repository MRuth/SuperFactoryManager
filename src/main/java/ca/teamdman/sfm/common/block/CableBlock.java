package ca.teamdman.sfm.common.block;

import ca.teamdman.sfm.client.ClientFacadeHelpers;
import ca.teamdman.sfm.common.cablenetwork.CableNetworkManager;
import ca.teamdman.sfm.common.cablenetwork.ICableBlock;
import ca.teamdman.sfm.common.net.ServerboundFacadePacket;
import ca.teamdman.sfm.common.registry.SFMBlocks;
import ca.teamdman.sfm.common.registry.SFMItems;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;

public class CableBlock extends Block implements ICableBlock, IFacadableBlock {
    public CableBlock() {
        super(Block.Properties
                      .of(Material.METAL)
                      .destroyTime(1f)
                      .sound(SoundType.METAL));
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onPlace(
            BlockState state,
            Level world,
            BlockPos pos,
            BlockState oldState,
            boolean isMoving
    ) {
        // does nothing but keeping for symmetry
        super.onPlace(state, world, pos, oldState, isMoving);

        if (!(oldState.getBlock() instanceof ICableBlock)) {
            CableNetworkManager.onCablePlaced(world, pos);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean isMoving
    ) {
        // purges block entity
        super.onRemove(state, level, pos, newState, isMoving);

        if (!(newState.getBlock() instanceof ICableBlock)) {
            CableNetworkManager.onCableRemoved(level, pos);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public InteractionResult use(
            BlockState pState,
            Level pLevel,
            BlockPos pPos,
            Player pPlayer,
            InteractionHand pHand,
            BlockHitResult pHit
    ) {
        if (pPlayer.getOffhandItem().getItem() == SFMItems.NETWORK_TOOL_ITEM.get()) {
            if (pLevel.isClientSide() && pHand == InteractionHand.MAIN_HAND) {
                ServerboundFacadePacket msg = new ServerboundFacadePacket(
                        pHit,
                        ServerboundFacadePacket.SpreadLogic.fromParts(Screen.hasControlDown(), Screen.hasAltDown()),
                        pPlayer.getMainHandItem(),
                        InteractionHand.MAIN_HAND
                );
                ClientFacadeHelpers.sendFacadePacketFromClientWithConfirmationIfNecessary(msg);
                return InteractionResult.CONSUME;
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public Block getNonFacadeBlock() {
        return SFMBlocks.CABLE_BLOCK.get();
    }

    @Override
    public Block getFacadeBlock() {
        return SFMBlocks.CABLE_FACADE_BLOCK.get();
    }
}
