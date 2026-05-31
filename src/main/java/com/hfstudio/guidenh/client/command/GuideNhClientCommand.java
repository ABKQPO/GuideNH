package com.hfstudio.guidenh.client.command;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.internal.GuideME;
import com.hfstudio.guidenh.guide.internal.GuideMEProxy;
import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.GuideScreen;
import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.internal.editor.SceneEditorScreen;
import com.hfstudio.guidenh.guide.internal.item.RegionWandExportMode;
import com.hfstudio.guidenh.guide.internal.item.RegionWandItem;
import com.hfstudio.guidenh.guide.internal.item.RegionWandSelection;
import com.hfstudio.guidenh.guide.internal.structure.GuideNhStructureExportAccess;
import com.hfstudio.guidenh.guide.internal.structure.GuideStructureCoordinateParser;
import com.hfstudio.guidenh.guide.internal.structure.GuideStructureVolume;
import com.hfstudio.guidenh.guide.siteexport.ExportTask;
import com.hfstudio.guidenh.guide.siteexport.site.GuideSiteExportOptions;
import com.hfstudio.guidenh.guide.siteexport.site.GuideSiteExportTask;
import com.hfstudio.guidenh.guide.siteexport.site.GuideSiteOutputPaths;

public class GuideNhClientCommand extends CommandBase {

    public static final String[] ROOT_SUB_COMMANDS = { "editor", "guideeditor", "guideedit", "list", "open", "reload",
        "search", "export", "exportsite", "exportstructure", "pos1", "pos2", "clearselection" };
    public static final String[] EXPORT_STRUCTURE_FLAGS = { "--mode", "snbt", "snbt_e", "blocks", "blocks_e" };
    public static final String[] EXPORT_SITE_FLAGS = { "--ponder-frames", "--ponder-every-tick" };

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
            case "editor" -> {
                if (!requireSceneExportEnabled(sender)) return;
                SceneEditorScreen.open();
            }
            case "guideeditor", "guideedit" -> toggleGuideEditor(sender);
            case "list" -> listGuides(sender);
            case "open" -> openGuide(sender, args);
            case "reload" -> reloadGuides(sender);
            case "search" -> searchGuides(sender, args);
            case "export" -> exportGuide(sender, args);
            case "exportsite" -> exportSite(sender, args);
            case "exportstructure" -> {
                if (!requireSceneExportEnabled(sender)) return;
                exportStructure(sender, args);
            }
            case "pos1" -> {
                if (!requireSceneExportEnabled(sender)) return;
                setSelectionPos(sender, args, 1);
            }
            case "pos2" -> {
                if (!requireSceneExportEnabled(sender)) return;
                setSelectionPos(sender, args, 2);
            }
            case "clearselection" -> {
                if (!requireSceneExportEnabled(sender)) return;
                clearSelection(sender);
            }
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
        if (args.length >= 2 && args[0].equalsIgnoreCase("exportsite")) {
            return getListOfStringsMatchingLastWord(args, EXPORT_SITE_FLAGS);
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("exportstructure")) {
            return getListOfStringsMatchingLastWord(args, EXPORT_STRUCTURE_FLAGS);
        }
        return List.of();
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

