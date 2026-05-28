package com.hfstudio.guidenh.guide.internal.editor.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.scene.annotation.TextAnnotation;

public class SceneEditorElementType {

    public static final Map<String, SceneEditorElementType> TYPES_BY_ID = new LinkedHashMap<>();
    public static final Map<String, SceneEditorElementType> TYPES_BY_TAG_NAME = new LinkedHashMap<>();

    public static final SceneEditorElementType BLOCK = register(
        builder("guidenh:block", "BlockAnnotation", GuidebookText.SceneEditorElementBlock)
            .iconPngPath("guidenh:textures/guide/buttons.png")
            .fallbackGlyph('B')
            .accentColor(0xFF9FC6FF)
            .pointHandleMode(PointHandleMode.POINT)
            .includePrimaryVector(true)
            .includeSecondaryVector(false)
            .includeThickness(true)
            .includeAlwaysOnTop(true)
            .includeTooltip(true)
            .primaryVectorLabel(GuidebookText.SceneEditorElementPosition)
            .defaultColorLiteral("#FFFFFFFF")
            .build());
    public static final SceneEditorElementType BOX = register(
        builder("guidenh:box", "BoxAnnotation", GuidebookText.SceneEditorElementBox)
            .iconPngPath("guidenh:textures/guide/buttons.png")
            .fallbackGlyph('O')
            .accentColor(0xFFFFC07A)
            .pointHandleMode(PointHandleMode.BOX)
            .includePrimaryVector(true)
            .includeSecondaryVector(true)
            .includeThickness(true)
            .includeAlwaysOnTop(true)
            .includeTooltip(true)
            .primaryVectorLabel(GuidebookText.SceneEditorElementMin)
            .secondaryVectorLabel(GuidebookText.SceneEditorElementMax)
            .defaultColorLiteral("#FFFFFFFF")
            .build());
    public static final SceneEditorElementType LINE = register(
        builder("guidenh:line", "LineAnnotation", GuidebookText.SceneEditorElementLine)
            .iconPngPath("guidenh:textures/guide/buttons.png")
            .fallbackGlyph('L')
            .accentColor(0xFF9FFFB0)
            .pointHandleMode(PointHandleMode.LINE)
            .includePrimaryVector(true)
            .includeSecondaryVector(true)
            .includeThickness(true)
            .includeAlwaysOnTop(true)
            .includeTooltip(true)
            .primaryVectorLabel(GuidebookText.SceneEditorElementFrom)
            .secondaryVectorLabel(GuidebookText.SceneEditorElementTo)
            .defaultColorLiteral("#FFFFFFFF")
            .build());
    public static final SceneEditorElementType DIAMOND = register(
        builder("guidenh:diamond", "DiamondAnnotation", GuidebookText.SceneEditorElementDiamond)
            .iconPngPath("guidenh:textures/guide/diamond.png")
            .fallbackGlyph('D')
            .accentColor(0xFFFFE16A)
            .pointHandleMode(PointHandleMode.POINT)
            .includePrimaryVector(true)
            .includeSecondaryVector(false)
            .includeThickness(false)
            .includeAlwaysOnTop(true)
            .includeTooltip(true)
            .primaryVectorLabel(GuidebookText.SceneEditorElementPosition)
            .defaultColorLiteral("#FF00E000")
            .build());
    public static final SceneEditorElementType TEXT = register(
        builder("guidenh:text", "TextAnnotation", GuidebookText.SceneEditorElementText).fallbackGlyph('T')
            .accentColor(0xFFFFF1A8)
            .pointHandleMode(PointHandleMode.POINT)
            .includePrimaryVector(true)
            .includeSecondaryVector(false)
            .includeThickness(false)
            .includeAlwaysOnTop(false)
            .includeTooltip(false)
            .includeText(true)
            .includeMaxWidth(true)
            .includeBackgroundAlpha(true)
            .primaryVectorLabel(GuidebookText.SceneEditorElementPosition)
            .defaultColorLiteral("#FFFFFFFF")
            .defaultText(GuidebookText.SceneEditorElementTextDefault.text())
            .defaultBackgroundAlpha(TextAnnotation.DEFAULT_BACKGROUND_ALPHA)
            .build());

    public final String id;
    public final String tagName;
    public final GuidebookText textKey;
    @Nullable
    public final String iconPngPath;
    public final char fallbackGlyph;
    public final int accentColor;
    public final PointHandleMode pointHandleMode;
    public final boolean includePrimaryVector;
    public final boolean includeSecondaryVector;
    public final boolean includeThickness;
    public final boolean includeAlwaysOnTop;
    public final boolean includeTooltip;
    public final boolean includeText;
    public final boolean includeMaxWidth;
    public final boolean includeBackgroundAlpha;
    public final GuidebookText primaryVectorLabel;
    @Nullable
    public final GuidebookText secondaryVectorLabel;
    public final String defaultColorLiteral;
    public final float defaultThickness;
    public final String defaultText;
    public final int defaultMaxWidth;
    public final int defaultBackgroundAlpha;

