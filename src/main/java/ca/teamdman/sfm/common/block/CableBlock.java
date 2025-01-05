package ca.teamdman.sfm.common.block;

import ca.teamdman.sfm.client.ClientFacadeWarningHelper;
import ca.teamdman.sfm.client.ClientKeyHelpers;
import ca.teamdman.sfm.client.handler.NetworkToolKeyMappingHandler;
import ca.teamdman.sfm.client.registry.SFMKeyMappings;
import ca.teamdman.sfm.common.cablenetwork.CableNetworkManager;
import ca.teamdman.sfm.common.cablenetwork.ICableBlock;
import ca.teamdman.sfm.common.facade.FacadeSpreadLogic;
import ca.teamdman.sfm.common.facade.FacadeTransparency;
import ca.teamdman.sfm.common.net.ServerboundFacadePacket;
import ca.teamdman.sfm.common.registry.SFMBlocks;
import ca.teamdman.sfm.common.registry.SFMItems;
import ca.teamdman.sfm.common.util.NotStored;
import ca.teamdman.sfm.common.util.Stored;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

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
            @Stored BlockPos pos,
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
            @Stored BlockPos pos,
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
            @Stored BlockPos pPos,
            Player pPlayer,
            InteractionHand pHand,
            BlockHitResult pHit
    ) {
        if (pPlayer.getOffhandItem().getItem() == SFMItems.NETWORK_TOOL_ITEM.get()) {
            if (pLevel.isClientSide() && pHand == InteractionHand.MAIN_HAND) {
                ServerboundFacadePacket msg = new ServerboundFacadePacket(
                        pHit,
                        FacadeSpreadLogic.fromParts(Screen.hasControlDown(), Screen.hasAltDown()),
                        pPlayer.getMainHandItem(),
                        InteractionHand.MAIN_HAND
                );
                if (ClientKeyHelpers.isKeyDown(SFMKeyMappings.TOGGLE_NETWORK_TOOL_OVERLAY_KEY)) {
                    // we don't want to toggle the overlay if we're using alt-click behaviour
                    NetworkToolKeyMappingHandler.setExternalDebounce();
                }
                ClientFacadeWarningHelper.sendFacadePacketFromClientWithConfirmationIfNecessary(msg);
                return InteractionResult.CONSUME;
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public IFacadableBlock getNonFacadeBlock() {
        return SFMBlocks.CABLE_BLOCK.get();
    }

    @Override
    public IFacadableBlock getFacadeBlock() {
        return SFMBlocks.CABLE_FACADE_BLOCK.get();
    }

    @Override
    public BlockState getStateForPlacementByFacadePlan(
            LevelAccessor level,
            @NotStored BlockPos pos,
            @Nullable FacadeTransparency facadeTransparency
    ) {
        return defaultBlockState();
    }
}
