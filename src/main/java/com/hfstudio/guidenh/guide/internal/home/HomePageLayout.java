package com.hfstudio.guidenh.guide.internal.home;

public class HomePageLayout {

    private static final float SHELL_WIDTH_RATIO = 0.8f;
    private static final float SHELL_HEIGHT_RATIO = 0.8f;
    private static final float LOGO_WIDTH_RATIO = 0.8f;
    private static final int PANEL_GAP = 12;
    private static final int LOGO_PANEL_GAP = 6;
    private static final int PANEL_MIN_WIDTH = 100;
    private static final int MIN_SHELL_WIDTH = 220;
    private static final int MIN_SHELL_HEIGHT = 120;
    private static final int MIN_PANEL_HEIGHT = 52;
    private static final int MIN_LOGO_WIDTH = 80;
    private static final int MIN_LOGO_HEIGHT = 24;
    private static final int MAX_LOGO_HEIGHT = 72;

    private HomePageLayout() {}

    public static LayoutRects compute(int contentX, int contentY, int contentW, int contentH, int logoWidth,
        int logoHeight) {
        int shellW = Math.max(MIN_SHELL_WIDTH, Math.round(contentW * SHELL_WIDTH_RATIO));
        int shellMaxH = Math.max(1, contentH);
        int shellMinH = Math.min(MIN_SHELL_HEIGHT, shellMaxH);
        int shellH = Math.clamp(Math.round(contentH * SHELL_HEIGHT_RATIO), shellMinH, shellMaxH);
        int shellX = contentX + Math.max(0, (contentW - shellW) / 2);
        int shellY = contentY + Math.max(0, (contentH - shellH) / 2);

        int panelW = Math.max(PANEL_MIN_WIDTH, (shellW - PANEL_GAP) / 2);
        int columnsW = panelW * 2 + PANEL_GAP;
        int columnsX = shellX + Math.max(0, (shellW - columnsW) / 2);

        int safeLogoWidth = Math.max(1, logoWidth);
        int safeLogoHeight = Math.max(1, logoHeight);
        int preferredLogoW = Math.max(MIN_LOGO_WIDTH, Math.round(panelW * LOGO_WIDTH_RATIO));
        int preferredLogoH = Math.round((float) preferredLogoW * safeLogoHeight / safeLogoWidth);
        int logoH = Math.clamp(preferredLogoH, MIN_LOGO_HEIGHT, Math.min(MAX_LOGO_HEIGHT, Math.max(1, shellH / 3)));
        int logoW = Math.max(1, Math.round((float) logoH * safeLogoWidth / safeLogoHeight));
        if (logoW > panelW) {
            logoW = panelW;
            logoH = Math.max(MIN_LOGO_HEIGHT, Math.round((float) logoW * safeLogoHeight / safeLogoWidth));
        }
        int recommendY = shellY + logoH + LOGO_PANEL_GAP;
        int availableRecommendH = Math.max(MIN_PANEL_HEIGHT * 2 + PANEL_GAP, shellY + shellH - recommendY);
        int rightHalfH = Math.max(MIN_PANEL_HEIGHT, (availableRecommendH - PANEL_GAP) / 2);
        int recommendH = rightHalfH * 2 + PANEL_GAP;

        Rect recommended = new Rect(columnsX, recommendY, panelW, recommendH);
        Rect logo = new Rect(columnsX + Math.max(0, (panelW - logoW) / 2), shellY, logoW, logoH);
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
