package com.hfstudio.guidenh.guide.document.flow;

import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.interaction.FlowInteractionPath;

public interface LytFlowContainer extends LytFlowParent {

    /**
     * Gets a stream of all the bounding rectangles for given flow content. Since flow content may be wrapped, it may
     * consist of several disjointed bounding boxes.
     */
    Stream<LytRect> enumerateContentBounds(LytFlowContent content);

    @Nullable
    FlowInteractionPath pickContent(int x, int y);
}
