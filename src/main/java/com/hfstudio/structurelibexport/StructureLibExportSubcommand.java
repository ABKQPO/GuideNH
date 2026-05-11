package com.hfstudio.structurelibexport;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import cpw.mods.fml.common.Optional;

public class StructureLibExportSubcommand {

    @Optional.Method(modid = "structurelib")
    public static void run(ICommandSender sender, String[] args) throws CommandException {
        StructureLibExportOptions options = StructureLibExportOptionParser.parse(args);
        new StructureLibExportRunner().run(sender, options);
    }
}
