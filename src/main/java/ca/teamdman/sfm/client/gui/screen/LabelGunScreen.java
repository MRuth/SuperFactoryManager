package ca.teamdman.sfm.client.gui.screen;

import ca.teamdman.sfm.common.Constants;
import ca.teamdman.sfm.common.net.ServerboundLabelGunClearPacket;
import ca.teamdman.sfm.common.net.ServerboundLabelGunPrunePacket;
import ca.teamdman.sfm.common.net.ServerboundLabelGunUpdatePacket;
import ca.teamdman.sfm.common.program.LabelHolder;
import ca.teamdman.sfm.common.registry.SFMPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;

public class LabelGunScreen extends Screen {
    private final InteractionHand HAND;
    private final LabelHolder LABEL_HOLDER;
    @SuppressWarnings("NotNullFieldNotInitialized")
    private EditBox labelField;
    private boolean shouldRebuildWidgets = false;

    public LabelGunScreen(ItemStack labelGunStack, InteractionHand hand) {
        super(Constants.LocalizationKeys.LABEL_GUN_GUI_TITLE.getComponent());
        LABEL_HOLDER = LabelHolder.from(labelGunStack);
        HAND = hand;
    }

    @Override
    protected void init() {
        super.init();
        assert this.minecraft != null;
        this.labelField = addRenderableWidget(new EditBox(
                this.font,
                this.width / 2 - 150,
                50,
                300,
                20,
                Constants.LocalizationKeys.LABEL_GUN_GUI_LABEL_PLACEHOLDER.getComponent()
        ));
        this.setInitialFocus(labelField);
        this.setFocused(labelField);

        this.addRenderableWidget(new Button.Builder(
                                         Constants.LocalizationKeys.LABEL_GUN_GUI_CLEAR_BUTTON.getComponent(),
                                         __ -> {
                                             SFMPackets.LABEL_GUN_ITEM_CHANNEL.sendToServer(new ServerboundLabelGunClearPacket(HAND));
                                             LABEL_HOLDER.clear();
                                             shouldRebuildWidgets = true;
                                         }
                                 )
                                         .pos(this.width / 2 - 210, 50)
                                         .size(50, 20).build()
        );
        this.addRenderableWidget(new Button.Builder(
                Constants.LocalizationKeys.LABEL_GUN_GUI_PRUNE_BUTTON.getComponent(),
                (btn) -> {
                    SFMPackets.LABEL_GUN_ITEM_CHANNEL.sendToServer(new ServerboundLabelGunPrunePacket(HAND));
                    LABEL_HOLDER.prune();
                    shouldRebuildWidgets = true;
                }
        )
                                         .pos(this.width / 2 + 160, 50)
                                         .size(50, 20).build());
        this.addRenderableWidget(new Button.Builder(CommonComponents.GUI_DONE, __ -> this.onDone())
                                         .pos(this.width / 2 - 2 - 150, this.height - 50)
                                         .size(300, 20)
                                         .build());
        {
            var labels = LABEL_HOLDER.get().keySet().stream().sorted(Comparator.naturalOrder()).toList();
            int i = 0;
            int buttonWidth = LABEL_HOLDER.get()
                                      .entrySet()
                                      .stream()
                                      .map(entry -> Constants.LocalizationKeys.LABEL_GUN_GUI_LABEL_BUTTON.getComponent(
                                              entry.getKey(),
                                              entry.getValue().size()
                                      ).getString())
                                      .mapToInt(this.font::width)
                                      .max().orElse(50) + 10;
            int buttonHeight = 20;
            int paddingX = 5;
            int paddingY = 5;
            int buttonsPerRow = this.width / (buttonWidth + paddingX);
            for (var label : labels) {
                int x = (this.width - (buttonWidth + paddingX) * Math.min(buttonsPerRow, labels.size())) / 2
                        + paddingX
                        + (i % buttonsPerRow) * (
                        buttonWidth
                        + paddingX
                );
                int y = 80 + (i / buttonsPerRow) * (buttonHeight + paddingY);
                int count = LABEL_HOLDER.getPositions(label).size();
                this.addRenderableWidget(new Button.Builder(
                                                 Constants.LocalizationKeys.LABEL_GUN_GUI_LABEL_BUTTON.getComponent(label, count),
                                                 (btn) -> {
                                                     this.labelField.setValue(label);
                                                     this.onDone();
                                                 }
                                         )
                                                 .pos(x, y)
                                                 .size(buttonWidth, buttonHeight).build()
                );
                i++;
            }
        }
    }

    @Override
    public boolean keyPressed(int key, int mod1, int mod2) {
        if (super.keyPressed(key, mod1, mod2)) return true;
        if (key != GLFW.GLFW_KEY_ENTER && key != GLFW.GLFW_KEY_KP_ENTER) return false;
        onDone();
        return true;
    }

    public void onDone() {
        SFMPackets.LABEL_GUN_ITEM_CHANNEL.sendToServer(new ServerboundLabelGunUpdatePacket(
                labelField.getValue(),
                HAND
        ));
        onClose();
    }

    @Override
    public void resize(Minecraft mc, int x, int y) {
        var prev = this.labelField.getValue();
        init(mc, x, y);
        super.resize(mc, x, y);
        this.labelField.setValue(prev);
    }

    @Override
    public void render(GuiGraphics graphics, int mx, int my, float partialTicks) {
        if (shouldRebuildWidgets) {
            // we delay this because focus gets reset _after_ the button event handler
            // we want to end with the label input field focused
            shouldRebuildWidgets = false;
            rebuildWidgets();
        }
        this.renderBackground(graphics);
        super.render(graphics, mx, my, partialTicks);
    }
}
