package com.hfstudio.guidenh.guide.internal.host;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.PageCollection;
import com.hfstudio.guidenh.guide.document.block.LytDocument;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.indices.PageIndex;

public interface ScriptContext {
    Map<String, Object> data();
    void replace(Object newNode);
    String allocateId(String prefix);
    LytDocument document();

    @Nullable
    byte[] loadAsset(ResourceLocation id);

    <T extends PageIndex> T getIndex(Class<T> indexClass);

    @Nullable
    PageCollection getPageCollection();

    void submitTask(DeferredTask task);

    /** Recursively dispatch MOUNT events into a detached subtree */
    void dispatchSubtree(LytNode root);
}
