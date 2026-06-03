package com.hfstudio.guidenh.bridge.semantic.providers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.tileentity.TileEntity;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.structurelib.alignment.IAlignment;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.hfstudio.guidenh.bridge.semantic.SemanticCapability;
import com.hfstudio.guidenh.bridge.semantic.SemanticProvider;
import com.hfstudio.guidenh.bridge.semantic.SemanticQuery;
import com.hfstudio.guidenh.bridge.semantic.SemanticQueryResult;
import com.hfstudio.guidenh.integration.structurelib.StructureLibImportRequest;
import com.hfstudio.guidenh.integration.structurelib.StructureLibRuntimeFacade;
import com.hfstudio.structurelibexport.StructureLibControllerDiscovery;
import com.hfstudio.structurelibexport.StructureLibControllerSpec;

public class StructureLibSemanticProvider implements SemanticProvider {

    @Override
    public String getCapability() {
        return SemanticCapability.STRUCTURELIB;
    }

    @Override
    public SemanticQueryResult query(SemanticQuery query) {
        List<Map<String, String>> entries = loadEntries(query);
        List<Map<String, String>> filteredEntries = filterEntries(entries, query.getPrefix());
        int cursor = parseCursor(query.getCursor(), filteredEntries.size());
        int limit = query.getLimit() > 0 ? query.getLimit() : filteredEntries.size();
        int end = Math.min(filteredEntries.size(), cursor + limit);
        String nextCursor = end < filteredEntries.size() ? Integer.toString(end) : null;
        return new SemanticQueryResult(
            SemanticCapability.STRUCTURELIB,
            computeVersion(entries),
            new ArrayList<>(filteredEntries.subList(cursor, end)),
            nextCursor);
    }

    private List<Map<String, String>> loadEntries(SemanticQuery query) {
        Map<String, String> filters = query.getFilters();
        String attribute = normalizeValue(filters.get("attribute"));
        if ("channel".equals(attribute)) {
            return loadChannelEntries(filters);
        }
        if ("piece".equals(attribute)) {
            return loadPieceEntries(filters);
        }
        if (isOrientationAttribute(attribute)) {
            return loadOrientationEntries(filters, attribute);
        }
        return loadControllerEntries();
    }

    private List<Map<String, String>> loadControllerEntries() {
        List<Map<String, String>> entries = new ArrayList<>();
        for (StructureLibControllerSpec controller : new StructureLibControllerDiscovery().discoverAllControllers()) {
            String id = normalizeValue(controller.getControllerArgument());
            if (id == null) {
                continue;
            }
            String label = normalizeValue(controller.getDisplayName());
            String detail = controller.getBlockId() + ":" + controller.getMeta();
            entries.add(createEntry(id, label, detail));
        }
        return normalizeEntries(entries);
    }

    private List<Map<String, String>> loadChannelEntries(Map<String, String> filters) {
        String controller = normalizeValue(filters.get("controller"));
        if (controller == null) {
            return List.of();
        }
        try {
            StructureLibImportRequest request = new StructureLibImportRequest(controller, null, null, null, null, null);
            StructureLibRuntimeFacade.ResolvedController resolvedController = StructureLibRuntimeFacade
                .resolveController(request);
            StructureLibRuntimeFacade.ControlAnalysis analysis = StructureLibRuntimeFacade
                .analyzeControls(request, resolvedController);
            int maxTier = analysis.getMaxTotalTier();
            if (maxTier <= 0) {
                return List.of();
            }

            List<Map<String, String>> entries = new ArrayList<>();
            String detail = describeTierRange(controller, analysis);
            for (int value = 1; value <= maxTier; value++) {
                entries.add(createEntry(Integer.toString(value), "StructureLib preview tier", detail));
            }
            return normalizeEntries(entries);
        } catch (Throwable ignored) {
            return List.of();
        }
    }

    private List<Map<String, String>> loadPieceEntries(Map<String, String> filters) {
        String controller = normalizeValue(filters.get("controller"));
        if (controller == null) {
            return List.of();
        }
        return List.of(createEntry("main", "Main structure", controller + " default constructable"));
    }

