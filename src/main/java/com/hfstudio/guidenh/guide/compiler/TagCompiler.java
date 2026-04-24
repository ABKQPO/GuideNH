package com.hfstudio.guidenh.guide.compiler;

import java.util.Set;

import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.guide.extensions.Extension;
import com.hfstudio.guidenh.guide.extensions.ExtensionPoint;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxTextElement;

/**
 * Tag compilers handle HTML-like tags found in Markdown content, such as <code>&lt;Image /&gt;</code> and similar.
 */
public interface TagCompiler extends Extension {

    ExtensionPoint<TagCompiler> EXTENSION_POINT = new ExtensionPoint<>(TagCompiler.class);

    /**
     * The tag names this compiler is responsible for.
     */
    Set<String> getTagNames();

    default void compileBlockContext(PageCompiler compiler, LytBlockContainer parent, MdxJsxFlowElement el) {
        parent.append(compiler.createErrorBlock("Cannot use MDX tag " + el.name + " in block context", el));
    }

    default void compileFlowContext(PageCompiler compiler, LytFlowParent parent, MdxJsxTextElement el) {
        parent.append(compiler.createErrorFlowContent("Cannot use MDX tag " + el.name() + " in flow context", el));
    }

    default void index(IndexingContext indexer, MdxJsxElementFields el, IndexingSink sink) {
        indexer.indexContent(el.children(), sink);
    }
}
