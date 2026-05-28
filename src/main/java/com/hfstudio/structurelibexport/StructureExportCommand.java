package com.hfstudio.structurelibexport;

import java.util.Arrays;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import com.hfstudio.guidenh.integration.Mods;

public class StructureExportCommand extends CommandBase {

    public static final String SUBCOMMAND_STRUCTURE_LIB = "structureLib";
    public static final String SUBCOMMAND_GAME_SCENE = "gameScene";
    public static final String[] SUBCOMMANDS = { SUBCOMMAND_STRUCTURE_LIB, SUBCOMMAND_GAME_SCENE };
    public static final String[] STRUCTURE_LIB_OPTIONS = { "--config", "--out", "--pixelsPerBlock", "--scale", "--tier",
        "--channel", "--layers", "--facing", "--rotation", "--flip", "--orientation", "--view", "--yaw", "--pitch",
        "--roll", "--rotateX", "--rotateY", "--rotateZ", "--background", "--maxPixels", "--batchSize",
        "--gt-active-controller", "--gt-place-hatches", "--force", "--dry-run" };
    public static final String[] GAME_SCENE_OPTIONS = { "--config", "--out", "--pixelsPerBlock", "--scale", "--layers",
        "--view", "--yaw", "--pitch", "--roll", "--rotateX", "--rotateY", "--rotateZ", "--background", "--maxPixels",
        "--batchSize", "--show-annotations", "--show-grid", "--force", "--dry-run" };

    @Override
    public String getCommandName() {
        return "exportstructure";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/exportStructure <structureLib|gameScene> [options...]";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args == null || args.length == 0) {
            throw new CommandException(getCommandUsage(sender));
        }
        String subcommand = args[0];
        String[] childArgs = Arrays.copyOfRange(args, 1, args.length);
        if (SUBCOMMAND_STRUCTURE_LIB.equals(subcommand)) {
            if (!Mods.StructureLib.isModLoaded()) {
                throw new CommandException("StructureLib is not loaded.");
            }
            StructureLibExportSubcommand.run(sender, childArgs);
            return;
        }
        if (SUBCOMMAND_GAME_SCENE.equals(subcommand)) {
            GameSceneExportOptions options = GameSceneExportOptionParser.parse(childArgs);
            new GameSceneExportRunner().run(sender, options);
            return;
        }
        throw new CommandException("Unknown exportStructure subcommand: " + subcommand);
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args == null || args.length == 0) {
            return List.of();
        }
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, availableSubcommands());
        }
        String subcommand = args[0];
        String[] childArgs = Arrays.copyOfRange(args, 1, args.length);
        if (SUBCOMMAND_STRUCTURE_LIB.equals(subcommand)) {
            if (!Mods.StructureLib.isModLoaded()) {
                return List.of();
            }
            return getListOfStringsMatchingLastWord(childArgs, STRUCTURE_LIB_OPTIONS);
        }
        if (SUBCOMMAND_GAME_SCENE.equals(subcommand)) {
            return getListOfStringsMatchingLastWord(childArgs, GAME_SCENE_OPTIONS);
        }
        return List.of();
    }

    private String[] availableSubcommands() {
        if (Mods.StructureLib.isModLoaded()) {
            return SUBCOMMANDS;
        }
        return new String[] { SUBCOMMAND_GAME_SCENE };
    }
}
