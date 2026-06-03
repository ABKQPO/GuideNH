package com.hfstudio.guidenh.guide.internal.search;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.PageCollection;
import com.hfstudio.guidenh.guide.compiler.IndexingContext;
import com.hfstudio.guidenh.guide.compiler.IndexingSink;
import com.hfstudio.guidenh.guide.compiler.TagCompiler;
import com.hfstudio.guidenh.guide.extensions.Extension;
import com.hfstudio.guidenh.guide.extensions.ExtensionCollection;
import com.hfstudio.guidenh.guide.extensions.ExtensionPoint;
import com.hfstudio.guidenh.guide.indices.PageIndex;
import com.hfstudio.guidenh.libs.mdast.MdAstYamlFrontmatter;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstDefinition;
import com.hfstudio.guidenh.libs.mdast.model.MdAstRoot;
import com.hfstudio.guidenh.libs.mdast.model.MdAstText;
import com.hfstudio.guidenh.libs.unist.UnistNode;

import cpw.mods.fml.common.FMLLog;

public class PageIndexer implements IndexingContext {

    private final PageCollection pages;
    private final ExtensionCollection extensions;
    private final ResourceLocation pageId;
    private final Map<String, TagCompiler> tagCompilers = new HashMap<>();

    public PageIndexer(PageCollection pages, ExtensionCollection extensions, ResourceLocation pageId) {
        this.pages = pages;
        this.extensions = extensions;
        this.pageId = pageId;

        // Index available tag-compilers
        for (var tagCompiler : extensions.get(TagCompiler.EXTENSION_POINT)) {
            for (String tagName : tagCompiler.getTagNames()) {
                tagCompilers.put(tagName, tagCompiler);
            }
        }
    }

    @Override
    public ExtensionCollection getExtensions() {
        return extensions;
    }

    @Override
    public <T extends Extension> List<T> getExtensions(ExtensionPoint<T> extensionPoint) {
        return extensions.get(extensionPoint);
    }

    public void index(MdAstRoot root, IndexingSink sink) {
        indexContent(root.children(), sink);
    }

    @Override
    public void indexContent(MdAstAnyContent content, IndexingSink sink) {
        if (content instanceof MdAstText astText) {
            sink.appendText(astText, astText.value);
        } else if (content instanceof MdxJsxElementFields el) {
            var compiler = tagCompilers.get(el.name());
            if (compiler == null) {
                FMLLog.getLogger()
                    .warn("[GuideNH] [PageIndexer] Unhandled MDX element in guide search indexing: {}", el.name());
                // Fallback: index children content
                indexContent(el.children(), sink);
            } else {
                compiler.index(this, el, sink);
            }
        } else if (content instanceof MdAstDefinition || content instanceof MdAstYamlFrontmatter) {
            // Handled via conversion
        } else {
            FMLLog.getLogger()
                .warn(
                    "[GuideNH] [PageIndexer] Unhandled node type in guide search indexing: {}",
                    ((UnistNode) content).type());
        }
    }

    @Override
    public void indexContent(List<? extends MdAstAnyContent> children, IndexingSink sink) {
        for (MdAstAnyContent child : children) {
            indexContent(child, sink);
        }
    }

    @Override
    public ResourceLocation getPageId() {
        return pageId;
    }

    @Override
    public PageCollection getPageCollection() {
        return pages;
    }

    @Override
    public <T extends PageIndex> T getIndex(Class<T> clazz) {
        return pages.getIndex(clazz);
    }
}
