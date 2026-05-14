package com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.GuideMarkdownOptions;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.SyntaxContextResolver;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.SyntaxElementType;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.SyntaxUtils;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.TextSyntaxContext;
import com.hfstudio.guidenh.libs.mdast.MdAst;
import com.hfstudio.guidenh.libs.mdast.MdastOptions;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstRoot;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxAttribute;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.micromark.ParseException;
import com.hfstudio.guidenh.libs.unist.UnistNode;
import com.hfstudio.guidenh.libs.unist.UnistPosition;

public class MdxSyntaxResolver implements SyntaxContextResolver {

    private static final MdastOptions PARSE_OPTIONS = GuideMarkdownOptions.runtime();

    // Matches open MDX tag: <TagName ... attrName="partialValue  (unclosed, no > required)
    // Group 1: tag name, Group 2: attribute name, Group 3: partial value text
    private static final Pattern FALLBACK_TAG =
        Pattern.compile("<([A-Za-z]\\w*)[^>]*?\\s+(\\w+)\\s*=\\s*\"([^\">]*)$");

    //
    // TODO: Fallback is intentionally limited for now.
    //
    // When the full-document AST parse throws ParseException (e.g. mismatched MDX tags,
    // unexpected closing slashes, etc.), the entire autocomplete system degrades to this
    // regex-based fallback. Currently it only covers the attribute-value-in-double-quotes
    // case. Missing cases that need parser-level error recovery first:
    //
    //   - Attribute-name completion inside an open tag  (<Entity |id=...>)
    //   - Single-quoted / brace-delimited / unquoted values
    //   - Cursor right after '=' with no opening quote yet
    //
    // Once the Micromark/MdastCompiler pipeline supports error-recovery (partial AST on
    // failure), extend resolveFromFallback to handle these cases. The extra patterns and
    // context-detection logic are ready but disabled until then.
    //

    // AST cache: re-parse only when text changes; cursor-only moves walk cached tree
    @Nullable
    private String cachedText;
    @Nullable
    private MdAstRoot cachedRoot;

    @Override
    @Nullable
    public TextSyntaxContext resolve(String text, int cursorIndex) {
        if (text == null || text.isEmpty()) return null;

        MdAstRoot root;
        if (text.equals(cachedText) && cachedRoot != null) {
            root = cachedRoot;
        } else {
            try {
                root = MdAst.fromMarkdown(text, PARSE_OPTIONS);
            } catch (ParseException e) {
                cachedText = null;
                cachedRoot = null;
                return resolveFromFallback(text, cursorIndex);
            }
            cachedText = text;
            cachedRoot = root;
        }

        return resolveFromAst(root, text, cursorIndex);
    }

    @Nullable
    private TextSyntaxContext resolveFromAst(MdAstRoot root, String text, int cursorIndex) {
        if (cursorIndex > 0 && text.charAt(cursorIndex - 1) == '<') {
            return new TextSyntaxContext(SyntaxElementType.TAG_START, cursorIndex, cursorIndex,
                new TagStartContext(cursorIndex, cursorIndex, ""));
        }
        MdxJsxElementFields element = findEnclosingMdxElement(root, cursorIndex);
        if (element != null) {
            return resolveMdxAttribute(element, text, cursorIndex);
        }
        return SyntaxUtils.resolveWord(text, cursorIndex);
    }

