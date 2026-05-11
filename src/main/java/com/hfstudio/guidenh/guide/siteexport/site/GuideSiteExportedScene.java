package com.hfstudio.guidenh.guide.siteexport.site;

public class GuideSiteExportedScene {

    private final String placeholderPath;
    private final String scenePath;
    private final String inWorldJson;
    private final String overlayJson;
    private final String hoverTargetsJson;
    private final String stateManifestPath;
    private final String blockStatsHtml;
    private final String blockStatsLayoutClass;
    private final String blockStatsLayoutStyle;
    private final boolean gridButtonEnabled;
    private final boolean gridVisible;
    private final String gridAnnotationJson;
    private final boolean blockStatsButtonEnabled;
    private final boolean blockStatsVisible;

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
        this(placeholderPath, scenePath, inWorldJson, overlayJson, hoverTargetsJson, stateManifestPath, null);
    }

    public GuideSiteExportedScene(String placeholderPath, String scenePath, String inWorldJson, String overlayJson,
        String hoverTargetsJson, String stateManifestPath, String blockStatsHtml) {
        this(
            placeholderPath,
            scenePath,
            inWorldJson,
            overlayJson,
            hoverTargetsJson,
            stateManifestPath,
            blockStatsHtml,
            null,
            null);
    }

    public GuideSiteExportedScene(String placeholderPath, String scenePath, String inWorldJson, String overlayJson,
        String hoverTargetsJson, String stateManifestPath, String blockStatsHtml, String blockStatsLayoutClass,
        String blockStatsLayoutStyle) {
        this(
            placeholderPath,
            scenePath,
            inWorldJson,
            overlayJson,
            hoverTargetsJson,
            stateManifestPath,
            blockStatsHtml,
            blockStatsLayoutClass,
            blockStatsLayoutStyle,
            false,
            false,
            null,
            false,
            false);
    }

    public GuideSiteExportedScene(String placeholderPath, String scenePath, String inWorldJson, String overlayJson,
        String hoverTargetsJson, String stateManifestPath, String blockStatsHtml, String blockStatsLayoutClass,
        String blockStatsLayoutStyle, boolean gridButtonEnabled, boolean gridVisible, String gridAnnotationJson,
        boolean blockStatsButtonEnabled, boolean blockStatsVisible) {
        this.placeholderPath = placeholderPath;
        this.scenePath = scenePath;
        this.inWorldJson = inWorldJson;
        this.overlayJson = overlayJson;
        this.hoverTargetsJson = hoverTargetsJson;
        this.stateManifestPath = stateManifestPath;
        this.blockStatsHtml = blockStatsHtml;
        this.blockStatsLayoutClass = blockStatsLayoutClass;
        this.blockStatsLayoutStyle = blockStatsLayoutStyle;
        this.gridButtonEnabled = gridButtonEnabled;
        this.gridVisible = gridVisible;
        this.gridAnnotationJson = gridAnnotationJson;
        this.blockStatsButtonEnabled = blockStatsButtonEnabled;
        this.blockStatsVisible = blockStatsVisible;
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

    public String blockStatsHtml() {
        return blockStatsHtml;
    }

    public String blockStatsLayoutClass() {
        return blockStatsLayoutClass;
    }

    public String blockStatsLayoutStyle() {
        return blockStatsLayoutStyle;
    }

    public boolean gridButtonEnabled() {
        return gridButtonEnabled;
    }

    public boolean gridVisible() {
        return gridVisible;
    }

    public String gridAnnotationJson() {
        return gridAnnotationJson;
    }

    public boolean blockStatsButtonEnabled() {
        return blockStatsButtonEnabled;
    }

    public boolean blockStatsVisible() {
        return blockStatsVisible;
    }
}
