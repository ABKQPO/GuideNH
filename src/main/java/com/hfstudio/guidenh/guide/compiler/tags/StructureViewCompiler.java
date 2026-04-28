package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytStructureView;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstLiteral;

/**
 * 
 * <pre>
 * &lt;Structure width="200" height="160"&gt;
 *   0 0 0 minecraft:stone
 *   1 0 0 minecraft:cobblestone
 *   0 1 0 minecraft:dirt
 *   0 0 1 minecraft:planks:2
 * &lt;/Structure&gt;
 * </pre>
 */
public class StructureViewCompiler extends BlockTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("Structure");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        var view = new LytStructureView();

        int width = MdxAttrs.getInt(compiler, parent, el, "width", 192);
        int height = MdxAttrs.getInt(compiler, parent, el, "height", 144);
        view.setViewSize(width, height);

        StringBuilder text = new StringBuilder();
        for (var child : el.children()) {
            if (child instanceof MdAstLiteral literal) {
                text.append(literal.value)
                    .append('\n');
            }
        }

        for (String rawLine : text.toString()
            .split("\\R")) {
            var line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            parseLine(compiler, parent, view, line, el);
        }

        parent.append(view);
    }

    public static void parseLine(PageCompiler compiler, LytBlockContainer parent, LytStructureView view, String line,
        MdxJsxElementFields el) {
        var parts = line.split("\\s+");
        if (parts.length < 4) {
            parent.appendError(compiler, "Structure entry needs '<x> <y> <z> <modid:name>': " + line, el);
            return;
        }

        int x, y, z;
        try {
            x = Integer.parseInt(parts[0]);
            y = Integer.parseInt(parts[1]);
            z = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            parent.appendError(compiler, "Structure entry has non-integer coords: " + line, el);
            return;
        }

        String idSpec = parts[3];
        int meta = 0;
        int firstColon = idSpec.indexOf(':');
        int lastColon = idSpec.lastIndexOf(':');
        String resourceId = idSpec;
        if (firstColon != lastColon) {
            try {
                meta = Integer.parseInt(idSpec.substring(lastColon + 1));
                resourceId = idSpec.substring(0, lastColon);
            } catch (NumberFormatException ignored) {}
        }

        ItemStack stack = resolveStack(resourceId, meta);
        if (stack == null) {
            parent.appendError(compiler, "Unknown block/item: " + idSpec, el);
            return;
        }

        view.addBlock(x, y, z, stack);
    }

    public static ItemStack resolveStack(String resourceId, int meta) {
        var item = (Item) Item.itemRegistry.getObject(resourceId);
        if (item != null) {
            return new ItemStack(item, 1, meta);
        }
        var block = (Block) Block.blockRegistry.getObject(resourceId);
        if (block != null) {
            return new ItemStack(block, 1, meta);
        }
        return null;
    }
}
