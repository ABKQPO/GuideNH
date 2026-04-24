package com.hfstudio.guidenh.libs.mdast.model;

/**
 * Represents a fragment of raw HTML.
 * HTML can be used where flow or phrasing content is expected. Its content is represented by its value field.
 * HTML nodes do not have the restriction of being valid or complete HTML ([HTML]) constructs.
 * For example, the following markdown:
 * &lt;div>
 * Yields:
 * {type: 'html', value: '&lt;div>'}
 */
public class MdAstHTML extends MdAstLiteral implements MdAstFlowContent, MdAstStaticPhrasingContent {

    public static final String TYPE = "html";

    public MdAstHTML() {
        super(TYPE);
    }
}
