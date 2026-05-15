package com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.GuideMarkdownOptions;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.SyntaxContextResolver;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.SyntaxElementType;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.SyntaxUtils;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.TextSyntaxContext;
import com.hfstudio.guidenh.libs.mdast.MdAst;
import com.hfstudio.guidenh.libs.mdast.MdastOptions;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxAttribute;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstRoot;
import com.hfstudio.guidenh.libs.unist.UnistNode;
import com.hfstudio.guidenh.libs.unist.UnistPosition;

public class MdxSyntaxResolver implements SyntaxContextResolver {

    private static final MdastOptions PARSE_OPTIONS = GuideMarkdownOptions.runtime();

    @Nullable
    private String cachedText;
    @Nullable
    private MdAstRoot cachedRoot;
    @Nullable
    private String cachedFallbackText;

    @Override
    @Nullable
    public TextSyntaxContext resolve(String text, int cursorIndex) {
        if (text == null || text.isEmpty() || cursorIndex < 0 || cursorIndex > text.length()) return null;

        MdAstRoot root;
        if (text.equals(cachedFallbackText)) {
            return resolveFromFallback(text, cursorIndex);
        }

        if (text.equals(cachedText) && cachedRoot != null) {
            root = cachedRoot;
        } else {
            try {
                root = MdAst.fromMarkdown(text, PARSE_OPTIONS);
            } catch (RuntimeException e) {
                cachedText = null;
                cachedRoot = null;
                cachedFallbackText = text;
                return resolveFromFallback(text, cursorIndex);
            }
            cachedText = text;
            cachedRoot = root;
            cachedFallbackText = null;
        }

        return resolveFromAst(root, text, cursorIndex);
    }

    @Nullable
    private TextSyntaxContext resolveFromAst(MdAstRoot root, String text, int cursorIndex) {
        TextSyntaxContext fallback = resolveFromFallback(text, cursorIndex);
        if (fallback != null && fallback.getElementType() != SyntaxElementType.WORD) {
            return fallback;
        }

        MdxJsxElementFields element = findEnclosingMdxElement(root, cursorIndex);
        if (element != null) {
            return resolveMdxAttribute(element, text, cursorIndex);
        }
        return resolvePlainTextWord(text, cursorIndex);
    }

    @Nullable
    private MdxJsxElementFields findEnclosingMdxElement(UnistNode node, int cursorIndex) {
        UnistPosition pos = node.position();
        if (pos != null && pos.start() != null && pos.end() != null) {
            if (cursorIndex < pos.start()
                .offset() || cursorIndex
                    > pos.end()
                        .offset()) {
                return null;
            }
        }

        if (node instanceof MdxJsxElementFields) {
            return (MdxJsxElementFields) node;
        }

        if (node instanceof MdAstParent) {
            for (UnistNode child : ((MdAstParent<?>) node).children()) {
                MdxJsxElementFields found = findEnclosingMdxElement(child, cursorIndex);
                if (found != null) return found;
            }
        }

        return null;
    }

    @Nullable
    private TextSyntaxContext resolveMdxAttribute(MdxJsxElementFields element, String text, int cursorIndex) {
        String tagName = element.name();
        if (tagName == null) return resolvePlainTextWord(text, cursorIndex);

        for (var attrNode : element.attributes()) {
            if (!(attrNode instanceof MdxJsxAttribute)) continue;
            MdxJsxAttribute attr = (MdxJsxAttribute) attrNode;
            if (attr.name == null || attr.name.isEmpty()) continue;

            UnistPosition pos = attrNode.position();
            if (pos == null || pos.start() == null || pos.end() == null) continue;

            int attrStart = pos.start()
                .offset();
            int attrEnd = pos.end()
                .offset();

            if (cursorIndex < attrStart || cursorIndex > attrEnd) continue;

            TextSyntaxContext valueContext = resolveAttributeValue(
                text,
                tagName,
                attr.name,
                attrStart,
                attrEnd,
                cursorIndex);
            if (valueContext != null) return valueContext;
            break;
        }

        UnistPosition elemPos = element.position();
        if (elemPos != null && elemPos.start() != null && elemPos.end() != null) {
            int tagStart = elemPos.start()
                .offset();
            int tagEnd = elemPos.end()
                .offset();
            if (cursorIndex > tagStart && cursorIndex < tagEnd) {
                return resolveAttributeNameFromTag(text, tagName, tagStart, tagEnd, cursorIndex);
            }
        }

        return resolvePlainTextWord(text, cursorIndex);
    }

