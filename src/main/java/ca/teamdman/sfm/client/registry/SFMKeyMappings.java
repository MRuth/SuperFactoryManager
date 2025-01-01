package ca.teamdman.sfm.client.registry;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.common.localization.LocalizationKeys;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;


@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = SFM.MOD_ID, value = Dist.CLIENT)

public class SFMKeyMappings {
    public static final Lazy<KeyMapping> MORE_INFO_TOOLTIP_KEY = Lazy.of(() -> new KeyMapping(
            LocalizationKeys.MORE_HOVER_INFO_KEY.key().get(),
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_SHIFT,
            LocalizationKeys.SFM_KEY_CATEGORY.key().get()
    ));

    public static final Lazy<KeyMapping> TOGGLE_LABEL_VIEW_KEY = Lazy.of(() -> new KeyMapping(
            LocalizationKeys.TOGGLE_LABEL_VIEW_KEY.key().get(),
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            LocalizationKeys.SFM_KEY_CATEGORY.key().get()
    ));
    public static final Lazy<KeyMapping> TOGGLE_NETWORK_TOOL_OVERLAY_KEY = Lazy.of(() -> new KeyMapping(
            LocalizationKeys.TOGGLE_NETWORK_TOOL_OVERLAY.key().get(),
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            LocalizationKeys.SFM_KEY_CATEGORY.key().get()
    ));

    public static final Lazy<KeyMapping> CONTAINER_INSPECTOR_KEY = Lazy.of(() -> new KeyMapping(
            LocalizationKeys.CONTAINER_INSPECTOR_TOGGLE_KEY.key().get(),
            KeyConflictContext.GUI,
            KeyModifier.CONTROL,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_I,
            LocalizationKeys.SFM_KEY_CATEGORY.key().get()
    ));

    public static final Lazy<KeyMapping> ITEM_INSPECTOR_KEY = Lazy.of(() -> new KeyMapping(
            LocalizationKeys.ITEM_INSPECTOR_TOGGLE_KEY.key().get(),
            KeyConflictContext.GUI,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
//            GLFW.GLFW_KEY_GRAVE_ACCENT,
            InputConstants.UNKNOWN.getValue(),
            LocalizationKeys.SFM_KEY_CATEGORY.key().get()
    ));

    public static final Lazy<KeyMapping> LABEL_GUN_PICK_BLOCK_MODIFIER_KEY = Lazy.of(() -> new KeyMapping(
            LocalizationKeys.LABEL_GUN_PICK_BLOCK_MODIFIER_KEY.key().get(),
            KeyConflictContext.GUI,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            LocalizationKeys.SFM_KEY_CATEGORY.key().get()
    ));


    public static final Lazy<KeyMapping> LABEL_GUN_NEXT_LABEL_KEY = Lazy.of(() -> new KeyMapping(
            LocalizationKeys.LABEL_GUN_NEXT_LABEL_KEY.key().get(),
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            LocalizationKeys.SFM_KEY_CATEGORY.key().get()
    ));

    public static final Lazy<KeyMapping> LABEL_GUN_PREVIOUS_LABEL_KEY = Lazy.of(() -> new KeyMapping(
            LocalizationKeys.LABEL_GUN_PREVIOUS_LABEL_KEY.key().get(),
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            LocalizationKeys.SFM_KEY_CATEGORY.key().get()
    ));

    @SubscribeEvent
    public static void registerBindings(RegisterKeyMappingsEvent event) {
        event.register(MORE_INFO_TOOLTIP_KEY.get());
        event.register(CONTAINER_INSPECTOR_KEY.get());
        event.register(ITEM_INSPECTOR_KEY.get());
        event.register(TOGGLE_LABEL_VIEW_KEY.get());
        event.register(LABEL_GUN_PICK_BLOCK_MODIFIER_KEY.get());
        event.register(LABEL_GUN_NEXT_LABEL_KEY.get());
        event.register(LABEL_GUN_PREVIOUS_LABEL_KEY.get());
    }
}
