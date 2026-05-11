package com.hfstudio.guidenh.guide.scene.level;

import java.util.Collection;

import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;

public interface GuidebookPreviewWorld {

    void syncLoadedTileEntities(Collection<TileEntity> tileEntities);

    void syncLoadedEntities(Collection<Entity> entities);

    void updateEntitiesForPreview();
}
