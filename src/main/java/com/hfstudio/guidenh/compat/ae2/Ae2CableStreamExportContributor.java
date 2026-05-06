package com.hfstudio.guidenh.compat.ae2;

import com.hfstudio.guidenh.compat.Mods;
import com.hfstudio.guidenh.guide.scene.snapshot.ExportBlockContext;
import com.hfstudio.guidenh.guide.scene.snapshot.ExportSession;
import com.hfstudio.guidenh.guide.scene.snapshot.StructureExportContributor;

/**
 * Writes {@link com.hfstudio.guidenh.guide.scene.level.GuideAe2CableSnbt#TAG_ROOT} via {@link Ae2CableStructureSupport}.
 */
public final class Ae2CableStreamExportContributor implements StructureExportContributor {

    public static final String SHARED_AE2_MP_SNAPSHOT = "guidenh_ae2CableMpSnapshot_v1";

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public void beginExport(ExportSession session) {
        if (!Mods.AE2.isModLoaded()) {
            return;
        }
        try {
            Ae2CableStructureSupport.Ae2CableMpSnapshot snap = Ae2CableStructureSupport.tryCreateMpSnapshot(
                session.access()
                    .getSourceWorld(),
                (x, y, z) -> session.access()
                    .getTileEntity(x, y, z),
                session.minX(),
                session.minY(),
                session.minZ(),
                session.maxX(),
                session.maxY(),
                session.maxZ());
            if (snap != null) {
                session.shared()
                    .put(SHARED_AE2_MP_SNAPSHOT, snap);
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public void contributeBlock(ExportBlockContext ctx) {
        if (!Mods.AE2.isModLoaded()) {
            return;
        }
        Ae2CableStructureSupport.Ae2CableMpSnapshot snap = ctx.session()
            .getShared(SHARED_AE2_MP_SNAPSHOT, Ae2CableStructureSupport.Ae2CableMpSnapshot.class);
        try {
            Ae2CableStructureSupport.attachCableStreamToExport(
                ctx.tileEntity(),
                ctx.structureBlockTag(),
                ctx.session()
                    .access()
                    .getSourceWorld(),
                snap);
        } catch (Throwable ignored) {}
    }
}
