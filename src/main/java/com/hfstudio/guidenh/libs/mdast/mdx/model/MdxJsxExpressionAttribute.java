package com.hfstudio.guidenh.libs.mdast.mdx.model;

import com.hfstudio.guidenh.libs.mdast.model.MdAstLiteral;

public class MdxJsxExpressionAttribute extends MdAstLiteral implements MdxJsxAttributeNode {

    public static final String TYPE = "mdxJsxExpressionAttribute";

    public MdxJsxExpressionAttribute() {
        super(TYPE);
    }
}
