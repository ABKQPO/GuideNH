package com.hfstudio.guidenh.guide.internal.markdown;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstBlockquote;
import com.hfstudio.guidenh.libs.mdast.model.MdAstInlineCode;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParagraph;
import com.hfstudio.guidenh.libs.mdast.model.MdAstText;

public final class MarkdownRuntimeBlocks {

    private MarkdownRuntimeBlocks() {}

    public static @Nullable GithubAlertBlock extractGithubAlert(MdAstBlockquote blockquote) {
        String firstText = getLeadingBlockquoteText(blockquote);
        if (firstText == null || !firstText.contains("[!")) {
            return null;
        }

        GithubAlertType type = GithubAlertType.fromDirective(firstText);
        if (type == null) {
            return null;
        }

        List<MdAstAnyContent> children = new ArrayList<>(blockquote.children());
        String remainingText = firstText.substring(firstText.indexOf(']') + 1)
            .trim();
        return new GithubAlertBlock(type, children, remainingText);
    }

    private static @Nullable String getLeadingBlockquoteText(MdAstBlockquote blockquote) {
        for (var child : blockquote.children()) {
            if (child instanceof MdAstParagraph paragraph) {
                String text = getLeadingParagraphText(paragraph);
                if (text != null && !text.trim()
                    .isEmpty()) {
                    return text;
                }
            } else if (child instanceof MdAstText text && !text.value.trim()
                .isEmpty()) {
                    return text.value;
                }
        }
        return null;
    }

    private static @Nullable String getLeadingParagraphText(MdAstParagraph paragraph) {
        for (var child : paragraph.children()) {
            if (child instanceof MdAstText text && !text.value.trim()
                .isEmpty()) {
                return text.value;
            }
            if (child instanceof MdAstInlineCode code && !code.value.trim()
                .isEmpty()) {
                return code.value;
            }
        }
        return null;
    }

    @Desugar
    public record GithubAlertBlock(GithubAlertType type, List<MdAstAnyContent> children, String remainingText) {}
}
