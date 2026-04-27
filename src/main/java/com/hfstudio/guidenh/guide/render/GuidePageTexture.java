package com.hfstudio.guidenh.guide.render;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.hfstudio.guidenh.guide.document.LytSize;

public class GuidePageTexture {

    private static final Logger LOG = LogManager.getLogger("GuideNH/GuidePageTexture");
    private static final GuidePageTexture MISSING = new GuidePageTexture(null, 0, 0, null);

    private static final Map<ResourceLocation, GuidePageTexture> CACHE = new HashMap<>();

    @Nullable
    private final ResourceLocation sourceId;
    private final int width;
    private final int height;
    @Nullable
    private final byte[] imageData;
    @Nullable
    private ResourceLocation texture;

    public GuidePageTexture(ResourceLocation texture, int width, int height) {
        this(texture, width, height, null);
    }

    private GuidePageTexture(@Nullable ResourceLocation sourceId, int width, int height, @Nullable byte[] imageData) {
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
        CACHE.clear();
    }

    private static String sanitize(String raw) {
        return raw.replaceAll("[^a-zA-Z0-9]", "_");
    }

    @Nullable
    public ResourceLocation getTexture() {
        if (texture != null || isMissing()) {
            return texture;
        }

        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageData));
            if (img == null) {
                LOG.warn("Failed to decode image {} while creating dynamic texture (ImageIO returned null)", sourceId);
                return null;
            }
            DynamicTexture tex = new DynamicTexture(img);
            String name = "guidenh_page_" + sanitize(sourceId.getResourceDomain() + "_" + sourceId.getResourcePath());
            texture = Minecraft.getMinecraft()
                .getTextureManager()
                .getDynamicTextureLocation(name, tex);
            return texture;
        } catch (Throwable t) {
            LOG.error("Failed to create guide page dynamic texture {}", sourceId, t);
            return null;
        }
    }

    public boolean isMissing() {
        return imageData == null && texture == null;
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
