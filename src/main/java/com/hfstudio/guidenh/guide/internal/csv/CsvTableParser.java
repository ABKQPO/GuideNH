package com.hfstudio.guidenh.guide.internal.csv;

import java.util.ArrayList;
import java.util.List;

public class CsvTableParser {

    protected CsvTableParser() {}

    public static List<List<String>> parse(String rawText) {
        String text = normalize(rawText);
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (inQuotes) {
                if (current == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        cell.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cell.append(current);
                }
                continue;
            }

            switch (current) {
                case '"' -> inQuotes = true;
                case ',' -> flushCell(row, cell);
                case '\n' -> flushRow(rows, row, cell);
                default -> cell.append(current);
            }
        }

        if (!row.isEmpty() || !cell.isEmpty()) {
            flushRow(rows, row, cell);
        }

        return rows;
    }

    private static String normalize(String rawText) {
        if (rawText == null || rawText.isEmpty()) {
            return "";
        }

        int start = rawText.charAt(0) == '\uFEFF' ? 1 : 0;
        StringBuilder normalized = null;
        for (int i = start; i < rawText.length(); i++) {
            char current = rawText.charAt(i);
            if (current == '\r') {
                if (normalized == null) {
                    normalized = new StringBuilder(rawText.length());
                    normalized.append(rawText, start, i);
                }
                normalized.append('\n');
                if (i + 1 < rawText.length() && rawText.charAt(i + 1) == '\n') {
                    i++;
                }
            } else if (normalized != null) {
                normalized.append(current);
            }
        }
        if (normalized != null) {
            return normalized.toString();
        }
        return start == 0 ? rawText : rawText.substring(start);
    }

    private static void flushCell(List<String> row, StringBuilder cell) {
        row.add(cell.toString());
        cell.setLength(0);
    }

    private static void flushRow(List<List<String>> rows, List<String> row, StringBuilder cell) {
        flushCell(row, cell);
        if (!isBlankRow(row)) {
            rows.add(new ArrayList<>(row));
        }
        row.clear();
    }

    private static boolean isBlankRow(List<String> row) {
        if (row.size() != 1) {
            return false;
        }
        return row.getFirst()
            .isEmpty();
    }
}
