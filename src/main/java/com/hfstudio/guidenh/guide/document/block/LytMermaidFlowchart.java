package com.hfstudio.guidenh.guide.document.block;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartDocument;
import com.hfstudio.guidenh.guide.style.BorderStyle;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;

import lombok.Getter;

public class LytMermaidFlowchart extends LytVBox implements InteractiveElement {

    @Getter
    private final FlowchartDocument flowchart;
    @Getter
    private final String sourceText;
    @Getter
    private final LytCodeBlockToolbar toolbar = new LytCodeBlockToolbar();
    private final LytMermaidFlowchartCanvas canvas;

    public LytMermaidFlowchart(FlowchartDocument flowchart, String sourceText) {
        this(flowchart, sourceText, Collections.emptyMap());
    }

    public LytMermaidFlowchart(FlowchartDocument flowchart, String sourceText, Map<String, LytBlock> nodeContent) {
        this.flowchart = flowchart;
        this.sourceText = sourceText != null ? sourceText : "";
        this.canvas = new LytMermaidFlowchartCanvas(
            flowchart,
            nodeContent != null ? nodeContent : Collections.emptyMap());

        setPadding(6);
        setGap(4);
        setBackgroundColor(SymbolicColor.BLOCKQUOTE_BACKGROUND);
        setBorder(new BorderStyle(SymbolicColor.TABLE_BORDER, 1));

        toolbar.setLanguageDisplayName("Flowchart");
        toolbar.setCopyText(this.sourceText);
        toolbar.setCopyButtonVisible(true);

        append(toolbar);
        append(canvas);
    }

    public void setPreferredSize(int width, int height) {
        canvas.setPreferredSize(width, height);
        toolbar.setPreferredWidth(width);
    }

    @Override
    public boolean mouseClicked(GuideUiHost screen, int x, int y, int button, boolean doubleClick) {
        return toolbar.mouseClicked(screen, x, y, button, doubleClick)
            || canvas.mouseClicked(screen, x, y, button, doubleClick);
    }

    @Override
    public Optional<GuideTooltip> getTooltip(float x, float y) {
        Optional<GuideTooltip> toolbarTooltip = toolbar.getTooltip(x, y);
        return toolbarTooltip.isPresent() ? toolbarTooltip : canvas.getTooltip(x, y);
    }
}
