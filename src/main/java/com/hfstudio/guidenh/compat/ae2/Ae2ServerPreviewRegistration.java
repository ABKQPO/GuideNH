package com.hfstudio.guidenh.compat.ae2;

import com.hfstudio.guidenh.compat.Mods;
import com.hfstudio.guidenh.guide.scene.snapshot.ExportBlockContext;
import com.hfstudio.guidenh.guide.scene.snapshot.ExportSession;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.snapshot.ImportBlockContext;
import com.hfstudio.guidenh.guide.scene.snapshot.ServerPreviewSupplementFetchContributor;
import com.hfstudio.guidenh.guide.scene.snapshot.ServerPreviewSupplementNbt;
import com.hfstudio.guidenh.guide.scene.snapshot.ServerPreviewSupplementRegistry;
import com.hfstudio.guidenh.guide.scene.snapshot.ServerPreviewSupplementSnippetCodec;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

/**
 * Registers AE2 server-authoritative cable-bus preview supplement (compat-layer strategy).
 */
public final class Ae2ServerPreviewRegistration {

    public static final String SUPPLEMENT_ID = "guidenh.ae2.cable_bus";

    private static final String SHARED_MP_SNAPSHOT = "guidenh.ae2.previewAuthority.mpSnapshot_v1";

    private Ae2ServerPreviewRegistration() {}

    public static void register() {
        if (!Mods.AE2.isModLoaded()) {
            return;
        }
        ServerPreviewSupplementRegistry.registerSnippetAndFetch(new Ae2Snippet(), new Ae2Fetch());
    }

    private static final class Ae2Fetch implements ServerPreviewSupplementFetchContributor {

        @Override
        public String supplementId() {
            return SUPPLEMENT_ID;
        }

        @Override
        public void beginExport(ExportSession session) {
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
            session.shared()
                .put(SHARED_MP_SNAPSHOT, snap);
        }
    }

    private static final class Ae2Snippet implements ServerPreviewSupplementSnippetCodec {

        @Override
        public String supplementId() {
            return SUPPLEMENT_ID;
        }

        @Override
        public void encodeBlock(ExportBlockContext ctx, NBTTagCompound structureBlockTag) {
            Ae2CableStructureSupport.Ae2CableMpSnapshot snap = ctx.session()
                .getShared(SHARED_MP_SNAPSHOT, Ae2CableStructureSupport.Ae2CableMpSnapshot.class);
            TileEntity te = ctx.tileEntity();
            Ae2CableStructureSupport.attachCableStreamToExport(
                te,
                structureBlockTag,
                ctx.session()
                    .access()
                    .getSourceWorld(),
                snap);
        }

        @Override
        public void decodeBlock(ImportBlockContext ctx) {
            GuidebookLevel level = ctx.level();
            long key = GuidebookLevel.packPos(ctx.x(), ctx.y(), ctx.z());
            byte[] raw = ServerPreviewSupplementNbt.readSupplement(ctx.structureBlockCompound(), SUPPLEMENT_ID);
            if (raw == null || raw.length == 0) {
                level.previewAuthorityStore()
                    .remove(key, SUPPLEMENT_ID);
            } else {
                level.previewAuthorityStore()
                    .put(key, SUPPLEMENT_ID, raw);
            }
        }
    }
}
