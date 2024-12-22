package ca.teamdman.sfm.common.util;

import ca.teamdman.sfm.client.ClientKeyHelpers;
import ca.teamdman.sfm.client.registry.SFMKeyMappings;
import ca.teamdman.sfm.common.localization.LocalizationKeys;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.List;

public class SFMItemUtils {
    public static void appendMoreInfoKeyReminderTextIfOnClient(List<Component> lines) {
        if (FMLEnvironment.dist.isClient()) {
            lines.add(
                    LocalizationKeys.GUI_ADVANCED_TOOLTIP_HINT.getComponent(
                                    SFMKeyMappings.MORE_INFO_TOOLTIP_KEY
                                            .get()
                                            .getTranslatedKeyMessage()
                                            .plainCopy()
                                            .withStyle(ChatFormatting.AQUA))
                            .withStyle(ChatFormatting.GRAY)
            );
        }
    }

    public static boolean isClientAndMoreInfoKeyPressed() {
        return FMLEnvironment.dist.isClient() && ClientKeyHelpers.isKeyDown(SFMKeyMappings.MORE_INFO_TOOLTIP_KEY);
    }

    public static MutableComponent getRainbow(int length) {
        var start = Component.empty();
        ChatFormatting[] rainbowColors = new ChatFormatting[]{
                ChatFormatting.DARK_RED,
                ChatFormatting.RED,
                ChatFormatting.GOLD,
                ChatFormatting.YELLOW,
                ChatFormatting.DARK_GREEN,
                ChatFormatting.GREEN,
                ChatFormatting.DARK_AQUA,
                ChatFormatting.AQUA,
                ChatFormatting.DARK_BLUE,
                ChatFormatting.BLUE,
                ChatFormatting.DARK_PURPLE,
                ChatFormatting.LIGHT_PURPLE
        };
        int rainbowColorsLength = rainbowColors.length;
        int fullCycleLength = 2 * rainbowColorsLength - 2;
        for (int i = 0; i < length - 2; i++) {
            int cyclePosition = i % fullCycleLength;
            int adjustedIndex = cyclePosition < rainbowColorsLength
                                ? cyclePosition
                                : fullCycleLength - cyclePosition;
            ChatFormatting color = rainbowColors[adjustedIndex];
            start = start.append(Component.literal("=").withStyle(color));
        }
        return start;
    }
}
