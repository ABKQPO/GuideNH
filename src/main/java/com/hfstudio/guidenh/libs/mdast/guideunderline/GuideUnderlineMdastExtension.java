package com.hfstudio.guidenh.libs.mdast.guideunderline;

import com.hfstudio.guidenh.libs.mdast.MdastContext;
import com.hfstudio.guidenh.libs.mdast.MdastExtension;
import com.hfstudio.guidenh.libs.micromark.Token;

/**
 * 把 {@link com.hfstudio.guidenh.libs.micromark.extensions.guideunderline.GuideUnderlineSyntax} 产生的
 * micromark token 转换为对应的 mdast 节点。
 */
public class GuideUnderlineMdastExtension {

    public static final MdastExtension INSTANCE = MdastExtension.builder()
        .canContainEol("guideUnderline", "guideWavyUnderline", "guideDottedUnderline")
        .enter("guideUnderline", GuideUnderlineMdastExtension::enterUnderline)
        .exit("guideUnderline", GuideUnderlineMdastExtension::exitAny)
        .enter("guideWavyUnderline", GuideUnderlineMdastExtension::enterWavy)
        .exit("guideWavyUnderline", GuideUnderlineMdastExtension::exitAny)
        .enter("guideDottedUnderline", GuideUnderlineMdastExtension::enterDotted)
        .exit("guideDottedUnderline", GuideUnderlineMdastExtension::exitAny)
        .build();

    private GuideUnderlineMdastExtension() {}

    public static void enterUnderline(MdastContext context, Token token) {
        context.enter(new MdAstUnderline(), token);
    }

    public static void enterWavy(MdastContext context, Token token) {
        context.enter(new MdAstWavyUnderline(), token);
    }

    public static void enterDotted(MdastContext context, Token token) {
        context.enter(new MdAstDottedUnderline(), token);
    }

    public static void exitAny(MdastContext context, Token token) {
        context.exit(token);
    }
}
