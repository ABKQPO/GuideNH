package com.hfstudio.guidenh.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.guide.internal.item.RegionWandItem;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class RegionWandRenderer {

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityClientPlayerMP player = mc.thePlayer;
        if (player == null) return;
        ItemStack held = player.getHeldItem();
        if (held == null || !(held.getItem() instanceof RegionWandItem)) return;

        int[] p1 = RegionWandItem.getPos(held, 1);
        int[] p2 = RegionWandItem.getPos(held, 2);
        if (p1 == null && p2 == null) return;

        float partialTicks = event.partialTicks;
        double camX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double camY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        double camZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

        GL11.glPushMatrix();
        GL11.glTranslated(-camX, -camY, -camZ);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glLineWidth(2f);

        if (p1 != null) drawBox(p1[0], p1[1], p1[2], p1[0] + 1, p1[1] + 1, p1[2] + 1, 1f, 0.2f, 0.2f, 1f);
        if (p2 != null) drawBox(p2[0], p2[1], p2[2], p2[0] + 1, p2[1] + 1, p2[2] + 1, 0.2f, 0.4f, 1f, 1f);
        if (p1 != null && p2 != null) {
            int minX = Math.min(p1[0], p2[0]);
            int minY = Math.min(p1[1], p2[1]);
            int minZ = Math.min(p1[2], p2[2]);
            int maxX = Math.max(p1[0], p2[0]) + 1;
            int maxY = Math.max(p1[1], p2[1]) + 1;
            int maxZ = Math.max(p1[2], p2[2]) + 1;
            drawBox(minX, minY, minZ, maxX, maxY, maxZ, 0f, 0.79f, 0.95f, 0.9f); // #00CAF2
            drawFilled(minX, minY, minZ, maxX, maxY, maxZ, 0f, 0.79f, 0.95f, 0.15f);
        }

        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glPopMatrix();
    }

    private static void drawBox(double x0, double y0, double z0, double x1, double y1, double z1, float r, float g,
        float b, float a) {
        GL11.glColor4f(r, g, b, a);
        GL11.glBegin(GL11.GL_LINES);
        line(x0, y0, z0, x1, y0, z0);
        line(x1, y0, z0, x1, y0, z1);
        line(x1, y0, z1, x0, y0, z1);
        line(x0, y0, z1, x0, y0, z0);
        line(x0, y1, z0, x1, y1, z0);
        line(x1, y1, z0, x1, y1, z1);
        line(x1, y1, z1, x0, y1, z1);
        line(x0, y1, z1, x0, y1, z0);
        line(x0, y0, z0, x0, y1, z0);
        line(x1, y0, z0, x1, y1, z0);
        line(x1, y0, z1, x1, y1, z1);
        line(x0, y0, z1, x0, y1, z1);
        GL11.glEnd();
    }

    private static void line(double x0, double y0, double z0, double x1, double y1, double z1) {
        GL11.glVertex3d(x0, y0, z0);
        GL11.glVertex3d(x1, y1, z1);
    }

    private static void drawFilled(double x0, double y0, double z0, double x1, double y1, double z1, float r, float g,
        float b, float a) {
        GL11.glColor4f(r, g, b, a);
        // Disable face culling so the translucent faces are visible both from outside and from
        // inside the region (the camera can enter the box when the player stands within it).
        boolean cullWasEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glBegin(GL11.GL_QUADS);
        // -Y
        GL11.glVertex3d(x0, y0, z0);
        GL11.glVertex3d(x1, y0, z0);
        GL11.glVertex3d(x1, y0, z1);
        GL11.glVertex3d(x0, y0, z1);
        // +Y
        GL11.glVertex3d(x0, y1, z0);
        GL11.glVertex3d(x0, y1, z1);
        GL11.glVertex3d(x1, y1, z1);
        GL11.glVertex3d(x1, y1, z0);
        // -Z
        GL11.glVertex3d(x0, y0, z0);
        GL11.glVertex3d(x0, y1, z0);
        GL11.glVertex3d(x1, y1, z0);
        GL11.glVertex3d(x1, y0, z0);
        // +Z
        GL11.glVertex3d(x0, y0, z1);
        GL11.glVertex3d(x1, y0, z1);
        GL11.glVertex3d(x1, y1, z1);
        GL11.glVertex3d(x0, y1, z1);
        // -X
        GL11.glVertex3d(x0, y0, z0);
        GL11.glVertex3d(x0, y0, z1);
        GL11.glVertex3d(x0, y1, z1);
        GL11.glVertex3d(x0, y1, z0);
        // +X
        GL11.glVertex3d(x1, y0, z0);
        GL11.glVertex3d(x1, y1, z0);
        GL11.glVertex3d(x1, y1, z1);
        GL11.glVertex3d(x1, y0, z1);
        GL11.glEnd();
        if (cullWasEnabled) {
            GL11.glEnable(GL11.GL_CULL_FACE);
        }
    }
}
