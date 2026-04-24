package com.hfstudio.guidenh.guide.color;

public final class ARGB {

    private ARGB() {}

    public static int alpha(int argb) {
        return (argb >> 24) & 0xFF;
    }

    public static int red(int argb) {
        return (argb >> 16) & 0xFF;
    }

    public static int green(int argb) {
        return (argb >> 8) & 0xFF;
    }

    public static int blue(int argb) {
        return argb & 0xFF;
    }

    public static int color(int alpha, int red, int green, int blue) {
        return (alpha & 0xFF) << 24 | (red & 0xFF) << 16 | (green & 0xFF) << 8 | (blue & 0xFF);
    }

    public static int color(int red, int green, int blue) {
        return color(0xFF, red, green, blue);
    }

    public static int multiply(int color1, int color2) {
        return color(
            alpha(color1) * alpha(color2) / 255,
            red(color1) * red(color2) / 255,
            green(color1) * green(color2) / 255,
            blue(color1) * blue(color2) / 255);
    }

    public static int opaque(int color) {
        return color | 0xFF000000;
    }

    public static int white(int alpha) {
        return color(alpha, 0xFF, 0xFF, 0xFF);
    }

    public static boolean hasTransparency(int color) {
        return alpha(color) < 0xFF;
    }

    public static int lerp(float t, int from, int to) {
        int fromA = alpha(from), fromR = red(from), fromG = green(from), fromB = blue(from);
        int toA = alpha(to), toR = red(to), toG = green(to), toB = blue(to);
        return color(
            (int) (fromA + t * (toA - fromA)),
            (int) (fromR + t * (toR - fromR)),
            (int) (fromG + t * (toG - fromG)),
            (int) (fromB + t * (toB - fromB)));
    }
}