    private TextSyntaxContext resolvePlainTextWord(String text, int cursorIndex) {
        return SyntaxUtils.resolveWord(text, cursorIndex);
    }

    @Nullable
    private TextSyntaxContext resolveFromFallback(String text, int cursorIndex) {
        TextSyntaxContext tagStart = resolveTagStart(text, cursorIndex);
        if (tagStart != null) return tagStart;

        OpenTag tag = findOpenTagBeforeCursor(text, cursorIndex);
        if (tag == null) {
            return resolvePlainTextWord(text, cursorIndex);
        }
        return resolveFromOpenTag(text, tag, cursorIndex);
    }

    @Nullable
    private TextSyntaxContext resolveTagStart(String text, int cursorIndex) {
        int start = cursorIndex - 1;
        if (start < 0 || text.charAt(start) != '<') {
            return null;
        }
        if (start + 1 < text.length()) {
            char next = text.charAt(start + 1);
            if (next == '/' || next == '!' || next == '?') {
                return null;
            }
        }
        return new TextSyntaxContext(
            SyntaxElementType.TAG_START,
            cursorIndex,
            cursorIndex,
            new TagStartContext(cursorIndex, cursorIndex, ""));
    }

    @Nullable
    private TextSyntaxContext resolveFromOpenTag(String text, OpenTag tag, int cursorIndex) {
        int tagNameEnd = tag.nameStart + tag.name.length();
        if (cursorIndex <= tagNameEnd && cursorIndex >= tag.nameStart) {
            String partial = text.substring(tag.nameStart, cursorIndex);
            return new TextSyntaxContext(
                SyntaxElementType.TAG_START,
                tag.nameStart,
                tagNameEnd,
                new TagStartContext(tag.nameStart, tagNameEnd, partial));
        }

        AttributeValueSpan valueSpan = findAttributeValueAtCursor(text, tag, cursorIndex);
        if (valueSpan != null) {
            String partial = text.substring(valueSpan.valueStart, cursorIndex);
            return new TextSyntaxContext(
                SyntaxElementType.ATTRIBUTE_VALUE,
                valueSpan.valueStart,
                valueSpan.valueEnd,
                new MdxValueContext(
                    tag.name,
                    valueSpan.name,
                    valueSpan.valueStart,
                    valueSpan.valueEnd,
                    partial,
                    valueSpan.missingValueTerminator));
        }

        return resolveAttributeNameFromTag(text, tag.name, tag.nameStart - 1, findTagEnd(text, tag), cursorIndex);
    }

    @Nullable
    private TextSyntaxContext resolveAttributeValue(String text, String tagName, String attrName, int attrStart,
        int attrEnd, int cursorIndex) {
        int eqIdx = indexOf(text, '=', attrStart, attrEnd);
        if (eqIdx < 0) return null;

        int valueStart = skipSpaces(text, eqIdx + 1, attrEnd);
        if (valueStart > cursorIndex) return null;
        AttributeValueBounds bounds = valueBounds(text, valueStart, attrEnd);
        if (cursorIndex < bounds.valueStart || cursorIndex > bounds.valueEnd) return null;

        String partialText = text.substring(bounds.valueStart, cursorIndex);
        return new TextSyntaxContext(
            SyntaxElementType.ATTRIBUTE_VALUE,
            bounds.valueStart,
            bounds.valueEnd,
            new MdxValueContext(
                tagName,
                attrName,
                bounds.valueStart,
                bounds.valueEnd,
                partialText,
                bounds.missingTerminator));
    }

