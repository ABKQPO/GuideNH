package com.hfstudio.guidenh.libs.mdast.model;

/**
 * InlineCode (Literal) represents a fragment of computer code, such as a file name, computer program, or anything a
 * computer could parse.
 * InlineCode can be used where phrasing content is expected. Its content is represented by its value field.
 * This node relates to the flow content concept Code.
 * For example, the following markdown:
 * `foo()`
 * Yields:
 * {type: 'inlineCode', value: 'foo()'}
 */
public class MdAstInlineCode extends MdAstLiteral implements MdAstStaticPhrasingContent {

    public static final String TYPE = "inlineCode";

    public MdAstInlineCode() {
        super(TYPE);
    }
}
