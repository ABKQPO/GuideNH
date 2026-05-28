package com.hfstudio.guidenh.guide.layout.flow;

import java.text.BreakIterator;
import java.text.CharacterIterator;
import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.document.DefaultStyles;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytItemImage;
import com.hfstudio.guidenh.guide.document.flow.InlineBlockAlignment;
import com.hfstudio.guidenh.guide.document.flow.LytFlowAnchor;
import com.hfstudio.guidenh.guide.document.flow.LytFlowBreak;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.document.flow.LytFlowText;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;
import com.hfstudio.guidenh.guide.style.TextAlignment;

/**
 * Does inline-flow layout similar to how it is described here:
 * <a href=
 * "https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Flow_Layout/Block_and_Inline_Layout_in_Normal_Flow">...</a>
 */
public class LineBuilder implements Consumer<LytFlowContent> {

    private static final ThreadLocal<BreakIterator> LINE_BREAK_ITERATOR = ThreadLocal
        .withInitial(BreakIterator::getLineInstance);

    private final LayoutContext context;
    private final List<Line> lines;
    // Contains any floating elements we construct as part of processing flow content
    private final List<LineBlock> floats;
    private final int lineBoxX;
    private final int startY;
    private int innerX;
    private int lineBoxY;
    private final int lineBoxWidth;
    private int remainingLineWidth;
    @Nullable
    private LineElement openLineElement;
    @Nullable
    private LineElement openLineTail;
    private final TextAlignment alignment;
    private final StringBuilder lineBuffer = new StringBuilder();

    /** Reusable CharacterIterator wrapping {@link #lineBuffer} to avoid String allocation in BreakIterator. */
    private final StringBuilderCharIterator charIterator = new StringBuilderCharIterator();

    /**
     * Incremental min-top, max-bottom, and max-right of elements on the current open line.
     * Updated by {@link #appendToOpenLine} and reset by {@link #endLine}.
     * Eliminates the O(N) scan in {@link #endLine} for line-height and line-width.
     */
    private int openLineMinTop = Integer.MAX_VALUE;
    private int openLineMaxBottom;
    private int openLineMaxRight;
    private boolean openLineHasInlineItemImage;

    public LineBuilder(LayoutContext context, int x, int y, int availableWidth, List<Line> lines,
        List<LineBlock> floats, TextAlignment alignment) {
        this.floats = floats;
        this.alignment = alignment;
        this.context = context;
        this.startY = y;
        lineBoxX = x;
        lineBoxY = y;
        lineBoxWidth = availableWidth;
        remainingLineWidth = getAvailableHorizontalSpace();
        this.lines = lines;
    }

    @Override
    public void accept(LytFlowContent content) {
        if (content instanceof LytFlowText text) {
            appendText(text.getText(), content);
        } else if (content instanceof LytFlowBreak) {
            appendBreak(content);
        } else if (content instanceof LytFlowInlineBlock inlineBlock) {
            appendInlineBlock(inlineBlock);
        } else if (content instanceof LytFlowAnchor anchor) {
            // Simply set the current layout position for the anchor
            anchor.setLayoutY(lineBoxY);
        } else {
            throw new IllegalArgumentException("Don't know how to layout flow content: " + content);
        }
    }

    private void appendBreak(@Nullable LytFlowContent flowContent) {
        // Append an empty line with the default style
        if (openLineElement == null) {
            openLineElement = new LineTextRun("", DefaultStyles.BASE_STYLE, DefaultStyles.BASE_STYLE);
            openLineElement.flowContent = flowContent;
            openLineTail = openLineElement;
        }
        endLine();

        // Clear floats, if requested
        if (flowContent instanceof LytFlowBreak flowBreak) {
            if (flowBreak.isClearLeft() || flowBreak.isClearRight()) {
                context.clearFloats(flowBreak.isClearLeft(), flowBreak.isClearRight())
                    .ifPresent(floatBottom -> lineBoxY = Math.max(lineBoxY, floatBottom));
            }
        }
    }

