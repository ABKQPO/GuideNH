package com.hfstudio.guidenh.guide.internal.structure;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import com.hfstudio.guidenh.config.ModConfig;

public class GuideNhServerStructureAccess {

    public static final String STRUCTURE_COMMAND_NAME = "guidenh";
    public static final int STRUCTURE_PERMISSION_LEVEL = 3;

    private GuideNhServerStructureAccess() {}

    public static boolean canUseSceneExport(EntityPlayerMP player) {
        return isSceneExportEnabled() && (isSinglePlayerServer() || hasStructurePermission(player));
    }

    public static boolean hasStructurePermission(EntityPlayerMP player) {
        return player != null && player.canCommandSenderUseCommand(STRUCTURE_PERMISSION_LEVEL, STRUCTURE_COMMAND_NAME);
    }

    public static boolean isSceneExportEnabled() {
        return ModConfig.ui.sceneExportEnabled;
    }

    public static boolean isSameDimension(EntityPlayerMP player, int dimensionId) {
        return player != null && player.worldObj != null
            && player.worldObj.provider != null
            && player.worldObj.provider.dimensionId == dimensionId;
    }

    private static boolean isSinglePlayerServer() {
        MinecraftServer server = MinecraftServer.getServer();
        return server != null && server.isSinglePlayer();
    }
}
