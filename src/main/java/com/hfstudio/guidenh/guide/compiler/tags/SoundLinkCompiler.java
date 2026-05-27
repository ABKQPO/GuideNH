package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.IndexingContext;
import com.hfstudio.guidenh.guide.compiler.IndexingSink;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.flow.LytFlowLink;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.guide.sound.GuideSoundParsers;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class SoundLinkCompiler extends FlowTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("SoundLink");
    }

    @Override
    protected void compile(PageCompiler compiler, LytFlowParent parent, MdxJsxElementFields el) {
        var sound = GuideSoundParsers.parseAttributes(compiler, parent, el);
        if (sound == null) {
            parent.appendError(compiler, "SoundLink requires a sound or src attribute.", el);
            return;
        }
        var link = new LytFlowLink();
        link.setClickSoundSpec(sound);
        link.setClickCallback(uiHost -> {});
        compiler.compileInlineFragment(el.children(), link);
        parent.append(link);
    }

    @Override
    public void index(IndexingContext indexer, MdxJsxElementFields el, IndexingSink sink) {
        indexer.indexContent(el.children(), sink);
    }
}
