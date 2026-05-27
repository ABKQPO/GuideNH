package com.hfstudio.guidenh.integration.betterquesting;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

public class QuestIdParser {

    private static final int UUID_BYTE_LENGTH = 16;

    private QuestIdParser() {}

    @Nullable
    public static UUID parse(@Nullable String rawQuestId) {
        if (rawQuestId == null) {
            return null;
        }

        String trimmed = rawQuestId.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        UUID compactId = parseCompactBase64(trimmed);
        if (compactId != null) {
            return compactId;
        }

        try {
            return UUID.fromString(trimmed);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @Nullable
    private static UUID parseCompactBase64(String rawQuestId) {
        String normalized = normalizePadding(rawQuestId);
        if (normalized == null) {
            return null;
        }

        byte[] bytes;
        try {
            bytes = Base64.getUrlDecoder()
                .decode(normalized);
        } catch (IllegalArgumentException ignored) {
            return null;
        }

        if (bytes.length != UUID_BYTE_LENGTH) {
            return null;
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return new UUID(buffer.getLong(), buffer.getLong());
    }

    @Nullable
    private static String normalizePadding(String rawQuestId) {
        int remainder = rawQuestId.length() % 4;
        if (remainder == 1) {
            return null;
        }
        if (remainder == 0) {
            return rawQuestId;
        }
        if (remainder == 2) {
            return rawQuestId + "==";
        }
        return rawQuestId + "=";
    }
}
