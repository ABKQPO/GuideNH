package com.hfstudio.guidenh.libs.mdx;

import java.util.Collections;

import com.hfstudio.guidenh.libs.micromark.Extension;
import com.hfstudio.guidenh.libs.micromark.symbol.Codes;

public class MdxSyntax {

    public static final Extension INSTANCE = new Extension();

    static {
        INSTANCE.flow.put(Codes.lessThan, Collections.singletonList(JsxFlow.INSTANCE));
        INSTANCE.text.put(Codes.lessThan, Collections.singletonList(JsxText.INSTANCE));

        // See https://github.com/micromark/micromark-extension-mdx-md/blob/main/index.js
        Collections.addAll(INSTANCE.nullDisable, "autolink", "codeIndented", "htmlFlow", "htmlText");
    }

}
