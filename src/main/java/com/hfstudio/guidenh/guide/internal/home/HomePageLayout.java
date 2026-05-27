package com.hfstudio.guidenh.guide.internal.home;

public class HomePageLayout {

    private static final float SHELL_WIDTH_RATIO = 0.8f;
    private static final float SHELL_HEIGHT_RATIO = 0.8f;
    private static final float LOGO_WIDTH_RATIO = 0.8f;
    private static final int PANEL_GAP = 12;
    private static final int LOGO_RAISE_PIXELS = 5;
    private static final int PANEL_MIN_WIDTH = 100;

    private HomePageLayout() {}

    public static LayoutRects compute(int contentX, int contentY, int contentW, int contentH, int logoWidth,
        int logoHeight) {
        int shellW = Math.max(220, Math.round(contentW * SHELL_WIDTH_RATIO));
        int shellH = Math.max(180, Math.round(contentH * SHELL_HEIGHT_RATIO));
        int shellX = contentX + Math.max(0, (contentW - shellW) / 2);
        int shellY = contentY + Math.max(0, (contentH - shellH) / 2);

        int panelW = Math.max(PANEL_MIN_WIDTH, (shellW - PANEL_GAP) / 2);
        int columnsW = panelW * 2 + PANEL_GAP;
        int columnsX = shellX + Math.max(0, (shellW - columnsW) / 2);

        int logoW = Math.max(80, Math.round(panelW * LOGO_WIDTH_RATIO));
        int safeLogoWidth = Math.max(1, logoWidth);
        int safeLogoHeight = Math.max(1, logoHeight);
        int logoH = Math.max(30, Math.round((float) logoW * safeLogoHeight / safeLogoWidth));
        int recommendY = shellY + logoH / 2;
        int availableRecommendH = Math.max(80, shellH - logoH / 2);
        int rightHalfH = Math.max(60, (availableRecommendH - PANEL_GAP) / 2);
        int recommendH = rightHalfH * 2 + PANEL_GAP;

        Rect recommended = new Rect(columnsX, recommendY, panelW, recommendH);
        Rect logo = new Rect(columnsX, recommendY - logoH - LOGO_RAISE_PIXELS, logoW, logoH);
        Rect bookmarks = new Rect(columnsX + panelW + PANEL_GAP, recommendY, panelW, rightHalfH);
        Rect history = new Rect(columnsX + panelW + PANEL_GAP, recommendY + rightHalfH + PANEL_GAP, panelW, rightHalfH);
        int recommendedTitleSafeTop = 0;
        return new LayoutRects(logo, recommended, bookmarks, history, recommendedTitleSafeTop);
    }

    public static class LayoutRects {

        private final Rect logo;
        private final Rect recommended;
        private final Rect bookmarks;
        private final Rect history;
        private final int recommendedTitleSafeTop;

        public LayoutRects(Rect logo, Rect recommended, Rect bookmarks, Rect history, int recommendedTitleSafeTop) {
            this.logo = logo;
            this.recommended = recommended;
            this.bookmarks = bookmarks;
            this.history = history;
            this.recommendedTitleSafeTop = recommendedTitleSafeTop;
        }

        public Rect logo() {
            return logo;
        }

        public Rect recommended() {
            return recommended;
        }

        public Rect bookmarks() {
            return bookmarks;
        }

        public Rect history() {
            return history;
        }

        public int recommendedTitleSafeTop() {
            return recommendedTitleSafeTop;
        }
    }

    public static class Rect {

        private final int x;
        private final int y;
        private final int width;
        private final int height;

        public Rect(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public int x() {
            return x;
        }

        public int y() {
            return y;
        }

        public int width() {
            return width;
        }

        public int height() {
            return height;
        }
    }
}
