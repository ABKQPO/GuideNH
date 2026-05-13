package com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver;

import java.util.EnumMap;
import java.util.Map;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.SelectionStrategy;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.SyntaxElementType;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.TextSyntaxContext;

public final class SelectionStrategies {

    private SelectionStrategies() {}

    public static Map<SyntaxElementType, SelectionStrategy> defaults() {
        EnumMap<SyntaxElementType, SelectionStrategy> map = new EnumMap<>(SyntaxElementType.class);
        map.put(SyntaxElementType.WORD, new WordSelection());
        map.put(SyntaxElementType.TAG_NAME, new ElementBoundarySelection());
        map.put(SyntaxElementType.ATTRIBUTE_NAME, new ElementBoundarySelection());
        map.put(SyntaxElementType.ATTRIBUTE_VALUE, new ElementBoundarySelection());
        map.put(SyntaxElementType.OTHER, new NoOpSelection());
        return map;
    }

    public static final class WordSelection implements SelectionStrategy {
        @Override
        public int getSelectionStart(TextSyntaxContext ctx, String text, int cursorIndex) {
            int pos = cursorIndex;
            while (pos > 0 && isWordChar(text.charAt(pos - 1))) {
                pos--;
            }
            return pos;
        }

        @Override
        public int getSelectionEnd(TextSyntaxContext ctx, String text, int cursorIndex) {
            int pos = cursorIndex;
            int len = text.length();
            while (pos < len && isWordChar(text.charAt(pos))) {
                pos++;
            }
            return pos;
        }

        private static boolean isWordChar(char c) {
            return Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.' || c == ':';
        }
    }

    public static final class ElementBoundarySelection implements SelectionStrategy {
        @Override
        public int getSelectionStart(TextSyntaxContext ctx, String text, int cursorIndex) {
            return ctx.getElementStart();
        }

        @Override
        public int getSelectionEnd(TextSyntaxContext ctx, String text, int cursorIndex) {
            return ctx.getElementEnd();
        }
    }

    public static final class NoOpSelection implements SelectionStrategy {
        @Override
        public int getSelectionStart(TextSyntaxContext ctx, String text, int cursorIndex) {
            return cursorIndex;
        }

        @Override
        public int getSelectionEnd(TextSyntaxContext ctx, String text, int cursorIndex) {
            return cursorIndex;
        }
    }
}
