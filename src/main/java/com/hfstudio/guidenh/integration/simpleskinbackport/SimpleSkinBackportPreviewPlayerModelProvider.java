package com.hfstudio.guidenh.integration.simpleskinbackport;

import com.hfstudio.guidenh.integration.api.client.PreviewPlayerModelProvider;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class SimpleSkinBackportPreviewPlayerModelProvider implements PreviewPlayerModelProvider {

    public SimpleSkinBackportPreviewPlayerModelProvider() {}

    @Override
    public boolean isModelProvided() {
        return SimpleSkinBackportHelpers.isAvailable();
    }

    @Override
    public boolean tryInitializeModel(Object model) {
        return SimpleSkinBackportHelpers.tryInitialize64xModel(model);
    }
}
