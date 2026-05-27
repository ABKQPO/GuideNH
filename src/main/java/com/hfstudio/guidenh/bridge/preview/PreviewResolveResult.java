package com.hfstudio.guidenh.bridge.preview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PreviewResolveResult {

    private final String capability;
    private final String previewKey;
    private final String id;
    private final String displayName;
    private final String detail;
    private final Integer meta;
    private final Integer count;
    private final String nbt;
    private final List<String> tooltipLines;
    private final String iconPngBase64;
    private final int pixelWidth;
    private final int pixelHeight;

    public PreviewResolveResult(String capability, String previewKey, String id, String displayName, String detail,
        Integer meta, Integer count, String nbt, List<String> tooltipLines, String iconPngBase64, int pixelWidth,
        int pixelHeight) {
        this.capability = capability == null ? "" : capability;
        this.previewKey = previewKey == null ? "" : previewKey;
        this.id = id == null ? "" : id;
        this.displayName = displayName;
        this.detail = detail;
        this.meta = meta;
        this.count = count;
        this.nbt = nbt;
        this.tooltipLines = tooltipLines == null ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(tooltipLines));
        this.iconPngBase64 = iconPngBase64 == null ? "" : iconPngBase64;
        this.pixelWidth = pixelWidth;
        this.pixelHeight = pixelHeight;
    }

    public String getCapability() {
        return capability;
    }

    public String getPreviewKey() {
        return previewKey;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDetail() {
        return detail;
    }

    public Integer getMeta() {
        return meta;
    }

    public Integer getCount() {
        return count;
    }

    public String getNbt() {
        return nbt;
    }

    public List<String> getTooltipLines() {
        return tooltipLines;
    }

    public String getIconPngBase64() {
        return iconPngBase64;
    }

    public int getPixelWidth() {
        return pixelWidth;
    }

    public int getPixelHeight() {
        return pixelHeight;
    }
}