    private void toggleGuideEditor(ICommandSender sender) {
        if (!GuideNhStructureExportAccess.canUseSceneExport()) {
            send(sender, GuidebookText.SceneExportDisabled);
            return;
        }
        boolean enabled = GuideScreen.toggleEditorModeFromCommand();
        send(sender, enabled ? GuidebookText.GuideEditorCommandEnabled : GuidebookText.GuideEditorCommandDisabled);
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
                result.pagesExported(),
                result.pagesFailed(),
                result.assetsCopied(),
                result.outDir());
        } catch (Throwable t) {
            send(sender, GuidebookText.CommandExportFailure, getErrorMessage(t));
        }
    }

    private void exportSite(ICommandSender sender, String[] args) {
        ExportSiteCommandOptions commandOptions = parseExportSiteCommandOptions(args);
        Path outDir = GuideSiteOutputPaths
            .resolveRequestedOrDefault(commandOptions.outDirArgument, Paths.get(""), LocalDateTime.now());
        send(sender, GuidebookText.CommandExportSiteStart, outDir);
        try {
            GuideSiteExportTask.Result result = new GuideSiteExportTask(
                outDir,
                new GuideSiteExportOptions(commandOptions.exportPonderEveryTick)).run();
            send(
                sender,
                GuidebookText.CommandExportSiteSuccess,
                result.guidesExported(),
                result.pagesExported(),
                result.pagesFailed(),
                result.outDir());
        } catch (Throwable t) {
            send(sender, GuidebookText.CommandExportSiteFailure, getErrorMessage(t));
        }
    }

    private ExportSiteCommandOptions parseExportSiteCommandOptions(String[] args) {
        String outDirArgument = null;
        boolean exportPonderEveryTick = false;
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if ("--ponder-frames".equalsIgnoreCase(arg) || "--ponder-every-tick".equalsIgnoreCase(arg)) {
                exportPonderEveryTick = true;
            } else if (outDirArgument == null) {
                outDirArgument = arg;
            }
        }
        return new ExportSiteCommandOptions(outDirArgument, exportPonderEveryTick);
    }

    private void exportStructure(ICommandSender sender, String[] args) throws CommandException {
        EntityPlayer player = getCommandSenderAsPlayer(sender);
        ExportStructureOptions options = parseExportStructureOptions(args);
        if (options.coordinateStartIndex() > 0 && args.length - options.coordinateStartIndex() < 6) {
            send(sender, GuidebookText.CommandExportStructureUsage);
            return;
        }

        try {
            int x;
            int y;
            int z;
            int sizeX;
            int sizeY;
            int sizeZ;
            if (options.coordinateStartIndex() > 0) {
                int baseX = MathHelper.floor_double(player.posX);
                int baseY = MathHelper.floor_double(player.posY);
                int baseZ = MathHelper.floor_double(player.posZ);
                int i = options.coordinateStartIndex();
                x = GuideStructureCoordinateParser.parsePosition(baseX, args[i]);
                y = GuideStructureCoordinateParser.parsePosition(baseY, args[i + 1]);
                z = GuideStructureCoordinateParser.parsePosition(baseZ, args[i + 2]);
                sizeX = GuideStructureCoordinateParser.parseSize(args[i + 3]);
                sizeY = GuideStructureCoordinateParser.parseSize(args[i + 4]);
                sizeZ = GuideStructureCoordinateParser.parseSize(args[i + 5]);
            } else {
                RegionWandSelection.Bounds bounds = RegionWandSelection.getBounds();
                if (bounds == null) {
                    send(sender, GuidebookText.RegionWandNeedTwoCorners);
                    return;
                }
                x = bounds.minX();
                y = bounds.minY();
                z = bounds.minZ();
                sizeX = bounds.sizeX();
                sizeY = bounds.sizeY();
                sizeZ = bounds.sizeZ();
            }
            if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) {
                send(sender, GuidebookText.CommandStructureInvalidSize);
                return;
            }

            String structureText = exportRegion(player, x, y, z, sizeX, sizeY, sizeZ, options.mode());
            if (structureText == null) {
                send(
                    sender,
                    GuidebookText.RegionWandAreaTooLarge,
                    GuideStructureVolume.blockCount(sizeX, sizeY, sizeZ));
                return;
            }

            Path savedPath = GuideNhClientBridgeController.getInstance()
                .exportStructureToFile(
                    "exportstructure-" + options.mode()
                        .getDisplayName(),
                    structureText);
            send(sender, GuidebookText.CommandStructureSaved, savedPath.toString());
        } catch (Throwable t) {
            send(sender, GuidebookText.CommandExportFailure, getErrorMessage(t));
        }
    }

    private String exportRegion(EntityPlayer player, int x, int y, int z, int sizeX, int sizeY, int sizeZ,
        RegionWandExportMode mode) {
        int maxX = x + sizeX - 1;
        int maxY = y + sizeY - 1;
        int maxZ = z + sizeZ - 1;
        boolean includeEntities = mode.includeEntities();
        List<Entity> entities = includeEntities ? collectEntities(player, x, y, z, maxX, maxY, maxZ) : List.of();
        if (mode == RegionWandExportMode.BLOCKS || mode == RegionWandExportMode.BLOCKS_ENTITIES) {
            return RegionWandItem.exportBlocks(player.worldObj, x, y, z, maxX, maxY, maxZ, entities)
                .text();
        }
        if (!includeEntities) {
            return RegionWandItem.exportRegionAsStructureSnbt(player.worldObj, x, y, z, sizeX, sizeY, sizeZ);
        }
        return RegionWandItem.exportSnbt(player.worldObj, x, y, z, maxX, maxY, maxZ, sizeX, sizeY, sizeZ, entities)
            .text();
    }

    private List<Entity> collectEntities(EntityPlayer player, int minX, int minY, int minZ, int maxX, int maxY,
        int maxZ) {
        List<Entity> all = player.worldObj.getEntitiesWithinAABBExcludingEntity(
            null,
            AxisAlignedBB.getBoundingBox(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1));
        return all != null ? all : List.of();
    }

    private ExportStructureOptions parseExportStructureOptions(String[] args) {
        RegionWandExportMode mode = RegionWandExportMode.SNBT;
        int coordinateStart = -1;
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if ("--mode".equalsIgnoreCase(arg) && i + 1 < args.length) {
                mode = normalizeExportMode(args[++i]);
            } else if (arg.startsWith("--mode=")) {
                mode = normalizeExportMode(arg.substring("--mode=".length()));
            } else if (!arg.startsWith("--") && coordinateStart < 0) {
                coordinateStart = i;
            }
        }
        return new ExportStructureOptions(mode, coordinateStart);
    }

    private RegionWandExportMode normalizeExportMode(String mode) {
        if ("blocks+entities".equalsIgnoreCase(mode)) {
            return RegionWandExportMode.BLOCKS_ENTITIES;
        }
        if ("snbt+entities".equalsIgnoreCase(mode)) {
            return RegionWandExportMode.SNBT_ENTITIES;
        }
        return RegionWandExportMode.fromCliValue(mode);
    }

    private void setSelectionPos(ICommandSender sender, String[] args, int which) throws CommandException {
        if (args.length < 4) {
            send(sender, GuidebookText.CommandRegionWandPosUsage);
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
            RegionWandSelection.setPos(which, x, y, z);
            send(sender, GuidebookText.RegionWandChatPos, which, x, y, z);
        } catch (Throwable t) {
            send(sender, GuidebookText.CommandExportFailure, getErrorMessage(t));
        }
    }

    private void clearSelection(ICommandSender sender) {
        RegionWandSelection.clear();
        send(sender, GuidebookText.RegionWandSelectionCleared);
    }

    private boolean requireSceneExportEnabled(ICommandSender sender) {
        if (GuideNhStructureExportAccess.canUseSceneExport()) {
            return true;
        }
        send(sender, GuidebookText.SceneExportDisabled);
        return false;
    }

    public static void send(ICommandSender sender, GuidebookText key, Object... args) {
        sender.addChatMessage(new ChatComponentTranslation(key.getTranslationKey(), args));
    }

    public static String getErrorMessage(Throwable throwable) {
        return throwable.getMessage() != null ? throwable.getMessage()
            : throwable.getClass()
                .getSimpleName();
    }

    public static class ExportSiteCommandOptions {

        private final String outDirArgument;
        private final boolean exportPonderEveryTick;

        public ExportSiteCommandOptions(String outDirArgument, boolean exportPonderEveryTick) {
            this.outDirArgument = outDirArgument;
            this.exportPonderEveryTick = exportPonderEveryTick;
        }
    }

    @Desugar
    private record ExportStructureOptions(RegionWandExportMode mode, int coordinateStartIndex) {}
}
