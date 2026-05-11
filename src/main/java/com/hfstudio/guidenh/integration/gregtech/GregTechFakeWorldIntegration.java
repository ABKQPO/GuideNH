package com.hfstudio.guidenh.integration.gregtech;

import net.minecraft.tileentity.TileEntity;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.integration.api.GuidebookFakeWorldIntegration;

public class GregTechFakeWorldIntegration implements GuidebookFakeWorldIntegration {

    public GregTechFakeWorldIntegration() {}

    @Override
    public void registerDummyWorld(Class<?> worldClass) {
        GregTechHelpers.registerDummyWorld(worldClass);
    }

    @Override
    public boolean suppressMarkBlockForUpdateDescriptionResync(TileEntity tileEntity, GuidebookLevel level) {
        return false;
    }
}
