package com.hfstudio.guidenh.guide.document.block;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * A placeholder block for images that will be materialized by a LytScript.
 * The styleClass (e.g., "Img" or "FloatingImage") tells LytHost which script handles it.
 */
public class LytImageBlock extends LytParagraph {

    @Nullable
    private String src;
    @Nullable
    private String alt;
    @Nullable
    private String title;
    private int explicitWidth = -1;
    private int explicitHeight = -1;
    @Nullable
    private String align;
    private final List<ImageRegionAnnotation> annotations = new ArrayList<>();

    @Nullable
    public String getSrc() {
        return src;
    }

    public void setSrc(@Nullable String src) {
        this.src = src;
    }

    @Nullable
    public String getAlt() {
        return alt;
    }

    public void setAlt(@Nullable String alt) {
        this.alt = alt;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    public void setTitle(@Nullable String title) {
        this.title = title;
    }

    public int getExplicitWidth() {
        return explicitWidth;
    }

    public void setExplicitWidth(int explicitWidth) {
        this.explicitWidth = explicitWidth > 0 ? explicitWidth : -1;
    }

    public int getExplicitHeight() {
        return explicitHeight;
    }

    public void setExplicitHeight(int explicitHeight) {
        this.explicitHeight = explicitHeight > 0 ? explicitHeight : -1;
    }

    @Nullable
    public String getAlign() {
        return align;
    }

    public void setAlign(@Nullable String align) {
        this.align = align;
    }

    public void addAnnotation(ImageRegionAnnotation annotation) {
        if (annotation != null) {
            annotations.add(annotation);
        }
    }

    public List<ImageRegionAnnotation> getAnnotations() {
        return annotations;
    }
}
