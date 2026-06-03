package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytList;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class ListCompiler extends BlockTagCompiler {

    private static final Set<String> TAG_NAMES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("ul", "ol")));

    @Override
    public Set<String> getTagNames() {
        return TAG_NAMES;
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        boolean ordered = "ol".equals(el.name());
        int start = parseIntSafe(el.getAttributeString("start", "1"), 1);
        LytList list = new LytList(ordered, start);
        for (var child : el.children()) {
            compiler.compileBlockContext(Collections.singletonList(child), list);
        }
        parent.append(list);
    }

    private static int parseIntSafe(String s, int fallback) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
