package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.AlignItems;
import com.hfstudio.guidenh.guide.document.block.LytAxisBox;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytHBox;
import com.hfstudio.guidenh.guide.document.block.LytVBox;
import com.hfstudio.guidenh.guide.document.block.LytWidthBox;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class BoxTagCompiler extends BlockTagCompiler {

    private final BoxFlowDirection direction;

    public BoxTagCompiler(BoxFlowDirection direction) {
        this.direction = direction;
    }

    @Override
    public Set<String> getTagNames() {
        return direction == BoxFlowDirection.ROW ? Collections.singleton("Row") : Collections.singleton("Column");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        var gap = MdxAttrs.getInt(compiler, parent, el, "gap", 5);
        var alignItems = MdxAttrs.getEnum(compiler, parent, el, "alignItems", AlignItems.START);
        var fullWidth = MdxAttrs.getBoolean(compiler, parent, el, "fullWidth", false);
        var width = MdxAttrs.getInt(compiler, parent, el, "width", 0);

        LytAxisBox box = switch (this.direction) {
            case ROW -> {
                var hbox = new LytHBox();
                hbox.setGap(gap);
                yield hbox;
            }
            case COLUMN -> {
                var vbox = new LytVBox();
                vbox.setGap(gap);
                yield vbox;
            }
        };

        box.setAlignItems(alignItems);
        box.setFullWidth(fullWidth);

        if (width > 0) {
            LytWidthBox widthBox = new LytWidthBox();
            widthBox.setPreferredWidth(width);
            widthBox.append(box);
            compiler.compileBlockTagChildren(el, box);
            parent.append(widthBox);
        } else {
            compiler.compileBlockTagChildren(el, box);
            parent.append(box);
        }
    }
}
