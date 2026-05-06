package com.hfstudio.guidenh.guide.scene.level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.tileentity.TileEntity;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.compat.gregtech.GregTechHelpers;
import com.hfstudio.guidenh.guide.scene.snapshot.ImportBlockContext;
import com.hfstudio.guidenh.guide.scene.snapshot.StructureImportPipeline;
import com.hfstudio.guidenh.guide.scene.support.GuideBlockDisplayResolver;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.guide.scene.support.GuideForgeMultipartSupport;
import com.hfstudio.guidenh.guide.scene.support.GuideGregTechTileSupport;

public class GuidebookPreviewBlockPlacer {

    public static final Logger LOG = LogManager.getLogger("GuideNH/ScenePreview");
    public static final String BYTE_ARRAY_WRAPPER_TAG = "__guidenh_byte_array_v1";
    public static final String GREGTECH_BLOCK_MACHINES_CLASS = "gregtech.common.blocks.BlockMachines";
    public static final String BARTWORKS_META_GENERATED_BLOCKS_CLASS = "bartworks.system.material.BWMetaGeneratedBlocks";
    public static final String BARTWORKS_META_GENERATED_TILE_CLASS = "bartworks.system.material.TileEntityMetaGeneratedBlock";
    public static final Set<String> KNOWN_GREGTECH_BYTE_ARRAY_KEYS = createKnownGregTechByteArrayKeys();
    private static final int MISSING_BASE_META = Integer.MIN_VALUE;
    private static final Map<Integer, Integer> GREGTECH_BASE_META_CACHE = new ConcurrentHashMap<>();

    private GuidebookPreviewBlockPlacer() {}

    public static void place(GuidebookLevel level, int x, int y, int z, Block block, int meta,
        @Nullable NBTTagCompound tileTag) {
        place(level, x, y, z, block, meta, tileTag, null, null);
    }

    public static void place(GuidebookLevel level, int x, int y, int z, Block block, int meta,
        @Nullable NBTTagCompound tileTag, @Nullable String explicitBlockId) {
        place(level, x, y, z, block, meta, tileTag, explicitBlockId, null);
    }

