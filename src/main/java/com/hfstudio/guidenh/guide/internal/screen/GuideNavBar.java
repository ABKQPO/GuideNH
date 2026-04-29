package com.hfstudio.guidenh.guide.internal.screen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.guide.GuidePageIcon;
import com.hfstudio.guidenh.guide.internal.util.DisplayScale;
import com.hfstudio.guidenh.guide.navigation.NavigationNode;
import com.hfstudio.guidenh.guide.navigation.NavigationTree;
import com.hfstudio.guidenh.guide.render.GuidePageTexture;

public class GuideNavBar {

    public static final int WIDTH_CLOSED = 10;
    public static final int WIDTH_OPEN = 150;
    public static final int CONTENT_PADDING = 2;
    public static final int ROW_H = 12;
    public static final int CHILD_INDENT = 12;
    public static final int EXPAND_INDENT = 8;
    public static final int ICON_SIZE = 9;

    private final List<Row> rows = new ArrayList<>();
    private final Set<NavigationNode> expanded = Collections.newSetFromMap(new IdentityHashMap<>());
    @Nullable
    private NavigationTree lastTree;

    private int x;
    private int y;
    private int height;
    private boolean open;
    private boolean pinned;
    private int scrollY;

    public void setBounds(int x, int y, int height) {
        this.x = x;
        this.y = y;
        this.height = height;
    }

    public int currentWidth() {
        return (open || pinned) ? WIDTH_OPEN : WIDTH_CLOSED;
    }

    public boolean isOpen() {
        return open || pinned;
    }

    public void update(int mouseX, int mouseY, NavigationTree tree) {
        if (tree != lastTree) {
            rebuildRows(tree);
        }
        if (pinned) {
            open = true;
            return;
        }
        int w = currentWidth();
        open = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + height;
    }

    private void rebuildRows(@Nullable NavigationTree tree) {
        rows.clear();
        lastTree = tree;
        if (tree == null) return;
        for (var root : tree.getRootNodes()) {
            addRowRecursive(root, 0);
        }
    }

    private void addRowRecursive(NavigationNode node, int depth) {
        rows.add(new Row(node, depth));
        if (expanded.contains(node)) {
            for (var child : node.children()) {
                addRowRecursive(child, depth + 1);
            }
        }
    }

    public void render(Minecraft mc, @Nullable ResourceLocation currentPageId, int mouseX, int mouseY) {
        if (lastTree == null || rows.isEmpty()) return;
        int w = currentWidth();
        int bgTop = 0xE0151515;
        int bgBot = 0xE0101010;
        drawVGradient(x, y, w, height, bgTop, bgBot);
        Gui.drawRect(x + w - 1, y, x + w, y + height, 0xFF2A2A2A);

        if (!isOpen()) {
            drawArrow(x + w / 2 - 2, y + height / 2 - 3, true, 0xFF888888);
            return;
        }

        int sf = DisplayScale.scaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * sf, mc.displayHeight - (y + height) * sf, w * sf, height * sf);

