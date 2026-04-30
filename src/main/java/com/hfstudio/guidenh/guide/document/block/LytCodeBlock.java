package com.hfstudio.guidenh.guide.document.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.guide.document.flow.LytFlowText;
import com.hfstudio.guidenh.guide.document.interaction.DocumentDragTarget;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorVerticalScrollbar;
import com.hfstudio.guidenh.guide.internal.markdown.CodeBlockLanguage;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.BorderStyle;
import com.hfstudio.guidenh.guide.style.WhiteSpaceMode;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;

public class LytCodeBlock extends LytVBox implements InteractiveElement, DocumentDragTarget {

    private static final ConstantColor CODE_DEFAULT = new ConstantColor(0xFFD7DEE7);
    private static final ConstantColor CODE_KEYWORD = new ConstantColor(0xFF7FD7FF);
    private static final ConstantColor CODE_STRING = new ConstantColor(0xFF9BE28F);
    private static final ConstantColor CODE_NUMBER = new ConstantColor(0xFFFFC774);
    private static final ConstantColor CODE_COMMENT = new ConstantColor(0xFF7D8794);
    private static final ConstantColor CODE_PUNCT = new ConstantColor(0xFFB7C0CD);
    private static final int BODY_PADDING = 6;
    private static final int SCROLLBAR_WIDTH = 5;
    private static final int MIN_SCROLLBAR_THUMB = 14;

    private final LytCodeBlockToolbar toolbar = new LytCodeBlockToolbar();
    private final LytParagraph body = new LytParagraph();

    private String codeText = "";
    private String languageFenceName = "";
    private String languageDisplayName = "Text";
    private String detectedLanguageId = "text";
    private int forcedBodyHeight;
    private int bodyContentHeight;
    private int bodyViewportHeight;
    private int bodyScrollOffsetY;
    private boolean draggingBody;
    private int dragLastDocumentY;
    private boolean draggingScrollbar;
    private int scrollbarGrabOffsetY;
    private int lastBodyLineCount;

    public LytCodeBlock() {
        setPadding(6);
        setGap(4);
        setFullWidth(true);
        setBackgroundColor(SymbolicColor.BLOCKQUOTE_BACKGROUND);
        setBorder(new BorderStyle(SymbolicColor.TABLE_BORDER, 1));

        body.setMarginTop(0);
        body.setMarginBottom(0);
        body.setPaddingLeft(BODY_PADDING);
        body.setPaddingRight(BODY_PADDING);
        body.setPaddingTop(BODY_PADDING);
        body.setPaddingBottom(BODY_PADDING);
        body.modifyStyle(
            style -> style.whiteSpace(WhiteSpaceMode.PRE_WRAP)
                .color(CODE_DEFAULT));

        append(toolbar);
        append(body);
        syncToolbar();
    }

    public String getCodeText() {
        return codeText;
    }

    public void setCodeText(String codeText) {
        this.codeText = codeText != null ? codeText : "";
        this.lastBodyLineCount = countBodyLines();
        toolbar.setCopyText(this.codeText);
        rebuildBody();
    }

    public String getLanguageFenceName() {
        return languageFenceName;
    }

    public void setLanguageFenceName(String languageFenceName) {
        this.languageFenceName = languageFenceName != null ? languageFenceName : "";
    }

    public String getLanguageDisplayName() {
        return languageDisplayName;
    }

    public void setLanguageDisplayName(String languageDisplayName) {
        this.languageDisplayName = languageDisplayName != null && !languageDisplayName.isEmpty() ? languageDisplayName
            : "Text";
        syncToolbar();
    }

    public String getDetectedLanguageId() {
        return detectedLanguageId;
    }

    public void applyLanguage(CodeBlockLanguage language) {
        if (language == null) {
            detectedLanguageId = "text";
            setLanguageDisplayName("Text");
            return;
        }
        detectedLanguageId = language.id();
        setLanguageDisplayName(language.displayName());
    }

