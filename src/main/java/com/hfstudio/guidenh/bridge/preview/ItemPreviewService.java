package com.hfstudio.guidenh.bridge.preview;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.bridge.protocol.BridgeProtocolLimits;
import com.hfstudio.guidenh.guide.compiler.IdUtils;
import com.hfstudio.guidenh.guide.document.interaction.ItemTooltip;
import com.hfstudio.guidenh.guide.internal.tooltip.GuideItemTooltipLines;
import com.hfstudio.guidenh.guide.scene.ponder.PonderNbtPath;
import com.hfstudio.guidenh.guide.siteexport.site.GuideSiteItemSupport;

public class ItemPreviewService {

    private static final int DEFAULT_ICON_SIZE = 64;
    private static final int LARGE_ICON_SIZE = 128;
    private static final int RENDER_TIMEOUT_SECONDS = 5;

    private final ItemPreviewCache cache;
    private final BridgeProtocolLimits limits;

    public ItemPreviewService(ItemPreviewCache cache, BridgeProtocolLimits limits) {
        this.cache = cache;
        this.limits = limits;
    }

    public PreviewResolveResult resolve(PreviewResolveQuery query) {
        if (!"items".equals(query.getCapability())) {
            throw new IllegalArgumentException("Unknown preview capability");
        }

        ItemStack stack = resolveItemStack(query);
        if (stack == null || stack.getItem() == null) {
            throw new IllegalArgumentException("Unknown item id");
        }

        ItemPreviewCacheKey cacheKey = new ItemPreviewCacheKey(
            query.getCapability(),
            query.getId(),
            stack.getItemDamage(),
            Math.max(1, query.getCount()),
            normalizedNbtText(stack.stackTagCompound),
            query.getRenderVariant());
        ItemPreviewPayload cached = cache.get(cacheKey);
        if (cached != null) {
            return cached.toResult(query.getCapability());
        }

        ItemPreviewPayload payload = renderOnClientThread(cacheKey, stack.copy());
        cache.put(cacheKey, payload);
        return payload.toResult(query.getCapability());
    }

    private ItemStack resolveItemStack(PreviewResolveQuery query) {
        ItemStack stack = IdUtils.resolveItemStack(query.getId(), "minecraft");
        if (stack == null || stack.getItem() == null) {
            return null;
        }
        stack.stackSize = Math.max(1, query.getCount());
        String nbt = query.getNbt();
        if (!nbt.isEmpty()) {
            try {
                NBTTagCompound parsedNbt = PonderNbtPath.parseCompound(nbt);
                stack.stackTagCompound = parsedNbt;
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid preview NBT", e);
            }
        }
        return stack;
    }

