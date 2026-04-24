package com.hfstudio.guidenh.guide.document;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.libs.unist.UnistNode;

public interface LytErrorSink {

    void appendError(PageCompiler compiler, String text, UnistNode node);
}
