package com.hfstudio.guidenh.libs.mdast.model;

/**
 * Strong (Parent) represents strong importance, seriousness, or urgency for its contents.
 * Strong can be used where phrasing content is expected. Its content model is transparent content.
 * For example, the following markdown:
 * **alpha** __bravo__
 * Yields:
 * 
 * <pre>
 * {type:'paragraph',children:[{type:'strong',children:[{type:'text',value:'alpha'}]},{type:'text',value:' '},{type:'strong',children:[{type:'text',value:'bravo'}]}]}
 * </pre>
 */
public class MdAstStrong extends MdAstParent<MdAstPhrasingContent> implements MdAstStaticPhrasingContent {

    public static final String TYPE = "strong";

    public MdAstStrong() {
        super(TYPE);
    }

    @Override
    protected Class<MdAstPhrasingContent> childClass() {
        return MdAstPhrasingContent.class;
    }
}
