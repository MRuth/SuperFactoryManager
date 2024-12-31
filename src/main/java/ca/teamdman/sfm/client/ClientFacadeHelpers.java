package ca.teamdman.sfm.client;

import ca.teamdman.sfm.common.facade.FacadePlanWarning;
import ca.teamdman.sfm.common.facade.FacadePlanner;
import ca.teamdman.sfm.common.facade.IFacadePlan;
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

        IFacadePlan facadePlan = FacadePlanner.getFacadePlan(
                player,
                level,
                msg,
                true
        );
        if (facadePlan == null) return;
        FacadePlanWarning warning = facadePlan.warning();
        if (warning == null) {
            // No confirmation necessary for single updates
            SFMPackets.sendToServer(msg);
            // Perform eager update
            facadePlan.apply(level);
        } else {
            ConfirmScreen confirmScreen = new ConfirmScreen(
                    (confirmed) -> {
                        minecraft.popGuiLayer(); // Close confirm screen
                        if (confirmed) {
                            // Send packet
                            SFMPackets.sendToServer(msg);
                            // Perform eager update
                            facadePlan.apply(level);
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
