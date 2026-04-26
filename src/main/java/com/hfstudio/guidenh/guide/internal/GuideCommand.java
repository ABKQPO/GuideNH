package com.hfstudio.guidenh.guide.internal;

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
import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.siteexport.ExportTask;

public class GuideCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "guide";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return GuidebookText.CommandUsage.getTranslationKey();
    }

    @Override
    public List<String> getCommandAliases() {
        return Collections.emptyList();
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            send(sender, GuidebookText.CommandUsage);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "list" -> {
                send(sender, GuidebookText.CommandListHeader);
                for (var guide : GuideRegistry.getAll()) {
                    send(sender, GuidebookText.CommandListEntry, guide.getId());
                }
            }
            case "open" -> {
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
            case "reload" -> {
                boolean ok = GuideMEProxy.instance()
                    .reloadResources();
                if (ok) {
                    send(sender, GuidebookText.CommandReloaded);
                } else {
                    send(sender, GuidebookText.CommandReloadUnsupported);
                }
            }
            case "search" -> {
                if (args.length < 2) {
                    send(sender, GuidebookText.CommandSearchUsage);
                    return;
                }
                StringBuilder qb = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    if (i > 1) qb.append(' ');
                    qb.append(args[i]);
                }
                String query = qb.toString();
                try {
                    var results = GuideME.getSearch()
                        .searchGuide(query, null);
                    if (results.isEmpty()) {
                        send(sender, GuidebookText.CommandSearchNoResults, query);
                    } else {
                        send(sender, GuidebookText.CommandSearchResults, query);
                        int shown = 0;
                        for (var r : results) {
                            var title = r.pageTitle() != null ? r.pageTitle()
                                : r.pageId()
                                    .toString();
                            send(sender, GuidebookText.CommandSearchResult, title, r.guideId(), r.pageId());
                            if (++shown >= 10) break;
                        }
                    }
                } catch (Throwable t) {
                    send(sender, GuidebookText.CommandSearchFailure, getErrorMessage(t));
                }
            }
            case "export" -> {
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
                    ExportTask task = new ExportTask(guide, outDir);
                    ExportTask.Result res = task.run();
                    send(
                        sender,
                        GuidebookText.CommandExportSuccess,
                        res.pagesExported,
                        res.pagesFailed,
                        res.assetsCopied,
                        res.outDir);
                } catch (Throwable t) {
                    send(sender, GuidebookText.CommandExportFailure, getErrorMessage(t));
                }
            }
            default -> send(sender, GuidebookText.CommandUsage);
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "list", "open", "reload", "search", "export");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("open") || args[0].equalsIgnoreCase("export"))) {
            var ids = new ArrayList<String>();
            for (var guide : GuideRegistry.getAll()) {
                ids.add(
                    guide.getId()
                        .toString());
            }
            return getListOfStringsMatchingLastWord(args, ids.toArray(new String[0]));
        }
        return new ArrayList<>();
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
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
