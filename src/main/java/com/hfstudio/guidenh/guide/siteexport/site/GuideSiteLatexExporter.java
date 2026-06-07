package com.hfstudio.guidenh.guide.siteexport.site;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.scilab.forge.jlatexmath.ParseException;
import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;

import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

public class GuideSiteLatexExporter {

    private static final String CALIBRATION_FORMULA = "x";

    private final GuideSiteAssetRegistry assets;
    private final Map<String, ExportedLatex> exports = new HashMap<>();
    private final Map<String, Integer> referenceHeights = new HashMap<>();

    public GuideSiteLatexExporter(GuideSiteAssetRegistry assets) {
        this.assets = assets;
    }

    public ExportedLatex export(String formula, int fillColorArgb, float sourceScale) {
        if (formula == null || formula.trim()
            .isEmpty()) {
            return null;
        }
        float safeSourceScale = Math.max(16f, sourceScale);
        String key = fillColorArgb + ":" + safeSourceScale + ":" + formula;
        ExportedLatex cached = exports.get(key);
        if (cached != null) {
            return cached;
        }

        try {
            TeXIcon icon = createIcon(formula, fillColorArgb, safeSourceScale);
            byte[] png = renderPng(icon);
            String src = GuideSitePageAssetExporter.ROOT_PREFIX + assets.writeShared("latex", ".png", png);
            ExportedLatex exported = new ExportedLatex(
                src,
                icon.getIconWidth(),
                icon.getIconHeight(),
                Math.max(0, (int) Math.ceil(icon.getTrueIconDepth())),
                referenceHeight(safeSourceScale));
            exports.put(key, exported);
            return exported;
        } catch (ParseException e) {
            GuideDebugLog.error(
                "[GuideNH] [GuideSiteLatexExporter] Failed to parse LaTeX formula '{}': {}",
                formula,
                e.getMessage());
            return null;
        } catch (Exception e) {
            GuideDebugLog.warnAlways(
                "[GuideNH] [GuideSiteLatexExporter] Failed to export LaTeX formula '{}': {}",
                formula,
                e.getMessage(),
                e);
            return null;
        }
    }

    private int referenceHeight(float sourceScale) throws ParseException {
        String key = String.format("%.2f", sourceScale);
        Integer cached = referenceHeights.get(key);
        if (cached != null) {
            return cached;
        }
        TeXIcon icon = createIcon(CALIBRATION_FORMULA, 0xFFFFFFFF, sourceScale);
        int height = Math.max(1, icon.getIconHeight());
        referenceHeights.put(key, height);
        return height;
    }

    private TeXIcon createIcon(String formula, int fillColorArgb, float sourceScale) throws ParseException {
        TeXFormula texFormula = new TeXFormula(formula);
        TeXIcon icon = texFormula.new TeXIconBuilder().setStyle(TeXConstants.STYLE_DISPLAY)
            .setSize(sourceScale)
            .setFGColor(new Color(fillColorArgb, true))
            .build();
        icon.setInsets(new Insets(2, 2, 2, 2));
        icon.setForeground(new Color(fillColorArgb, true));
        return icon;
    }

    private byte[] renderPng(TeXIcon icon) throws Exception {
        BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(
                RenderingHints.KEY_ALPHA_INTERPOLATION,
                RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            graphics.setColor(new Color(0, 0, 0, 0));
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            icon.paintIcon(null, graphics, 0, 0);
        } finally {
            graphics.dispose();
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    public static class ExportedLatex {

        private final String src;
        private final int widthPx;
        private final int heightPx;
        private final int depthPx;
        private final int referenceHeightPx;

        public ExportedLatex(String src, int widthPx, int heightPx, int depthPx, int referenceHeightPx) {
            this.src = src;
            this.widthPx = widthPx;
            this.heightPx = heightPx;
            this.depthPx = depthPx;
            this.referenceHeightPx = referenceHeightPx;
        }

        public String src() {
            return src;
        }

        public int widthPx() {
            return widthPx;
        }

        public int heightPx() {
            return heightPx;
        }

        public int depthPx() {
            return depthPx;
        }

        public int referenceHeightPx() {
            return referenceHeightPx;
        }
    }
}
