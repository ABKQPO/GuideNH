package com.hfstudio.guidenh.guide.document.block;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.guide.document.DefaultStyles;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.document.interaction.ItemTooltip;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;
import com.hfstudio.guidenh.guide.style.TextStyle;

public class LytItemImage extends LytBlock implements InteractiveElement {

    public static final int BASE_SIZE = 16;

    private static final int LABEL_GAP = 2;

    public static int DEFAULT_TEXT_INLINE_Y_OFFSET = -3;
    public static int DEFAULT_INLINE_Y_OFFSET = -4;

    protected ItemStack stack;
    private float scale = 1f;
    private boolean showTooltip = true;
    private boolean inline = false;
    @Nullable
    private Integer inlineYOffsetOverride = null;
    @Nullable
    private Integer labelYOffsetOverride = null;
    private boolean showIcon = true;
    @Nullable
    private String labelPosition = null;
    @Nullable
    private String labelFormat = null;
    @Nullable
    private ResolvedTextStyle cachedLabelStyle = null;
    @Nullable
    private String cachedLabelTemplate = null;

    public LytItemImage(ItemStack stack) {
        this.stack = stack;
    }

    public void setScale(float scale) {
        this.scale = Math.max(0.125f, scale);
    }

    public float getScale() {
        return scale;
    }

    /** Kept for backward compatibility. Prefer {@link #setShowTooltip(boolean)}. */
    public void setTooltipSuppressed(boolean suppressed) {
        this.showTooltip = !suppressed;
    }

    /** Controls whether hovering over this element shows an item tooltip. Default {@code true}. */
    public void setShowTooltip(boolean show) {
        this.showTooltip = show;
    }

    /** Controls whether the item icon graphic is rendered. Default {@code true}. */
    public void setShowIcon(boolean show) {
        this.showIcon = show;
    }

    /**
     * Sets the label text position relative to the icon.
     * Accepted values: {@code "left"}, {@code "right"}, or {@code null} for no label.
     */
    public void setLabelPosition(@Nullable String position) {
        this.labelPosition = position;
    }

    /**
     * Sets the label format pattern. Supports Markdown-style wrapping markers and an optional
     * {@code %s} placeholder for the item display name. {@code null} reverts to the default
     * (italic item display name).
     */
    public void setLabelFormat(@Nullable String format) {
        this.labelFormat = format;
        this.cachedLabelStyle = null;
        this.cachedLabelTemplate = null;
    }

    /**
     * Flag this image as being laid out inline with text. Only inline images receive the
     * {@link #DEFAULT_INLINE_Y_OFFSET} correction; block-level images render at their raw layout
     * position so they do not escape their reserved rect.
     */
    public void setInline(boolean inline) {
        this.inline = inline;
    }

    public void setInlineYOffsetOverride(@Nullable Integer override) {
        this.inlineYOffsetOverride = override;
    }

    /** Overrides the default inline Y offset for the label text only. Does not affect the icon. */
    public void setLabelYOffsetOverride(@Nullable Integer override) {
        this.labelYOffsetOverride = override;
    }

