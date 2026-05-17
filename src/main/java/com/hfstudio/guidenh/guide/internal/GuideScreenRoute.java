package com.hfstudio.guidenh.guide.internal;

import java.util.Objects;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.internal.search.GuideSearchPage;

public class GuideScreenRoute {

    public enum Kind {
        HOME,
        HOME_SEARCH,
        CONTENT
    }

    private static final GuideScreenRoute HOME = new GuideScreenRoute(Kind.HOME, null, null);

    private final Kind kind;
    @Nullable
    private final ResourceLocation guideId;
    @Nullable
    private final PageAnchor anchor;

    private GuideScreenRoute(Kind kind, @Nullable ResourceLocation guideId, @Nullable PageAnchor anchor) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.guideId = guideId;
        this.anchor = anchor;
    }

    public static GuideScreenRoute home() {
        return HOME;
    }

    public static GuideScreenRoute homeSearch(@Nullable String query) {
        return new GuideScreenRoute(Kind.HOME_SEARCH, null, GuideSearchPage.anchorForQuery(query));
    }

    public static GuideScreenRoute content(ResourceLocation guideId, PageAnchor anchor) {
        return new GuideScreenRoute(
            Kind.CONTENT,
            Objects.requireNonNull(guideId, "guideId"),
            Objects.requireNonNull(anchor, "anchor"));
    }

    public boolean isHome() {
        return kind == Kind.HOME;
    }

    public boolean isHomeSearch() {
        return kind == Kind.HOME_SEARCH;
    }

    public boolean isContent() {
        return kind == Kind.CONTENT;
    }

    public Kind kind() {
        return kind;
    }

    @Nullable
    public ResourceLocation guideId() {
        return guideId;
    }

    @Nullable
    public PageAnchor anchor() {
        return anchor;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof GuideScreenRoute other)) {
            return false;
        }
        return kind == other.kind && Objects.equals(guideId, other.guideId) && Objects.equals(anchor, other.anchor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, guideId, anchor);
    }

    @Override
    public String toString() {
        return "GuideScreenRoute{" + "kind=" + kind + ", guideId=" + guideId + ", anchor=" + anchor + '}';
    }
}
