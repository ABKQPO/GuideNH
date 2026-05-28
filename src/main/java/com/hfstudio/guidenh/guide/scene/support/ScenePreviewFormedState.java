package com.hfstudio.guidenh.guide.scene.support;

import net.minecraft.tileentity.TileEntity;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.integration.gregtech.GregTechHelpers;

public class ScenePreviewFormedState {

    public static final String SUPPLEMENT_ID = "guidenh.scene.unformed";

    private static final byte[] UNFORMED = new byte[] { 1 };

    protected ScenePreviewFormedState() {}

    public static void updateAfterPlacement(@Nullable GuidebookLevel level, int x, int y, int z, boolean formed) {
        if (level == null) {
            return;
        }
        TileEntity tileEntity = level.getTileEntity(x, y, z);
        if (!GregTechHelpers.isMultiblockController(tileEntity)) {
            setFormed(level, x, y, z, true);
            return;
        }
        setFormed(level, x, y, z, formed);
    }

    public static void setFormed(@Nullable GuidebookLevel level, int x, int y, int z, boolean formed) {
        if (level == null) {
            return;
        }
        long key = GuidebookLevel.packPos(x, y, z);
        if (formed) {
            level.previewAuthorityStore()
                .remove(key, SUPPLEMENT_ID);
            return;
        }
        level.previewAuthorityStore()
            .put(key, SUPPLEMENT_ID, UNFORMED);
    }

    public static boolean shouldSkipFormedSync(@Nullable GuidebookLevel level, int x, int y, int z) {
        if (level == null) {
            return false;
        }
        byte[] payload = level.previewAuthorityStore()
            .get(GuidebookLevel.packPos(x, y, z), SUPPLEMENT_ID);
        return payload != null && payload.length > 0;
    }
}
