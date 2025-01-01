package ca.teamdman.sfm.common.net;

import ca.teamdman.sfm.common.item.LabelGunItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public record ServerboundLabelGunToggleLabelViewPacket(
        InteractionHand hand
) implements SFMPacket {
    public static class Daddy implements SFMPacketDaddy<ServerboundLabelGunToggleLabelViewPacket> {
        @Override
        public PacketDirection getPacketDirection() {
            return PacketDirection.SERVERBOUND;
        }
        @Override
        public void encode(
                ServerboundLabelGunToggleLabelViewPacket msg,
                FriendlyByteBuf buf
        ) {
            buf.writeEnum(msg.hand);
        }

        @Override
        public ServerboundLabelGunToggleLabelViewPacket decode(FriendlyByteBuf buf) {
            return new ServerboundLabelGunToggleLabelViewPacket(buf.readEnum(InteractionHand.class));
        }

        @Override
        public void handle(
                ServerboundLabelGunToggleLabelViewPacket msg,
                SFMPacketHandlingContext context
        ) {
            ServerPlayer sender = context.sender();
            if (sender == null) return;
            ItemStack gun = sender.getItemInHand(msg.hand);
            if (gun.getItem() instanceof LabelGunItem) {
                boolean active = LabelGunItem.getOnlyShowActiveLabel(gun);
                LabelGunItem.setOnlyShowActiveLabel(gun, !active);
            }
        }

        @Override
        public Class<ServerboundLabelGunToggleLabelViewPacket> getPacketClass() {
            return ServerboundLabelGunToggleLabelViewPacket.class;
        }
    }
}

