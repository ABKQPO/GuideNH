package com.hfstudio.guidenh.mixins.early.minecraft;

import net.minecraft.client.renderer.Tessellator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.guide.siteexport.site.GuideSiteSceneTessellatorCapture;

@Mixin(Tessellator.class)
public abstract class MixinTessellatorSceneExportCapture {

    @Shadow
    private int[] rawBuffer;

    @Shadow
    private int vertexCount;

    @Shadow
    private boolean hasTexture;

    @Shadow
    private boolean hasColor;

    @Shadow
    private boolean hasBrightness;

    @Shadow
    private boolean hasNormals;

    @Inject(method = "startDrawing", at = @At("HEAD"))
    private void guidenh$captureStartDrawing(int drawMode, CallbackInfo ci) {
        GuideSiteSceneTessellatorCapture capture = GuideSiteSceneTessellatorCapture.getActive();
        if (capture == null) {
            return;
        }
        try {
            capture.startDrawing(drawMode);
        } catch (Throwable e) {
            GuideDebugLog.warnAlways("Scene capture startDrawing(mode={}) failed", drawMode, e);
        }
    }

    @Inject(method = "draw", at = @At("HEAD"))
    private void guidenh$captureDraw(CallbackInfoReturnable<Integer> cir) {
        GuideSiteSceneTessellatorCapture capture = GuideSiteSceneTessellatorCapture.getActive();
        if (capture == null) {
            return;
        }
        try {
            capture.captureRawBuffer(rawBuffer, vertexCount, hasTexture, hasColor, hasBrightness, hasNormals);
            capture.draw();
        } catch (Throwable e) {
            GuideDebugLog.warnAlways("Scene capture draw failed", e);
        }
    }
}