    /**
     * @param structureBlockCompound full SNBT {@code blocks[]} compound (not only TE {@code nbt}); optional sidecars
     *                                 (e.g. AE2 cable stream tag {@link GuideAe2CableSnbt#TAG_ROOT}) via {@link StructureImportPipeline}.
     */
    public static void place(GuidebookLevel level, int x, int y, int z, Block block, int meta,
        @Nullable NBTTagCompound tileTag, @Nullable String explicitBlockId,
        @Nullable NBTTagCompound structureBlockCompound) {
        NBTTagCompound previewTileTag = sanitizeGregTechInitTag(tileTag);
        PlacementData placementData = resolvePlacementData(block, meta, previewTileTag);
        logPlacementRequest(x, y, z, block, meta, previewTileTag, explicitBlockId, placementData);

        // Place the block before loading its tile so world-aware tile initialization sees the correct block/meta.
        level.setBlock(x, y, z, block, placementData.blockMeta, null);

        TileEntity tileEntity = null;
        NBTTagCompound tileSnapshot = null;
        if (previewTileTag != null || block.hasTileEntity(placementData.blockMeta)) {
            try {
                tileEntity = GuidebookTileEntityLoader
                    .load(level.getOrCreateFakeWorld(), block, placementData.blockMeta, x, y, z, previewTileTag);
            } catch (Throwable t) {
                GuideDebugLog.warn(LOG, "Preview tile entity load failed, falling back to block-only placement", t);
            }
        }
        if (tileEntity != null) {
            level.setTileEntity(x, y, z, tileEntity);
            logLoadedTile("loaded", x, y, z, tileEntity, placementData.metaTileId, previewTileTag);
            initializeGregTechMetaTile(tileEntity, placementData.metaTileId, previewTileTag);
            logLoadedTile("gregtech-init", x, y, z, tileEntity, placementData.metaTileId, previewTileTag);
            applyGregTechDefaultFacing(tileEntity, previewTileTag);
            applyBartWorksGeneratedBlockMeta(tileEntity, block, placementData.blockMeta);
            logLoadedTile("post-facing-meta", x, y, z, tileEntity, placementData.metaTileId, previewTileTag);
            TileEntity residentTile = preferPreparedTileEntity(
                tileEntity,
                resolveWorldResidentTile(level.getOrCreateFakeWorld(), x, y, z, tileEntity));
            level.setTileEntity(x, y, z, residentTile);
            residentTile = finalizeSpecialPreviewTile(level, x, y, z, residentTile);
            tileSnapshot = captureTileSnapshot(residentTile);
            logLoadedTile("resident", x, y, z, residentTile, placementData.metaTileId, previewTileTag);
        } else if (shouldLogPlacement(block, placementData)) {
            GuideGregTechTileSupport.logInfoOnce(
                "preview-place-missing-tile:" + x
                    + ":"
                    + y
                    + ":"
                    + z
                    + ":"
                    + GuideGregTechTileSupport.describeBlock(block),
                "Preview tile load produced no TileEntity at {} for block={} explicitId={} blockMeta={} metaTileId={} tileTag=[{}]",
                describePosition(x, y, z),
                GuideGregTechTileSupport.describeBlock(block),
                explicitBlockId,
                placementData.blockMeta,
                placementData.metaTileId,
                GuideGregTechTileSupport.describeTileTag(previewTileTag));
        }
        invokeOnBlockAdded(block, level.getOrCreateFakeWorld(), x, y, z);
        tileEntity = restoreTileAfterOnBlockAdded(
            level,
            x,
            y,
            z,
            block,
            placementData,
            tileEntity,
            tileSnapshot,
            previewTileTag);
        logLoadedTile("post-block-added", x, y, z, tileEntity, placementData.metaTileId, previewTileTag);
        level.setExplicitBlockId(x, y, z, explicitBlockId);
        StructureImportPipeline.apply(new ImportBlockContext(level, x, y, z, structureBlockCompound));
    }

    public static PlacementData resolvePlacementData(Block block, int requestedMeta, @Nullable NBTTagCompound tileTag) {
        Integer metaTileId = resolveGregTechMetaTileId(block, requestedMeta, tileTag);
        if (metaTileId == null) {
            Integer bartWorksMeta = resolveBartWorksBlockMeta(block, requestedMeta, tileTag);
            return new PlacementData(bartWorksMeta != null ? bartWorksMeta : requestedMeta, null);
        }

        Integer blockMeta = resolveGregTechBaseMeta(metaTileId);
        if (blockMeta == null) {
            return new PlacementData(requestedMeta, null);
        }

        return new PlacementData(blockMeta, metaTileId);
    }

    @Nullable
    public static Integer resolveGregTechMetaTileId(Block block, int requestedMeta, @Nullable NBTTagCompound tileTag) {
        if (!GuideBlockDisplayResolver.isBlockInstanceOf(block, GREGTECH_BLOCK_MACHINES_CLASS)) {
            return null;
        }
        if (tileTag != null && tileTag.hasKey("mID")) {
            int tagMetaTileId = tileTag.getInteger("mID");
            return tagMetaTileId > 0 ? tagMetaTileId : null;
        }
        return requestedMeta > 15 ? requestedMeta : null;
    }

    @Nullable
    public static Integer resolveBartWorksBlockMeta(Block block, int requestedMeta, @Nullable NBTTagCompound tileTag) {
        if (!GuideBlockDisplayResolver.isBlockInstanceOf(block, BARTWORKS_META_GENERATED_BLOCKS_CLASS)) {
            return null;
        }
        if (tileTag != null && tileTag.hasKey("m")) {
            int metaFromTag = tileTag.getShort("m");
            if (metaFromTag <= 0) {
                metaFromTag = tileTag.getInteger("m");
            }
            if (metaFromTag > 0) {
                return metaFromTag;
            }
        }
        return Math.max(0, requestedMeta);
    }

