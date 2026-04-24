package com.hfstudio.guidenh.libs.mdast.gfm.model;

import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParent;

public class GfmTableRow extends MdAstParent<GfmTableCell> implements MdAstAnyContent {

    public static final String TYPE = "tableRow";

    public GfmTableRow() {
        super(TYPE);
    }

    @Override
    protected Class<GfmTableCell> childClass() {
        return GfmTableCell.class;
    }
}
