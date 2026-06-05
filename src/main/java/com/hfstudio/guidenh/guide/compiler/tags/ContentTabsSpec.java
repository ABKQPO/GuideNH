package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.libs.unist.UnistNode;

@Desugar
public record ContentTabsSpec(@Nullable String defaultTitle, @Nullable Integer defaultIndex, List<TabEntry> tabs,
    List<ValidationIssue> issues) {

    public boolean hasRenderableTabs() {
        return !tabs.isEmpty();
    }

    @Desugar
    public record TabEntry(String title, LytBlock body, UnistNode sourceNode) {}

    @Desugar
    public record ValidationIssue(String message, UnistNode sourceNode) {}
}
