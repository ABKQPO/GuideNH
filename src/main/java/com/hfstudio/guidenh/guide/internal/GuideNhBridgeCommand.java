package com.hfstudio.guidenh.guide.internal;

import java.util.Collections;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.MathHelper;

import com.hfstudio.guidenh.guide.internal.structure.GuideNhStructureRuntime;
import com.hfstudio.guidenh.guide.internal.structure.GuideStructureCoordinateParser;
import com.hfstudio.guidenh.guide.internal.structure.GuideStructureWorldPlacementTarget;
import com.hfstudio.guidenh.network.GuideNhClientBridgeMessage;
import com.hfstudio.guidenh.network.GuideNhNetwork;

public final class GuideNhBridgeCommand extends CommandBase {

    private static final String[] ROOT_SUB_COMMANDS = { "importstructure", "placeallstructures" };

    @Override
    public String getCommandName() {
        return "guidenh";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return GuidebookText.CommandBridgeUsage.getTranslationKey();
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            send(sender, GuidebookText.CommandBridgeUsage);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "importstructure" -> importStructure(sender, args);
            case "placeallstructures" -> placeAllStructures(sender, args);
            default -> send(sender, GuidebookText.CommandBridgeUsage);
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, ROOT_SUB_COMMANDS);
        }
        return Collections.emptyList();
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    private void importStructure(ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 4) {
            send(sender, GuidebookText.CommandImportStructureUsage);
            return;
        }

        EntityPlayerMP player = requirePlayer(sender);
        if (!player.canCommandSenderUseCommand(3, getCommandName())) {
            send(sender, GuidebookText.CommandStructurePermissionDenied);
            return;
        }

        int baseX = MathHelper.floor_double(player.posX);
        int baseY = MathHelper.floor_double(player.posY);
        int baseZ = MathHelper.floor_double(player.posZ);
        int x = GuideStructureCoordinateParser.parsePosition(baseX, args[1]);
        int y = GuideStructureCoordinateParser.parsePosition(baseY, args[2]);
        int z = GuideStructureCoordinateParser.parsePosition(baseZ, args[3]);
        GuideNhNetwork.channel()
            .sendTo(GuideNhClientBridgeMessage.importStructure(x, y, z), player);
    }

    private void placeAllStructures(ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 4) {
            send(sender, GuidebookText.CommandPlaceAllStructuresUsage);
            return;
        }

        EntityPlayerMP player = requirePlayer(sender);
        if (!player.canCommandSenderUseCommand(3, getCommandName())) {
            send(sender, GuidebookText.CommandStructurePermissionDenied);
            return;
        }

        int baseX = MathHelper.floor_double(player.posX);
        int baseY = MathHelper.floor_double(player.posY);
        int baseZ = MathHelper.floor_double(player.posZ);
        int x = GuideStructureCoordinateParser.parsePosition(baseX, args[1]);
        int y = GuideStructureCoordinateParser.parsePosition(baseY, args[2]);
        int z = GuideStructureCoordinateParser.parsePosition(baseZ, args[3]);
        var sessionStore = GuideNhStructureRuntime.getServerSessionStore();
        var structures = sessionStore.snapshotData(player.getUniqueID());
        if (structures.isEmpty()) {
            send(sender, GuidebookText.CommandStructureNoMemory);
            return;
        }

        GuideNhStructureRuntime.getPlacementService()
            .placeAll(new GuideStructureWorldPlacementTarget(player.worldObj), structures, x, y, z);
        send(sender, GuidebookText.CommandStructurePlacedAll, structures.size(), x, y, z);
    }

    private EntityPlayerMP requirePlayer(ICommandSender sender) throws CommandException {
        EntityPlayer player = getCommandSenderAsPlayer(sender);
        if (player instanceof EntityPlayerMP playerMp) {
            return playerMp;
        }
        throw new CommandException("commands.generic.player.notFound");
    }

    private static void send(ICommandSender sender, GuidebookText key, Object... args) {
        sender.addChatMessage(new ChatComponentTranslation(key.getTranslationKey(), args));
    }
}
