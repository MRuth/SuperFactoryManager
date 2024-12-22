package ca.teamdman.sfm.client;

import ca.teamdman.sfm.common.net.ServerboundFacadePacket;
import ca.teamdman.sfm.common.registry.SFMPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class ClientFacadeHelpers {
    public static void sendFacadePacketFromClientWithConfirmationIfNecessary(ServerboundFacadePacket msg) {
        // Given the incentives for a single cable network to be used,
        // we want to protect users from accidentally clobbering their designs in a single action
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        assert player != null;
        Level level = player.level;

        ServerboundFacadePacket.FacadePlan facadePlan = ServerboundFacadePacket.getFacadePlan(
                player,
                level,
                msg,
                true
        );
        if (facadePlan == null) return;
        ServerboundFacadePacket.FacadePlanWarning warning = facadePlan.warning();
        if (warning == null) {
            // No confirmation necessary for single updates
            SFMPackets.sendToServer(msg);
            // Perform eager update
            ServerboundFacadePacket.handle(msg, player);
        } else {
            ConfirmScreen confirmScreen = new ConfirmScreen(
                    (confirmed) -> {
                        minecraft.popGuiLayer(); // Close confirm screen
                        if (confirmed) {
                            // Send packet
                            SFMPackets.sendToServer(msg);
                            // Perform eager update
                            ServerboundFacadePacket.handle(msg, player);
                        }
                    },
                    warning.confirmTitle(),
                    warning.confirmMessage(),
                    warning.confirmYes(),
                    warning.confirmNo()
            );
            ClientScreenHelpers.setOrPushScreen(confirmScreen);
            confirmScreen.setDelay(10);
        }
    }
}
