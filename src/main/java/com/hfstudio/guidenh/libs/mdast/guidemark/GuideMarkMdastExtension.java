package com.hfstudio.guidenh.libs.mdast.guidemark;

import com.hfstudio.guidenh.libs.mdast.MdastContext;
import com.hfstudio.guidenh.libs.mdast.MdastExtension;
import com.hfstudio.guidenh.libs.micromark.Token;

public class GuideMarkMdastExtension {

    public static final MdastExtension INSTANCE = MdastExtension.builder()
        .canContainEol("guideMark")
        .enter("guideMark", GuideMarkMdastExtension::enterMark)
        .exit("guideMark", GuideMarkMdastExtension::exitMark)
        .build();

    private GuideMarkMdastExtension() {}

    public static void enterMark(MdastContext context, Token token) {
        context.enter(new MdAstMark(), token);
    }

    public static void exitMark(MdastContext context, Token token) {
        context.exit(token);
    }
}
