package com.hfstudio.guidenh.libs.mdast.guideunderline;

import com.hfstudio.guidenh.libs.mdast.model.MdAstParent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstPhrasingContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstStaticPhrasingContent;

/**
 * 行内"直下划线"节点，对应 Markdown 语法 {@code ++text++}。内容模型与 strong / emphasis 一致。
 */
public class MdAstUnderline extends MdAstParent<MdAstPhrasingContent> implements MdAstStaticPhrasingContent {

    public static final String TYPE = "guideUnderline";

    public MdAstUnderline() {
        super(TYPE);
    }

    @Override
    protected Class<MdAstPhrasingContent> childClass() {
        return MdAstPhrasingContent.class;
    }
}
