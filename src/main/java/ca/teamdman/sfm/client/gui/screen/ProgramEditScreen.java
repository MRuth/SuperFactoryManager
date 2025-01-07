package ca.teamdman.sfm.client.gui.screen;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.client.ProgramSyntaxHighlightingHelper;
import ca.teamdman.sfm.client.ProgramTokenContextActions;
import ca.teamdman.sfm.client.gui.EditorUtils;
import ca.teamdman.sfm.common.config.SFMConfig;
import ca.teamdman.sfm.common.localization.LocalizationEntry;
import ca.teamdman.sfm.common.localization.LocalizationKeys;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static ca.teamdman.sfm.common.localization.LocalizationKeys.PROGRAM_EDIT_SCREEN_DONE_BUTTON_TOOLTIP;
import static ca.teamdman.sfm.common.localization.LocalizationKeys.PROGRAM_EDIT_SCREEN_TOGGLE_LINE_NUMBERS_BUTTON_TOOLTIP;

public class ProgramEditScreen extends Screen {
    protected final String INITIAL_CONTENT;
    protected final Consumer<String> SAVE_CALLBACK;
    @SuppressWarnings("NotNullFieldNotInitialized")
    protected MyMultiLineEditBox textarea;
    protected String lastProgram = "";
    protected List<MutableComponent> lastProgramWithSyntaxHighlighting = Collections.emptyList();

    public ProgramEditScreen(
            String initialContent,
            Consumer<String> saveCallback
    ) {
        super(LocalizationKeys.PROGRAM_EDIT_SCREEN_TITLE.getComponent());
        this.INITIAL_CONTENT = initialContent;
        this.SAVE_CALLBACK = saveCallback;
    }