    private void appendInlineBlock(LytFlowInlineBlock inlineBlock) {
        var layoutBounds = inlineBlock.getPreferredBounds(lineBoxWidth);
        var size = layoutBounds.size();
        var block = inlineBlock.getBlock();
        var marginLeft = block.getMarginLeft();
        var marginRight = block.getMarginRight();
        var marginTop = block.getMarginTop();
        var marginBottom = block.getMarginBottom();

        // Is there enough space to have this element here?
        var outerWidth = size.width() + marginLeft + marginRight;
        ensureSpaceIsAvailable(outerWidth);

        var el = new LineBlock(block);
        el.bounds = new LytRect(innerX + marginLeft, layoutBounds.y(), size.width(), size.height());
        el.flowContent = inlineBlock;

        if (inlineBlock.getAlignment() == InlineBlockAlignment.FLOAT_LEFT) {
            // Float it to the left of the actual text content.
            // endLine will take care of moving any existing text in the line
            el.bounds = el.bounds.withX(getInnerLeftEdge() + marginLeft)
                .withY(lineBoxY + layoutBounds.y());
            // Update the layout of the contained block to update its absolute position
            block.layout(context, el.bounds.x(), el.bounds.y(), size.width());
            el.floating = true;
            context.addLeftFloat(el.bounds.expand(0, 0, marginRight, marginBottom));
            floats.add(el);
            remainingLineWidth -= outerWidth;
        } else if (inlineBlock.getAlignment() == InlineBlockAlignment.FLOAT_RIGHT) {
            // Float it to the right the actual text content.
            el.bounds = el.bounds.withX(getInnerRightEdge() - el.bounds.width() + marginRight)
                .withY(lineBoxY + layoutBounds.y());
            // Update the layout of the contained block to update its absolute position
            block.layout(context, el.bounds.x(), el.bounds.y(), size.width());
            el.floating = true;
            context.addRightFloat(el.bounds.expand(marginLeft, 0, 0, marginBottom));
            floats.add(el);
            remainingLineWidth -= outerWidth;
        } else {
            // Treat as a normal inline element for positioning
            innerX += size.width();
            appendToOpenLine(el);

            // Since no margin is actually accounted for here, the remaining line width should just
            // be reduced
            remainingLineWidth -= size.width();
        }
    }

    private void ensureSpaceIsAvailable(int width) {
        if (width <= remainingLineWidth) {
            return; // Got enough
        }

        // First, try closing out any open line if we don't have enough space
        endLine();

        if (width <= remainingLineWidth) {
            return; // We got enough by ending the current line and advancing to the next
        }

        // If we *still* don't have enough room, we need to advance down to clear floats
        // as long as any float is still open
        var nextFloatEdge = context.getNextFloatBottomEdge(lineBoxY);
        while (nextFloatEdge.isPresent()) {
            lineBoxY = nextFloatEdge.getAsInt();
            context.clearFloatsAbove(lineBoxY);
            remainingLineWidth = getAvailableHorizontalSpace();
            if (width <= remainingLineWidth) {
                break; // Finally, we're good!
            }
            nextFloatEdge = context.getNextFloatBottomEdge(lineBoxY);
        }
    }

    @Nullable
    private LineElement getEndOfOpenLine() {
        return openLineTail;
    }

    private void appendText(String text, LytFlowContent flowContent) {
        var style = flowContent.resolveStyle();
        var hoverStyle = flowContent.resolveHoverStyle(style);

        char lastChar = '\0';
        var endOfOpenLine = getEndOfOpenLine();
        if (endOfOpenLine instanceof LineTextRun textRun && !textRun.text.isEmpty()) {
            lastChar = textRun.text.charAt(textRun.text.length() - 1);
        } else if (endOfOpenLine == null || endOfOpenLine.floating) {
            // Treat the first text in a line or text directly after a float as if it was after a line-break.
            lastChar = '\n';
        }

        iterateRuns(text, style, lastChar, (run, width, endLine) -> {
            if (run.length() != 0) {
                var el = new LineTextRun(run.toString(), style, hoverStyle);
                el.flowContent = flowContent;
                int w = Math.round(width);
                int h = context.getLineHeight(style);
                el.bounds = new LytRect(innerX, 0, w, h);
                appendToOpenLine(el);
                innerX += w;
                remainingLineWidth -= w;
            }
            if (endLine) {
                endLine();
            }
        });
    }

