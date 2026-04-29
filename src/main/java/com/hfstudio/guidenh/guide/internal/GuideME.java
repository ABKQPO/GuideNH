package com.hfstudio.guidenh.guide.internal;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.GuideNH;
import com.hfstudio.guidenh.guide.internal.search.GuideSearch;

public class GuideME {

    static GuideMEProxy PROXY = new GuideMEServerProxy();

    @Nullable
    public static GuideSearch SEARCH = null;

    private GuideME() {}

    public static ResourceLocation makeId(String path) {
        return new ResourceLocation(GuideNH.MODID, path);
    }

    public static void initClientProxy() {
        PROXY = new GuideMEClientProxy();
    }

    public static synchronized GuideSearch getSearch() {
        if (SEARCH == null) {
            SEARCH = new GuideSearch();
        }
        return SEARCH;
    }
}
