package com.hfstudio.guidenh.libs.mdast.model;

/**
 * Paragraph (Parent) represents a unit of discourse dealing with a particular point or idea.
 * Paragraph can be used where content is expected. Its content model is phrasing content.
 * For example, the following markdown:
 * Alpha bravo charlie.
 * Yields:
 * { type: 'paragraph', children: [{type: 'text', value: 'Alpha bravo charlie.'}] }
 */
public class MdAstParagraph extends MdAstParent<MdAstPhrasingContent> implements MdAstContent {

    public static final String TYPE = "paragraph";

    public MdAstParagraph() {
        super(TYPE);
    }

    @Override
    protected Class<MdAstPhrasingContent> childClass() {
        return MdAstPhrasingContent.class;
    }
}
