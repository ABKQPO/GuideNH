package com.hfstudio.guidenh.mixins.early.minecraft;

import java.util.List;

import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hfstudio.guidenh.guide.siteexport.site.GuideSiteSceneTessellatorCapture;

@Mixin(ModelRenderer.class)
public abstract class MixinModelRendererSceneExportCapture {

    @Shadow
    public float rotationPointX;
    @Shadow
    public float rotationPointY;
    @Shadow
    public float rotationPointZ;
    @Shadow
    public float rotateAngleX;
    @Shadow
    public float rotateAngleY;
    @Shadow
    public float rotateAngleZ;
    @Shadow
    public boolean showModel;
    @Shadow
    public boolean isHidden;
    @Shadow
    public List<ModelBox> cubeList;
    @Shadow
    public List<ModelRenderer> childModels;
    @Shadow
    public float offsetX;
    @Shadow
    public float offsetY;
    @Shadow
    public float offsetZ;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void guidenh$captureRender(float scale, CallbackInfo ci) {
        if (GuideSiteSceneTessellatorCapture.getActive() == null) {
            return;
        }

        if (!isHidden && showModel) {
            GL11.glTranslatef(offsetX, offsetY, offsetZ);

            if (rotateAngleX == 0.0F && rotateAngleY == 0.0F && rotateAngleZ == 0.0F) {
                if (rotationPointX == 0.0F && rotationPointY == 0.0F && rotationPointZ == 0.0F) {
                    guidenh$renderModelGeometry(scale);
                    guidenh$renderChildren(scale);
                } else {
                    GL11.glTranslatef(rotationPointX * scale, rotationPointY * scale, rotationPointZ * scale);
                    guidenh$renderModelGeometry(scale);
                    guidenh$renderChildren(scale);
                    GL11.glTranslatef(-rotationPointX * scale, -rotationPointY * scale, -rotationPointZ * scale);
                }
            } else {
                GL11.glPushMatrix();
                GL11.glTranslatef(rotationPointX * scale, rotationPointY * scale, rotationPointZ * scale);

                if (rotateAngleZ != 0.0F) {
                    GL11.glRotatef(rotateAngleZ * (180F / (float) Math.PI), 0.0F, 0.0F, 1.0F);
                }
                if (rotateAngleY != 0.0F) {
                    GL11.glRotatef(rotateAngleY * (180F / (float) Math.PI), 0.0F, 1.0F, 0.0F);
                }
                if (rotateAngleX != 0.0F) {
                    GL11.glRotatef(rotateAngleX * (180F / (float) Math.PI), 1.0F, 0.0F, 0.0F);
                }

                guidenh$renderModelGeometry(scale);
                guidenh$renderChildren(scale);
                GL11.glPopMatrix();
            }

            GL11.glTranslatef(-offsetX, -offsetY, -offsetZ);
        }

        ci.cancel();
    }

    @Inject(method = "renderWithRotation", at = @At("HEAD"), cancellable = true)
    private void guidenh$captureRenderWithRotation(float scale, CallbackInfo ci) {
        if (GuideSiteSceneTessellatorCapture.getActive() == null) {
            return;
        }

        if (!isHidden && showModel) {
            GL11.glPushMatrix();
            GL11.glTranslatef(rotationPointX * scale, rotationPointY * scale, rotationPointZ * scale);

            if (rotateAngleY != 0.0F) {
                GL11.glRotatef(rotateAngleY * (180F / (float) Math.PI), 0.0F, 1.0F, 0.0F);
            }
            if (rotateAngleX != 0.0F) {
                GL11.glRotatef(rotateAngleX * (180F / (float) Math.PI), 1.0F, 0.0F, 0.0F);
            }
            if (rotateAngleZ != 0.0F) {
                GL11.glRotatef(rotateAngleZ * (180F / (float) Math.PI), 0.0F, 0.0F, 1.0F);
            }

            guidenh$renderModelGeometry(scale);
            GL11.glPopMatrix();
        }

        ci.cancel();
    }

    @Inject(method = "postRender", at = @At("HEAD"), cancellable = true)
    private void guidenh$capturePostRender(float scale, CallbackInfo ci) {
        if (GuideSiteSceneTessellatorCapture.getActive() == null) {
            return;
        }

        if (!isHidden && showModel) {
            if (rotateAngleX == 0.0F && rotateAngleY == 0.0F && rotateAngleZ == 0.0F) {
                if (rotationPointX != 0.0F || rotationPointY != 0.0F || rotationPointZ != 0.0F) {
                    GL11.glTranslatef(rotationPointX * scale, rotationPointY * scale, rotationPointZ * scale);
                }
            } else {
                GL11.glTranslatef(rotationPointX * scale, rotationPointY * scale, rotationPointZ * scale);

                if (rotateAngleZ != 0.0F) {
                    GL11.glRotatef(rotateAngleZ * (180F / (float) Math.PI), 0.0F, 0.0F, 1.0F);
                }
                if (rotateAngleY != 0.0F) {
                    GL11.glRotatef(rotateAngleY * (180F / (float) Math.PI), 0.0F, 1.0F, 0.0F);
                }
                if (rotateAngleX != 0.0F) {
                    GL11.glRotatef(rotateAngleX * (180F / (float) Math.PI), 1.0F, 0.0F, 0.0F);
                }
            }
        }

        ci.cancel();
    }

    @Unique
    private void guidenh$renderModelGeometry(float scale) {
        Tessellator tessellator = Tessellator.instance;
        for (int i = 0; i < cubeList.size(); ++i) {
            cubeList.get(i)
                .render(tessellator, scale);
        }
    }

    @Unique
    private void guidenh$renderChildren(float scale) {
        if (childModels == null) {
            return;
        }
        for (int i = 0; i < childModels.size(); ++i) {
            childModels.get(i)
                .render(scale);
        }
    }
}
