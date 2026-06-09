package com.hfstudio.guidenh.guide.render.emoji;

import java.util.ArrayList;
import java.util.List;

public class GuideEmojiParser {

    public static final int VARIATION_SELECTOR_TEXT = 0xFE0E;
    public static final int VARIATION_SELECTOR_EMOJI = 0xFE0F;
    public static final int ZERO_WIDTH_JOINER = 0x200D;
    public static final int COMBINING_ENCLOSING_KEYCAP = 0x20E3;
    public static final int REGIONAL_INDICATOR_START = 0x1F1E6;
    public static final int REGIONAL_INDICATOR_END = 0x1F1FF;

    public List<GuideEmojiRun> findEmojiRuns(String text) {
        var runs = new ArrayList<GuideEmojiRun>();
        if (text == null || text.isEmpty()) {
            return runs;
        }

        int index = 0;
        while (index < text.length()) {
            int next = findEmojiEnd(text, index);
            if (next > index) {
                runs.add(new GuideEmojiRun(index, next, text.substring(index, next)));
                index = next;
                continue;
            }
            int codePoint = text.codePointAt(index);
            index += Character.charCount(codePoint);
        }
        return runs;
    }

    public boolean containsEmoji(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (int index = 0; index < text.length();) {
            int next = findEmojiEnd(text, index);
            if (next > index) {
                return true;
            }
            int codePoint = text.codePointAt(index);
            index += Character.charCount(codePoint);
        }
        return false;
    }

    public int findEmojiEnd(String text, int start) {
        if (text == null || start < 0 || start >= text.length()) {
            return -1;
        }

        int index = start;
        int firstCodePoint = text.codePointAt(index);
        if (isKeycapBase(firstCodePoint)) {
            int keycapEnd = consumeKeycapSequence(text, index + Character.charCount(firstCodePoint));
            return keycapEnd > index ? keycapEnd : -1;
        }
        if (!isEmojiBase(firstCodePoint)) {
            return -1;
        }

        index += Character.charCount(firstCodePoint);
        index = consumeEmojiModifiers(text, index);

        if (isRegionalIndicator(firstCodePoint)) {
            return consumeRegionalIndicatorPair(text, index);
        }

        while (index < text.length()) {
            int codePoint = text.codePointAt(index);
            if (codePoint != ZERO_WIDTH_JOINER) {
                break;
            }
            int joinerEnd = index + Character.charCount(codePoint);
            if (joinerEnd >= text.length()) {
                break;
            }
            int joinedCodePoint = text.codePointAt(joinerEnd);
            if (!isEmojiBase(joinedCodePoint)) {
                break;
            }
            index = joinerEnd + Character.charCount(joinedCodePoint);
            index = consumeEmojiModifiers(text, index);
        }
        return index;
    }

    private static int consumeEmojiModifiers(String text, int index) {
        boolean consumed;
        do {
            consumed = false;
            if (index >= text.length()) {
                return index;
            }
            int codePoint = text.codePointAt(index);
            if (codePoint == VARIATION_SELECTOR_EMOJI || codePoint == VARIATION_SELECTOR_TEXT
                || isSkinToneModifier(codePoint)
                || codePoint == COMBINING_ENCLOSING_KEYCAP) {
                index += Character.charCount(codePoint);
                consumed = true;
            }
        } while (consumed);
        return index;
    }

    private static int consumeKeycapSequence(String text, int index) {
        if (index < text.length()) {
            int codePoint = text.codePointAt(index);
            if (codePoint == VARIATION_SELECTOR_EMOJI || codePoint == VARIATION_SELECTOR_TEXT) {
                index += Character.charCount(codePoint);
            }
        }
        if (index < text.length() && text.codePointAt(index) == COMBINING_ENCLOSING_KEYCAP) {
            return index + Character.charCount(COMBINING_ENCLOSING_KEYCAP);
        }
        return -1;
    }

