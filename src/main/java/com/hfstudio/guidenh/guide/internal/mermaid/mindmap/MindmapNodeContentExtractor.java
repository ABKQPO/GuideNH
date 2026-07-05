package com.hfstudio.guidenh.guide.internal.mermaid.mindmap;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.MdxBlockTagSourceExtractor;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidSourceExtractor;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.unist.UnistPosition;

public class MindmapNodeContentExtractor {

    private MindmapNodeContentExtractor() {}

    public static @Nullable String extractDiagramSource(MdxJsxElementFields element, @Nullable String pageSource) {
        String source = extractBlockTagChildrenSource(element, pageSource);
        if (source != null && !source.trim()
            .isEmpty()) {
            return MermaidSourceExtractor.stripExplicitNodeContentBlocks(source);
        }

        source = extractChildrenSource(element, pageSource);
        if (source != null && !source.trim()
            .isEmpty()) {
            return MermaidSourceExtractor.stripExplicitNodeContentBlocks(source);
        }
        return null;
    }

    private static @Nullable String extractBlockTagChildrenSource(MdxJsxElementFields element,
        @Nullable String pageSource) {
        if (element == null || pageSource == null || pageSource.isEmpty()) {
            return null;
        }

        String body = MdxBlockTagSourceExtractor.extractRawBody(element, pageSource);
        if (body == null) {
            return null;
        }

        return dedentBlockTagBody(body);
    }

    private static @Nullable String extractChildrenSource(MdxJsxElementFields element, @Nullable String pageSource) {
        if (element == null || pageSource == null || pageSource.isEmpty()) {
            return null;
        }

        List<? extends MdAstAnyContent> children = element.children();
        if (children == null || children.isEmpty()) {
            return null;
        }

        UnistPosition firstPosition = null;
        UnistPosition lastPosition = null;
        for (MdAstAnyContent child : children) {
            UnistPosition position = child.position();
            if (position == null || position.start() == null || position.end() == null) {
                return null;
            }
            if (firstPosition == null) {
                firstPosition = position;
            }
            lastPosition = position;
        }

        int sourceStart = firstPosition.start()
            .offset();
        int sourceEnd = lastPosition.end()
            .offset();
        if (sourceStart < 0 || sourceEnd <= sourceStart || sourceEnd > pageSource.length()) {
            return null;
        }
        return dedentBlockTagBody(pageSource.substring(sourceStart, sourceEnd));
    }

    private static String dedentBlockTagBody(String body) {
        return MermaidSourceExtractor.dedentBlockTagBody(body);
    }
}
