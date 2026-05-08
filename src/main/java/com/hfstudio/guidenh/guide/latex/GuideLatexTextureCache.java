package com.hfstudio.guidenh.guide.latex;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.lwjgl.opengl.GL11;

public final class GuideLatexTextureCache {

    public static final GuideLatexTextureCache INSTANCE = new GuideLatexTextureCache();

    private static final int MAX_TEXTURE_ENTRIES = 128;

    /** Maps cacheKey -> [textureId, widthPx, heightPx]. Evicts LRU entries when full. */
    private final Map<String, int[]> textureCache = new LinkedHashMap<>(MAX_TEXTURE_ENTRIES + 1, 0.75f, true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, int[]> eldest) {
            if (size() > MAX_TEXTURE_ENTRIES) {
                GL11.glDeleteTextures(eldest.getValue()[0]);
                return true;
            }
            return false;
        }
    };

    /** Maps formula string -> error message from parse/render failure. */
    private final Map<String, String> failureCache = new HashMap<>();

    /** Maps (formula + sourceScale string) -> [widthPx, heightPx], no GL needed. Layout thread safe. */
    private final ConcurrentHashMap<String, int[]> sizeCache = new ConcurrentHashMap<>();

    private GuideLatexTextureCache() {}

    /**
     * Returns the cached texture entry for the given cache key, or {@code null} if not found.
     * The returned array is [textureId, widthPx, heightPx].
     */
    public int[] getTexture(String cacheKey) {
        return textureCache.get(cacheKey);
    }

    /**
     * Stores a rendered texture entry in the cache.
     *
     * @param cacheKey  key produced by {@link #buildTextureCacheKey}
     * @param textureId OpenGL texture ID
     * @param widthPx   texture width in pixels
     * @param heightPx  texture height in pixels
     */
    public void putTexture(String cacheKey, int textureId, int widthPx, int heightPx) {
        textureCache.put(cacheKey, new int[] { textureId, widthPx, heightPx });
    }

    /**
     * Returns the cached pixel dimensions for the given size-key, or {@code null} if not available yet.
     * The returned array is [widthPx, heightPx]. Safe to call from any thread.
     */
    public int[] getSize(String sizeKey) {
        return sizeCache.get(sizeKey);
    }

    /**
     * Stores measured pixel dimensions (no GL resource). Safe to call from any thread.
     *
     * @param sizeKey  key produced by {@link #buildSizeCacheKey}
     * @param widthPx  icon width from jlatexmath
     * @param heightPx icon height from jlatexmath
     */
    public void putSize(String sizeKey, int widthPx, int heightPx) {
        sizeCache.put(sizeKey, new int[] { widthPx, heightPx });
    }

    /**
     * Returns {@code true} if a previous attempt to render {@code formula} has failed.
     */
    public boolean hasFailed(String formula) {
        return failureCache.containsKey(formula);
    }

    /**
     * Records a render failure so that repeated attempts are suppressed.
     *
     * @param formula  the LaTeX source string
     * @param errorMsg the parse or render error message
     */
    public void markFailed(String formula, String errorMsg) {
        failureCache.put(formula, errorMsg == null ? "" : errorMsg);
    }

    /**
     * Returns the recorded error message for {@code formula}, or {@code null} if no failure was recorded.
     */
    public String getFailureError(String formula) {
        return failureCache.get(formula);
    }

    /**
     * Deletes all OpenGL textures and clears every internal map. Must be called on the render thread.
     */
    public void clearAll() {
        for (int[] entry : textureCache.values()) {
            GL11.glDeleteTextures(entry[0]);
        }
        textureCache.clear();
        failureCache.clear();
        sizeCache.clear();
    }

    /**
     * Builds the cache key used for both texture and size lookups.
     *
     * @param formula       LaTeX source string
     * @param fillColorArgb ARGB fill colour
     * @param sourceScale   jlatexmath render scale (e.g. 100.0f)
     * @return cache key string
     */
    public static String buildTextureCacheKey(String formula, int fillColorArgb, float sourceScale) {
        return String.format("%08x:%.2f:%s", fillColorArgb, sourceScale, formula);
    }

    /**
     * Builds the cache key used for size-only lookups (no colour dependency).
     *
     * @param formula     LaTeX source string
     * @param sourceScale jlatexmath render scale
     * @return size cache key string
     */
    public static String buildSizeCacheKey(String formula, float sourceScale) {
        return String.format("%.2f:%s", sourceScale, formula);
    }
}