    @Nullable
    public static Integer resolveGregTechBaseMeta(int metaTileId) {
        if (metaTileId <= 0) {
            return null;
        }
        Integer cached = GREGTECH_BASE_META_CACHE.get(metaTileId);
        if (cached != null) {
            return cached.intValue() == MISSING_BASE_META ? null : cached;
        }
        Integer resolved = resolveGregTechBaseMetaUncached(metaTileId);
        GREGTECH_BASE_META_CACHE.put(metaTileId, resolved != null ? resolved : MISSING_BASE_META);
        return resolved;
    }

    @Nullable
    private static Integer resolveGregTechBaseMetaUncached(int metaTileId) {
        try {
            return GregTechHelpers.getMetaTileBaseType(metaTileId);
        } catch (Throwable t) {
            GuideDebugLog.warn(LOG, "Failed to resolve GregTech base meta for preview block {}", metaTileId, t);
            return null;
        }
    }

    public static void initializeGregTechMetaTile(@Nullable TileEntity tileEntity, @Nullable Integer metaTileId,
        @Nullable NBTTagCompound tileTag) {
        if (tileEntity == null || metaTileId == null || metaTileId <= 0) {
            return;
        }
        try {
            NBTTagCompound initTag = sanitizeGregTechInitTag(tileTag);
            if (initTag != null && (!initTag.hasKey("mID") || initTag.getInteger("mID") != metaTileId)) {
                initTag = (NBTTagCompound) initTag.copy();
                initTag.setInteger("mID", metaTileId);
            }
            GregTechHelpers.initializeMetaTile(tileEntity, metaTileId, initTag);
        } catch (Throwable t) {
            GuideGregTechTileSupport.logInfoOnce(
                "preview-gregtech-init-bytearray-shapes:" + metaTileId
                    + ":"
                    + describeKnownGregTechByteArrayKeys(tileTag),
                "Preview GregTech init byte-array key shapes for metaTileId={} are [{}]",
                metaTileId,
                describeKnownGregTechByteArrayKeys(tileTag));
            GuideDebugLog.warn(LOG, "Failed to initialize GregTech preview tile {}", metaTileId, t);
        }
    }

    public static void applyGregTechDefaultFacing(@Nullable TileEntity tileEntity, @Nullable NBTTagCompound tileTag) {
        GregTechHelpers.applyDefaultFacing(tileEntity, tileTag);
    }

    public static void applyBartWorksGeneratedBlockMeta(@Nullable TileEntity tileEntity, Block block, int blockMeta) {
        if (tileEntity == null || blockMeta <= 0
            || !GuideBlockDisplayResolver.isBlockInstanceOf(block, BARTWORKS_META_GENERATED_BLOCKS_CLASS)
            || !isInstanceOf(tileEntity, BARTWORKS_META_GENERATED_TILE_CLASS)) {
            return;
        }
        try {
            tileEntity.getClass()
                .getField("mMetaData")
                .setShort(tileEntity, (short) blockMeta);
        } catch (Throwable t) {
            GuideDebugLog.warn(LOG, "Failed to apply BartWorks preview meta {}", blockMeta, t);
        }
    }

    public static void invokeOnBlockAdded(Block block, GuidebookFakeWorld world, int x, int y, int z) {
        if (block == null || world == null) {
            return;
        }
        try {
            block.onBlockAdded(world, x, y, z);
        } catch (Throwable t) {
            GuideDebugLog.warn(LOG, "Preview block onBlockAdded hook failed for {}", block, t);
        }
    }