    public int getForcedBodyHeight() {
        return forcedBodyHeight;
    }

    public void setForcedBodyHeight(int forcedBodyHeight) {
        this.forcedBodyHeight = Math.max(0, forcedBodyHeight);
    }

    public int getBodyScrollOffsetY() {
        return bodyScrollOffsetY;
    }

    public int getBodyViewportHeight() {
        return bodyViewportHeight;
    }

    public int getBodyContentHeight() {
        return bodyContentHeight;
    }

    public int getBodyLineCount() {
        return lastBodyLineCount;
    }

    @Override
    public boolean mouseClicked(GuideUiHost screen, int x, int y, int button, boolean doubleClick) {
        if (toolbar.mouseClicked(screen, x, y, button, doubleClick)) {
            return true;
        }
        if (button == 0 && getScrollbarTrackBounds().contains(x, y)) {
            LytRect thumbBounds = getScrollbarThumbBounds();
            if (!thumbBounds.isEmpty() && thumbBounds.contains(x, y)) {
                scrollbarGrabOffsetY = y - thumbBounds.y();
            } else {
                scrollbarGrabOffsetY = thumbBounds.isEmpty() ? 0 : thumbBounds.height() / 2;
                updateScrollFromMouseY(y);
            }
            draggingScrollbar = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean beginDrag(int documentX, int documentY, int button) {
        if (button != 0) {
            return false;
        }
        if (getScrollbarTrackBounds().contains(documentX, documentY)) {
            return draggingScrollbar;
        }
        if (!getBodyViewportBounds().contains(documentX, documentY) || getMaxBodyScroll() <= 0) {
            return false;
        }
        draggingBody = true;
        dragLastDocumentY = documentY;
        return true;
    }

    @Override
    public void dragTo(int documentX, int documentY) {
        if (draggingScrollbar) {
            updateScrollFromMouseY(documentY);
            return;
        }
        if (!draggingBody) {
            return;
        }
        int deltaY = documentY - dragLastDocumentY;
        dragLastDocumentY = documentY;
        setBodyScrollOffset(bodyScrollOffsetY - deltaY);
    }

    @Override
    public void endDrag() {
        draggingBody = false;
        draggingScrollbar = false;
    }

    @Override
    public boolean scroll(int documentX, int documentY, int wheelDelta) {
        if (wheelDelta == 0 || !getBodyViewportBounds().contains(documentX, documentY) || getMaxBodyScroll() <= 0) {
            return false;
        }
        int step = Math.max(12, resolveLineHeight() * 2);
        setBodyScrollOffset(bodyScrollOffsetY - Integer.signum(wheelDelta) * step);
        return true;
    }

    @Override
    protected LytRect computeBoxLayout(LayoutContext context, int x, int y, int availableWidth) {
        int safeWidth = Math.max(1, availableWidth);
        LytRect toolbarBounds = toolbar.layout(context, x, y, safeWidth);

        int bodyY = toolbarBounds.bottom() + getGap();
        int bodyAvailableWidth = safeWidth;
        boolean reserveScrollbar = false;

        LytRect measuredBody = body.layout(context, x, bodyY, bodyAvailableWidth);
        bodyContentHeight = measuredBody.height();
        bodyViewportHeight = forcedBodyHeight > 0 ? forcedBodyHeight : bodyContentHeight;
        if (forcedBodyHeight > 0 && bodyContentHeight > bodyViewportHeight) {
            reserveScrollbar = true;
            bodyAvailableWidth = Math.max(1, safeWidth - SCROLLBAR_WIDTH - 4);
            measuredBody = body.layout(context, x, bodyY, bodyAvailableWidth);
            bodyContentHeight = measuredBody.height();
        }

        bodyViewportHeight = forcedBodyHeight > 0 ? forcedBodyHeight : bodyContentHeight;
        setBodyScrollOffset(bodyScrollOffsetY);

        body.setLayoutPos(
            body.getBounds()
                .point()
                .add(0, -bodyScrollOffsetY));
        int totalHeight = toolbarBounds.height() + getGap() + bodyViewportHeight;
        if (reserveScrollbar) {
            totalHeight = Math.max(totalHeight, toolbarBounds.height() + getGap() + bodyViewportHeight);
        }
        return new LytRect(x, y, safeWidth, totalHeight);
    }

    @Override
    public void render(RenderContext context) {
        LytRect ownBounds = getBounds();
        if (ownBounds.isEmpty()) {
            return;
        }
        if (getBackgroundColor() != null) {
            context.fillRect(ownBounds, getBackgroundColor());
        }

        toolbar.render(context);

        LytRect bodyViewport = getBodyViewportBounds();
        context.pushLocalScissor(bodyViewport);
        try {
            body.render(context);
        } finally {
            context.popScissor();
        }

        renderScrollbar(context);
        new BorderRenderer()
            .render(context, ownBounds, getBorderTop(), getBorderLeft(), getBorderRight(), getBorderBottom());
    }

    private void syncToolbar() {
        toolbar.setLanguageDisplayName(languageDisplayName);
        toolbar.setCopyText(codeText);
    }

    private void rebuildBody() {
        body.clearContent();
        List<LytFlowSpan> lines = highlightLines();
        for (int i = 0; i < lines.size(); i++) {
            body.append(lines.get(i));
            if (i < lines.size() - 1) {
                body.appendBreak();
            }
        }
    }

    private int countBodyLines() {
        String normalized = codeText.replace("\r\n", "\n")
            .replace('\r', '\n');
        if (normalized.isEmpty()) {
            return 1;
        }
        int lines = 1;
        for (int i = 0; i < normalized.length(); i++) {
            if (normalized.charAt(i) == '\n') {
                lines++;
            }
        }
        return lines;
    }

    private int resolveLineHeight() {
        return 10;
    }

    private void renderScrollbar(RenderContext context) {
        if (getMaxBodyScroll() <= 0) {
            return;
        }
        LytRect track = getScrollbarTrackBounds();
        if (track.isEmpty()) {
            return;
        }
        context.fillRect(track, 0x30242B33);
        LytRect thumb = getScrollbarThumbBounds();
        if (!thumb.isEmpty()) {
            context.fillRect(thumb, draggingScrollbar ? 0xFFCDD6E1 : 0xA0AAB5C2);
        }
    }

    private LytRect getBodyViewportBounds() {
        LytRect own = getBounds();
        LytRect toolbarBounds = toolbar.getBounds();
        int viewportY = toolbarBounds.bottom() + getGap();
        int viewportHeight = Math.max(0, bodyViewportHeight);
        int viewportWidth = own.width();
        if (getMaxBodyScroll() > 0) {
            viewportWidth = Math.max(1, viewportWidth - SCROLLBAR_WIDTH - 4);
        }
        return new LytRect(own.x(), viewportY, viewportWidth, viewportHeight);
    }

    private LytRect getScrollbarTrackBounds() {
        if (getMaxBodyScroll() <= 0) {
            return LytRect.empty();
        }
        LytRect own = getBounds();
        LytRect viewport = getBodyViewportBounds();
        int x = own.right() - SCROLLBAR_WIDTH - 1;
        return new LytRect(x, viewport.y(), SCROLLBAR_WIDTH, viewport.height());
    }

    private LytRect getScrollbarThumbBounds() {
        LytRect track = getScrollbarTrackBounds();
        if (track.isEmpty()) {
            return LytRect.empty();
        }
        int thumbHeight = Math
            .max(MIN_SCROLLBAR_THUMB, track.height() * track.height() / Math.max(track.height(), bodyContentHeight));
        thumbHeight = Math.min(thumbHeight, track.height());
        int maxScroll = getMaxBodyScroll();
        int thumbTrack = Math.max(1, track.height() - thumbHeight);
        int thumbY = track.y();
        if (maxScroll > 0) {
            thumbY += (int) ((long) thumbTrack * bodyScrollOffsetY / maxScroll);
        }
        return new LytRect(track.x(), thumbY, track.width(), thumbHeight);
    }

    private int getMaxBodyScroll() {
        return Math.max(0, bodyContentHeight - bodyViewportHeight);
    }

    private void setBodyScrollOffset(int bodyScrollOffsetY) {
        this.bodyScrollOffsetY = SceneEditorVerticalScrollbar.clamp(bodyScrollOffsetY, 0, getMaxBodyScroll());
    }

    private void updateScrollFromMouseY(int mouseY) {
        LytRect track = getScrollbarTrackBounds();
        LytRect thumb = getScrollbarThumbBounds();
        if (track.isEmpty() || thumb.isEmpty()) {
            setBodyScrollOffset(0);
            return;
        }
        int thumbTrack = Math.max(1, track.height() - thumb.height());
        int thumbTop = SceneEditorVerticalScrollbar
            .clamp(mouseY - scrollbarGrabOffsetY, track.y(), track.y() + thumbTrack);
        int maxScroll = getMaxBodyScroll();
        setBodyScrollOffset((int) ((long) (thumbTop - track.y()) * maxScroll / thumbTrack));
    }

    private List<LytFlowSpan> highlightLines() {
        String normalized = codeText.replace("\r\n", "\n")
            .replace('\r', '\n');
        String[] lines = normalized.split("\n", -1);
        List<LytFlowSpan> result = new ArrayList<>(lines.length);
        for (String line : lines) {
            result.add(highlightLine(line));
        }
        if (lines.length == 0) {
            result.add(new LytFlowSpan());
        }
        return result;
    }

    private LytFlowSpan highlightLine(String line) {
        LytFlowSpan root = new LytFlowSpan();
        if (line.isEmpty()) {
            root.append(LytFlowText.of(""));
            return root;
        }

        String lowerLanguage = detectedLanguageId.toLowerCase(Locale.ROOT);
        int index = 0;
        while (index < line.length()) {
            int commentStart = findCommentStart(line, index, lowerLanguage);
            if (commentStart == index) {
                appendStyled(root, line.substring(index), CODE_COMMENT);
                break;
            }
            if (commentStart > index) {
                index = appendTokens(root, line, index, commentStart, lowerLanguage);
                continue;
            }
            index = appendTokens(root, line, index, line.length(), lowerLanguage);
        }
        return root;
    }

    private int appendTokens(LytFlowSpan root, String line, int start, int end, String language) {
        int index = start;
        while (index < end) {
            char current = line.charAt(index);
            if (current == '"' || current == '\'') {
                int close = findStringEnd(line, index + 1, current, end);
                appendStyled(root, line.substring(index, close), CODE_STRING);
                index = close;
                continue;
            }
            if (Character.isDigit(current)) {
                int close = index + 1;
                while (close < end && (Character.isDigit(line.charAt(close)) || line.charAt(close) == '.')) {
                    close++;
                }
                appendStyled(root, line.substring(index, close), CODE_NUMBER);
                index = close;
                continue;
            }
            if (Character.isLetter(current) || current == '_' || current == '$') {
                int close = index + 1;
                while (close < end) {
                    char next = line.charAt(close);
                    if (!Character.isLetterOrDigit(next) && next != '_' && next != '$') {
                        break;
                    }
                    close++;
                }
                String token = line.substring(index, close);
                appendStyled(root, token, isKeyword(token, language) ? CODE_KEYWORD : CODE_DEFAULT);
                index = close;
                continue;
            }
            if (!Character.isWhitespace(current)) {
                appendStyled(root, Character.toString(current), CODE_PUNCT);
                index++;
                continue;
            }
            int close = index + 1;
            while (close < end && Character.isWhitespace(line.charAt(close))) {
                close++;
            }
            appendStyled(root, line.substring(index, close), CODE_DEFAULT);
            index = close;
        }
        return index;
    }

    private int findCommentStart(String line, int start, String language) {
        int slashSlash = line.indexOf("//", start);
        int hash = line.indexOf('#', start);
        int dashDash = line.indexOf("--", start);
        int semicolon = line.indexOf(';', start);
        int result = -1;
        if (supportsSlashComment(language)) {
            result = minPositive(result, slashSlash);
        }
        if (supportsHashComment(language)) {
            result = minPositive(result, hash);
        }
        if (supportsDashDashComment(language)) {
            result = minPositive(result, dashDash);
        }
        if ("properties".equals(language)) {
            result = minPositive(result, semicolon);
        }
        return result;
    }

    private int minPositive(int current, int next) {
        if (next < 0) {
            return current;
        }
        if (current < 0) {
            return next;
        }
        return Math.min(current, next);
    }

    private boolean supportsSlashComment(String language) {
        return "java".equals(language) || "kotlin".equals(language)
            || "scala".equals(language)
            || "groovy".equals(language)
            || "json".equals(language)
            || "javascript".equals(language);
    }

    private boolean supportsHashComment(String language) {
        return "yaml".equals(language) || "bash".equals(language)
            || "powershell".equals(language)
            || "properties".equals(language)
            || "mermaid".equals(language);
    }

    private boolean supportsDashDashComment(String language) {
        return "lua".equals(language);
    }

    private int findStringEnd(String line, int start, char quote, int end) {
        int index = start;
        while (index < end) {
            char current = line.charAt(index);
            if (current == '\\') {
                index += 2;
                continue;
            }
            index++;
            if (current == quote) {
                break;
            }
        }
        return Math.min(index, end);
    }

    private boolean isKeyword(String token, String language) {
        return switch (language) {
            case "java" -> matches(
                token,
                "public",
                "private",
                "protected",
                "class",
                "interface",
                "enum",
                "static",
                "void",
                "new",
                "return",
                "if",
                "else",
                "switch",
                "case",
                "for",
                "while",
                "try",
                "catch",
                "throws");
            case "kotlin" -> matches(
                token,
                "fun",
                "val",
                "var",
                "class",
                "object",
                "when",
                "is",
                "in",
                "return",
                "if",
                "else",
                "data",
                "sealed");
            case "scala" -> matches(
                token,
                "object",
                "class",
                "trait",
                "case",
                "def",
                "val",
                "var",
                "extends",
                "match",
                "yield",
                "given",
                "using");
            case "lua" -> matches(
                token,
                "local",
                "function",
                "end",
                "if",
                "then",
                "elseif",
                "else",
                "for",
                "while",
                "repeat",
                "until",
                "return",
                "nil",
                "true",
                "false");
            case "groovy" -> matches(
                token,
                "def",
                "class",
                "interface",
                "enum",
                "return",
                "if",
                "else",
                "switch",
                "case",
                "for",
                "while",
                "in",
                "as");
            case "json" -> matches(token, "true", "false", "null");
            case "yaml" -> matches(token, "true", "false", "null", "yes", "no");
            case "xml" -> false;
            case "properties" -> false;
            case "bash" -> matches(token, "if", "then", "else", "fi", "for", "do", "done", "case", "esac", "function");
            case "powershell" -> matches(token, "function", "param", "if", "else", "foreach", "switch", "return");
            case "markdown" -> token.startsWith("#");
            case "csv" -> false;
            case "mermaid" -> matches(token, "graph", "flowchart", "mindmap", "subgraph");
            default -> false;
        };
    }

    private boolean matches(String token, String... keywords) {
        for (String keyword : keywords) {
            if (keyword.equals(token)) {
                return true;
            }
        }
        return false;
    }

    private void appendStyled(LytFlowSpan root, String text, ConstantColor color) {
        LytFlowSpan span = new LytFlowSpan();
        span.modifyStyle(style -> style.color(color));
        span.append(LytFlowText.of(text));
        root.append(span);
    }
}
