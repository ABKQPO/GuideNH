package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytFileTree;
import com.hfstudio.guidenh.guide.internal.markdown.FileTreeCompiler;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstHTML;
import com.hfstudio.guidenh.libs.mdast.model.MdAstText;
import com.hfstudio.guidenh.libs.unist.UnistNode;
import com.hfstudio.guidenh.libs.unist.UnistParent;

/**
 * Compiles the {@code <FileTree>} block tag. Body content is recovered as raw source text via
 * {@link PageCompiler#getBlockTagChildrenSource} so that the verbatim tree characters survive
 * markdown re-tokenization. When the source position is unavailable (e.g. authored programmatically
 * without offsets) a best-effort text join over the element children is used as fallback.
 */
public class FileTreeTagCompiler extends BlockTagCompiler {

    public static final String TAG_NAME = "FileTree";

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton(TAG_NAME);
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        String source = compiler.getBlockTagChildrenSource(el);
        if (source == null || source.isEmpty()) {
            source = collectTextFallback(el);
        }
        LytFileTree tree = FileTreeCompiler.compile(compiler, source);
        if (tree.isEmpty()) {
            return;
        }
        applyOptionalIntAttribute(el, "indent", tree::setIndentPx);
        applyOptionalIntAttribute(el, "gap", tree::setRowGapPx);
        parent.append(tree);
    }

    private static void applyOptionalIntAttribute(MdxJsxElementFields el, String name,
        java.util.function.IntConsumer setter) {
        String raw = el.getAttributeString(name, null);
        if (raw == null || raw.isEmpty()) {
            return;
        }
        try {
            setter.accept(Integer.parseInt(raw.trim()));
        } catch (NumberFormatException ignored) {
            // Silently ignore malformed values; the default already produced a valid layout.
        }
    }

    private static String collectTextFallback(MdxJsxElementFields el) {
        StringBuilder builder = new StringBuilder();
        for (MdAstAnyContent child : el.children()) {
            appendText(child, builder);
        }
        return builder.toString();
    }

    private static void appendText(Object node, StringBuilder builder) {
        if (node instanceof MdAstText text) {
            builder.append(text.value);
            return;
        }
        if (node instanceof MdAstHTML html) {
            builder.append(html.value);
            return;
        }
        if (node instanceof UnistParent parent) {
            for (UnistNode child : parent.children()) {
                appendText(child, builder);
            }
        }
    }
}
