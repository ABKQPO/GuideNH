package com.hfstudio.guidenh.guide.internal.structure;

public class GuideStructureCoordinateParser {

    private GuideStructureCoordinateParser() {}

    public static int parsePosition(int base, String token) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Missing coordinate");
        }
        if (!token.startsWith("~")) {
            return Integer.parseInt(token);
        }
        if (token.length() == 1) {
            return base;
        }
        return base + Integer.parseInt(token.substring(1));
    }

    public static int parseSize(String token) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Missing size");
        }
        if (!token.startsWith("~")) {
            return Integer.parseInt(token);
        }
        if (token.length() == 1) {
            return 0;
        }
        return Integer.parseInt(token.substring(1));
    }
}
