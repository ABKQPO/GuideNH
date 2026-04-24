package com.hfstudio.guidenh.guide.ui;

import com.hfstudio.guidenh.guide.PageAnchor;

public interface GuideUiHost {

    void navigateTo(PageAnchor anchor);

    void close();
}
