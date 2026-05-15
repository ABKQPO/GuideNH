package com.hfstudio.guidenh.guide.scene.element;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.scene.SceneTagCompiler;
import com.hfstudio.guidenh.integration.structurelib.StructureLibSceneOptions;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxAttribute;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstNode;

public class StructureLibSceneOptionParser {

    private StructureLibSceneOptionParser() {}

    public static StructureLibSceneOptions parseChildren(PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields parent) {
        StructureLibSceneOptions.Builder builder = StructureLibSceneOptions.builder();
        if (parent == null || parent.children() == null) {
            return builder.build();
        }
        for (MdAstAnyContent child : parent.children()) {
            MdxJsxElementFields childElement = SceneTagCompiler.unwrapSceneElement(child);
            if (childElement == null || childElement.name() == null) {
                continue;
            }
            applyChild(compiler, errorSink, builder, childElement);
        }
        return builder.build();
    }

    public static StructureLibSceneOptions parseAttributes(PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields element) {
        StructureLibSceneOptions.Builder builder = StructureLibSceneOptions.builder();
        applyIntAttribute(compiler, errorSink, element, builder, "tier");
        applyStringAttribute(compiler, errorSink, element, builder, "facing");
        applyStringAttribute(compiler, errorSink, element, builder, "rotation");
        applyStringAttribute(compiler, errorSink, element, builder, "flip");
        if (MdxAttrs.getBoolean(compiler, errorSink, element, "gtActiveController", false)) {
            builder.gregTechActiveController(true);
        }
        if (MdxAttrs.getBoolean(compiler, errorSink, element, "gtPlaceHatches", false)) {
            builder.gregTechPlaceHatches(true);
        }
        String channelName = MdxAttrs.getString(compiler, errorSink, element, "channelName", null);
        if (channelName == null) {
            channelName = MdxAttrs.getString(compiler, errorSink, element, "name", null);
        }
        if (channelName != null) {
            int value = readInt(compiler, errorSink, element, "channel", 0);
            if (value <= 0) {
                value = readInt(compiler, errorSink, element, "value", 0);
            }
            if (value > 0) {
                builder.channel(channelName, value);
            }
        }
        return builder.build();
    }

    private static void applyChild(PageCompiler compiler, LytErrorSink errorSink,
        StructureLibSceneOptions.Builder builder, MdxJsxElementFields childElement) {
        String name = childElement.name();
        switch (name) {
            case "Tier":
                builder.tier(resolveIntValue(compiler, errorSink, childElement));
                break;
            case "Channel":
                applyChannel(compiler, errorSink, builder, childElement);
                break;
            case "Facing":
                builder.facing(resolveTextValue(compiler, errorSink, childElement));
                break;
            case "Rotation":
                builder.rotation(resolveTextValue(compiler, errorSink, childElement));
                break;
            case "Flip":
                builder.flip(resolveTextValue(compiler, errorSink, childElement));
                break;
            case "Orientation":
                applyOrientation(compiler, errorSink, builder, childElement);
                break;
            case "GregTechActiveController":
            case "GtActiveController":
                builder.gregTechActiveController(true);
                break;
            case "GregTechPlaceHatches":
            case "GtPlaceHatches":
                builder.gregTechPlaceHatches(true);
                break;
            default:
                break;
        }
    }

    private static void applyChannel(PageCompiler compiler, LytErrorSink errorSink,
        StructureLibSceneOptions.Builder builder, MdxJsxElementFields childElement) {
        String name = MdxAttrs.getString(compiler, errorSink, childElement, "name", null);
        if (name == null) {
            name = MdxAttrs.getString(compiler, errorSink, childElement, "id", null);
        }
        Integer value = resolveIntValue(compiler, errorSink, childElement);
        if (name != null && value != null) {
            builder.channel(name, value);
        }
    }

    private static void applyOrientation(PageCompiler compiler, LytErrorSink errorSink,
        StructureLibSceneOptions.Builder builder, MdxJsxElementFields childElement) {
        String raw = resolveTextValue(compiler, errorSink, childElement);
        if (raw == null) {
            return;
        }
        String[] parts = raw.split(":");
        if (parts.length > 0) {
            builder.facing(parts[0]);
        }
        if (parts.length > 1) {
            builder.rotation(parts[1]);
        }
        if (parts.length > 2) {
            builder.flip(parts[2]);
        }
    }

    @Nullable
    private static Integer resolveIntValue(PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields childElement) {
        for (String attribute : new String[] { "value", "expr", "tier" }) {
            MdxJsxAttribute rawAttribute = childElement.getAttribute(attribute);
            if (rawAttribute != null) {
                return readInt(compiler, errorSink, childElement, attribute, 0);
            }
        }
        String text = childText(childElement);
        if (text == null) {
            return null;
        }
        try {
            return Integer.valueOf(text.trim());
        } catch (NumberFormatException e) {
            errorSink.appendError(compiler, "Malformed StructureLib option integer: '" + text + "'", childElement);
            return null;
        }
    }

    @Nullable
    private static String resolveTextValue(PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields childElement) {
        for (String attribute : new String[] { "value", "expr", "name" }) {
            String value = MdxAttrs.getString(compiler, errorSink, childElement, attribute, null);
            if (value != null && !value.trim()
                .isEmpty()) {
                return value;
            }
        }
        return childText(childElement);
    }

    @Nullable
    private static String childText(MdxJsxElementFields childElement) {
        if (childElement.children() == null || childElement.children()
            .isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (MdAstAnyContent content : childElement.children()) {
            if (content instanceof MdAstNode node) {
                builder.append(node.toText());
            }
        }
        String text = builder.toString()
            .trim();
        return text.isEmpty() ? null : text;
    }

    private static void applyIntAttribute(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields element,
        StructureLibSceneOptions.Builder builder, String attribute) {
        if (element.getAttribute(attribute) != null) {
            builder.tier(readInt(compiler, errorSink, element, attribute, 0));
        }
    }

    private static void applyStringAttribute(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields element,
        StructureLibSceneOptions.Builder builder, String attribute) {
        String value = MdxAttrs.getString(compiler, errorSink, element, attribute, null);
        if (value == null || value.trim()
            .isEmpty()) {
            return;
        }
        switch (attribute.toLowerCase(Locale.ROOT)) {
            case "facing":
                builder.facing(value);
                break;
            case "rotation":
                builder.rotation(value);
                break;
            case "flip":
                builder.flip(value);
                break;
            default:
                break;
        }
    }

    private static int readInt(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields element,
        String attribute, int defaultValue) {
        return MdxAttrs.getInt(compiler, errorSink, element, attribute, defaultValue);
    }
}
