package com.hfstudio.guidenh.bridge.preview;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.hfstudio.guidenh.bridge.semantic.providers.RuntimeSemanticSupport;

public class ItemPreviewSearchService {

    public PreviewSearchResult search(PreviewSearchQuery query) {
        if (!"items".equals(query.getCapability())) {
            throw new IllegalArgumentException("Unknown preview capability");
        }

        List<Map<String, String>> semanticEntries = new ArrayList<>();
        RuntimeSemanticSupport.addItemEntries(semanticEntries);
        RuntimeSemanticSupport.addBlockOnlyEntries(semanticEntries);
        Map<String, Integer> familySizes = buildFamilySizes(semanticEntries);

        String normalizedPrefix = normalize(query.getPrefix());
        List<RankedPreviewSearchEntry> rankedEntries = new ArrayList<>();
        for (Map<String, String> semanticEntry : semanticEntries) {
            String id = trimToNull(semanticEntry.get("id"));
            if (id == null) {
                continue;
            }
            String label = trimToNull(semanticEntry.get("label"));
            String detail = trimToNull(semanticEntry.get("detail"));
            int score = scoreEntry(id, label, detail, normalizedPrefix);
            if (score == Integer.MAX_VALUE) {
                continue;
            }
            String path = extractRawPath(id);
            String compactPrefix = compact(normalizedPrefix);
            rankedEntries.add(
                new RankedPreviewSearchEntry(
                    score,
                    computeStructuredMatchSpecificity(path, compactPrefix, score),
                    resolveFamilySize(familySizes, id),
                    new PreviewSearchEntry(id, label, detail, buildPreviewKey(id), describeMatchKind(score))));
        }

        rankedEntries.sort(
            Comparator.comparingInt(RankedPreviewSearchEntry::getScore)
                .thenComparingInt(
                    entry -> prefersLargerFamilyForScore(entry.getScore()) ? -entry.getFamilySize() : Integer.MAX_VALUE)
                .thenComparingInt(
                    entry -> prefersHigherStructuredSpecificityForScore(entry.getScore())
                        ? -entry.getStructuredSpecificity()
                        : Integer.MAX_VALUE)
                .thenComparingInt(
                    entry -> prefersShorterPathForScore(entry.getScore()) ? entry.getPathLength() : Integer.MAX_VALUE)
                .thenComparing(
                    entry -> entry.getEntry()
                        .getId(),
                    String.CASE_INSENSITIVE_ORDER)
                .thenComparing(
                    entry -> entry.getEntry()
                        .getLabel(),
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        List<PreviewSearchEntry> entries = new ArrayList<>(rankedEntries.size());
        for (RankedPreviewSearchEntry rankedEntry : rankedEntries) {
            entries.add(rankedEntry.getEntry());
        }
        return PreviewSearchResult.page(query.getCapability(), entries, query.getCursor(), query.getLimit());
    }

    private int scoreEntry(String id, String label, String detail, String prefix) {
        if (prefix.isEmpty()) {
            return 0;
        }

        String normalizedId = normalize(id);
        String normalizedLabel = normalize(label);
        String normalizedDetail = normalize(detail);
        String namespace = normalizedId.contains(":") ? normalizedId.substring(0, normalizedId.indexOf(':'))
            : normalizedId;
        String path = normalizedId.contains(":") ? normalizedId.substring(normalizedId.indexOf(':') + 1) : normalizedId;
        String rawPath = extractRawPath(id);
        String compactId = compact(normalizedId);
        String compactLabel = compact(normalizedLabel);
        String compactDetail = compact(normalizedDetail);
        String compactPath = compact(path);
        String compactPrefix = compact(prefix);
        String tokenInitials = createTokenInitials(rawPath);
        String labelInitials = createTokenInitials(normalizedLabel);
        boolean shortPrefix = isShortPrefix(prefix);

        if (normalizedLabel.equals(prefix)) {
            return 0;
        }
        if (normalizedId.equals(prefix)) {
            return 1;
        }
        if (namespace.startsWith(prefix) && shortPrefix) {
            return 2;
        }
        if (normalizedLabel.startsWith(prefix)) {
            return 3;
        }
        if (matchesTokenPrefix(normalizedLabel, prefix)) {
            return 4;
        }
        if (normalizedId.startsWith(prefix)) {
            return 5;
        }
        if (path.startsWith(prefix)) {
            return 6;
        }
        if (matchesTokenPrefix(normalizedId, prefix) || matchesTokenPrefix(path, prefix)) {
            return 7;
        }
        if (normalizedDetail.startsWith(prefix)) {
            return 8;
        }
        if (matchesStructuredPathAbbreviation(rawPath, compactPrefix)) {
            return 9;
        }
        if (!compactPrefix.isEmpty() && compactPrefix.length() >= 2 && tokenInitials.startsWith(compactPrefix)) {
            return 10;
        }
        if (!compactPrefix.isEmpty() && compactId.startsWith(compactPrefix)) {
            return 11;
        }
        if (!compactPrefix.isEmpty() && compactPath.startsWith(compactPrefix)) {
            return 12;
        }
        if (!compactPrefix.isEmpty() && compactPrefix.length() >= 2 && labelInitials.startsWith(compactPrefix)) {
            return 13;
        }
        if (!compactPrefix.isEmpty() && compactLabel.startsWith(compactPrefix)) {
            return 14;
        }
        if (!compactPrefix.isEmpty() && compactDetail.startsWith(compactPrefix)) {
            return 15;
        }
        if (normalizedId.contains(prefix)) {
            return 16;
        }
        if (normalizedLabel.contains(prefix)) {
            return 17;
        }
        if (normalizedDetail.contains(prefix)) {
            return 18;
        }
        return Integer.MAX_VALUE;
    }

    private boolean isShortPrefix(String prefix) {
        return prefix.indexOf(':') < 0 && !prefix.isEmpty() && prefix.length() <= 4;
    }

    private String describeMatchKind(int score) {
        return switch (score) {
            case 0 -> "label-exact";
            case 1 -> "id-exact";
            case 2 -> "namespace-prefix";
            case 3 -> "label-prefix";
            case 4 -> "label-token";
            case 5 -> "id-prefix";
            case 6 -> "path-prefix";
            case 7 -> "path-token";
            case 8 -> "detail-prefix";
            case 9 -> "path-structured";
            case 10 -> "path-acronym";
            case 11 -> "id-compact";
            case 12 -> "path-compact";
            case 13 -> "label-acronym";
            case 14 -> "label-compact";
            case 15 -> "detail-compact";
            case 16 -> "id-contains";
            case 17 -> "label-contains";
            case 18 -> "detail-contains";
            default -> "runtime";
        };
    }

    private String buildPreviewKey(String id) {
        return new ItemPreviewCacheKey("items", id, 0, 1, "", "default").toPreviewKey();
    }

    private String normalize(String value) {
        return value == null ? ""
            : value.trim()
                .toLowerCase(Locale.ROOT);
    }

    private String compact(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if ((current >= 'a' && current <= 'z') || (current >= '0' && current <= '9')) {
                builder.append(current);
            }
        }
        return builder.toString();
    }

    private boolean matchesTokenPrefix(String value, String prefix) {
        if (value == null || value.isEmpty() || prefix.isEmpty()) {
            return false;
        }
        int length = value.length();
        int tokenStart = -1;
        for (int index = 0; index <= length; index++) {
            char current = index < length ? value.charAt(index) : 0;
            boolean tokenCharacter = index < length
                && ((current >= 'a' && current <= 'z') || (current >= '0' && current <= '9'));
            if (tokenCharacter && tokenStart < 0) {
                tokenStart = index;
                continue;
            }
            if (tokenCharacter) {
                continue;
            }
            if (tokenStart >= 0 && value.regionMatches(tokenStart, prefix, 0, prefix.length())) {
                return true;
            }
            tokenStart = -1;
        }
        return false;
    }

    private String createTokenInitials(String value) {
        List<String> tokens = splitSearchTokens(value);
        if (tokens.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(tokens.size());
        for (String token : tokens) {
            builder.append(token.charAt(0));
        }
        return builder.toString();
    }

    private boolean matchesStructuredPathAbbreviation(String path, String compactPrefix) {
        if (compactPrefix == null || compactPrefix.length() < 2 || path == null || path.isEmpty()) {
            return false;
        }
        List<String> nonEmptyTokens = splitSearchTokens(path);
        if (nonEmptyTokens.size() < 2) {
            return false;
        }
        String firstToken = nonEmptyTokens.getFirst();
        if (!compactPrefix.startsWith(firstToken) || compactPrefix.length() <= firstToken.length()) {
            return false;
        }
        int queryIndex = firstToken.length();
        for (int tokenIndex = 1; tokenIndex < nonEmptyTokens.size()
            && queryIndex < compactPrefix.length(); tokenIndex++) {
            String token = nonEmptyTokens.get(tokenIndex);
            if (!token.startsWith(String.valueOf(compactPrefix.charAt(queryIndex)))) {
                return false;
            }
            queryIndex++;
        }
        return queryIndex == compactPrefix.length();
    }

    private String extractRawPath(String id) {
        if (id == null) {
            return "";
        }
        int separator = id.indexOf(':');
        return separator >= 0 ? id.substring(separator + 1) : id;
    }

    private List<String> splitSearchTokens(String value) {
        List<String> tokens = new ArrayList<>();
        if (value == null || value.isEmpty()) {
            return tokens;
        }
        StringBuilder builder = new StringBuilder(value.length());
        char previous = 0;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (!Character.isLetterOrDigit(current)) {
                flushToken(tokens, builder);
                previous = 0;
                continue;
            }
            if (shouldSplitToken(
                previous,
                current,
                index + 1 < value.length() ? value.charAt(index + 1) : 0,
                builder.length())) {
                flushToken(tokens, builder);
            }
            builder.append(Character.toLowerCase(current));
            previous = current;
        }
        flushToken(tokens, builder);
        return tokens;
    }

    private boolean shouldSplitToken(char previous, char current, char next, int currentLength) {
        if (currentLength <= 0 || previous == 0) {
            return false;
        }
        if (Character.isDigit(previous) != Character.isDigit(current)) {
            return true;
        }
        if (Character.isLowerCase(previous) && Character.isUpperCase(current)) {
            return true;
        }
        return Character.isUpperCase(previous) && Character.isUpperCase(current)
            && next != 0
            && Character.isLowerCase(next);
    }

    private void flushToken(List<String> tokens, StringBuilder builder) {
        if (builder.length() <= 0) {
            return;
        }
        tokens.add(builder.toString());
        builder.setLength(0);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean prefersShorterPathForScore(int score) {
        return score == 9 || score == 10 || score == 11 || score == 12;
    }

    private boolean prefersHigherStructuredSpecificityForScore(int score) {
        return score == 9;
    }

    private boolean prefersLargerFamilyForScore(int score) {
        return score == 9;
    }

    private int computeStructuredMatchSpecificity(String path, String compactPrefix, int score) {
        if (score != 9 || compactPrefix == null || compactPrefix.length() < 2 || path == null || path.isEmpty()) {
            return 0;
        }
        List<String> nonEmptyTokens = splitSearchTokens(path);
        if (nonEmptyTokens.size() < 2) {
            return 0;
        }
        String firstToken = nonEmptyTokens.getFirst();
        if (!compactPrefix.startsWith(firstToken) || compactPrefix.length() <= firstToken.length()) {
            return 0;
        }
        int queryIndex = firstToken.length();
        int specificity = firstToken.length();
        for (int tokenIndex = 1; tokenIndex < nonEmptyTokens.size()
            && queryIndex < compactPrefix.length(); tokenIndex++) {
            String token = nonEmptyTokens.get(tokenIndex);
            if (!token.startsWith(String.valueOf(compactPrefix.charAt(queryIndex)))) {
                return 0;
            }
            specificity += token.length();
            queryIndex++;
        }
        return queryIndex == compactPrefix.length() ? specificity : 0;
    }

    private Map<String, Integer> buildFamilySizes(List<Map<String, String>> semanticEntries) {
        Map<String, Integer> familySizes = new java.util.HashMap<>();
        for (Map<String, String> semanticEntry : semanticEntries) {
            String id = trimToNull(semanticEntry.get("id"));
            if (id == null) {
                continue;
            }
            String familyKey = toFamilyKey(id);
            familySizes.put(familyKey, familySizes.getOrDefault(familyKey, 0) + 1);
        }
        return familySizes;
    }

    private int resolveFamilySize(Map<String, Integer> familySizes, String id) {
        Integer value = familySizes.get(toFamilyKey(id));
        return value == null ? 0 : value;
    }

    private String toFamilyKey(String id) {
        String rawPath = extractRawPath(id);
        int metaSeparator = rawPath.lastIndexOf(':');
        if (metaSeparator >= 0) {
            String trailing = rawPath.substring(metaSeparator + 1);
            if (!trailing.isEmpty() && isDigitsOnly(trailing)) {
                rawPath = rawPath.substring(0, metaSeparator);
            }
        }
        String namespace = "";
        int namespaceSeparator = id.indexOf(':');
        if (namespaceSeparator >= 0) {
            namespace = id.substring(0, namespaceSeparator + 1)
                .toLowerCase(Locale.ROOT);
        }
        return namespace + rawPath.toLowerCase(Locale.ROOT);
    }

    private boolean isDigitsOnly(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (!Character.isDigit(value.charAt(index))) {
                return false;
            }
        }
        return !value.isEmpty();
    }

    public static class RankedPreviewSearchEntry {

        private final int score;
        private final int structuredSpecificity;
        private final int familySize;
        private final PreviewSearchEntry entry;
        private final int pathLength;

        public RankedPreviewSearchEntry(int score, int structuredSpecificity, int familySize,
            PreviewSearchEntry entry) {
            this.score = score;
            this.structuredSpecificity = structuredSpecificity;
            this.familySize = familySize;
            this.entry = entry;
            String id = entry.getId();
            int separator = id == null ? -1 : id.indexOf(':');
            String path = separator >= 0 ? id.substring(separator + 1) : id;
            this.pathLength = path == null ? Integer.MAX_VALUE : path.length();
        }

        public int getScore() {
            return score;
        }

        public PreviewSearchEntry getEntry() {
            return entry;
        }

        public int getStructuredSpecificity() {
            return structuredSpecificity;
        }

        public int getFamilySize() {
            return familySize;
        }

        public int getPathLength() {
            return pathLength;
        }
    }
}
