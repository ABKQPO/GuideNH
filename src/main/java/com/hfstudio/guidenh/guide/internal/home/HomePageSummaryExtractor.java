package com.hfstudio.guidenh.guide.internal.home;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstHeading;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParagraph;
import com.hfstudio.guidenh.libs.mdast.model.MdAstRoot;

public class HomePageSummaryExtractor {

    public String extract(@Nullable ParsedGuidePage page) {
        MdAstRoot root = page != null ? page.getAstRoot() : null;
        if (root == null) {
            return "";
        }

        for (MdAstAnyContent block : root.children()) {
            if (block instanceof MdAstHeading) {
                continue;
            }
            if (!(block instanceof MdAstParagraph paragraph)) {
                continue;
            }
            String text = normalizeWhitespace(paragraph.toText());
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
            if (block instanceof MdAstHeading heading) {
                String text = normalizeWhitespace(heading.toText());
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }

        return "";
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
