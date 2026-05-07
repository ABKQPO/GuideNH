package com.hfstudio.guidenh.guide.internal.util;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hfstudio.guidenh.guide.GuidePageIcon;
import com.hfstudio.guidenh.guide.PageCollection;
import com.hfstudio.guidenh.guide.compiler.NavigationIconEntry;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.compiler.YamlNbtConverter;
import com.hfstudio.guidenh.guide.render.GuidePageTexture;

public class NavigationUtil {

    public static final Logger LOG = LoggerFactory.getLogger(NavigationUtil.class);

    private NavigationUtil() {}

    @Nullable
    public static GuidePageIcon createNavigationIcon(ParsedGuidePage page, @Nullable PageCollection pages) {
        var navigation = page.getFrontmatter()
            .navigationEntry();
        if (navigation == null) {
            return null;
        }

        var iconTextureEntries = navigation.iconTextureEntries();
        if (iconTextureEntries != null && !iconTextureEntries.isEmpty()) {
            List<GuidePageTexture> cycleTextures = new ArrayList<>();
            List<ResourceLocation> cycleTextureIds = new ArrayList<>(iconTextureEntries);
            if (pages != null) {
                for (ResourceLocation texId : iconTextureEntries) {
                    var texture = loadTexture(page, pages, texId);
                    cycleTextures.add(texture != null ? texture : GuidePageTexture.missing());
                }
            }
            return new GuidePageIcon(
                null,
                cycleTextureIds.get(0),
                cycleTextures.isEmpty() ? null : cycleTextures.get(0),
                null,
                cycleTextures.isEmpty() ? null : cycleTextures,
                cycleTextureIds);
        }

        var iconTextureId = navigation.iconTextureId();
        if (iconTextureId != null) {
            if (pages != null) {
                var textureIcon = createTextureIcon(page, pages, iconTextureId);
                if (textureIcon != null) {
                    return textureIcon;
                }
            }
            if (navigation.iconItemId() == null && (navigation.iconEntries() == null || navigation.iconEntries()
                .isEmpty())) {
                return new GuidePageIcon(null, iconTextureId, null, null, null, null);
            }
        }

        var iconEntries = navigation.iconEntries();
        if (iconEntries != null && !iconEntries.isEmpty()) {
            List<ItemStack> cycleItems = new ArrayList<>();
            for (NavigationIconEntry entry : iconEntries) {
                var stack = resolveItemStack(page, entry.itemId(), entry.meta(), entry.nbt());
                if (stack != null) {
                    cycleItems.add(stack);
                }
            }
            if (!cycleItems.isEmpty()) {
                return new GuidePageIcon(cycleItems.get(0), null, null, cycleItems, null, null);
            }
        }

        if (navigation.iconItemId() == null) {
            if (iconTextureId != null) {
                return new GuidePageIcon(null, iconTextureId, null, null, null, null);
            }
            return null;
        }

        var stack = resolveItemStack(
            page,
            navigation.iconItemId(),
            navigation.iconItemMeta(),
            navigation.iconComponents());
        if (stack == null) return null;
        return new GuidePageIcon(stack, null, null, null, null, null);
    }

    @Nullable
    public static GuidePageIcon createNavigationIcon(ParsedGuidePage page) {
        return createNavigationIcon(page, null);
    }

    @Nullable
    public static GuidePageIcon createTextureIcon(ParsedGuidePage page, PageCollection pages, ResourceLocation iconId) {
        var texture = loadTexture(page, pages, iconId);
        if (texture == null) return null;
        return new GuidePageIcon(null, iconId, texture, null, null, null);
    }

    @Nullable
    private static ItemStack resolveItemStack(ParsedGuidePage page, String itemId, int meta,
        @Nullable java.util.Map<?, ?> nbt) {
        var item = (Item) Item.itemRegistry.getObject(itemId);
        if (item == null) {
            LOG.error("Couldn't find icon item {} for page {}", itemId, page.getId());
            return null;
        }
        var stack = new ItemStack(item, 1, meta);
        if (nbt != null) {
            NBTTagCompound nbtTag = YamlNbtConverter.toNbt(nbt);
            stack.setTagCompound(nbtTag);
        }
        return stack;
    }

    @Nullable
    private static GuidePageTexture loadTexture(ParsedGuidePage page, PageCollection pages, ResourceLocation iconId) {
        var data = pages.loadAsset(iconId);
        if (data == null || data.length == 0) {
            LOG.error("Couldn't find icon texture {} for page {}", iconId, page.getId());
            return null;
        }
        var texture = GuidePageTexture.load(iconId, data);
        if (texture.isMissing()) {
            LOG.error("Couldn't decode icon texture {} for page {}", iconId, page.getId());
            return null;
        }
        return texture;
    }
}
