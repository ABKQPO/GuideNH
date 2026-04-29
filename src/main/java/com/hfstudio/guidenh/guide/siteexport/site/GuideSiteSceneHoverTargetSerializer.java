package com.hfstudio.guidenh.guide.siteexport.site;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hfstudio.guidenh.guide.document.interaction.ContentTooltip;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.ItemTooltip;
import com.hfstudio.guidenh.guide.document.interaction.TextTooltip;
import com.hfstudio.guidenh.guide.internal.GuideScreen;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.structurelib.StructureLibSceneMetadata;
import com.hfstudio.guidenh.guide.scene.structurelib.StructureLibTooltipContentBuilder;
import com.hfstudio.guidenh.guide.scene.support.GuideBlockBoundsResolver;
import com.hfstudio.guidenh.guide.scene.support.GuideEntityDisplayResolver;

public final class GuideSiteSceneHoverTargetSerializer {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping()
        .serializeNulls()
        .create();
    private static final float DEFAULT_HOVER_THICKNESS = 1.0f / 128.0f;
    private static final String BLOCK_HOVER_COLOR = "rgba(255,255,255,0.92)";
    private static final String ENTITY_HOVER_COLOR = "rgba(255,255,255,0.92)";
    private static final String HATCH_HOVER_COLOR = "rgba(217,180,74,0.9)";

    private GuideSiteSceneHoverTargetSerializer() {}

    public static String serialize(LytGuidebookScene scene, GuideSiteTemplateRegistry templates,
        @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver) {
        if (scene == null) {
            return "[]";
        }

        GuidebookLevel level = scene.getLevel();
        if (level == null) {
            return "[]";
        }

        List<Map<String, Object>> targets = new ArrayList<Map<String, Object>>();
        Map<String, String> templateIdsByHtml = new LinkedHashMap<String, String>();
        Integer visibleLayerY = resolveVisibleLayerY(scene);
        StructureLibSceneMetadata structureLibMetadata = scene.getStructureLibSceneMetadata();
        Set<Long> hatchPositions = structureLibMetadata != null ? structureLibMetadata.getHatchTooltipPositions()
            : Collections.<Long>emptySet();
        Set<Long> exportedHatchPositions = new LinkedHashSet<Long>();

        for (int[] pos : level.getFilledBlocks()) {
            if (pos == null || pos.length < 3 || !isVisibleBlock(pos[1], visibleLayerY)) {
                continue;
            }

            int x = pos[0];
            int y = pos[1];
            int z = pos[2];
            long packedPos = StructureLibSceneMetadata.packBlockPos(x, y, z);
            if (hatchPositions.contains(packedPos)) {
                exportedHatchPositions.add(Long.valueOf(packedPos));
                targets.add(
                    buildBlockTarget(
                        "structurelib",
                        x,
                        y,
                        z,
                        AxisAlignedBB.getBoundingBox(x, y, z, x + 1d, y + 1d, z + 1d),
                        HATCH_HOVER_COLOR,
                        resolveSceneBlockTooltip(scene, x, y, z),
                        templates,
                        templateIdsByHtml,
                        currentPageId,
                        assetExporter,
                        itemIconResolver));
                continue;
            }

            targets.add(
                buildBlockTarget(
                    "block",
                    x,
                    y,
                    z,
                    resolveBlockBounds(level, x, y, z),
                    BLOCK_HOVER_COLOR,
                    resolveSceneBlockTooltip(scene, x, y, z),
                    templates,
                    templateIdsByHtml,
                    currentPageId,
                    assetExporter,
                    itemIconResolver));
        }

        if (structureLibMetadata != null) {
            for (StructureLibSceneMetadata.BlockTooltipEntry entry : structureLibMetadata.getHatchTooltipEntries()) {
                if (entry == null || !isVisibleBlock(entry.getY(), visibleLayerY)) {
                    continue;
                }
                long packedPos = StructureLibSceneMetadata.packBlockPos(entry.getX(), entry.getY(), entry.getZ());
                if (!exportedHatchPositions.add(Long.valueOf(packedPos))) {
                    continue;
                }
                targets.add(
                    buildBlockTarget(
                        "structurelib",
                        entry.getX(),
                        entry.getY(),
                        entry.getZ(),
                        AxisAlignedBB.getBoundingBox(
                            entry.getX(),
                            entry.getY(),
                            entry.getZ(),
                            entry.getX() + 1d,
                            entry.getY() + 1d,
                            entry.getZ() + 1d),
                        HATCH_HOVER_COLOR,
                        resolveSceneBlockTooltip(scene, entry.getX(), entry.getY(), entry.getZ()),
                        templates,
                        templateIdsByHtml,
                        currentPageId,
                        assetExporter,
                        itemIconResolver));
            }
        }

        for (net.minecraft.entity.Entity entity : level.getEntities()) {
            if (entity == null || entity.boundingBox == null || !isVisibleEntity(entity.boundingBox, visibleLayerY)) {
                continue;
            }

            targets.add(
                buildEntityTarget(
                    entity.boundingBox,
                    resolveEntityTooltip(entity),
                    templates,
                    templateIdsByHtml,
                    currentPageId,
                    assetExporter,
                    itemIconResolver));
        }

        return GSON.toJson(targets);
    }

