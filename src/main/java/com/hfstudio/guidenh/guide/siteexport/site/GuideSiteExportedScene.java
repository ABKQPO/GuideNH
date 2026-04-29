package com.hfstudio.guidenh.guide.siteexport.site;

public final class GuideSiteExportedScene {

    private final String placeholderPath;
    private final String scenePath;
    private final String inWorldJson;
    private final String overlayJson;
    private final String hoverTargetsJson;

    public GuideSiteExportedScene(String placeholderPath, String scenePath) {
        this(placeholderPath, scenePath, null, null, null);
    }

    public GuideSiteExportedScene(String placeholderPath, String scenePath, String inWorldJson, String overlayJson) {
        this(placeholderPath, scenePath, inWorldJson, overlayJson, null);
    }

    public GuideSiteExportedScene(String placeholderPath, String scenePath, String inWorldJson, String overlayJson,
        String hoverTargetsJson) {
        this.placeholderPath = placeholderPath;
        this.scenePath = scenePath;
        this.inWorldJson = inWorldJson;
        this.overlayJson = overlayJson;
        this.hoverTargetsJson = hoverTargetsJson;
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

    public String hoverTargetsJson() {
        return hoverTargetsJson;
    }
}
