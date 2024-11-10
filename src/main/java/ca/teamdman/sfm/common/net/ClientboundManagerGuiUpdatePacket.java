package ca.teamdman.sfm.common.net;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.common.blockentity.ManagerBlockEntity;
import ca.teamdman.sfm.common.containermenu.ManagerContainerMenu;
import ca.teamdman.sfml.ast.Program;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;

public record ClientboundManagerGuiUpdatePacket(
        int windowId,
        String program,
        ManagerBlockEntity.State state,
        long[] tickTimes
) implements SFMPacket {
    public ClientboundManagerGuiUpdatePacket cloneWithWindowId(int windowId) {
        return new ClientboundManagerGuiUpdatePacket(windowId, program(), state(), tickTimes());
    }

    public static class Daddy implements SFMPacketDaddy<ClientboundManagerGuiUpdatePacket> {
        @Override
        public Class<ClientboundManagerGuiUpdatePacket> getPacketClass() {
            return ClientboundManagerGuiUpdatePacket.class;
        }

        @Override
        public void encode(
                ClientboundManagerGuiUpdatePacket msg,
                FriendlyByteBuf friendlyByteBuf
        ) {
            friendlyByteBuf.writeVarInt(msg.windowId());
            friendlyByteBuf.writeUtf(msg.program(), Program.MAX_PROGRAM_LENGTH);
            friendlyByteBuf.writeEnum(msg.state());
            friendlyByteBuf.writeLongArray(msg.tickTimes());
        }

        @Override
        public ClientboundManagerGuiUpdatePacket decode(FriendlyByteBuf friendlyByteBuf) {
            return new ClientboundManagerGuiUpdatePacket(
                    friendlyByteBuf.readVarInt(),
                    friendlyByteBuf.readUtf(Program.MAX_PROGRAM_LENGTH),
                    friendlyByteBuf.readEnum(ManagerBlockEntity.State.class),
                    friendlyByteBuf.readLongArray()
            );
        }

        @Override
        public void handle(
                ClientboundManagerGuiUpdatePacket msg,
                SFMPacketHandlingContext context
        ) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null
                || !(player.containerMenu instanceof ManagerContainerMenu menu)
                || menu.containerId != msg.windowId()) {
                SFM.LOGGER.error("Invalid logs packet received, ignoring.");
                return;
            }
            menu.tickTimeNanos = msg.tickTimes();
            menu.state = msg.state();
            menu.program = msg.program();
        }

    }
}
