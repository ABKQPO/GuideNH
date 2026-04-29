package com.hfstudio.guidenh.guide.siteexport.site;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.compiler.IdUtils;

final class GuideSiteHrefResolver {

    private GuideSiteHrefResolver() {}

    public static String resolveRawHref(@Nullable ResourceLocation currentPageId, String href) {
        if (href == null || href.isEmpty() || currentPageId == null) {
            return href;
        }

        URI uri;
        try {
            uri = URI.create(href);
        } catch (Exception ignored) {
            uri = null;
        }

        if (uri != null && uri.isAbsolute()) {
            return href;
        }

        String fragment = null;
        String target = href;
        int fragmentSep = href.indexOf('#');
        if (fragmentSep >= 0) {
            fragment = href.substring(fragmentSep + 1);
            target = href.substring(0, fragmentSep);
        }

        try {
            ResourceLocation targetPageId = IdUtils.resolveLink(target, currentPageId);
            return resolvePageAnchor(currentPageId, new PageAnchor(targetPageId, fragment));
        } catch (IllegalArgumentException ignored) {
            return href;
        }
    }

    public static String resolvePageAnchor(@Nullable ResourceLocation currentPageId, PageAnchor anchor) {
        if (anchor == null) {
            return "";
        }

        ResourceLocation targetPageId = anchor.pageId();
        if (targetPageId == null) {
            return anchor.anchor() != null ? "#" + anchor.anchor() : "";
        }

        if (currentPageId != null && currentPageId.equals(targetPageId) && anchor.anchor() != null
            && !anchor.anchor()
                .isEmpty()) {
            return "#" + anchor.anchor();
        }

        String relative = relativizePagePath(currentPageId, targetPageId);
        if (anchor.anchor() != null && !anchor.anchor()
            .isEmpty()) {
            return relative + "#" + anchor.anchor();
        }
        return relative;
    }

    public static String headingAnchor(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
            .trim()
            .replaceAll("\\s+", "-");
    }

    public static String outputPageFile(ResourceLocation pageId) {
        String path = pageId.getResourcePath();
        if (path.endsWith(".md")) {
            return path.substring(0, path.length() - 3) + ".html";
        }
        return path + ".html";
    }

    private static String relativizePagePath(@Nullable ResourceLocation currentPageId, ResourceLocation targetPageId) {
        Path target = Paths.get(outputPageFile(targetPageId));
        Path current = currentPageId != null ? Paths.get(outputPageFile(currentPageId)) : null;
        Path currentDir = current != null ? current.getParent() : null;
        String relative = currentDir != null ? currentDir.relativize(target)
            .toString() : target.toString();
        return relative.replace('\\', '/');
    }
}
