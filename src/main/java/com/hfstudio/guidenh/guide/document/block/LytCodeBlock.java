package com.hfstudio.guidenh.guide.document.block;

import java.util.List;
import java.util.Objects;

import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.guide.document.interaction.DocumentDragTarget;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorVerticalScrollbar;
import com.hfstudio.guidenh.guide.internal.markdown.CodeBlockLanguage;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.CodeHighlightMode;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.CodeHighlightResult;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.CodeHighlightTheme;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.CodeHighlighter;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.CodeTokenType;
import com.hfstudio.guidenh.guide.internal.util.GuideStringLines;
import com.hfstudio.guidenh.guide.internal.util.SmoothFloatState;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.BorderStyle;
import com.hfstudio.guidenh.guide.style.WhiteSpaceMode;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;

public class LytCodeBlock extends LytVBox implements InteractiveElement, DocumentDragTarget {

    private static final CodeHighlightTheme CODE_THEME = CodeHighlightTheme.GITHUB_DARK_DEFAULT;
    private static final CodeHighlighter CODE_HIGHLIGHTER = new CodeHighlighter();
    private static final CodeHighlightFlowBuilder FLOW_BUILDER = new CodeHighlightFlowBuilder(CODE_THEME);
    private static final ConstantColor CODE_DEFAULT = new ConstantColor(CODE_THEME.colorOf(CodeTokenType.PLAIN));
    private static final ConstantColor CODE_BACKGROUND = new ConstantColor(CODE_THEME.backgroundArgb());
    private static final ConstantColor CODE_BORDER = new ConstantColor(CODE_THEME.borderArgb());
    private static final int BODY_PADDING = 6;
    private static final int SCROLLBAR_WIDTH = 5;
    private static final int MIN_SCROLLBAR_THUMB = 14;

    private final LytCodeBlockToolbar toolbar = new LytCodeBlockToolbar();
    private final LytParagraph body = new LytParagraph();

    private String codeText = "";
    private String normalizedCodeText = "";
    private String languageFenceName = "";
    private String languageDisplayName = "Text";
    private String detectedLanguageId = "text";
    private int preferredBodyWidth;
    private int forcedBodyHeight;
    private int bodyContentHeight;
    private int bodyViewportHeight;
    private int bodyScrollOffsetY;
    private final SmoothFloatState visualBodyScrollOffsetY = new SmoothFloatState();
    private boolean draggingBody;
    private int dragLastDocumentY;
    private boolean draggingScrollbar;
    private int scrollbarGrabOffsetY;
    private int lastBodyLineCount;
    private CodeHighlightResult highlightResult = new CodeHighlightResult("text", CodeHighlightMode.PLAIN, List.of());
    private List<LytFlowSpan> highlightedLines = List.of();

    public LytCodeBlock() {
        setPadding(6);
        setGap(4);
        setFullWidth(true);
        setBorder(new BorderStyle(CODE_BORDER, 1));

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
        setCodeContent(languageFenceName, codeText);
    }

    public String getLanguageFenceName() {
        return languageFenceName;
    }

    public void setLanguageFenceName(String languageFenceName) {
        setCodeContent(languageFenceName, codeText);
    }

