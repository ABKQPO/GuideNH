package com.hfstudio.guidenh.libs.mdx;

import java.util.List;

import com.hfstudio.guidenh.libs.micromark.Extension;
import com.hfstudio.guidenh.libs.micromark.symbol.Codes;

public class MdxSyntax {

    public static final Extension INSTANCE = new Extension();

    static {
        INSTANCE.flow.put(Codes.lessThan, List.of(JsxFlow.INSTANCE));
        INSTANCE.text.put(Codes.lessThan, List.of(JsxText.INSTANCE));
    }

}
