package com.hfstudio.guidenh.guide.internal.editor.guide;

public class GuideScreenEditorMerge {

    private GuideScreenEditorMerge() {}

    public static Result merge(String baseText, String localText, String externalText) {
        String base = safe(baseText);
        String local = safe(localText);
        String external = safe(externalText);

        if (local.equals(external)) {
            return Result.externalWins(external);
        }
        if (local.equals(base)) {
            return Result.externalWins(external);
        }
        if (external.equals(base)) {
            return Result.localWins(local);
        }

        Change localChange = diff(base, local);
        Change externalChange = diff(base, external);
        if (localChange == null || externalChange == null) {
            return Result.conflict();
        }
        if (localChange.overlaps(externalChange)) {
            return Result.conflict();
        }

        String merged = apply(base, localChange);
        merged = apply(merged, externalChange.shifted(localChange));
        return Result.merged(merged);
    }

    private static String safe(String text) {
        return text != null ? text : "";
    }

    private static Change diff(String base, String next) {
        int prefix = 0;
        int maxPrefix = Math.min(base.length(), next.length());
        while (prefix < maxPrefix && base.charAt(prefix) == next.charAt(prefix)) {
            prefix++;
        }
        int suffix = 0;
        int maxSuffix = Math.min(base.length() - prefix, next.length() - prefix);
        while (suffix < maxSuffix
            && base.charAt(base.length() - 1 - suffix) == next.charAt(next.length() - 1 - suffix)) {
            suffix++;
        }
        return new Change(prefix, base.length() - suffix, next.substring(prefix, next.length() - suffix));
    }

    private static String apply(String text, Change change) {
        return text.substring(0, change.start) + change.replacement + text.substring(change.end);
    }

    public static final class Result {

        public enum Kind {
            MERGED,
            LOCAL_WINS,
            EXTERNAL_WINS,
            CONFLICT
        }

        private final Kind kind;
        private final String text;

        private Result(Kind kind, String text) {
            this.kind = kind;
            this.text = text;
        }

        public static Result merged(String text) {
            return new Result(Kind.MERGED, text);
        }

        public static Result localWins(String text) {
            return new Result(Kind.LOCAL_WINS, text);
        }

        public static Result externalWins(String text) {
            return new Result(Kind.EXTERNAL_WINS, text);
        }

        public static Result conflict() {
            return new Result(Kind.CONFLICT, "");
        }

        public Kind getKind() {
            return kind;
        }

        public String getText() {
            return text;
        }
    }

    private static final class Change {

        private final int start;
        private final int end;
        private final String replacement;

        private Change(int start, int end, String replacement) {
            this.start = start;
            this.end = end;
            this.replacement = replacement;
        }

        private boolean overlaps(Change other) {
            return start < other.end && other.start < end;
        }

        private Change shifted(Change earlier) {
            int delta = earlier.replacement.length() - (earlier.end - earlier.start);
            int shiftedStart = start;
            int shiftedEnd = end;
            if (earlier.end <= start) {
                shiftedStart += delta;
                shiftedEnd += delta;
            }
            return new Change(shiftedStart, shiftedEnd, replacement);
        }
    }
}