    @Nullable
    static NBTTagCompound sanitizeGregTechInitTag(@Nullable NBTTagCompound tileTag) {
        if (tileTag == null) {
            return tileTag;
        }

        NBTTagCompound sanitized = normalizeGregTechByteArrays(tileTag);
        ArrayList<String> unresolvedWrappers = new ArrayList<>();
        collectUnresolvedByteArrayWrappers(sanitized, "", unresolvedWrappers);
        if (!unresolvedWrappers.isEmpty()) {
            GuideGregTechTileSupport.logInfoOnce(
                "preview-gregtech-bytearray-wrapper:" + unresolvedWrappers
                    + ":"
                    + GuideGregTechTileSupport.describeTileTag(tileTag),
                "Preview GregTech init tag still contains unresolved byte-array wrappers at {} for tileTag=[{}]",
                unresolvedWrappers,
                GuideGregTechTileSupport.describeTileTag(tileTag));
        }
        if (!sanitized.hasKey("mRedstoneSided")) {
            return sanitized;
        }

        NBTBase tag = sanitized.getTag("mRedstoneSided");
        if (tag instanceof NBTTagByteArray) {
            return sanitized;
        }

        byte[] normalized = tryDecodeLegacyByteArray(tag, true);
        if (normalized == null) {
            normalized = new byte[6];
        }

        sanitized.setByteArray("mRedstoneSided", normalized);
        return sanitized;
    }

    public static NBTTagCompound normalizeGregTechByteArrays(NBTTagCompound tileTag) {
        NBTTagCompound normalized = new NBTTagCompound();
        ArrayList<String> keys = new ArrayList<>(tileTag.func_150296_c());
        for (String key : keys) {
            NBTBase value = tileTag.getTag(key);
            if (value != null) {
                byte[] knownByteArray = decodeKnownGregTechByteArray(key, value);
                if (knownByteArray != null) {
                    normalized.setByteArray(key, knownByteArray);
                    continue;
                }
                normalized.setTag(key, normalizeGregTechTag(value));
            }
        }
        return normalized;
    }

    public static NBTBase normalizeGregTechTag(NBTBase tag) {
        if (tag instanceof NBTTagCompound compound) {
            byte[] decoded = decodeWrappedByteArray(compound);
            if (decoded != null) {
                return new NBTTagByteArray(decoded);
            }
            return normalizeGregTechByteArrays(compound);
        }
        if (tag instanceof NBTTagList list) {
            byte[] decoded = tryDecodeLegacyByteArray(list, false);
            if (decoded != null) {
                return new NBTTagByteArray(decoded);
            }
            return normalizeGregTechList(list);
        }
        return tag.copy();
    }

    public static NBTTagList normalizeGregTechList(NBTTagList list) {
        NBTTagList normalized = new NBTTagList();
        NBTTagList remaining = (NBTTagList) list.copy();
        int count = remaining.tagCount();
        for (int index = 0; index < count; index++) {
            normalized.appendTag(normalizeGregTechTag(remaining.removeTag(0)));
        }
        return normalized;
    }

    public static boolean isEncodedByteArrayWrapper(NBTTagCompound compound) {
        return compound.func_150296_c()
            .size() == 1 && compound.hasKey(BYTE_ARRAY_WRAPPER_TAG);
    }

    @Nullable
    public static byte[] decodeWrappedByteArray(NBTTagCompound compound) {
        if (!isEncodedByteArrayWrapper(compound)) {
            return null;
        }
        return tryDecodeLegacyByteArray(compound.getTag(BYTE_ARRAY_WRAPPER_TAG), true);
    }

    public static byte[] toByteArray(int[] ints) {
        byte[] bytes = new byte[ints.length];
        for (int index = 0; index < ints.length; index++) {
            bytes[index] = (byte) ints[index];
        }
        return bytes;
    }

    @Nullable
    public static byte[] tryDecodeLegacyByteArray(@Nullable NBTBase tag, boolean allowEmptyList) {
        if (tag instanceof NBTTagByteArray byteArray) {
            return byteArray.func_150292_c();
        }
        if (tag instanceof NBTTagIntArray intArray) {
            int[] source = intArray.func_150302_c();
            byte[] decoded = new byte[source.length];
            for (int index = 0; index < source.length; index++) {
                decoded[index] = (byte) source[index];
            }
            return decoded;
        }
        if (tag instanceof NBTTagString stringTag) {
            return parseByteArrayLiteral(stringTag.func_150285_a_());
        }
        if (tag instanceof NBTTagList list) {
            byte[] numeric = tryDecodeNumericByteList(list, allowEmptyList);
            if (numeric != null) {
                return numeric;
            }
            if (list.tagCount() == 1) {
                Integer legacyLength = parseLegacyByteArrayLength(list.getStringTagAt(0));
                if (legacyLength != null) {
                    return new byte[legacyLength];
                }
            }
        }
        return null;
    }