    private List<Map<String, String>> loadOrientationEntries(Map<String, String> filters, String attribute) {
        String controller = normalizeValue(filters.get("controller"));
        if (controller == null || attribute == null) {
            return List.of();
        }
        try {
            StructureLibControllerSpec controllerSpec = StructureLibControllerSpec.parse(controller);
            List<ExtendedFacing> allowedFacings = findAllowedFacings(controllerSpec);
            if (allowedFacings.isEmpty()) {
                return List.of();
            }
            List<Map<String, String>> entries = switch (attribute) {
                case "facing" -> createFacingEntries(controllerSpec, allowedFacings, filters);
                case "rotation" -> createRotationEntries(controllerSpec, allowedFacings, filters);
                case "flip" -> createFlipEntries(controllerSpec, allowedFacings, filters);
                default -> List.of();
            };
            return normalizeEntries(entries);
        } catch (Throwable ignored) {
            return List.of();
        }
    }

    private List<Map<String, String>> createFacingEntries(StructureLibControllerSpec controller,
        List<ExtendedFacing> allowedFacings, Map<String, String> filters) {
        List<Map<String, String>> entries = new ArrayList<>();
        for (ExtendedFacing facing : allowedFacings) {
            if (matchesOrientationFilters(facing, filters, "facing")) {
                String value = facing.getDirection()
                    .name()
                    .toLowerCase(Locale.ROOT);
                entries.add(createEntry(value, "StructureLib facing", describeOrientation(controller)));
            }
        }
        return entries;
    }

    private List<Map<String, String>> createRotationEntries(StructureLibControllerSpec controller,
        List<ExtendedFacing> allowedFacings, Map<String, String> filters) {
        List<Map<String, String>> entries = new ArrayList<>();
        for (ExtendedFacing facing : allowedFacings) {
            if (matchesOrientationFilters(facing, filters, "rotation")) {
                entries.add(
                    createEntry(
                        facing.getRotation()
                            .getName(),
                        "StructureLib rotation",
                        describeOrientation(controller)));
            }
        }
        return entries;
    }

    private List<Map<String, String>> createFlipEntries(StructureLibControllerSpec controller,
        List<ExtendedFacing> allowedFacings, Map<String, String> filters) {
        List<Map<String, String>> entries = new ArrayList<>();
        for (ExtendedFacing facing : allowedFacings) {
            if (matchesOrientationFilters(facing, filters, "flip")) {
                entries.add(
                    createEntry(
                        facing.getFlip()
                            .getName(),
                        "StructureLib flip",
                        describeOrientation(controller)));
            }
        }
        return entries;
    }

    private List<ExtendedFacing> findAllowedFacings(StructureLibControllerSpec controller) {
        List<ExtendedFacing> allowedFacings = new ArrayList<>();
        StructureLibRuntimeFacade.BuildContext context = new StructureLibRuntimeFacade.BuildContext();
        try {
            StructureLibRuntimeFacade.ResolvedController resolvedController = new StructureLibRuntimeFacade.ResolvedController(
                controller.getBlockId(),
                controller.getBlock(),
                controller.getMeta());
            TileEntity tile = StructureLibRuntimeFacade
                .placeControllerDirectly(context.getLevel(), context.getWorld(), resolvedController, new ArrayList<>());
            if (tile == null) {
                return List.of();
            }
            IAlignment alignment = StructureLibRuntimeFacade.resolveAlignment(tile);
            if (alignment == null) {
                return List.of();
            }
            for (ExtendedFacing facing : ExtendedFacing.VALUES) {
                if (alignment.getAlignmentLimits() != null ? alignment.getAlignmentLimits()
                    .isNewExtendedFacingValid(facing) : alignment.checkedSetExtendedFacing(facing)) {
                    allowedFacings.add(facing);
                }
            }
            return allowedFacings;
        } catch (Throwable ignored) {
            return List.of();
        } finally {
            context.clear();
        }
    }

    private boolean matchesOrientationFilters(ExtendedFacing facing, Map<String, String> filters,
        String targetAttribute) {
        return matchesOrientationFilterValue(
            facing.getDirection()
                .name()
                .toLowerCase(Locale.ROOT),
            normalizeValue(filters.get("facing")),
            targetAttribute,
            "facing")
            && matchesOrientationFilterValue(
                facing.getRotation()
                    .getName(),
                normalizeValue(filters.get("rotation")),
                targetAttribute,
                "rotation")
            && matchesOrientationFilterValue(
                facing.getFlip()
                    .getName(),
                normalizeValue(filters.get("flip")),
                targetAttribute,
                "flip");
    }