    public ItemStack getStack() {
        return stack;
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        int iconSize = Math.round(BASE_SIZE * scale);
        boolean hasLabel = labelPosition != null && stack != null;

        if (!showIcon && !hasLabel) {
            return new LytRect(x, y, 0, 0);
        }
        if (!hasLabel) {
            return new LytRect(x, y, iconSize, iconSize);
        }

        ResolvedTextStyle textStyle = resolveLabelStyle();
        String text = resolveLabelText();
        int textW = measureTextWidth(context, text, textStyle);
        int textH = context.getLineHeight(textStyle);

        if (!showIcon) {
            return new LytRect(x, y, textW, textH);
        }
        int totalW = iconSize + LABEL_GAP + textW;
        int totalH = Math.max(iconSize, textH);
        return new LytRect(x, y, totalW, totalH);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {}

    @Override
    public void render(RenderContext context) {
        if (stack == null || stack.stackSize == 0) return;

        int baseX = bounds.x();
        int baseY = bounds.y();
        int iconSize = Math.round(BASE_SIZE * scale);
        boolean hasLabel = labelPosition != null;

        int iconX = baseX;
        int textX = baseX;
        int textY = baseY;

        if (hasLabel) {
            ResolvedTextStyle textStyle = resolveLabelStyle();
            String text = resolveLabelText();
            int textW = context.getStringWidth(text, textStyle);
            int textH = context.getLineHeight(textStyle);
            int textVCenter = showIcon ? (iconSize - textH) / 2 : 0;

            if ("left".equals(labelPosition)) {
                textX = baseX;
                iconX = showIcon ? baseX + textW + LABEL_GAP : baseX;
            } else {
                iconX = baseX;
                textX = showIcon ? baseX + iconSize + LABEL_GAP : baseX;
            }
            textY = baseY + textVCenter;
            if (inline && showIcon) {
                int base = labelYOffsetOverride != null ? labelYOffsetOverride : DEFAULT_TEXT_INLINE_Y_OFFSET;
                textY += Math.round(base * scale);
            }
            context.drawText(text, textX, textY, textStyle);
        }

        if (showIcon) {
            int renderX = iconX;
            int renderY = baseY;
            if (inline) {
                int base = inlineYOffsetOverride != null ? inlineYOffsetOverride : DEFAULT_INLINE_Y_OFFSET;
                renderY += Math.round(base * scale);
            }
            if (scale == 1f) {
                context.renderItem(stack, renderX, renderY);
            } else {
                GL11.glPushMatrix();
                GL11.glTranslatef(renderX, renderY, 0);
                GL11.glScalef(scale, scale, 1f);
                context.renderItem(stack, 0, 0);
                GL11.glPopMatrix();
            }
        }
    }

    @Override
    public Optional<GuideTooltip> getTooltip(float x, float y) {
        if (!showTooltip) return Optional.empty();
        if (stack == null || stack.stackSize == 0) return Optional.empty();
        return Optional.of(new ItemTooltip(stack));
    }

    public List<ItemStack> getStacks() {
        return stack == null ? Collections.emptyList() : Collections.singletonList(stack);
    }

    /** Resolves the final label text based on the current stack and format pattern. */
    protected String resolveLabelText() {
        if (stack == null) return "";
        if (labelFormat == null) return stack.getDisplayName();
        if (cachedLabelTemplate == null) {
            cachedLabelTemplate = stripFormatMarkers(labelFormat);
        }
        return cachedLabelTemplate.contains("%s") ? String.format(cachedLabelTemplate, stack.getDisplayName())
            : cachedLabelTemplate;
    }

    /** Resolves the {@link ResolvedTextStyle} for the label based on the format pattern. */
    protected ResolvedTextStyle resolveLabelStyle() {
        if (cachedLabelStyle == null) {
            cachedLabelStyle = labelFormat == null ? TextStyle.builder()
                .italic(true)
                .build()
                .mergeWith(DefaultStyles.BASE_STYLE)
                : buildFormatStyle(labelFormat).mergeWith(DefaultStyles.BASE_STYLE);
        }
        return cachedLabelStyle;
    }

    private int measureTextWidth(LayoutContext context, String text, ResolvedTextStyle style) {
        float width = 0f;
        for (int offset = 0; offset < text.length();) {
            int cp = text.codePointAt(offset);
            width += context.getAdvance(cp, style);
            offset += Character.charCount(cp);
        }
        return Math.round(width);
    }

    private static boolean isWrapped(String s, String marker) {
        return s.length() > 2 * marker.length() && s.startsWith(marker) && s.endsWith(marker);
    }

    /**
     * Strips all wrapping Markdown-style markers from {@code s}, returning the inner text
     * (which may still contain a {@code %s} placeholder).
     */
    private static String stripFormatMarkers(String s) {
        boolean changed = true;
        while (changed) {
            changed = false;
            if (isWrapped(s, "~~")) {
                s = s.substring(2, s.length() - 2);
                changed = true;
            } else if (isWrapped(s, "**")) {
                s = s.substring(2, s.length() - 2);
                changed = true;
            } else if (isWrapped(s, "__")) {
                s = s.substring(2, s.length() - 2);
                changed = true;
            } else if (isWrapped(s, "^^")) {
                s = s.substring(2, s.length() - 2);
                changed = true;
            } else if (isWrapped(s, "::")) {
                s = s.substring(2, s.length() - 2);
                changed = true;
            } else if (isWrapped(s, "++")) {
                s = s.substring(2, s.length() - 2);
                changed = true;
            } else if (isWrapped(s, "*")) {
                s = s.substring(1, s.length() - 1);
                changed = true;
            } else if (isWrapped(s, "_")) {
                s = s.substring(1, s.length() - 1);
                changed = true;
            }
        }
        return s;
    }

    /**
     * Builds a {@link TextStyle} by interpreting wrapping Markdown-style markers in {@code format}.
     * Processed markers: {@code **bold**}, {@code *italic*}, {@code _italic_}, {@code ~~strike~~},
     * {@code __underline__}, {@code ++underline++}, {@code ^^wavy^^}, {@code ::dotted::}.
     */
    private static TextStyle buildFormatStyle(String s) {
        TextStyle.Builder builder = TextStyle.builder();
        boolean changed = true;
        while (changed) {
            changed = false;
            if (isWrapped(s, "~~")) {
                builder.strikethrough(true);
                s = s.substring(2, s.length() - 2);
                changed = true;
            } else if (isWrapped(s, "**")) {
                builder.bold(true);
                s = s.substring(2, s.length() - 2);
                changed = true;
            } else if (isWrapped(s, "__")) {
                builder.underlined(true);
                s = s.substring(2, s.length() - 2);
                changed = true;
            } else if (isWrapped(s, "^^")) {
                builder.wavyUnderline(true);
                s = s.substring(2, s.length() - 2);
                changed = true;
            } else if (isWrapped(s, "::")) {
                builder.dottedUnderline(true);
                s = s.substring(2, s.length() - 2);
                changed = true;
            } else if (isWrapped(s, "++")) {
                builder.underlined(true);
                s = s.substring(2, s.length() - 2);
                changed = true;
            } else if (isWrapped(s, "*")) {
                builder.italic(true);
                s = s.substring(1, s.length() - 1);
                changed = true;
            } else if (isWrapped(s, "_")) {
                builder.italic(true);
                s = s.substring(1, s.length() - 1);
                changed = true;
            }
        }
        return builder.build();
    }
}
