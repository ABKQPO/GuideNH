package com.hfstudio.guidenh.guide.internal.markdown;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

public final class MarkdownLiteralAutolink {

    private static final Pattern LITERAL_LINK = Pattern.compile(
        "(?i)(?:(?:https://|http://)[^\\s<>()]+|(?:www\\.)[^\\s<>()]+|(?:[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}))");

    private MarkdownLiteralAutolink() {}

    public static boolean mayContainLiteralAutolink(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return text.contains("://") || text.contains("www.") || text.indexOf('@') >= 0;
    }

    public static List<Segment> split(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }

        Matcher matcher = LITERAL_LINK.matcher(text);
        List<Segment> result = new ArrayList<>();
        int index = 0;
        while (matcher.find()) {
            if (matcher.start() > index) {
                result.add(Segment.text(text.substring(index, matcher.start())));
            }

            String token = trimTrailingPunctuation(matcher.group());
            if (token.isEmpty()) {
                continue;
            }

            result.add(Segment.link(token, toHref(token)));
            index = matcher.start() + token.length();
        }

        if (index < text.length()) {
            result.add(Segment.text(text.substring(index)));
        }
        return result;
    }

    private static String toHref(String token) {
        if (token.regionMatches(true, 0, "http://", 0, "http://".length())
            || token.regionMatches(true, 0, "https://", 0, "https://".length())) {
            return token;
        }
        if (token.regionMatches(true, 0, "www.", 0, "www.".length())) {
            return "https://" + token;
        }
        return "mailto:" + token;
    }

    private static String trimTrailingPunctuation(String token) {
        int end = token.length();
        while (end > 0) {
            char current = token.charAt(end - 1);
            if (current == '.' || current == ','
                || current == '!'
                || current == '?'
                || current == ':'
                || current == ';') {
                end--;
                continue;
            }
            break;
        }
        return token.substring(0, end);
    }

    public static URI toUri(String href) {
        return URI.create(href);
    }

    public static final class Segment {

        private final String text;
        private final String href;

        private Segment(String text, String href) {
            this.text = text;
            this.href = href;
        }

        public static Segment text(String text) {
            return new Segment(text, null);
        }

        public static Segment link(String text, String href) {
            return new Segment(text, href);
        }

        public boolean isLink() {
            return href != null && !href.isEmpty();
        }

        public String text() {
            return text;
        }

        public @Nullable String href() {
            return href;
        }
    }
}
