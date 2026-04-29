package com.hfstudio.guidenh.mixins.early.minecraft;

import net.minecraft.client.renderer.Tessellator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.hfstudio.guidenh.guide.siteexport.site.GuideSiteSceneTessellatorCapture;

@Mixin(Tessellator.class)
public abstract class MixinTessellatorSceneExportCapture {

    @Inject(method = "startDrawing", at = @At("HEAD"), cancellable = true)
    private void guidenh$captureStartDrawing(int drawMode, CallbackInfo ci) {
        GuideSiteSceneTessellatorCapture capture = GuideSiteSceneTessellatorCapture.getActive();
        if (capture == null) {
            return;
        }
        capture.startDrawing(drawMode);
        ci.cancel();
    }

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true)
    private void guidenh$captureDraw(CallbackInfoReturnable<Integer> cir) {
        GuideSiteSceneTessellatorCapture capture = GuideSiteSceneTessellatorCapture.getActive();
        if (capture == null) {
            return;
        }
        cir.setReturnValue(Integer.valueOf(capture.draw()));
    }

    @Inject(method = "setTextureUV", at = @At("HEAD"), cancellable = true)
    private void guidenh$captureTextureUv(double u, double v, CallbackInfo ci) {
        GuideSiteSceneTessellatorCapture capture = GuideSiteSceneTessellatorCapture.getActive();
        if (capture == null) {
            return;
        }
        capture.setTextureUV(u, v);
        ci.cancel();
    }

    @Inject(method = "setBrightness", at = @At("HEAD"), cancellable = true)
    private void guidenh$captureBrightness(int brightness, CallbackInfo ci) {
        GuideSiteSceneTessellatorCapture capture = GuideSiteSceneTessellatorCapture.getActive();
        if (capture == null) {
            return;
        }
        capture.setBrightness(brightness);
        ci.cancel();
    }

    @Inject(method = "setColorOpaque_F", at = @At("HEAD"), cancellable = true)
    private void guidenh$captureColorOpaqueF(float red, float green, float blue, CallbackInfo ci) {
        GuideSiteSceneTessellatorCapture capture = GuideSiteSceneTessellatorCapture.getActive();
        if (capture == null) {
            return;
        }
        capture.setColorOpaque_F(red, green, blue);
        ci.cancel();
    }

    @Inject(method = "setColorRGBA_F", at = @At("HEAD"), cancellable = true)
    private void guidenh$captureColorRgbaF(float red, float green, float blue, float alpha, CallbackInfo ci) {
        GuideSiteSceneTessellatorCapture capture = GuideSiteSceneTessellatorCapture.getActive();
        if (capture == null) {
            return;
        }
        capture.setColorRGBA_F(red, green, blue, alpha);
        ci.cancel();
    }

    @Inject(method = "setColorOpaque", at = @At("HEAD"), cancellable = true)
    private void guidenh$captureColorOpaque(int red, int green, int blue, CallbackInfo ci) {
        GuideSiteSceneTessellatorCapture capture = GuideSiteSceneTessellatorCapture.getActive();
        if (capture == null) {
            return;
        }
        capture.setColorOpaque(red, green, blue);
        ci.cancel();
    }

    @Inject(method = "setColorRGBA", at = @At("HEAD"), cancellable = true)
    private void guidenh$captureColorRgba(int red, int green, int blue, int alpha, CallbackInfo ci) {
        GuideSiteSceneTessellatorCapture capture = GuideSiteSceneTessellatorCapture.getActive();
        if (capture == null) {
            return;
        }
        capture.setColorRGBA(red, green, blue, alpha);
        ci.cancel();
    }

    @Inject(method = "func_154352_a", at = @At("HEAD"), cancellable = true)
    private void guidenh$capturePackedColor(byte red, byte green, byte blue, CallbackInfo ci) {
        GuideSiteSceneTessellatorCapture capture = GuideSiteSceneTessellatorCapture.getActive();
        if (capture == null) {
            return;
        }
        capture.func_154352_a(red, green, blue);
        ci.cancel();
    }

    @Inject(method = "setColorOpaque_I", at = @At("HEAD"), cancellable = true)
    private void guidenh$captureColorOpaqueI(int color, CallbackInfo ci) {
        GuideSiteSceneTessellatorCapture capture = GuideSiteSceneTessellatorCapture.getActive();
        if (capture == null) {
            return;
        }
        capture.setColorOpaque_I(color);
        ci.cancel();
    }

    @Inject(method = "setColorRGBA_I", at = @At("HEAD"), cancellable = true)
    private void guidenh$captureColorRgbaI(int color, int alpha, CallbackInfo ci) {
        GuideSiteSceneTessellatorCapture capture = GuideSiteSceneTessellatorCapture.getActive();
        if (capture == null) {
            return;
        }
        capture.setColorRGBA_I(color, alpha);
        ci.cancel();
    }

    @Inject(method = "disableColor", at = @At("HEAD"), cancellable = true)
    private void guidenh$captureDisableColor(CallbackInfo ci) {
        GuideSiteSceneTessellatorCapture capture = GuideSiteSceneTessellatorCapture.getActive();
        if (capture == null) {
            return;
        }
        capture.disableColor();
        ci.cancel();
    }

    @Inject(method = "setNormal", at = @At("HEAD"), cancellable = true)
    private void guidenh$captureNormal(float x, float y, float z, CallbackInfo ci) {
        GuideSiteSceneTessellatorCapture capture = GuideSiteSceneTessellatorCapture.getActive();
        if (capture == null) {
            return;
        }
        capture.setNormal(x, y, z);
        ci.cancel();
    }

    @Inject(method = "setTranslation", at = @At("HEAD"), cancellable = true)
    private void guidenh$captureSetTranslation(double x, double y, double z, CallbackInfo ci) {
        GuideSiteSceneTessellatorCapture capture = GuideSiteSceneTessellatorCapture.getActive();
        if (capture == null) {
            return;
        }
        capture.setTranslation(x, y, z);
        ci.cancel();
    }

    @Inject(method = "addTranslation", at = @At("HEAD"), cancellable = true)
    private void guidenh$captureAddTranslation(float x, float y, float z, CallbackInfo ci) {
        GuideSiteSceneTessellatorCapture capture = GuideSiteSceneTessellatorCapture.getActive();
        if (capture == null) {
            return;
        }
        capture.addTranslation(x, y, z);
        ci.cancel();
    }

    @Inject(method = "addVertex", at = @At("HEAD"), cancellable = true)
    private void guidenh$captureVertex(double x, double y, double z, CallbackInfo ci) {
        GuideSiteSceneTessellatorCapture capture = GuideSiteSceneTessellatorCapture.getActive();
        if (capture == null) {
            return;
        }
        capture.addVertex(x, y, z);
        ci.cancel();
    }

    @Inject(method = "addVertexWithUV", at = @At("HEAD"), cancellable = true)
    private void guidenh$captureVertexWithUv(double x, double y, double z, double u, double v, CallbackInfo ci) {
        GuideSiteSceneTessellatorCapture capture = GuideSiteSceneTessellatorCapture.getActive();
        if (capture == null) {
            return;
        }
        capture.addVertexWithUV(x, y, z, u, v);
        ci.cancel();
    }
}
