package ca.teamdman.sfm.client.gui.screen;

import ca.teamdman.sfm.client.ProgramTokenContextActions;
import ca.teamdman.sfm.client.gui.EditorUtils;
import ca.teamdman.sfm.common.config.SFMConfig;
import ca.teamdman.sfm.common.localization.LocalizationEntry;
import ca.teamdman.sfm.common.localization.LocalizationKeys;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
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

public class ProgramEditScreenV2 extends Screen {
    protected final String INITIAL_CONTENT;
    protected final Consumer<String> SAVE_CALLBACK;
    @SuppressWarnings("NotNullFieldNotInitialized")
    protected TextAreaWithSyntaxHighlighting textArea;
    protected String lastProgram = "";
    protected List<MutableComponent> lastProgramWithSyntaxHighlighting = Collections.emptyList();

    public ProgramEditScreenV2(
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
        component.visit(
                (style, content) -> {
                    int contentStart = Math.max(start - seen.get(), 0);
                    int contentEnd = Math.min(end - seen.get(), content.length());

                    if (contentStart < contentEnd) {
                        rtn.append(Component.literal(content.substring(contentStart, contentEnd)).withStyle(style));
                    }
                    seen.addAndGet(content.length());
                    return Optional.empty();
                }, Style.EMPTY
        );
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
        SAVE_CALLBACK.accept(textArea.getValue());

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
        if (!INITIAL_CONTENT.equals(textArea.getValue())) {
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
            textArea.rebuild(Screen.hasControlDown());
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
        textArea.setScrollAmount(0d);
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
            String content = textArea.getValue();
            int cursor = textArea.getCursorPosition();
            int selectionCursor = textArea.getSelectionCursorPosition();
            EditorUtils.ManipulationResult result;
            if (Screen.hasShiftDown()) { // de-indent
                result = EditorUtils.deindent(content, cursor, selectionCursor);
            } else { // indent
                result = EditorUtils.indent(content, cursor, selectionCursor);
            }
            textArea.setValue(result.content());
            textArea.setCursorPosition(result.cursorPosition());
            textArea.setSelectionCursorPosition(result.selectionCursorPosition());
            return true;
        }
        if (pKeyCode == GLFW.GLFW_KEY_LEFT_CONTROL || pKeyCode == GLFW.GLFW_KEY_RIGHT_CONTROL) {
            // if control pressed => update syntax highlighting
            textArea.rebuild(Screen.hasControlDown());
            return true;
        }
        if (pKeyCode == GLFW.GLFW_KEY_SLASH && Screen.hasControlDown()) {
            // toggle line comments for selected lines
            String content = textArea.getValue();
            int cursor = textArea.getCursorPosition();
            int selectionCursor = textArea.getSelectionCursorPosition();
            EditorUtils.ManipulationResult result = EditorUtils.toggleComments(content, cursor, selectionCursor);
            textArea.setValue(result.content());
            textArea.setCursorPosition(result.cursorPosition());
            textArea.setSelectionCursorPosition(result.selectionCursorPosition());
            return true;
        }
        if (pKeyCode == GLFW.GLFW_KEY_SPACE && Screen.hasControlDown()) {
            ProgramTokenContextActions.getContextAction(
                            textArea.getValue(),
                            textArea.getCursorPosition()
                    )
                    .ifPresent(Runnable::run);

            // disable the underline since it doesn't refresh when the context action closes
            textArea.rebuild(false);
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
        var prev = this.textArea.getValue();
        init(mc, x, y);
        super.resize(mc, x, y);
        this.textArea.setValue(prev);
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
        this.textArea = this.addRenderableWidget(new TextAreaWithSyntaxHighlighting(
                this.font,
                this.width / 2 - 200,
                this.width / 2 - 110,
                400,
                200,
                Component.literal(INITIAL_CONTENT)
        ));
//        textArea.setValue(INITIAL_CONTENT);
        this.setInitialFocus(textArea);


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
}

