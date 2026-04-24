package com.hfstudio.guidenh.guide.compiler;

import com.hfstudio.guidenh.libs.unist.UnistNode;

/**
 * Sink for indexing page content.
 */
public interface IndexingSink {

    void appendText(UnistNode parent, String text);

    void appendBreak();
}
