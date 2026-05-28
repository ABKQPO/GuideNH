package com.hfstudio.guidenh.guide.internal.structure;

import net.minecraft.server.MinecraftServer;

import com.hfstudio.guidenh.config.ModConfig;

import cpw.mods.fml.common.FMLCommonHandler;

public class GuideNhStructureExportAccess {

    private GuideNhStructureExportAccess() {}

    public static boolean canUseSceneExport() {
        boolean singlePlayer = isSinglePlayer();
        return canUseSceneExport(singlePlayer, GuideNhStructureRuntime.isServerStructureCommandsAvailable());
    }

    public static boolean canUseSceneExport(boolean singlePlayer, boolean serverGuideNhInstalled) {
        if (singlePlayer) {
            return ModConfig.ui.sceneExportEnabled;
        }
        return serverGuideNhInstalled && ModConfig.ui.sceneExportEnabled;
    }

    private static boolean isSinglePlayer() {
        MinecraftServer server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        return server != null && server.isSinglePlayer();
    }
}
