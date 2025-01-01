package ca.teamdman.sfm.client.gui.screen;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.client.ClientDiagnosticInfo;
import ca.teamdman.sfm.client.ClientScreenHelpers;
import ca.teamdman.sfm.common.command.ConfigCommandBehaviour;
import ca.teamdman.sfm.common.containermenu.ManagerContainerMenu;
import ca.teamdman.sfm.common.item.DiskItem;
import ca.teamdman.sfm.common.localization.LocalizationEntry;
import ca.teamdman.sfm.common.localization.LocalizationKeys;
import ca.teamdman.sfm.common.net.*;
import ca.teamdman.sfm.common.program.LabelPositionHolder;
import ca.teamdman.sfm.common.registry.SFMPackets;
import ca.teamdman.sfml.ast.Program;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.client.gui.widget.ExtendedButton;
import org.apache.logging.log4j.Level;
import org.lwjgl.glfw.GLFW;

import java.text.DecimalFormat;
import java.util.List;

import static ca.teamdman.sfm.common.localization.LocalizationKeys.*;

public class ManagerScreen extends AbstractContainerScreen<ManagerContainerMenu> {
    private static final ResourceLocation BACKGROUND_TEXTURE_LOCATION = new ResourceLocation(
            SFM.MOD_ID,
            "textures/gui/container/manager.png"
    );
    private final float STATUS_DURATION = 40;
    private Component status = Component.empty();
    private float statusCountdown = 0;
    @SuppressWarnings("NotNullFieldNotInitialized")
    private ExtendedButton diagButton;
    @SuppressWarnings("NotNullFieldNotInitialized")
    private ExtendedButton clipboardPasteButton;
    @SuppressWarnings("NotNullFieldNotInitialized")
    private ExtendedButton clipboardCopyButton;
    @SuppressWarnings("NotNullFieldNotInitialized")
    private ExtendedButton resetButton;
    @SuppressWarnings("NotNullFieldNotInitialized")
    private ExtendedButton editButton;
    @SuppressWarnings("NotNullFieldNotInitialized")
    private ExtendedButton examplesButton;
    @SuppressWarnings("NotNullFieldNotInitialized")
    private ExtendedButton logsButton;
    @SuppressWarnings("NotNullFieldNotInitialized")
    private ExtendedButton rebuildButton;
    @SuppressWarnings("NotNullFieldNotInitialized")
    private ExtendedButton serverConfigButton;

    public ManagerScreen(
            ManagerContainerMenu menu,
            Inventory inv,
            Component title
    ) {
        super(menu, inv, title);
    }

    public List<ExtendedButton> getButtonsForJEIExclusionZones() {
        return List.of(
                clipboardPasteButton,
                editButton,
                examplesButton,
                clipboardCopyButton,
                logsButton,
                rebuildButton,
                serverConfigButton
        );
    }

