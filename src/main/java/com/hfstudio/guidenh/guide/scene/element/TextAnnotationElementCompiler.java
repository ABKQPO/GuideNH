package com.hfstudio.guidenh.guide.scene.element;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.joml.Vector3f;

import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.internal.localization.GuideResourceLanguageIndex;
import com.hfstudio.guidenh.guide.internal.util.GuideStringLines;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldBoxAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.TextAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.compiler.AnnotationTagCompiler;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class TextAnnotationElementCompiler implements SceneElementTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("TextAnnotation");
    }

    @Override
    public void compile(GuidebookLevel level, CameraSettings camera, PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el) {
        LytGuidebookScene scene = AnnotationTagCompiler.CURRENT_SCENE.get();
        if (scene == null) {
            errorSink.appendError(compiler, "TextAnnotation used outside <GameScene>", el);
            return;
        }

        String fallbackText = resolveFallbackText(compiler, errorSink, el);
        String text = resolveLocalizedText(compiler, errorSink, el, fallbackText);
        if (text == null || text.trim()
            .isEmpty()) {
            errorSink.appendError(compiler, "Missing text content. Provide text, textKey, or tag body content.", el);
            return;
        }

        var color = MdxAttrs.getColor(compiler, errorSink, el, "color", ConstantColor.WHITE);
        int maxWidth = MdxAttrs.getInt(compiler, errorSink, el, "maxWidth", 0);
        int backgroundAlpha = clampAlpha(
            MdxAttrs.getInt(compiler, errorSink, el, "backgroundAlpha", TextAnnotation.DEFAULT_BACKGROUND_ALPHA));
        boolean independent = MdxAttrs.getBoolean(compiler, errorSink, el, "independent", false);
        int yOffset = MdxAttrs.getInt(compiler, errorSink, el, "yOffset", 0);
        Vector3f pos = readPosition(compiler, errorSink, el);
        TextAnnotation annotation = independent ? new TextAnnotation(text, color, yOffset, maxWidth)
            : new TextAnnotation(pos, text, color, maxWidth);
        annotation.setBackgroundAlpha(backgroundAlpha);
        annotation.setConnector(
            parseConnectorSide(compiler, errorSink, el),
            MdxAttrs.getInt(compiler, errorSink, el, "connectorOffset", 0),
            MdxAttrs.getInt(compiler, errorSink, el, "connectorLength", TextAnnotation.CONNECTOR_HEIGHT));
        if (compiler != null) {
            var paragraph = new LytParagraph();
            compiler.compileInlineMarkdown(text, paragraph);
            annotation.setRichContent(paragraph);
        }
        scene.addAnnotation(annotation);

        String hlMinXRaw = el.getAttributeString("hlMinX", null);
        String hlMinYRaw = el.getAttributeString("hlMinY", null);
        String hlMinZRaw = el.getAttributeString("hlMinZ", null);
        String hlMaxXRaw = el.getAttributeString("hlMaxX", null);
        String hlMaxYRaw = el.getAttributeString("hlMaxY", null);
        String hlMaxZRaw = el.getAttributeString("hlMaxZ", null);
        if (hlMinXRaw == null && hlMinYRaw == null
            && hlMinZRaw == null
            && hlMaxXRaw == null
            && hlMaxYRaw == null
            && hlMaxZRaw == null) {
            return;
        }

        float hlMinX = parseFloat(compiler, errorSink, el, "hlMinX", 0f);
        float hlMinY = parseFloat(compiler, errorSink, el, "hlMinY", 0f);
        float hlMinZ = parseFloat(compiler, errorSink, el, "hlMinZ", 0f);
        float hlMaxX = parseFloat(compiler, errorSink, el, "hlMaxX", 1f);
        float hlMaxY = parseFloat(compiler, errorSink, el, "hlMaxY", 1f);
        float hlMaxZ = parseFloat(compiler, errorSink, el, "hlMaxZ", 1f);
        int hlArgb = parseColor(compiler, errorSink, el, "highlightColor", 0x8000FFAA);
        var box = new InWorldBoxAnnotation(
            new Vector3f(hlMinX, hlMinY, hlMinZ),
            new Vector3f(hlMaxX, hlMaxY, hlMaxZ),
            new ConstantColor(hlArgb),
            InWorldBoxAnnotation.DEFAULT_THICKNESS);
        box.setAlwaysOnTop(false);
        scene.addAnnotation(box);
    }

    private static float parseFloat(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el, String name,
        float defaultValue) {
        String raw = el.getAttributeString(name, null);
        if (raw == null || raw.trim()
            .isEmpty()) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(raw.trim());
        } catch (NumberFormatException e) {
            errorSink.appendError(compiler, "Malformed float value: '" + raw + "'", el);
            return defaultValue;
        }
    }

    private static int parseColor(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el, String name,
        int defaultValue) {
        String raw = el.getAttributeString(name, null);
        if (raw == null || raw.trim()
            .isEmpty()) {
            return defaultValue;
        }
        try {
            return (int) Long.parseLong(
                raw.trim()
                    .replace("0x", "")
                    .replace("0X", ""),
                16);
        } catch (NumberFormatException e) {
            errorSink.appendError(compiler, "Malformed color value: '" + raw + "'", el);
            return defaultValue;
        }
    }

    private static Vector3f readPosition(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el) {
        String posRaw = el.getAttributeString("pos", null);
        if (posRaw != null && !posRaw.trim()
            .isEmpty()) {
            return MdxAttrs.getVector3(compiler, errorSink, el, "pos", new Vector3f());
        }

        float x = MdxAttrs.getFloat(compiler, errorSink, el, "x", 0f);
        float y = MdxAttrs.getFloat(compiler, errorSink, el, "y", 0f);
        float z = MdxAttrs.getFloat(compiler, errorSink, el, "z", 0f);
        return new Vector3f(x, y, z);
    }

    private static String resolveFallbackText(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el) {
        String text = MdxAttrs.getString(compiler, errorSink, el, "text", null);
        if (text != null) {
            return text;
        }
        text = compiler.getBlockTagChildrenSource(el);
        if (text != null) {
            return trimCommonIndent(text);
        }
        return null;
    }

    private static String resolveLocalizedText(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el,
        String fallbackText) {
        String textKey = MdxAttrs.getString(compiler, errorSink, el, "textKey", null);
        if (textKey == null || compiler == null) {
            return fallbackText;
        }
        String normalizedKey = textKey.trim();
        if (normalizedKey.isEmpty()) {
            return fallbackText;
        }
        String localized = GuideResourceLanguageIndex.getValue(compiler.getLanguage(), normalizedKey);
        return localized != null && !localized.isEmpty() ? localized : fallbackText;
    }

    private static TextAnnotation.ConnectorSide parseConnectorSide(PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el) {
        String rawSide = MdxAttrs.getString(compiler, errorSink, el, "connectorSide", null);
        try {
            return TextAnnotation.ConnectorSide.fromSerializedName(rawSide);
        } catch (IllegalArgumentException e) {
            errorSink.appendError(compiler, e.getMessage(), el);
            return TextAnnotation.ConnectorSide.BOTTOM;
        }
    }

    private static String trimCommonIndent(String text) {
        List<String> lines = GuideStringLines.splitLines(text);
        int start = 0;
        int end = lines.size();
        while (start < end && lines.get(start)
            .trim()
            .isEmpty()) {
            start++;
        }
        while (end > start && lines.get(end - 1)
            .trim()
            .isEmpty()) {
            end--;
        }
        int indent = Integer.MAX_VALUE;
        for (int i = start; i < end; i++) {
            if (lines.get(i)
                .trim()
                .isEmpty()) {
                continue;
            }
            indent = Math.min(indent, countLeadingSpaces(lines.get(i)));
        }
        if (indent == Integer.MAX_VALUE) {
            indent = 0;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < end; i++) {
            if (i > start) {
                builder.append('\n');
            }
            String line = lines.get(i);
            builder.append(line.substring(Math.min(indent, line.length())));
        }
        return builder.toString();
    }

    private static int countLeadingSpaces(String line) {
        int count = 0;
        while (count < line.length() && line.charAt(count) == ' ') {
            count++;
        }
        return count;
    }

    private static int clampAlpha(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
