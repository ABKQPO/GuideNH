package com.hfstudio.guidenh.guide.internal.editor.gui;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.util.List;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.guide.internal.util.DisplayScale;

public class SceneEditorMultilineTextArea {

    public static final int PADDING = 4;
    public static final int SCROLLBAR_SIZE = 5;
    public static final int BORDER_COLOR = 0xFF53565C;
    public static final int FOCUSED_BORDER_COLOR = 0xFF7FC8FF;
    public static final int ERROR_BORDER_COLOR = 0xFFFF6767;
    public static final int BACKGROUND_COLOR = 0xA0121216;
    public static final int SCROLLBAR_TRACK_COLOR = 0x35101010;
    public static final int SCROLLBAR_THUMB_COLOR = 0xA0D8D8D8;
    public static final int SELECTION_COLOR = 0x663D89C9;
    public static final int EXTERNAL_HIGHLIGHT_COLOR = 0x4438BDF8;

    private final FontRenderer fontRenderer;
    private final SceneEditorScrollState scrollState = new SceneEditorScrollState();
    private final SceneEditorTextSelectionModel selectionModel = new SceneEditorTextSelectionModel();
    private final SceneEditorMultilineTextLayoutCache layoutCache = new SceneEditorMultilineTextLayoutCache();
    private final ClipboardAccess clipboardAccess;

    private int x;
    private int y;
    private int width;
    private int height;
    private int textViewportWidth;
    private int textViewportHeight;
    private int horizontalOffsetPixels;
    private boolean wrapEnabled;
    private boolean verticalScrollbarVisible;
    private boolean horizontalScrollbarVisible;
    private boolean focused;
    private boolean selectingWithMouse;
    private boolean draggingVerticalScrollbar;
    private boolean draggingHorizontalScrollbar;
    private int verticalScrollbarGrabOffset;
    private int horizontalScrollbarGrabOffset;
    private int externalHighlightStart;
    private int externalHighlightEnd;

    public SceneEditorMultilineTextArea(FontRenderer fontRenderer) {
        this(fontRenderer, new ClipboardAccess() {

            @Override
            public void copy(String text) {
                Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(new StringSelection(text), null);
            }

            @Override
            public String paste() {
                try {
                    Object data = Toolkit.getDefaultToolkit()
                        .getSystemClipboard()
                        .getData(DataFlavor.stringFlavor);
                    return data instanceof String ? (String) data : "";
                } catch (Exception ignored) {
                    return "";
                }
            }
        });
    }

    public SceneEditorMultilineTextArea(FontRenderer fontRenderer, ClipboardAccess clipboardAccess) {
        this.fontRenderer = fontRenderer;
        this.clipboardAccess = clipboardAccess;
        this.wrapEnabled = true;
        this.focused = false;
        this.selectingWithMouse = false;
        this.draggingVerticalScrollbar = false;
        this.draggingHorizontalScrollbar = false;
        this.verticalScrollbarGrabOffset = 0;
        this.horizontalScrollbarGrabOffset = 0;
        this.externalHighlightStart = -1;
        this.externalHighlightEnd = -1;
    }

    public void setBounds(int x, int y, int width, int height) {
        int safeWidth = Math.max(0, width);
        int safeHeight = Math.max(0, height);
        if (this.x == x && this.y == y && this.width == safeWidth && this.height == safeHeight) {
            return;
        }
        this.x = x;
        this.y = y;
        this.width = safeWidth;
        this.height = safeHeight;
        rebuildLayoutCache();
    }

    public void setText(String text) {
        String safeText = text != null ? text : "";
        if (selectionModel.getText()
            .equals(safeText)) {
            return;
        }
        selectionModel.setText(safeText);
        selectionModel.setCursorIndex(
            Math.min(
                selectionModel.getCursorIndex(),
                selectionModel.getText()
                    .length()));
        rebuildLayoutCache();
        ensureCursorVisible();
    }

    public String getText() {
        return selectionModel.getText();
    }

    public int getCursorIndex() {
        return selectionModel.getCursorIndex();
    }

