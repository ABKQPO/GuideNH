package com.hfstudio.guidenh.guide.scene.cache;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.hfstudio.guidenh.guide.internal.scene.GuidebookPreviewPlayerPose;
import com.hfstudio.guidenh.guide.internal.structure.GuideTextNbtCodec;
import com.hfstudio.guidenh.guide.scene.element.GuidebookCapeControllable;
import com.hfstudio.guidenh.guide.scene.element.GuidebookNameplateControllable;
import com.hfstudio.guidenh.guide.scene.element.GuidebookPlayerPoseControllable;
import com.hfstudio.guidenh.guide.scene.element.GuidebookSceneEntityLoader;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.level.GuidebookPreviewBlockPlacer;
import com.hfstudio.guidenh.guide.scene.level.GuidebookTileEntityLoader;

public class GuideSceneStructureSnapshot implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final List<BlockStateEntry> blocks = new ArrayList<>();
    private final List<TileEntityEntry> tileEntities = new ArrayList<>();
    private final List<EntityEntry> entities = new ArrayList<>();
    private final List<ExplicitBlockIdEntry> explicitBlockIds = new ArrayList<>();
    private final List<PreviewAuthorityEntry> previewAuthorityEntries = new ArrayList<>();

    public static GuideSceneStructureSnapshot capture(GuidebookLevel level) {
        GuideSceneStructureSnapshot snapshot = new GuideSceneStructureSnapshot();
        snapshot.captureBlocks(level);
        snapshot.captureTileEntities(level);
        snapshot.captureEntities(level);
        snapshot.captureExplicitBlockIds(level);
        snapshot.capturePreviewAuthority(level);
        return snapshot;
    }

    public GuidebookLevel restoreLevel() {
        GuidebookLevel level = new GuidebookLevel();
        Map<Long, String> explicitBlockIdsByPos = indexExplicitBlockIds();
        restoreBlocks(level, explicitBlockIdsByPos);
        restoreTileEntities(level);
        restoreEntities(level);
        restoreEntityMounts(level);
        restorePreviewAuthority(level);
        return level;
    }

    private void captureBlocks(GuidebookLevel level) {
        for (int[] pos : level.getFilledBlocks()) {
            if (!isValidPos(pos)) {
                continue;
            }
            Block block = level.getBlock(pos[0], pos[1], pos[2]);
            String blockId = GuidebookLevel.resolveBlockId(block);
            if (blockId == null) {
                continue;
            }
            blocks.add(
                new BlockStateEntry(pos[0], pos[1], pos[2], blockId, level.getBlockMetadata(pos[0], pos[1], pos[2])));
        }
    }

    private void captureTileEntities(GuidebookLevel level) {
        for (int[] pos : level.getFilledBlocks()) {
            if (!isValidPos(pos)) {
                continue;
            }
            Block block = level.getBlock(pos[0], pos[1], pos[2]);
            String blockId = GuidebookLevel.resolveBlockId(block);
            if (blockId == null) {
                continue;
            }
            var tileEntity = level.getTileEntity(pos[0], pos[1], pos[2]);
            if (tileEntity == null) {
                continue;
            }
            NBTTagCompound tileTag = new NBTTagCompound();
            try {
                tileEntity.writeToNBT(tileTag);
            } catch (Throwable ignored) {
                continue;
            }
            String encoded = encodeCompound(
                GuidebookTileEntityLoader.withWorldPosition(tileTag, pos[0], pos[1], pos[2]));
            tileEntities.add(
                new TileEntityEntry(
                    pos[0],
                    pos[1],
                    pos[2],
                    blockId,
                    level.getBlockMetadata(pos[0], pos[1], pos[2]),
                    encoded));
        }
    }

    private void captureEntities(GuidebookLevel level) {
        for (Entity entity : level.getEntities()) {
            if (entity == null) {
                continue;
            }
            NBTTagCompound tag = new NBTTagCompound();
            try {
                entity.writeToNBT(tag);
            } catch (Throwable ignored) {
                continue;
            }
            String entityId = resolveEntityId(entity);
            if (entityId == null) {
                continue;
            }
            String sceneEntityId = level.getSceneEntityId(entity.getEntityId());
            GuidebookLevel.SceneEntityMountState mountState = level.getSceneEntityMountState(sceneEntityId);
            entities.add(
                new EntityEntry(
                    entityId,
                    encodeCompound(tag),
                    sceneEntityId,
                    entity instanceof EntityPlayer ? entity.getCommandSenderName() : null,
                    entity instanceof EntityPlayer && entity.getUniqueID() != null ? entity.getUniqueID()
                        .toString() : null,
                    mountState != null ? mountState.vehicleSceneEntityId() : null,
                    mountState == null ? null : Boolean.FALSE,
                    entity instanceof GuidebookNameplateControllable nameplateControllable
                        ? nameplateControllable.isGuidebookNameplateVisible()
                        : null,
                    entity instanceof GuidebookCapeControllable capeControllable
                        ? capeControllable.isGuidebookCapeVisible()
                        : null,
                    capturePreviewPlayerPose(entity),
                    entity instanceof EntityPlayer ? tryReadIsChild(entity) : null));
        }
    }

    private void captureExplicitBlockIds(GuidebookLevel level) {
        for (Map.Entry<Long, String> entry : level.snapshotExplicitBlockIds()
            .entrySet()) {
            long packedPos = entry.getKey();
            explicitBlockIds.add(
                new ExplicitBlockIdEntry(unpackX(packedPos), unpackY(packedPos), unpackZ(packedPos), entry.getValue()));
        }
    }

    private void capturePreviewAuthority(GuidebookLevel level) {
        Map<Long, Map<String, byte[]>> snapshot = level.previewAuthorityStore()
            .snapshotAll();
        for (Map.Entry<Long, Map<String, byte[]>> entry : snapshot.entrySet()) {
            previewAuthorityEntries.add(new PreviewAuthorityEntry(entry.getKey(), entry.getValue()));
        }
    }

    private Map<Long, String> indexExplicitBlockIds() {
        if (explicitBlockIds.isEmpty()) {
            return Map.of();
        }
        HashMap<Long, String> indexed = new HashMap<>(explicitBlockIds.size());
        for (ExplicitBlockIdEntry entry : explicitBlockIds) {
            indexed.put(GuidebookLevel.packPos(entry.x, entry.y, entry.z), entry.explicitBlockId);
        }
        return indexed;
    }

    private void restoreBlocks(GuidebookLevel level, Map<Long, String> explicitBlockIdsByPos) {
        HashMap<String, Block> blockCache = new HashMap<>();
        for (BlockStateEntry entry : blocks) {
            Block block = resolveBlock(entry.blockId, blockCache);
            if (block != null) {
                level.restoreBlockFast(
                    entry.x,
                    entry.y,
                    entry.z,
                    block,
                    entry.meta,
                    explicitBlockIdsByPos.get(GuidebookLevel.packPos(entry.x, entry.y, entry.z)));
            }
        }
    }

    private void restoreTileEntities(GuidebookLevel level) {
        World world = tryResolveWorld(level);
        HashMap<String, Block> blockCache = new HashMap<>();
        for (TileEntityEntry entry : tileEntities) {
            Block block = resolveBlock(entry.blockId, blockCache);
            if (block == null) {
                continue;
            }
            NBTTagCompound tag = decodeCompound(entry.nbt);
            var tileEntity = world != null
                ? GuidebookTileEntityLoader.load(world, block, entry.meta, entry.x, entry.y, entry.z, tag)
                : GuidebookTileEntityLoader.tryCreateAndLoad(tag, entry.x, entry.y, entry.z);
            if (tileEntity != null) {
                Integer metaTileId = GuidebookPreviewBlockPlacer.resolveGregTechMetaTileId(block, entry.meta, tag);
                GuidebookPreviewBlockPlacer.initializeGregTechMetaTile(tileEntity, metaTileId, tag);
                GuidebookPreviewBlockPlacer.applyGregTechDefaultFacing(tileEntity, tag);
                GuidebookPreviewBlockPlacer.applyBartWorksGeneratedBlockMeta(tileEntity, block, entry.meta);
                level.restoreTileEntityFast(entry.x, entry.y, entry.z, tileEntity);
                GuidebookPreviewBlockPlacer.finalizeSpecialPreviewTile(level, entry.x, entry.y, entry.z, tileEntity);
            }
        }
    }

    private void restoreEntities(GuidebookLevel level) {
        World world = tryResolveWorld(level);
        for (EntityEntry entry : entities) {
            Entity entity = GuidebookSceneEntityLoader
                .loadFromNbt(world, entry.entityId, decodeCompound(entry.nbt), entry.playerName, entry.playerUuid);
            if (entity != null) {
                applyEntityRuntimeState(entity, entry);
                level.addEntity(entity, entry.sceneEntityId);
            }
        }
    }

    private void restoreEntityMounts(GuidebookLevel level) {
        if (entities.isEmpty()) {
            return;
        }
        Set<String> appliedUnmounts = new HashSet<>();
        for (EntityEntry entry : entities) {
            String sceneEntityId = GuidebookSceneEntityLoader.trimToNull(entry.sceneEntityId);
            if (sceneEntityId == null) {
                continue;
            }
            if (Boolean.TRUE.equals(entry.unmount) && appliedUnmounts.add(sceneEntityId)) {
                level.clearSceneEntityMount(sceneEntityId);
            }
            String mountTargetSceneEntityId = GuidebookSceneEntityLoader.trimToNull(entry.mountTargetSceneEntityId);
            if (mountTargetSceneEntityId != null) {
                level.setSceneEntityMount(sceneEntityId, mountTargetSceneEntityId);
            }
        }
    }

    private void restorePreviewAuthority(GuidebookLevel level) {
        if (previewAuthorityEntries.isEmpty()) {
            return;
        }
        LinkedHashMap<Long, Map<String, byte[]>> restored = new LinkedHashMap<>();
        for (PreviewAuthorityEntry entry : previewAuthorityEntries) {
            restored.put(entry.packedPos, entry.payloads());
        }
        level.previewAuthorityStore()
            .restoreAll(restored);
    }

    private static boolean isValidPos(int @Nullable [] pos) {
        return pos != null && pos.length >= 3;
    }

    private static int unpackX(long packedPos) {
        return (int) (packedPos << 38 >> 38);
    }

    private static int unpackY(long packedPos) {
        return (int) (packedPos >>> 52);
    }

    private static int unpackZ(long packedPos) {
        return (int) (packedPos << 12 >> 38);
    }

    @Nullable
    private static World tryResolveWorld(GuidebookLevel level) {
        try {
            return level.getOrCreateFakeWorld();
        } catch (IllegalStateException ignored) {
            return null;
        }
    }

    @Nullable
    private static Block resolveBlock(String blockId, Map<String, Block> blockCache) {
        Block cached = blockCache.get(blockId);
        if (cached != null) {
            return cached;
        }
        Object block = Block.blockRegistry.getObject(blockId);
        Block resolved = block instanceof Block ? (Block) block : null;
        if (resolved != null) {
            blockCache.put(blockId, resolved);
        }
        return resolved;
    }

    @Nullable
    private static String resolveEntityId(Entity entity) {
        if (entity instanceof EntityPlayer) {
            return "player";
        }
        String name = EntityList.getEntityString(entity);
        return name != null && !name.isEmpty() ? name : null;
    }

    @Nullable
    private static PreviewPlayerPoseEntry capturePreviewPlayerPose(Entity entity) {
        if (!(entity instanceof GuidebookPlayerPoseControllable poseControllable)) {
            return null;
        }
        GuidebookPreviewPlayerPose pose = poseControllable.getGuidebookPreviewPlayerPose();
        return pose != null ? PreviewPlayerPoseEntry.capture(pose) : null;
    }

    @Nullable
    private static Boolean tryReadIsChild(Entity entity) {
        try {
            return (Boolean) entity.getClass()
                .getMethod("isChild")
                .invoke(entity);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void applyEntityRuntimeState(Entity entity, EntityEntry entry) {
        if (entry.previewNameplateVisible != null && entity instanceof GuidebookNameplateControllable nameplate) {
            nameplate.setGuidebookNameplateVisible(entry.previewNameplateVisible);
        }
        if (entry.previewCapeVisible != null && entity instanceof GuidebookCapeControllable cape) {
            cape.setGuidebookCapeVisible(entry.previewCapeVisible);
        }
        if (entry.previewPose != null && entity instanceof GuidebookPlayerPoseControllable poseControllable) {
            poseControllable.setGuidebookPreviewPlayerPose(entry.previewPose.restore());
        }
        if (entry.previewBaby != null) {
            tryInvokeBooleanInstanceMethod(entity, "setGuidebookBaby", entry.previewBaby);
        }
    }

    private static boolean tryInvokeBooleanInstanceMethod(Entity entity, String methodName, boolean value) {
        try {
            entity.getClass()
                .getMethod(methodName, Boolean.TYPE)
                .invoke(entity, value);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String encodeCompound(@Nullable NBTTagCompound tag) {
        return tag != null ? GuideTextNbtCodec.writeTextSafeCompound(tag) : "";
    }

    private static NBTTagCompound decodeCompound(@Nullable String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return new NBTTagCompound();
        }
        try {
            return GuideTextNbtCodec.readTextSafeCompound(encoded);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decode cached GameScene structure NBT", e);
        }
    }

    public static class BlockStateEntry implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private final int x;
        private final int y;
        private final int z;
        private final String blockId;
        private final int meta;

        public BlockStateEntry(int x, int y, int z, String blockId, int meta) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.blockId = blockId;
            this.meta = meta;
        }
    }

    public static class TileEntityEntry implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private final int x;
        private final int y;
        private final int z;
        private final String blockId;
        private final int meta;
        private final String nbt;

        public TileEntityEntry(int x, int y, int z, String blockId, int meta, String nbt) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.blockId = blockId;
            this.meta = meta;
            this.nbt = nbt;
        }
    }

    public static class EntityEntry implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private final String entityId;
        private final String nbt;
        @Nullable
        private final String sceneEntityId;
        @Nullable
        private final String playerName;
        @Nullable
        private final String playerUuid;
        @Nullable
        private final String mountTargetSceneEntityId;
        @Nullable
        private final Boolean unmount;
        @Nullable
        private final Boolean previewNameplateVisible;
        @Nullable
        private final Boolean previewCapeVisible;
        @Nullable
        private final PreviewPlayerPoseEntry previewPose;
        @Nullable
        private final Boolean previewBaby;

        public EntityEntry(String entityId, String nbt, @Nullable String sceneEntityId, @Nullable String playerName,
            @Nullable String playerUuid, @Nullable String mountTargetSceneEntityId, @Nullable Boolean unmount,
            @Nullable Boolean previewNameplateVisible, @Nullable Boolean previewCapeVisible,
            @Nullable PreviewPlayerPoseEntry previewPose, @Nullable Boolean previewBaby) {
            this.entityId = entityId;
            this.nbt = nbt;
            this.sceneEntityId = sceneEntityId;
            this.playerName = playerName;
            this.playerUuid = playerUuid;
            this.mountTargetSceneEntityId = mountTargetSceneEntityId;
            this.unmount = unmount;
            this.previewNameplateVisible = previewNameplateVisible;
            this.previewCapeVisible = previewCapeVisible;
            this.previewPose = previewPose;
            this.previewBaby = previewBaby;
        }
    }

    public static class ExplicitBlockIdEntry implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private final int x;
        private final int y;
        private final int z;
        @Nullable
        private final String explicitBlockId;

        public ExplicitBlockIdEntry(int x, int y, int z, @Nullable String explicitBlockId) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.explicitBlockId = explicitBlockId;
        }
    }

    public static class PreviewAuthorityEntry implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private final long packedPos;
        private final Map<String, byte[]> payloads;

        public PreviewAuthorityEntry(long packedPos, @Nullable Map<String, byte[]> payloads) {
            this.packedPos = packedPos;
            LinkedHashMap<String, byte[]> copied = new LinkedHashMap<>();
            if (payloads != null) {
                for (Map.Entry<String, byte[]> entry : payloads.entrySet()) {
                    String key = entry.getKey();
                    byte[] payload = entry.getValue();
                    if (key != null && payload != null && payload.length > 0) {
                        copied.put(key, payload.clone());
                    }
                }
            }
            this.payloads = copied.isEmpty() ? Map.of() : Map.copyOf(copied);
        }

        public Map<String, byte[]> payloads() {
            if (payloads.isEmpty()) {
                return Map.of();
            }
            LinkedHashMap<String, byte[]> copied = new LinkedHashMap<>();
            for (Map.Entry<String, byte[]> entry : payloads.entrySet()) {
                copied.put(
                    entry.getKey(),
                    entry.getValue()
                        .clone());
            }
            return copied;
        }
    }

    public static class PreviewPlayerPoseEntry implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Nullable
        private final Vector3Entry headRotationDegrees;
        @Nullable
        private final Vector3Entry leftArmRotationDegrees;
        @Nullable
        private final Vector3Entry rightArmRotationDegrees;
        @Nullable
        private final Vector3Entry leftLegRotationDegrees;
        @Nullable
        private final Vector3Entry rightLegRotationDegrees;
        @Nullable
        private final Vector3Entry capeRotationDegrees;

        public PreviewPlayerPoseEntry(@Nullable Vector3Entry headRotationDegrees,
            @Nullable Vector3Entry leftArmRotationDegrees, @Nullable Vector3Entry rightArmRotationDegrees,
            @Nullable Vector3Entry leftLegRotationDegrees, @Nullable Vector3Entry rightLegRotationDegrees,
            @Nullable Vector3Entry capeRotationDegrees) {
            this.headRotationDegrees = headRotationDegrees;
            this.leftArmRotationDegrees = leftArmRotationDegrees;
            this.rightArmRotationDegrees = rightArmRotationDegrees;
            this.leftLegRotationDegrees = leftLegRotationDegrees;
            this.rightLegRotationDegrees = rightLegRotationDegrees;
            this.capeRotationDegrees = capeRotationDegrees;
        }

        public static PreviewPlayerPoseEntry capture(GuidebookPreviewPlayerPose pose) {
            return new PreviewPlayerPoseEntry(
                Vector3Entry.capture(pose.getHeadRotationDegrees()),
                Vector3Entry.capture(pose.getLeftArmRotationDegrees()),
                Vector3Entry.capture(pose.getRightArmRotationDegrees()),
                Vector3Entry.capture(pose.getLeftLegRotationDegrees()),
                Vector3Entry.capture(pose.getRightLegRotationDegrees()),
                Vector3Entry.capture(pose.resolveCapeRotationDegrees()));
        }

        public GuidebookPreviewPlayerPose restore() {
            return new GuidebookPreviewPlayerPose(
                Vector3Entry.restore(headRotationDegrees),
                Vector3Entry.restore(leftArmRotationDegrees),
                Vector3Entry.restore(rightArmRotationDegrees),
                Vector3Entry.restore(leftLegRotationDegrees),
                Vector3Entry.restore(rightLegRotationDegrees),
                Vector3Entry.restore(capeRotationDegrees));
        }
    }

    public static class Vector3Entry implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private final float x;
        private final float y;
        private final float z;

        public Vector3Entry(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Nullable
        public static Vector3Entry capture(@Nullable Vector3f vector) {
            return vector != null ? new Vector3Entry(vector.x, vector.y, vector.z) : null;
        }

        @Nullable
        public static Vector3f restore(@Nullable Vector3Entry entry) {
            return entry != null ? new Vector3f(entry.x, entry.y, entry.z) : null;
        }
    }
}
