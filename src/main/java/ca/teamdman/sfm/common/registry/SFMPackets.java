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

    public static void register() {
        registerServerboundPacket(new ServerboundManagerProgramPacket.Daddy());
        registerServerboundPacket(new ServerboundManagerResetPacket.Daddy());
        registerServerboundPacket(new ServerboundManagerFixPacket.Daddy());
        registerClientboundPacket(new ClientboundManagerGuiUpdatePacket.Daddy());
        registerServerboundPacket(new ServerboundManagerSetLogLevelPacket.Daddy());
        registerServerboundPacket(new ServerboundManagerClearLogsPacket.Daddy());
        registerServerboundPacket(new ServerboundManagerLogDesireUpdatePacket.Daddy());
        registerClientboundPacket(new ClientboundManagerLogsPacket.Daddy());
        registerServerboundPacket(new ServerboundManagerRebuildPacket.Daddy());
        registerClientboundPacket(new ClientboundManagerLogLevelUpdatedPacket.Daddy());
        registerServerboundPacket(new ServerboundLabelGunUpdatePacket.Daddy());
        registerServerboundPacket(new ServerboundLabelGunPrunePacket.Daddy());
        registerServerboundPacket(new ServerboundLabelGunClearPacket.Daddy());
        registerServerboundPacket(new ServerboundLabelGunUsePacket.Daddy());
        registerServerboundPacket(new ServerboundLabelGunToggleLabelViewPacket.Daddy());
        registerServerboundPacket(new ServerboundDiskItemSetProgramPacket.Daddy());
        registerServerboundPacket(new ServerboundContainerExportsInspectionRequestPacket.Daddy());
        registerClientboundPacket(new ClientboundContainerExportsInspectionResultsPacket.Daddy());
        registerServerboundPacket(new ServerboundLabelInspectionRequestPacket.Daddy());
        registerClientboundPacket(new ClientboundLabelInspectionResultsPacket.Daddy());
        registerServerboundPacket(new ServerboundInputInspectionRequestPacket.Daddy());
        registerClientboundPacket(new ClientboundInputInspectionResultsPacket.Daddy());
        registerServerboundPacket(new ServerboundOutputInspectionRequestPacket.Daddy());
        registerClientboundPacket(new ClientboundOutputInspectionResultsPacket.Daddy());
        registerServerboundPacket(new ServerboundNetworkToolUsePacket.Daddy());
        registerServerboundPacket(new ServerboundIfStatementInspectionRequestPacket.Daddy());
        registerClientboundPacket(new ClientboundIfStatementInspectionResultsPacket.Daddy());
        registerServerboundPacket(new ServerboundBoolExprStatementInspectionRequestPacket.Daddy());
        registerClientboundPacket(new ClientboundBoolExprStatementInspectionResultsPacket.Daddy());
        registerServerboundPacket(new ServerboundConfigRequestPacket.Daddy());
        registerClientboundPacket(new ClientboundConfigResponsePacket.Daddy());
        registerServerboundPacket(new ServerboundConfigUpdatePacket.Daddy());
        registerServerboundPacket(new ServerboundFacadePacket.Daddy());
        registerClientboundPacket(new ClientboundShowChangelogPacket.Daddy());
        registerServerboundPacket(new ServerboundNetworkToolToggleOverlayPacket.Daddy());
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
