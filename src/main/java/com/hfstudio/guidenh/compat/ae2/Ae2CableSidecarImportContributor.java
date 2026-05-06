package com.hfstudio.guidenh.compat.ae2;

import com.hfstudio.guidenh.compat.Mods;
import com.hfstudio.guidenh.guide.scene.level.GuideAe2CableSnbt;
import com.hfstudio.guidenh.guide.scene.snapshot.ImportBlockContext;
import com.hfstudio.guidenh.guide.scene.snapshot.StructureImportContributor;

/**
 * Applies {@link GuideAe2CableSnbt} sidecar from structure blocks into {@link com.hfstudio.guidenh.guide.scene.level.GuidebookLevel}.
 */
public final class Ae2CableSidecarImportContributor implements StructureImportContributor {

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public void apply(ImportBlockContext ctx) {
        if (!Mods.AE2.isModLoaded()) {
            return;
        }
        GuideAe2CableSnbt.applySidecar(ctx.level(), ctx.x(), ctx.y(), ctx.z(), ctx.structureBlockCompound());
    }
}
