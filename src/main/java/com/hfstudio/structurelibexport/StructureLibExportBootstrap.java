package com.hfstudio.structurelibexport;

import net.minecraftforge.client.ClientCommandHandler;

import com.hfstudio.guidenh.integration.Mods;

public class StructureLibExportBootstrap {

    private static boolean registered;

    public static void registerClientCommands() {
        if (registered || !Mods.StructureLib.isModLoaded()) {
            return;
        }
        registered = true;
        ClientCommandHandler.instance.registerCommand(new StructureLibExportCommand());
    }
}
