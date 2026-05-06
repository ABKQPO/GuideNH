package com.hfstudio.guidenh.guide.scene.snapshot;

import net.minecraft.nbt.NBTTagCompound;

/**
 * Encodes/decodes one supplement id for structure export/import (no network I/O).
 */
public interface ServerPreviewSupplementSnippetCodec {

    String supplementId();

    default int priority() {
        return 10;
    }

    void encodeBlock(ExportBlockContext ctx, NBTTagCompound structureBlockTag);

    void decodeBlock(ImportBlockContext ctx);
}
