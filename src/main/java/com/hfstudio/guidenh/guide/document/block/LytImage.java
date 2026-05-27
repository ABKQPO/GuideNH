package com.hfstudio.guidenh.guide.document.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.GuiAssets;
import com.hfstudio.guidenh.guide.render.GuidePageTexture;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.sound.GuideSoundPlayback;
import com.hfstudio.guidenh.guide.sound.GuideSoundSpec;
import com.hfstudio.guidenh.guide.sound.GuideSoundTrigger;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;

public class LytImage extends LytBlock implements InteractiveElement {

    private ResourceLocation imageId;
    private GuidePageTexture texture = GuidePageTexture.missing();
    private String title;
    private String alt;

    private int explicitWidth = -1;
    private int explicitHeight = -1;

    private final List<ImageRegionAnnotation> annotations = new ArrayList<>();
    @Nullable
    private ImageRegionAnnotation hoveredSoundAnnotation;

    public ResourceLocation getImageId() {
        return imageId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAlt() {
        return alt;
    }

    public void setAlt(String alt) {
        this.alt = alt;
    }

    public void setImage(ResourceLocation id, byte @Nullable [] imageData) {
        this.imageId = id;
        if (imageData != null) {
            this.texture = GuidePageTexture.load(id, imageData);
        } else {
            this.texture = GuidePageTexture.missing();
        }
    }

    public void setTexture(@Nullable ResourceLocation id, @Nullable GuidePageTexture texture) {
        this.imageId = id;
        this.texture = texture != null ? texture : GuidePageTexture.missing();
    }

    public void setExplicitWidth(int width) {
        this.explicitWidth = width > 0 ? width : -1;
    }

    public void setExplicitHeight(int height) {
        this.explicitHeight = height > 0 ? height : -1;
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        if (texture == null) {
            return new LytRect(x, y, 32, 32);
        }

        var size = texture.getSize();
        int natW = Math.max(1, size.width());
        int natH = Math.max(1, size.height());

        int width;
        int height;
        if (explicitWidth > 0 && explicitHeight > 0) {
            width = explicitWidth;
            height = explicitHeight;
        } else if (explicitWidth > 0) {
            width = explicitWidth;
            height = Math.max(1, Math.round(explicitWidth * (natH / (float) natW)));
        } else if (explicitHeight > 0) {
            height = explicitHeight;
            width = Math.max(1, Math.round(explicitHeight * (natW / (float) natH)));
        } else {
            width = natW / 4;
            height = natH / 4;
        }

        float visualScale = context.getVisualScale();
        if (visualScale < 0.999f) {
            width = Math.max(1, Math.round(width * visualScale));
            height = Math.max(1, Math.round(height * visualScale));
        }

        if (width > availableWidth) {
            var f = availableWidth / (float) width;
            width = Math.max(1, Math.round(width * f));
            height = Math.max(1, Math.round(height * f));
        }

        return new LytRect(x, y, width, height);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {}

    @Override
    public void onMouseLeave() {
        hoveredSoundAnnotation = null;
    }

    @Override
    public void render(RenderContext context) {
        if (texture == null) {
            context.fillIcon(getBounds(), GuiAssets.MISSING_TEXTURE);
        } else {
            context.fillTexturedRect(getBounds(), texture);
        }
        drawAnnotationBorders(context);
    }

    private void drawAnnotationBorders(RenderContext context) {
        if (annotations.isEmpty()) {
            return;
        }
        var bounds = getBounds();
        int dispW = bounds.width();
        int dispH = bounds.height();
        if (dispW <= 0 || dispH <= 0) {
            return;
        }
        int natW;
        int natH;
        if (texture != null && !texture.isMissing()) {
            var size = texture.getSize();
            natW = Math.max(1, size.width());
            natH = Math.max(1, size.height());
        } else {
            natW = dispW;
            natH = dispH;
        }
        for (var ann : annotations) {
            if (!ann.isShowBorder()) {
                continue;
            }
            int bx;
            int by;
            int bw;
            int bh;
            if (ann.isWholeImage()) {
                bx = bounds.x();
                by = bounds.y();
                bw = bounds.width();
                bh = bounds.height();
            } else {
                // Clamp the annotation region to [0, natW] x [0, natH] so the border
                // cannot extend beyond the displayed image area regardless of scaling.
                int clampedX = Math.max(0, Math.min(ann.getImgX(), natW));
                int clampedY = Math.max(0, Math.min(ann.getImgY(), natH));
                int clampedW = Math.min(ann.getImgX() + ann.getImgW(), natW) - clampedX;
                int clampedH = Math.min(ann.getImgY() + ann.getImgH(), natH) - clampedY;
                if (clampedW <= 0 || clampedH <= 0) {
                    continue;
                }
                bx = bounds.x() + clampedX * dispW / natW;
                by = bounds.y() + clampedY * dispH / natH;
                bw = Math.max(1, clampedW * dispW / natW);
                bh = Math.max(1, clampedH * dispH / natH);
            }
            context.drawBorder(bx, by, bw, bh, context.resolveColor(ann.getBorderColor()), ann.getBorderThickness());
        }
    }

    /**
     * Adds a region annotation to this image. Annotations are tested in reverse insertion order
     * (last-added wins) when querying the tooltip for a given cursor position.
     */
    public void addAnnotation(ImageRegionAnnotation annotation) {
        if (annotation != null) {
            annotations.add(annotation);
        }
    }

    public List<ImageRegionAnnotation> getAnnotations() {
        return annotations;
    }

    @Override
    public Optional<GuideTooltip> getTooltip(float x, float y) {
        playHoverSound(x, y);
        if (annotations.isEmpty()) {
            return Optional.empty();
        }
        var bounds = getBounds();
        if (texture == null || texture.isMissing()) {
            return Optional.empty();
        }
        int dispW = bounds.width();
        int dispH = bounds.height();
        if (dispW <= 0 || dispH <= 0) {
            return Optional.empty();
        }
        var size = texture.getSize();
        int natW = Math.max(1, size.width());
        int natH = Math.max(1, size.height());
        float localX = x - bounds.x();
        float localY = y - bounds.y();
        float imgPx = localX * natW / dispW;
        float imgPy = localY * natH / dispH;
        for (int i = annotations.size() - 1; i >= 0; i--) {
            var ann = annotations.get(i);
            if (ann.getTooltip() == null) {
                continue;
            }
            if (ann.containsImagePoint(imgPx, imgPy)) {
                return Optional.of(ann.getTooltip());
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean mouseClicked(GuideUiHost screen, int x, int y, int button, boolean doubleClick) {
        if (button != 0) {
            return false;
        }
        ImageRegionAnnotation annotation = findSoundAnnotation(x, y, GuideSoundTrigger.CLICK);
        if (annotation == null) {
            return false;
        }
        return GuideSoundPlayback.play(annotation.getSound());
    }

    private void playHoverSound(float x, float y) {
        ImageRegionAnnotation annotation = findSoundAnnotation(x, y, GuideSoundTrigger.HOVER);
        if (annotation == hoveredSoundAnnotation) {
            return;
        }
        hoveredSoundAnnotation = annotation;
        if (annotation != null) {
            GuideSoundPlayback.play(annotation.getSound());
        }
    }

    @Nullable
    private ImageRegionAnnotation findSoundAnnotation(float x, float y, GuideSoundTrigger trigger) {
        if (annotations.isEmpty()) {
            return null;
        }
        ImagePoint point = toImagePoint(x, y);
        if (point == null) {
            return null;
        }
        for (int i = annotations.size() - 1; i >= 0; i--) {
            ImageRegionAnnotation annotation = annotations.get(i);
            GuideSoundSpec sound = annotation.getSound();
            if (sound == null || annotation.getSoundTrigger() != trigger) {
                continue;
            }
            if (annotation.containsImagePoint(point.x, point.y)) {
                return annotation;
            }
        }
        return null;
    }

    @Nullable
    private ImagePoint toImagePoint(float x, float y) {
        var bounds = getBounds();
        if (texture == null || texture.isMissing()) {
            return null;
        }
        int dispW = bounds.width();
        int dispH = bounds.height();
        if (dispW <= 0 || dispH <= 0
            || x < bounds.x()
            || x >= bounds.right()
            || y < bounds.y()
            || y >= bounds.bottom()) {
            return null;
        }
        var size = texture.getSize();
        int natW = Math.max(1, size.width());
        int natH = Math.max(1, size.height());
        float localX = x - bounds.x();
        float localY = y - bounds.y();
        return new ImagePoint(localX * natW / dispW, localY * natH / dispH);
    }

    public static class ImagePoint {

        public final float x;
        public final float y;

        public ImagePoint(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}
