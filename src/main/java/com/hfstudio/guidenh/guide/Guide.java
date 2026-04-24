package com.hfstudio.guidenh.guide;

import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.extensions.ExtensionCollection;

public interface Guide extends PageCollection {

    static GuideBuilder builder(ResourceLocation id) {
        return new GuideBuilder(id);
    }

    ResourceLocation getId();

    ResourceLocation getStartPage();

    String getDefaultNamespace();

    String getContentRootFolder();

    ExtensionCollection getExtensions();
}
