package com.hfstudio.guidenh.guide.internal.mermaid;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.util.GuideStringLines;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstBlockquote;
import com.hfstudio.guidenh.libs.mdast.model.MdAstBreak;
import com.hfstudio.guidenh.libs.mdast.model.MdAstCode;
import com.hfstudio.guidenh.libs.mdast.model.MdAstEmphasis;
import com.hfstudio.guidenh.libs.mdast.model.MdAstHTML;
import com.hfstudio.guidenh.libs.mdast.model.MdAstInlineCode;
import com.hfstudio.guidenh.libs.mdast.model.MdAstLink;
import com.hfstudio.guidenh.libs.mdast.model.MdAstLinkReference;
import com.hfstudio.guidenh.libs.mdast.model.MdAstList;
import com.hfstudio.guidenh.libs.mdast.model.MdAstListItem;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParagraph;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstStrong;
import com.hfstudio.guidenh.libs.mdast.model.MdAstText;

public class MermaidSourceExtractor {

    public static final String NODE_CONTENT_TAG = "NodeContent";
    private static final Pattern NODE_CONTENT_BLOCK = Pattern
        .compile("(?is)\\n?[ \\t]*<NodeContent\\b[^>]*>.*?</NodeContent>[ \\t]*");

    protected MermaidSourceExtractor() {}

    public static boolean isFrontmatterDelimiter(String line) {
        return "---".equals(line.trim());
    }

    public static int findFrontmatterEnd(List<String> lines) {
        for (int i = 1; i < lines.size(); i++) {
            if (isFrontmatterDelimiter(lines.get(i))) {
                return i;
            }
        }
        return -1;
    }

    public static boolean isCommentLine(String line) {
        return line.trim()
            .startsWith("%%");
    }

    public static String normalize(String source) {
        if (source == null || source.isEmpty()) {
            return "";
        }
        return GuideStringLines.normalizeLineEndings(source);
    }

    public static String dedentBlockTagBody(String body) {
        String normalized = GuideStringLines.normalizeLineEndings(body);
        if (normalized.isEmpty()) {
            return normalized;
        }

        List<String> lines = GuideStringLines.splitLines(normalized);
        int firstContentLine = 0;
        while (firstContentLine < lines.size() && lines.get(firstContentLine)
            .trim()
            .isEmpty()) {
            firstContentLine++;
        }

        int minIndent = Integer.MAX_VALUE;
        for (int index = firstContentLine; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line.trim()
                .isEmpty()) {
                continue;
            }
            minIndent = Math.min(minIndent, leadingWhitespaceWidth(line));
        }
        if (minIndent == Integer.MAX_VALUE) {
            minIndent = 0;
        }

        StringBuilder result = new StringBuilder(normalized.length());
        for (int index = firstContentLine; index < lines.size(); index++) {
            if (index > firstContentLine) {
                result.append('\n');
            }
            result.append(removeLeadingWhitespace(lines.get(index), minIndent));
        }

        while (!result.isEmpty() && result.charAt(result.length() - 1) == '\n') {
            result.setLength(result.length() - 1);
        }
        return result.toString();
    }

    private static int leadingWhitespaceWidth(String line) {
        int width = 0;
        for (int index = 0; index < line.length(); index++) {
            char value = line.charAt(index);
            if (value == ' ') {
                width++;
            } else if (value == '\t') {
                width += 4;
            } else {
                break;
            }
        }
        return width;
    }

    private static String removeLeadingWhitespace(String line, int width) {
        if (width <= 0 || line.isEmpty()) {
            return line;
        }

        int index = 0;
        int remaining = width;
        while (index < line.length() && remaining > 0) {
            char value = line.charAt(index);
            if (value == ' ') {
                remaining--;
                index++;
            } else if (value == '\t') {
                remaining -= Math.min(remaining, 4);
                index++;
            } else {
                break;
            }
        }
        return line.substring(index);
    }

    public static boolean isNodeContentElement(@Nullable MdAstAnyContent content) {
        return content instanceof MdxJsxFlowElement flowElement && NODE_CONTENT_TAG.equals(flowElement.name());
    }

    public static @Nullable String readNodeContentId(MdxJsxElementFields element) {
        if (element == null) {
            return null;
        }
        String id = element.getAttributeString("id", "");
        if (id == null) {
            return null;
        }
        String trimmed = id.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static List<MdxJsxFlowElement> collectNodeContentElements(Iterable<? extends MdAstAnyContent> children) {
        List<MdxJsxFlowElement> result = new ArrayList<>();
        for (MdAstAnyContent child : children) {
            if (child instanceof MdxJsxFlowElement flowElement && NODE_CONTENT_TAG.equals(flowElement.name())) {
                result.add(flowElement);
            }
        }
        return result;
    }

    public static String extractDiagramSource(Iterable<? extends MdAstAnyContent> children) {
        StringBuilder builder = new StringBuilder();
        appendSource(builder, children, false);
        return GuideStringLines.normalizeLineEndings(
            builder.toString()
                .trim());
    }

    public static String stripExplicitNodeContentBlocks(String source) {
        if (source == null || source.isEmpty()) {
            return "";
        }
        return GuideStringLines.normalizeLineEndings(
            NODE_CONTENT_BLOCK.matcher(source)
                .replaceAll("\n")
                .trim());
    }

    private static void appendSource(StringBuilder builder, Iterable<? extends MdAstAnyContent> children,
        boolean appendBreak) {
        boolean first = true;
        for (MdAstAnyContent child : children) {
            if (isNodeContentElement(child)) {
                continue;
            }
            if (!first && appendBreak) {
                builder.append('\n');
            }
            appendSource(builder, child);
            first = false;
        }
    }

    private static void appendSource(StringBuilder builder, MdAstAnyContent content) {
        if (content instanceof MdAstText text) {
            builder.append(text.value);
            return;
        }
        if (content instanceof MdAstBreak) {
            builder.append('\n');
            return;
        }
        if (content instanceof MdAstInlineCode code) {
            builder.append(code.value);
            return;
        }
        if (content instanceof MdAstCode codeBlock) {
            builder.append(codeBlock.value);
            return;
        }
        if (content instanceof MdAstHTML html) {
            builder.append(html.value);
            return;
        }
        if (content instanceof MdAstLink link) {
            appendSource(builder, link.children(), false);
            return;
        }
        if (content instanceof MdAstLinkReference reference) {
            appendSource(builder, reference.children(), false);
            return;
        }
        if (content instanceof MdAstStrong strong) {
            appendSource(builder, strong.children(), false);
            return;
        }
        if (content instanceof MdAstEmphasis emphasis) {
            appendSource(builder, emphasis.children(), false);
            return;
        }
        if (content instanceof MdAstParagraph paragraph) {
            appendSource(builder, paragraph.children(), false);
            builder.append('\n');
            return;
        }
        if (content instanceof MdAstBlockquote blockquote) {
            appendSource(builder, blockquote.children(), true);
            builder.append('\n');
            return;
        }
        if (content instanceof MdAstList list) {
            for (MdAstAnyContent child : list.children()) {
                appendSource(builder, child);
                builder.append('\n');
            }
            return;
        }
        if (content instanceof MdAstListItem listItem) {
            appendSource(builder, listItem.children(), true);
            return;
        }
        if (content instanceof MdAstParent<?>parent) {
            appendSource(builder, parent.children(), false);
        }
    }
}
