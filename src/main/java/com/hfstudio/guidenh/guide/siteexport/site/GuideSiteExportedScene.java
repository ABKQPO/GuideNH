package com.hfstudio.guidenh.guide.siteexport.site;

public final class GuideSiteExportedScene {

    private final String placeholderPath;
    private final String scenePath;
    private final String inWorldJson;
    private final String overlayJson;
    private final String hoverTargetsJson;
    private final String stateManifestPath;

    public GuideSiteExportedScene(String placeholderPath, String scenePath) {
        this(placeholderPath, scenePath, null, null, null, null);
    }

    public GuideSiteExportedScene(String placeholderPath, String scenePath, String inWorldJson, String overlayJson) {
        this(placeholderPath, scenePath, inWorldJson, overlayJson, null, null);
    }

    public GuideSiteExportedScene(String placeholderPath, String scenePath, String inWorldJson, String overlayJson,
        String hoverTargetsJson) {
        this(placeholderPath, scenePath, inWorldJson, overlayJson, hoverTargetsJson, null);
    }

    public GuideSiteExportedScene(String placeholderPath, String scenePath, String inWorldJson, String overlayJson,
        String hoverTargetsJson, String stateManifestPath) {
        this.placeholderPath = placeholderPath;
        this.scenePath = scenePath;
        this.inWorldJson = inWorldJson;
        this.overlayJson = overlayJson;
        this.hoverTargetsJson = hoverTargetsJson;
        this.stateManifestPath = stateManifestPath;
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

    public String stateManifestPath() {
        return stateManifestPath;
    }
}
