package com.hfstudio.guidenh.guide.siteexport.site;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface GuideSiteItemIconResolver {

    GuideSiteItemIconResolver NONE = new GuideSiteItemIconResolver() {

        @Override
        public String exportIcon(@Nullable ItemStack stack) {
            return "";
        }
    };

    String exportIcon(@Nullable ItemStack stack);
}
