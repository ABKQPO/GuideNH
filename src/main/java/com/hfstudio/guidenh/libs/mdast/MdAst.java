package com.hfstudio.guidenh.libs.mdast;

import com.hfstudio.guidenh.libs.mdast.model.MdAstRoot;
import com.hfstudio.guidenh.libs.micromark.Micromark;

public final class MdAst {

    private MdAst() {}

    public static MdAstRoot fromMarkdown(String markdown, MdastOptions options) {
        var evts = Micromark.parseAndPostprocess(markdown, options);
        return new MdastCompiler(options).compile(evts);
    }
}
