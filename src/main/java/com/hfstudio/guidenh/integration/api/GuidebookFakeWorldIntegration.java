package com.hfstudio.guidenh.integration.api;

import net.minecraft.tileentity.TileEntity;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

public interface GuidebookFakeWorldIntegration {

    FakeWorldCreationScope NOOP_FAKE_WORLD_SCOPE = () -> {};

    interface FakeWorldCreationScope extends AutoCloseable {

        @Override
        void close();
    }

    void registerDummyWorld(Class<?> worldClass);

    boolean suppressMarkBlockForUpdateDescriptionResync(TileEntity tileEntity, GuidebookLevel level);

    default FakeWorldCreationScope openFakeWorldCreationScope() {
        return NOOP_FAKE_WORLD_SCOPE;
    }
}
