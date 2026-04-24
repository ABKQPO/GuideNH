package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytItemImage;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class BlockImageCompiler extends BlockTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("BlockImage");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        var blockAndId = MdxAttrs.getRequiredBlockAndId(compiler, parent, el, "id");
        if (blockAndId == null) return;

        var block = blockAndId.getRight();
        var item = Item.getItemFromBlock(block);
        if (item == null) {
            parent.appendError(compiler, "No item form for block " + blockAndId.getLeft(), el);
            return;
        }

        float scale = MdxAttrs.getFloat(compiler, parent, el, "scale", 1f);
        var stack = new ItemStack(item);
        var img = new LytItemImage(stack);
        img.setScale(scale);
        parent.append(img);
    }
}
