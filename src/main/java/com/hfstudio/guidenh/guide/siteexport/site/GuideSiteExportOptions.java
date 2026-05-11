package com.hfstudio.guidenh.guide.siteexport.site;

public class GuideSiteExportOptions {

    public static final GuideSiteExportOptions DEFAULT = new GuideSiteExportOptions(false);

    private final boolean exportPonderEveryTick;

    public GuideSiteExportOptions(boolean exportPonderEveryTick) {
        this.exportPonderEveryTick = exportPonderEveryTick;
    }

    public boolean exportPonderEveryTick() {
        return exportPonderEveryTick;
    }
}
