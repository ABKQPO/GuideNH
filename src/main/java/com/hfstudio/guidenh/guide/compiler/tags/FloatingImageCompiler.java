package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Random;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.compiler.IdUtils;
import com.hfstudio.guidenh.guide.compiler.IndexingContext;
import com.hfstudio.guidenh.guide.compiler.IndexingSink;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.ImageRegionAnnotation;
import com.hfstudio.guidenh.guide.document.block.LytImage;
import com.hfstudio.guidenh.guide.document.block.LytVBox;
import com.hfstudio.guidenh.guide.document.flow.InlineBlockAlignment;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.guide.document.interaction.ContentTooltip;
import com.hfstudio.guidenh.guide.sound.GuideSoundParsers;
import com.hfstudio.guidenh.guide.sound.GuideSoundTrigger;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

import cpw.mods.fml.common.FMLLog;

public class FloatingImageCompiler extends FlowTagCompiler {

    public static final String TAG_NAME = "FloatingImage";

    private static final Random RANDOM = new Random();

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton(TAG_NAME);
    }

    @Override
    protected void compile(PageCompiler compiler, LytFlowParent parent, MdxJsxElementFields el) {
        var src = el.getAttributeString("src", null);
        if (src == null || src.trim()
            .isEmpty()) {
            parent.appendError(compiler, "FloatingImage requires a non-empty src attribute.", el);
            return;
        }
        var align = el.getAttributeString("align", "left");
        var title = el.getAttributeString("title", null);
        int widthPx = parseIntAttr(el, "width", -1);
        int heightPx = parseIntAttr(el, "height", -1);

        var image = new LytImage();
        if (title != null) {
            image.setTitle(title);
        }
        image.setExplicitWidth(widthPx);
        image.setExplicitHeight(heightPx);
        try {
            var imageId = IdUtils.resolveLink(src, compiler.getPageId());
            var imageContent = compiler.loadAsset(imageId);
            if (imageContent == null) {
                FMLLog.getLogger()
                    .error("[GuideNH] [FloatingImageCompiler] Couldn't find image {}", src);
                image.setTitle("Missing image: " + src);
            }
            image.setImage(imageId, imageContent);
        } catch (IllegalArgumentException e) {
            FMLLog.getLogger()
                .error("[GuideNH] [FloatingImageCompiler] Invalid image id: {}", src);
            image.setTitle("Invalid image URL: " + src);
        }

        var wholeImageSound = GuideSoundParsers.parseAttributes(compiler, parent, el, "soundSrc");
        if (wholeImageSound != null) {
            var soundAnnotation = new ImageRegionAnnotation(false, ConstantColor.WHITE, 1);
            soundAnnotation.setSound(wholeImageSound);
            soundAnnotation.setSoundTrigger(parseTrigger(compiler, parent, el));
            image.addAnnotation(soundAnnotation);
        }

        // Parse <ImageAnnotation> child elements.
        var children = el.children();
        if (children != null) {
            for (var child : children) {
                if (child instanceof MdxJsxElementFields annEl && "ImageAnnotation".equals(annEl.name())) {
                    var ann = parseImageAnnotation(compiler, parent, annEl);
                    image.addAnnotation(ann);
                } else if (child instanceof MdxJsxElementFields soundEl && "SoundArea".equals(soundEl.name())) {
                    var ann = parseSoundArea(compiler, parent, soundEl);
                    if (ann != null) {
                        image.addAnnotation(ann);
                    }
                }
            }
        }

        // Wrap it in a flow content inline block
        var inlineBlock = new LytFlowInlineBlock();
        inlineBlock.setBlock(image);
        switch (align) {
            case "left" -> {
                inlineBlock.setAlignment(InlineBlockAlignment.FLOAT_LEFT);
                image.setMarginRight(5);
                image.setMarginBottom(5);
            }
            case "right" -> {
                inlineBlock.setAlignment(InlineBlockAlignment.FLOAT_RIGHT);
                image.setMarginLeft(5);
                image.setMarginBottom(5);
            }
            default -> {
                parent.append(compiler.createErrorFlowContent("Invalid align. Must be left or right.", el));
                return;
            }
        }

        parent.append(inlineBlock);
    }

    /**
     * Parses a single {@code <ImageAnnotation>} child element into an {@link ImageRegionAnnotation}.
     * Returns {@code null} only when a fatal parse error occurs.
     *
     * <p>
     * Attributes:
     * <ul>
     * <li>{@code x}, {@code y}, {@code w}, {@code h}: region in image pixels; omitting all four
     * means the annotation covers the whole image.</li>
     * <li>{@code border}: boolean flag; presence (or {@code {true}}) enables the border.</li>
     * <li>{@code borderColor}: {@code #RRGGBB} or {@code #AARRGGBB}; omit for a random color.</li>
     * <li>{@code borderThickness}: integer pixel thickness, default 1.</li>
     * </ul>
     * Child MDX content is compiled as the rich-text tooltip body.
     */
    @NotNull
    private static ImageRegionAnnotation parseImageAnnotation(PageCompiler compiler, LytFlowParent parent,
        MdxJsxElementFields annEl) {
        ImageRegionAnnotation ann = parseImageAnnotationRegion(compiler, parent, annEl, true);

        // Compile tooltip rich-text content from child elements.
        var contentBox = new LytVBox();
        compiler.compileBlockTagChildren(annEl, contentBox);
        if (!contentBox.getChildren()
            .isEmpty()) {
            ann.setTooltip(new ContentTooltip(contentBox));
        }
        ann.setSound(GuideSoundParsers.parseAttributes(compiler, parent, annEl));
        ann.setSoundTrigger(parseTrigger(compiler, parent, annEl));

        return ann;
    }

    private static ImageRegionAnnotation parseSoundArea(PageCompiler compiler, LytFlowParent parent,
        MdxJsxElementFields el) {
        var sound = GuideSoundParsers.parseAttributes(compiler, parent, el);
        if (sound == null) {
            parent.appendError(compiler, "SoundArea requires a sound or src attribute.", el);
            return null;
        }
        ImageRegionAnnotation ann = parseImageAnnotationRegion(compiler, parent, el, false);
        ann.setSound(sound);
        ann.setSoundTrigger(parseTrigger(compiler, parent, el));
        return ann;
    }

    private static ImageRegionAnnotation parseImageAnnotationRegion(PageCompiler compiler, LytFlowParent parent,
        MdxJsxElementFields el, boolean allowBorder) {
        int x = MdxAttrs.getInt(compiler, parent, el, "x", -1);
        int y = MdxAttrs.getInt(compiler, parent, el, "y", -1);
        int w = MdxAttrs.getInt(compiler, parent, el, "w", -1);
        int h = MdxAttrs.getInt(compiler, parent, el, "h", -1);
        boolean wholeImage = x < 0 && y < 0 && w < 0 && h < 0;

        boolean showBorder = allowBorder && MdxAttrs.getBoolean(compiler, parent, el, "border", false);
        int borderThickness = allowBorder ? MdxAttrs.getInt(compiler, parent, el, "borderThickness", 1) : 1;

        ColorValue borderColor;
        if (allowBorder && el.getAttribute("borderColor") != null) {
            borderColor = MdxAttrs.getColor(compiler, parent, el, "borderColor", ConstantColor.WHITE);
        } else {
            borderColor = allowBorder ? new ConstantColor(0xFF000000 | RANDOM.nextInt(0x1000000)) : ConstantColor.WHITE;
        }

        if (wholeImage) {
            return new ImageRegionAnnotation(showBorder, borderColor, borderThickness);
        }

        int ax = Math.max(x, 0);
        int ay = Math.max(y, 0);
        int aw = Math.max(1, w < 0 ? 1 : w);
        int ah = Math.max(1, h < 0 ? 1 : h);
        return new ImageRegionAnnotation(ax, ay, aw, ah, showBorder, borderColor, borderThickness);
    }

    private static GuideSoundTrigger parseTrigger(PageCompiler compiler, LytFlowParent parent, MdxJsxElementFields el) {
        return GuideSoundTrigger
            .parse(MdxAttrs.getString(compiler, parent, el, "trigger", null), GuideSoundTrigger.CLICK);
    }

    @Override
    public void index(IndexingContext indexer, MdxJsxElementFields el, IndexingSink sink) {
        var title = el.getAttributeString("title", null);
        if (title != null) {
            sink.appendText(el, title);
        }
    }

    public static int parseIntAttr(MdxJsxElementFields el, String name, int def) {
        var s = el.getAttributeString(name, null);
        if (s == null || s.isEmpty()) return def;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException ex) {
            return def;
        }
    }
}
