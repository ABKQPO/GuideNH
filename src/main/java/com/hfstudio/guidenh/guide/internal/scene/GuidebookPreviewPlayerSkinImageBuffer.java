package com.hfstudio.guidenh.guide.internal.scene;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import net.minecraft.client.renderer.IImageBuffer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuidebookPreviewPlayerSkinImageBuffer implements IImageBuffer {

    private static final int IMAGE_WIDTH = 64;
    private static final int IMAGE_HEIGHT = 64;
    private static final int LEGACY_IMAGE_HEIGHT = 32;

    private int[] imageData;

    @Override
    public BufferedImage parseUserSkin(BufferedImage sourceImage) {
        if (sourceImage == null) {
            return null;
        }

        BufferedImage outputImage = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = outputImage.getGraphics();
        graphics.drawImage(sourceImage, 0, 0, null);

        if (sourceImage.getHeight() == LEGACY_IMAGE_HEIGHT) {
            graphics.setColor(new Color(0, 0, 0, 0));
            graphics.fillRect(0, LEGACY_IMAGE_HEIGHT, IMAGE_WIDTH, LEGACY_IMAGE_HEIGHT);
            copyLegacySkinLayout(graphics, sourceImage);
        }

        graphics.dispose();
        this.imageData = ((DataBufferInt) outputImage.getRaster()
            .getDataBuffer()).getData();

        setAreaOpaque(0, 0, 32, 16);
        setAreaOpaque(0, 16, 64, 32);
        setAreaOpaque(16, 48, 48, 64);
        setAreaTransparent(32, 0, 64, 32);
        setAreaTransparent(0, 32, 16, 48);
        setAreaTransparent(16, 32, 40, 48);
        setAreaTransparent(40, 32, 56, 48);
        setAreaTransparent(0, 48, 16, 64);
        setAreaTransparent(48, 48, 64, 64);
        return outputImage;
    }

    @Override
    public void func_152634_a() {}

    private void copyLegacySkinLayout(Graphics graphics, BufferedImage legacyImage) {
        graphics.drawImage(legacyImage, 24, 48, 20, 52, 4, 16, 8, 20, null);
        graphics.drawImage(legacyImage, 28, 48, 24, 52, 8, 16, 12, 20, null);
        graphics.drawImage(legacyImage, 20, 52, 16, 64, 8, 20, 12, 32, null);
        graphics.drawImage(legacyImage, 24, 52, 20, 64, 4, 20, 8, 32, null);
        graphics.drawImage(legacyImage, 28, 52, 24, 64, 0, 20, 4, 32, null);
        graphics.drawImage(legacyImage, 32, 52, 28, 64, 12, 20, 16, 32, null);
        graphics.drawImage(legacyImage, 40, 48, 36, 52, 44, 16, 48, 20, null);
        graphics.drawImage(legacyImage, 44, 48, 40, 52, 48, 16, 52, 20, null);
        graphics.drawImage(legacyImage, 36, 52, 32, 64, 48, 20, 52, 32, null);
        graphics.drawImage(legacyImage, 40, 52, 36, 64, 44, 20, 48, 32, null);
        graphics.drawImage(legacyImage, 44, 52, 40, 64, 40, 20, 44, 32, null);
        graphics.drawImage(legacyImage, 48, 52, 44, 64, 52, 20, 56, 32, null);
    }

    private void setAreaTransparent(int minX, int minY, int maxX, int maxY) {
        if (!hasTransparency(minX, minY, maxX, maxY)) {
            for (int x = minX; x < maxX; ++x) {
                for (int y = minY; y < maxY; ++y) {
                    this.imageData[x + y * IMAGE_WIDTH] &= 0x00FFFFFF;
                }
            }
        }
    }

    private void setAreaOpaque(int minX, int minY, int maxX, int maxY) {
        for (int x = minX; x < maxX; ++x) {
            for (int y = minY; y < maxY; ++y) {
                this.imageData[x + y * IMAGE_WIDTH] |= 0xFF000000;
            }
        }
    }

    private boolean hasTransparency(int minX, int minY, int maxX, int maxY) {
        for (int x = minX; x < maxX; ++x) {
            for (int y = minY; y < maxY; ++y) {
                int color = this.imageData[x + y * IMAGE_WIDTH];
                if ((color >> 24 & 255) < 128) {
                    return true;
                }
            }
        }
        return false;
    }
}
