package ca.teamdman.sfm.common.registry;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.common.net.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public class SFMPackets {
    public static final String SFM_CHANNEL_VERSION="1.0.0";
    public static final SimpleChannel SFM_CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SFM.MOD_ID, "manager"),
            SFM_CHANNEL_VERSION::toString,
            SFM_CHANNEL_VERSION::equals,
            SFM_CHANNEL_VERSION::equals
    );

    private static int registrationIndex = 0;
    public static <T extends SFMPacket> void registerServerboundPacket(
            SFMPacketDaddy<T> packetDaddy
    ) {
        SFM_CHANNEL.registerMessage(
                registrationIndex++,
                packetDaddy.getPacketClass(),
                packetDaddy::encode,
                packetDaddy::decode,
                packetDaddy::handleOuter
        );
    }

    public static <T extends SFMPacket> void registerClientboundPacket(
            SFMPacketDaddy<T> packetDaddy
    ) {
        SFM_CHANNEL.registerMessage(
                registrationIndex++,
                packetDaddy.getPacketClass(),
                packetDaddy::encode,
                packetDaddy::decode,
                packetDaddy::handleOuter
        );
    }

    public static <T extends SFMPacket> void registerPacket(
            SFMPacketDaddy<T> packetDaddy
    ) {
        switch (packetDaddy.getPacketDirection()) {
            case SERVERBOUND -> registerServerboundPacket(packetDaddy);
            case CLIENTBOUND -> registerClientboundPacket(packetDaddy);
        }
    }

    public static void register() {
        registerPacket(new ClientboundBoolExprStatementInspectionResultsPacket.Daddy());
        registerPacket(new ClientboundConfigResponsePacket.Daddy());
        registerPacket(new ClientboundContainerExportsInspectionResultsPacket.Daddy());
        registerPacket(new ClientboundIfStatementInspectionResultsPacket.Daddy());
        registerPacket(new ClientboundInputInspectionResultsPacket.Daddy());
        registerPacket(new ClientboundLabelInspectionResultsPacket.Daddy());
        registerPacket(new ClientboundManagerGuiUpdatePacket.Daddy());
        registerPacket(new ClientboundManagerLogLevelUpdatedPacket.Daddy());
        registerPacket(new ClientboundManagerLogsPacket.Daddy());
        registerPacket(new ClientboundOutputInspectionResultsPacket.Daddy());
        registerPacket(new ClientboundShowChangelogPacket.Daddy());
        registerPacket(new ServerboundBoolExprStatementInspectionRequestPacket.Daddy());
        registerPacket(new ServerboundConfigRequestPacket.Daddy());
        registerPacket(new ServerboundConfigUpdatePacket.Daddy());
        registerPacket(new ServerboundContainerExportsInspectionRequestPacket.Daddy());
        registerPacket(new ServerboundDiskItemSetProgramPacket.Daddy());
        registerPacket(new ServerboundFacadePacket.Daddy());
        registerPacket(new ServerboundIfStatementInspectionRequestPacket.Daddy());
        registerPacket(new ServerboundInputInspectionRequestPacket.Daddy());
        registerPacket(new ServerboundLabelGunClearPacket.Daddy());
        registerPacket(new ServerboundLabelGunPrunePacket.Daddy());
        registerPacket(new ServerboundLabelGunToggleLabelViewPacket.Daddy());
        registerPacket(new ServerboundLabelGunUpdatePacket.Daddy());
        registerPacket(new ServerboundLabelGunUsePacket.Daddy());
        registerPacket(new ServerboundLabelInspectionRequestPacket.Daddy());
        registerPacket(new ServerboundManagerClearLogsPacket.Daddy());
        registerPacket(new ServerboundManagerFixPacket.Daddy());
        registerPacket(new ServerboundManagerLogDesireUpdatePacket.Daddy());
        registerPacket(new ServerboundManagerProgramPacket.Daddy());
        registerPacket(new ServerboundManagerRebuildPacket.Daddy());
        registerPacket(new ServerboundManagerResetPacket.Daddy());
        registerPacket(new ServerboundManagerSetLogLevelPacket.Daddy());
        registerPacket(new ServerboundNetworkToolToggleOverlayPacket.Daddy());
        registerPacket(new ServerboundNetworkToolUsePacket.Daddy());
        registerPacket(new ServerboundOutputInspectionRequestPacket.Daddy());
    }

    public static void sendToServer(
            Object packet
    ) {
        SFM_CHANNEL.sendToServer(packet);
    }

    public static void sendToPlayer(
            Supplier<ServerPlayer> player,
            Object packet
    ) {
        SFM_CHANNEL.send(PacketDistributor.PLAYER.with(player), packet);
    }

    public static void sendToPlayer(
            ServerPlayer player,
            Object packet
    ) {
        SFM_CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
