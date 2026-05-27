package com.hfstudio.guidenh.guide.scene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.integration.structurelib.StructureLibPreviewSelection;

public class StructureLibSceneCondition {

    @Nullable
    private final String structureName;
    @Nullable
    private final StructureLibValueCondition tierCondition;
    private final Map<String, StructureLibValueCondition> channelConditions;

    public StructureLibSceneCondition(@Nullable String structureName,
        @Nullable StructureLibValueCondition tierCondition,
        @Nullable Map<String, StructureLibValueCondition> channelConditions) {
        this.structureName = normalizeStructureName(structureName);
        this.tierCondition = tierCondition;
        this.channelConditions = immutableChannelConditions(channelConditions);
    }

    public static StructureLibSceneCondition parse(@Nullable String structureName, @Nullable String tierExpression,
        @Nullable String channelExpression) {
        StructureLibValueCondition parsedTier = StructureLibValueCondition.parse(tierExpression);
        Map<String, StructureLibValueCondition> parsedChannels = parseChannelConditions(channelExpression);
        String normalizedStructureName = normalizeStructureName(structureName);
        if (normalizedStructureName == null && parsedTier == null && parsedChannels.isEmpty()) {
            return null;
        }
        return new StructureLibSceneCondition(normalizedStructureName, parsedTier, parsedChannels);
    }

    @Nullable
    public String getStructureName() {
        return structureName;
    }

    @Nullable
    public StructureLibValueCondition getTierCondition() {
        return tierCondition;
    }

    public Map<String, StructureLibValueCondition> getChannelConditions() {
        return channelConditions;
    }

    public boolean hasAnyConstraint() {
        return structureName != null || tierCondition != null || !channelConditions.isEmpty();
    }

    public Map<String, Object> toSiteExportData() {
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        if (structureName != null) {
            data.put("structure", structureName);
        }
        if (tierCondition != null) {
            data.put("tier", tierCondition.toSiteExportData());
        }
        if (!channelConditions.isEmpty()) {
            LinkedHashMap<String, Object> serializedChannels = new LinkedHashMap<>(channelConditions.size());
            for (Map.Entry<String, StructureLibValueCondition> entry : channelConditions.entrySet()) {
                serializedChannels.put(
                    entry.getKey(),
                    entry.getValue()
                        .toSiteExportData());
            }
            data.put("channels", serializedChannels);
        }
        return data;
    }

    private static Map<String, StructureLibValueCondition> parseChannelConditions(@Nullable String expression) {
        if (expression == null || expression.trim()
            .isEmpty()) {
            return Collections.emptyMap();
        }
        LinkedHashMap<String, List<String>> clausesByChannel = new LinkedHashMap<>();
        String currentChannel = null;
        for (String rawToken : expression.split(",")) {
            String token = rawToken != null ? rawToken.trim() : "";
            if (token.isEmpty()) {
                continue;
            }
            int delimiter = token.indexOf(':');
            if (delimiter >= 0) {
                String channelId = StructureLibPreviewSelection.normalizeChannelId(token.substring(0, delimiter));
                if (channelId == null) {
                    throw new IllegalArgumentException("Channel condition is missing a channel id.");
                }
                String clause = token.substring(delimiter + 1)
                    .trim();
                if (clause.isEmpty()) {
                    throw new IllegalArgumentException("Channel condition for '" + channelId + "' is missing a value.");
                }
                currentChannel = channelId;
                clausesByChannel.computeIfAbsent(channelId, ignored -> new ArrayList<>())
                    .add(clause);
                continue;
            }
            if (currentChannel == null) {
                throw new IllegalArgumentException(
                    "Channel condition token '" + token + "' must follow a '<channel>:<value>' clause.");
            }
            clausesByChannel.computeIfAbsent(currentChannel, ignored -> new ArrayList<>())
                .add(token);
        }

        if (clausesByChannel.isEmpty()) {
            return Collections.emptyMap();
        }

        LinkedHashMap<String, StructureLibValueCondition> parsed = new LinkedHashMap<>(clausesByChannel.size());
        for (Map.Entry<String, List<String>> entry : clausesByChannel.entrySet()) {
            StructureLibValueCondition condition = StructureLibValueCondition.parse(String.join(",", entry.getValue()));
            if (condition != null) {
                parsed.put(entry.getKey(), condition);
            }
        }
        return parsed.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(parsed);
    }

    @Nullable
    public static String normalizeStructureName(@Nullable String structureName) {
        if (structureName == null) {
            return null;
        }
        String trimmed = structureName.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static Map<String, StructureLibValueCondition> immutableChannelConditions(
        @Nullable Map<String, StructureLibValueCondition> channelConditions) {
        if (channelConditions == null || channelConditions.isEmpty()) {
            return Collections.emptyMap();
        }
        LinkedHashMap<String, StructureLibValueCondition> copied = new LinkedHashMap<>(channelConditions.size());
        for (Map.Entry<String, StructureLibValueCondition> entry : channelConditions.entrySet()) {
            String channelId = StructureLibPreviewSelection.normalizeChannelId(entry.getKey());
            StructureLibValueCondition condition = entry.getValue();
            if (channelId != null && condition != null) {
                copied.put(channelId, condition);
            }
        }
        return copied.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(copied);
    }
}
