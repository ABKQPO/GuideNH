package com.hfstudio.guidenh.guide.document.block;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.guide.document.flow.LytFlowText;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.CodeHighlightLine;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.CodeHighlightResult;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.CodeHighlightTheme;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.CodeHighlightToken;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.CodeTokenType;

public class CodeHighlightFlowBuilder {

    private final Map<CodeTokenType, ConstantColor> colors;

    public CodeHighlightFlowBuilder(CodeHighlightTheme theme) {
        colors = buildColors(theme);
    }

    public List<LytFlowSpan> buildLines(CodeHighlightResult result) {
        List<LytFlowSpan> lines = new ArrayList<>(
            Math.max(
                1,
                result.lines()
                    .size()));
        for (CodeHighlightLine line : result.lines()) {
            lines.add(buildLine(line));
        }
        if (lines.isEmpty()) {
            lines.add(new LytFlowSpan());
        }
        return lines;
    }

    private LytFlowSpan buildLine(CodeHighlightLine line) {
        LytFlowSpan span = new LytFlowSpan();
        for (CodeHighlightToken token : line.tokens()) {
            var node = LytFlowText.of(token.text());
            node.modifyStyle(style -> style.color(colorOf(token.type())));
            span.append(node);
        }
        return span;
    }

    private ConstantColor colorOf(CodeTokenType type) {
        return colors.getOrDefault(type, colors.get(CodeTokenType.PLAIN));
    }

    private Map<CodeTokenType, ConstantColor> buildColors(CodeHighlightTheme theme) {
        Map<CodeTokenType, ConstantColor> result = new EnumMap<>(CodeTokenType.class);
        for (CodeTokenType type : CodeTokenType.values()) {
            result.put(type, new ConstantColor(theme.colorOf(type)));
        }
        return Map.copyOf(result);
    }
}
