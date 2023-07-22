package ca.teamdman.sfm.client.handler;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.client.ClientStuff;
import ca.teamdman.sfm.client.registry.SFMKeyMappings;
import ca.teamdman.sfm.common.Constants;
import ca.teamdman.sfm.common.net.ServerboundContainerExportsInspectionRequestPacket;
import ca.teamdman.sfm.common.registry.SFMPackets;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.widget.ExtendedButton;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber(modid = SFM.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ContainerScreenInspectorHandler {
    private static boolean visible = false;
    @Nullable
    private static AbstractContainerScreen<?> lastScreen = null;
    private static final ExtendedButton exportInspectorButton = new ExtendedButton(
            5,
            50,
            100,
            20,
            Constants.LocalizationKeys.CONTAINER_INSPECTOR_SHOW_EXPORTS_BUTTON.getComponent(),
            (button) -> {
                BlockEntity lookBlockEntity = ClientStuff.getLookBlockEntity();
                if (lastScreen != null && lookBlockEntity != null) {
                    SFMPackets.INSPECTION_CHANNEL.sendToServer(new ServerboundContainerExportsInspectionRequestPacket(
                            lastScreen.getMenu().containerId,
                            lookBlockEntity.getBlockPos()
                    ));
                }
            }
    );

    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.KeyPressed.MouseButtonPressed.Pre event) {
        boolean shouldCapture = Minecraft.getInstance().screen instanceof AbstractContainerScreen<?>;
        if (shouldCapture && visible && exportInspectorButton.clicked(event.getMouseX(), event.getMouseY())) {
            exportInspectorButton.playDownSound(Minecraft.getInstance().getSoundManager());
            exportInspectorButton.onClick(event.getMouseX(), event.getMouseY());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onGuiRender(ScreenEvent.Render.Post event) {
        if (!visible) return;
        if (event.getScreen() instanceof AbstractContainerScreen<?> acs) {
            lastScreen = acs;
            AbstractContainerMenu menu = acs.getMenu();
            int containerSlotCount = 0;
            int inventorySlotCount = 0;
            PoseStack poseStack = event.getPoseStack();
            poseStack.pushPose();
            poseStack.translate(0, 0, 350); // render text over the items but under the tooltips

            // draw the button
            exportInspectorButton.render(poseStack, event.getMouseX(), event.getMouseY(), event.getPartialTick());


            // draw index on each slot
            for (var slot : menu.slots) {
                int colour;
                if (slot.container instanceof Inventory) {
                    //noinspection DataFlowIssue
                    colour = ChatFormatting.YELLOW.getColor();
                    inventorySlotCount++;
                } else {
                    colour = 0xFFF;
                    containerSlotCount++;
                }
                Minecraft.getInstance().font.draw(
                        poseStack,
                        Component.literal(Integer.toString(slot.getSlotIndex())),
                        acs.getGuiLeft() + slot.x,
                        acs.getGuiTop() + slot.y,
                        colour
                );
            }

            // draw text for slot totals
            Minecraft.getInstance().font.drawShadow(
                    poseStack,
                    Constants.LocalizationKeys.CONTAINER_INSPECTOR_CONTAINER_SLOT_COUNT.getComponent(Component
                                                                                                             .literal(
                                                                                                                     String.valueOf(
                                                                                                                             containerSlotCount))
                                                                                                             .withStyle(
                                                                                                                     ChatFormatting.BLUE)),
                    5,
                    5,
                    0xFFFFFF
            );
            Minecraft.getInstance().font.drawShadow(
                    poseStack,
                    Constants.LocalizationKeys.CONTAINER_INSPECTOR_INVENTORY_SLOT_COUNT.getComponent(Component
                                                                                                             .literal(
                                                                                                                     String.valueOf(
                                                                                                                             inventorySlotCount))
                                                                                                             .withStyle(
                                                                                                                     ChatFormatting.YELLOW)),
                    5,
                    25,
                    0xFFFFFF
            );
            poseStack.popPose();
        }
    }

    @SubscribeEvent
    public static void onKeyDown(ScreenEvent.KeyPressed.Pre event) {
        var toggleKey = SFMKeyMappings.CONTAINER_INSPECTOR_KEY.get();
        var toggleKeyPressed = toggleKey.isActiveAndMatches(InputConstants.Type.KEYSYM.getOrCreate(event.getKeyCode()));
        if (toggleKeyPressed) {
            visible = !visible;
            event.setCanceled(true);
        }
    }
}
