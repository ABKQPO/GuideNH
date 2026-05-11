package com.hfstudio.guidenh.libs.mdast.guidemark;

import com.hfstudio.guidenh.libs.mdast.model.MdAstParent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstPhrasingContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstStaticPhrasingContent;

public class MdAstMark extends MdAstParent<MdAstPhrasingContent> implements MdAstStaticPhrasingContent {

    public static final String TYPE = "guideMark";

    public MdAstMark() {
        super(TYPE);
    }

    @Override
    protected Class<MdAstPhrasingContent> childClass() {
        return MdAstPhrasingContent.class;
    }
}
