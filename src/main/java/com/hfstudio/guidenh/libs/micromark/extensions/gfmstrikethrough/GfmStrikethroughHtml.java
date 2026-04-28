package com.hfstudio.guidenh.libs.micromark.extensions.gfmstrikethrough;

import com.hfstudio.guidenh.libs.micromark.html.HtmlExtension;

public class GfmStrikethroughHtml {

    private GfmStrikethroughHtml() {}

    public static final HtmlExtension EXTENSION = HtmlExtension.builder()
        .enter("strikethrough", (context, token) -> context.tag("<del>"))
        .exit("strikethrough", (context, token) -> context.tag("</del>"))
        .build();
}
