package com.hfstudio.guidenh.guide.internal;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.GuideNH;
import com.hfstudio.guidenh.guide.internal.search.GuideSearch;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

public class GuideME {

    static GuideMEProxy PROXY = new GuideMEServerProxy();

    @Nullable
    public static volatile GuideSearch SEARCH = null;

    private GuideME() {}

    public static ResourceLocation makeId(String path) {
        return new ResourceLocation(GuideNH.MODID, path);
    }

    public static void initClientProxy() {
        PROXY = new GuideMEClientProxy();
    }

    public static GuideSearch getSearch() {
        GuideSearch s = SEARCH;
        if (s == null) {
            synchronized (GuideME.class) {
                s = SEARCH;
                if (s == null) {
                    SEARCH = s = new GuideSearch();
                }
            }
        }
        return s;
    }

    public static synchronized void closeSearch() {
        if (SEARCH != null) {
            try {
                SEARCH.close();
            } catch (Exception e) {
                GuideDebugLog.error("[GuideNH] [GuideME] Failed to close GuideSearch", e);
            } finally {
                SEARCH = null;
            }
        }
    }
}
