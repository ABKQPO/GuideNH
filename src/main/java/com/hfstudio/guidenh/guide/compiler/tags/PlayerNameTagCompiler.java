package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import net.minecraft.client.Minecraft;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

/**
 * This tag compiles to the name of current players game profile.
 */
public class PlayerNameTagCompiler extends FlowTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("PlayerName");
    }

    @Override
    protected void compile(PageCompiler compiler, LytFlowParent parent, MdxJsxElementFields el) {
        var playerName = Minecraft.getMinecraft()
            .getSession()
            .getUsername();
        parent.appendText(playerName);
    }
}
