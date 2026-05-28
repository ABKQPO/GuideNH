package com.hfstudio.guidenh.guide.scene;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

public class StructureLibValueCondition {

    private final List<ValueRange> includeRanges;
    private final List<ValueRange> excludeRanges;

    public StructureLibValueCondition(List<ValueRange> includeRanges, List<ValueRange> excludeRanges) {
        this.includeRanges = immutableRanges(includeRanges);
        this.excludeRanges = immutableRanges(excludeRanges);
    }

    public static StructureLibValueCondition parse(@Nullable String expression) {
        if (expression == null || expression.trim()
            .isEmpty()) {
            return null;
        }
        ArrayList<ValueRange> includes = new ArrayList<>();
        ArrayList<ValueRange> excludes = new ArrayList<>();
        for (String rawToken : expression.split(",")) {
            String token = rawToken != null ? rawToken.trim() : "";
            if (token.isEmpty()) {
                continue;
            }
            boolean negated = token.charAt(0) == '!';
            String body = negated ? token.substring(1)
                .trim() : token;
            if (body.isEmpty()) {
                throw new IllegalArgumentException("Condition token '!' is missing a value.");
            }
            ValueRange range = ValueRange.parse(body);
            if (negated) {
                excludes.add(range);
            } else {
                includes.add(range);
            }
        }
        if (includes.isEmpty() && excludes.isEmpty()) {
            return null;
        }
        return new StructureLibValueCondition(includes, excludes);
    }

    public boolean matches(int value) {
        if (!includeRanges.isEmpty()) {
            boolean matchedInclude = false;
            for (ValueRange range : includeRanges) {
                if (range.contains(value)) {
                    matchedInclude = true;
                    break;
                }
            }
            if (!matchedInclude) {
                return false;
            }
        }
        for (ValueRange range : excludeRanges) {
            if (range.contains(value)) {
                return false;
            }
        }
        return true;
    }

    public boolean hasIncludes() {
        return !includeRanges.isEmpty();
    }

    public boolean hasExcludes() {
        return !excludeRanges.isEmpty();
    }

    public List<ValueRange> getIncludeRanges() {
        return includeRanges;
    }

    public List<ValueRange> getExcludeRanges() {
        return excludeRanges;
    }

    public Map<String, Object> toSiteExportData() {
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        if (!includeRanges.isEmpty()) {
            data.put("include", serializeRanges(includeRanges));
        }
        if (!excludeRanges.isEmpty()) {
            data.put("exclude", serializeRanges(excludeRanges));
        }
        return data;
    }

    private static List<int[]> serializeRanges(List<ValueRange> ranges) {
        ArrayList<int[]> serialized = new ArrayList<>(ranges.size());
        for (ValueRange range : ranges) {
            serialized.add(new int[] { range.getMinValue(), range.getMaxValue() });
        }
        return serialized;
    }

    private static List<ValueRange> immutableRanges(@Nullable List<ValueRange> ranges) {
        if (ranges == null || ranges.isEmpty()) {
            return List.of();
        }
        ArrayList<ValueRange> copied = new ArrayList<>(ranges.size());
        for (ValueRange range : ranges) {
            if (range != null) {
                copied.add(range);
            }
        }
        return copied.isEmpty() ? List.of() : List.copyOf(copied);
    }

    public static class ValueRange {

        private final int minValue;
        private final int maxValue;

        public ValueRange(int minValue, int maxValue) {
            this.minValue = Math.min(minValue, maxValue);
            this.maxValue = Math.max(minValue, maxValue);
        }

        public static ValueRange parse(String raw) {
            String token = raw != null ? raw.trim() : "";
            if (token.isEmpty()) {
                throw new IllegalArgumentException("Condition value cannot be empty.");
            }
            int delimiter = token.indexOf("..");
            if (delimiter < 0) {
                int value = parseInt(token);
                return new ValueRange(value, value);
            }
            String min = token.substring(0, delimiter)
                .trim();
            String max = token.substring(delimiter + 2)
                .trim();
            if (min.isEmpty() || max.isEmpty()) {
                throw new IllegalArgumentException("Condition range '" + token + "' is incomplete.");
            }
            return new ValueRange(parseInt(min), parseInt(max));
        }

        private static int parseInt(String raw) {
            try {
                return Integer.parseInt(raw);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Condition value '" + raw + "' is not a valid integer.", e);
            }
        }

        public boolean contains(int value) {
            return value >= minValue && value <= maxValue;
        }

        public int getMinValue() {
            return minValue;
        }

        public int getMaxValue() {
            return maxValue;
        }
    }
}
