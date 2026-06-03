package com.hfstudio.guidenh.guide.layout.flow;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.render.RenderContext;

public abstract class LineElement {

    /**
     * Next Element in flow direction.
     */
    @Nullable
    public LineElement next;

    public LytRect bounds = LytRect.empty();

    /**
     * The original flow content this line element is associated with.
     */
    @Nullable
    public LytFlowContent flowContent;

    public boolean containsMouse;
    public boolean revealedBySpoiler;

    public boolean floating;

    @Nullable
    public LytFlowContent getFlowContent() {
        return flowContent;
    }

    /**
     * Render any other content individually.
     */
    public void render(RenderContext context) {}
}
