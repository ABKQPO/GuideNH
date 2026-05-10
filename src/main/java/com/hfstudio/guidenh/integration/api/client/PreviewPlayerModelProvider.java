package com.hfstudio.guidenh.integration.api.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface PreviewPlayerModelProvider {

    boolean isModelProvided();

    boolean tryInitializeModel(Object model);
}
