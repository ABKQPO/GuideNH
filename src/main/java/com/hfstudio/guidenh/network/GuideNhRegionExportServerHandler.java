package com.hfstudio.guidenh.network;

import java.util.Collections;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.AxisAlignedBB;

import com.hfstudio.guidenh.guide.internal.item.RegionWandItem;
import com.hfstudio.guidenh.guide.internal.structure.GuideStructureVolume;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class GuideNhRegionExportServerHandler implements IMessageHandler<GuideNhRegionExportRequestMessage, IMessage> {

    @Override
    public IMessage onMessage(GuideNhRegionExportRequestMessage message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        if (player == null) {
            return null;
        }
        try {
            String structureText = exportRegion(player, message);
            if (structureText == null) {
                GuideNhNetwork.channel()
                    .sendTo(GuideNhRegionExportReplyMessage.error(message.getRequestId(), "Region too large"), player);
                return null;
            }
            for (GuideNhRegionExportReplyMessage packet : GuideNhRegionExportReplyMessage
                .successPackets(message.getRequestId(), structureText)) {
                GuideNhNetwork.channel()
                    .sendTo(packet, player);
            }
        } catch (Throwable t) {
            GuideNhNetwork.channel()
                .sendTo(GuideNhRegionExportReplyMessage.error(message.getRequestId(), getErrorMessage(t)), player);
        }
        return null;
    }

    private String exportRegion(EntityPlayerMP player, GuideNhRegionExportRequestMessage message) {
        int sizeX = message.getSizeX();
        int sizeY = message.getSizeY();
        int sizeZ = message.getSizeZ();
        if (sizeX <= 0 || sizeY <= 0
            || sizeZ <= 0
            || GuideStructureVolume.exceedsLimit(sizeX, sizeY, sizeZ, RegionWandItem.MAX_EXPORT_BLOCKS)) {
            return null;
        }
        int x = message.getX();
        int y = message.getY();
        int z = message.getZ();
        int maxX = x + sizeX - 1;
        int maxY = y + sizeY - 1;
        int maxZ = z + sizeZ - 1;
        if (!message.isIncludeEntities()) {
            return RegionWandItem.exportRegionAsStructureSnbt(player.worldObj, x, y, z, sizeX, sizeY, sizeZ);
        }
        return RegionWandItem
            .exportSnbt(
                player.worldObj,
                x,
                y,
                z,
                maxX,
                maxY,
                maxZ,
                sizeX,
                sizeY,
                sizeZ,
                collectEntities(player, x, y, z, maxX, maxY, maxZ))
            .text();
    }

    @SuppressWarnings("unchecked")
    private List<Entity> collectEntities(EntityPlayer player, int minX, int minY, int minZ, int maxX, int maxY,
        int maxZ) {
        List<Entity> all = player.worldObj.getEntitiesWithinAABBExcludingEntity(
            null,
            AxisAlignedBB.getBoundingBox(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1));
        return all != null ? all : Collections.emptyList();
    }

    private static String getErrorMessage(Throwable throwable) {
        return throwable.getMessage() != null ? throwable.getMessage()
            : throwable.getClass()
                .getSimpleName();
    }
}
