package com.hfstudio.guidenh.libs.mdast.model;

/**
 * Blockquote (Parent) represents a section quoted from somewhere else.
 * Blockquote can be used where flow content is expected. Its content model is also flow content.
 * For example, the following markdown:
 * > Alpha bravo charlie.
 * Yields:
 * { type: 'blockquote', children: [{ type: 'paragraph', children: [{type: 'text', value: 'Alpha bravo charlie.'}] }] }
 */
public class MdAstBlockquote extends MdAstParent<MdAstFlowContent> implements MdAstFlowContent {

    public static final String TYPE = "blockquote";

    public MdAstBlockquote() {
        super(TYPE);
    }

    @Override
    protected Class<MdAstFlowContent> childClass() {
        return MdAstFlowContent.class;
    }
}
