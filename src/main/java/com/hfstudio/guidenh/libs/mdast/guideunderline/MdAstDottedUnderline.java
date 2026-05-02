package com.hfstudio.guidenh.libs.mdast.guideunderline;

import com.hfstudio.guidenh.libs.mdast.model.MdAstParent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstPhrasingContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstStaticPhrasingContent;

/**
 * 行内"点状下划线 / 着重号"节点，对应 Markdown 语法 {@code ::text::}。在每个字下方居中绘制一个点。
 */
public class MdAstDottedUnderline extends MdAstParent<MdAstPhrasingContent> implements MdAstStaticPhrasingContent {

    public static final String TYPE = "guideDottedUnderline";

    public MdAstDottedUnderline() {
        super(TYPE);
    }

    @Override
    protected Class<MdAstPhrasingContent> childClass() {
        return MdAstPhrasingContent.class;
    }
}
