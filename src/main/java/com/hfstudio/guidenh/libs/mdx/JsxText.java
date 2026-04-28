package com.hfstudio.guidenh.libs.mdx;

import com.hfstudio.guidenh.libs.micromark.Construct;
import com.hfstudio.guidenh.libs.micromark.State;
import com.hfstudio.guidenh.libs.micromark.TokenizeContext;
import com.hfstudio.guidenh.libs.micromark.Tokenizer;

public class JsxText {

    public static final Construct INSTANCE = new Construct();

    static {
        INSTANCE.tokenize = JsxText::tokenize;
    }

    public static State tokenize(TokenizeContext context, Tokenizer.Effects effects, State ok, State nok) {
        return FactoryTag.create(
            context,
            effects,
            ok,
            nok,
            true,
            "mdxJsxTextTag",
            "mdxJsxTextTagMarker",
            "mdxJsxTextTagClosingMarker",
            "mdxJsxTextTagSelfClosingMarker",
            "mdxJsxTextTagName",
            "mdxJsxTextTagNamePrimary",
            "mdxJsxTextTagNameMemberMarker",
            "mdxJsxTextTagNameMember",
            "mdxJsxTextTagNamePrefixMarker",
            "mdxJsxTextTagNameLocal",
            "mdxJsxTextTagExpressionAttribute",
            "mdxJsxTextTagExpressionAttributeMarker",
            "mdxJsxTextTagExpressionAttributeValue",
            "mdxJsxTextTagAttribute",
            "mdxJsxTextTagAttributeName",
            "mdxJsxTextTagAttributeNamePrimary",
            "mdxJsxTextTagAttributeNamePrefixMarker",
            "mdxJsxTextTagAttributeNameLocal",
            "mdxJsxTextTagAttributeInitializerMarker",
            "mdxJsxTextTagAttributeValueLiteral",
            "mdxJsxTextTagAttributeValueLiteralMarker",
            "mdxJsxTextTagAttributeValueLiteralValue",
            "mdxJsxTextTagAttributeValueExpression",
            "mdxJsxTextTagAttributeValueExpressionMarker",
            "mdxJsxTextTagAttributeValueExpressionValue");
    }

}