    public boolean isReadOnly() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null || player.isSpectator();
    }

    public void updateVisibilities() {
        boolean diskPresent = menu.getSlot(0).hasItem();
        diagButton.visible = shouldShowDiagButton();
        clipboardCopyButton.visible = diskPresent;
        logsButton.visible = diskPresent;
        rebuildButton.visible = diskPresent && !isReadOnly();
        clipboardPasteButton.visible = diskPresent && !isReadOnly();
        resetButton.visible = diskPresent && !isReadOnly();
        editButton.visible = diskPresent && !isReadOnly();
    }

    @Override
    public boolean keyPressed(
            int pKeyCode,
            int pScanCode,
            int pModifiers
    ) {
        if (Screen.isPaste(pKeyCode) && clipboardPasteButton.visible) {
            onClipboardPasteButtonClicked();
            return true;
        } else if (Screen.isCopy(pKeyCode) && clipboardCopyButton.visible) {
            onClipboardCopyButtonClicked();
            return true;
        } else if (pKeyCode == GLFW.GLFW_KEY_E
                   && Screen.hasControlDown()
                   && Screen.hasShiftDown()
                   && examplesButton.visible) {
            onExamplesButtonClicked();
            return true;
        } else if (pKeyCode == GLFW.GLFW_KEY_E && Screen.hasControlDown() && editButton.visible) {
            onEditButtonClicked();
            return true;
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    public ChatFormatting getMillisecondColour(float ms) {
        if (ms <= 5) {
            return ChatFormatting.GREEN;
        } else if (ms <= 15) {
            return ChatFormatting.YELLOW;
        } else {
            return ChatFormatting.RED;
        }
    }

    @Override
    public void render(
            PoseStack poseStack,
            int mx,
            int my,
            float partialTicks
    ) {
        this.renderBackground(poseStack);
        super.render(poseStack, mx, my, partialTicks);
        this.renderTooltip(poseStack, mx, my);

        updateVisibilities();

        // update status countdown
        statusCountdown -= partialTicks;
    }

    private Button.OnTooltip buildTooltip(LocalizationEntry entry) {
        return (btn, pose, mx, my) -> renderTooltip(
                pose,
                font.split(entry.getComponent(), Math.max(width / 2 - 43, 170)),
                mx,
                my
        );
    }

    @Override
    protected void init() {
        super.init();
        int buttonWidth = 120;
        clipboardPasteButton = this.addRenderableWidget(new ExtendedButtonWithTooltip(
                (this.width - this.imageWidth) / 2 - buttonWidth,
                (this.height - this.imageHeight) / 2 + 16,
                buttonWidth,
                16,
                MANAGER_GUI_PASTE_FROM_CLIPBOARD_BUTTON.getComponent(),
                button -> this.onClipboardPasteButtonClicked(),
                buildTooltip(MANAGER_GUI_PASTE_FROM_CLIPBOARD_BUTTON_TOOLTIP)
        ));
        editButton = this.addRenderableWidget(new ExtendedButtonWithTooltip(
                (this.width - this.imageWidth) / 2 - buttonWidth,
                (this.height - this.imageHeight) / 2 + 16 + 50,
                buttonWidth,
                16,
                MANAGER_GUI_EDIT_BUTTON.getComponent(),
                button -> onEditButtonClicked(),
                buildTooltip(MANAGER_GUI_EDIT_BUTTON_TOOLTIP)
        ));
        examplesButton = this.addRenderableWidget(new ExtendedButtonWithTooltip(
                (this.width - this.imageWidth) / 2 - buttonWidth,
                (this.height - this.imageHeight) / 2 + 16 * 2 + 50,
                buttonWidth,
                16,
                MANAGER_GUI_VIEW_EXAMPLES_BUTTON.getComponent(),
                button -> onExamplesButtonClicked(),
                buildTooltip(MANAGER_GUI_VIEW_EXAMPLES_BUTTON_TOOLTIP)
        ));
        clipboardCopyButton = this.addRenderableWidget(new ExtendedButton(
                (this.width - this.imageWidth) / 2 - buttonWidth,
                (this.height - this.imageHeight) / 2 + 128,
                buttonWidth,
                16,
                MANAGER_GUI_COPY_TO_CLIPBOARD_BUTTON.getComponent(),
                button -> this.onClipboardCopyButtonClicked()
        ));
        logsButton = this.addRenderableWidget(new ExtendedButton(
                (this.width - this.imageWidth) / 2 - buttonWidth,
                (this.height - this.imageHeight) / 2 + 16 * 9,
                buttonWidth,
                16,
                MANAGER_GUI_VIEW_LOGS_BUTTON.getComponent(),
                button -> onLogsButtonClicked()
        ));
        rebuildButton = this.addRenderableWidget(new ExtendedButton(
                (this.width - this.imageWidth) / 2 - buttonWidth,
                (this.height - this.imageHeight) / 2 + 16 * 10,
                buttonWidth,
                16,
                MANAGER_GUI_REBUILD_BUTTON.getComponent(),
                button -> this.onRebuildButtonClicked()
        ));
        serverConfigButton = this.addRenderableWidget(new ExtendedButton(
                (this.width - this.imageWidth) / 2 - buttonWidth,
                (this.height - this.imageHeight) / 2 + 16 * 11,
                buttonWidth,
                16,
                MANAGER_GUI_SERVER_CONFIG_BUTTON.getComponent(),
                button -> this.onServerConfigButtonClicked()
        ));
        resetButton = this.addRenderableWidget(new ExtendedButtonWithTooltip(
                (this.width - this.imageWidth) / 2 + 120,
                (this.height - this.imageHeight) / 2 + 10,
                50,
                12,
                MANAGER_GUI_RESET_BUTTON.getComponent(),
                button -> onResetButtonClicked(),
                buildTooltip(MANAGER_GUI_RESET_BUTTON_TOOLTIP)
        ));
        diagButton = this.addRenderableWidget(new ExtendedButtonWithTooltip(
                (this.width - this.imageWidth) / 2 + 35,
                (this.height - this.imageHeight) / 2 + 48,
                12,
                14,
                Component.literal("!"),
                button -> onDiagButtonClicked(),
                buildTooltip(isReadOnly()
                             ? MANAGER_GUI_WARNING_BUTTON_TOOLTIP_READ_ONLY
                             : MANAGER_GUI_WARNING_BUTTON_TOOLTIP)
        ));
        updateVisibilities();
    }

    private void onDiagButtonClicked() {
        if (Screen.hasShiftDown() && !isReadOnly()) {
            sendAttemptFix();
        } else {
            this.onSaveDiagClipboard();
        }
    }

    private String getProgram() {
        return menu.program;
    }

    private void onEditButtonClicked() {
        ClientScreenHelpers.showProgramEditScreen(getProgram(), this::sendProgram);
    }

    private void onExamplesButtonClicked() {
        ClientScreenHelpers.showExampleListScreen(getProgram(), this::sendProgram);
    }

    private void onLogsButtonClicked() {
        ClientScreenHelpers.showLogsScreen(menu);
    }

    private void performReset() {
        SFMPackets.sendToServer(new ServerboundManagerResetPacket(
                menu.containerId,
                menu.MANAGER_POSITION
        ));
        status = MANAGER_GUI_STATUS_RESET.getComponent();
        statusCountdown = STATUS_DURATION;
    }

    private void onResetButtonClicked() {
        if (getProgram().isBlank() && LabelPositionHolder.from(menu.getDisk()).isEmpty()) {
            performReset();
            return;
        }
        ConfirmScreen confirmScreen = new ConfirmScreen(
                proceed -> {
                    assert this.minecraft != null;
                    this.minecraft.popGuiLayer(); // Close confirm screen
                    if (proceed) {
                        performReset();
                    }
                },
                LocalizationKeys.MANAGER_RESET_CONFIRM_SCREEN_TITLE.getComponent(),
                LocalizationKeys.MANAGER_RESET_CONFIRM_SCREEN_MESSAGE.getComponent(),
                LocalizationKeys.MANAGER_RESET_CONFIRM_SCREEN_YES_BUTTON.getComponent(),
                LocalizationKeys.MANAGER_RESET_CONFIRM_SCREEN_NO_BUTTON.getComponent()
        );
        assert this.minecraft != null;
        this.minecraft.pushGuiLayer(confirmScreen);
        confirmScreen.setDelay(20);
    }

    private void onRebuildButtonClicked() {
        SFMPackets.sendToServer(new ServerboundManagerRebuildPacket(
                menu.containerId,
                menu.MANAGER_POSITION
        ));
        status = MANAGER_GUI_STATUS_REBUILD.getComponent();
        statusCountdown = STATUS_DURATION;
    }

    private void onServerConfigButtonClicked() {
        SFMPackets.sendToServer(new ServerboundConfigRequestPacket(ConfigCommandBehaviour.SHOW));
    }

    private void sendAttemptFix() {
        SFMPackets.sendToServer(new ServerboundManagerFixPacket(
                menu.containerId,
                menu.MANAGER_POSITION
        ));
        status = MANAGER_GUI_STATUS_FIX.getComponent();
        statusCountdown = STATUS_DURATION;
    }

    private void sendProgram(String program) {
        program = SFMPacketDaddy.truncate(program, Program.MAX_PROGRAM_LENGTH);
        SFMPackets.sendToServer(new ServerboundManagerProgramPacket(
                menu.containerId,
                menu.MANAGER_POSITION,
                program
        ));
        menu.program = program;
        status = MANAGER_GUI_STATUS_LOADED_CLIPBOARD.getComponent();
        statusCountdown = STATUS_DURATION;
    }

    private void onClipboardCopyButtonClicked() {
        try {
            Minecraft.getInstance().keyboardHandler.setClipboard(menu.program);
            status = MANAGER_GUI_STATUS_SAVED_CLIPBOARD.getComponent();
            statusCountdown = STATUS_DURATION;
        } catch (Throwable t) {
            SFM.LOGGER.error("failed to save clipboard", t);
        }
    }

    private boolean shouldShowDiagButton() {
        var disk = menu.getDisk();
        if (!(disk.getItem() instanceof DiskItem)) return false;
        var errors = DiskItem.getErrors(disk);
        var warnings = DiskItem.getWarnings(disk);
        return !errors.isEmpty() || !warnings.isEmpty();
    }

    private void onSaveDiagClipboard() {
        try {
            var disk = menu.CONTAINER.getItem(0);
            if (!(disk.getItem() instanceof DiskItem)) return;
            String diagnosticInfo = ClientDiagnosticInfo.getDiagnosticInfo(menu.program, disk);
            Minecraft.getInstance().keyboardHandler.setClipboard(diagnosticInfo);
            status = MANAGER_GUI_STATUS_SAVED_CLIPBOARD.getComponent();
            statusCountdown = STATUS_DURATION;
        } catch (Throwable t) {
            SFM.LOGGER.error("failed saving clipboard", t);
        }
    }

    private void onClipboardPasteButtonClicked() {
        String clipboardContents;
        try {
            clipboardContents = Minecraft.getInstance().keyboardHandler.getClipboard();
        } catch (Throwable t) {
            SFM.LOGGER.error("failed loading clipboard", t);
            return;
        }
        String existingProgram = getProgram();
        boolean shouldConfirm = !existingProgram.isBlank() && !existingProgram.equals(clipboardContents);
        if (!shouldConfirm) {
            sendProgram(clipboardContents);
            return;
        }

        ConfirmScreen confirmScreen = new ConfirmScreen(
                proceed -> {
                    assert this.minecraft != null;
                    this.minecraft.popGuiLayer(); // Close confirm screen
                    if (proceed) {
                        sendProgram(clipboardContents);
                    }
                },
                LocalizationKeys.MANAGER_PASTE_CONFIRM_SCREEN_TITLE.getComponent(),
                LocalizationKeys.MANAGER_PASTE_CONFIRM_SCREEN_MESSAGE.getComponent(),
                LocalizationKeys.MANAGER_PASTE_CONFIRM_SCREEN_YES_BUTTON.getComponent(),
                LocalizationKeys.MANAGER_PASTE_CONFIRM_SCREEN_NO_BUTTON.getComponent()
        );
        assert this.minecraft != null;
        this.minecraft.pushGuiLayer(confirmScreen);
        confirmScreen.setDelay(20);
    }

    @Override
    protected void renderLabels(
            PoseStack poseStack,
            int mx,
            int my
    ) {
        // draw title
        super.renderLabels(poseStack, mx, my);

        // draw state string
        var state = menu.state;
        this.font.draw(
                poseStack,
                MANAGER_GUI_STATE.getComponent(state.LOC.getComponent().withStyle(state.COLOR)),
                titleLabelX,
                20,
                0
        );

        // draw log level
        if (!menu.logLevel.equals(Level.OFF.name())) {
            poseStack.pushPose();
            poseStack.translate(
                    titleLabelX,
                    font.lineHeight * 1.5,
                    0f
            );
            poseStack.scale(0.5f, 0.5f, 1f);
            this.font.draw(
                    poseStack,
                    Component.literal(menu.logLevel),
                    0f,
                    0f,
                    0
            );
            poseStack.popPose();
        }

        // draw status string
        if (statusCountdown > 0) {
            this.font.draw(
                    poseStack,
                    status,
                    inventoryLabelX + font.width(playerInventoryTitle.getString()) + 5,
                    inventoryLabelY,
                    0
            );
        }

        // Find the maximum tick time for normalization
        long peakTickTimeNanoseconds = 0;
        for (int i = 0; i < menu.tickTimeNanos.length; i++) {
            peakTickTimeNanoseconds = Long.max(peakTickTimeNanoseconds, menu.tickTimeNanos[i]);
        }
        long yMax = Long.max(peakTickTimeNanoseconds, 50000000); // Start with max at 50ms but allow it to grow

        // Constants for the plot size and position
        final int plotX = titleLabelX + 45;
        final int plotY = 40;
        final int spaceBetweenPoints = 6;
        final int plotWidth = spaceBetweenPoints * (menu.tickTimeNanos.length - 1);
        final int plotHeight = 30;


        // Set up rendering
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        Matrix4f pose = poseStack.last().pose();
        BufferBuilder bufferbuilder;

        // Draw the plot background
        bufferbuilder = tesselator.getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        bufferbuilder.vertex(pose, plotX, plotY, 0).color(0, 0, 0, 0.5f).endVertex();
        bufferbuilder.vertex(pose, plotX + plotWidth, plotY, 0).color(0, 0, 0, 0.5f).endVertex();
        bufferbuilder.vertex(pose, plotX + plotWidth, plotY + plotHeight, 0).color(0, 0, 0, 0.5f).endVertex();
        bufferbuilder.vertex(pose, plotX, plotY + plotHeight, 0).color(0, 0, 0, 0.5f).endVertex();
        bufferbuilder.vertex(pose, plotX, plotY, 0).color(0, 0, 0, 0.5f).endVertex();
        tesselator.end();

        // Draw lines for each data point
        bufferbuilder = tesselator.getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        int mouseTickTimeIndex = -1;
        for (int i = 0; i < menu.tickTimeNanos.length; i++) {
            long y = menu.tickTimeNanos[i];
            float normalizedTickTime = y == 0 ? 0 : (float) (Math.log10(y) / Math.log10(yMax));
            int plotPosY = plotY + plotHeight - (int) (normalizedTickTime * plotHeight);

            int plotPosX = plotX + spaceBetweenPoints * i;

            // Color the lines based on their tick times (green to red)
            var c = getMillisecondColour(y / 1_000_000f);
            //noinspection DataFlowIssue
            float red = ((c.getColor() >> 16) & 0xFF) / 255f;
            float green = ((c.getColor() >> 8) & 0xFF) / 255f;
            float blue = (c.getColor() & 0xFF) / 255f;

            bufferbuilder
                    .vertex(pose, (float) plotPosX, (float) plotPosY, (float) getBlitOffset())
                    .color(red, green, blue, 1f)
                    .endVertex();

            // Check if the mouse is hovering over this line
            if (mx - leftPos >= plotPosX - spaceBetweenPoints / 2
                && mx - leftPos <= plotPosX + spaceBetweenPoints / 2
                && my - topPos >= plotY - 2
                && my - topPos <= plotY + plotHeight + 2) {
                mouseTickTimeIndex = i;
            }
        }
        tesselator.end();

        // Draw the tick time text
        var format = new DecimalFormat("0.000");
        if (mouseTickTimeIndex != -1) { // We are hovering over the plot
            // Draw the tick time text for the hovered point instead of peak
            {
                long hoveredTickTimeNanoseconds = menu.tickTimeNanos[mouseTickTimeIndex];
                var hoveredTickTimeMilliseconds = hoveredTickTimeNanoseconds / 1_000_000f;
                String formattedMillis = format.format(hoveredTickTimeMilliseconds);
                ChatFormatting lagColor = getMillisecondColour(hoveredTickTimeMilliseconds);
                Component milliseconds = Component.literal(formattedMillis).withStyle(lagColor);
                this.font.draw(
                        poseStack,
                        MANAGER_GUI_HOVERED_TICK_TIME_MS.getComponent(milliseconds),
                        titleLabelX,
                        20f + font.lineHeight + 0.1f,
                        0
                );
            }

            // draw a vertical line
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            tesselator = Tesselator.getInstance();
            bufferbuilder = tesselator.getBuilder();
            bufferbuilder.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            pose = poseStack.last().pose();

            int x = plotX + spaceBetweenPoints * mouseTickTimeIndex;
            bufferbuilder
                    .vertex(pose, (float) x, (float) plotY, (float) getBlitOffset())
                    .color(1f, 1f, 1f, 1f)
                    .endVertex();
            bufferbuilder
                    .vertex(pose, (float) x, (float) plotY + plotHeight, (float) getBlitOffset())
                    .color(1f, 1f, 1f, 1f)
                    .endVertex();
            tesselator.end();
        } else {
            // Draw the tick time text for peak value
            var peakTickTimeMilliseconds = peakTickTimeNanoseconds / 1_000_000f;
            String formattedMillis = format.format(peakTickTimeMilliseconds);
            ChatFormatting lagColor = getMillisecondColour(peakTickTimeMilliseconds);
            Component milliseconds = Component.literal(formattedMillis).withStyle(lagColor);
            this.font.draw(
                    poseStack,
                    MANAGER_GUI_PEAK_TICK_TIME_MS.getComponent(milliseconds),
                    titleLabelX,
                    20f + font.lineHeight + 0.1f,
                    0
            );
        }

        // Restore stuff
        RenderSystem.disableBlend();
        RenderSystem.enableTexture();
    }

    @Override
    protected void renderTooltip(
            PoseStack pose,
            int mx,
            int my
    ) {
        if (Minecraft.getInstance().screen != this) {
            // this should fix the annoying Ctrl+E popup when editing
            this.renderables
                    .stream()
                    .filter(AbstractWidget.class::isInstance)
                    .map(AbstractWidget.class::cast)
                    .forEach(w -> w.setFocused(false));
            return;
        }

        // 1.19.2: manually render button tooltips
        this.renderables
                .stream()
                .filter(ExtendedButtonWithTooltip.class::isInstance)
                .map(ExtendedButtonWithTooltip.class::cast)
                .forEach(x -> x.renderToolTip(pose, mx, my));

        // render hovered item
        super.renderTooltip(pose, mx, my);
    }

    @Override
    protected void renderBg(
            PoseStack matrixStack,
            float partialTicks,
            int mx,
            int my
    ) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        if (!menu.logLevel.equals(Level.OFF.name())) {
            RenderSystem.setShaderColor(0.2f, 0.8f, 1f, 1f);
        } else {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }
        RenderSystem.setShaderTexture(0, BACKGROUND_TEXTURE_LOCATION);
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        this.blit(matrixStack, i, j, 0, 0, this.imageWidth, this.imageHeight);
    }
}
