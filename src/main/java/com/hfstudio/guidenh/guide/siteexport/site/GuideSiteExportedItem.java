package com.hfstudio.guidenh.guide.siteexport.site;

public final class GuideSiteExportedItem {

    private final String itemId;
    private final String displayName;
    private final String iconSrc;

    public GuideSiteExportedItem(String itemId, String displayName, String iconSrc) {
        this.itemId = itemId != null ? itemId : "";
        this.displayName = displayName != null ? displayName : "";
        this.iconSrc = iconSrc != null ? iconSrc : "";
    }

    public String itemId() {
        return itemId;
    }

    public String displayName() {
        return displayName;
    }

    public String iconSrc() {
        return iconSrc;
    }

    public boolean hasIcon() {
        return !iconSrc.isEmpty();
    }

    public boolean isEmpty() {
        return itemId.isEmpty() && displayName.isEmpty() && iconSrc.isEmpty();
    }
}
