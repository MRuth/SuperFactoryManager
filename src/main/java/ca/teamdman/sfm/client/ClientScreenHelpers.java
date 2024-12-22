package ca.teamdman.sfm.client;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.client.gui.screen.*;
import ca.teamdman.sfm.common.containermenu.ManagerContainerMenu;
import ca.teamdman.sfm.common.localization.LocalizationKeys;
import ca.teamdman.sfm.common.net.ServerboundManagerLogDesireUpdatePacket;
import ca.teamdman.sfm.common.registry.SFMPackets;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.function.Consumer;

public class ClientScreenHelpers {
    public static void setOrPushScreen(Screen screen) {
        if (Minecraft.getInstance().screen == null) {
            Minecraft
                    .getInstance()
                    .setScreen(screen);
        } else {
            Minecraft
                    .getInstance()
                    .pushGuiLayer(screen);
        }
    }

    public static void showLabelGunScreen(
            ItemStack stack,
            InteractionHand hand
    ) {
        setOrPushScreen(new LabelGunScreen(stack, hand));
    }

    public static void showProgramEditScreen(
            String initialContent,
            Consumer<String> saveCallback
    ) {
        ProgramEditScreen screen = new ProgramEditScreen(initialContent, saveCallback);
        setOrPushScreen(screen);
        screen.scrollToTop();
    }

    public static void showProgramEditScreen(String initialContent) {
        showProgramEditScreen(initialContent, (x) -> {
        });
    }

    public static void showExampleListScreen(
            String program,
            Consumer<String> saveCallback
    ) {
        setOrPushScreen(new ExamplesScreen((chosenTemplate, templates) -> showExampleEditScreen(
                program,
                chosenTemplate,
                templates,
                saveCallback
        )));
    }

    public static void showExampleEditScreen(
            String program,
            String chosenTemplate,
            Map<String, String> templates,
            Consumer<String> saveCallback
    ) {
        ProgramEditScreen screen = new ExampleEditScreen(program, chosenTemplate, templates, saveCallback);
        setOrPushScreen(screen);
        screen.scrollToTop();
    }

    public static void showLogsScreen(ManagerContainerMenu menu) {
        LogsScreen screen = new LogsScreen(menu);
        setOrPushScreen(screen);
        screen.scrollToBottom();
        SFMPackets.sendToServer(new ServerboundManagerLogDesireUpdatePacket(
                menu.containerId,
                menu.MANAGER_POSITION,
                true
        ));
    }

    // TODO: copy item id, not just NBT
    // TODO: replace with showing a screen with the data
    public static void showItemInspectorScreen(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            String content = tag.toString();
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.keyboardHandler.setClipboard(content);
            SFM.LOGGER.info("Copied {} characters to clipboard", content.length());
            assert minecraft.player != null;
            minecraft.player.sendSystemMessage(
                    LocalizationKeys.ITEM_INSPECTOR_COPIED_TO_CLIPBOARD.getComponent(
                            Component.literal(String.valueOf(content.length())).withStyle(ChatFormatting.AQUA)
                    )
            );
        }
    }
}
