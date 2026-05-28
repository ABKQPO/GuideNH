package com.hfstudio.guidenh.guide.internal.editor.md;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorElementModel;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorElementType;

public class SceneEditorMarkdownElementRangeIndex {

    public static final SceneEditorMarkdownElementRangeIndex EMPTY = new SceneEditorMarkdownElementRangeIndex(
        List.of());

    private final List<SceneEditorMarkdownElementRange> ranges;

    private SceneEditorMarkdownElementRangeIndex(List<SceneEditorMarkdownElementRange> ranges) {
        this.ranges = ranges;
    }

    public static SceneEditorMarkdownElementRangeIndex empty() {
        return EMPTY;
    }

    public static SceneEditorMarkdownElementRangeIndex fromBestEffortText(String text,
        List<SceneEditorElementModel> elements) {
        if (text == null || text.isEmpty() || elements == null || elements.isEmpty()) {
            return empty();
        }

        List<MatchedTag> matchedTags = collectMatchedTags(text);
        if (matchedTags.isEmpty()) {
            return empty();
        }

        List<SceneEditorMarkdownElementRange> ranges = new ArrayList<>();
        int matchIndex = 0;
        for (SceneEditorElementModel element : elements) {
            while (matchIndex < matchedTags.size()) {
                MatchedTag tag = matchedTags.get(matchIndex++);
                if (!element.getType()
                    .getTagName()
                    .equals(tag.tagName)) {
                    continue;
                }
                ranges.add(new SceneEditorMarkdownElementRange(element.getId(), tag.startIndex, tag.endIndex));
                break;
            }
        }
        if (ranges.isEmpty()) {
            return empty();
        }
        return new SceneEditorMarkdownElementRangeIndex(List.copyOf(ranges));
    }

    public boolean isEmpty() {
        return ranges.isEmpty();
    }

    public Optional<SceneEditorMarkdownElementRange> findByElementId(UUID elementId) {
        if (elementId == null) {
            return Optional.empty();
        }
        for (SceneEditorMarkdownElementRange range : ranges) {
            if (range.getElementId()
                .equals(elementId)) {
                return Optional.of(range);
            }
        }
        return Optional.empty();
    }

    public Optional<UUID> findByCursor(int cursorIndex) {
        for (SceneEditorMarkdownElementRange range : ranges) {
            if (range.contains(cursorIndex)) {
                return Optional.of(range.getElementId());
            }
        }
        return Optional.empty();
    }

    public static List<MatchedTag> collectMatchedTags(String text) {
        List<MatchedTag> matchedTags = new ArrayList<>();
        Matcher matcher = createElementStartPattern().matcher(text);
        while (matcher.find()) {
            String tagName = matcher.group(1);
            int startIndex = matcher.start();
            int openTagEnd = text.indexOf('>', matcher.end());
            if (openTagEnd < 0) {
                continue;
            }
            if (isSelfClosingTag(text, openTagEnd)) {
                matchedTags.add(new MatchedTag(tagName, startIndex, openTagEnd + 1));
                continue;
            }
            String closingTag = "</" + tagName + ">";
            int closingIndex = text.indexOf(closingTag, openTagEnd + 1);
            if (closingIndex < 0) {
                continue;
            }
            matchedTags.add(new MatchedTag(tagName, startIndex, closingIndex + closingTag.length()));
        }
        return matchedTags;
    }

    private static Pattern createElementStartPattern() {
        StringBuilder builder = new StringBuilder("<(");
        List<SceneEditorElementType> types = SceneEditorElementType.values();
        for (int i = 0; i < types.size(); i++) {
            if (i > 0) {
                builder.append('|');
            }
            builder.append(
                Pattern.quote(
                    types.get(i)
                        .getTagName()));
        }
        builder.append(")\\b");
        return Pattern.compile(builder.toString());
    }

    public static boolean isSelfClosingTag(String text, int openTagEnd) {
        for (int i = openTagEnd - 1; i >= 0; i--) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                continue;
            }
            return c == '/';
        }
        return false;
    }

    public static class MatchedTag {

        private final String tagName;
        private final int startIndex;
        private final int endIndex;

        private MatchedTag(String tagName, int startIndex, int endIndex) {
            this.tagName = tagName;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }
    }
}
