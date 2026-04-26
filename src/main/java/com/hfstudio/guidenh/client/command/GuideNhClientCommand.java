package com.hfstudio.guidenh.client.command;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.internal.GuideME;
import com.hfstudio.guidenh.guide.internal.GuideMEProxy;
import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.internal.editor.SceneEditorScreen;
import com.hfstudio.guidenh.guide.internal.item.RegionWandItem;
import com.hfstudio.guidenh.guide.internal.structure.GuideStructureCoordinateParser;
import com.hfstudio.guidenh.guide.internal.structure.GuideStructureVolume;
import com.hfstudio.guidenh.guide.siteexport.ExportTask;

public final class GuideNhClientCommand extends CommandBase {

    private static final String[] ROOT_SUB_COMMANDS = { "editor", "list", "open", "reload", "search", "export",
        "exportstructure" };

    @Override
    public String getCommandName() {
        return "guidenhc";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return GuidebookText.CommandClientUsage.getTranslationKey();
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            send(sender, GuidebookText.CommandClientUsage);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "editor" -> SceneEditorScreen.open();
            case "list" -> listGuides(sender);
            case "open" -> openGuide(sender, args);
            case "reload" -> reloadGuides(sender);
            case "search" -> searchGuides(sender, args);
            case "export" -> exportGuide(sender, args);
            case "exportstructure" -> exportStructure(sender, args);
            default -> send(sender, GuidebookText.CommandClientUsage);
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, ROOT_SUB_COMMANDS);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("open") || args[0].equalsIgnoreCase("export"))) {
            var guides = GuideRegistry.getAll();
            var ids = new ArrayList<String>(guides.size());
            for (var guide : guides) {
                ids.add(
                    guide.getId()
                        .toString());
            }
            return getListOfStringsMatchingLastWord(args, ids.toArray(new String[0]));
        }
        return Collections.emptyList();
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    private void listGuides(ICommandSender sender) {
        send(sender, GuidebookText.CommandListHeader);
        for (var guide : GuideRegistry.getAll()) {
            send(sender, GuidebookText.CommandListEntry, guide.getId());
        }
    }

    private void openGuide(ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2) {
            send(sender, GuidebookText.CommandOpenUsage);
            return;
        }

        EntityPlayer player = getCommandSenderAsPlayer(sender);
        var guideId = new ResourceLocation(args[1]);
        Guide guide = GuideRegistry.getById(guideId);
        if (guide == null) {
            send(sender, GuidebookText.CommandGuideNotFound, guideId);
            return;
        }
        GuideMEProxy.instance()
            .openGuide(player, guideId, null);
    }

    private void reloadGuides(ICommandSender sender) {
        boolean ok = GuideMEProxy.instance()
            .reloadResources();
        if (ok) {
            send(sender, GuidebookText.CommandReloaded);
            return;
        }
        send(sender, GuidebookText.CommandReloadUnsupported);
    }

    private void searchGuides(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            send(sender, GuidebookText.CommandSearchUsage);
            return;
        }

        StringBuilder qb = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) {
                qb.append(' ');
            }
            qb.append(args[i]);
        }
        String query = qb.toString();
        try {
            var results = GuideME.getSearch()
                .searchGuide(query, null);
            if (results.isEmpty()) {
                send(sender, GuidebookText.CommandSearchNoResults, query);
                return;
            }

            send(sender, GuidebookText.CommandSearchResults, query);
            int shown = 0;
            for (var result : results) {
                var title = result.pageTitle() != null ? result.pageTitle()
                    : result.pageId()
                        .toString();
                send(sender, GuidebookText.CommandSearchResult, title, result.guideId(), result.pageId());
                if (++shown >= 10) {
                    break;
                }
            }
        } catch (Throwable t) {
            send(sender, GuidebookText.CommandSearchFailure, getErrorMessage(t));
        }
    }

    private void exportGuide(ICommandSender sender, String[] args) {
        if (args.length < 3) {
            send(sender, GuidebookText.CommandExportUsage);
            return;
        }

        var guideId = new ResourceLocation(args[1]);
        Guide guide = GuideRegistry.getById(guideId);
        if (guide == null) {
            send(sender, GuidebookText.CommandGuideNotFound, guideId);
            return;
        }
        Path outDir = Paths.get(args[2])
            .toAbsolutePath();
        send(sender, GuidebookText.CommandExportStart, guideId, outDir);
        try {
            ExportTask.Result result = new ExportTask(guide, outDir).run();
            send(
                sender,
                GuidebookText.CommandExportSuccess,
                result.pagesExported,
                result.pagesFailed,
                result.assetsCopied,
                result.outDir);
        } catch (Throwable t) {
            send(sender, GuidebookText.CommandExportFailure, getErrorMessage(t));
        }
    }

    private void exportStructure(ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 7) {
            send(sender, GuidebookText.CommandExportStructureUsage);
            return;
        }

        EntityPlayer player = getCommandSenderAsPlayer(sender);
        int baseX = MathHelper.floor_double(player.posX);
        int baseY = MathHelper.floor_double(player.posY);
        int baseZ = MathHelper.floor_double(player.posZ);
        try {
            int x = GuideStructureCoordinateParser.parsePosition(baseX, args[1]);
            int y = GuideStructureCoordinateParser.parsePosition(baseY, args[2]);
            int z = GuideStructureCoordinateParser.parsePosition(baseZ, args[3]);
            int sizeX = GuideStructureCoordinateParser.parseSize(args[4]);
            int sizeY = GuideStructureCoordinateParser.parseSize(args[5]);
            int sizeZ = GuideStructureCoordinateParser.parseSize(args[6]);
            if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) {
                send(sender, GuidebookText.CommandStructureInvalidSize);
                return;
            }

            String structureText = RegionWandItem
                .exportRegionAsStructureSnbt(player.worldObj, x, y, z, sizeX, sizeY, sizeZ);
            if (structureText == null) {
                send(
                    sender,
                    GuidebookText.RegionWandAreaTooLarge,
                    GuideStructureVolume.blockCount(sizeX, sizeY, sizeZ));
                return;
            }

            Path savedPath = GuideNhClientBridgeController.getInstance()
                .exportStructureToFile("exportstructure", structureText);
            send(sender, GuidebookText.CommandStructureSaved, savedPath.toString());
        } catch (Throwable t) {
            send(sender, GuidebookText.CommandExportFailure, getErrorMessage(t));
        }
    }

    private static void send(ICommandSender sender, GuidebookText key, Object... args) {
        sender.addChatMessage(new ChatComponentTranslation(key.getTranslationKey(), args));
    }

    private static String getErrorMessage(Throwable throwable) {
        return throwable.getMessage() != null ? throwable.getMessage()
            : throwable.getClass()
                .getSimpleName();
    }
}
