package com.hfstudio.guidenh.guide.internal.markdown;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParagraph;
import com.hfstudio.guidenh.libs.mdast.model.MdAstText;

public final class MarkdownListSemantics {

    private static final Pattern TASK_PATTERN = Pattern.compile("^\\[( |x|X)]\\s+(.*)$");

    private MarkdownListSemantics() {}

    public static @Nullable TaskMarker extractTaskMarker(List<? extends MdAstAnyContent> children) {
        if (children.size() != 1 || !(children.get(0) instanceof MdAstParagraph paragraph)) {
            return null;
        }
        if (paragraph.children()
            .isEmpty()) {
            return null;
        }
        if (!(paragraph.children()
            .get(0) instanceof MdAstText text)) {
            return null;
        }

        Matcher matcher = TASK_PATTERN.matcher(text.value);
        if (!matcher.matches()) {
            return null;
        }

        return new TaskMarker(!" ".equals(matcher.group(1)), matcher.group(2));
    }

    @Desugar
    public record TaskMarker(boolean checked, String remainingText) {}
}