        FontRenderer fr = mc.fontRenderer;
        int firstVisibleRow = getFirstVisibleRowIndex();
        for (int rowIndex = firstVisibleRow; rowIndex < rows.size(); rowIndex++) {
            var row = rows.get(rowIndex);
            int rowY = getRowY(rowIndex);
            if (rowY >= y + height) break;

            int indent = row.depth * CHILD_INDENT;
            int rowX = x + 2 + indent;

            boolean hovered = mouseX >= x && mouseX < x + w - 1 && mouseY >= rowY && mouseY < rowY + ROW_H;
            boolean current = currentPageId != null && currentPageId.equals(row.node.pageId());
            if (current) {
                Gui.drawRect(x, rowY, x + w - 1, rowY + ROW_H, 0x40FFFFFF);
            } else if (hovered) {
                Gui.drawRect(x, rowY, x + w - 1, rowY + ROW_H, 0x20FFFFFF);
            }

            boolean hasChildren = !row.node.children()
                .isEmpty();
            if (hasChildren) {
                boolean exp = expanded.contains(row.node);
                drawArrow(rowX, rowY + 2, !exp, 0xFFCCCCCC);
            }
            int textX = rowX + EXPAND_INDENT;

            GuidePageIcon icon = row.node.icon();
            if (icon != null) {
                int iy = rowY + (ROW_H - ICON_SIZE) / 2;
                drawMiniIcon(mc, icon, textX, iy);
                textX += ICON_SIZE + 2;
            }

            String title = row.node.title();
            int maxTw = (x + w - 2) - textX;
            if (maxTw > 0) {
                if (fr.getStringWidth(title) > maxTw) {
                    title = fr.trimStringToWidth(title, maxTw - 4) + "\u2026";
                }
                int color = current ? 0xFFFFFFFF : (hovered ? 0xFF88BBFF : 0xFFBBBBBB);
                fr.drawString(title, textX, rowY + 2, color, false);
            }
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    @Nullable
    public ResourceLocation mouseClicked(int mouseX, int mouseY) {
        if (!isOpen()) return null;
        int w = currentWidth();
        if (mouseX < x || mouseX >= x + w || mouseY < y || mouseY >= y + height) return null;
        int rowIndex = getRowIndexAt(mouseY);
        if (rowIndex < 0) {
            return null;
        }

        var row = rows.get(rowIndex);
        boolean hasChildren = !row.node.children()
            .isEmpty();
        if (hasChildren) {
            toggleExpand(row.node);
        }
        if (row.node.pageId() != null && row.node.hasPage()) {
            return row.node.pageId();
        }
        return null;
    }

    public boolean contains(int mouseX, int mouseY) {
        int w = currentWidth();
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + height;
    }

    public void scroll(int dwheel) {
        int contentH = rows.size() * ROW_H + CONTENT_PADDING * 2;
        int max = Math.max(0, contentH - height);
        scrollY -= Integer.signum(dwheel) * ROW_H * 2;
        if (scrollY < 0) scrollY = 0;
        if (scrollY > max) scrollY = max;
    }

    private int getFirstVisibleRowIndex() {
        int firstVisibleRow = Math.floorDiv(scrollY - CONTENT_PADDING - 1, ROW_H);
        return Math.max(0, Math.min(firstVisibleRow, rows.size()));
    }

    private int getRowIndexAt(int mouseY) {
        int relativeY = mouseY - y + scrollY - CONTENT_PADDING;
        if (relativeY < 0) {
            return -1;
        }

        int rowIndex = relativeY / ROW_H;
        return rowIndex >= 0 && rowIndex < rows.size() ? rowIndex : -1;
    }

    private int getRowY(int rowIndex) {
        return y + CONTENT_PADDING - scrollY + rowIndex * ROW_H;
    }

    private void toggleExpand(@Nullable NavigationNode node) {
        if (node == null) return;
        if (expanded.contains(node)) expanded.remove(node);
        else expanded.add(node);
        rebuildRows(lastTree);
    }

    public static void drawVGradient(int x, int y, int w, int h, int topColor, int botColor) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        var tes = Tessellator.instance;
        tes.startDrawingQuads();
        float ta = ((topColor >>> 24) & 0xFF) / 255f;
        float tr = ((topColor >>> 16) & 0xFF) / 255f;
        float tg = ((topColor >>> 8) & 0xFF) / 255f;
        float tb = (topColor & 0xFF) / 255f;
        float ba = ((botColor >>> 24) & 0xFF) / 255f;
        float br = ((botColor >>> 16) & 0xFF) / 255f;
        float bg = ((botColor >>> 8) & 0xFF) / 255f;
        float bb = (botColor & 0xFF) / 255f;
        tes.setColorRGBA_F(br, bg, bb, ba);
        tes.addVertex(x, y + h, 0);
        tes.addVertex(x + w, y + h, 0);
        tes.setColorRGBA_F(tr, tg, tb, ta);
        tes.addVertex(x + w, y, 0);
        tes.addVertex(x, y, 0);
        tes.draw();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    public static void drawArrow(int x, int y, boolean pointRight, int color) {
        if (pointRight) {
            // row0 1px; row1 2px; row2 3px; row3 4px; row4 3px; row5 2px; row6 1px
            Gui.drawRect(x, y, x + 1, y + 7, color);
            Gui.drawRect(x + 1, y + 1, x + 2, y + 6, color);
            Gui.drawRect(x + 2, y + 2, x + 3, y + 5, color);
            Gui.drawRect(x + 3, y + 3, x + 4, y + 4, color);
        } else {
            Gui.drawRect(x, y, x + 7, y + 1, color);
            Gui.drawRect(x + 1, y + 1, x + 6, y + 2, color);
            Gui.drawRect(x + 2, y + 2, x + 5, y + 3, color);
            Gui.drawRect(x + 3, y + 3, x + 4, y + 4, color);
        }
    }

    public static void drawMiniIcon(Minecraft mc, GuidePageIcon icon, int x, int y) {
        if (icon.isTextureIcon()) {
            drawMiniTextureIcon(icon.texture(), x, y);
            return;
        }
        drawMiniItemIcon(mc, icon.itemStack(), x, y);
    }

    public static void drawMiniItemIcon(Minecraft mc, net.minecraft.item.ItemStack stack, int x, int y) {
        try {
            GL11.glPushMatrix();
            GL11.glTranslatef(x, y, 0);
            float s = (float) ICON_SIZE / 16f;
            GL11.glScalef(s, s, 1f);
            RenderHelper.enableGUIStandardItemLighting();
            GL11.glEnable(GL11.GL_NORMALIZE);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            RenderItem ri = RenderItem.getInstance();
            ri.renderItemAndEffectIntoGUI(mc.fontRenderer, mc.getTextureManager(), stack, 0, 0);
            RenderHelper.disableStandardItemLighting();
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glPopMatrix();
        } catch (Throwable ignored) {
            GL11.glPopMatrix();
        }
    }

    public static void drawMiniTextureIcon(@Nullable GuidePageTexture texture, int x, int y) {
        if (texture == null || texture.isMissing()) {
            return;
        }

        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(texture.getTexture());
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glColor4f(1f, 1f, 1f, 1f);

        var tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.addVertexWithUV(x, y + ICON_SIZE, 0, 0f, 1f);
        tess.addVertexWithUV(x + ICON_SIZE, y + ICON_SIZE, 0, 1f, 1f);
        tess.addVertexWithUV(x + ICON_SIZE, y, 0, 1f, 0f);
        tess.addVertexWithUV(x, y, 0, 0f, 0f);
        tess.draw();
    }

    public static class Row {

        final NavigationNode node;
        final int depth;

        Row(NavigationNode node, int depth) {
            this.node = node;
            this.depth = depth;
        }
    }
}
