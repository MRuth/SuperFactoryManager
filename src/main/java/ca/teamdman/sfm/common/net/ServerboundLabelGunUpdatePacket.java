package ca.teamdman.sfm.common.net;

import ca.teamdman.sfm.common.item.LabelGunItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;

public record ServerboundLabelGunUpdatePacket(
        String label,
        InteractionHand hand
) implements SFMPacket {
    public static final int MAX_LABEL_LENGTH = 80;

    public static class Daddy implements SFMPacketDaddy<ServerboundLabelGunUpdatePacket> {
        @Override
        public PacketDirection getPacketDirection() {
            return PacketDirection.SERVERBOUND;
        }
        @Override
        public void encode(
                ServerboundLabelGunUpdatePacket msg,
                FriendlyByteBuf buf
        ) {
            buf.writeUtf(msg.label, MAX_LABEL_LENGTH);
            buf.writeEnum(msg.hand);
        }

        @Override
        public ServerboundLabelGunUpdatePacket decode(FriendlyByteBuf buf) {
            return new ServerboundLabelGunUpdatePacket(
                    buf.readUtf(MAX_LABEL_LENGTH),
                    buf.readEnum(InteractionHand.class)
            );
        }

        @Override
        public void handle(
                ServerboundLabelGunUpdatePacket msg,
                SFMPacketHandlingContext context
        ) {
            var sender = context.sender();
            if (sender == null) {
                return;
            }
            var stack = sender.getItemInHand(msg.hand);
            if (stack.getItem() instanceof LabelGunItem) {
                LabelGunItem.setActiveLabel(stack, msg.label);
            }
        }

        @Override
        public Class<ServerboundLabelGunUpdatePacket> getPacketClass() {
            return ServerboundLabelGunUpdatePacket.class;
        }
    }

}
