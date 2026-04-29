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

    private static final ThreadLocal<ExportContext> EXPORT_CONTEXT = new ThreadLocal<ExportContext>();

    private GuideSiteHrefResolver() {}

    public static ContextScope exportContext(String namespace, String guidePath, String language) {
        ExportContext previous = EXPORT_CONTEXT.get();
        EXPORT_CONTEXT.set(new ExportContext(namespace, guidePath, language));
        return new ContextScope(previous);
    }

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

        if (currentPageId != null && currentPageId.equals(targetPageId)
            && anchor.anchor() != null
            && !anchor.anchor()
                .isEmpty()) {
            return "#" + anchor.anchor();
        }

        String relative = resolvePagePath(currentPageId, targetPageId);
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

    private static String resolvePagePath(@Nullable ResourceLocation currentPageId, ResourceLocation targetPageId) {
        ExportContext exportContext = EXPORT_CONTEXT.get();
        if (exportContext != null) {
            return exportContext.pageUrl(targetPageId);
        }
        return relativizePagePath(currentPageId, targetPageId);
    }

    private static String relativizePagePath(@Nullable ResourceLocation currentPageId, ResourceLocation targetPageId) {
        Path target = Paths.get(outputPageFile(targetPageId));
        Path current = currentPageId != null ? Paths.get(outputPageFile(currentPageId)) : null;
        Path currentDir = current != null ? current.getParent() : null;
        String relative = currentDir != null ? currentDir.relativize(target)
            .toString() : target.toString();
        return relative.replace('\\', '/');
    }

    public static final class ContextScope implements AutoCloseable {

        @Nullable
        private final ExportContext previous;
        private boolean closed;

        private ContextScope(@Nullable ExportContext previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            if (previous == null) {
                EXPORT_CONTEXT.remove();
            } else {
                EXPORT_CONTEXT.set(previous);
            }
        }
    }

    private static final class ExportContext {

        private final String namespace;
        private final String guidePath;
        private final String language;

        private ExportContext(String namespace, String guidePath, String language) {
            this.namespace = namespace;
            this.guidePath = guidePath;
            this.language = language;
        }

        private String pageUrl(ResourceLocation pageId) {
            return "guides/" + namespace
                + "/"
                + guidePath
                + "/"
                + language
                + "/"
                + outputPageFile(pageId).replace('\\', '/');
        }
    }
}