    @Nullable
    private TextSyntaxContext resolveAttributeNameFromTag(String text, String tagName, int tagStart, int tagEnd,
        int cursorIndex) {
        int scanStart = Math.max(tagStart + 1 + tagName.length(), 0);
        if (cursorIndex < scanStart || cursorIndex > tagEnd) return null;
        if (isInsideAnyAttributeValue(text, scanStart, tagEnd, cursorIndex)) return null;

        int nameStart = cursorIndex;
        while (nameStart > scanStart && isAttributeNameChar(text.charAt(nameStart - 1))) {
            nameStart--;
        }
        int nameEnd = cursorIndex;
        while (nameEnd < tagEnd && isAttributeNameChar(text.charAt(nameEnd))) {
            nameEnd++;
        }
        String partial = text.substring(nameStart, cursorIndex);
        return new TextSyntaxContext(
            SyntaxElementType.ATTRIBUTE_NAME,
            nameStart,
            nameEnd,
            new MdxAttrNameContext(tagName, nameStart, nameEnd, partial));
    }

    @Nullable
    private OpenTag findOpenTagBeforeCursor(String text, int cursorIndex) {
        int start = Math.min(cursorIndex, text.length()) - 1;
        for (int i = start; i >= 0; i--) {
            char c = text.charAt(i);
            if (c == '\n' || c == '\r') {
                break;
            }
            if (c == '>') {
                break;
            }
            if (c == '<') {
                if (i + 1 >= text.length() || !isTagNameStart(text.charAt(i + 1))) {
                    return null;
                }
                int nameStart = i + 1;
                int nameEnd = nameStart + 1;
                while (nameEnd < text.length() && isTagNameChar(text.charAt(nameEnd))) {
                    nameEnd++;
                }
                return new OpenTag(nameStart, text.substring(nameStart, nameEnd));
            }
        }
        return null;
    }

    @Nullable
    private AttributeValueSpan findAttributeValueAtCursor(String text, OpenTag tag, int cursorIndex) {
        int tagEnd = findTagEnd(text, tag);
        int pos = tag.nameStart + tag.name.length();
        while (pos < tagEnd) {
            pos = skipSpaces(text, pos, tagEnd);
            if (pos >= tagEnd || text.charAt(pos) == '/' || text.charAt(pos) == '>') break;
            if (!isAttributeNameStart(text.charAt(pos))) {
                pos++;
                continue;
            }
            int nameStart = pos;
            pos++;
            while (pos < tagEnd && isAttributeNameChar(text.charAt(pos))) {
                pos++;
            }
            String attrName = text.substring(nameStart, pos);
            int afterName = skipSpaces(text, pos, tagEnd);
            if (afterName >= tagEnd || text.charAt(afterName) != '=') {
                pos = afterName;
                continue;
            }
            int rawValueStart = skipSpaces(text, afterName + 1, tagEnd);
            AttributeValueBounds bounds = valueBounds(text, rawValueStart, tagEnd);
            if (cursorIndex >= bounds.valueStart && cursorIndex <= bounds.valueEnd) {
                return new AttributeValueSpan(attrName, bounds.valueStart, bounds.valueEnd, bounds.missingTerminator);
            }
            pos = Math.max(bounds.rawEnd, rawValueStart + 1);
        }
        return null;
    }

