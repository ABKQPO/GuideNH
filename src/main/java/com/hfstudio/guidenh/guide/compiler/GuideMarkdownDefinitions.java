package com.hfstudio.guidenh.guide.compiler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstDefinition;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParent;

public class GuideMarkdownDefinitions {

    protected GuideMarkdownDefinitions() {}

    public static Map<String, MdAstDefinition> collect(MdAstParent<?> parent) {
        return collect(parent.children());
    }

    public static Map<String, MdAstDefinition> collect(List<? extends MdAstAnyContent> children) {
        Map<String, MdAstDefinition> definitions = new HashMap<>();
        for (MdAstAnyContent child : children) {
            // Pre-conversion: raw MdAstDefinition
            if (child instanceof MdAstDefinition && ((MdAstDefinition) child).identifier != null) {
                MdAstDefinition definition = (MdAstDefinition) child;
                definitions.putIfAbsent(definition.identifier, definition);
            }
            // Post-conversion: <definition> element
            if (child instanceof MdxJsxFlowElement && "definition".equals(((MdxJsxFlowElement) child).name())) {
                MdxJsxFlowElement el = (MdxJsxFlowElement) child;
                String identifier = el.getAttributeString("identifier", null);
                if (identifier != null) {
                    MdAstDefinition def = new MdAstDefinition();
                    def.identifier = identifier;
                    def.url = el.getAttributeString("url", "");
                    def.title = el.getAttributeString("title", "");
                    definitions.putIfAbsent(identifier, def);
                }
            }
        }
        return definitions;
    }

    public static @Nullable MdAstDefinition find(Map<String, MdAstDefinition> definitions,
        @Nullable String identifier) {
        return identifier != null ? definitions.get(identifier) : null;
    }
}
