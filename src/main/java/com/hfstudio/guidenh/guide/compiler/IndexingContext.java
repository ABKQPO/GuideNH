package com.hfstudio.guidenh.guide.compiler;

import java.util.List;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.PageCollection;
import com.hfstudio.guidenh.guide.extensions.Extension;
import com.hfstudio.guidenh.guide.extensions.ExtensionCollection;
import com.hfstudio.guidenh.guide.extensions.ExtensionPoint;
import com.hfstudio.guidenh.guide.indices.PageIndex;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;

/**
 * The context used during search indexing of custom tags {@link TagCompiler}.
 */
public interface IndexingContext {

    ExtensionCollection getExtensions();

    default <T extends Extension> List<T> getExtensions(ExtensionPoint<T> extensionPoint) {
        return getExtensions().get(extensionPoint);
    }

    /**
     * Get the current page id.
     */
    ResourceLocation getPageId();

    PageCollection getPageCollection();

    default void indexContent(List<? extends MdAstAnyContent> children, IndexingSink sink) {
        for (var child : children) {
            indexContent(child, sink);
        }
    }

    void indexContent(MdAstAnyContent content, IndexingSink sink);

    default @Nullable byte[] loadAsset(ResourceLocation imageId) {
        return getPageCollection().loadAsset(imageId);
    }

    default <T extends PageIndex> T getIndex(Class<T> clazz) {
        return getPageCollection().getIndex(clazz);
    }
}
