package com.hfstudio.guidenh.guide.siteexport.site;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;

import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hfstudio.guidenh.guide.document.interaction.ContentTooltip;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.ItemTooltip;
import com.hfstudio.guidenh.guide.document.interaction.TextTooltip;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.StructureLibSceneBinding;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.support.GuideBlockBoundsResolver;
import com.hfstudio.guidenh.guide.scene.support.GuideBlockDisplayResolver;
import com.hfstudio.guidenh.guide.scene.support.GuideEntityDisplayResolver;
import com.hfstudio.guidenh.integration.structurelib.StructureLibSceneMetadata;
import com.hfstudio.guidenh.integration.structurelib.StructureLibTooltipContentBuilder;

public final class GuideSiteSceneHoverTargetSerializer {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping()
        .serializeNulls()
        .create();
    private static final float DEFAULT_HOVER_THICKNESS = 0.5f / 16.0f;
    private static final String BLOCK_HOVER_COLOR = "rgba(255,255,255,0.92)";
    private static final String ENTITY_HOVER_COLOR = "rgba(255,255,255,0.92)";
    private static final String HATCH_HOVER_COLOR = "rgba(217,180,74,0.9)";
    private static final double BOUNDS_EPSILON = 1.0e-4d;
    private static final int BLOCK_FACE_COUNT = 6;
    private static final int[][] RAY_SIDE_OFFSETS = { { 0, -1, 0 }, { 0, 1, 0 }, { 0, 0, -1 }, { 0, 0, 1 },
        { -1, 0, 0 }, { 1, 0, 0 } };

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

        level.prepareForPreview();

        List<Map<String, Object>> targets = new ArrayList<>();
        Map<String, String> templateIdsByHtml = new LinkedHashMap<>();
        Integer visibleLayerY = resolveVisibleLayerY(scene);
        List<StructureLibSceneMetadata> structureLibMetadataList = collectStructureLibMetadata(scene);
        Set<Long> hatchPositions = new LinkedHashSet<>();
        for (StructureLibSceneMetadata metadata : structureLibMetadataList) {
            hatchPositions.addAll(metadata.getHatchTooltipPositions());
        }
        Set<Long> exportedHatchPositions = new LinkedHashSet<>();

        for (int[] pos : level.getFilledBlocks()) {
            if (pos == null || pos.length < 3 || !isVisibleBlock(pos[1], visibleLayerY)) {
                continue;
            }

            int x = pos[0];
            int y = pos[1];
            int z = pos[2];
            long packedPos = StructureLibSceneMetadata.packBlockPos(x, y, z);
            if (hatchPositions.contains(packedPos)) {
                exportedHatchPositions.add(packedPos);
                targets.add(
                    buildBlockTarget(
                        "structurelib",
                        x,
                        y,
                        z,
                        AxisAlignedBB.getBoundingBox(x, y, z, x + 1d, y + 1d, z + 1d),
                        HATCH_HOVER_COLOR,
                        resolveSceneBlockTooltip(scene, structureLibMetadataList, x, y, z, null),
                        templates,
                        templateIdsByHtml,
                        currentPageId,
                        assetExporter,
                        itemIconResolver));
                continue;
            }

            targets.addAll(
                buildBlockTargets(
                    scene,
                    x,
                    y,
                    z,
                    structureLibMetadataList,
                    templates,
                    templateIdsByHtml,
                    currentPageId,
                    assetExporter,
                    itemIconResolver));
        }

