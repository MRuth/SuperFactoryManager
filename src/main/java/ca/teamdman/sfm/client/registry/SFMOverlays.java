package ca.teamdman.sfm.client.registry;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.client.gui.LabelGunReminderOverlay;
import ca.teamdman.sfm.client.gui.NetworkToolReminderOverlay;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = SFM.MOD_ID, value = Dist.CLIENT)
public class SFMOverlays {
    public static final Lazy<LabelGunReminderOverlay> LABEL_GUN_REMINDER_OVERLAY = Lazy.of(LabelGunReminderOverlay::new);
    public static final Lazy<NetworkToolReminderOverlay> NETWORK_TOOL_REMINDER_OVERLAY = Lazy.of(NetworkToolReminderOverlay::new);

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(
                VanillaGuiOverlay.HOTBAR.id(),
                "label_gun_reminder",
                LABEL_GUN_REMINDER_OVERLAY.get()
        );
        event.registerAbove(
                VanillaGuiOverlay.HOTBAR.id(),
                "network_tool_reminder",
                NETWORK_TOOL_REMINDER_OVERLAY.get()
        );
    }
}
