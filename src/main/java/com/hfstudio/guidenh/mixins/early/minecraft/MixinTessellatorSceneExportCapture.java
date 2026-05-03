package com.hfstudio.guidenh.mixins.early.minecraft;

import net.minecraft.client.renderer.Tessellator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.hfstudio.guidenh.guide.siteexport.site.GuideSiteSceneTessellatorCapture;

@Mixin(Tessellator.class)
public abstract class MixinTessellatorSceneExportCapture {

    @Unique
    private static final Logger LOGGER = LogManager.getLogger("GuideNH/TessCapture");

    @Inject(method = "startDrawing", at = @At("HEAD"))
    private void guidenh$captureStartDrawing(int drawMode, CallbackInfo ci) {
        GuideSiteSceneTessellatorCapture capture = GuideSiteSceneTessellatorCapture.getActive();
        if (capture == null) {
            return;
        }
        try {
            capture.startDrawing(drawMode);
        } catch (Throwable e) {
            LOGGER.warn("Scene capture startDrawing(mode={}) failed", drawMode, e);
        }
    }

    @Inject(method = "draw", at = @At("HEAD"))
    private void guidenh$captureDraw(CallbackInfoReturnable<Integer> cir) {
        GuideSiteSceneTessellatorCapture capture = GuideSiteSceneTessellatorCapture.getActive();
        if (capture == null) {
            return;
        }
        try {
            capture.draw();
        } catch (Throwable e) {
            LOGGER.warn("Scene capture draw failed", e);
        }
    }

    @Inject(method = "setTextureUV", at = @At("HEAD"))
    private void guidenh$captureTextureUv(double u, double v, CallbackInfo ci) {
        GuideSiteSceneTessellatorCapture capture = GuideSiteSceneTessellatorCapture.getActive();
        if (capture == null) {
            return;
        }
        capture.setTextureUV(u, v);
    }

    @Inject(method = "setBrightness", at = @At("HEAD"))
    private void guidenh$captureBrightness(int brightness, CallbackInfo ci) {
        GuideSiteSceneTessellatorCapture capture = GuideSiteSceneTessellatorCapture.getActive();
        if (capture == null) {
            return;
        }
        capture.setBrightness(brightness);
    }

    @Inject(method = "setColorOpaque_F", at = @At("HEAD"))
    private void guidenh$captureColorOpaqueF(float red, float green, float blue, CallbackInfo ci) {
        GuideSiteSceneTessellatorCapture capture = GuideSiteSceneTessellatorCapture.getActive();
        if (capture == null) {
            return;
        }
        capture.setColorOpaque_F(red, green, blue);
    }

    @Inject(method = "setColorRGBA_F", at = @At("HEAD"))
    private void guidenh$captureColorRgbaF(float red, float green, float blue, float alpha, CallbackInfo ci) {
        GuideSiteSceneTessellatorCapture capture = GuideSiteSceneTessellatorCapture.getActive();
        if (capture == null) {
            return;
        }
        capture.setColorRGBA_F(red, green, blue, alpha);
    }

    @Inject(method = "setColorOpaque", at = @At("HEAD"))
    private void guidenh$captureColorOpaque(int red, int green, int blue, CallbackInfo ci) {
        GuideSiteSceneTessellatorCapture capture = GuideSiteSceneTessellatorCapture.getActive();
        if (capture == null) {
            return;
        }
        capture.setColorOpaque(red, green, blue);
    }

    @Inject(method = "setColorRGBA", at = @At("HEAD"))
    private void guidenh$captureColorRgba(int red, int green, int blue, int alpha, CallbackInfo ci) {
        GuideSiteSceneTessellatorCapture capture = GuideSiteSceneTessellatorCapture.getActive();
        if (capture == null) {
            return;
        }
        capture.setColorRGBA(red, green, blue, alpha);
    }

    @Inject(method = "func_154352_a", at = @At("HEAD"))
    private void guidenh$capturePackedColor(byte red, byte green, byte blue, CallbackInfo ci) {
        GuideSiteSceneTessellatorCapture capture = GuideSiteSceneTessellatorCapture.getActive();
        if (capture == null) {
            return;
        }
        capture.func_154352_a(red, green, blue);
    }

    @Inject(method = "setColorOpaque_I", at = @At("HEAD"))
    private void guidenh$captureColorOpaqueI(int color, CallbackInfo ci) {
        GuideSiteSceneTessellatorCapture capture = GuideSiteSceneTessellatorCapture.getActive();
        if (capture == null) {
            return;
        }
        capture.setColorOpaque_I(color);
    }

    @Inject(method = "setColorRGBA_I", at = @At("HEAD"))
    private void guidenh$captureColorRgbaI(int color, int alpha, CallbackInfo ci) {
        GuideSiteSceneTessellatorCapture capture = GuideSiteSceneTessellatorCapture.getActive();
        if (capture == null) {
            return;
        }
        capture.setColorRGBA_I(color, alpha);
    }

    @Inject(method = "disableColor", at = @At("HEAD"))
    private void guidenh$captureDisableColor(CallbackInfo ci) {
        GuideSiteSceneTessellatorCapture capture = GuideSiteSceneTessellatorCapture.getActive();
        if (capture == null) {
            return;
        }
        capture.disableColor();
    }

    @Inject(method = "setNormal", at = @At("HEAD"))
    private void guidenh$captureNormal(float x, float y, float z, CallbackInfo ci) {
        GuideSiteSceneTessellatorCapture capture = GuideSiteSceneTessellatorCapture.getActive();
        if (capture == null) {
            return;
        }
        capture.setNormal(x, y, z);
    }

    @Inject(method = "setTranslation", at = @At("HEAD"))
    private void guidenh$captureSetTranslation(double x, double y, double z, CallbackInfo ci) {
        GuideSiteSceneTessellatorCapture capture = GuideSiteSceneTessellatorCapture.getActive();
        if (capture == null) {
            return;
        }
        capture.setTranslation(x, y, z);
    }

    @Inject(method = "addTranslation", at = @At("HEAD"))
    private void guidenh$captureAddTranslation(float x, float y, float z, CallbackInfo ci) {
        GuideSiteSceneTessellatorCapture capture = GuideSiteSceneTessellatorCapture.getActive();
        if (capture == null) {
            return;
        }
        capture.addTranslation(x, y, z);
    }

    @Inject(method = "addVertex", at = @At("HEAD"))
    private void guidenh$captureVertex(double x, double y, double z, CallbackInfo ci) {
        GuideSiteSceneTessellatorCapture capture = GuideSiteSceneTessellatorCapture.getActive();
        if (capture == null) {
            return;
        }
        capture.addVertex(x, y, z);
    }
}