    public boolean isWrapEnabled() {
        return wrapEnabled;
    }

    public void setWrapEnabled(boolean wrapEnabled) {
        if (this.wrapEnabled == wrapEnabled) {
            return;
        }
        this.wrapEnabled = wrapEnabled;
        if (wrapEnabled) {
            this.horizontalOffsetPixels = 0;
        }
        rebuildLayoutCache();
        ensureCursorVisible();
    }

    public boolean isFocused() {
        return focused;
    }

    public void setBackgroundHighlight(int startIndex, int endIndex) {
        this.externalHighlightStart = Math.max(0, startIndex);
        this.externalHighlightEnd = Math.max(this.externalHighlightStart, endIndex);
    }

    public void clearBackgroundHighlight() {
        this.externalHighlightStart = -1;
        this.externalHighlightEnd = -1;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
        if (!focused) {
            selectingWithMouse = false;
            draggingVerticalScrollbar = false;
            draggingHorizontalScrollbar = false;
        }
    }

    public boolean contains(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public void scrollWheel(int wheelDelta) {
        if (wheelDelta == 0) {
            return;
        }
        if (!wrapEnabled && horizontalScrollbarVisible && GuiScreen.isShiftKeyDown()) {
            horizontalOffsetPixels = clampHorizontalOffset(horizontalOffsetPixels - Integer.signum(wheelDelta) * 24);
            return;
        }
        scrollState.scrollPixels(-Integer.signum(wheelDelta) * 16);
    }

    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!contains(mouseX, mouseY)) {
            return false;
        }
        if (button != 0 && button != 1) {
            return true;
        }

        if (button == 0 && isInsideVerticalScrollbar(mouseX, mouseY)) {
            SceneEditorVerticalScrollbar.Thumb thumb = getVerticalScrollbarThumb();
            if (thumb != null && mouseY >= thumb.start() && mouseY < thumb.end()) {
                verticalScrollbarGrabOffset = mouseY - thumb.start();
            } else if (thumb != null) {
                verticalScrollbarGrabOffset = thumb.size() / 2;
                scrollState.setOffsetPixels(
                    SceneEditorVerticalScrollbar.offsetFromDrag(
                        mouseY,
                        verticalScrollbarGrabOffset,
                        y,
                        getVerticalScrollbarTrackLength(),
                        scrollState.getContentPixels(),
                        scrollState.getViewportPixels()));
            }
            draggingVerticalScrollbar = true;
            selectingWithMouse = false;
            return true;
        }

        if (button == 0 && isInsideHorizontalScrollbar(mouseX, mouseY)) {
            SceneEditorHorizontalScrollbar.Thumb thumb = getHorizontalScrollbarThumb();
            if (thumb != null && mouseX >= thumb.start() && mouseX < thumb.end()) {
                horizontalScrollbarGrabOffset = mouseX - thumb.start();
            } else if (thumb != null) {
                horizontalScrollbarGrabOffset = thumb.size() / 2;
                horizontalOffsetPixels = clampHorizontalOffset(
                    SceneEditorHorizontalScrollbar.offsetFromDrag(
                        mouseX,
                        horizontalScrollbarGrabOffset,
                        x,
                        getHorizontalScrollbarTrackLength(),
                        layoutCache.getContentWidthPixels(),
                        textViewportWidth));
            }
            draggingHorizontalScrollbar = true;
            selectingWithMouse = false;
            return true;
        }