    @Nullable
    private MdxJsxElementFields findEnclosingMdxElement(UnistNode node, int cursorIndex) {
        UnistPosition pos = node.position();
        if (pos != null && pos.start() != null && pos.end() != null) {
            if (cursorIndex < pos.start().offset() || cursorIndex > pos.end().offset()) {
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

            int attrStart = pos.start().offset();
            int attrEnd = pos.end().offset();

            if (cursorIndex < attrStart || cursorIndex > attrEnd) continue;

            // Find the value range within the attribute text
            String attrText = text.substring(attrStart, attrEnd);
            int eqIdx = attrText.indexOf('=');
            if (eqIdx < 0) break; // boolean attribute, no value to complete

            int valRelStart = eqIdx + 1;
            while (valRelStart < attrText.length() && attrText.charAt(valRelStart) == ' ') {
                valRelStart++;
            }
            if (valRelStart >= attrText.length()) break;

            char openChar = attrText.charAt(valRelStart);
            char closeChar = (openChar == '{') ? '}' : openChar; // " ' or {
            if (openChar == '"' || openChar == '\'' || openChar == '{') {
                valRelStart++; // skip opening quote/brace
            } else {
                valRelStart = eqIdx + 1; // unquoted value (shouldn't happen in MDX)
                closeChar = ' ';
            }

            int valRelEnd = attrText.length();
            if (closeChar != ' ' && valRelEnd > 0 && attrText.charAt(valRelEnd - 1) == closeChar) {
                valRelEnd--; // exclude closing quote/brace
            }

            int valueStart = attrStart + valRelStart;
            int valueEnd = attrStart + valRelEnd;

            String partialText = text.substring(
                Math.max(valueStart, 0),
                Math.min(Math.max(cursorIndex, valueStart), text.length()));

            return new TextSyntaxContext(
                SyntaxElementType.ATTRIBUTE_VALUE,
                valueStart,
                valueEnd,
                new MdxValueContext(tagName, attr.name, valueStart, valueEnd, partialText));
        }

        // ATTRIBUTE_NAME detection: cursor inside tag body but not in any attribute.
        // AST-only — fallback cannot reliably distinguish tag-internal whitespace
        // from plain-text whitespace without false positives. We fail closed.
        // TODO: When parser supports error-recovery AST, enable fallback here.
        com.hfstudio.guidenh.libs.unist.UnistPosition elemPos = element.position();
        if (elemPos != null && elemPos.start() != null && elemPos.end() != null) {
            int elemEnd = elemPos.end().offset();
            if (cursorIndex > elemPos.start().offset() && cursorIndex < elemEnd) {
                String partial = text.substring(cursorIndex, Math.min(cursorIndex + 1, text.length()));
                return new TextSyntaxContext(SyntaxElementType.ATTRIBUTE_NAME,
                    cursorIndex, cursorIndex,
                    new MdxAttrNameContext(tagName, cursorIndex, cursorIndex, partial));
            }
        }

        return resolvePlainTextWord(text, cursorIndex);
    }

    private TextSyntaxContext resolvePlainTextWord(String text, int cursorIndex) {
        return SyntaxUtils.resolveWord(text, cursorIndex);
    }

    @Nullable
    private TextSyntaxContext resolveFromFallback(String text, int cursorIndex) {
        if (cursorIndex > 0 && text.charAt(cursorIndex - 1) == '<') {
            return new TextSyntaxContext(SyntaxElementType.TAG_START, cursorIndex, cursorIndex,
                new TagStartContext(cursorIndex, cursorIndex, ""));
        }
        String prefix = text.substring(0, Math.min(cursorIndex, text.length()));
        Matcher m = FALLBACK_TAG.matcher(prefix);

        String tagName = null;
        String attrName = null;
        int valueStart = -1;

        int searchFrom = 0;
        while (m.find(searchFrom)) {
            int vs = m.start(3);
            if (vs <= cursorIndex) {
                tagName = m.group(1);
                attrName = m.group(2);
                valueStart = vs;
            }
            searchFrom = m.start() + 1;
        }

        if (tagName != null && attrName != null && valueStart >= 0) {
            // Scan forward for the closing quote to determine replaceEnd
            int valueEnd = findCloseQuote(text, cursorIndex);
            String partialText = text.substring(valueStart, cursorIndex);
            return new TextSyntaxContext(
                SyntaxElementType.ATTRIBUTE_VALUE,
                valueStart,
                valueEnd,
                new MdxValueContext(tagName, attrName, valueStart, valueEnd, partialText));
        }

        return resolvePlainTextWord(text, cursorIndex);
    }

    /** Scan forward from pos for a closing double-quote, or fall back to next '>' or newline. */
    private static int findCloseQuote(String text, int pos) {
        int len = text.length();
        for (int i = pos; i < len; i++) {
            char c = text.charAt(i);
            if (c == '"') return i;
            if (c == '>' || c == '\n') return i;
        }
        return len;
    }
}
