package com.hfstudio.guidenh.compat.translocators;

import net.minecraft.tileentity.TileEntity;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

import codechicken.translocator.TileTranslocator;
import codechicken.translocator.TileTranslocator.Attachment;
import cpw.mods.fml.common.Optional;

public final class TranslocatorsHelpers {

    @Optional.Method(modid = "Translocator")
    public static void prepare(GuidebookLevel level) {
        for (TileEntity te : level.getTileEntities()) {
            if (!(te instanceof TileTranslocator ttrans)) continue;
            fixEjectStates(ttrans);
        }
    }

    @Optional.Method(modid = "Translocator")
    private static void fixEjectStates(TileTranslocator ttrans) {
        for (Attachment a : ttrans.attachments) {
            if (a == null) continue;
            fixSingleAttachment(a);
        }
    }

    @Optional.Method(modid = "Translocator")
    private static void fixSingleAttachment(Attachment a) {
        // a_eject = (redstone && false) != invert_redstone = invert_redstone
        boolean aEject = a.invert_redstone;
        a.a_eject = aEject;
        a.b_eject = aEject;
        double insertPos = aEject ? 1.0 : 0.0;
        a.a_insertpos = insertPos;
        a.b_insertpos = insertPos;
    }
}