    public void setCodeContent(String languageFenceName, String codeText) {
        String resolvedFenceName = languageFenceName != null ? languageFenceName : "";
        String resolvedCodeText = codeText != null ? codeText : "";
        String resolvedNormalizedCodeText = GuideStringLines.normalizeLineEndings(resolvedCodeText);
        boolean changed = !Objects.equals(this.languageFenceName, resolvedFenceName)
            || !Objects.equals(this.codeText, resolvedCodeText);
        this.languageFenceName = resolvedFenceName;
        this.codeText = resolvedCodeText;
        this.normalizedCodeText = resolvedNormalizedCodeText;
        this.lastBodyLineCount = countBodyLines(resolvedNormalizedCodeText);
        toolbar.setCopyText(this.codeText);
        if (changed) {
            rebuildBody();
        }
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

    public CodeHighlightResult getHighlightResult() {
        return highlightResult;
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

    public int getPreferredBodyWidth() {
        return preferredBodyWidth;
    }

    public void setPreferredBodyWidth(int preferredBodyWidth) {
        this.preferredBodyWidth = Math.max(0, preferredBodyWidth);
        setFullWidth(this.preferredBodyWidth <= 0);
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
        // Scrollbar-related interactions are handled by beginDrag/dragTo (mouseDown can start a drag directly).
        return toolbar.mouseClicked(screen, x, y, button, doubleClick);
    }

    @Override
    public boolean beginDrag(int documentX, int documentY, int button) {
        if (button != 0) {
            return false;
        }
        if (toolbar.getBounds()
            .contains(documentX, documentY)) {
            return false;
        }
        if (getScrollbarTrackBounds().contains(documentX, documentY)) {
            LytRect thumbBounds = getScrollbarThumbBounds();
            if (!thumbBounds.isEmpty() && thumbBounds.contains(documentX, documentY)) {
                scrollbarGrabOffsetY = documentY - thumbBounds.y();
            } else {
                scrollbarGrabOffsetY = thumbBounds.isEmpty() ? 0 : thumbBounds.height() / 2;
                updateScrollFromMouseY(documentY);
            }
            draggingScrollbar = true;
            return true;
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

    public boolean isDraggingBody() {
        return draggingBody;
    }

    public boolean isDraggingScrollbar() {
        return draggingScrollbar;
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
        int safeWidth = preferredBodyWidth > 0 ? Math.max(1, Math.min(availableWidth, preferredBodyWidth))
            : Math.max(1, availableWidth);
        toolbar.setPreferredWidth(safeWidth);
        LytRect toolbarBounds = toolbar.layout(context, x, y, safeWidth);

        int bodyY = toolbarBounds.bottom() + getGap();
        int bodyAvailableWidth = safeWidth;

        LytRect measuredBody = body.layout(context, x, bodyY, bodyAvailableWidth);
        bodyContentHeight = measuredBody.height();
        bodyViewportHeight = forcedBodyHeight > 0 ? forcedBodyHeight : bodyContentHeight;
        if (forcedBodyHeight > 0 && bodyContentHeight > bodyViewportHeight) {
            bodyAvailableWidth = Math.max(1, safeWidth - SCROLLBAR_WIDTH - 4);
            measuredBody = body.layout(context, x, bodyY, bodyAvailableWidth);
            bodyContentHeight = measuredBody.height();
        }

        bodyViewportHeight = forcedBodyHeight > 0 ? forcedBodyHeight : bodyContentHeight;
        setBodyScrollOffset(bodyScrollOffsetY);
        snapVisualScrollToTarget();
        return new LytRect(x, y, safeWidth, toolbarBounds.height() + getGap() + bodyViewportHeight);
    }

    @Override
    public void render(RenderContext context) {
        updateVisualScroll();
        LytRect ownBounds = getBounds();
        if (ownBounds.isEmpty()) {
            return;
        }
        context.fillRect(ownBounds, CODE_BACKGROUND);

        toolbar.render(context);

        LytRect bodyViewport = getBodyViewportBounds();
        context.pushLocalScissor(bodyViewport);
        try {
            renderBodyWithVisualOffset(context);
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
        highlightResult = CODE_HIGHLIGHTER.highlight(languageFenceName, normalizedCodeText);
        detectedLanguageId = highlightResult.languageId();
        highlightedLines = FLOW_BUILDER.buildLines(highlightResult);
        body.clearContent();
        for (int i = 0; i < highlightedLines.size(); i++) {
            body.append(highlightedLines.get(i));
            if (i < highlightedLines.size() - 1) {
                body.appendBreak();
            }
        }
    }

    private int countBodyLines(String text) {
        if (text.isEmpty()) {
            return 1;
        }
        int lines = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
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
        context.fillRect(track, CODE_THEME.scrollbarTrackArgb());
        LytRect thumb = getScrollbarThumbBounds();
        if (!thumb.isEmpty()) {
            context.fillRect(
                thumb,
                draggingScrollbar ? CODE_THEME.scrollbarThumbActiveArgb() : CODE_THEME.scrollbarThumbArgb());
        }
    }

    private LytRect getBodyViewportBounds() {
        LytRect toolbarBounds = toolbar.getBounds();
        int viewportY = toolbarBounds.bottom() + getGap();
        int viewportHeight = Math.max(0, bodyViewportHeight);
        return new LytRect(
            body.getBounds()
                .x(),
            viewportY,
            body.getBounds()
                .width(),
            viewportHeight);
    }

    private LytRect getScrollbarTrackBounds() {
        if (getMaxBodyScroll() <= 0) {
            return LytRect.empty();
        }
        LytRect viewport = getBodyViewportBounds();
        int x = viewport.right() + 4;
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
            thumbY += (int) ((long) thumbTrack * visualBodyScrollOffsetY.rounded() / maxScroll);
        }
        return new LytRect(track.x(), thumbY, track.width(), thumbHeight);
    }

    private int getMaxBodyScroll() {
        return Math.max(0, bodyContentHeight - bodyViewportHeight);
    }

    private void setBodyScrollOffset(int bodyScrollOffsetY) {
        this.bodyScrollOffsetY = SceneEditorVerticalScrollbar.clamp(bodyScrollOffsetY, 0, getMaxBodyScroll());
        updateBodyPosition();
    }

    private void updateBodyPosition() {
        if (!body.getBounds()
            .isEmpty()
            && !toolbar.getBounds()
                .isEmpty()) {
            int bodyViewportY = toolbar.getBounds()
                .bottom() + getGap();
            body.moveLayoutPos(
                0,
                bodyViewportY - bodyScrollOffsetY
                    - body.getBounds()
                        .y());
        }
    }

    private void renderBodyWithVisualOffset(RenderContext context) {
        int renderDeltaY = bodyScrollOffsetY - visualBodyScrollOffsetY.rounded();
        if (renderDeltaY == 0) {
            body.render(context);
            return;
        }
        body.moveLayoutPos(0, renderDeltaY);
        try {
            body.render(context);
        } finally {
            body.moveLayoutPos(0, -renderDeltaY);
        }
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

    private void snapVisualScrollToTarget() {
        visualBodyScrollOffsetY.snapTo(bodyScrollOffsetY);
    }

    private void updateVisualScroll() {
        visualBodyScrollOffsetY
            .updateTowards(bodyScrollOffsetY, 28f, 0.25f, 0.01f, Math.max(128f, bodyViewportHeight * 2f));
    }
}
