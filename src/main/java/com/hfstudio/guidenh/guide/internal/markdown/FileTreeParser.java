package com.hfstudio.guidenh.guide.internal.markdown;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;

/**
 * Parses a textual file tree into a depth-annotated structure. Two prefix flavors are accepted on
 * the same input:
 * <ul>
 * <li>Unicode box drawing produced by `tree -F` and similar utilities, using {@code |}, {@code +},
 * {@code \} or the proper {@code U+2502 U+251C U+2514 U+2500} characters.</li>
 * <li>ASCII output where a continuation column is {@code "|   "} and a branch is {@code "|-- "} or
 * {@code "\\-- "}.</li>
 * </ul>
 * Each non-empty input line is split into 4-character prefix slots followed by a payload. The
 * payload may begin with a directive of the form {@code "{:icon=...}"}, {@code "{:iconPng=...}"} or
 * {@code "{:iconItem=...}"} which is extracted into {@link FileTreeIcon}; the rest of the payload
 * is returned verbatim and is intended to be re-parsed as inline markdown by the caller.
 */
public final class FileTreeParser {

    private static final int SLOT_WIDTH = 4;

    private FileTreeParser() {}

    public static FileTreeModel parse(String source) {
        if (source == null || source.isEmpty()) {
            return new FileTreeModel(Collections.emptyList());
        }
        String normalized = source.replace("\r\n", "\n")
            .replace("\r", "\n");
        String[] lines = normalized.split("\n", -1);
        List<FileTreeEntry> entries = new ArrayList<>();
        for (String rawLine : lines) {
            if (rawLine.isEmpty() || rawLine.trim()
                .isEmpty()) {
                continue;
            }
            FileTreeEntry entry = parseLine(rawLine);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return new FileTreeModel(Collections.unmodifiableList(entries));
    }

    @Nullable
    private static FileTreeEntry parseLine(String line) {
        List<SlotKind> slots = new ArrayList<>();
        int cursor = 0;
        while (cursor + SLOT_WIDTH <= line.length()) {
            String chunk = line.substring(cursor, cursor + SLOT_WIDTH);
            SlotKind kind = classifySlot(chunk);
            if (kind == null) {
                break;
            }
            slots.add(kind);
            cursor += SLOT_WIDTH;
            if (kind == SlotKind.BRANCH || kind == SlotKind.LAST_BRANCH) {
                break;
            }
        }

        String payloadRaw = line.substring(cursor);
        if (payloadRaw.isEmpty() && slots.isEmpty()) {
            return null;
        }

        IconExtraction extraction = extractIcon(payloadRaw);
        String payload = extraction.payload();
        if (slots.isEmpty() && payload.trim()
            .isEmpty()) {
            return null;
        }

        boolean isLastSibling = !slots.isEmpty() && slots.get(slots.size() - 1) == SlotKind.LAST_BRANCH;
        return new FileTreeEntry(Collections.unmodifiableList(slots), isLastSibling, payload, extraction.icon());
    }

    @Nullable
    private static SlotKind classifySlot(String chunk) {
        char c0 = chunk.charAt(0);
        char c1 = chunk.charAt(1);
        char c2 = chunk.charAt(2);
        char c3 = chunk.charAt(3);
        if (isVerticalChar(c0) && c1 == ' ' && c2 == ' ' && c3 == ' ') {
            return SlotKind.VERTICAL;
        }
        if (isBranchChar(c0) && isHorizontalChar(c1) && isHorizontalChar(c2) && c3 == ' ') {
            return SlotKind.BRANCH;
        }
        if (isLastBranchChar(c0) && isHorizontalChar(c1) && isHorizontalChar(c2) && c3 == ' ') {
            return SlotKind.LAST_BRANCH;
        }
        if (c0 == ' ' && c1 == ' ' && c2 == ' ' && c3 == ' ') {
            return SlotKind.EMPTY;
        }
        return null;
    }

    private static boolean isVerticalChar(char c) {
        return c == '\u2502' || c == '|';
    }

    private static boolean isBranchChar(char c) {
        return c == '\u251C' || c == '+' || c == '|';
    }

    private static boolean isLastBranchChar(char c) {
        return c == '\u2514' || c == '\\';
    }

    private static boolean isHorizontalChar(char c) {
        return c == '\u2500' || c == '-';
    }

    private static IconExtraction extractIcon(String payload) {
        String trimmedLeading = stripLeadingSpaces(payload);
        if (!trimmedLeading.startsWith("{:")) {
            return new IconExtraction(payload, null);
        }
        int closing = trimmedLeading.indexOf('}');
        if (closing < 0) {
            return new IconExtraction(payload, null);
        }
        String body = trimmedLeading.substring(2, closing)
            .trim();
        FileTreeIcon icon = parseIconDirective(body);
        if (icon == null) {
            return new IconExtraction(payload, null);
        }
        String remaining = trimmedLeading.substring(closing + 1);
        return new IconExtraction(stripLeadingSpaces(remaining), icon);
    }

    @Nullable
    private static FileTreeIcon parseIconDirective(String body) {
        if (body.isEmpty()) {
            return null;
        }
        int eq = body.indexOf('=');
        if (eq <= 0 || eq >= body.length() - 1) {
            return null;
        }
        String key = body.substring(0, eq)
            .trim();
        String value = stripQuotes(
            body.substring(eq + 1)
                .trim());
        if (value.isEmpty()) {
            return null;
        }
        FileTreeIconKind kind;
        if ("icon".equalsIgnoreCase(key)) {
            kind = FileTreeIconKind.TEXT;
        } else if ("iconPng".equalsIgnoreCase(key) || "icon_png".equalsIgnoreCase(key)) {
            kind = FileTreeIconKind.PNG;
        } else if ("iconItem".equalsIgnoreCase(key) || "icon_item".equalsIgnoreCase(key)) {
            kind = FileTreeIconKind.ITEM;
        } else {
            return null;
        }
        return new FileTreeIcon(kind, value);
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    private static String stripLeadingSpaces(String text) {
        int i = 0;
        while (i < text.length() && text.charAt(i) == ' ') {
            i++;
        }
        return i == 0 ? text : text.substring(i);
    }

    public enum SlotKind {
        VERTICAL,
        BRANCH,
        LAST_BRANCH,
        EMPTY
    }

    public enum FileTreeIconKind {
        TEXT,
        PNG,
        ITEM
    }

    @Desugar
    public record FileTreeIcon(FileTreeIconKind kind, String value) {}

    @Desugar
    public record FileTreeEntry(List<SlotKind> slots, boolean isLastSibling, String payloadSource,
        @Nullable FileTreeIcon icon) {

        public int depth() {
            return slots.size();
        }
    }

    @Desugar
    public record FileTreeModel(List<FileTreeEntry> entries) {}

    @Desugar
    private record IconExtraction(String payload, @Nullable FileTreeIcon icon) {}
}
