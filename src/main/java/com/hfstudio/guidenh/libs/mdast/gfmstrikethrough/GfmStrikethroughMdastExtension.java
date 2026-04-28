package com.hfstudio.guidenh.libs.mdast.gfmstrikethrough;

import com.hfstudio.guidenh.libs.mdast.MdastContext;
import com.hfstudio.guidenh.libs.mdast.MdastExtension;
import com.hfstudio.guidenh.libs.micromark.Token;

public class GfmStrikethroughMdastExtension {

    public static final MdastExtension INSTANCE = MdastExtension.builder()
        .canContainEol("delete")
        .enter("strikethrough", GfmStrikethroughMdastExtension::enterStrikethrough)
        .exit("strikethrough", GfmStrikethroughMdastExtension::exitStrikethrough)
        .build();

    private GfmStrikethroughMdastExtension() {}

    public static void enterStrikethrough(MdastContext context, Token token) {
        context.enter(new MdAstDelete(), token);
    }

    public static void exitStrikethrough(MdastContext context, Token token) {
        context.exit(token);
    }
}
