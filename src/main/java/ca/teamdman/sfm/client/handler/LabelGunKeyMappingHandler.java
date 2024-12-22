package ca.teamdman.sfm.client.handler;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.client.ClientKeyHelpers;
import ca.teamdman.sfm.client.registry.SFMKeyMappings;
import ca.teamdman.sfm.common.item.LabelGunItem;
import ca.teamdman.sfm.common.net.ServerboundLabelGunToggleLabelViewPacket;
import ca.teamdman.sfm.common.net.ServerboundLabelGunUpdatePacket;
import ca.teamdman.sfm.common.registry.SFMItems;
import ca.teamdman.sfm.common.registry.SFMPackets;
import ca.teamdman.sfm.common.util.SFMHandUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SFM.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)

public class LabelGunKeyMappingHandler {
    private static AltState altState = AltState.Idle;
    private static boolean labelSwitchKeyDown = false;

    public static void setExternalDebounce() {
        altState = AltState.PressCancelledExternally;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        Player player = minecraft.player;
        if (player == null) return;
        handleAltKeyLogic();
        handleLabelSwitchKeyLogic(player);
    }

    private static void handleLabelSwitchKeyLogic(Player player) {
        boolean nextLabelKeyDown = ClientKeyHelpers.isKeyDown(SFMKeyMappings.LABEL_GUN_NEXT_LABEL_KEY);
        boolean prevLabelKeyDown = ClientKeyHelpers.isKeyDown(SFMKeyMappings.LABEL_GUN_PREVIOUS_LABEL_KEY);
        boolean justPressed = !labelSwitchKeyDown && (nextLabelKeyDown || prevLabelKeyDown);
        labelSwitchKeyDown = nextLabelKeyDown || prevLabelKeyDown;
        if (justPressed) {
            var labelGun = SFMHandUtils.getItemAndHand(player, SFMItems.LABEL_GUN_ITEM.get());
            if (labelGun == null) return;
            var nextLabel = LabelGunItem.getNextLabel(labelGun.stack(), prevLabelKeyDown ? -1 : 1);
            SFMPackets.sendToServer(new ServerboundLabelGunUpdatePacket(nextLabel, labelGun.hand()));
        }
    }

    private static void handleAltKeyLogic() {
        Minecraft minecraft = Minecraft.getInstance();

        // don't do anything if a screen is open
        if (minecraft.screen != null) return;

        // only do something if the key was pressed
        boolean alt_down = ClientKeyHelpers.isKeyDown(SFMKeyMappings.TOGGLE_LABEL_VIEW_KEY);
        switch (altState) {
            case Idle -> {
                if (alt_down) {
                    altState = AltState.Pressed;
                }
            }
            case Pressed -> {
                if (!alt_down) {
                    altState = AltState.Idle;
                    assert minecraft.player != null;
                    InteractionHand hand = SFMHandUtils.getHandHoldingItem(
                            minecraft.player,
                            SFMItems.LABEL_GUN_ITEM.get()
                    );
                    if (hand == null) return;
                    // send packet to server to toggle mode
                    SFMPackets.sendToServer(new ServerboundLabelGunToggleLabelViewPacket(hand));
                }
            }
            case PressCancelledExternally -> {
                if (!alt_down) {
                    altState = AltState.Idle;
                }
            }
        }
    }

    private enum AltState {
        Idle,
        Pressed,
        PressCancelledExternally,
    }
}