    @Nullable
    public static byte[] tryDecodeNumericByteList(NBTTagList list, boolean allowEmptyList) {
        if (list.tagCount() <= 0) {
            return allowEmptyList ? new byte[0] : null;
        }
        byte[] decoded = new byte[list.tagCount()];
        for (int index = 0; index < list.tagCount(); index++) {
            String value = list.getStringTagAt(index);
            try {
                decoded[index] = Byte.parseByte(trimNumericSuffix(value));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return decoded;
    }

    @Nullable
    public static byte[] parseByteArrayLiteral(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return null;
        }
        String content = trimmed.substring(1, trimmed.length() - 1)
            .trim();
        if (content.isEmpty()) {
            return new byte[0];
        }

        String[] parts = content.split(",");
        ArrayList<Byte> decoded = new ArrayList<>(parts.length);
        for (String part : parts) {
            String numeric = trimNumericSuffix(part);
            if (numeric.isEmpty()) {
                continue;
            }
            try {
                decoded.add((byte) Integer.parseInt(numeric));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        byte[] result = new byte[decoded.size()];
        for (int index = 0; index < decoded.size(); index++) {
            result[index] = decoded.get(index);
        }
        return result;
    }

    @Nullable
    public static Integer parseLegacyByteArrayLength(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (!trimmed.endsWith("bytes")) {
            return null;
        }
        int space = trimmed.indexOf(' ');
        if (space <= 0) {
            return null;
        }
        try {
            return Math.max(0, Integer.parseInt(trimmed.substring(0, space)));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static String trimNumericSuffix(String value) {
        String trimmed = value != null ? value.trim() : "";
        if (trimmed.endsWith("b") || trimmed.endsWith("B")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    public static void collectUnresolvedByteArrayWrappers(NBTTagCompound tag, String path, ArrayList<String> issues) {
        ArrayList<String> keys = new ArrayList<>(tag.func_150296_c());
        for (String key : keys) {
            String nextPath = path.isEmpty() ? key : path + "." + key;
            NBTBase value = tag.getTag(key);
            if (value instanceof NBTTagCompound compound) {
                if (isEncodedByteArrayWrapper(compound)) {
                    NBTBase wrappedValue = compound.getTag(BYTE_ARRAY_WRAPPER_TAG);
                    if (!(wrappedValue instanceof NBTTagByteArray)) {
                        issues.add(
                            nextPath + "("
                                + (wrappedValue != null ? wrappedValue.getClass()
                                    .getSimpleName() : "null")
                                + ")");
                    }
                }
                collectUnresolvedByteArrayWrappers(compound, nextPath, issues);
            } else if (value instanceof NBTTagList list) {
                collectUnresolvedByteArrayWrappers(list, nextPath, issues);
            }
        }
    }

    public static void collectUnresolvedByteArrayWrappers(NBTTagList list, String path, ArrayList<String> issues) {
        NBTTagList remaining = (NBTTagList) list.copy();
        int count = remaining.tagCount();
        for (int index = 0; index < count; index++) {
            NBTBase entry = remaining.removeTag(0);
            String nextPath = path + "[" + index + "]";
            if (entry instanceof NBTTagCompound compound) {
                if (isEncodedByteArrayWrapper(compound)) {
                    NBTBase wrappedValue = compound.getTag(BYTE_ARRAY_WRAPPER_TAG);
                    if (!(wrappedValue instanceof NBTTagByteArray)) {
                        issues.add(
                            nextPath + "("
                                + (wrappedValue != null ? wrappedValue.getClass()
                                    .getSimpleName() : "null")
                                + ")");
                    }
                }
                collectUnresolvedByteArrayWrappers(compound, nextPath, issues);
            } else if (entry instanceof NBTTagList nestedList) {
                collectUnresolvedByteArrayWrappers(nestedList, nextPath, issues);
            }
        }
    }

    @Nullable
    public static byte[] decodeKnownGregTechByteArray(String key, NBTBase tag) {
        if (!KNOWN_GREGTECH_BYTE_ARRAY_KEYS.contains(key)) {
            return null;
        }
        if (tag instanceof NBTTagCompound compound) {
            byte[] wrapped = decodeWrappedByteArray(compound);
            if (wrapped != null) {
                return wrapped;
            }
        }
        return tryDecodeLegacyByteArray(tag, true);
    }

    public static String describeKnownGregTechByteArrayKeys(@Nullable NBTTagCompound tileTag) {
        if (tileTag == null) {
            return "null-tag";
        }
        StringBuilder builder = new StringBuilder();
        for (String key : KNOWN_GREGTECH_BYTE_ARRAY_KEYS) {
            if (!tileTag.hasKey(key)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(", ");
            }
            NBTBase value = tileTag.getTag(key);
            builder.append(key)
                .append('=')
                .append(
                    value != null ? value.getClass()
                        .getSimpleName() : "null");
        }
        return builder.length() > 0 ? builder.toString() : "no-known-byte-array-keys";
    }

    public static Set<String> createKnownGregTechByteArrayKeys() {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        keys.add("mRedstoneSided");
        keys.add("eyeOfHarmonyOutputoutputEU_BigInt");
        keys.add("eyeOfHarmonyOutputusedEU");
        keys.add("powerTally");
        return Collections.unmodifiableSet(keys);
    }

    @Nullable
    public static TileEntity restoreTileAfterOnBlockAdded(GuidebookLevel level, int x, int y, int z, Block block,
        PlacementData placementData, @Nullable TileEntity preparedTileEntity, @Nullable NBTTagCompound tileSnapshot,
        @Nullable NBTTagCompound tileTag) {
        if (preparedTileEntity == null) {
            return null;
        }

        GuidebookFakeWorld world = level.getOrCreateFakeWorld();
        TileEntity residentTile = resolveWorldResidentTile(world, x, y, z, preparedTileEntity);
        if (residentTile == preparedTileEntity) {
            return preparedTileEntity;
        }

        NBTTagCompound restoreTag = tileSnapshot != null ? tileSnapshot : tileTag;
        TileEntity restoredTile = residentTile;
        if (restoredTile == null || (restoreTag != null && !applyTileSnapshot(restoredTile, restoreTag, x, y, z))) {
            restoredTile = preparedTileEntity;
            if (restoreTag != null) {
                applyTileSnapshot(restoredTile, restoreTag, x, y, z);
            }
        }

        initializeGregTechMetaTile(restoredTile, placementData.metaTileId, restoreTag);
        applyGregTechDefaultFacing(restoredTile, restoreTag);
        applyBartWorksGeneratedBlockMeta(restoredTile, block, placementData.blockMeta);
        level.setTileEntity(x, y, z, restoredTile);
        restoredTile = finalizeSpecialPreviewTile(level, x, y, z, restoredTile);
        return resolveWorldResidentTile(world, x, y, z, restoredTile);
    }

    @Nullable
    public static TileEntity finalizeSpecialPreviewTile(GuidebookLevel level, int x, int y, int z,
        @Nullable TileEntity tileEntity) {
        TileEntity finalizedTile = GuideForgeMultipartSupport.finalizePreviewTile(tileEntity);
        if (finalizedTile != null && finalizedTile != tileEntity) {
            level.setTileEntity(x, y, z, finalizedTile);
        }
        return finalizedTile;
    }

    @Nullable
    public static NBTTagCompound captureTileSnapshot(@Nullable TileEntity tileEntity) {
        if (tileEntity == null) {
            return null;
        }
        try {
            NBTTagCompound snapshot = new NBTTagCompound();
            tileEntity.writeToNBT(snapshot);
            return snapshot;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean applyTileSnapshot(@Nullable TileEntity tileEntity, @Nullable NBTTagCompound snapshot, int x,
        int y, int z) {
        if (tileEntity == null || snapshot == null) {
            return false;
        }
        try {
            tileEntity.readFromNBT(withWorldPosition(snapshot, x, y, z));
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isInstanceOf(@Nullable Object instance, String className) {
        if (instance == null || className == null || className.isEmpty()) {
            return false;
        }
        for (Class<?> type = instance.getClass(); type != null; type = type.getSuperclass()) {
            if (className.equals(type.getName())) {
                return true;
            }
        }
        return false;
    }

    public static TileEntity resolveWorldResidentTile(GuidebookFakeWorld world, int x, int y, int z,
        TileEntity fallback) {
        TileEntity resident = world.getTileEntity(x, y, z);
        return resident != null ? resident : fallback;
    }

    public static TileEntity preferPreparedTileEntity(TileEntity preparedTile, @Nullable TileEntity residentTile) {
        if (residentTile == null || residentTile == preparedTile) {
            return preparedTile;
        }
        return residentTile.getClass() == preparedTile.getClass() ? preparedTile : residentTile;
    }

    public static NBTTagCompound withWorldPosition(NBTTagCompound snapshot, int x, int y, int z) {
        NBTTagCompound copy = (NBTTagCompound) snapshot.copy();
        copy.setInteger("x", x);
        copy.setInteger("y", y);
        copy.setInteger("z", z);
        return copy;
    }

    public static void logPlacementRequest(int x, int y, int z, Block block, int requestedMeta,
        @Nullable NBTTagCompound tileTag, @Nullable String explicitBlockId, PlacementData placementData) {
        if (!shouldLogPlacement(block, placementData)) {
            return;
        }
        GuideGregTechTileSupport.logInfoOnce(
            "preview-place-request:" + x + ":" + y + ":" + z + ":" + GuideGregTechTileSupport.describeBlock(block),
            "Preview place request {}: block={} explicitId={} requestedMeta={} resolvedBlockMeta={} resolvedMetaTileId={} tileTag=[{}]",
            describePosition(x, y, z),
            GuideGregTechTileSupport.describeBlock(block),
            explicitBlockId,
            requestedMeta,
            placementData.blockMeta,
            placementData.metaTileId,
            GuideGregTechTileSupport.describeTileTag(tileTag));
    }

    public static void logLoadedTile(String stage, int x, int y, int z, @Nullable TileEntity tileEntity,
        @Nullable Integer metaTileId, @Nullable NBTTagCompound tileTag) {
        if (!GuideGregTechTileSupport.isGregTechTileEntity(tileEntity)
            && !isInstanceOf(tileEntity, BARTWORKS_META_GENERATED_TILE_CLASS)) {
            return;
        }
        GuideGregTechTileSupport.logInfoOnce(
            "preview-place-" + stage
                + ":"
                + x
                + ":"
                + y
                + ":"
                + z
                + ":"
                + (tileEntity != null ? tileEntity.getClass()
                    .getName() : "null"),
            "Preview tile {} {}: tile={} metaTileId={} tileTag=[{}]",
            stage,
            describePosition(x, y, z),
            GuideGregTechTileSupport.describeTile(tileEntity),
            metaTileId,
            GuideGregTechTileSupport.describeTileTag(tileTag));
    }

    public static boolean shouldLogPlacement(Block block, PlacementData placementData) {
        return placementData.metaTileId != null
            || GuideBlockDisplayResolver.isBlockInstanceOf(block, BARTWORKS_META_GENERATED_BLOCKS_CLASS);
    }

    public static String describePosition(int x, int y, int z) {
        return "(" + x + "," + y + "," + z + ")";
    }

    public static class PlacementData {

        private final int blockMeta;
        @Nullable
        private final Integer metaTileId;

        private PlacementData(int blockMeta, @Nullable Integer metaTileId) {
            this.blockMeta = blockMeta;
            this.metaTileId = metaTileId;
        }
    }
}
