package com.hfstudio.guidenh.libs.mdast.guideunderline;

import com.hfstudio.guidenh.libs.mdast.model.MdAstParent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstPhrasingContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstStaticPhrasingContent;

/**
 * 行内"波浪下划线"节点，对应 Markdown 语法 {@code ^^text^^}。
 */
public class MdAstWavyUnderline extends MdAstParent<MdAstPhrasingContent> implements MdAstStaticPhrasingContent {

    public static final String TYPE = "guideWavyUnderline";

    public MdAstWavyUnderline() {
        super(TYPE);
    }

    @Override
    protected Class<MdAstPhrasingContent> childClass() {
        return MdAstPhrasingContent.class;
    }
}
