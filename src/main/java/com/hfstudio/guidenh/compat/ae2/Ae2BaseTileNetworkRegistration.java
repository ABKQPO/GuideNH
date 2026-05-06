package com.hfstudio.guidenh.compat.ae2;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import com.hfstudio.guidenh.compat.Mods;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.snapshot.ExportBlockContext;
import com.hfstudio.guidenh.guide.scene.snapshot.ExportSession;
import com.hfstudio.guidenh.guide.scene.snapshot.ImportBlockContext;
import com.hfstudio.guidenh.guide.scene.snapshot.ServerPreviewSupplementFetchContributor;
import com.hfstudio.guidenh.guide.scene.snapshot.ServerPreviewSupplementNbt;
import com.hfstudio.guidenh.guide.scene.snapshot.ServerPreviewSupplementRegistry;
import com.hfstudio.guidenh.guide.scene.snapshot.ServerPreviewSupplementSnippetCodec;

/**
 * Registers type2 AE2 preview supplement {@link Ae2BaseTileNetworkStreamPreview#SUPPLEMENT_ID}
 * ({@link appeng.tile.AEBaseTile}
 * excluding {@link appeng.tile.networking.TileCableBus}): multiplayer batch RPC, snippet encode/decode, session shared
 * key {@link #SHARED_BASE_TILE_MP_SNAPSHOT}. Priority 11 so cable-bus fetch/snippet runs first (default 10).
 */
public final class Ae2BaseTileNetworkRegistration {

    /** Session key parallel to {@link Ae2ServerPreviewRegistration} cable snapshot key. */
    static final String SHARED_BASE_TILE_MP_SNAPSHOT = "guidenh.ae2.previewAuthority.baseTileXpSnapshot_v1";

    private Ae2BaseTileNetworkRegistration() {}

    public static void register() {
        if (!Mods.AE2.isModLoaded()) {
            return;
        }
        ServerPreviewSupplementRegistry.registerSnippetAndFetch(new Ae2NetworkSnippet(), new Ae2NetworkFetch());
    }

    private static final class Ae2NetworkFetch implements ServerPreviewSupplementFetchContributor {

        @Override
        public String supplementId() {
            return Ae2BaseTileNetworkStreamPreview.SUPPLEMENT_ID;
        }

        @Override
        public int priority() {
            return 11;
        }

        @Override
        public void beginExport(ExportSession session) {
            Ae2BaseTileNetworkStructureSupport.Ae2BaseTileNetworkMpSnapshot snap = Ae2BaseTileNetworkStructureSupport
                .tryCreateMpSnapshot(
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
                .put(SHARED_BASE_TILE_MP_SNAPSHOT, snap);
        }
    }

    private static final class Ae2NetworkSnippet implements ServerPreviewSupplementSnippetCodec {

        @Override
        public String supplementId() {
            return Ae2BaseTileNetworkStreamPreview.SUPPLEMENT_ID;
        }

        @Override
        public int priority() {
            return 11;
        }

        @Override
        public void encodeBlock(ExportBlockContext ctx, NBTTagCompound structureBlockTag) {
            Ae2BaseTileNetworkStructureSupport.Ae2BaseTileNetworkMpSnapshot snap = ctx.session()
                .getShared(
                    SHARED_BASE_TILE_MP_SNAPSHOT,
                    Ae2BaseTileNetworkStructureSupport.Ae2BaseTileNetworkMpSnapshot.class);
            TileEntity te = ctx.tileEntity();
            Ae2BaseTileNetworkStructureSupport.attachBaseTileNetworkToExport(
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
            byte[] raw = ServerPreviewSupplementNbt
                .readSupplement(ctx.structureBlockCompound(), Ae2BaseTileNetworkStreamPreview.SUPPLEMENT_ID);
            if (raw == null || raw.length == 0) {
                level.previewAuthorityStore()
                    .remove(key, Ae2BaseTileNetworkStreamPreview.SUPPLEMENT_ID);
            } else {
                level.previewAuthorityStore()
                    .put(key, Ae2BaseTileNetworkStreamPreview.SUPPLEMENT_ID, raw);
            }
        }
    }
}
