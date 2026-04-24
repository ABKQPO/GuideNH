package com.hfstudio.guidenh.libs.mdast.model;

/**
 * ThematicBreak (Node) represents a thematic break, such as a scene change in a story, a transition to another topic,
 * or a new document.
 * ThematicBreak can be used where flow content is expected. It has no content model.
 * For example, the following markdown:
 * ***
 * Yields:
 * {type: 'thematicBreak'}
 */
public class MdAstThematicBreak extends MdAstNode implements MdAstFlowContent {

    public static final String TYPE = "thematicBreak";

    public MdAstThematicBreak() {
        super(TYPE);
    }

    @Override
    public void toText(StringBuilder buffer) {}
}
