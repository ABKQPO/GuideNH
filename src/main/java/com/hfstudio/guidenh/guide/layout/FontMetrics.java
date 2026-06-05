package com.hfstudio.guidenh.guide.layout;

import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

public interface FontMetrics {

    float getAdvance(int codePoint, ResolvedTextStyle style);

    int getLineHeight(ResolvedTextStyle style);

    default int getStringWidth(String text, ResolvedTextStyle style) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        float width = 0f;
        for (int index = 0; index < text.length();) {
            int codePoint = text.codePointAt(index);
            width += getAdvance(codePoint, style);
            index += Character.charCount(codePoint);
        }
        return Math.round(width);
    }
}
