package com.hfstudio.guidenh.guide;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;

/**
 * Points to a guidebook page with an optional anchor within that page.
 *
 * @param pageId
 * @param anchor ID of an anchor in the page.
 */
@Desugar
public record PageAnchor(ResourceLocation pageId, @Nullable String anchor) {

    public static PageAnchor page(ResourceLocation pageId) {
        return new PageAnchor(pageId, null);
    }

    public static PageAnchor parse(String anchor) {
        int sep = anchor.indexOf('#');
        ResourceLocation pageId = null;
        String fragment = null;
        if (sep != -1) {
            pageId = new ResourceLocation(anchor.substring(0, sep));
            fragment = anchor.substring(sep + 1);
        } else {
            pageId = new ResourceLocation(anchor);
        }
        return new PageAnchor(pageId, fragment);
    }

    @Override
    public String toString() {
        if (anchor != null) {
            return pageId.toString() + "#" + anchor;
        } else {
            return pageId.toString();
        }
    }
}
