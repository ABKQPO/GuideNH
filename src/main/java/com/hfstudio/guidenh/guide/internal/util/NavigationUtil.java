package com.hfstudio.guidenh.guide.internal.util;

import javax.annotation.Nullable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;

public final class NavigationUtil {

    private static final Logger LOG = LoggerFactory.getLogger(NavigationUtil.class);

    private NavigationUtil() {}

    @Nullable
    public static ItemStack createNavigationIcon(ParsedGuidePage page) {
        var navigation = page.getFrontmatter()
            .navigationEntry();
        if (navigation == null || navigation.iconItemId() == null) {
            return null;
        }

        var iconItemId = navigation.iconItemId();
        var item = (Item) Item.itemRegistry.getObject(iconItemId.toString());
        if (item == null) {
            LOG.error("Couldn't find icon item {} for page {}", iconItemId, page.getId());
            return null;
        }
        return new ItemStack(item);
    }
}
