package com.hfstudio.guidenh.integration.bartworks;

import net.minecraft.tileentity.TileEntity;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.integration.api.GuidebookFakeWorldIntegration;

public class BartWorksFakeWorldIntegration implements GuidebookFakeWorldIntegration {

    public BartWorksFakeWorldIntegration() {}

    @Override
    public void registerDummyWorld(Class<?> worldClass) {}

    @Override
    public boolean suppressMarkBlockForUpdateDescriptionResync(TileEntity tileEntity, GuidebookLevel level) {
        return BartWorksHelpers.isRotorBlock(tileEntity);
    }
}