    public static MutableComponent substring(
            MutableComponent component,
            int start,
            int end
    ) {
        var rtn = Component.empty();
        AtomicInteger seen = new AtomicInteger(0);
        component.visit((style, content) -> {
            int contentStart = Math.max(start - seen.get(), 0);
            int contentEnd = Math.min(end - seen.get(), content.length());

            if (contentStart < contentEnd) {
                rtn.append(Component.literal(content.substring(contentStart, contentEnd)).withStyle(style));
            }
            seen.addAndGet(content.length());
            return Optional.empty();
        }, Style.EMPTY);
        return rtn;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * The user has indicated to save by hitting Shift+Enter or by pressing the Done button
     */
    public void saveAndClose() {
        SAVE_CALLBACK.accept(textarea.getValue());

        assert this.minecraft != null;
        this.minecraft.popGuiLayer();
    }

    public void closeWithoutSaving() {
        assert this.minecraft != null;
        this.minecraft.popGuiLayer();
    }

    /**
     * The user has tried to close the GUI without saving by hitting the Esc key
     */
    @Override
    public void onClose() {
        // If the content is different, ask to save
        if (!INITIAL_CONTENT.equals(textarea.getValue())) {
            assert this.minecraft != null;
            ConfirmScreen exitWithoutSavingConfirmScreen = getExitWithoutSavingConfirmScreen();
            this.minecraft.pushGuiLayer(exitWithoutSavingConfirmScreen);
            exitWithoutSavingConfirmScreen.setDelay(20);
        } else {
            super.onClose();
        }
    }

    @Override
    public boolean keyReleased(
            int pKeyCode,
            int pScanCode,
            int pModifiers
    ) {
        if (pKeyCode == GLFW.GLFW_KEY_LEFT_CONTROL || pKeyCode == GLFW.GLFW_KEY_RIGHT_CONTROL) {
            // if control released => update syntax highlighting
            textarea.rebuild(Screen.hasControlDown());
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(
            char pCodePoint,
            int pModifiers
    ) {
        if (Screen.hasControlDown() && pCodePoint == ' ') {
            return true;
        }
        return super.charTyped(pCodePoint, pModifiers);
    }

    public void scrollToTop() {
        textarea.setScrollAmount(0d);
    }

    @Override
    public boolean keyPressed(
            int pKeyCode,
            int pScanCode,
            int pModifiers
    ) {
        // TODO: add separate keybindings for
        // context action - hold to arm
        // context action - execute
        // indent - increase
        // indent - decrease
        // save and close - hold to arm
        // save and close - execute
        if ((pKeyCode == GLFW.GLFW_KEY_ENTER || pKeyCode == GLFW.GLFW_KEY_KP_ENTER) && Screen.hasShiftDown()) {
            saveAndClose();
            return true;
        }
        if (pKeyCode == GLFW.GLFW_KEY_TAB) {
            // if tab pressed with no selection and not holding shift => insert 4 spaces
            // if tab pressed with no selection and holding shift => de-indent current line
            // if tab pressed with selection and not holding shift => de-indent lines containing selection 4 spaces
            // if tab pressed with selection and holding shift => indent lines containing selection 4 spaces
            String content = textarea.getValue();
            int cursor = textarea.getCursorPosition();
            int selectionCursor = textarea.getSelectionCursorPosition();
            EditorUtils.ManipulationResult result;
            if (Screen.hasShiftDown()) { // de-indent
                result = EditorUtils.deindent(content, cursor, selectionCursor);
            } else { // indent
                result = EditorUtils.indent(content, cursor, selectionCursor);
            }
            textarea.setValue(result.content());
            textarea.setCursorPosition(result.cursorPosition());
            textarea.setSelectionCursorPosition(result.selectionCursorPosition());
            return true;
        }
        if (pKeyCode == GLFW.GLFW_KEY_LEFT_CONTROL || pKeyCode == GLFW.GLFW_KEY_RIGHT_CONTROL) {
            // if control pressed => update syntax highlighting
            textarea.rebuild(Screen.hasControlDown());
            return true;
        }
        if (pKeyCode == GLFW.GLFW_KEY_SLASH && Screen.hasControlDown()) {
            // toggle line comments for selected lines
            String content = textarea.getValue();
            int cursor = textarea.getCursorPosition();
            int selectionCursor = textarea.getSelectionCursorPosition();
            EditorUtils.ManipulationResult result = EditorUtils.toggleComments(content, cursor, selectionCursor);
            textarea.setValue(result.content());
            textarea.setCursorPosition(result.cursorPosition());
            textarea.setSelectionCursorPosition(result.selectionCursorPosition());
            return true;
        }
        if (pKeyCode == GLFW.GLFW_KEY_SPACE && Screen.hasControlDown()) {
            ProgramTokenContextActions.getContextAction(
                            textarea.getValue(),
                            textarea.getCursorPosition()
                    )
                    .ifPresent(Runnable::run);

            // disable the underline since it doesn't refresh when the context action closes
            textarea.rebuild(false);
            return true;
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public void resize(
            Minecraft mc,
            int x,
            int y
    ) {
        var prev = this.textarea.getValue();
        init(mc, x, y);
        super.resize(mc, x, y);
        this.textarea.setValue(prev);
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
    }

    private static boolean shouldShowLineNumbers() {
        return SFMConfig.getOrDefault(SFMConfig.CLIENT.showLineNumbers);
    }

    @Override
    protected void init() {
        super.init();
        assert this.minecraft != null;
        this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
        this.textarea = this.addRenderableWidget(new MyMultiLineEditBox());
        textarea.setValue(INITIAL_CONTENT);
        this.setInitialFocus(textarea);


        this.addRenderableWidget(new Button(
                this.width / 2 - 200,
                this.height / 2 - 100 + 195,
                16,
                20,
                Component.literal("#"),
                (button) -> this.onToggleLineNumbersButtonClicked(),
                buildTooltip(PROGRAM_EDIT_SCREEN_TOGGLE_LINE_NUMBERS_BUTTON_TOOLTIP)
        ));
        this.addRenderableWidget(new Button(
                this.width / 2 - 2 - 150,
                this.height / 2 - 100 + 195,
                200,
                20,
                CommonComponents.GUI_DONE,
                (button) -> this.saveAndClose(),
                buildTooltip(PROGRAM_EDIT_SCREEN_DONE_BUTTON_TOOLTIP)
        ));
        this.addRenderableWidget(new Button(
                this.width / 2 - 2 + 100,
                this.height / 2 - 100 + 195,
                100,
                20,
                CommonComponents.GUI_CANCEL,
                (button) -> this.onClose()
        ));
    }

    private Button.OnTooltip buildTooltip(LocalizationEntry entry) {
        return (btn, pose, mx, my) -> renderTooltip(
                pose,
                font.split(
                        entry.getComponent(),
                        Math.max(
                                width
                                / 2
                                - 43,
                                170
                        )
                ),
                mx,
                my
        );
    }

    private void onToggleLineNumbersButtonClicked() {
        SFMConfig.CLIENT.showLineNumbers.set(!shouldShowLineNumbers());
    }

    protected @NotNull ConfirmScreen getSaveConfirmScreen(Runnable onConfirm) {
        return new ConfirmScreen(
                doSave -> {
                    assert this.minecraft != null;
                    this.minecraft.popGuiLayer(); // Close confirm screen

                    //noinspection StatementWithEmptyBody
                    if (doSave) {
                        onConfirm.run();
                    } else {
                        // do nothing, continue editing
                    }
                },
                LocalizationKeys.SAVE_CHANGES_CONFIRM_SCREEN_TITLE.getComponent(),
                LocalizationKeys.SAVE_CHANGES_CONFIRM_SCREEN_MESSAGE.getComponent(),
                LocalizationKeys.SAVE_CHANGES_CONFIRM_SCREEN_YES_BUTTON.getComponent(),
                LocalizationKeys.SAVE_CHANGES_CONFIRM_SCREEN_NO_BUTTON.getComponent()
        );
    }

    protected @NotNull ConfirmScreen getExitWithoutSavingConfirmScreen() {
        return new ConfirmScreen(
                doSave -> {
                    assert this.minecraft != null;
                    this.minecraft.popGuiLayer(); // Close confirm screen

                    //noinspection StatementWithEmptyBody
                    if (doSave) {
                        closeWithoutSaving();
                    } else {
                        // do nothing; continue editing
                    }
                },
                LocalizationKeys.EXIT_WITHOUT_SAVING_CONFIRM_SCREEN_TITLE.getComponent(),
                LocalizationKeys.EXIT_WITHOUT_SAVING_CONFIRM_SCREEN_MESSAGE.getComponent(),
                LocalizationKeys.EXIT_WITHOUT_SAVING_CONFIRM_SCREEN_YES_BUTTON.getComponent(),
                LocalizationKeys.EXIT_WITHOUT_SAVING_CONFIRM_SCREEN_NO_BUTTON.getComponent()
        );
    }

    protected class MyMultiLineEditBox extends MultiLineEditBox {
        public MyMultiLineEditBox() {
            super(
                    ProgramEditScreen.this.font,
                    ProgramEditScreen.this.width / 2 - 200,
                    ProgramEditScreen.this.height / 2 - 110,
                    400,
                    200,
                    Component.literal(""),
                    Component.literal("")
            );
        }

        public int getCursorPosition() {
            return this.textField.cursor;
        }

        public void setCursorPosition(int cursor) {
            this.textField.cursor = cursor;
        }

        public int getLineNumberWidth() {
            if (shouldShowLineNumbers()) {
                return this.font.width("000");
            } else {
                return 0;
            }
        }

        @Override
        public boolean mouseClicked(
                double mx,
                double my,
                int button
        ) {
            try {
                // if mouse in bounds, translate to accommodate line numbers
                if (mx >= this.x + 1 && mx <= this.x + this.width - 1) {
                    mx -= getLineNumberWidth();
                }
                return super.mouseClicked(mx, my, button);
            } catch (Exception e) {
                SFM.LOGGER.error("Error in ProgramEditScreen.MyMultiLineEditBox.mouseClicked", e);
                return false;
            }
        }

        @Override
        public boolean mouseDragged(
                double mx,
                double my,
                int button,
                double dx,
                double dy
        ) {
            // if mouse in bounds, translate to accommodate line numbers
            if (mx >= this.x + 1 && mx <= this.x + this.width - 1) {
                mx -= getLineNumberWidth();
            }
            return super.mouseDragged(mx, my, button, dx, dy);
        }

        public int getSelectionCursorPosition() {
            return this.textField.selectCursor;
        }

        public void setSelectionCursorPosition(int cursor) {
            this.textField.selectCursor = cursor;
        }

        private void rebuild(boolean showContextActionHints) {
            lastProgram = this.textField.value();
            lastProgramWithSyntaxHighlighting = ProgramSyntaxHighlightingHelper.withSyntaxHighlighting(
                    lastProgram,
                    showContextActionHints
            );
        }

        @Override
        protected void renderContents(
                PoseStack poseStack,
                int mx,
                int my,
                float partialTicks
        ) {
            Matrix4f matrix4f = poseStack.last().pose();
            if (!lastProgram.equals(this.textField.value())) {
                rebuild(Screen.hasControlDown());
            }
            List<MutableComponent> lines = lastProgramWithSyntaxHighlighting;
            boolean isCursorVisible = this.isFocused() && this.frame / 6 % 2 == 0;
            boolean isCursorAtEndOfLine = false;
            int cursorIndex = textField.cursor();
            int lineX = this.x + this.innerPadding() + getLineNumberWidth();
            int lineY = this.y + this.innerPadding();
            int charCount = 0;
            int cursorX = 0;
            int cursorY = 0;
            MultilineTextField.StringView selectedRange = this.textField.getSelected();
            int selectionStart = selectedRange.beginIndex();
            int selectionEnd = selectedRange.endIndex();

            for (int line = 0; line < lines.size(); ++line) {
                var componentColoured = lines.get(line);
                int lineLength = componentColoured.getString().length();
                int lineHeight = this.font.lineHeight + (line == 0 ? 2 : 0);
                boolean cursorOnThisLine = isCursorVisible
                                           && cursorIndex >= charCount
                                           && cursorIndex <= charCount + lineLength;
                var buffer = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());

                if (shouldShowLineNumbers()) {
                    // Draw line number
                    String lineNumber = String.valueOf(line + 1);
                    this.font.drawInBatch(
                            lineNumber,
                            lineX - 2 - this.font.width(lineNumber),
                            lineY,
                            -1,
                            true,
                            matrix4f,
                            buffer,
                            false,
                            0,
                            LightTexture.FULL_BRIGHT
                    );
                }

                if (cursorOnThisLine) {
                    isCursorAtEndOfLine = cursorIndex == charCount + lineLength;
                    cursorY = lineY;
                    // draw text before cursor
                    cursorX = this.font.drawInBatch(
                            substring(componentColoured, 0, cursorIndex - charCount),
                            lineX,
                            lineY,
                            -1,
                            true,
                            matrix4f,
                            buffer,
                            false,
                            0,
                            LightTexture.FULL_BRIGHT
                    ) - 1;
                    // draw text after cursor
                    this.font.drawInBatch(
                            substring(componentColoured, cursorIndex - charCount, lineLength),
                            cursorX,
                            lineY,
                            -1,
                            true,
                            matrix4f,
                            buffer,
                            false,
                            0,
                            LightTexture.FULL_BRIGHT
                    );
                } else {
                    this.font.drawInBatch(
                            componentColoured,
                            lineX,
                            lineY,
                            -1,
                            true,
                            matrix4f,
                            buffer,
                            false,
                            0,
                            LightTexture.FULL_BRIGHT
                    );
                }
                buffer.endBatch();

                // Check if the selection is within the current line
                if (selectionStart <= charCount + lineLength && selectionEnd > charCount) {
                    int lineSelectionStart = Math.max(selectionStart - charCount, 0);
                    int lineSelectionEnd = Math.min(selectionEnd - charCount, lineLength);

                    int highlightStartX = this.font.width(substring(componentColoured, 0, lineSelectionStart));
                    int highlightEndX = this.font.width(substring(componentColoured, 0, lineSelectionEnd));

                    this.renderHighlight(
                            poseStack,
                            lineX + highlightStartX,
                            lineY,
                            lineX + highlightEndX,
                            lineY + lineHeight
                    );
                }

                lineY += lineHeight;
                charCount += lineLength + 1;
            }

            if (isCursorAtEndOfLine) {
                this.font.drawShadow(poseStack, "_", cursorX, cursorY, -1);
            } else {
                GuiComponent.fill(poseStack, cursorX, cursorY - 1, cursorX + 1, cursorY + 1 + 9, -1);
            }
        }

    }
}

