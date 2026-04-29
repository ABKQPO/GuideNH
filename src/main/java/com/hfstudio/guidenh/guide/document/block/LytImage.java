package com.hfstudio.guidenh.guide.document.block;

import java.util.Optional;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.GuiAssets;
import com.hfstudio.guidenh.guide.render.GuidePageTexture;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class LytImage extends LytBlock {

    private ResourceLocation imageId;
    private GuidePageTexture texture = GuidePageTexture.missing();
    private String title;
    private String alt;

    private int explicitWidth = -1;
    private int explicitHeight = -1;

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

    public void setImage(ResourceLocation id, @Nullable byte[] imageData) {
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
    public void render(RenderContext context) {
        if (texture == null) {
            context.fillIcon(getBounds(), GuiAssets.MISSING_TEXTURE);
        } else {
            context.fillTexturedRect(getBounds(), texture);
        }
    }

    public Optional<String> getTooltip(float x, float y) {
        if (title != null) {
            return Optional.of("tooltip");
        }
        return Optional.empty();
    }
}
