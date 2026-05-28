package com.hfstudio.guidenh.integration.api.client;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.internal.MutableGuide;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuideNhClientIntegrationRegistry {

    private static final GuideNhClientIntegrationRegistry GLOBAL = new GuideNhClientIntegrationRegistry();
    private final List<PreviewPlayerSlimArmProvider> previewPlayerSlimArmProviders = new ArrayList<>();
    private final List<PreviewPlayerModelProvider> previewPlayerModelProviders = new ArrayList<>();
    private final List<PreviewPlayerElytraProvider> previewPlayerElytraProviders = new ArrayList<>();
    private final List<PreviewBlockRenderProvider> previewBlockRenderProviders = new ArrayList<>();
    private final List<QuestHoverProvider> questHoverProviders = new ArrayList<>();

    public GuideNhClientIntegrationRegistry() {}

    public static GuideNhClientIntegrationRegistry global() {
        return GLOBAL;
    }

    public synchronized void registerPreviewPlayerSlimArmProvider(PreviewPlayerSlimArmProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider");
        }
        if (!previewPlayerSlimArmProviders.contains(provider)) {
            previewPlayerSlimArmProviders.add(provider);
        }
    }

    public synchronized List<PreviewPlayerSlimArmProvider> previewPlayerSlimArmProviders() {
        return List.copyOf(previewPlayerSlimArmProviders);
    }

    public synchronized void registerPreviewPlayerModelProvider(PreviewPlayerModelProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider");
        }
        if (!previewPlayerModelProviders.contains(provider)) {
            previewPlayerModelProviders.add(provider);
        }
    }

    public synchronized List<PreviewPlayerModelProvider> previewPlayerModelProviders() {
        return List.copyOf(previewPlayerModelProviders);
    }

    public synchronized void registerPreviewPlayerElytraProvider(PreviewPlayerElytraProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider");
        }
        if (!previewPlayerElytraProviders.contains(provider)) {
            previewPlayerElytraProviders.add(provider);
        }
    }

    public synchronized List<PreviewPlayerElytraProvider> previewPlayerElytraProviders() {
        return List.copyOf(previewPlayerElytraProviders);
    }

    public synchronized void registerPreviewBlockRenderProvider(PreviewBlockRenderProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider");
        }
        if (!previewBlockRenderProviders.contains(provider)) {
            previewBlockRenderProviders.add(provider);
        }
    }

    public synchronized List<PreviewBlockRenderProvider> previewBlockRenderProviders() {
        return List.copyOf(previewBlockRenderProviders);
    }

    public synchronized void registerQuestHoverProvider(QuestHoverProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider");
        }
        if (!questHoverProviders.contains(provider)) {
            questHoverProviders.add(provider);
        }
    }

    public synchronized List<QuestHoverProvider> questHoverProviders() {
        return List.copyOf(questHoverProviders);
    }

    @Nullable
    public Boolean resolveSlimArms(@Nullable AbstractClientPlayer player) {
        for (PreviewPlayerSlimArmProvider provider : previewPlayerSlimArmProviders()) {
            Boolean slimArms = provider.resolveSlimArms(player);
            if (slimArms != null) {
                return slimArms;
            }
        }
        return null;
    }

    public boolean isPreviewPlayerModelProvided() {
        for (PreviewPlayerModelProvider provider : previewPlayerModelProviders()) {
            if (provider.isModelProvided()) {
                return true;
            }
        }
        return false;
    }

    public boolean tryInitializePreviewPlayerModel(Object model) {
        if (model == null) {
            return false;
        }
        for (PreviewPlayerModelProvider provider : previewPlayerModelProviders()) {
            if (provider.tryInitializeModel(model)) {
                return true;
            }
        }
        return false;
    }

    public boolean isPreviewPlayerElytraStack(@Nullable ItemStack stack) {
        if (stack == null) {
            return false;
        }
        for (PreviewPlayerElytraProvider provider : previewPlayerElytraProviders()) {
            if (provider.isElytraStack(stack)) {
                return true;
            }
        }
        return false;
    }

    public boolean tryRenderPreviewPlayerElytraLayer(@Nullable EntityLivingBase entity, float limbSwing,
        float limbSwingAmount, float partialTicks, float ageInTicks, float scale) {
        if (entity == null) {
            return false;
        }
        for (PreviewPlayerElytraProvider provider : previewPlayerElytraProviders()) {
            if (provider.tryRenderElytraLayer(entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, scale)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public TileEntity promotePreviewBlockTileEntity(@Nullable Block block, @Nullable TileEntity tileEntity) {
        if (block == null || tileEntity == null) {
            return tileEntity;
        }
        TileEntity current = tileEntity;
        for (PreviewBlockRenderProvider provider : previewBlockRenderProviders()) {
            TileEntity promoted = provider.promoteTileEntity(block, current);
            if (promoted != null) {
                return promoted;
            }
        }
        return current;
    }

    public boolean tryRenderPreviewWorldBlock(@Nullable RenderBlocks renderBlocks, @Nullable IBlockAccess blockAccess,
        @Nullable Block block, int x, int y, int z) {
        if (renderBlocks == null || blockAccess == null || block == null) {
            return false;
        }
        for (PreviewBlockRenderProvider provider : previewBlockRenderProviders()) {
            if (provider.tryRenderWorldBlock(renderBlocks, blockAccess, block, x, y, z)) {
                return true;
            }
        }
        return false;
    }

    public boolean isQuestHoverAvailable() {
        for (QuestHoverProvider provider : questHoverProviders()) {
            if (provider.isQuestHoverAvailable()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public UUID currentHoveredQuestId() {
        for (QuestHoverProvider provider : questHoverProviders()) {
            if (!provider.isQuestHoverAvailable()) {
                continue;
            }
            UUID questId = provider.currentHoveredQuestId();
            if (questId != null) {
                return questId;
            }
        }
        return null;
    }

    @Nullable
    public PageAnchor findQuestHoverPage(@Nullable MutableGuide guide, @Nullable UUID questId) {
        if (guide == null || questId == null) {
            return null;
        }
        for (QuestHoverProvider provider : questHoverProviders()) {
            if (!provider.isQuestHoverAvailable()) {
                continue;
            }
            PageAnchor pageAnchor = provider.findQuestHoverPage(guide, questId);
            if (pageAnchor != null) {
                return pageAnchor;
            }
        }
        return null;
    }
}