    private boolean matchesOrientationFilterValue(String actualValue, @Nullable String requestedValue,
        String targetAttribute, String attributeName) {
        if (targetAttribute.equals(attributeName) || requestedValue == null) {
            return true;
        }
        return actualValue.equalsIgnoreCase(requestedValue);
    }

    private String describeOrientation(StructureLibControllerSpec controller) {
        return "Allowed orientation for " + controller.getControllerArgument();
    }

    private boolean isOrientationAttribute(@Nullable String attribute) {
        return "facing".equals(attribute) || "rotation".equals(attribute) || "flip".equals(attribute);
    }

    private String describeTierRange(String controller, StructureLibRuntimeFacade.ControlAnalysis analysis) {
        StringBuilder detail = new StringBuilder();
        detail.append("Preview tier for ")
            .append(controller)
            .append(" (max ")
            .append(analysis.getMaxTotalTier())
            .append(')');
        if (analysis.getChannelMaxTierMap()
            .isEmpty()) {
            return detail.toString();
        }

        detail.append(" | Channel caps: ");
        boolean first = true;
        for (Map.Entry<String, Integer> entry : analysis.getChannelMaxTierMap()
            .entrySet()) {
            String channelId = normalizeValue(entry.getKey());
            Integer maxValue = entry.getValue();
            if (channelId == null || maxValue == null || maxValue <= 0) {
                continue;
            }
            if (!first) {
                detail.append(", ");
            }
            detail.append(channelId)
                .append('=')
                .append(maxValue);
            first = false;
        }
        return detail.toString();
    }

    private List<Map<String, String>> normalizeEntries(List<Map<String, String>> entries) {
        Map<String, Map<String, String>> deduplicated = new LinkedHashMap<>();
        for (Map<String, String> entry : entries) {
            if (entry == null) {
                continue;
            }
            String id = normalizeValue(entry.get("id"));
            if (id == null) {
                continue;
            }
            Map<String, String> normalized = new LinkedHashMap<>();
            normalized.put("id", id);
            String label = normalizeValue(entry.get("label"));
            if (label != null) {
                normalized.put("label", label);
            }
            String detail = normalizeValue(entry.get("detail"));
            if (detail != null) {
                normalized.put("detail", detail);
            }
            deduplicated.putIfAbsent(id.toLowerCase(Locale.ROOT), normalized);
        }
        List<Map<String, String>> normalizedEntries = new ArrayList<>(deduplicated.values());
        normalizedEntries.sort(
            (left, right) -> left.get("id")
                .compareToIgnoreCase(right.get("id")));
        return normalizedEntries;
    }

    private List<Map<String, String>> filterEntries(List<Map<String, String>> entries, String prefix) {
        String normalizedPrefix = prefix == null ? ""
            : prefix.trim()
                .toLowerCase(Locale.ROOT);
        if (normalizedPrefix.isEmpty()) {
            return entries;
        }

        List<Map<String, String>> filteredEntries = new ArrayList<>();
        for (Map<String, String> entry : entries) {
            if (startsWithIgnoreCase(entry.get("id"), normalizedPrefix)
                || startsWithIgnoreCase(entry.get("label"), normalizedPrefix)
                || startsWithIgnoreCase(entry.get("detail"), normalizedPrefix)) {
                filteredEntries.add(entry);
            }
        }
        return filteredEntries;
    }

    private int parseCursor(String cursor, int size) {
        if (cursor == null || cursor.isEmpty()) {
            return 0;
        }
        try {
            return Math.clamp(Integer.parseInt(cursor), 0, size);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private int computeVersion(List<Map<String, String>> entries) {
        int hash = entries.hashCode();
        if (hash == Integer.MIN_VALUE) {
            return Integer.MAX_VALUE;
        }
        return Math.abs(hash) + 1;
    }

    private boolean startsWithIgnoreCase(@Nullable String value, String prefix) {
        return value != null && value.toLowerCase(Locale.ROOT)
            .startsWith(prefix);
    }

    private @Nullable String normalizeValue(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Map<String, String> createEntry(String id, @Nullable String label, @Nullable String detail) {
        Map<String, String> entry = new LinkedHashMap<>();
        entry.put("id", id);
        if (label != null) {
            entry.put("label", label);
        }
        if (detail != null) {
            entry.put("detail", detail);
        }
        return entry;
    }
}
