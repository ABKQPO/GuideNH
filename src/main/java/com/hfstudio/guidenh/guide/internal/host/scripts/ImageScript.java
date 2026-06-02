package com.hfstudio.guidenh.guide.internal.host.scripts;

import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.document.block.ImageRegionAnnotation;
import com.hfstudio.guidenh.guide.document.block.LytImage;
import com.hfstudio.guidenh.guide.document.block.LytImageBlock;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.internal.host.EventType;
import com.hfstudio.guidenh.guide.internal.host.LytEvent;
import com.hfstudio.guidenh.guide.internal.host.LytScript;
import com.hfstudio.guidenh.guide.internal.host.ScriptContext;
import com.hfstudio.guidenh.guide.internal.host.ScriptType;

public class ImageScript implements LytScript {

    @Override
    public ScriptType type() { return ScriptType.JAVA; }

    @Override
    public String styleClass() { return "Img"; }

    @Override
    public boolean isAsync() { return true; }

    @Override
    public void onEvent(Object node, LytEvent event, ScriptContext ctx) {
        if (event.type() != EventType.MOUNT) return;

        LytImageBlock placeholder = LytFlowInlineBlock.unwrapPlaceholder(node, LytImageBlock.class);
        if (placeholder == null) return;
        boolean isWrapped = node instanceof LytFlowInlineBlock;

        String src = placeholder.getSrc();
        if (src == null || src.isEmpty()) {
            replaceFlowError(ctx, isWrapped, "[Image] Missing src attribute");
            return;
        }

        ResourceLocation imageId;
        try {
            imageId = new ResourceLocation(src);
        } catch (Exception e) {
            replaceFlowError(ctx, isWrapped, "[Image] Invalid image path: " + src);
            return;
        }

        byte[] imageData = ctx.loadAsset(imageId);
        LytImage image = new LytImage();
        image.setImage(imageId, imageData); // null imageData → GuidePageTexture.missing()

        String alt = placeholder.getAlt();
        if (alt != null && !alt.isEmpty()) image.setAlt(alt);
        String title = placeholder.getTitle();
        if (title != null && !title.isEmpty()) image.setTitle(title);
        image.setExplicitWidth(placeholder.getExplicitWidth());
        image.setExplicitHeight(placeholder.getExplicitHeight());
        for (ImageRegionAnnotation ann : placeholder.getAnnotations()) {
            image.addAnnotation(ann);
        }

        if (isWrapped) {
            LytFlowInlineBlock newWrapper = new LytFlowInlineBlock();
            newWrapper.setBlock(image);
            ctx.replace(newWrapper);
        } else {
            ctx.replace(image);
        }
    }

    private void replaceFlowError(ScriptContext ctx, boolean isWrapped, String message) {
        LytParagraph error = LytParagraph.error(message);
        if (isWrapped) {
            LytFlowInlineBlock wrapper = new LytFlowInlineBlock();
            wrapper.setBlock(error);
            ctx.replace(wrapper);
        } else {
            ctx.replace(error);
        }
    }
}