        for (StructureLibSceneMetadata structureLibMetadata : structureLibMetadataList) {
            for (StructureLibSceneMetadata.BlockTooltipEntry entry : structureLibMetadata.getHatchTooltipEntries()) {
                if (entry == null || !isVisibleBlock(entry.getY(), visibleLayerY)) {
                    continue;
                }
                long packedPos = StructureLibSceneMetadata.packBlockPos(entry.getX(), entry.getY(), entry.getZ());
                if (!exportedHatchPositions.add(packedPos)) {
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
                        resolveSceneBlockTooltip(
                            scene,
                            structureLibMetadataList,
                            entry.getX(),
                            entry.getY(),
                            entry.getZ(),
                            null),
                        templates,
                        templateIdsByHtml,
                        currentPageId,
                        assetExporter,
                        itemIconResolver));
            }
        }

        for (Entity entity : level.getEntities()) {
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

    private static List<StructureLibSceneMetadata> collectStructureLibMetadata(LytGuidebookScene scene) {
        if (scene == null) {
            return Collections.emptyList();
        }
        List<StructureLibSceneMetadata> metadataList = new ArrayList<>();
        for (StructureLibSceneBinding binding : scene.getStructureLibBindings()) {
            if (binding != null && binding.getMetadata() != null) {
                metadataList.add(binding.getMetadata());
            }
        }
        if (!metadataList.isEmpty()) {
            return metadataList;
        }
        StructureLibSceneMetadata metadata = scene.getStructureLibSceneMetadata();
        return metadata != null ? Collections.singletonList(metadata) : Collections.emptyList();
    }

    private static List<Map<String, Object>> buildBlockTargets(LytGuidebookScene scene, int x, int y, int z,
        List<StructureLibSceneMetadata> structureLibMetadataList, GuideSiteTemplateRegistry templates,
        Map<String, String> templateIdsByHtml, @Nullable ResourceLocation currentPageId,
        @Nullable GuideSitePageAssetExporter assetExporter, GuideSiteItemIconResolver itemIconResolver) {
        BlockHoverGeometry geometry = resolveBlockHoverGeometry(scene.getLevel(), x, y, z);
        if (geometry == null || geometry.bounds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> targets = new ArrayList<>(geometry.bounds.size());
        for (AxisAlignedBB bounds : geometry.bounds) {
            MovingObjectPosition target = resolveTooltipTarget(scene.getLevel(), geometry, x, y, z, bounds);
            targets.add(
                buildBlockTarget(
                    "block",
                    x,
                    y,
                    z,
                    bounds,
                    BLOCK_HOVER_COLOR,
                    resolveSceneBlockTooltip(scene, structureLibMetadataList, x, y, z, target),
                    templates,
                    templateIdsByHtml,
                    currentPageId,
                    assetExporter,
                    itemIconResolver));
        }
        return targets;
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
        Map<String, Object> target = new LinkedHashMap<>();
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
        target.put("thickness", DEFAULT_HOVER_THICKNESS);
        target.put("alwaysOnTop", Boolean.TRUE);
        return target;
    }

    @Nullable
    private static GuideTooltip resolveSceneBlockTooltip(LytGuidebookScene scene,
        List<StructureLibSceneMetadata> structureLibMetadataList, int x, int y, int z,
        @Nullable MovingObjectPosition target) {
        ItemStack stack = resolveSceneBlockStack(scene, x, y, z, target);
        if (stack != null && stack.stackSize > 0) {
            return new ItemTooltip(stack.copy());
        }

        String name = resolveSceneBlockName(scene, x, y, z, target);
        GuideTooltip structureLibTooltip = resolveStructureLibTooltip(structureLibMetadataList, x, y, z, name);
        if (structureLibTooltip != null) {
            return structureLibTooltip;
        }
        return name != null && !name.trim()
            .isEmpty() ? new TextTooltip(name) : null;
    }

    @Nullable
    private static GuideTooltip resolveStructureLibTooltip(List<StructureLibSceneMetadata> structureLibMetadataList,
        int x, int y, int z, @Nullable String blockName) {
        for (StructureLibSceneMetadata metadata : structureLibMetadataList) {
            StructureLibSceneMetadata.BlockTooltipData tooltipData = metadata.getBlockTooltipData(x, y, z);
            if (tooltipData == null || !tooltipData.hasAdditionalTooltipContent()) {
                continue;
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
        return null;
    }

    @Nullable
    private static GuideTooltip resolveEntityTooltip(@Nullable Entity entity) {
        String name = GuideEntityDisplayResolver.resolveDisplayName(entity);
        return name != null && !name.trim()
            .isEmpty() ? new TextTooltip(name) : null;
    }

    @Nullable
    private static String resolveSceneBlockName(LytGuidebookScene scene, int x, int y, int z,
        @Nullable MovingObjectPosition target) {
        try {
            return target != null ? GuideBlockDisplayResolver.resolveDisplayName(scene.getLevel(), x, y, z, target)
                : GuideBlockDisplayResolver.resolveDisplayName(scene.getLevel(), x, y, z);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static ItemStack resolveSceneBlockStack(LytGuidebookScene scene, int x, int y, int z,
        @Nullable MovingObjectPosition target) {
        try {
            return target != null ? GuideBlockDisplayResolver.resolveDisplayStack(scene.getLevel(), x, y, z, target)
                : GuideBlockDisplayResolver.resolveDisplayStack(scene.getLevel(), x, y, z);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static BlockHoverGeometry resolveBlockHoverGeometry(GuidebookLevel level, int x, int y, int z) {
        Block block = level.getBlock(x, y, z);
        if (block == null || block == Blocks.air) {
            return null;
        }

        List<AxisAlignedBB> collisionBounds;
        try {
            collisionBounds = GuideBlockBoundsResolver.collectCollisionBounds(level, block, x, y, z);
        } catch (Throwable ignored) {
            collisionBounds = Collections.emptyList();
        }

        List<AxisAlignedBB> bounds = new ArrayList<>(collisionBounds.size());
        for (AxisAlignedBB collisionBoundsBox : collisionBounds) {
            AxisAlignedBB normalized = normalizeBounds(collisionBoundsBox);
            if (!GuideBlockBoundsResolver.isNonEmpty(normalized)) {
                continue;
            }
            appendUniqueBounds(bounds, normalized);
        }

        if (!bounds.isEmpty()) {
            AxisAlignedBB selectedBounds = GuideBlockBoundsResolver.resolveSelectedBounds(level, x, y, z);
            return new BlockHoverGeometry(
                block,
                bounds,
                collisionBounds,
                selectedBounds != null ? normalizeBounds(selectedBounds) : null);
        }

        AxisAlignedBB fallbackBounds = GuideBlockBoundsResolver.resolveSelectedBounds(level, x, y, z);
        if (fallbackBounds == null || !GuideBlockBoundsResolver.isNonEmpty(fallbackBounds)) {
            fallbackBounds = AxisAlignedBB.getBoundingBox(x, y, z, x + 1d, y + 1d, z + 1d);
        }
        AxisAlignedBB normalizedFallbackBounds = normalizeBounds(fallbackBounds);
        return new BlockHoverGeometry(
            block,
            Collections.singletonList(normalizedFallbackBounds),
            Collections.<AxisAlignedBB>emptyList(),
            normalizedFallbackBounds);
    }

    @Nullable
    private static MovingObjectPosition resolveTooltipTarget(GuidebookLevel level, BlockHoverGeometry geometry, int x,
        int y, int z, AxisAlignedBB bounds) {
        if (geometry.block == null || geometry.block == Blocks.air) {
            return null;
        }

        for (Integer side : resolveCandidateSides(bounds, x, y, z)) {
            MovingObjectPosition target = resolveTooltipTargetForSide(level, geometry, x, y, z, bounds, side);
            if (target != null) {
                return target;
            }
        }

        Integer inferredSide = inferPreferredSide(bounds, x, y, z);
        return inferredSide != null ? createSyntheticTarget(x, y, z, bounds, inferredSide) : null;
    }

    @Nullable
    private static MovingObjectPosition resolveTooltipTargetForSide(GuidebookLevel level, BlockHoverGeometry geometry,
        int x, int y, int z, AxisAlignedBB bounds, int side) {
        Vec3 hitPoint = faceCenter(bounds, side);
        int[] sideOffset = RAY_SIDE_OFFSETS[side];
        Vec3 rayStart = Vec3.createVectorHelper(
            hitPoint.xCoord + sideOffset[0] * 2.0d,
            hitPoint.yCoord + sideOffset[1] * 2.0d,
            hitPoint.zCoord + sideOffset[2] * 2.0d);
        Vec3 rayEnd = Vec3.createVectorHelper(
            hitPoint.xCoord - sideOffset[0] * 0.25d,
            hitPoint.yCoord - sideOffset[1] * 0.25d,
            hitPoint.zCoord - sideOffset[2] * 0.25d);

        AxisAlignedBB hitBounds = resolveNearestRayHitBounds(geometry.collisionBounds, rayStart, rayEnd);
        if (hitBounds == null) {
            hitBounds = geometry.selectedBounds;
        }
        if (!sameBounds(bounds, hitBounds)) {
            return null;
        }

        try {
            MovingObjectPosition hit = geometry.block
                .collisionRayTrace(level.getOrCreateFakeWorld(), x, y, z, rayStart, rayEnd);
            if (hit != null && hit.hitVec != null) {
                return new MovingObjectPosition(x, y, z, hit.sideHit, hit.hitVec);
            }
        } catch (Throwable ignored) {}

        return createSyntheticTarget(x, y, z, bounds, side);
    }

    private static List<Integer> resolveCandidateSides(AxisAlignedBB bounds, int x, int y, int z) {
        List<Integer> sides = new ArrayList<>(BLOCK_FACE_COUNT);
        if (touches(bounds.minY, y)) {
            sides.add(0);
        }
        if (touches(bounds.maxY, y + 1d)) {
            sides.add(1);
        }
        if (touches(bounds.minZ, z)) {
            sides.add(2);
        }
        if (touches(bounds.maxZ, z + 1d)) {
            sides.add(3);
        }
        if (touches(bounds.minX, x)) {
            sides.add(4);
        }
        if (touches(bounds.maxX, x + 1d)) {
            sides.add(5);
        }
        Integer inferredSide = inferPreferredSide(bounds, x, y, z);
        if (inferredSide != null && !sides.contains(inferredSide)) {
            sides.add(0, inferredSide);
        }
        return sides;
    }

    @Nullable
    private static Integer inferPreferredSide(AxisAlignedBB bounds, int x, int y, int z) {
        double[] distances = { Math.abs(bounds.minY - y), Math.abs(bounds.maxY - (y + 1d)), Math.abs(bounds.minZ - z),
            Math.abs(bounds.maxZ - (z + 1d)), Math.abs(bounds.minX - x), Math.abs(bounds.maxX - (x + 1d)) };
        Integer preferredSide = null;
        double preferredDistance = Double.POSITIVE_INFINITY;
        for (int side = 0; side < distances.length; side++) {
            double distance = distances[side];
            if (distance + BOUNDS_EPSILON < preferredDistance) {
                preferredDistance = distance;
                preferredSide = side;
            }
        }
        return preferredDistance <= 0.5d + BOUNDS_EPSILON ? preferredSide : null;
    }

    @Nullable
    private static AxisAlignedBB resolveNearestRayHitBounds(List<AxisAlignedBB> collisionBounds, Vec3 rayStart,
        Vec3 rayEnd) {
        if (collisionBounds == null || rayStart == null || rayEnd == null) {
            return null;
        }
        AxisAlignedBB bestBounds = null;
        double bestDistanceSq = Double.POSITIVE_INFINITY;
        for (AxisAlignedBB collisionBoundsBox : collisionBounds) {
            if (collisionBoundsBox == null || !GuideBlockBoundsResolver.isNonEmpty(collisionBoundsBox)) {
                continue;
            }
            MovingObjectPosition intercept = collisionBoundsBox.calculateIntercept(rayStart, rayEnd);
            if (intercept == null || intercept.hitVec == null) {
                continue;
            }
            double distanceSq = intercept.hitVec.squareDistanceTo(rayStart);
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                bestBounds = collisionBoundsBox;
            }
        }
        return bestBounds != null ? GuideBlockBoundsResolver.copyOf(bestBounds) : null;
    }

    private static boolean sameBounds(AxisAlignedBB expected, @Nullable AxisAlignedBB actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return Math.abs(expected.minX - actual.minX) <= BOUNDS_EPSILON
            && Math.abs(expected.minY - actual.minY) <= BOUNDS_EPSILON
            && Math.abs(expected.minZ - actual.minZ) <= BOUNDS_EPSILON
            && Math.abs(expected.maxX - actual.maxX) <= BOUNDS_EPSILON
            && Math.abs(expected.maxY - actual.maxY) <= BOUNDS_EPSILON
            && Math.abs(expected.maxZ - actual.maxZ) <= BOUNDS_EPSILON;
    }

    private static boolean touches(double actual, double expected) {
        return Math.abs(actual - expected) <= BOUNDS_EPSILON;
    }

    private static void appendUniqueBounds(List<AxisAlignedBB> bounds, AxisAlignedBB candidate) {
        for (AxisAlignedBB existing : bounds) {
            if (sameBounds(existing, candidate)) {
                return;
            }
        }
        bounds.add(candidate);
    }

    private static MovingObjectPosition createSyntheticTarget(int x, int y, int z, AxisAlignedBB bounds, int side) {
        return new MovingObjectPosition(x, y, z, side, faceCenter(bounds, side));
    }

    private static Vec3 faceCenter(AxisAlignedBB bounds, int side) {
        double centerX = (bounds.minX + bounds.maxX) * 0.5d;
        double centerY = (bounds.minY + bounds.maxY) * 0.5d;
        double centerZ = (bounds.minZ + bounds.maxZ) * 0.5d;
        return switch (side) {
            case 0 -> Vec3.createVectorHelper(centerX, bounds.minY, centerZ);
            case 1 -> Vec3.createVectorHelper(centerX, bounds.maxY, centerZ);
            case 2 -> Vec3.createVectorHelper(centerX, centerY, bounds.minZ);
            case 3 -> Vec3.createVectorHelper(centerX, centerY, bounds.maxZ);
            case 4 -> Vec3.createVectorHelper(bounds.minX, centerY, centerZ);
            case 5 -> Vec3.createVectorHelper(bounds.maxX, centerY, centerZ);
            default -> Vec3.createVectorHelper(centerX, centerY, centerZ);
        };
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
        return scene.getLevel()
            .getBounds()[1] + currentLayer
            - 1;
    }

    private static boolean isVisibleBlock(int y, @Nullable Integer visibleLayerY) {
        return visibleLayerY == null || y == visibleLayerY;
    }

    private static boolean isVisibleEntity(AxisAlignedBB bounds, @Nullable Integer visibleLayerY) {
        return visibleLayerY == null || bounds.maxY > visibleLayerY && bounds.minY < visibleLayerY + 1.0D;
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

    private static class BlockHoverGeometry {

        private final Block block;
        private final List<AxisAlignedBB> bounds;
        private final List<AxisAlignedBB> collisionBounds;
        private final AxisAlignedBB selectedBounds;

        private BlockHoverGeometry(Block block, List<AxisAlignedBB> bounds, List<AxisAlignedBB> collisionBounds,
            AxisAlignedBB selectedBounds) {
            this.block = block;
            this.bounds = bounds;
            this.collisionBounds = collisionBounds;
            this.selectedBounds = selectedBounds;
        }
    }
}
