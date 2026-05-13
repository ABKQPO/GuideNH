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
                new MdxAutocompleteContext(tagName, attr.name, valueStart, valueEnd, partialText));
        }

        return resolvePlainTextWord(text, cursorIndex);
    }

    private TextSyntaxContext resolvePlainTextWord(String text, int cursorIndex) {
        return SyntaxUtils.resolveWord(text, cursorIndex);
    }

    @Nullable
    private TextSyntaxContext resolveFromFallback(String text, int cursorIndex) {
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
                new MdxAutocompleteContext(tagName, attrName, valueStart, valueEnd, partialText));
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
