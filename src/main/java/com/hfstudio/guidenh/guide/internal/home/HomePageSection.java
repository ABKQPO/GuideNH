package com.hfstudio.guidenh.guide.internal.home;

import java.util.List;

public class HomePageSection {

    public enum Kind {
        RECOMMENDED,
        BOOKMARKS,
        HISTORY
    }

    private final Kind kind;
    private final String title;
    private final String emptyText;
    private final List<HomePageEntry> entries;

    public HomePageSection(Kind kind, String title, String emptyText, List<HomePageEntry> entries) {
        this.kind = kind;
        this.title = title;
        this.emptyText = emptyText;
        this.entries = List.copyOf(entries);
    }

    public Kind kind() {
        return kind;
    }

    public String title() {
        return title;
    }

    public String emptyText() {
        return emptyText;
    }

    public List<HomePageEntry> entries() {
        return entries;
    }
}
