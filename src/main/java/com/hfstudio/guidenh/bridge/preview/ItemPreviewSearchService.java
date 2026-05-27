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
            rankedEntries.add(
                new RankedPreviewSearchEntry(
                    score,
                    new PreviewSearchEntry(id, label, detail, buildPreviewKey(id), describeMatchKind(score))));
        }

        rankedEntries.sort(
            Comparator.comparingInt(RankedPreviewSearchEntry::getScore)
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
        String compactId = compact(normalizedId);
        String compactLabel = compact(normalizedLabel);
        String compactDetail = compact(normalizedDetail);
        String compactPath = compact(path);
        String compactPrefix = compact(prefix);
        String tokenInitials = createTokenInitials(path);
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
        if (!compactPrefix.isEmpty() && compactPrefix.length() >= 2 && labelInitials.startsWith(compactPrefix)) {
            return 9;
        }
        if (!compactPrefix.isEmpty() && compactPrefix.length() >= 2 && tokenInitials.startsWith(compactPrefix)) {
            return 10;
        }
        if (!compactPrefix.isEmpty() && compactId.startsWith(compactPrefix)) {
            return 11;
        }
        if (!compactPrefix.isEmpty() && compactLabel.startsWith(compactPrefix)) {
            return 12;
        }
        if (!compactPrefix.isEmpty() && compactPath.startsWith(compactPrefix)) {
            return 13;
        }
        if (!compactPrefix.isEmpty() && compactDetail.startsWith(compactPrefix)) {
            return 14;
        }
        if (normalizedId.contains(prefix)) {
            return 15;
        }
        if (normalizedLabel.contains(prefix)) {
            return 16;
        }
        if (normalizedDetail.contains(prefix)) {
            return 17;
        }
        return Integer.MAX_VALUE;
    }

    private boolean isShortPrefix(String prefix) {
        return prefix.indexOf(':') < 0 && prefix.length() > 0 && prefix.length() <= 4;
    }

    private String describeMatchKind(int score) {
        switch (score) {
            case 0:
                return "label-exact";
            case 1:
                return "id-exact";
            case 2:
                return "namespace-prefix";
            case 3:
                return "label-prefix";
            case 4:
                return "label-token";
            case 5:
                return "id-prefix";
            case 6:
                return "path-prefix";
            case 7:
                return "path-token";
            case 8:
                return "detail-prefix";
            case 9:
                return "label-acronym";
            case 10:
                return "path-acronym";
            case 11:
                return "id-compact";
            case 12:
                return "label-compact";
            case 13:
                return "path-compact";
            case 14:
                return "detail-compact";
            case 15:
                return "id-contains";
            case 16:
                return "label-contains";
            case 17:
                return "detail-contains";
            default:
                return "runtime";
        }
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
        if (value == null || value.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(value.length());
        int length = value.length();
        int tokenStart = -1;
        for (int index = 0; index <= length; index++) {
            char current = index < length ? value.charAt(index) : 0;
            boolean tokenCharacter = index < length
                && ((current >= 'a' && current <= 'z') || (current >= '0' && current <= '9'));
            if (tokenCharacter && tokenStart < 0) {
                tokenStart = index;
                builder.append(current);
                continue;
            }
            if (!tokenCharacter) {
                tokenStart = -1;
            }
        }
        return builder.toString();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static class RankedPreviewSearchEntry {

        private final int score;
        private final PreviewSearchEntry entry;

        public RankedPreviewSearchEntry(int score, PreviewSearchEntry entry) {
            this.score = score;
            this.entry = entry;
        }

        public int getScore() {
            return score;
        }

        public PreviewSearchEntry getEntry() {
            return entry;
        }
    }
}