    public SceneEditorElementType(Builder builder) {
        this.id = builder.id;
        this.tagName = builder.tagName;
        this.textKey = builder.textKey;
        this.iconPngPath = builder.iconPngPath;
        this.fallbackGlyph = builder.fallbackGlyph;
        this.accentColor = builder.accentColor;
        this.pointHandleMode = builder.pointHandleMode;
        this.includePrimaryVector = builder.includePrimaryVector;
        this.includeSecondaryVector = builder.includeSecondaryVector;
        this.includeThickness = builder.includeThickness;
        this.includeAlwaysOnTop = builder.includeAlwaysOnTop;
        this.includeTooltip = builder.includeTooltip;
        this.includeText = builder.includeText;
        this.includeMaxWidth = builder.includeMaxWidth;
        this.includeBackgroundAlpha = builder.includeBackgroundAlpha;
        this.primaryVectorLabel = builder.primaryVectorLabel;
        this.secondaryVectorLabel = builder.secondaryVectorLabel;
        this.defaultColorLiteral = builder.defaultColorLiteral;
        this.defaultThickness = builder.defaultThickness;
        this.defaultText = builder.defaultText;
        this.defaultMaxWidth = builder.defaultMaxWidth;
        this.defaultBackgroundAlpha = builder.defaultBackgroundAlpha;
    }

    public static Builder builder(String id, String tagName, GuidebookText textKey) {
        return new Builder(id, tagName, textKey);
    }

    public static synchronized SceneEditorElementType register(SceneEditorElementType type) {
        Objects.requireNonNull(type, "type");
        if (TYPES_BY_ID.containsKey(type.getId())) {
            throw new IllegalArgumentException("Duplicate scene editor element type id: " + type.getId());
        }
        if (TYPES_BY_TAG_NAME.containsKey(type.getTagName())) {
            throw new IllegalArgumentException("Duplicate scene editor element tag name: " + type.getTagName());
        }
        TYPES_BY_ID.put(type.getId(), type);
        TYPES_BY_TAG_NAME.put(type.getTagName(), type);
        return type;
    }

    public static List<SceneEditorElementType> values() {
        return List.copyOf(TYPES_BY_ID.values());
    }

    @Nullable
    public static SceneEditorElementType getById(String id) {
        return TYPES_BY_ID.get(id);
    }

    @Nullable
    public static SceneEditorElementType getByTagName(String tagName) {
        return TYPES_BY_TAG_NAME.get(tagName);
    }

    public String getId() {
        return id;
    }

    public String getTagName() {
        return tagName;
    }

    public String getDisplayText() {
        return textKey.text();
    }

    public GuidebookText getTextKey() {
        return textKey;
    }

    @Nullable
    public String getIconPngPath() {
        return iconPngPath;
    }

    public char getFallbackGlyph() {
        return fallbackGlyph;
    }

    public int getAccentColor() {
        return accentColor;
    }

    public PointHandleMode getPointHandleMode() {
        return pointHandleMode;
    }

    public boolean supportsPointHandles() {
        return pointHandleMode != PointHandleMode.NONE;
    }

    public boolean supportsPrimaryVector() {
        return includePrimaryVector;
    }

    public boolean supportsSecondaryVector() {
        return includeSecondaryVector;
    }

    public boolean supportsThickness() {
        return includeThickness;
    }

    public boolean supportsAlwaysOnTop() {
        return includeAlwaysOnTop;
    }

    public boolean supportsTooltip() {
        return includeTooltip;
    }

    public boolean supportsText() {
        return includeText;
    }

    public boolean supportsMaxWidth() {
        return includeMaxWidth;
    }

    public boolean supportsBackgroundAlpha() {
        return includeBackgroundAlpha;
    }

    public GuidebookText getPrimaryVectorLabel() {
        return primaryVectorLabel;
    }

    @Nullable
    public GuidebookText getSecondaryVectorLabel() {
        return secondaryVectorLabel;
    }

    public String getDefaultColorLiteral() {
        return defaultColorLiteral;
    }

    public float getDefaultThickness() {
        return defaultThickness;
    }

    public String getDefaultText() {
        return defaultText;
    }

    public int getDefaultMaxWidth() {
        return defaultMaxWidth;
    }

    public int getDefaultBackgroundAlpha() {
        return defaultBackgroundAlpha;
    }

    @Override
    public String toString() {
        return id;
    }

    public enum PointHandleMode {
        NONE,
        POINT,
        LINE,
        BOX
    }

    public static final class Builder {

