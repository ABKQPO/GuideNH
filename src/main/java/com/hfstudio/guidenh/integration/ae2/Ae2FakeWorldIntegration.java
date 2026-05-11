package com.hfstudio.guidenh.integration.ae2;

import net.minecraft.tileentity.TileEntity;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.integration.Mods;
import com.hfstudio.guidenh.integration.api.GuidebookFakeWorldIntegration;

public class Ae2FakeWorldIntegration implements GuidebookFakeWorldIntegration {

    public Ae2FakeWorldIntegration() {}

    @Override
    public void registerDummyWorld(Class<?> worldClass) {}

    @Override
    public boolean suppressMarkBlockForUpdateDescriptionResync(TileEntity tileEntity, GuidebookLevel level) {
        if (!Mods.AE2.isModLoaded()) {
            return false;
        }
        try {
            return Ae2Helpers.suppressMarkBlockForUpdateDescriptionResync(tileEntity, level);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
