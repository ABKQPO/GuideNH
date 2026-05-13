package com.hfstudio.guidenh.network;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentTranslation;

import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.internal.structure.GuideNhStructureRuntime;
import com.hfstudio.guidenh.guide.internal.structure.GuideStructureMemoryStore;
import com.hfstudio.guidenh.guide.internal.structure.GuideStructureWorldPlacementTarget;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class GuideNhStructureRequestHandler implements IMessageHandler<GuideNhStructureRequestMessage, IMessage> {

    private static final ConcurrentHashMap<TransferKey, GuideNhStructureChunkAssembler> CHUNK_TRANSFERS = new ConcurrentHashMap<>();

    @Override
    public IMessage onMessage(GuideNhStructureRequestMessage message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        if (player == null) {
            return null;
        }
        var playerId = player.getUniqueID();
        var sessionStore = GuideNhStructureRuntime.getServerSessionStore();

        try {
            byte action = message.getAction();
            String structureText = message.getStructureText();
            if (message.isChunkedStructureTransfer()) {
                TransferKey key = new TransferKey(playerId, message.getAction(), message.getTransferId());
                GuideNhStructureChunkAssembler assembler = CHUNK_TRANSFERS
                    .computeIfAbsent(key, ignored -> new GuideNhStructureChunkAssembler(message.getChunkCount()));
                structureText = assembler.accept(message);
                if (structureText == null) {
                    return null;
                }
                CHUNK_TRANSFERS.remove(key);
                action = message.getCompletedChunkAction();
            }

            switch (action) {
                case GuideNhStructureRequestMessage.ACTION_CACHE:
                    sessionStore.remember(playerId, "client-cache", structureText);
                    break;
                case GuideNhStructureRequestMessage.ACTION_IMPORT_AND_PLACE:
                    if (!player.canCommandSenderUseCommand(3, "guidenh")) {
                        send(player, GuidebookText.CommandStructurePermissionDenied);
                        break;
                    }
                    GuideStructureMemoryStore.Entry entry = sessionStore
                        .remember(playerId, "client-import", structureText);
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

    public static void send(EntityPlayerMP player, GuidebookText key, Object... args) {
        player.addChatMessage(new ChatComponentTranslation(key.getTranslationKey(), args));
    }

    public static String getErrorMessage(Throwable throwable) {
        return throwable.getMessage() != null ? throwable.getMessage()
            : throwable.getClass()
                .getSimpleName();
    }

    private static final class TransferKey {

        private final UUID playerId;
        private final byte action;
        private final int transferId;

        private TransferKey(UUID playerId, byte action, int transferId) {
            this.playerId = playerId;
            this.action = action;
            this.transferId = transferId;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof TransferKey other)) {
                return false;
            }
            return action == other.action && transferId == other.transferId && playerId.equals(other.playerId);
        }

        @Override
        public int hashCode() {
            int result = playerId.hashCode();
            result = 31 * result + action;
            result = 31 * result + transferId;
            return result;
        }
    }
}
