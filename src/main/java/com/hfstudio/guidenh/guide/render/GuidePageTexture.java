package com.hfstudio.guidenh.guide.render;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.document.LytSize;

import cpw.mods.fml.relauncher.ReflectionHelper;

public class GuidePageTexture {

    public static final Logger LOG = LogManager.getLogger("GuideNH/GuidePageTexture");
    public static final GuidePageTexture MISSING = new GuidePageTexture(null, 0, 0, null);
    private static final String TEXTURE_OBJECTS_FIELD = "mapTextureObjects";
    private static final String TEXTURE_OBJECTS_SRG_FIELD = "field_110585_a";

    public static final Map<ResourceLocation, GuidePageTexture> CACHE = new HashMap<>();

    @Nullable
    private final ResourceLocation sourceId;
    private final int width;
    private final int height;
    private byte @Nullable [] imageData;
    @Nullable
    private ResourceLocation texture;

    public GuidePageTexture(ResourceLocation texture, int width, int height) {
        this(texture, width, height, null);
    }

    private GuidePageTexture(@Nullable ResourceLocation sourceId, int width, int height, byte @Nullable [] imageData) {
        this.sourceId = sourceId;
        this.width = width;
        this.height = height;
        this.imageData = imageData;
        this.texture = imageData == null ? sourceId : null;
    }

    public static GuidePageTexture missing() {
        return MISSING;
    }

    public static GuidePageTexture of(ResourceLocation texture) {
        return new GuidePageTexture(texture, 256, 256);
    }

    public static synchronized GuidePageTexture load(ResourceLocation id, byte[] imageData) {
        var cached = CACHE.get(id);
        if (cached != null) return cached;
        if (imageData == null || imageData.length == 0) return missing();
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageData));
            if (img == null) {
                LOG.warn("Failed to decode image {} (ImageIO returned null)", id);
                return missing();
            }
            var gpt = new GuidePageTexture(id, img.getWidth(), img.getHeight(), imageData);
            CACHE.put(id, gpt);
            return gpt;
        } catch (Throwable t) {
            LOG.error("Failed to load guide page texture {}", id, t);
            return missing();
        }
    }

    public static synchronized void clear() {
        TextureManager textureManager = Minecraft.getMinecraft()
            .getTextureManager();
        for (GuidePageTexture pageTexture : CACHE.values()) {
            pageTexture.releaseTexture(textureManager);
        }
        CACHE.clear();
    }

    public static String sanitize(String raw) {
        StringBuilder sanitized = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            sanitized.append(isSafeTextureNameCharacter(ch) ? ch : '_');
        }
        return sanitized.toString();
    }

    private static boolean isSafeTextureNameCharacter(char ch) {
        return ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' || ch >= '0' && ch <= '9';
    }

    @Nullable
    public ResourceLocation getTexture() {
        if (texture != null || isMissing()) {
            return texture;
        }

        byte[] data = imageData;
        if (data == null) {
            return null;
        }

        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
            if (img == null) {
                LOG.warn("Failed to decode image {} while creating dynamic texture (ImageIO returned null)", sourceId);
                imageData = null;
                return null;
            }
            DynamicTexture tex = new DynamicTexture(img);
            String name = "guidenh_page_" + sanitize(sourceId.getResourceDomain() + "_" + sourceId.getResourcePath());
            texture = Minecraft.getMinecraft()
                .getTextureManager()
                .getDynamicTextureLocation(name, tex);
            imageData = null;
            return texture;
        } catch (Throwable t) {
            LOG.error("Failed to create guide page dynamic texture {}", sourceId, t);
            imageData = null;
            return null;
        }
    }

    public boolean isMissing() {
        return imageData == null && texture == null;
    }

    private void releaseTexture(TextureManager textureManager) {
        if (texture == null || texture == sourceId) {
            return;
        }
        ITextureObject textureObject = removeTextureObject(textureManager, texture);
        if (textureObject != null) {
            TextureUtil.deleteTexture(textureObject.getGlTextureId());
        }
        texture = null;
        imageData = null;
    }

    @Nullable
    private static ITextureObject removeTextureObject(TextureManager textureManager, ResourceLocation location) {
        try {
            Map<ResourceLocation, ITextureObject> textureObjects = ReflectionHelper.getPrivateValue(
                TextureManager.class,
                textureManager,
                TEXTURE_OBJECTS_FIELD,
                TEXTURE_OBJECTS_SRG_FIELD);
            return textureObjects.remove(location);
        } catch (Throwable t) {
            LOG.warn("Failed to remove dynamic guide page texture {} from Minecraft texture manager", location, t);
            return null;
        }
    }

    public LytSize getSize() {
        if (width <= 0 || height <= 0) return new LytSize(256, 256);
        return new LytSize(width, height);
    }

    @Nullable
    public ResourceLocation getSourceId() {
        return sourceId;
    }
}
