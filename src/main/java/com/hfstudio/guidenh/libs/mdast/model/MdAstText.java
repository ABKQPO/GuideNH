package com.hfstudio.guidenh.libs.mdast.model;

/**
 * Represents everything that is just text.
 * Text can be used where phrasing content is expected. Its content is represented by its value field.
 * For example, the following markdown:
 * Alpha bravo charlie.
 * Yields:
 * {type: 'text', value: 'Alpha bravo charlie.'}
 */
public class MdAstText extends MdAstLiteral implements MdAstStaticPhrasingContent {

    public static final String TYPE = "text";

    public MdAstText() {
        super(TYPE);
    }
}
