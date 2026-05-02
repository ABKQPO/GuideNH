package com.hfstudio.guidenh.guide.siteexport.site;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface GuideSiteItemIconResolver {

    GuideSiteItemIconResolver NONE = stack -> "";

    String exportIcon(@Nullable ItemStack stack);
}
