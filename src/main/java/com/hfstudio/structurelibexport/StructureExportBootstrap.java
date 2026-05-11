package com.hfstudio.structurelibexport;

import net.minecraftforge.client.ClientCommandHandler;

public class StructureExportBootstrap {

    private static boolean registered;

    public static void registerClientCommands() {
        if (registered) {
            return;
        }
        registered = true;
        ClientCommandHandler.instance.registerCommand(new StructureExportCommand());
    }
}