    private static Map<String, Object> buildBlockTarget(String targetType, int x, int y, int z, AxisAlignedBB bounds,
        String color, @Nullable GuideTooltip tooltip, GuideSiteTemplateRegistry templates,
        Map<String, String> templateIdsByHtml, @Nullable ResourceLocation currentPageId,
        @Nullable GuideSitePageAssetExporter assetExporter, GuideSiteItemIconResolver itemIconResolver) {
        Map<String, Object> target = createBaseTarget(targetType, bounds, color);
        target.put("blockPos", new int[] { x, y, z });
        String templateId = createTemplateId(
            tooltip,
            templates,
            templateIdsByHtml,
            currentPageId,
            assetExporter,
            itemIconResolver);
        if (templateId != null) {
            target.put("contentTemplateId", templateId);
        }
        return target;
    }

    private static Map<String, Object> buildEntityTarget(AxisAlignedBB bounds, @Nullable GuideTooltip tooltip,
        GuideSiteTemplateRegistry templates, Map<String, String> templateIdsByHtml,
        @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver) {
        Map<String, Object> target = createBaseTarget("entity", bounds, ENTITY_HOVER_COLOR);
        String templateId = createTemplateId(
            tooltip,
            templates,
            templateIdsByHtml,
            currentPageId,
            assetExporter,
            itemIconResolver);
        if (templateId != null) {
            target.put("contentTemplateId", templateId);
        }
        return target;
    }

    private static Map<String, Object> createBaseTarget(String targetType, AxisAlignedBB bounds, String color) {
        AxisAlignedBB normalizedBounds = normalizeBounds(bounds);
        Map<String, Object> target = new LinkedHashMap<String, Object>();
        target.put("type", "box");
        target.put("targetType", targetType);
        target.put(
            "minCorner",
            new float[] { (float) normalizedBounds.minX, (float) normalizedBounds.minY,
                (float) normalizedBounds.minZ });
        target.put(
            "maxCorner",
            new float[] { (float) normalizedBounds.maxX, (float) normalizedBounds.maxY,
                (float) normalizedBounds.maxZ });
        target.put("color", color);
        target.put("thickness", Float.valueOf(DEFAULT_HOVER_THICKNESS));
        target.put("alwaysOnTop", Boolean.TRUE);
        return target;
    }

    @Nullable
    private static GuideTooltip resolveSceneBlockTooltip(LytGuidebookScene scene, int x, int y, int z) {
        String name = GuideScreen.blockDisplayName(scene, x, y, z);
        GuideTooltip structureLibTooltip = resolveStructureLibTooltip(scene, x, y, z, name);
        ItemStack stack = GuideScreen.blockDisplayStack(scene, x, y, z);
        if (stack != null && stack.stackSize > 0) {
            return new ItemTooltip(stack.copy());
        }
        if (structureLibTooltip != null) {
            return structureLibTooltip;
        }
        return name != null && !name.trim()
            .isEmpty() ? new TextTooltip(name) : null;
    }

