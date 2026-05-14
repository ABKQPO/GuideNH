package com.hfstudio.guidenh.guide.internal.editor;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.internal.editor.io.SceneEditorStructureCache;
import com.hfstudio.guidenh.guide.internal.item.RegionWandItem;
import com.hfstudio.guidenh.guide.internal.item.RegionWandSelection;

public class SceneEditorOpenService {

    private final SceneEditorStructureCache structureCache;

    public SceneEditorOpenService() {
        this(SceneEditorStructureCache.createDefault());
    }

    public SceneEditorOpenService(SceneEditorStructureCache structureCache) {
        this.structureCache = structureCache;
    }

    public OpenResult createInitialSession(@Nullable EntityPlayer player) {
        if (player == null) {
            return new OpenResult(SceneEditorSession.createBlank(), false, null);
        }

        if (!RegionWandSelection.hasCompleteSelection()) {
            return createInitialSession(false, null);
        }

        ItemStack held = player.getHeldItem();
        String mode = RegionWandItem.getExportMode(held);
        // blocks/blocks_e modes generate <GameScene><Block> MDX, not SNBT ImportStructure.
        // Open blank so the editor doesn't pre-fill with the wrong format.
        if (RegionWandItem.MODE_BLOCKS.equals(mode) || RegionWandItem.MODE_BLOCKS_ENTITIES.equals(mode)) {
            return new OpenResult(SceneEditorSession.createBlank(), false, null);
        }

        boolean includeEntities = RegionWandItem.includeEntities(mode);
        String structureSnbt = RegionWandItem.exportSelectionAsStructureSnbt(player.worldObj, held, includeEntities);
        return createInitialSession(true, structureSnbt);
    }

    @Nullable
    public ServerSelectionRequest createServerSelectionRequest(@Nullable EntityPlayer player) {
        if (player == null || !RegionWandSelection.hasCompleteSelection()) {
            return null;
        }
        ItemStack held = player.getHeldItem();
        String mode = RegionWandItem.getExportMode(held);
        if (RegionWandItem.MODE_BLOCKS.equals(mode) || RegionWandItem.MODE_BLOCKS_ENTITIES.equals(mode)) {
            return null;
        }
        RegionWandSelection.Bounds bounds = RegionWandSelection.getBounds();
        if (bounds == null) {
            return null;
        }
        return new ServerSelectionRequest(
            bounds.minX(),
            bounds.minY(),
            bounds.minZ(),
            bounds.sizeX(),
            bounds.sizeY(),
            bounds.sizeZ(),
            RegionWandItem.includeEntities(mode));
    }

    OpenResult createInitialSession(@Nullable ItemStack held, @Nullable String structureSnbt) {
        boolean canImportSelection = held != null && held.getItem() instanceof RegionWandItem
            && RegionWandItem.hasCompleteSelection(held);
        return createInitialSession(canImportSelection, structureSnbt);
    }

    OpenResult createInitialSession(boolean canImportSelection, @Nullable String structureSnbt) {
        if (!canImportSelection || structureSnbt == null || structureSnbt.isEmpty()) {
            return new OpenResult(SceneEditorSession.createBlank(), true, GuidebookText.SceneEditorImportUnavailable);
        }

        SceneEditorSession session = SceneEditorSession.createImported(structureCache.createStructureSource());
        session.setImportedStructureSnbt(structureSnbt);
        applyImportedStructureDefaults(session, structureSnbt);
        return new OpenResult(session, false, GuidebookText.SceneEditorImportedSession);
    }

    void applyImportedStructureDefaults(SceneEditorSession session, String structureSnbt) {
        float[] structureCenter = extractStructureCenter(structureSnbt);
        if (structureCenter == null) {
            return;
        }
        session.getSceneModel()
            .setCenterX(structureCenter[0]);
        session.getSceneModel()
            .setCenterY(structureCenter[1]);
        session.getSceneModel()
            .setCenterZ(structureCenter[2]);
    }

    @Nullable
    private float[] extractStructureCenter(String structureSnbt) {
        try {
            NBTBase parsed = JsonToNBT.func_150315_a(structureSnbt);
            if (!(parsed instanceof NBTTagCompound root)) {
                return null;
            }
            int[] size = root.getIntArray("size");
            if (size.length < 3) {
                return null;
            }
            return new float[] { size[0] * 0.5f, size[1] * 0.5f, size[2] * 0.5f };
        } catch (Exception ignored) {
            return null;
        }
    }

    public static class OpenResult {

        private final SceneEditorSession session;
        private final boolean importUnavailable;
        @Nullable
        private final GuidebookText openFeedbackMessage;

        private OpenResult(SceneEditorSession session, boolean importUnavailable,
            @Nullable GuidebookText openFeedbackMessage) {
            this.session = session;
            this.importUnavailable = importUnavailable;
            this.openFeedbackMessage = openFeedbackMessage;
        }

        public SceneEditorSession getSession() {
            return session;
        }

        public boolean isImportUnavailable() {
            return importUnavailable;
        }

        @Nullable
        public GuidebookText getOpenFeedbackMessage() {
            return openFeedbackMessage;
        }
    }

    public static class ServerSelectionRequest {

        private final int x;
        private final int y;
        private final int z;
        private final int sizeX;
        private final int sizeY;
        private final int sizeZ;
        private final boolean includeEntities;

        private ServerSelectionRequest(int x, int y, int z, int sizeX, int sizeY, int sizeZ, boolean includeEntities) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
            this.includeEntities = includeEntities;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getZ() {
            return z;
        }

        public int getSizeX() {
            return sizeX;
        }

        public int getSizeY() {
            return sizeY;
        }

        public int getSizeZ() {
            return sizeZ;
        }

        public boolean isIncludeEntities() {
            return includeEntities;
        }
    }
}
