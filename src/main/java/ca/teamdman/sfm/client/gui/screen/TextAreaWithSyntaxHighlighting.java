package ca.teamdman.sfm.client.gui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractScrollWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class TextAreaWithSyntaxHighlighting extends AbstractScrollWidget {
    private final Font font;

    private int cachedLineCount;
    private int linePadding = 2;
    private int cursorPosition;
    private int selectionCursorPosition;

    public TextAreaWithSyntaxHighlighting(
            Font font,
            int x,
            int y,
            int width,
            int height,
            Component initialContent
    ) {
        super(x, y, width, height, initialContent);
        this.font = font;
    }

    public String getValue() {
        return this.getMessage().getString();
    }

    public int getCursorPosition() {
        return 0;
    }

    @Override
    public boolean mouseClicked(
            double pMouseX,
            double pMouseY,
            int pButton
    ) {
        if (super.mouseClicked(pMouseX, pMouseY, pButton)) {
            // handle parent scrollbar logic
            return true;
        }
        if (this.withinContentAreaPoint(pMouseX, pMouseY)) {
            if (pButton == 0) {
                this.setCursorPosition(this.getCursorLineIndex(pMouseX, pMouseY));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(
            int pKeyCode,
            int pScanCode,
            int pModifiers
    ) {
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public boolean charTyped(
            char pCodePoint,
            int pModifiers
    ) {
        if (this.visible && this.isFocused()) {
            //TODO: verify that non-latin characters are allowed
            this.insertText(Character.toString(pCodePoint));
        }
        return false;
    }

    private void insertText(String string) {
        int cursorPosition = this.getCursorPosition();
        String currentText = this.getValue();
        String newText = currentText.substring(0, cursorPosition) + string + currentText.substring(cursorPosition);
        this.setValue(newText);
        this.setCursorPosition(cursorPosition + string.length());
    }

    private int getCursorLineIndex(
            double pMouseX,
            double pMouseY
    ) {
        return 0;
    }

    public int getSelectionCursorPosition() {
        return 0;
    }

    public void setValue(String content) {
        this.setMessage(Component.nullToEmpty(content));
        this.updateCachedLineCount();
    }

    public void setCursorPosition(int i) {
        this.cursorPosition = i;
    }

    public void setSelectionCursorPosition(int i) {
        this.selectionCursorPosition = i;
    }

    public void rebuild(boolean hasModifierKeyDown) {
        //todo: handle underlines for action-available text
        this.updateCachedLineCount();
    }

    private void updateCachedLineCount() {
        this.cachedLineCount = this.font.split(this.getMessage(), this.width).size();
    }

    protected int getLineCount() {
        return cachedLineCount;
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(
                NarratedElementType.TITLE,
                Component.translatable("narration.edit_box", this.getMessage().getString())
        );
    }

    @Override
    protected int getInnerHeight() {
        return this.getLineHeight() * this.getLineCount();
    }

    protected int getLineHeight() {
        return this.font.lineHeight + this.linePadding;
    }

    protected int getDisplayedLineCount() {
        return Math.min(this.getLineCount(), this.height / this.font.lineHeight);
    }

    @Override
    protected boolean scrollbarVisible() {
        return this.getLineCount() > this.getDisplayedLineCount();
    }

    @Override
    protected double scrollRate() {
        return 9.0D / 2.0D;
    }

    @Override
    protected void renderContents(
            PoseStack poseStack,
            int i,
            int i1,
            float v
    ) {
        this.font.drawShadow(
                poseStack,
                this.getMessage(),
                (float) this.x,
                (float) this.y,
                0xFFFFFF
        );
    }
}