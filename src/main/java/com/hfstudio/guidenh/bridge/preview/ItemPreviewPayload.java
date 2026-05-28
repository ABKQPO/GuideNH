package com.hfstudio.guidenh.bridge.preview;

import java.util.List;

public class ItemPreviewPayload {

    private final String previewKey;
    private final String id;
    private final String displayName;
    private final String detail;
    private final int meta;
    private final int count;
    private final String nbt;
    private final List<String> tooltipLines;
    private final String iconPngBase64;
    private final int pixelWidth;
    private final int pixelHeight;

    public ItemPreviewPayload(String previewKey, String id, String displayName, String detail, int meta, int count,
        String nbt, List<String> tooltipLines, String iconPngBase64, int pixelWidth, int pixelHeight) {
        this.previewKey = previewKey == null ? "" : previewKey;
        this.id = id == null ? "" : id;
        this.displayName = displayName;
        this.detail = detail;
        this.meta = meta;
        this.count = count;
        this.nbt = nbt == null ? "" : nbt;
        this.tooltipLines = tooltipLines == null ? List.of() : List.copyOf(tooltipLines);
        this.iconPngBase64 = iconPngBase64 == null ? "" : iconPngBase64;
        this.pixelWidth = pixelWidth;
        this.pixelHeight = pixelHeight;
    }

    public PreviewResolveResult toResult(String capability) {
        return new PreviewResolveResult(
            capability,
            previewKey,
            id,
            displayName,
            detail,
            meta,
            count,
            nbt,
            tooltipLines,
            iconPngBase64,
            pixelWidth,
            pixelHeight);
    }
}