    private static boolean isInsideAnyAttributeValue(String text, int scanStart, int tagEnd, int cursorIndex) {
        int pos = scanStart;
        while (pos < tagEnd) {
            pos = skipSpaces(text, pos, tagEnd);
            if (pos >= tagEnd || !isAttributeNameStart(text.charAt(pos))) {
                pos++;
                continue;
            }
            pos++;
            while (pos < tagEnd && isAttributeNameChar(text.charAt(pos))) {
                pos++;
            }
            int afterName = skipSpaces(text, pos, tagEnd);
            if (afterName >= tagEnd || text.charAt(afterName) != '=') {
                pos = afterName;
                continue;
            }
            int rawValueStart = skipSpaces(text, afterName + 1, tagEnd);
            AttributeValueBounds bounds = valueBounds(text, rawValueStart, tagEnd);
            if (cursorIndex >= rawValueStart && cursorIndex <= bounds.valueEnd) {
                return true;
            }
            pos = Math.max(bounds.rawEnd, rawValueStart + 1);
        }
        return false;
    }

    private static int findTagEnd(String text, OpenTag tag) {
        int start = tag.nameStart + tag.name.length();
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '>' || c == '\n' || c == '\r') {
                return i;
            }
        }
        return text.length();
    }

    private static AttributeValueBounds valueBounds(String text, int rawValueStart, int limit) {
        if (rawValueStart >= limit) {
            return new AttributeValueBounds(rawValueStart, rawValueStart, rawValueStart, '\0');
        }
        char open = text.charAt(rawValueStart);
        if (open == '"' || open == '\'' || open == '{') {
            char close = open == '{' ? '}' : open;
            int valueStart = rawValueStart + 1;
            int rawEnd = findClosingValue(text, valueStart, limit, close);
            boolean closed = rawEnd < limit && text.charAt(rawEnd) == close;
            int rawValueEnd = closed ? rawEnd + 1 : rawEnd;
            return new AttributeValueBounds(valueStart, rawEnd, rawValueEnd, closed ? '\0' : close);
        }

        int rawEnd = rawValueStart;
        while (rawEnd < limit) {
            char c = text.charAt(rawEnd);
            if (Character.isWhitespace(c) || c == '>' || c == '/') {
                break;
            }
            rawEnd++;
        }
        return new AttributeValueBounds(rawValueStart, rawEnd, rawEnd, '\0');
    }

    private static int findClosingValue(String text, int start, int limit, char close) {
        for (int i = start; i < limit; i++) {
            char c = text.charAt(i);
            if (c == close || c == '>' || c == '\n' || c == '\r') {
                return i;
            }
        }
        return limit;
    }

    private static int indexOf(String text, char target, int start, int end) {
        for (int i = start; i < end; i++) {
            if (text.charAt(i) == target) return i;
        }
        return -1;
    }

    private static int skipSpaces(String text, int start, int end) {
        int pos = start;
        while (pos < end && text.charAt(pos) == ' ') {
            pos++;
        }
        return pos;
    }

    private static boolean isTagNameStart(char c) {
        return Character.isLetter(c);
    }

    private static boolean isTagNameChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.';
    }

    private static boolean isAttributeNameStart(char c) {
        return Character.isLetter(c) || c == '_' || c == ':';
    }

    private static boolean isAttributeNameChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == ':' || c == '.';
    }

    private static class OpenTag {

        private final int nameStart;
        private final String name;

        private OpenTag(int nameStart, String name) {
            this.nameStart = nameStart;
            this.name = name;
        }
    }

    private static class AttributeValueSpan {

        private final String name;
        private final int valueStart;
        private final int valueEnd;
        private final char missingValueTerminator;

        private AttributeValueSpan(String name, int valueStart, int valueEnd, char missingValueTerminator) {
            this.name = name;
            this.valueStart = valueStart;
            this.valueEnd = valueEnd;
            this.missingValueTerminator = missingValueTerminator;
        }
    }

    private static class AttributeValueBounds {

        private final int valueStart;
        private final int valueEnd;
        private final int rawEnd;
        private final char missingTerminator;

        private AttributeValueBounds(int valueStart, int valueEnd, int rawEnd, char missingTerminator) {
            this.valueStart = valueStart;
            this.valueEnd = valueEnd;
            this.rawEnd = rawEnd;
            this.missingTerminator = missingTerminator;
        }
    }
}
