package com.hfstudio.guidenh.guide.internal.editor.io;

import java.util.Locale;

public enum SceneEditorScreenshotFormat {

    PNG("png", true),
    JPG("jpg", false),
    BMP("bmp", false),
    WEBP("webp", false);

    private final String configValue;
    private final boolean alphaSupported;

    SceneEditorScreenshotFormat(String configValue, boolean alphaSupported) {
        this.configValue = configValue;
        this.alphaSupported = alphaSupported;
    }

    public String configValue() {
        return configValue;
    }

    public String fileExtension() {
        return configValue;
    }

    public boolean supportsAlpha() {
        return alphaSupported;
    }

    public static SceneEditorScreenshotFormat fromConfigValue(String raw) {
        if (raw == null) {
            return PNG;
        }
        String normalized = raw.trim()
            .toLowerCase(Locale.ROOT);
        for (SceneEditorScreenshotFormat format : values()) {
            if (format.configValue.equals(normalized)) {
                return format;
            }
        }
        return PNG;
    }
}
