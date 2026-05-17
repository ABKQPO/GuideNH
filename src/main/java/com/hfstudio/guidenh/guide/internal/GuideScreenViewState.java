package com.hfstudio.guidenh.guide.internal;

import java.util.Objects;

public class GuideScreenViewState {

    private final GuideScreenRoute route;
    private final int scrollY;

    private GuideScreenViewState(GuideScreenRoute route, int scrollY) {
        this.route = Objects.requireNonNull(route, "route");
        this.scrollY = scrollY;
    }

    public static GuideScreenViewState home() {
        return new GuideScreenViewState(GuideScreenRoute.home(), 0);
    }

    public static GuideScreenViewState of(GuideScreenRoute route, int scrollY) {
        return new GuideScreenViewState(route, Math.max(0, scrollY));
    }

    public GuideScreenRoute route() {
        return route;
    }

    public int scrollY() {
        return scrollY;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof GuideScreenViewState other)) {
            return false;
        }
        return scrollY == other.scrollY && route.equals(other.route);
    }

    @Override
    public int hashCode() {
        return Objects.hash(route, scrollY);
    }

    @Override
    public String toString() {
        return "GuideScreenViewState{" + "route=" + route + ", scrollY=" + scrollY + '}';
    }
}
