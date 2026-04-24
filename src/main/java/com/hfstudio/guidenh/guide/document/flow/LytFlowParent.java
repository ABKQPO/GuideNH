package com.hfstudio.guidenh.guide.document.flow;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.libs.unist.UnistNode;

public interface LytFlowParent extends LytErrorSink {

    void append(LytFlowContent child);

    default LytFlowText appendText(String text) {
        var node = new LytFlowText();
        node.setText(text);
        append(node);
        return node;
    }

    /**
     * Converts formatted Minecraft text into our flow content.
     */
    default void appendComponent(String formattedText) {
        appendText(formattedText);
    }

    default void appendBreak() {
        var br = new LytFlowBreak();
        append(br);
    }

    @Override
    default void appendError(PageCompiler compiler, String text, UnistNode node) {
        append(compiler.createErrorFlowContent(text, node));
    }
}
