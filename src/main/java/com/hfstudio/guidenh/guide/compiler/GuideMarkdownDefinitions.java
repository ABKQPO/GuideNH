package com.hfstudio.guidenh.guide.compiler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstDefinition;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParent;

public final class GuideMarkdownDefinitions {

    private GuideMarkdownDefinitions() {}

    public static Map<String, MdAstDefinition> collect(MdAstParent<?> parent) {
        return collect(parent.children());
    }

    public static Map<String, MdAstDefinition> collect(List<? extends MdAstAnyContent> children) {
        Map<String, MdAstDefinition> definitions = new HashMap<>();
        for (MdAstAnyContent child : children) {
            if (child instanceof MdAstDefinition definition && definition.identifier != null) {
                definitions.putIfAbsent(definition.identifier, definition);
            }
        }
        return definitions;
    }

    public static @Nullable MdAstDefinition find(Map<String, MdAstDefinition> definitions,
        @Nullable String identifier) {
        return identifier != null ? definitions.get(identifier) : null;
    }
}
