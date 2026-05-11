package com.hfstudio.structurelibexport;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.command.CommandException;

public class StructureLibNumericFilter {

    private final boolean all;
    private final List<Range> includes;
    private final List<Range> excludes;

    public StructureLibNumericFilter(boolean all, List<Range> includes, List<Range> excludes) {
        this.all = all;
        this.includes = includes != null ? new ArrayList<>(includes) : new ArrayList<>();
        this.excludes = excludes != null ? new ArrayList<>(excludes) : new ArrayList<>();
    }

    public static StructureLibNumericFilter all() {
        return new StructureLibNumericFilter(true, new ArrayList<>(), new ArrayList<>());
    }

    public static StructureLibNumericFilter parse(String expression) throws CommandException {
        if (expression == null || expression.trim()
            .isEmpty() || "all".equalsIgnoreCase(expression.trim())) {
            return all();
        }

        List<Range> includes = new ArrayList<>();
        List<Range> excludes = new ArrayList<>();
        String[] tokens = expression.split(",");
        for (String token : tokens) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            boolean excluded = trimmed.startsWith("!");
            String value = excluded ? trimmed.substring(1)
                .trim() : trimmed;
            Range range = Range.parse(value);
            if (excluded) {
                excludes.add(range);
            } else {
                includes.add(range);
            }
        }
        return new StructureLibNumericFilter(includes.isEmpty(), includes, excludes);
    }

    public boolean matches(int value) {
        boolean included = all || includes.isEmpty();
        if (!included) {
            for (Range range : includes) {
                if (range.contains(value)) {
                    included = true;
                    break;
                }
            }
        }
        if (!included) {
            return false;
        }
        for (Range range : excludes) {
            if (range.contains(value)) {
                return false;
            }
        }
        return true;
    }

    public Set<Integer> resolveWithin(int min, int max) {
        LinkedHashSet<Integer> values = new LinkedHashSet<>();
        int lower = Math.min(min, max);
        int upper = Math.max(min, max);
        for (int value = lower; value <= upper; value++) {
            if (matches(value)) {
                values.add(value);
            }
        }
        return values;
    }

    public boolean isAll() {
        return all && excludes.isEmpty();
    }

    public static class Range {

        private final int min;
        private final int max;

        public Range(int min, int max) {
            this.min = Math.min(min, max);
            this.max = Math.max(min, max);
        }

        public static Range parse(String raw) throws CommandException {
            if (raw == null || raw.trim()
                .isEmpty()) {
                throw new CommandException("Empty numeric filter token.");
            }
            String trimmed = raw.trim();
            int dash = trimmed.indexOf('-', 1);
            try {
                if (dash >= 0) {
                    return new Range(
                        Integer.parseInt(
                            trimmed.substring(0, dash)
                                .trim()),
                        Integer.parseInt(
                            trimmed.substring(dash + 1)
                                .trim()));
                }
                int value = Integer.parseInt(trimmed);
                return new Range(value, value);
            } catch (NumberFormatException e) {
                throw new CommandException("Invalid numeric filter token: " + raw);
            }
        }

        public boolean contains(int value) {
            return value >= min && value <= max;
        }
    }
}
