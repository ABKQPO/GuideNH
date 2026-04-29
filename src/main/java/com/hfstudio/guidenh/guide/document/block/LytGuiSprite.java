package com.hfstudio.guidenh.guide.document.block;

import javax.annotation.Nullable;

import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.LytSize;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.GuiSprite;
import com.hfstudio.guidenh.guide.render.RenderContext;

/**
 * Render a {@link GuiSprite}.
 */
public class LytGuiSprite extends LytBlock implements InteractiveElement {

    @Nullable
    private GuiSprite sprite;

    private ColorValue color = ConstantColor.WHITE;

    private LytSize size = new LytSize(16, 16);

    public LytGuiSprite() {}

    public LytGuiSprite(GuiSprite sprite, LytSize size) {
        this.sprite = sprite;
        this.size = size;
    }

    @Nullable
    public GuiSprite getSprite() {
        return sprite;
    }

    public void setSprite(@Nullable GuiSprite sprite) {
        this.sprite = sprite;
    }

    public ColorValue getColor() {
        return color;
    }

    public void setColor(ColorValue color) {
        this.color = color != null ? color : ConstantColor.WHITE;
    }

    public LytSize getSize() {
        return size;
    }

    public void setSize(LytSize size) {
        this.size = size;
    }

    public void setSize(int width, int height) {
        setSize(new LytSize(width, height));
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        float actualWidth = size.width();
        float actualHeight = size.height();

        if (actualWidth > availableWidth) {
            var f = availableWidth / actualWidth;
            actualWidth *= f;
            actualHeight *= f;
        }

        return new LytRect(x, y, Math.round(actualWidth), Math.round(actualHeight));
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {}

    @Override
    public void render(RenderContext context) {
        if (sprite != null) {
            context.fillIcon(getBounds(), sprite, color);
        }
    }
}
