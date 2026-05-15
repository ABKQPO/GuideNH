package com.hfstudio.guidenh.guide.internal.editor.autocomplete;

import java.util.List;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.AutocompleteCandidate;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.MdxAttrNameContext;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.MdxValueContext;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.TagStartContext;

public class AutocompleteCommitService {

    private AutocompleteCommitService() {}

    public static AutocompleteCommit commit(String text, AutocompleteContext context, AutocompleteCandidate candidate) {
        String source = text != null ? text : "";
        int replaceStart = clamp(context.replaceStart(), 0, source.length());
        int replaceEnd = clamp(context.replaceEnd(), replaceStart, source.length());
        Replacement replacement = createReplacement(source, context, candidate.replacementText());
        String newText = source.substring(0, replaceStart) + replacement.text + source.substring(replaceEnd);
        int cursor = replaceStart + replacement.cursorOffset;
        int selectionEnd = replaceStart + replacement.selectionEndOffset;
        return new AutocompleteCommit(newText, cursor, selectionEnd);
    }

    private static Replacement createReplacement(String source, AutocompleteContext context, String rawText) {
        String replacement = rawText != null ? rawText : "";
        if (context instanceof TagStartContext) {
            return createTagReplacement(source, (TagStartContext) context, replacement);
        }
        if (context instanceof MdxAttrNameContext) {
            return createAttributeNameReplacement(source, (MdxAttrNameContext) context, replacement);
        }
        if (context instanceof MdxValueContext) {
            return createAttributeValueReplacement(source, (MdxValueContext) context, replacement);
        }
        return Replacement.cursorAtEnd(replacement);
    }

    private static Replacement createTagReplacement(String source, TagStartContext context, String tagName) {
        int replaceEnd = clamp(context.replaceEnd(), 0, source.length());
        int pos = skipSpaces(source, replaceEnd);
        if (pos < source.length()) {
            char next = source.charAt(pos);
            if (next == '>' || next == '/') {
                return Replacement.cursorAtEnd(tagName);
            }
        }
        String text = tagName + " />";
        return new Replacement(text, text.length() - 2, text.length() - 2);
    }

    private static Replacement createAttributeNameReplacement(String source, MdxAttrNameContext context,
        String attributeName) {
        AttributeSpec spec = findSpec(context.getTagName(), attributeName);
        if (spec == null) {
            return Replacement.cursorAtEnd(attributeName);
        }

        AttrType type = spec.getType();
        String text;
        int cursorOffset;
        int selectionEndOffset;
        if (type == AttrType.BOOLEAN) {
            text = attributeName + "={true}";
            cursorOffset = attributeName.length() + 2;
            selectionEndOffset = cursorOffset + 4;
        } else if (shouldUseBraceValue(type)) {
            text = attributeName + "={}";
            cursorOffset = text.length() - 1;
            selectionEndOffset = cursorOffset;
        } else {
            text = attributeName + "=\"\"";
            cursorOffset = text.length() - 1;
            selectionEndOffset = cursorOffset;
        }
        return new Replacement(text, cursorOffset, selectionEndOffset);
    }

    private static Replacement createAttributeValueReplacement(String source, MdxValueContext context, String value) {
        int replaceStart = clamp(context.replaceStart(), 0, source.length());
        int replaceEnd = clamp(context.replaceEnd(), replaceStart, source.length());
        ValueEnvelope envelope = findValueEnvelope(source, replaceStart, replaceEnd);
        String replacement = value;
        int cursorOffset = replacement.length();
        int selectionEndOffset = cursorOffset;

        if (!envelope.hasDelimiter && shouldQuoteAttributeValue(context)) {
            replacement = "\"" + replacement + "\"";
            cursorOffset = replacement.length();
            selectionEndOffset = cursorOffset;
        } else if (context.getMissingValueTerminator() != '\0'
            && !endsWith(replacement, context.getMissingValueTerminator())) {
                replacement += context.getMissingValueTerminator();
                cursorOffset = replacement.length() - 1;
                selectionEndOffset = cursorOffset;
            }
        return new Replacement(replacement, cursorOffset, selectionEndOffset);
    }

    private static AttributeSpec findSpec(String tagName, String attributeName) {
        List<AttributeSpec> specs = TagAttributeRegistry.get(tagName);
        for (AttributeSpec spec : specs) {
            if (spec.getName()
                .equals(attributeName)) {
                return spec;
            }
        }
        return null;
    }

    private static boolean shouldUseBraceValue(AttrType type) {
        switch (type) {
            case INT:
            case FLOAT:
            case VECTOR3:
            case SNBT:
            case EXPRESSION:
                return true;
            default:
                return false;
        }
    }

    private static boolean shouldQuoteAttributeValue(MdxValueContext context) {
        AttributeSpec spec = findSpec(context.getTagName(), context.getAttrName());
        if (spec == null) {
            return false;
        }
        switch (spec.getType()) {
            case INT:
            case FLOAT:
            case BOOLEAN:
            case VECTOR3:
            case SNBT:
            case EXPRESSION:
                return false;
            default:
                return true;
        }
    }

    private static ValueEnvelope findValueEnvelope(String source, int valueStart, int valueEnd) {
        int before = valueStart - 1;
        if (before >= 0) {
            char open = source.charAt(before);
            if (open == '"' || open == '\'' || open == '{') {
                return new ValueEnvelope(true);
            }
        }
        if (valueEnd < source.length()) {
            char close = source.charAt(valueEnd);
            if (close == '"' || close == '\'' || close == '}') {
                return new ValueEnvelope(true);
            }
        }
        return new ValueEnvelope(false);
    }

    private static int skipSpaces(String source, int start) {
        int pos = start;
        while (pos < source.length() && Character.isWhitespace(source.charAt(pos))) {
            pos++;
        }
        return pos;
    }

    private static boolean endsWith(String value, char suffix) {
        return !value.isEmpty() && value.charAt(value.length() - 1) == suffix;
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static class Replacement {

        private final String text;
        private final int cursorOffset;
        private final int selectionEndOffset;

        private Replacement(String text, int cursorOffset, int selectionEndOffset) {
            this.text = text;
            this.cursorOffset = cursorOffset;
            this.selectionEndOffset = selectionEndOffset;
        }

        private static Replacement cursorAtEnd(String text) {
            return new Replacement(text, text.length(), text.length());
        }
    }

    private static class ValueEnvelope {

        private final boolean hasDelimiter;

        private ValueEnvelope(boolean hasDelimiter) {
            this.hasDelimiter = hasDelimiter;
        }
    }
}
