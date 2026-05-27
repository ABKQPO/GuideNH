package com.hfstudio.guidenh.guide.mediawiki;

import java.nio.charset.StandardCharsets;

public class MediaWikiTitleCodec {

    private MediaWikiTitleCodec() {}

    public static String decodeHex(String value) {
        if (value == null || value.isEmpty() || (value.length() & 1) != 0) {
            return "";
        }
        byte[] bytes = new byte[value.length() / 2];
        for (int index = 0; index < value.length(); index += 2) {
            int current = Integer.parseInt(value.substring(index, index + 2), 16);
            bytes[index / 2] = (byte) current;
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