    private void iterateRuns(CharSequence text, ResolvedTextStyle style, char lastChar, LineConsumer consumer) {
        float curLineWidth = 0;

        lineBuffer.setLength(0);

        // Hoist whitespace mode flags out of the per-character loop to avoid repeated method calls.
        boolean collapseSegmentBreaks = style.whiteSpace()
            .isCollapseSegmentBreaks();
        boolean collapseWhitespace = style.whiteSpace()
            .isCollapseWhitespace();

        boolean lastCharWasWhitespace = Character.isWhitespace(lastChar);
        boolean canBreakAtStart = lastCharWasWhitespace;

        for (var i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            int codePoint = ch;

            // UTF-16 surrogate handling
            if (Character.isHighSurrogate(ch) && i + 1 < text.length()) {
                // Always consume the next char if it's a low surrogate
                char low = text.charAt(i + 1);
                if (Character.isLowSurrogate(low)) {
                    i++; // Skip the low surrogate
                    codePoint = Character.toCodePoint(ch, low);
                }
            }

            // Handle explicit line breaks
            if (codePoint == '\n') {
                if (collapseSegmentBreaks) {
                    codePoint = ' ';
                } else {
                    consumer.visitRun(lineBuffer, curLineWidth, true);
                    lineBuffer.setLength(0);
                    lastCharWasWhitespace = true;
                    remainingLineWidth = getAvailableHorizontalSpace();
                    continue;
                }
            }

            if (Character.isWhitespace(codePoint)) {
                // Skip if the last one was a space already
                if (lastCharWasWhitespace && collapseWhitespace) {
                    continue; // White space collapsing
                }
                lastCharWasWhitespace = true;
            } else {
                lastCharWasWhitespace = false;
            }

            var advance = context.getAdvance(codePoint, style);
            // Break line if necessary
            if (curLineWidth + advance > remainingLineWidth) {
                int precedingBreakOpportunity;

                // BreakIterator will only ever break *AFTER* whitespace, but since we ignore the last break opportunity
                // we need to also ignore that...
                if (lastCharWasWhitespace) {
                    precedingBreakOpportunity = lineBuffer.length();
                } else {
                    // Find break opportunities and include the current character in it.
                    // Append the char temporarily to avoid a string-concatenation allocation.
                    var breakIterator = LINE_BREAK_ITERATOR.get();
                    lineBuffer.append((char) codePoint);
                    charIterator.reset(lineBuffer);
                    breakIterator.setText(charIterator);
                    precedingBreakOpportunity = breakIterator.preceding(lineBuffer.length());
                    lineBuffer.setLength(lineBuffer.length() - 1);
                }

                // If the preceding text chunk ended on a whitespace, we can break there if the
                // current word does not offer us any opportunity to.
                if (precedingBreakOpportunity > 0 || precedingBreakOpportunity == 0 && canBreakAtStart) {
                    // Determine width up until the break opportunity.
                    // Short-circuit: when breaking at end-of-buffer (whitespace case), curLineWidth
                    // already equals the total, so skip the O(N) recomputation loop.
                    float widthAtBreakOpportunity;
                    if (precedingBreakOpportunity == lineBuffer.length()) {
                        widthAtBreakOpportunity = curLineWidth;
                    } else {
                        widthAtBreakOpportunity = 0f;
                        for (var j = 0; j < precedingBreakOpportunity; j++) {
                            widthAtBreakOpportunity += context.getAdvance(lineBuffer.charAt(j), style);
                        }
                    }

                    consumer
                        .visitRun(lineBuffer.subSequence(0, precedingBreakOpportunity), widthAtBreakOpportunity, true);
                    curLineWidth -= widthAtBreakOpportunity;
                    lineBuffer.delete(0, precedingBreakOpportunity);
                    if (lineBuffer.length() != 0 && Character.isWhitespace(lineBuffer.charAt(0))) {
                        var firstChar = lineBuffer.charAt(0);
                        lineBuffer.deleteCharAt(0);
                        curLineWidth -= context.getAdvance(firstChar, style);
                    }
                } else {
                    // We exceeded the line length, but did not find a break opportunity
                    // this causes a forced break mid-word
                    consumer.visitRun(lineBuffer, curLineWidth, true);
                    lineBuffer.setLength(0);
                    curLineWidth = 0;
                }
                remainingLineWidth = getAvailableHorizontalSpace();
                // If a white-space character broke the line, ignore it as it
                // would otherwise be at the start of the next line
                if (lastCharWasWhitespace) {
                    continue;
                }
            }
            curLineWidth += advance;
            lineBuffer.appendCodePoint(codePoint);
        }

        if (lineBuffer.length() != 0) {
            consumer.visitRun(lineBuffer, curLineWidth, false);
        }
    }

