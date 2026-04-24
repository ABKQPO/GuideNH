package com.hfstudio.guidenh.libs.mdast.model;

/**
 * Break (Node) represents a line break, such as in poems or addresses.
 * Break can be used where phrasing content is expected. It has no content model.
 * For example, the following markdown:
 * foo·· bar
 * Yields:
 * { type: 'paragraph', children: [ {type: 'text', value: 'foo'}, {type: 'break'}, {type: 'text', value: 'bar'} ] }
 */
public class MdAstBreak extends MdAstNode implements MdAstStaticPhrasingContent {

    public static final String TYPE = "break";

    public MdAstBreak() {
        super(TYPE);
    }

    @Override
    public void toText(StringBuilder buffer) {}
}
