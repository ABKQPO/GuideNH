package com.hfstudio.guidenh.guide.document.block;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidMindmapDocument;
import com.hfstudio.guidenh.guide.style.BorderStyle;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;

public class LytMermaidMindmap extends LytVBox implements InteractiveElement {

    private final MermaidMindmapDocument mindmap;
    private final String sourceText;
    private final LytCodeBlockToolbar toolbar = new LytCodeBlockToolbar();
    private final LytMermaidMindmapCanvas canvas;

    public LytMermaidMindmap(MermaidMindmapDocument mindmap, String sourceText) {
        this(mindmap, sourceText, Collections.<String, LytBlock>emptyMap());
    }

    public LytMermaidMindmap(MermaidMindmapDocument mindmap, String sourceText, Map<String, LytBlock> nodeContent) {
        this.mindmap = mindmap;
        this.sourceText = sourceText != null ? sourceText : "";
        this.canvas = new LytMermaidMindmapCanvas(
            mindmap,
            nodeContent != null ? nodeContent : Collections.<String, LytBlock>emptyMap());

        setPadding(6);
        setGap(4);
        setBackgroundColor(SymbolicColor.BLOCKQUOTE_BACKGROUND);
        setBorder(new BorderStyle(SymbolicColor.TABLE_BORDER, 1));

        toolbar.setLanguageDisplayName("Mermaid");
        toolbar.setCopyText(this.sourceText);

        append(toolbar);
        append(canvas);
    }

    public MermaidMindmapDocument getMindmap() {
        return mindmap;
    }

    public String getSourceText() {
        return sourceText;
    }

    public LytCodeBlockToolbar getToolbar() {
        return toolbar;
    }

    public LytMermaidMindmapCanvas getCanvas() {
        return canvas;
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