    private void endLine() {
        if (openLineElement == null) {
            return;
        }

        context.clearFloatsAbove(lineBoxY);
        if (openLineHasInlineItemImage) {
            alignInlineItemImages();
            recomputeOpenLineMetrics();
        }

        // Use incrementally tracked values instead of rescanning the linked list.
        var lineHeight = Math.max(1, openLineMaxBottom - openLineMinTop);
        var lineWidth = openLineMaxRight;

        var textAreaStart = getInnerLeftEdge();
        var textAreaEnd = getInnerRightEdge();

        // Apply alignment
        int xTranslation = textAreaStart;
        if (alignment == TextAlignment.RIGHT) {
            xTranslation = textAreaEnd - lineWidth;
        } else if (alignment == TextAlignment.CENTER) {
            xTranslation = textAreaStart + ((textAreaEnd - textAreaStart) - lineWidth) / 2;
        }

        // reposition all line elements
        int actualRight = lineBoxX;
        for (var el = openLineElement; el != null; el = el.next) {
            el.bounds = el.bounds.move(xTranslation, lineBoxY);
            // Ensure that inline blocks update their blocks absolute position
            if (el instanceof LineBlock lineBlock) {
                lineBlock.getBlock()
                    .layout(context, el.bounds.x(), el.bounds.y(), el.bounds.width());
            }

            actualRight = Math.max(actualRight, el.bounds.right());
        }

        var lineBounds = new LytRect(lineBoxX, lineBoxY + openLineMinTop, actualRight - lineBoxX, lineHeight);
        var line = new Line(lineBounds, openLineElement);
        lines.add(line);

        // Advance vertically
        lineBoxY = line.bounds.bottom();

        // Close out any floats that are above the fold
        context.clearFloatsAbove(lineBoxY);

        // Reset horizontal position
        openLineElement = null;
        openLineTail = null;
        innerX = 0;
        openLineMinTop = Integer.MAX_VALUE;
        openLineMaxBottom = 0;
        openLineMaxRight = 0;
        openLineHasInlineItemImage = false;

        // Recompute now that floats may have been closed, what the horizontal space really is
        remainingLineWidth = getInnerRightEdge() - getInnerLeftEdge();
    }

    // How much horizontal space is available in a new line, accounting for active floats that take up space
    private int getAvailableHorizontalSpace() {
        return Math.max(0, getInnerRightEdge() - getInnerLeftEdge());
    }

    // Absolute X coord of the beginning of the text area of the current line box
    private int getInnerLeftEdge() {
        return context.getLeftFloatRightEdgeOr(lineBoxX);
    }

    // Absolute X coord of the end of the text area of the current line box
    private int getInnerRightEdge() {
        return context.getRightFloatLeftEdgeOr(lineBoxX + lineBoxWidth);
    }

    private void appendToOpenLine(LineElement el) {
        if (openLineElement != null) {
            if (openLineTail != null) {
                openLineTail.next = el;
            }
        } else {
            openLineElement = el;
        }
        openLineTail = el;
        int top = el.bounds.y();
        if (top < openLineMinTop) openLineMinTop = top;
        int bottom = el.bounds.bottom();
        if (bottom > openLineMaxBottom) openLineMaxBottom = bottom;
        int right = el.bounds.right();
        if (right > openLineMaxRight) openLineMaxRight = right;
        if (isInlineItemImage(el)) {
            openLineHasInlineItemImage = true;
        }
    }