        public final String id;
        public final String tagName;
        public final GuidebookText textKey;
        @Nullable
        public String iconPngPath;
        public char fallbackGlyph;
        public int accentColor;
        public PointHandleMode pointHandleMode;
        public boolean includePrimaryVector;
        public boolean includeSecondaryVector;
        public boolean includeThickness;
        public boolean includeAlwaysOnTop;
        public boolean includeTooltip;
        public boolean includeText;
        public boolean includeMaxWidth;
        public boolean includeBackgroundAlpha;
        public GuidebookText primaryVectorLabel;
        @Nullable
        public GuidebookText secondaryVectorLabel;
        public String defaultColorLiteral;
        public float defaultThickness;
        public String defaultText;
        public int defaultMaxWidth;
        public int defaultBackgroundAlpha;

        public Builder(String id, String tagName, GuidebookText textKey) {
            this.id = requireNonEmpty(id, "id");
            this.tagName = requireNonEmpty(tagName, "tagName");
            this.textKey = Objects.requireNonNull(textKey, "textKey");
            this.iconPngPath = null;
            this.fallbackGlyph = '?';
            this.accentColor = 0xFFFFFFFF;
            this.pointHandleMode = PointHandleMode.NONE;
            this.includePrimaryVector = false;
            this.includeSecondaryVector = false;
            this.includeThickness = false;
            this.includeAlwaysOnTop = false;
            this.includeTooltip = false;
            this.includeText = false;
            this.includeMaxWidth = false;
            this.includeBackgroundAlpha = false;
            this.primaryVectorLabel = GuidebookText.SceneEditorElementPosition;
            this.secondaryVectorLabel = null;
            this.defaultColorLiteral = "#FFFFFFFF";
            this.defaultThickness = 1f;
            this.defaultText = "";
            this.defaultMaxWidth = 0;
            this.defaultBackgroundAlpha = TextAnnotation.DEFAULT_BACKGROUND_ALPHA;
        }

        public Builder iconPngPath(@Nullable String iconPngPath) {
            this.iconPngPath = iconPngPath;
            return this;
        }

        public Builder fallbackGlyph(char fallbackGlyph) {
            this.fallbackGlyph = fallbackGlyph;
            return this;
        }

        public Builder accentColor(int accentColor) {
            this.accentColor = accentColor;
            return this;
        }

        public Builder pointHandleMode(PointHandleMode pointHandleMode) {
            this.pointHandleMode = Objects.requireNonNull(pointHandleMode, "pointHandleMode");
            return this;
        }

        public Builder includePrimaryVector(boolean includePrimaryVector) {
            this.includePrimaryVector = includePrimaryVector;
            return this;
        }

        public Builder includeSecondaryVector(boolean includeSecondaryVector) {
            this.includeSecondaryVector = includeSecondaryVector;
            return this;
        }

        public Builder includeThickness(boolean includeThickness) {
            this.includeThickness = includeThickness;
            return this;
        }

        public Builder includeAlwaysOnTop(boolean includeAlwaysOnTop) {
            this.includeAlwaysOnTop = includeAlwaysOnTop;
            return this;
        }

        public Builder includeTooltip(boolean includeTooltip) {
            this.includeTooltip = includeTooltip;
            return this;
        }

        public Builder includeText(boolean includeText) {
            this.includeText = includeText;
            return this;
        }

        public Builder includeMaxWidth(boolean includeMaxWidth) {
            this.includeMaxWidth = includeMaxWidth;
            return this;
        }

        public Builder includeBackgroundAlpha(boolean includeBackgroundAlpha) {
            this.includeBackgroundAlpha = includeBackgroundAlpha;
            return this;
        }

        public Builder primaryVectorLabel(GuidebookText primaryVectorLabel) {
            this.primaryVectorLabel = Objects.requireNonNull(primaryVectorLabel, "primaryVectorLabel");
            return this;
        }

        public Builder secondaryVectorLabel(@Nullable GuidebookText secondaryVectorLabel) {
            this.secondaryVectorLabel = secondaryVectorLabel;
            return this;
        }

        public Builder defaultColorLiteral(String defaultColorLiteral) {
            this.defaultColorLiteral = requireNonEmpty(defaultColorLiteral, "defaultColorLiteral");
            return this;
        }

        public Builder defaultThickness(float defaultThickness) {
            this.defaultThickness = defaultThickness;
            return this;
        }

        public Builder defaultText(String defaultText) {
            this.defaultText = defaultText != null ? defaultText : "";
            return this;
        }

        public Builder defaultMaxWidth(int defaultMaxWidth) {
            this.defaultMaxWidth = defaultMaxWidth;
            return this;
        }

        public Builder defaultBackgroundAlpha(int defaultBackgroundAlpha) {
            this.defaultBackgroundAlpha = Math.max(0, Math.min(255, defaultBackgroundAlpha));
            return this;
        }

        public SceneEditorElementType build() {
            if (includeSecondaryVector && !includePrimaryVector) {
                throw new IllegalArgumentException("Secondary vectors require a primary vector");
            }
            return new SceneEditorElementType(this);
        }

        public static String requireNonEmpty(String value, String name) {
            if (value == null || value.trim()
                .isEmpty()) {
                throw new IllegalArgumentException(name + " cannot be empty");
            }
            return value;
        }
    }
}
