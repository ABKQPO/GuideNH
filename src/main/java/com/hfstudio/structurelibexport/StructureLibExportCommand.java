package com.hfstudio.structurelibexport;

import java.util.Collections;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

public class StructureLibExportCommand extends CommandBase {

    public static final String[] OPTIONS = { "--config", "--out", "--pixelsPerBlock", "--scale", "--tier", "--channel",
        "--layers", "--facing", "--rotation", "--flip", "--orientation", "--view", "--yaw", "--pitch", "--roll",
        "--rotateX", "--rotateY", "--rotateZ", "--background", "--maxPixels", "--batchSize", "--gt-active-controller",
        "--gt-place-hatches", "--force", "--dry-run" };

    @Override
    public String getCommandName() {
        return "exportStructureLib";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/exportStructureLib [controller] [options...]";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        StructureLibExportOptions options = StructureLibExportOptionParser.parse(args);
        new StructureLibExportRunner().run(sender, options);
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            return Collections.emptyList();
        }
        return getListOfStringsMatchingLastWord(args, OPTIONS);
    }
}