    private void alignInlineItemImages() {
        int targetHeight = getTextLineHeightOrDefault();
        for (var el = openLineElement; el != null; el = el.next) {
            if (el instanceof LineBlock lineBlock && lineBlock.getBlock() instanceof LytItemImage itemImage
                && itemImage.isInline()
                && itemImage.isShowingIcon()) {
                int offset = getInlineItemYOffset(itemImage);
                targetHeight = Math.max(targetHeight, el.bounds.height() + Math.abs(offset) * 2);
            }
        }

        for (var el = openLineElement; el != null; el = el.next) {
            if (el instanceof LineTextRun) {
                el.bounds = el.bounds.withY((targetHeight - el.bounds.height()) / 2);
            } else if (el instanceof LineBlock lineBlock && lineBlock.getBlock() instanceof LytItemImage itemImage
                && itemImage.isInline()
                && itemImage.isShowingIcon()) {
                    int centeredTop = (targetHeight - el.bounds.height()) / 2;
                    el.bounds = el.bounds.withY(centeredTop + getInlineItemYOffset(itemImage));
                }
        }
    }

    private int getInlineItemYOffset(LytItemImage itemImage) {
        return itemImage.getInlineVerticalOffset();
    }

    private int getTextLineHeightOrDefault() {
        int lineHeight = 0;
        for (var el = openLineElement; el != null; el = el.next) {
            if (el instanceof LineTextRun) {
                lineHeight = Math.max(lineHeight, el.bounds.height());
            }
        }
        return lineHeight > 0 ? lineHeight : context.getLineHeight(DefaultStyles.BASE_STYLE);
    }

    private void recomputeOpenLineMetrics() {
        openLineMinTop = Integer.MAX_VALUE;
        openLineMaxBottom = 0;
        openLineMaxRight = 0;
        for (var el = openLineElement; el != null; el = el.next) {
            int top = el.bounds.y();
            if (top < openLineMinTop) openLineMinTop = top;
            int bottom = el.bounds.bottom();
            if (bottom > openLineMaxBottom) openLineMaxBottom = bottom;
            int right = el.bounds.right();
            if (right > openLineMaxRight) openLineMaxRight = right;
        }
    }

    private boolean isInlineItemImage(LineElement element) {
        return element instanceof LineBlock lineBlock && lineBlock.getBlock() instanceof LytItemImage itemImage
            && itemImage.isInline()
            && itemImage.isShowingIcon();
    }

    public void end() {
        endLine();
    }

    public LytRect getBounds() {
        int width = 0;
        for (var line : lines) {
            width = Math.max(width, line.bounds.width());
        }
        return new LytRect(lineBoxX, startY, width, lineBoxY - startY);
    }

    @FunctionalInterface
    interface LineConsumer {

        void visitRun(CharSequence run, float width, boolean end);
    }

    /**
     * A {@link CharacterIterator} that wraps a {@link StringBuilder} without copying it to a {@link String}.
     * Reuse by calling {@link #reset} before each use.
     */
    private static final class StringBuilderCharIterator implements CharacterIterator {

        private StringBuilder sb;
        private int begin;
        private int end;
        private int pos;

        /** Points this iterator at the full content of {@code buf}. */
        public void reset(StringBuilder buf) {
            this.sb = buf;
            this.begin = 0;
            this.end = buf.length();
            this.pos = 0;
        }

        @Override
        public char first() {
            pos = begin;
            return current();
        }

        @Override
        public char last() {
            if (end == begin) {
                pos = end;
                return DONE;
            }
            pos = end - 1;
            return current();
        }

        @Override
        public char current() {
            return (pos >= begin && pos < end) ? sb.charAt(pos) : DONE;
        }

        @Override
        public char next() {
            if (pos < end - 1) {
                pos++;
                return current();
            }
            pos = end;
            return DONE;
        }

        @Override
        public char previous() {
            if (pos > begin) {
                pos--;
                return current();
            }
            return DONE;
        }

        @Override
        public char setIndex(int position) {
            if (position < begin || position > end) {
                throw new IllegalArgumentException("Invalid index: " + position);
            }
            pos = position;
            return current();
        }

        @Override
        public int getBeginIndex() {
            return begin;
        }

        @Override
        public int getEndIndex() {
            return end;
        }

        @Override
        public int getIndex() {
            return pos;
        }

        @Override
        public Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(e);
            }
        }
    }
}
