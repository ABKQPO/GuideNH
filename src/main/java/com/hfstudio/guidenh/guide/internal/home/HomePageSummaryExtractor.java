package com.hfstudio.guidenh.guide.internal.home;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstRoot;
import com.hfstudio.guidenh.libs.mdast.model.MdAstText;

public class HomePageSummaryExtractor {

    public String extract(@Nullable ParsedGuidePage page) {
        MdAstRoot root = page != null ? page.getAstRoot() : null;
        if (root == null) {
            return "";
        }

        for (MdAstAnyContent block : root.children()) {
            if (isHeading(block)) {
                continue;
            }
            if (!isParagraph(block)) {
                continue;
            }
            String text = normalizeWhitespace(extractText((MdxJsxElementFields) block));
            if (!text.isEmpty()) {
                return text;
            }
        }
        return "";
    }

    public String extractHeadingText(@Nullable ParsedGuidePage page) {
        MdAstRoot root = page != null ? page.getAstRoot() : null;
        if (root == null) {
            return "";
        }

        for (MdAstAnyContent block : root.children()) {
            if (isHeading(block)) {
                String text = normalizeWhitespace(extractText((MdxJsxElementFields) block));
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }

        return "";
    }

    private static boolean isHeading(MdAstAnyContent block) {
        if (block instanceof MdxJsxElementFields el) {
            String name = el.name();
            return name != null && name.length() == 2
                && name.charAt(0) == 'h'
                && name.charAt(1) >= '1'
                && name.charAt(1) <= '6';
        }
        return false;
    }

    private static boolean isParagraph(MdAstAnyContent block) {
        return block instanceof MdxJsxElementFields el && "p".equals(el.name());
    }

    private static String extractText(MdxJsxElementFields el) {
        StringBuilder sb = new StringBuilder();
        collectText(el, sb);
        return sb.toString();
    }

    private static void collectText(MdxJsxElementFields el, StringBuilder sb) {
        for (Object child : el.children()) {
            if (child instanceof MdAstText) {
                sb.append(((MdAstText) child).value);
            } else if (child instanceof MdxJsxElementFields) {
                collectText((MdxJsxElementFields) child, sb);
            }
        }
    }

    private String normalizeWhitespace(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder normalized = new StringBuilder(text.length());
        boolean previousWhitespace = true;
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (Character.isWhitespace(current)) {
                if (!previousWhitespace) {
                    normalized.append(' ');
                    previousWhitespace = true;
                }
                continue;
            }
            normalized.append(current);
            previousWhitespace = false;
        }
        int length = normalized.length();
        if (length > 0 && normalized.charAt(length - 1) == ' ') {
            normalized.setLength(length - 1);
        }
        return normalized.toString();
    }
}