        focused = true;
        int cursorIndex = getCursorIndexAt(mouseX, mouseY);
        if (button == 0) {
            selectionModel.beginSelection(cursorIndex);
            selectingWithMouse = true;
        } else {
            selectionModel.setCursorIndex(cursorIndex);
            selectingWithMouse = false;
        }
        if (button == 1) {
            selectionModel.insertText("");
        }
        rebuildLayoutCache();
        ensureCursorVisible();
        return true;
    }

    public boolean mouseDragged(int mouseX, int mouseY, int button) {
        if (button == 0 && draggingVerticalScrollbar) {
            scrollState.setOffsetPixels(
                SceneEditorVerticalScrollbar.offsetFromDrag(
                    mouseY,
                    verticalScrollbarGrabOffset,
                    y,
                    getVerticalScrollbarTrackLength(),
                    scrollState.getContentPixels(),
                    scrollState.getViewportPixels()));
            return true;
        }
        if (button == 0 && draggingHorizontalScrollbar) {
            horizontalOffsetPixels = clampHorizontalOffset(
                SceneEditorHorizontalScrollbar.offsetFromDrag(
                    mouseX,
                    horizontalScrollbarGrabOffset,
                    x,
                    getHorizontalScrollbarTrackLength(),
                    layoutCache.getContentWidthPixels(),
                    textViewportWidth));
            return true;
        }
        if (!focused || button != 0 || !selectingWithMouse) {
            return false;
        }
        selectionModel.updateSelection(getCursorIndexAt(mouseX, mouseY));
        ensureCursorVisible();
        return true;
    }

    public void mouseReleased(int button) {
        if (button == 0) {
            selectingWithMouse = false;
            draggingVerticalScrollbar = false;
            draggingHorizontalScrollbar = false;
        }
    }

    public boolean keyTyped(char typedChar, int keyCode) {
        if (!focused) {
            return false;
        }

        if (isCtrlKeyCombo(keyCode, Keyboard.KEY_A)) {
            selectionModel.selectAll();
            ensureCursorVisible();
            return true;
        }
        if (isCtrlKeyCombo(keyCode, Keyboard.KEY_C)) {
            if (selectionModel.hasSelection()) {
                clipboardAccess.copy(selectionModel.getSelectedText());
            }
            return true;
        }
        if (isCtrlKeyCombo(keyCode, Keyboard.KEY_X)) {
            if (selectionModel.hasSelection()) {
                clipboardAccess.copy(selectionModel.cutSelection());
                rebuildLayoutCache();
                ensureCursorVisible();
            }
            return true;
        }
        if (isCtrlKeyCombo(keyCode, Keyboard.KEY_V)) {
            selectionModel.insertText(clipboardAccess.paste());
            rebuildLayoutCache();
            ensureCursorVisible();
            return true;
        }

        switch (keyCode) {
            case Keyboard.KEY_RETURN:
            case Keyboard.KEY_NUMPADENTER:
                selectionModel.insertText("\n");
                rebuildLayoutCache();
                ensureCursorVisible();
                return true;
            case Keyboard.KEY_TAB:
                selectionModel.insertText("    ");
                rebuildLayoutCache();
                ensureCursorVisible();
                return true;
            case Keyboard.KEY_BACK:
                selectionModel.deleteBackward();
                rebuildLayoutCache();
                ensureCursorVisible();
                return true;
            case Keyboard.KEY_DELETE:
                selectionModel.deleteForward();
                rebuildLayoutCache();
                ensureCursorVisible();
                return true;
            case Keyboard.KEY_LEFT:
                selectionModel.moveCursor(selectionModel.getCursorIndex() - 1, GuiScreen.isShiftKeyDown());
                ensureCursorVisible();
                return true;
            case Keyboard.KEY_RIGHT:
                selectionModel.moveCursor(selectionModel.getCursorIndex() + 1, GuiScreen.isShiftKeyDown());
                ensureCursorVisible();
                return true;
            case Keyboard.KEY_UP:
                moveCursorVertical(-1, GuiScreen.isShiftKeyDown());
                return true;
            case Keyboard.KEY_DOWN:
                moveCursorVertical(1, GuiScreen.isShiftKeyDown());
                return true;
            case Keyboard.KEY_HOME:
                moveCursorToLineBoundary(true, GuiScreen.isShiftKeyDown());
                return true;
            case Keyboard.KEY_END:
                moveCursorToLineBoundary(false, GuiScreen.isShiftKeyDown());
                return true;
            case Keyboard.KEY_PRIOR:
                scrollState.scrollPixels(-Math.max(16, textViewportHeight - 24));
                return true;
            case Keyboard.KEY_NEXT:
                scrollState.scrollPixels(Math.max(16, textViewportHeight - 24));
                return true;
            default:
                break;
        }

        if (typedChar >= 32 || typedChar == ' ') {
            selectionModel.insertText(Character.toString(typedChar));
            rebuildLayoutCache();
            ensureCursorVisible();
            return true;
        }
        return false;
    }

    public void draw(boolean validationError) {
        int borderColor = validationError ? ERROR_BORDER_COLOR : (focused ? FOCUSED_BORDER_COLOR : BORDER_COLOR);
        Gui.drawRect(x - 1, y - 1, x + width + 1, y + height + 1, borderColor);
        Gui.drawRect(x, y, x + width, y + height, BACKGROUND_COLOR);

        int clipWidth = Math.max(0, getContentClipWidth());
        int clipHeight = Math.max(0, getContentClipHeight());
        int scaleFactor = DisplayScale.scaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(
            x * scaleFactor,
            DisplayScale.scaledHeight() * scaleFactor - (y + clipHeight) * scaleFactor,
            clipWidth * scaleFactor,
            clipHeight * scaleFactor);

        List<SceneEditorMultilineTextLayoutCache.VisualLine> lines = layoutCache.getVisualLines();
        int lineHeight = getLineHeight();
        int drawY = y + PADDING - scrollState.getOffsetPixels();
        for (int i = 0; i < lines.size(); i++) {
            SceneEditorMultilineTextLayoutCache.VisualLine line = lines.get(i);
            if (drawY + lineHeight >= y && drawY < y + clipHeight) {
                drawExternalHighlightForLine(line, drawY);
                drawSelectionForLine(line, drawY);
                fontRenderer.drawString(line.text(), x + PADDING - horizontalOffsetPixels, drawY, 0xF0F0F0);
            }
            drawY += lineHeight;
        }

        if (focused && shouldRenderCursor()) {
            int cursorLine = getVisualLineIndex(selectionModel.getCursorIndex());
            SceneEditorMultilineTextLayoutCache.VisualLine visualLine = lines.get(cursorLine);
            int cursorPixel = getCursorPixelOnLine(selectionModel.getCursorIndex(), visualLine);
            int cursorX = x + PADDING + cursorPixel - horizontalOffsetPixels;
            int cursorY = y + PADDING + cursorLine * lineHeight - scrollState.getOffsetPixels();
            Gui.drawRect(cursorX, cursorY, cursorX + 1, cursorY + fontRenderer.FONT_HEIGHT + 1, 0xFFFFFFFF);
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        drawVerticalScrollbar();
        drawHorizontalScrollbar();
    }

    private void rebuildLayoutCache() {
        boolean verticalVisible = false;
        boolean horizontalVisible = false;
        int resolvedTextWidth = Math.max(4, width - PADDING * 2);
        int resolvedViewportHeight = Math.max(0, height - PADDING * 2);

        for (int i = 0; i < 3; i++) {
            resolvedTextWidth = Math.max(4, width - PADDING * 2 - (verticalVisible ? SCROLLBAR_SIZE + 1 : 0));
            layoutCache
                .rebuild(selectionModel.getText(), fontRenderer, resolvedTextWidth, wrapEnabled, getLineHeight());
            horizontalVisible = !wrapEnabled && layoutCache.getContentWidthPixels() > resolvedTextWidth;
            resolvedViewportHeight = Math.max(0, height - PADDING * 2 - (horizontalVisible ? SCROLLBAR_SIZE + 1 : 0));
            boolean newVerticalVisible = layoutCache.getContentHeightPixels() > resolvedViewportHeight;
            if (newVerticalVisible == verticalVisible) {
                verticalVisible = newVerticalVisible;
                break;
            }
            verticalVisible = newVerticalVisible;
        }

        this.textViewportWidth = resolvedTextWidth;
        this.textViewportHeight = resolvedViewportHeight;
        this.verticalScrollbarVisible = verticalVisible;
        this.horizontalScrollbarVisible = horizontalVisible;
        if (wrapEnabled) {
            horizontalOffsetPixels = 0;
        } else {
            horizontalOffsetPixels = clampHorizontalOffset(horizontalOffsetPixels);
        }
        scrollState.setViewportPixels(textViewportHeight);
        scrollState.setContentPixels(layoutCache.getContentHeightPixels());
    }

    private void drawSelectionForLine(SceneEditorMultilineTextLayoutCache.VisualLine line, int drawY) {
        if (!selectionModel.hasSelection()) {
            return;
        }
        int selectionStart = selectionModel.getSelectionStart();
        int selectionEnd = selectionModel.getSelectionEnd();
        boolean spansLineBreak = line.endsWithNewline() && selectionStart <= line.endIndex()
            && selectionEnd > line.endIndex();
        int highlightStart = Math.max(selectionStart, line.startIndex());
        int highlightEnd = Math.min(selectionEnd, line.endIndex());
        if (highlightEnd <= highlightStart && !spansLineBreak) {
            return;
        }

        String beforeSelection = line.text()
            .substring(0, Math.max(0, highlightStart - line.startIndex()));
        String selectedText = line.text()
            .substring(
                Math.max(0, highlightStart - line.startIndex()),
                Math.max(0, Math.min(highlightEnd, line.endIndex()) - line.startIndex()));
        int selectionX = x + PADDING + fontRenderer.getStringWidth(beforeSelection) - horizontalOffsetPixels;
        int selectionWidth = fontRenderer.getStringWidth(selectedText);
        if (selectionWidth <= 0 && spansLineBreak) {
            selectionWidth = 2;
        }
        if (selectionWidth > 0) {
            Gui.drawRect(
                selectionX,
                drawY - 1,
                selectionX + selectionWidth,
                drawY + fontRenderer.FONT_HEIGHT + 1,
                SELECTION_COLOR);
        }
    }

    private void drawExternalHighlightForLine(SceneEditorMultilineTextLayoutCache.VisualLine line, int drawY) {
        if (externalHighlightStart < 0 || externalHighlightEnd <= externalHighlightStart) {
            return;
        }
        boolean spansLineBreak = line.endsWithNewline() && externalHighlightStart <= line.endIndex()
            && externalHighlightEnd > line.endIndex();
        int highlightStart = Math.max(externalHighlightStart, line.startIndex());
        int highlightEnd = Math.min(externalHighlightEnd, line.endIndex());
        if (highlightEnd <= highlightStart && !spansLineBreak) {
            return;
        }

        String beforeHighlight = line.text()
            .substring(0, Math.max(0, highlightStart - line.startIndex()));
        String highlightedText = line.text()
            .substring(
                Math.max(0, highlightStart - line.startIndex()),
                Math.max(0, Math.min(highlightEnd, line.endIndex()) - line.startIndex()));
        int highlightX = x + PADDING + fontRenderer.getStringWidth(beforeHighlight) - horizontalOffsetPixels;
        int highlightWidth = fontRenderer.getStringWidth(highlightedText);
        if (highlightWidth <= 0 && spansLineBreak) {
            highlightWidth = 2;
        }
        if (highlightWidth > 0) {
            Gui.drawRect(
                highlightX,
                drawY - 1,
                highlightX + highlightWidth,
                drawY + fontRenderer.FONT_HEIGHT + 1,
                EXTERNAL_HIGHLIGHT_COLOR);
        }
    }

    private void drawVerticalScrollbar() {
        if (!verticalScrollbarVisible) {
            return;
        }
        int barLeft = x + width - SCROLLBAR_SIZE;
        int barBottom = y + getVerticalScrollbarTrackLength();
        Gui.drawRect(barLeft, y, x + width, barBottom, SCROLLBAR_TRACK_COLOR);
        SceneEditorVerticalScrollbar.Thumb thumb = getVerticalScrollbarThumb();
        if (thumb != null) {
            Gui.drawRect(barLeft, thumb.start(), x + width, thumb.end(), SCROLLBAR_THUMB_COLOR);
        }
    }

    private void drawHorizontalScrollbar() {
        if (!horizontalScrollbarVisible) {
            return;
        }
        int barTop = y + height - SCROLLBAR_SIZE;
        int barRight = x + getHorizontalScrollbarTrackLength();
        Gui.drawRect(x, barTop, barRight, y + height, SCROLLBAR_TRACK_COLOR);
        SceneEditorHorizontalScrollbar.Thumb thumb = getHorizontalScrollbarThumb();
        if (thumb != null) {
            Gui.drawRect(thumb.start(), barTop, thumb.end(), y + height, SCROLLBAR_THUMB_COLOR);
        }
    }

    private boolean isInsideVerticalScrollbar(int mouseX, int mouseY) {
        return verticalScrollbarVisible && mouseX >= x + width - SCROLLBAR_SIZE
            && mouseX < x + width
            && mouseY >= y
            && mouseY < y + getVerticalScrollbarTrackLength();
    }

    private boolean isInsideHorizontalScrollbar(int mouseX, int mouseY) {
        return horizontalScrollbarVisible && mouseX >= x
            && mouseX < x + getHorizontalScrollbarTrackLength()
            && mouseY >= y + height - SCROLLBAR_SIZE
            && mouseY < y + height;
    }

    private SceneEditorVerticalScrollbar.Thumb getVerticalScrollbarThumb() {
        if (!verticalScrollbarVisible) {
            return null;
        }
        return SceneEditorVerticalScrollbar.computeThumb(
            y,
            getVerticalScrollbarTrackLength(),
            scrollState.getContentPixels(),
            scrollState.getViewportPixels(),
            scrollState.getOffsetPixels());
    }

    private SceneEditorHorizontalScrollbar.Thumb getHorizontalScrollbarThumb() {
        if (!horizontalScrollbarVisible) {
            return null;
        }
        return SceneEditorHorizontalScrollbar.computeThumb(
            x,
            getHorizontalScrollbarTrackLength(),
            layoutCache.getContentWidthPixels(),
            textViewportWidth,
            horizontalOffsetPixels);
    }

    private void moveCursorVertical(int direction, boolean keepSelection) {
        List<SceneEditorMultilineTextLayoutCache.VisualLine> lines = layoutCache.getVisualLines();
        if (lines.isEmpty()) {
            return;
        }
        int currentLine = getVisualLineIndex(selectionModel.getCursorIndex());
        int nextLine = currentLine + direction;
        if (nextLine < 0 || nextLine >= lines.size()) {
            return;
        }
        int currentPixel = getCursorPixelOnLine(selectionModel.getCursorIndex(), lines.get(currentLine));
        int nextIndex = getCursorIndexAtPixel(lines.get(nextLine), currentPixel);
        selectionModel.moveCursor(nextIndex, keepSelection);
        ensureCursorVisible();
    }

    private void moveCursorToLineBoundary(boolean start, boolean keepSelection) {
        List<SceneEditorMultilineTextLayoutCache.VisualLine> lines = layoutCache.getVisualLines();
        if (lines.isEmpty()) {
            return;
        }
        SceneEditorMultilineTextLayoutCache.VisualLine line = lines
            .get(getVisualLineIndex(selectionModel.getCursorIndex()));
        selectionModel.moveCursor(start ? line.startIndex() : line.endIndex(), keepSelection);
        ensureCursorVisible();
    }

    private int getCursorIndexAt(int mouseX, int mouseY) {
        List<SceneEditorMultilineTextLayoutCache.VisualLine> lines = layoutCache.getVisualLines();
        if (lines.isEmpty()) {
            return 0;
        }
        int localY = mouseY - y - PADDING + scrollState.getOffsetPixels();
        int lineIndex = localY <= 0 ? 0 : localY / getLineHeight();
        if (lineIndex < 0) {
            lineIndex = 0;
        }
        if (lineIndex >= lines.size()) {
            lineIndex = lines.size() - 1;
        }
        int localX = Math.max(0, mouseX - x - PADDING + horizontalOffsetPixels);
        return getCursorIndexAtPixel(lines.get(lineIndex), localX);
    }

    private void ensureCursorVisible() {
        List<SceneEditorMultilineTextLayoutCache.VisualLine> lines = layoutCache.getVisualLines();
        if (lines.isEmpty()) {
            return;
        }

        int lineIndex = getVisualLineIndex(selectionModel.getCursorIndex());
        int lineTop = lineIndex * getLineHeight();
        int lineBottom = lineTop + getLineHeight();
        if (lineTop < scrollState.getOffsetPixels()) {
            scrollState.setOffsetPixels(lineTop);
        } else {
            int visibleBottom = scrollState.getOffsetPixels() + scrollState.getViewportPixels();
            if (lineBottom > visibleBottom) {
                scrollState.setOffsetPixels(lineBottom - scrollState.getViewportPixels());
            }
        }

        if (wrapEnabled) {
            horizontalOffsetPixels = 0;
            return;
        }
        int cursorPixel = getCursorPixelOnLine(selectionModel.getCursorIndex(), lines.get(lineIndex));
        if (cursorPixel < horizontalOffsetPixels) {
            horizontalOffsetPixels = clampHorizontalOffset(cursorPixel);
            return;
        }
        if (cursorPixel > horizontalOffsetPixels + textViewportWidth - 1) {
            horizontalOffsetPixels = clampHorizontalOffset(cursorPixel - textViewportWidth + 1);
        }
    }

    private int getVisualLineIndex(int cursorIndex) {
        List<SceneEditorMultilineTextLayoutCache.VisualLine> lines = layoutCache.getVisualLines();
        for (int i = 0; i < lines.size(); i++) {
            SceneEditorMultilineTextLayoutCache.VisualLine line = lines.get(i);
            if (cursorIndex < line.startIndex()) {
                return Math.max(0, i - 1);
            }
            if (cursorIndex <= line.endIndex()) {
                return i;
            }
        }
        return Math.max(0, lines.size() - 1);
    }

    private int getCursorPixelOnLine(int cursorIndex, SceneEditorMultilineTextLayoutCache.VisualLine line) {
        int charCount = Math.max(
            0,
            Math.min(
                cursorIndex - line.startIndex(),
                line.text()
                    .length()));
        return fontRenderer.getStringWidth(
            line.text()
                .substring(0, charCount));
    }

    private int getCursorIndexAtPixel(SceneEditorMultilineTextLayoutCache.VisualLine line, int localX) {
        int column = 0;
        for (int i = 1; i <= line.text()
            .length(); i++) {
            if (fontRenderer.getStringWidth(
                line.text()
                    .substring(0, i))
                > localX) {
                break;
            }
            column = i;
        }
        return Math.min(
            line.startIndex() + column,
            selectionModel.getText()
                .length());
    }

    private int clampHorizontalOffset(int requestedOffset) {
        int maxOffset = Math.max(0, layoutCache.getContentWidthPixels() - textViewportWidth);
        if (requestedOffset < 0) {
            return 0;
        }
        return requestedOffset > maxOffset ? maxOffset : requestedOffset;
    }

    private int getContentClipWidth() {
        return width - (verticalScrollbarVisible ? SCROLLBAR_SIZE : 0);
    }

    private int getContentClipHeight() {
        return height - (horizontalScrollbarVisible ? SCROLLBAR_SIZE : 0);
    }

    private int getVerticalScrollbarTrackLength() {
        return Math.max(0, height - (horizontalScrollbarVisible ? SCROLLBAR_SIZE : 0));
    }

    private int getHorizontalScrollbarTrackLength() {
        return Math.max(0, width - (verticalScrollbarVisible ? SCROLLBAR_SIZE : 0));
    }

    private int getLineHeight() {
        return fontRenderer.FONT_HEIGHT + 2;
    }

    private boolean shouldRenderCursor() {
        return (System.currentTimeMillis() / 500L) % 2L == 0L;
    }

    public static boolean isCtrlKeyCombo(int keyCode, int expectedKeyCode) {
        return keyCode == expectedKeyCode
            && (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL));
    }

    public interface ClipboardAccess {

        void copy(String text);

        String paste();
    }
}
