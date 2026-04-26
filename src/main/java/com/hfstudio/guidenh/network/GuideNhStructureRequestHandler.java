package com.hfstudio.guidenh.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentTranslation;

import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.internal.structure.GuideNhStructureRuntime;
import com.hfstudio.guidenh.guide.internal.structure.GuideStructureMemoryStore;
import com.hfstudio.guidenh.guide.internal.structure.GuideStructureWorldPlacementTarget;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public final class GuideNhStructureRequestHandler implements IMessageHandler<GuideNhStructureRequestMessage, IMessage> {

    @Override
    public IMessage onMessage(GuideNhStructureRequestMessage message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        if (player == null) {
            return null;
        }
        var playerId = player.getUniqueID();
        var sessionStore = GuideNhStructureRuntime.getServerSessionStore();

        try {
            switch (message.getAction()) {
                case GuideNhStructureRequestMessage.ACTION_CACHE:
                    sessionStore.remember(playerId, "client-cache", message.getStructureText());
                    break;
                case GuideNhStructureRequestMessage.ACTION_IMPORT_AND_PLACE:
                    if (!player.canCommandSenderUseCommand(3, "guidenh")) {
                        send(player, GuidebookText.CommandStructurePermissionDenied);
                        break;
                    }
                    GuideStructureMemoryStore.Entry entry = sessionStore
                        .remember(playerId, "client-import", message.getStructureText());
                    GuideNhStructureRuntime.getPlacementService()
                        .place(
                            new GuideStructureWorldPlacementTarget(player.worldObj),
                            entry.getData(),
                            message.getX(),
                            message.getY(),
                            message.getZ());
                    send(
                        player,
                        GuidebookText.CommandStructureImportSuccess,
                        message.getX(),
                        message.getY(),
                        message.getZ());
                    break;
                case GuideNhStructureRequestMessage.ACTION_PLACE_ALL:
                    if (!player.canCommandSenderUseCommand(3, "guidenh")) {
                        send(player, GuidebookText.CommandStructurePermissionDenied);
                        break;
                    }
                    var structures = sessionStore.snapshotData(playerId);
                    if (structures.isEmpty()) {
                        send(player, GuidebookText.CommandStructureNoMemory);
                        break;
                    }
                    GuideNhStructureRuntime.getPlacementService()
                        .placeAll(
                            new GuideStructureWorldPlacementTarget(player.worldObj),
                            structures,
                            message.getX(),
                            message.getY(),
                            message.getZ());
                    send(
                        player,
                        GuidebookText.CommandStructurePlacedAll,
                        structures.size(),
                        message.getX(),
                        message.getY(),
                        message.getZ());
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            send(player, GuidebookText.CommandStructureImportFailure, getErrorMessage(e));
        }
        return null;
    }

    private static void send(EntityPlayerMP player, GuidebookText key, Object... args) {
        player.addChatMessage(new ChatComponentTranslation(key.getTranslationKey(), args));
    }

    private static String getErrorMessage(Throwable throwable) {
        return throwable.getMessage() != null ? throwable.getMessage()
            : throwable.getClass()
                .getSimpleName();
    }
}
