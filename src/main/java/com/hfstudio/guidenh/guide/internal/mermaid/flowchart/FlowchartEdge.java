package com.hfstudio.guidenh.guide.internal.mermaid.flowchart;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.mermaid.MermaidArrowHead;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidEdgeStyle;

public class FlowchartEdge {

    private final String from;
    private final String to;
    @Nullable
    private final String label;
    private final MermaidEdgeStyle style;
    private final boolean arrowFwd;
    private final boolean arrowRev;
    private final MermaidArrowHead forwardHead;
    private final MermaidArrowHead reverseHead;
    @Nullable
    private final String edgeId;
    private final int length;
    @Nullable
    private final String styleOverride;

    public FlowchartEdge(String from, String to, @Nullable String label, MermaidEdgeStyle style, boolean arrowFwd,
        boolean arrowRev, MermaidArrowHead forwardHead, MermaidArrowHead reverseHead, @Nullable String edgeId,
        int length) {
        this(from, to, label, style, arrowFwd, arrowRev, forwardHead, reverseHead, edgeId, length, null);
    }

    public FlowchartEdge(String from, String to, @Nullable String label, MermaidEdgeStyle style, boolean arrowFwd,
        boolean arrowRev, MermaidArrowHead forwardHead, MermaidArrowHead reverseHead, @Nullable String edgeId,
        int length, @Nullable String styleOverride) {
        this.from = Objects.requireNonNullElse(from, "");
        this.to = Objects.requireNonNullElse(to, "");
        this.label = label;
        this.style = style != null ? style : MermaidEdgeStyle.SOLID;
        this.arrowFwd = arrowFwd;
        this.arrowRev = arrowRev;
        this.forwardHead = forwardHead != null ? forwardHead : MermaidArrowHead.NONE;
        this.reverseHead = reverseHead != null ? reverseHead : MermaidArrowHead.NONE;
        this.edgeId = edgeId;
        this.length = Math.max(0, length);
        this.styleOverride = styleOverride;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public @Nullable String getLabel() {
        return label;
    }

    public MermaidEdgeStyle getStyle() {
        return style;
    }

    public boolean isArrowFwd() {
        return arrowFwd;
    }

    public boolean isArrowRev() {
        return arrowRev;
    }

    public MermaidArrowHead getForwardHead() {
        return forwardHead;
    }

    public MermaidArrowHead getReverseHead() {
        return reverseHead;
    }

    public @Nullable String getEdgeId() {
        return edgeId;
    }

    public int getLength() {
        return length;
    }

    public @Nullable String getStyleOverride() {
        return styleOverride;
    }
}