    @Nullable
    private static GuideTooltip resolveStructureLibTooltip(LytGuidebookScene scene, int x, int y, int z,
        @Nullable String blockName) {
        StructureLibSceneMetadata metadata = scene.getStructureLibSceneMetadata();
        if (metadata == null) {
            return null;
        }

        StructureLibSceneMetadata.BlockTooltipData tooltipData = metadata.getBlockTooltipData(x, y, z);
        if (tooltipData == null || !tooltipData.hasAdditionalTooltipContent()) {
            return null;
        }

        ContentTooltip tooltip = StructureLibTooltipContentBuilder.build(
            blockName != null && !blockName.trim()
                .isEmpty() ? blockName : "Block",
            tooltipData.getStructureLibDescription(),
            false,
            tooltipData.getBlockCandidates(),
            tooltipData.getHatchDescriptionLines(),
            tooltipData.getHatchCandidates());
        return tooltip;
    }

    @Nullable
    private static GuideTooltip resolveEntityTooltip(@Nullable net.minecraft.entity.Entity entity) {
        String name = GuideEntityDisplayResolver.resolveDisplayName(entity);
        return name != null && !name.trim()
            .isEmpty() ? new TextTooltip(name) : null;
    }

    private static AxisAlignedBB resolveBlockBounds(GuidebookLevel level, int x, int y, int z) {
        AxisAlignedBB bounds = GuideBlockBoundsResolver.resolveSelectedBounds(level, x, y, z);
        if (bounds == null || bounds.maxX <= bounds.minX || bounds.maxY <= bounds.minY || bounds.maxZ <= bounds.minZ) {
            return AxisAlignedBB.getBoundingBox(x, y, z, x + 1d, y + 1d, z + 1d);
        }
        return bounds;
    }

    private static AxisAlignedBB normalizeBounds(AxisAlignedBB bounds) {
        if (bounds == null) {
            return AxisAlignedBB.getBoundingBox(0d, 0d, 0d, 1d, 1d, 1d);
        }
        double minX = Math.min(bounds.minX, bounds.maxX);
        double minY = Math.min(bounds.minY, bounds.maxY);
        double minZ = Math.min(bounds.minZ, bounds.maxZ);
        double maxX = Math.max(bounds.minX, bounds.maxX);
        double maxY = Math.max(bounds.minY, bounds.maxY);
        double maxZ = Math.max(bounds.minZ, bounds.maxZ);
        if (maxX <= minX || maxY <= minY || maxZ <= minZ) {
            return AxisAlignedBB.getBoundingBox(minX, minY, minZ, minX + 1d, minY + 1d, minZ + 1d);
        }
        return AxisAlignedBB.getBoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Nullable
    private static Integer resolveVisibleLayerY(LytGuidebookScene scene) {
        int currentLayer = scene.getCurrentVisibleLayer();
        if (currentLayer <= 0) {
            return null;
        }
        return Integer.valueOf(
            scene.getLevel()
                .getBounds()[1] + currentLayer
                - 1);
    }

    private static boolean isVisibleBlock(int y, @Nullable Integer visibleLayerY) {
        return visibleLayerY == null || y == visibleLayerY.intValue();
    }

    private static boolean isVisibleEntity(AxisAlignedBB bounds, @Nullable Integer visibleLayerY) {
        return visibleLayerY == null
            || bounds.maxY > visibleLayerY.intValue() && bounds.minY < visibleLayerY.intValue() + 1.0D;
    }

    @Nullable
    private static String createTemplateId(@Nullable GuideTooltip tooltip, GuideSiteTemplateRegistry templates,
        Map<String, String> templateIdsByHtml, @Nullable ResourceLocation currentPageId,
        @Nullable GuideSitePageAssetExporter assetExporter, GuideSiteItemIconResolver itemIconResolver) {
        if (tooltip == null) {
            return null;
        }

        String html = GuideSiteSceneAnnotationSerializer
            .renderTooltipHtml(tooltip, currentPageId, assetExporter, itemIconResolver, templates);
        if (html == null || html.trim()
            .isEmpty()) {
            return null;
        }

        String existing = templateIdsByHtml.get(html);
        if (existing != null) {
            return existing;
        }

        String templateId = templates.create(html);
        templateIdsByHtml.put(html, templateId);
        return templateId;
    }
}