    private ItemPreviewPayload renderOnClientThread(ItemPreviewCacheKey cacheKey, ItemStack stack) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null) {
            throw new IllegalStateException("Minecraft client is not ready for preview rendering.");
        }
        if (minecraft.func_152345_ab()) {
            return createPayload(cacheKey, stack, minecraft);
        }

        CompletableFuture<ItemPreviewPayload> future = new CompletableFuture<>();
        minecraft.func_152344_a(() -> {
            try {
                future.complete(createPayload(cacheKey, stack, Minecraft.getMinecraft()));
            } catch (Throwable error) {
                future.completeExceptionally(error);
            }
        });

        try {
            return future.get(RENDER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception error) {
            throw new IllegalStateException("Timed out waiting for preview rendering.", error);
        }
    }

    private ItemPreviewPayload createPayload(ItemPreviewCacheKey cacheKey, ItemStack stack, Minecraft minecraft) {
        int iconSize = resolveIconSize(cacheKey.getRenderVariant());
        byte[] iconBytes = renderPng(stack, iconSize, minecraft);
        String iconPngBase64 = Base64.getEncoder()
            .encodeToString(iconBytes);
        List<String> tooltipLines = sanitizeTooltipLines(
            GuideItemTooltipLines.build(new ItemTooltip(stack), minecraft));
        String displayName = GuideSiteItemSupport.displayName(stack);
        String resolvedId = GuideSiteItemSupport.itemId(stack);
        String detail = buildDetail(resolvedId, stack.getItemDamage());
        return new ItemPreviewPayload(
            cacheKey.toPreviewKey(),
            cacheKey.getId(),
            displayName,
            detail,
            stack.getItemDamage(),
            stack.stackSize,
            normalizedNbtText(stack.stackTagCompound),
            tooltipLines,
            iconPngBase64,
            iconSize,
            iconSize);
    }

    private int resolveIconSize(String renderVariant) {
        int requestedSize = "picker".equalsIgnoreCase(renderVariant) ? LARGE_ICON_SIZE : DEFAULT_ICON_SIZE;
        int maxEdge = (int) Math.sqrt(Math.max(16, limits.getMaxPreviewIconPixels()));
        return Math.clamp(requestedSize, 16, maxEdge);
    }

    private byte[] renderPng(ItemStack stack, int iconSize, Minecraft minecraft) {
        if (minecraft.gameSettings == null || minecraft.fontRenderer == null) {
            throw new IllegalStateException("Minecraft client is not ready for preview rendering.");
        }

        Framebuffer framebuffer = new Framebuffer(iconSize, iconSize, true);
        framebuffer.setFramebufferColor(0f, 0f, 0f, 0f);

        int previousDisplayWidth = minecraft.displayWidth;
        int previousDisplayHeight = minecraft.displayHeight;
        int previousGuiScale = minecraft.gameSettings.guiScale;

        boolean projectionPushed = false;
        boolean modelViewPushed = false;

        try {
            minecraft.displayWidth = iconSize;
            minecraft.displayHeight = iconSize;
            minecraft.gameSettings.guiScale = 1;

            framebuffer.bindFramebuffer(true);
            GL11.glViewport(0, 0, iconSize, iconSize);
            GL11.glClearColor(0f, 0f, 0f, 0f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPushMatrix();
            projectionPushed = true;
            GL11.glLoadIdentity();
            GL11.glOrtho(0.0D, iconSize, iconSize, 0.0D, 1000.0D, 3000.0D);

            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPushMatrix();
            modelViewPushed = true;
            GL11.glLoadIdentity();
            GL11.glTranslatef(0.0F, 0.0F, -2000.0F);

            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1f);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(1f, 1f, 1f, 1f);

            float scale = (iconSize - 2f) / 16f;
            float origin = (iconSize - 16f * scale) / 2f;
            GL11.glPushMatrix();
            try {
                GL11.glTranslatef(origin, origin, 0f);
                GL11.glScalef(scale, scale, 1f);

                RenderHelper.enableGUIStandardItemLighting();
                GL11.glEnable(GL11.GL_NORMALIZE);
                GL11.glEnable(GL11.GL_DEPTH_TEST);

                RenderItem itemRenderer = RenderItem.getInstance();
                itemRenderer.zLevel = 100f;
                itemRenderer
                    .renderItemAndEffectIntoGUI(minecraft.fontRenderer, minecraft.getTextureManager(), stack, 0, 0);
                itemRenderer
                    .renderItemOverlayIntoGUI(minecraft.fontRenderer, minecraft.getTextureManager(), stack, 0, 0);
                itemRenderer.zLevel = 0f;
            } finally {
                GL11.glPopMatrix();
                RenderHelper.disableStandardItemLighting();
            }

            BufferedImage image = readPixels(iconSize);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        } catch (Exception error) {
            throw new IllegalStateException("Failed to render item preview icon.", error);
        } finally {
            if (modelViewPushed) {
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
                GL11.glPopMatrix();
            }
            if (projectionPushed) {
                GL11.glMatrixMode(GL11.GL_PROJECTION);
                GL11.glPopMatrix();
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
            }

            framebuffer.unbindFramebuffer();
            framebuffer.deleteFramebuffer();
            minecraft.displayWidth = previousDisplayWidth;
            minecraft.displayHeight = previousDisplayHeight;
            minecraft.gameSettings.guiScale = previousGuiScale;
            GL11.glViewport(0, 0, previousDisplayWidth, previousDisplayHeight);

            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(1f, 1f, 1f, 1f);
        }
    }

    private BufferedImage readPixels(int iconSize) {
        java.nio.ByteBuffer buffer = BufferUtils.createByteBuffer(iconSize * iconSize * 4);
        GL11.glReadPixels(0, 0, iconSize, iconSize, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        BufferedImage image = new BufferedImage(iconSize, iconSize, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < iconSize; y++) {
            int flippedY = iconSize - 1 - y;
            for (int x = 0; x < iconSize; x++) {
                int index = (x + y * iconSize) * 4;
                int red = buffer.get(index) & 0xFF;
                int green = buffer.get(index + 1) & 0xFF;
                int blue = buffer.get(index + 2) & 0xFF;
                int alpha = buffer.get(index + 3) & 0xFF;
                image.setRGB(x, flippedY, (alpha << 24) | (red << 16) | (green << 8) | blue);
            }
        }
        return image;
    }

    private List<String> sanitizeTooltipLines(List<String> tooltipLines) {
        List<String> sanitizedLines = new ArrayList<>();
        int maxLines = limits.getMaxPreviewTooltipLines();
        for (String tooltipLine : tooltipLines) {
            if (sanitizedLines.size() >= maxLines) {
                break;
            }
            sanitizedLines.add(tooltipLine == null ? "" : tooltipLine);
        }
        if (sanitizedLines.isEmpty()) {
            sanitizedLines.add(EnumChatFormatting.WHITE + "Unknown item");
        }
        return sanitizedLines;
    }

    private String buildDetail(String resolvedId, int meta) {
        if (resolvedId == null || resolvedId.isEmpty()) {
            return "";
        }
        return resolvedId + ":" + Math.max(meta, 0);
    }

    private String normalizedNbtText(NBTTagCompound tagCompound) {
        return tagCompound == null ? "" : tagCompound.toString();
    }
}
