package com.hfstudio.guidenh.guide.internal.editor.gui;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.guide.compiler.GuideMarkdownOptions;
import com.hfstudio.guidenh.guide.internal.markdown.MdAstToMdxConverter;
import com.hfstudio.guidenh.guide.internal.util.DisplayScale;
import com.hfstudio.guidenh.guide.internal.util.SmoothFloatState;
import com.hfstudio.guidenh.libs.mdast.MdAst;
import com.hfstudio.guidenh.libs.mdast.model.MdAstDefinition;
import com.hfstudio.guidenh.libs.mdast.model.MdAstList;
import com.hfstudio.guidenh.libs.mdast.model.MdAstListContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstListItem;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstRoot;
import com.hfstudio.guidenh.libs.unist.UnistNode;
import com.hfstudio.guidenh.libs.unist.UnistPosition;

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
    public static final int SYNTAX_WARNING_COLOR = 0xFFFF6767;
    public static final long IME_DUPLICATE_WINDOW_MILLIS = 250L;

    private final FontRenderer fontRenderer;
    private final SceneEditorScrollState scrollState = new SceneEditorScrollState();
    private final SceneEditorTextSelectionModel selectionModel = new SceneEditorTextSelectionModel();
    private final SceneEditorMultilineTextLayoutCache layoutCache = new SceneEditorMultilineTextLayoutCache();
    private final ClipboardAccess clipboardAccess;
    private final GuiTextField imeFocusProxy;

    private int x;
    private int y;
    private int width;
    private int height;
    private int textViewportWidth;
    private int textViewportHeight;
    private int horizontalOffsetPixels;
    private final SmoothFloatState visualVerticalOffsetPixels = new SmoothFloatState();
    private final SmoothFloatState visualHorizontalOffsetPixels = new SmoothFloatState();
    private boolean wrapEnabled;
    private boolean verticalScrollbarVisible;
    private boolean horizontalScrollbarVisible;
    private boolean focused;
    private boolean selectingWithMouse;
    private boolean panningWithMiddleMouse;
    private boolean draggingVerticalScrollbar;
    private boolean draggingHorizontalScrollbar;
    private int panLastMouseX;
    private int panLastMouseY;
    private int verticalScrollbarGrabOffset;
    private int horizontalScrollbarGrabOffset;
    private int externalHighlightStart;
    private int externalHighlightEnd;
    private int syntaxWarningStart;
    private int syntaxWarningEnd;
    private int pendingImePhysicalDuplicateChar;
    private int recentPhysicalAsciiChar;
    private long recentPhysicalAsciiAtMillis;

    // Double-click word selection
    private long lastClickTimeMillis;
    private static final long DOUBLE_CLICK_WINDOW_MS = 400;
    @Nullable
    private DoubleClickHandler doubleClickHandler;

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
        this.imeFocusProxy = new GuiTextField(fontRenderer, 0, 0, 1, Math.max(1, fontRenderer.FONT_HEIGHT + 2));
        this.imeFocusProxy.setEnableBackgroundDrawing(false);
        this.imeFocusProxy.setMaxStringLength(1);
        this.wrapEnabled = true;
        this.focused = false;
        this.selectingWithMouse = false;
        this.panningWithMiddleMouse = false;
        this.draggingVerticalScrollbar = false;
        this.draggingHorizontalScrollbar = false;
        this.verticalScrollbarGrabOffset = 0;
        this.horizontalScrollbarGrabOffset = 0;
        this.externalHighlightStart = -1;
        this.externalHighlightEnd = -1;
        this.syntaxWarningStart = -1;
        this.syntaxWarningEnd = -1;
        this.pendingImePhysicalDuplicateChar = -1;
        this.recentPhysicalAsciiChar = -1;
        this.recentPhysicalAsciiAtMillis = 0L;
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
        syncImeFocusProxy();
    }

    public void setText(String text) {
        String safeText = normalizeLineEndings(text);
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
        syncImeFocusProxy();
    }

    public String getText() {
        return selectionModel.getText();
    }

    public int getCursorIndex() {
        return selectionModel.getCursorIndex();
    }

    public boolean hasSelection() {
        return selectionModel.hasSelection();
    }

    public int getSelectionStart() {
        return selectionModel.getSelectionStart();
    }

    public int getSelectionEnd() {
        return selectionModel.getSelectionEnd();
    }

    public String getSelectedText() {
        return selectionModel.getSelectedText();
    }

    public void selectAll() {
        selectionModel.selectAll();
        ensureCursorVisible();
        syncImeFocusProxy();
    }

    public void copySelection() {
        if (!selectionModel.hasSelection()) {
            return;
        }
        clipboardAccess.copy(selectionModel.getSelectedText());
    }

    public boolean cutSelection() {
        if (!selectionModel.hasSelection()) {
            return false;
        }
        clipboardAccess.copy(selectionModel.cutSelection());
        rebuildLayoutCache();
        ensureCursorVisible();
        syncImeFocusProxy();
        return true;
    }

    public boolean pasteClipboard() {
        selectionModel.insertText(normalizeLineEndings(clipboardAccess.paste()));
        rebuildLayoutCache();
        ensureCursorVisible();
        syncImeFocusProxy();
        return true;
    }

    public void applyEdit(String text, int selectionStart, int selectionEnd) {
        selectionModel.setText(normalizeLineEndings(text));
        selectionModel.setSelection(selectionStart, selectionEnd);
        rebuildLayoutCache();
        ensureCursorVisible();
        syncImeFocusProxy();
    }

    public void insertAtSelection(String text) {
        selectionModel.insertText(normalizeLineEndings(text));
        rebuildLayoutCache();
        ensureCursorVisible();
        syncImeFocusProxy();
    }

    public void insertAtMouse(String text, int mouseX, int mouseY) {
        int cursorIndex = getCursorIndexAt(mouseX, mouseY);
        selectionModel.setSelection(cursorIndex, cursorIndex);
        selectionModel.insertText(text);
        rebuildLayoutCache();
        ensureCursorVisible();
        syncImeFocusProxy();
    }

    public boolean isRichTagInsertionSafeAtMouse(int mouseX, int mouseY) {
        int cursorIndex = getCursorIndexAt(mouseX, mouseY);
        return isRichTagInsertionSafeAt(cursorIndex);
    }

    private boolean isRichTagInsertionSafeAt(int cursorIndex) {
        String text = selectionModel.getText();
        int index = clamp(cursorIndex, 0, text.length());
        if (isInsideOpenTag(text, index)) {
            return false;
        }
        return hasTagBoundaryBefore(text, index) && hasTagBoundaryAfter(text, index);
    }

    private boolean isInsideOpenTag(String text, int cursorIndex) {
        boolean inQuote = false;
        char quote = 0;
        boolean openTag = false;
        for (int i = 0; i < cursorIndex; i++) {
            char c = text.charAt(i);
            if (inQuote) {
                if (c == quote && !isEscaped(text, i)) {
                    inQuote = false;
                }
                continue;
            }
            if (c == '"' || c == '\'') {
                if (openTag) {
                    quote = c;
                    inQuote = true;
                }
                continue;
            }
            if (c == '>') {
                openTag = false;
                continue;
            }
            if (c == '<') {
                openTag = true;
            }
        }
        return openTag || inQuote;
    }

    private boolean hasTagBoundaryBefore(String text, int cursorIndex) {
        int index = cursorIndex - 1;
        while (index >= 0 && Character.isWhitespace(text.charAt(index))) {
            index--;
        }
        if (index < 0) {
            return true;
        }
        char c = text.charAt(index);
        return c == '>' || c == ')' || c == ']' || c == '}' || c == '`' || c == '.' || c == ',' || c == ':' || c == ';';
    }

    private boolean hasTagBoundaryAfter(String text, int cursorIndex) {
        int index = cursorIndex;
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        if (index >= text.length()) {
            return true;
        }
        char c = text.charAt(index);
        return c == '<' || c == '(' || c == '[' || c == '{' || c == '`' || c == '.' || c == ',' || c == ':' || c == ';';
    }

    private boolean isEscaped(String text, int index) {
        int count = 0;
        for (int i = index - 1; i >= 0 && text.charAt(i) == '\\'; i--) {
            count++;
        }
        return count % 2 == 1;
    }

    public float getVerticalScrollFraction() {
        int maxOffset = Math.max(0, scrollState.getContentPixels() - scrollState.getViewportPixels());
        if (maxOffset <= 0) {
            return 0f;
        }
        return scrollState.getOffsetPixels() / (float) maxOffset;
    }

    public void setVerticalScrollFraction(float fraction) {
        int maxOffset = Math.max(0, scrollState.getContentPixels() - scrollState.getViewportPixels());
        int offset = Math.round(Math.clamp(fraction, 0f, 1f) * maxOffset);
        scrollState.setOffsetPixels(offset);
        snapVisualOffsetsToTarget();
        syncImeFocusProxy();
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
        syncImeFocusProxy();
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

    public void setSyntaxWarning(int startIndex, int endIndex) {
        this.syntaxWarningStart = Math.max(0, startIndex);
        this.syntaxWarningEnd = Math.max(this.syntaxWarningStart, endIndex);
    }

    public void clearSyntaxWarning() {
        this.syntaxWarningStart = -1;
        this.syntaxWarningEnd = -1;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
        if (focused) {
            syncImeFocusProxy();
        }
        imeFocusProxy.setFocused(focused);
        if (!focused) {
            selectingWithMouse = false;
            panningWithMiddleMouse = false;
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
            syncImeFocusProxy();
            return;
        }
        scrollState.scrollPixels(-Integer.signum(wheelDelta) * 16);
        syncImeFocusProxy();
    }

    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!contains(mouseX, mouseY)) {
            return false;
        }
        if (button == 2) {
            setFocused(true);
            panningWithMiddleMouse = true;
            selectingWithMouse = false;
            draggingVerticalScrollbar = false;
            draggingHorizontalScrollbar = false;
            panLastMouseX = mouseX;
            panLastMouseY = mouseY;
            return true;
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

        setFocused(true);
        int cursorIndex = getCursorIndexAt(mouseX, mouseY);
        if (button == 0) {
            selectionModel.beginSelection(cursorIndex);
            long now = System.currentTimeMillis();
            long elapsed = now - lastClickTimeMillis;
            lastClickTimeMillis = now;
            if (elapsed < DOUBLE_CLICK_WINDOW_MS && doubleClickHandler != null) {
                doubleClickHandler.onDoubleClick(selectionModel.getCursorIndex());
                return true;
            }
            selectingWithMouse = true;
        } else {
            selectionModel.setCursorIndex(cursorIndex);
            selectingWithMouse = false;
        }
        rebuildLayoutCache();
        syncImeFocusProxy();
        ensureCursorVisible();
        syncImeFocusProxy();
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
            syncImeFocusProxy();
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
            syncImeFocusProxy();
            return true;
        }
        if (button == 2 && panningWithMiddleMouse) {
            int deltaX = mouseX - panLastMouseX;
            int deltaY = mouseY - panLastMouseY;
            panLastMouseX = mouseX;
            panLastMouseY = mouseY;
            horizontalOffsetPixels = clampHorizontalOffset(horizontalOffsetPixels - deltaX);
            scrollState.scrollPixels(-deltaY);
            syncImeFocusProxy();
            return true;
        }
        if (!focused || button != 0 || !selectingWithMouse) {
            return false;
        }
        selectionModel.updateSelection(getCursorIndexAt(mouseX, mouseY));
        ensureCursorVisible();
        syncImeFocusProxy();
        return true;
    }

    public void mouseReleased(int button) {
        if (button == 0) {
            selectingWithMouse = false;
            draggingVerticalScrollbar = false;
            draggingHorizontalScrollbar = false;
        }
        if (button == 2) {
            panningWithMiddleMouse = false;
        }
    }

    public boolean keyTyped(char typedChar, int keyCode) {
        if (!focused) {
            return false;
        }

        boolean handled = handleKeyTyped(typedChar, keyCode);
        if (handled) {
            syncImeFocusProxy();
        }
        return handled;
    }

    private boolean handleKeyTyped(char typedChar, int keyCode) {
        if (shouldConsumeImePhysicalDuplicate(typedChar, keyCode)) {
            return true;
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
            selectionModel.insertText(normalizeLineEndings(clipboardAccess.paste()));
            rebuildLayoutCache();
            ensureCursorVisible();
            return true;
        }

        switch (keyCode) {
            case Keyboard.KEY_RETURN:
            case Keyboard.KEY_NUMPADENTER:
                applySmartNewline();
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
            if (shouldConsumeImeCommittedDuplicate(typedChar, keyCode)) {
                return true;
            }
            selectionModel.insertText(Character.toString(typedChar));
            rememberInsertedAsciiCharacter(typedChar, keyCode);
            rebuildLayoutCache();
            ensureCursorVisible();
            return true;
        }
        return false;
    }

    private boolean shouldConsumeImePhysicalDuplicate(char typedChar, int keyCode) {
        if (pendingImePhysicalDuplicateChar < 0) {
            return false;
        }
        if (keyCode == Keyboard.KEY_NONE || typedChar < 32) {
            return false;
        }
        int expectedChar = pendingImePhysicalDuplicateChar;
        pendingImePhysicalDuplicateChar = -1;
        return typedChar == expectedChar;
    }

    private boolean shouldConsumeImeCommittedDuplicate(char typedChar, int keyCode) {
        if (keyCode != Keyboard.KEY_NONE || typedChar < 32 || typedChar >= 128 || recentPhysicalAsciiChar < 0) {
            return false;
        }
        boolean duplicate = typedChar == recentPhysicalAsciiChar
            && System.currentTimeMillis() - recentPhysicalAsciiAtMillis <= IME_DUPLICATE_WINDOW_MILLIS;
        if (duplicate) {
            recentPhysicalAsciiChar = -1;
            pendingImePhysicalDuplicateChar = -1;
        }
        return duplicate;
    }

    private void rememberInsertedAsciiCharacter(char typedChar, int keyCode) {
        if (typedChar < 32 || typedChar >= 128) {
            pendingImePhysicalDuplicateChar = -1;
            recentPhysicalAsciiChar = -1;
            return;
        }
        if (keyCode == Keyboard.KEY_NONE) {
            pendingImePhysicalDuplicateChar = typedChar;
            return;
        }
        pendingImePhysicalDuplicateChar = -1;
        recentPhysicalAsciiChar = typedChar;
        recentPhysicalAsciiAtMillis = System.currentTimeMillis();
    }

    private void applySmartNewline() {
        String text = selectionModel.getText();
        int cursor = selectionModel.getCursorIndex();

        // 1. Try AST-based list continuation
        MdAstRoot root = MdAst.fromMarkdown(text, GuideMarkdownOptions.runtime());
        MdAstToMdxConverter.convert(root, Collections.<String, MdAstDefinition>emptyMap());
        MdAstListItem item = findEnclosingListItem(root, cursor);
        if (item != null) {
            int lineStart = findLineStart(text, cursor - 1);
            if (isListItemContentEmpty(item, text)) {
                selectionModel.setSelection(lineStart, cursor);
                selectionModel.insertText(leadingWhitespace(text.substring(lineStart, findLineEnd(text, cursor))));
                return;
            }
            String nextMarker = resolveNextListMarker(text, item, root);
            if (nextMarker != null) {
                selectionModel.insertText("\n" + nextMarker);
                return;
            }
        }

        // 2. Fallback: manual list marker continuation (for YAML and non-Markdown lists)
        int lineStart = findLineStart(text, Math.max(0, cursor - 1));
        int lineEnd = findLineEnd(text, cursor);
        String line = text.substring(lineStart, lineEnd);
        String indent = leadingWhitespace(line);
        String trimmed = line.trim();
        String manualMarker = resolveManualListMarker(trimmed);
        if (manualMarker != null) {
            int markerLen = manualMarker.length();
            if (trimmed.length() <= markerLen || (trimmed.length() > markerLen && trimmed.substring(markerLen)
                .trim()
                .isEmpty())) {
                // Empty list item: remove marker
                selectionModel.setSelection(lineStart, cursor);
                selectionModel.insertText(indent);
                return;
            }
            selectionModel.insertText("\n" + indent + manualMarker);
            return;
        }
        if (trimmed.isEmpty()) {
            // Blank line: move cursor to next line instead of inserting another blank line.
            int nextLineStart = findLineEnd(text, cursor) + 1;
            if (nextLineStart <= text.length()) {
                selectionModel.setSelection(nextLineStart, nextLineStart);
            }
            return;
        }
        selectionModel.insertText("\n" + indent);
    }

    @Nullable
    private static String resolveManualListMarker(String trimmed) {
        if (trimmed.isEmpty()) return null;
        char first = trimmed.charAt(0);
        if (first == '-' || first == '*' || first == '+') {
            if (trimmed.length() >= 2 && trimmed.charAt(1) == ' ') return "- ";
            if (trimmed.length() == 1) return "- "; // bare marker, empty item
        }
        // Ordered list: number. or number)
        int i = 0;
        while (i < trimmed.length() && Character.isDigit(trimmed.charAt(i))) i++;
        if (i > 0 && i + 1 < trimmed.length()
            && (trimmed.charAt(i) == '.' || trimmed.charAt(i) == ')')
            && trimmed.charAt(i + 1) == ' ') {
            int num = Integer.parseInt(trimmed.substring(0, i));
            return (num + 1) + trimmed.charAt(i) + " ";
        }
        return null;
    }

    @Nullable
    private static MdAstListItem findEnclosingListItem(UnistNode node, int cursorIndex) {
        UnistPosition pos = node.position();
        if (pos != null && pos.start() != null && pos.end() != null) {
            if (cursorIndex < pos.start()
                .offset() || cursorIndex
                    > pos.end()
                        .offset()) {
                return null;
            }
        }
        if (node instanceof MdAstListItem) {
            return (MdAstListItem) node;
        }
        if (node instanceof MdAstParent) {
            for (UnistNode child : ((MdAstParent<?>) node).children()) {
                MdAstListItem found = findEnclosingListItem(child, cursorIndex);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static boolean isListItemContentEmpty(MdAstListItem item, String text) {
        UnistPosition pos = item.position();
        if (pos == null || pos.start() == null || pos.end() == null) return false;
        int start = pos.start()
            .offset();
        int end = pos.end()
            .offset();
        if (start >= end || end > text.length()) return false;
        // Find first newline to get the first line (contains the marker)
        int firstLineEnd = text.indexOf('\n', start);
        if (firstLineEnd < 0 || firstLineEnd >= end) firstLineEnd = end;
        // Skip past the marker: find the first non-digit/non-marker char after indent
        String firstLine = text.substring(start, firstLineEnd);
        String trimmed = firstLine.trim();
        if (trimmed.isEmpty()) return true;
        // Check if after the marker the content is empty
        int contentStart = findListContentStart(trimmed);
        return contentStart >= trimmed.length() || trimmed.substring(contentStart)
            .trim()
            .isEmpty();
    }

    private static int findListContentStart(String trimmed) {
        int i = 0;
        // Unordered: -, *, +
        if (i < trimmed.length()
            && (trimmed.charAt(i) == '-' || trimmed.charAt(i) == '*' || trimmed.charAt(i) == '+')) {
            i++;
            if (i < trimmed.length() && trimmed.charAt(i) == ' ') return i + 1;
        }
        // Ordered: number. or number)
        while (i < trimmed.length() && Character.isDigit(trimmed.charAt(i))) i++;
        if (i > 0 && i + 1 < trimmed.length()
            && (trimmed.charAt(i) == '.' || trimmed.charAt(i) == ')')
            && trimmed.charAt(i + 1) == ' ') {
            return i + 2;
        }
        return 0;
    }

    @Nullable
    private static String resolveNextListMarker(String text, MdAstListItem item, MdAstRoot root) {
        MdAstList list = findParentList(root, item);
        if (list == null) return null;
        // Find the marker from the current item's source text
        UnistPosition pos = item.position();
        if (pos == null || pos.start() == null) return null;
        int itemStart = pos.start()
            .offset();
        int firstLineEnd = text.indexOf('\n', itemStart);
        if (firstLineEnd < 0) firstLineEnd = Math.min(itemStart + 80, text.length());
        String firstLine = text.substring(itemStart, Math.min(firstLineEnd, text.length()))
            .trim();
        String marker = extractListMarker(firstLine);
        if (marker == null) return null;

        if (list.ordered) {
            // Compute next number: find the index of this item in the list's children
            int index = 0;
            for (MdAstListContent child : list.children()) {
                if (child == item) break;
                if (child instanceof MdAstListItem) index++;
            }
            int nextNumber = list.start + index + 1;
            char delimiter = marker.charAt(marker.length() - 1); // . or )
            return indentFor(item) + nextNumber + delimiter + " ";
        }
        return indentFor(item) + marker;
    }

    @Nullable
    private static String extractListMarker(String firstLine) {
        if (firstLine.isEmpty()) return null;
        int i = 0;
        char c = firstLine.charAt(i);
        if (c == '-' || c == '*' || c == '+') {
            return i + 1 < firstLine.length() && firstLine.charAt(i + 1) == ' ' ? "- " : null;
        }
        while (i < firstLine.length() && Character.isDigit(firstLine.charAt(i))) i++;
        if (i > 0 && i + 1 < firstLine.length()
            && (firstLine.charAt(i) == '.' || firstLine.charAt(i) == ')')
            && firstLine.charAt(i + 1) == ' ') {
            return firstLine.substring(0, i) + firstLine.charAt(i) + " ";
        }
        return null;
    }

    private static String indentFor(MdAstListItem item) {
        // Simple: use empty indent (list items are typically left-aligned)
        return "";
    }

    @Nullable
    private static MdAstList findParentList(UnistNode root, MdAstListItem target) {
        if (!(root instanceof MdAstParent)) return null;
        for (UnistNode child : ((MdAstParent<?>) root).children()) {
            if (child instanceof MdAstList) {
                for (MdAstListContent item : ((MdAstList) child).children()) {
                    if (item == target) return (MdAstList) child;
                }
                MdAstList found = findParentList(child, target);
                if (found != null) return found;
            } else {
                MdAstList found = findParentList(child, target);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static int findLineStart(String text, int index) {
        int pos = Math.min(index, text.length());
        while (pos > 0) {
            char previous = text.charAt(pos - 1);
            if (previous == '\n' || previous == '\r') {
                break;
            }
            pos--;
        }
        return pos;
    }

    private static int findLineEnd(String text, int index) {
        int pos = Math.min(index, text.length());
        while (pos < text.length()) {
            char current = text.charAt(pos);
            if (current == '\n' || current == '\r') {
                break;
            }
            pos++;
        }
        return pos;
    }

    private static String leadingWhitespace(String line) {
        int end = 0;
        while (end < line.length()) {
            char c = line.charAt(end);
            if (c != ' ' && c != '\t') {
                break;
            }
            end++;
        }
        return line.substring(0, end);
    }

    private static String normalizeLineEndings(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder normalized = null;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != '\r') {
                if (normalized != null) {
                    normalized.append(c);
                }
                continue;
            }
            if (normalized == null) {
                normalized = new StringBuilder(text.length());
                normalized.append(text, 0, i);
            }
            if (i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                normalized.append('\n');
                i++;
            } else {
                normalized.append('\n');
            }
        }
        return normalized != null ? normalized.toString() : text;
    }

    public void draw(boolean validationError) {
        updateVisualOffsets();
        int renderedVerticalOffset = visualVerticalOffsetPixels.rounded();
        int renderedHorizontalOffset = visualHorizontalOffsetPixels.rounded();
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
        int drawY = y + PADDING - renderedVerticalOffset;
        for (SceneEditorMultilineTextLayoutCache.VisualLine line : lines) {
            if (drawY + lineHeight >= y && drawY < y + clipHeight) {
                drawExternalHighlightForLine(line, drawY, renderedHorizontalOffset);
                drawSelectionForLine(line, drawY, renderedHorizontalOffset);
                fontRenderer.drawString(line.text(), x + PADDING - renderedHorizontalOffset, drawY, 0xF0F0F0);
                drawSyntaxWarningForLine(line, drawY, renderedHorizontalOffset);
            }
            drawY += lineHeight;
        }

        if (focused && shouldRenderCursor()) {
            int cursorLine = getVisualLineIndex(selectionModel.getCursorIndex());
            SceneEditorMultilineTextLayoutCache.VisualLine visualLine = lines.get(cursorLine);
            int cursorPixel = getCursorPixelOnLine(selectionModel.getCursorIndex(), visualLine);
            int cursorX = x + PADDING + cursorPixel - renderedHorizontalOffset;
            int cursorY = y + PADDING + cursorLine * lineHeight - renderedVerticalOffset;
            Gui.drawRect(cursorX, cursorY, cursorX + 1, cursorY + fontRenderer.FONT_HEIGHT + 1, 0xFFFFFFFF);
        }

        if (focused) {
            syncImeFocusProxy();
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1f, 1f, 1f, 1f);
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
        snapVisualOffsetsToTarget();
    }

    private void syncImeFocusProxy() {
        int lineHeight = getLineHeight();
        int cursorX = x + PADDING;
        int cursorY = y + PADDING;
        int renderedVerticalOffset = visualVerticalOffsetPixels.rounded();
        int renderedHorizontalOffset = visualHorizontalOffsetPixels.rounded();
        List<SceneEditorMultilineTextLayoutCache.VisualLine> lines = layoutCache.getVisualLines();
        if (!lines.isEmpty()) {
            int cursorLine = getVisualLineIndex(selectionModel.getCursorIndex());
            SceneEditorMultilineTextLayoutCache.VisualLine visualLine = lines.get(cursorLine);
            int cursorPixel = getCursorPixelOnLine(selectionModel.getCursorIndex(), visualLine);
            cursorX += cursorPixel - renderedHorizontalOffset;
            cursorY += cursorLine * lineHeight - renderedVerticalOffset;
        }

        int contentLeft = x + PADDING;
        int contentTop = y + PADDING;
        int contentRight = Math.max(contentLeft, x + getContentClipWidth() - PADDING);
        int contentBottom = Math.max(contentTop, y + getContentClipHeight() - PADDING);
        int clampedX = clamp(cursorX, contentLeft, contentRight);
        int clampedY = clamp(cursorY, contentTop, contentBottom);
        imeFocusProxy.xPosition = clampedX + 1;
        imeFocusProxy.yPosition = clampedY + 1;
        imeFocusProxy.width = 1;
        imeFocusProxy.height = Math.max(1, lineHeight);
    }

    private void drawSelectionForLine(SceneEditorMultilineTextLayoutCache.VisualLine line, int drawY,
        int renderedHorizontalOffset) {
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

        int max = Math.max(0, highlightStart - line.startIndex());
        String beforeSelection = line.text()
            .substring(0, max);
        String selectedText = line.text()
            .substring(max, Math.max(0, highlightEnd - line.startIndex()));
        int selectionX = x + PADDING + fontRenderer.getStringWidth(beforeSelection) - renderedHorizontalOffset;
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

    private void drawExternalHighlightForLine(SceneEditorMultilineTextLayoutCache.VisualLine line, int drawY,
        int renderedHorizontalOffset) {
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

        int max = Math.max(0, highlightStart - line.startIndex());
        String beforeHighlight = line.text()
            .substring(0, max);
        String highlightedText = line.text()
            .substring(max, Math.max(0, highlightEnd - line.startIndex()));
        int highlightX = x + PADDING + fontRenderer.getStringWidth(beforeHighlight) - renderedHorizontalOffset;
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

    private void drawSyntaxWarningForLine(SceneEditorMultilineTextLayoutCache.VisualLine line, int drawY,
        int renderedHorizontalOffset) {
        if (syntaxWarningStart < 0 || syntaxWarningEnd <= syntaxWarningStart) {
            return;
        }
        boolean spansLineBreak = line.endsWithNewline() && syntaxWarningStart <= line.endIndex()
            && syntaxWarningEnd > line.endIndex();
        int highlightStart = Math.max(syntaxWarningStart, line.startIndex());
        int highlightEnd = Math.min(syntaxWarningEnd, line.endIndex());
        if (highlightEnd <= highlightStart && !spansLineBreak) {
            return;
        }

        int max = Math.max(0, highlightStart - line.startIndex());
        String beforeWarning = line.text()
            .substring(0, max);
        String warnedText = line.text()
            .substring(max, Math.max(0, highlightEnd - line.startIndex()));
        int warningX = x + PADDING + fontRenderer.getStringWidth(beforeWarning) - renderedHorizontalOffset;
        int warningWidth = fontRenderer.getStringWidth(warnedText);
        if (warningWidth <= 0 && spansLineBreak) {
            warningWidth = 2;
        }
        int warningY = drawY + fontRenderer.FONT_HEIGHT + 1;
        for (int pixelX = warningX; pixelX < warningX + warningWidth; pixelX += 4) {
            Gui.drawRect(pixelX, warningY, pixelX + 2, warningY + 1, SYNTAX_WARNING_COLOR);
            Gui.drawRect(pixelX + 2, warningY + 1, pixelX + 4, warningY + 2, SYNTAX_WARNING_COLOR);
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
            visualVerticalOffsetPixels.rounded());
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
            visualHorizontalOffsetPixels.rounded());
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

    public int getCursorIndexAtPublic(int mouseX, int mouseY) {
        return getCursorIndexAt(mouseX, mouseY);
    }

    /** Returns the pixel X position of the cursor relative to this text area. */
    public int getCursorPixelX() {
        List<SceneEditorMultilineTextLayoutCache.VisualLine> lines = layoutCache.getVisualLines();
        if (lines.isEmpty()) return PADDING;
        int lineIdx = getVisualLineIndex(selectionModel.getCursorIndex());
        return PADDING + getCursorPixelOnLine(selectionModel.getCursorIndex(), lines.get(lineIdx))
            - visualHorizontalOffsetPixels.rounded();
    }

    /** Returns the pixel Y position of the cursor relative to this text area. */
    public int getCursorPixelY() {
        List<SceneEditorMultilineTextLayoutCache.VisualLine> lines = layoutCache.getVisualLines();
        if (lines.isEmpty()) return PADDING;
        int lineIdx = getVisualLineIndex(selectionModel.getCursorIndex());
        return PADDING + lineIdx * getLineHeight() - visualVerticalOffsetPixels.rounded();
    }

    public boolean isCursorVisibleInViewport() {
        int cursorX = getCursorPixelX();
        int cursorY = getCursorPixelY();
        return cursorX >= PADDING && cursorX <= getContentClipWidth() - PADDING
            && cursorY >= PADDING
            && cursorY <= getContentClipHeight() - PADDING;
    }

    private int getCursorIndexAt(int mouseX, int mouseY) {
        List<SceneEditorMultilineTextLayoutCache.VisualLine> lines = layoutCache.getVisualLines();
        if (lines.isEmpty()) {
            return 0;
        }
        int localY = mouseY - y - PADDING + visualVerticalOffsetPixels.rounded();
        int lineIndex = localY <= 0 ? 0 : localY / getLineHeight();
        if (lineIndex < 0) {
            lineIndex = 0;
        }
        if (lineIndex >= lines.size()) {
            lineIndex = lines.size() - 1;
        }
        int localX = Math.max(0, mouseX - x - PADDING + visualHorizontalOffsetPixels.rounded());
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
        return Math.min(requestedOffset, maxOffset);
    }

    private void snapVisualOffsetsToTarget() {
        visualVerticalOffsetPixels.snapTo(scrollState.getOffsetPixels());
        visualHorizontalOffsetPixels.snapTo(horizontalOffsetPixels);
    }

    private void updateVisualOffsets() {
        visualVerticalOffsetPixels.updateTowards(
            scrollState.getOffsetPixels(),
            30f,
            0.25f,
            0.01f,
            Math.max(160f, getContentClipHeight() * 2f));
        visualHorizontalOffsetPixels
            .updateTowards(horizontalOffsetPixels, 30f, 0.25f, 0.01f, Math.max(160f, textViewportWidth * 2f));
    }

    private int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
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

    public void setDoubleClickHandler(@Nullable DoubleClickHandler handler) {
        this.doubleClickHandler = handler;
    }

    public interface DoubleClickHandler {

        void onDoubleClick(int cursorIndex);
    }

    public interface ClipboardAccess {

        void copy(String text);

        String paste();
    }
}
