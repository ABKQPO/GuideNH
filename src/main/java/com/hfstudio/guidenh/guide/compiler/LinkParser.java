package com.hfstudio.guidenh.guide.compiler;

import java.net.URI;

import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.PageAnchor;

public final class LinkParser {

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
        try {
            pageId = IdUtils.resolveLink(href, compiler.getPageId());
        } catch (IllegalArgumentException ignored) {
            visitor.handleError("Invalid link");
            return;
        }

        if (!compiler.getPageCollection()
            .pageExists(pageId)) {
            visitor.handleError("Page does not exist");
            return;
        }

        visitor.handlePage(new PageAnchor(pageId, fragment));
    }

    public interface Visitor {

        default void handlePage(PageAnchor page) {}

        default void handleExternal(URI uri) {}

        default void handleError(String error) {}
    }

}
