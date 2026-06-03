package com.hfstudio.structurelibexport;

public class StructureLibExportImageLimits {

    public static final long DEFAULT_MAX_PIXELS = 200L * 200L * 128L * 128L;
    public static final int MAX_TILE_PIXELS = 4 * 1024 * 1024;

    public void validateSize(int width, int height, long maxPixels) {
        long pixels = pixelCount(width, height);
        if (width <= 0 || height <= 0 || pixels <= 0L) {
            throw new IllegalArgumentException("StructureLib export image size is invalid: " + width + "x" + height);
        }
        if (maxPixels != -1L && pixels > maxPixels) {
            throw new IllegalArgumentException(
                "StructureLib export image would be " + width
                    + "x"
                    + height
                    + " ("
                    + pixels
                    + " pixels), exceeding the configured limit of "
                    + maxPixels
                    + " pixels. Reduce --pixelsPerBlock or --scale, crop with --layers, raise --maxPixels, or use --maxPixels -1.");
        }
    }

    public int resolveMaxTileEdge(int maxFboSize) {
        int edge = (int) Math.floor(Math.sqrt(MAX_TILE_PIXELS));
        return Math.clamp(edge, 1, maxFboSize);
    }

    private static long pixelCount(int width, int height) {
        return (long) width * (long) height;
    }
}
