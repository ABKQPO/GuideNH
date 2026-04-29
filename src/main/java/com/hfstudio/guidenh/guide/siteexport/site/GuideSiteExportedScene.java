package com.hfstudio.guidenh.guide.siteexport.site;

public final class GuideSiteExportedScene {

    private final String placeholderPath;
    private final String scenePath;
    private final String inWorldJson;
    private final String overlayJson;

    public GuideSiteExportedScene(String placeholderPath, String scenePath) {
        this(placeholderPath, scenePath, null, null);
    }

    public GuideSiteExportedScene(String placeholderPath, String scenePath, String inWorldJson, String overlayJson) {
        this.placeholderPath = placeholderPath;
        this.scenePath = scenePath;
        this.inWorldJson = inWorldJson;
        this.overlayJson = overlayJson;
    }

    public String placeholderPath() {
        return placeholderPath;
    }

    public String scenePath() {
        return scenePath;
    }

    public String inWorldJson() {
        return inWorldJson;
    }

    public String overlayJson() {
        return overlayJson;
    }
}
