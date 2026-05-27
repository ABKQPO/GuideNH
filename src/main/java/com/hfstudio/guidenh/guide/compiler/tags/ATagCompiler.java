package com.hfstudio.guidenh.guide.compiler.tags;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.compiler.IndexingContext;
import com.hfstudio.guidenh.guide.compiler.IndexingSink;
import com.hfstudio.guidenh.guide.compiler.LinkParser;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.flow.LytFlowAnchor;
import com.hfstudio.guidenh.guide.document.flow.LytFlowLink;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.guide.document.interaction.TextTooltip;
import com.hfstudio.guidenh.guide.sound.GuideSoundParsers;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class ATagCompiler extends FlowTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("a");
    }

    @Override
    protected void compile(PageCompiler compiler, LytFlowParent parent, MdxJsxElementFields el) {
        var anchorName = el.getAttributeString("name", "");
        if (!anchorName.isEmpty()) {
            parent.append(new LytFlowAnchor(anchorName));
        }

        // The link only materializes if it has a HREF attribute, otherwise we compile the children directly
        var href = el.getAttributeString("href", "");
        var title = el.getAttributeString("title", "");
        if (!href.isEmpty() || !title.isEmpty()) {
            var link = new LytFlowLink();
            if (!title.isEmpty()) {
                link.setTooltip(new TextTooltip(title));
            }
            var sound = GuideSoundParsers.parseActionUri(compiler, href);
            if (sound != null) {
                link.setClickSoundSpec(sound);
                link.setClickCallback(uiHost -> {});
            } else if (!href.isEmpty()) {
                LinkParser.parseLink(compiler, href, new LinkParser.Visitor() {

                    @Override
                    public void handlePage(ResourceLocation guideId, PageAnchor page) {
                        link.setGuideLink(guideId, page);
                    }

                    @Override
                    public void handleExternal(URI uri) {
                        link.setExternalUrl(uri);
                    }

                    @Override
                    public void handleError(String error) {
                        parent.appendError(compiler, error, el);
                    }
                });
            }
            compiler.compileInlineFragment(el.children(), link);
            parent.append(link);
        } else {
            compiler.compileInlineFragment(el.children(), parent);
        }
    }

    @Override
    public void index(IndexingContext indexer, MdxJsxElementFields el, IndexingSink sink) {
        var title = el.getAttributeString("title", "");
        if (!title.trim()
            .isEmpty()) {
            sink.appendText(el, title);
        }

        indexer.indexContent(el.children(), sink);
    }
}
