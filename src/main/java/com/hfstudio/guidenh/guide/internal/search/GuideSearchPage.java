package com.hfstudio.guidenh.guide.internal.search;

import javax.annotation.Nullable;

import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.internal.GuideME;

public class GuideSearchPage {

    public static final ResourceLocation PAGE_ID = GuideME.makeId("search");

    private GuideSearchPage() {}

    public static boolean isSearchAnchor(@Nullable PageAnchor anchor) {
        return anchor != null && PAGE_ID.equals(anchor.pageId());
    }

    public static String normalizeQuery(@Nullable String query) {
        return query == null ? "" : query.trim();
    }

    public static String queryFromAnchor(@Nullable PageAnchor anchor) {
        return isSearchAnchor(anchor) ? anchor.anchor() == null ? "" : anchor.anchor() : "";
    }

    public static PageAnchor anchorForQuery(@Nullable String query) {
        return query == null || query.isEmpty() ? PageAnchor.page(PAGE_ID) : new PageAnchor(PAGE_ID, query);
    }
}
