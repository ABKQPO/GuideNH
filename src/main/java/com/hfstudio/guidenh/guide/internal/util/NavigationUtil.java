package com.hfstudio.guidenh.guide.internal.util;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hfstudio.guidenh.guide.GuidePageIcon;
import com.hfstudio.guidenh.guide.PageCollection;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
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

        var iconTextureId = navigation.iconTextureId();
        if (iconTextureId != null && pages != null) {
            var textureIcon = createTextureIcon(page, pages, iconTextureId);
            if (textureIcon != null) {
                return textureIcon;
            }
            // Fallback: even if the page collection couldn't pre-load the texture bytes, still
            // preserve `textureId` on the icon so the static-site exporter can resolve the file
            // through its own asset loader (which has additional resource-pack lookups).
            if (navigation.iconItemId() == null) {
                return new GuidePageIcon(null, iconTextureId, null);
            }
        }

        if (navigation.iconItemId() == null) {
            // No item icon either: still expose textureId when frontmatter declared one so the
            // exporter can attempt to resolve it lazily.
            if (iconTextureId != null) {
                return new GuidePageIcon(null, iconTextureId, null);
            }
            return null;
        }

        var iconItemId = navigation.iconItemId();
        var item = (Item) Item.itemRegistry.getObject(iconItemId.toString());
        if (item == null) {
            LOG.error("Couldn't find icon item {} for page {}", iconItemId, page.getId());
            return null;
        }
        return new GuidePageIcon(new ItemStack(item), null, null);
    }

    @Nullable
    public static GuidePageIcon createNavigationIcon(ParsedGuidePage page) {
        return createNavigationIcon(page, null);
    }

    @Nullable
    public static GuidePageIcon createTextureIcon(ParsedGuidePage page, PageCollection pages, ResourceLocation iconId) {
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

        return new GuidePageIcon(null, iconId, texture);
    }
}
