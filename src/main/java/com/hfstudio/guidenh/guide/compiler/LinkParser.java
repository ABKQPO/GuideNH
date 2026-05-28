package com.hfstudio.guidenh.guide.compiler;

import java.net.URI;

import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.GuideAnchor;
import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiPageIds;

public class LinkParser {

    private LinkParser() {}

    /**
     * Parses a textual reference found in a link.
     */
    public static void parseLink(PageCompiler compiler, String href, Visitor visitor) {
        // Internal vs. external links
        URI uri;
        try {
            uri = URI.create(href);
        } catch (Exception ignored) {
            uri = null;
        }

        // External link
        if (uri != null && uri.isAbsolute()
            && (uri.getScheme()
                .equals("http")
                || uri.getScheme()
                    .equalsIgnoreCase("https"))) {
            visitor.handleExternal(uri);
            return;
        }

        String fragment = null;
        var fragmentSep = href.indexOf('#');
        if (fragmentSep != -1) {
            fragment = href.substring(fragmentSep + 1);
            href = href.substring(0, fragmentSep);
        }

        // Determine the page id, account for relative paths
        ResourceLocation pageId;
        if (href.isEmpty()) {
            pageId = compiler.getPageId();
        } else {
            try {
                pageId = resolveGuidePageLink(compiler, href);
            } catch (IllegalArgumentException ignored) {
                visitor.handleError("Invalid link");
                return;
            }
        }

        var guideId = resolveGuideId(compiler, pageId);
        if (!compiler.pageExistsForLink(guideId, pageId)) {
            visitor.handleError("Page does not exist");
            return;
        }

        visitor.handlePage(new GuideAnchor(guideId, new PageAnchor(pageId, fragment)));
    }

    public static ResourceLocation resolveGuideId(PageCompiler compiler, ResourceLocation pageId) {
        ResourceLocation currentGuideId = compiler.getGuideId();
        if (pageId.getResourceDomain()
            .equals(
                compiler.getPageId()
                    .getResourceDomain())) {
            return currentGuideId;
        }
        return new ResourceLocation(pageId.getResourceDomain(), currentGuideId.getResourcePath());
    }

    private static ResourceLocation resolveGuidePageLink(PageCompiler compiler, String href) {
        ResourceLocation syntheticPageId = MediaWikiPageIds.tryResolveSyntheticTitle(
            compiler.getPageId()
                .getResourceDomain(),
            href);
        return syntheticPageId != null ? syntheticPageId : IdUtils.resolveLink(href, compiler.getPageId());
    }

    public interface Visitor {

        default void handlePage(GuideAnchor anchor) {
            handlePage(anchor.guideId(), anchor.page());
        }

        default void handlePage(ResourceLocation guideId, PageAnchor page) {
            handlePage(page);
        }

        default void handlePage(PageAnchor page) {}

        default void handleExternal(URI uri) {}

        default void handleError(String error) {}
    }

}
