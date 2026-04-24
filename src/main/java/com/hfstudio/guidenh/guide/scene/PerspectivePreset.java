package com.hfstudio.guidenh.guide.scene;

public enum PerspectivePreset {

    ISOMETRIC_NORTH_EAST("isometric-north-east"),
    ISOMETRIC_NORTH_WEST("isometric-north-west"),
    UP("up");

    private final String serializedName;

    PerspectivePreset(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return serializedName;
    }

    public static PerspectivePreset fromSerializedName(String name) {
        for (var p : values()) {
            if (p.serializedName.equalsIgnoreCase(name)) return p;
        }
        return ISOMETRIC_NORTH_EAST;
    }
}
