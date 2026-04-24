package com.hfstudio.guidenh.guide.layout;

import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

public interface FontMetrics {

    float getAdvance(int codePoint, ResolvedTextStyle style);

    int getLineHeight(ResolvedTextStyle style);
}
