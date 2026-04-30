package com.hfstudio.guidenh.guide.internal.csv;

import java.util.ArrayList;
import java.util.List;

public final class CsvTableParser {

    private CsvTableParser() {}

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

        if (!row.isEmpty() || cell.length() > 0) {
            flushRow(rows, row, cell);
        }

        return rows;
    }

    private static String normalize(String rawText) {
        if (rawText == null || rawText.isEmpty()) {
            return "";
        }

        String text = rawText.replace("\r\n", "\n")
            .replace('\r', '\n');
        if (!text.isEmpty() && text.charAt(0) == '\uFEFF') {
            return text.substring(1);
        }
        return text;
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
        return row.get(0)
            .isEmpty();
    }
}