    private static int consumeRegionalIndicatorPair(String text, int index) {
        if (index >= text.length()) {
            return index;
        }
        int codePoint = text.codePointAt(index);
        if (isRegionalIndicator(codePoint)) {
            return index + Character.charCount(codePoint);
        }
        return index;
    }

    public static boolean isEmojiBase(int codePoint) {
        return switch (codePoint) {
            case 0x00A9, 0x00AE, 0x203C, 0x2049, 0x2122, 0x2139, 0x2194, 0x2195, 0x2196, 0x2197, 0x2198, 0x2199, 0x21A9, 0x21AA, 0x231A, 0x231B, 0x2328, 0x23CF, 0x23E9, 0x23EA, 0x23EB, 0x23EC, 0x23ED, 0x23EE, 0x23EF, 0x23F0, 0x23F1, 0x23F2, 0x23F3, 0x23F8, 0x23F9, 0x23FA, 0x24C2, 0x25AA, 0x25AB, 0x25B6, 0x25C0, 0x25FB, 0x25FC, 0x25FD, 0x25FE, 0x2600, 0x2601, 0x2602, 0x2603, 0x2604, 0x260E, 0x2611, 0x2614, 0x2615, 0x2618, 0x261D, 0x2620, 0x2622, 0x2623, 0x2626, 0x262A, 0x262E, 0x262F, 0x2638, 0x2639, 0x263A, 0x2640, 0x2642, 0x2648, 0x2649, 0x264A, 0x264B, 0x264C, 0x264D, 0x264E, 0x264F, 0x2650, 0x2651, 0x2652, 0x2653, 0x265F, 0x2660, 0x2663, 0x2665, 0x2666, 0x2668, 0x267B, 0x267E, 0x267F, 0x2692, 0x2693, 0x2694, 0x2695, 0x2696, 0x2697, 0x2699, 0x269B, 0x269C, 0x26A0, 0x26A1, 0x26A7, 0x26AA, 0x26AB, 0x26B0, 0x26B1, 0x26BD, 0x26BE, 0x26C4, 0x26C5, 0x26C8, 0x26CE, 0x26CF, 0x26D1, 0x26D3, 0x26D4, 0x26E9, 0x26EA, 0x26F0, 0x26F1, 0x26F2, 0x26F3, 0x26F4, 0x26F5, 0x26F7, 0x26F8, 0x26F9, 0x26FA, 0x26FD, 0x2702, 0x2705, 0x2708, 0x2709, 0x270A, 0x270B, 0x270C, 0x270D, 0x270F, 0x2712, 0x2714, 0x2716, 0x271D, 0x2721, 0x2728, 0x2733, 0x2734, 0x2744, 0x2747, 0x274C, 0x274E, 0x2753, 0x2754, 0x2755, 0x2757, 0x2763, 0x2764, 0x2795, 0x2796, 0x2797, 0x27A1, 0x27B0, 0x27BF, 0x2934, 0x2935, 0x2B05, 0x2B06, 0x2B07, 0x2B1B, 0x2B1C, 0x2B50, 0x2B55, 0x3030, 0x303D, 0x3297, 0x3299 -> true;
            default -> isRegionalIndicator(codePoint) || isInRange(codePoint, 0x1F000, 0x1FAFF)
                || isInRange(codePoint, 0x1FC00, 0x1FFFD);
        };
    }

    public static boolean isKeycapBase(int codePoint) {
        return codePoint == '#' || codePoint == '*' || isInRange(codePoint, '0', '9');
    }

    public static boolean isRegionalIndicator(int codePoint) {
        return isInRange(codePoint, REGIONAL_INDICATOR_START, REGIONAL_INDICATOR_END);
    }

    public static boolean isSkinToneModifier(int codePoint) {
        return isInRange(codePoint, 0x1F3FB, 0x1F3FF);
    }

    private static boolean isInRange(int codePoint, int start, int end) {
        return codePoint >= start && codePoint <= end;
    }
}
