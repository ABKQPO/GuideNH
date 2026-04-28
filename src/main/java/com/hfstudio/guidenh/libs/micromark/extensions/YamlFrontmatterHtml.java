package com.hfstudio.guidenh.libs.micromark.extensions;

import com.hfstudio.guidenh.libs.micromark.Token;
import com.hfstudio.guidenh.libs.micromark.html.HtmlContext;
import com.hfstudio.guidenh.libs.micromark.html.HtmlExtension;

public class YamlFrontmatterHtml {

    public static final HtmlExtension INSTANCE = HtmlExtension.builder()
        .enter("yaml", YamlFrontmatterHtml::enter)
        .exit("yaml", YamlFrontmatterHtml::exit)
        .build();

    public static void enter(HtmlContext context, Token token) {
        context.buffer();
    }

    public static void exit(HtmlContext context, Token token) {
        context.resume();
        context.setSlurpOneLineEnding(true);
    }

}
