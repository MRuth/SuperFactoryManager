package ca.teamdman.sfm.common.net;

import ca.teamdman.sfm.common.facade.FacadePlan;
import ca.teamdman.sfm.common.facade.FacadePlanner;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public record ServerboundFacadePacket(
        BlockHitResult hitResult,
        SpreadLogic spreadLogic,
        ItemStack paintStack,
        InteractionHand paintHand
) implements SFMPacket {
    public static void handle(
            ServerboundFacadePacket msg,
            Player sender
    ) {
        Level level = sender.level;
        FacadePlan facadePlan = FacadePlanner.getFacadePlan(sender, level, msg, false);
        if (facadePlan == null) {
            return;
        }
        facadePlan.apply(level);
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
