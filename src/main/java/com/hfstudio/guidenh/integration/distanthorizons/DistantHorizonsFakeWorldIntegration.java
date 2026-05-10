package com.hfstudio.guidenh.integration.distanthorizons;

import net.minecraft.tileentity.TileEntity;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.integration.api.GuidebookFakeWorldIntegration;

public class DistantHorizonsFakeWorldIntegration implements GuidebookFakeWorldIntegration {

    public DistantHorizonsFakeWorldIntegration() {}

    @Override
    public void registerDummyWorld(Class<?> worldClass) {}

    @Override
    public boolean suppressMarkBlockForUpdateDescriptionResync(TileEntity tileEntity, GuidebookLevel level) {
        return false;
    }

    @Override
    public FakeWorldCreationScope openFakeWorldCreationScope() {
        DistantHorizonsCompat.SuppressionToken token = DistantHorizonsCompat.suppressClientWorldLoadHooks();
        return token::close;
    }
}
