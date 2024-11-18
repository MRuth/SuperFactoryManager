package ca.teamdman.sfm.common.net;

import ca.teamdman.sfm.SFM;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public interface SFMPacketDaddy<T extends SFMPacket> {
    Class<T> getPacketClass();

    void encode(
            T msg,
            FriendlyByteBuf friendlyByteBuf
    );

    T decode(FriendlyByteBuf friendlyByteBuf);

    void handle(
            T msg,
            SFMPacketHandlingContext context
    );

    default void handleOuter(
            T msg,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        SFMPacketHandlingContext context = new SFMPacketHandlingContext(contextSupplier);
        context.enqueueAndFinish(() -> {
            try {
                handle(msg, context);
            } catch (Throwable t) {
                SFM.LOGGER.warn("Encountered exception while handling packet", t);
                throw t;
            }
        });
    }
}
