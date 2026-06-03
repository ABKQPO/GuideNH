package com.hfstudio.guidenh.guide.internal.markdown;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstText;

public class MarkdownListSemantics {

    private static final Pattern TASK_PATTERN = Pattern.compile("^\\[( |x|X)]\\s+(.*)$");

    private MarkdownListSemantics() {}

    public static @Nullable TaskMarker extractTaskMarker(List<? extends MdAstAnyContent> children) {
        if (children.size() != 1) {
            return null;
        }
        MdAstAnyContent firstChild = children.get(0);
        // Post-conversion: <p> element wrapping the task text
        if (firstChild instanceof MdxJsxFlowElement && "p".equals(((MdxJsxFlowElement) firstChild).name())) {
            MdxJsxFlowElement p = (MdxJsxFlowElement) firstChild;
            if (p.children()
                .isEmpty()) {
                return null;
            }
            if (p.children()
                .get(0) instanceof MdAstText) {
                MdAstText text = (MdAstText) p.children()
                    .get(0);
                Matcher matcher = TASK_PATTERN.matcher(text.value);
                if (matcher.matches()) {
                    return new TaskMarker(!" ".equals(matcher.group(1)), matcher.group(2));
                }
            }
        }
        return null;
    }

    @Desugar
    public record TaskMarker(boolean checked, String remainingText) {}
}
