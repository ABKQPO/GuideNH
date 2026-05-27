package com.hfstudio.guidenh.guide.siteexport.site;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.compiler.IdUtils;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiExternalLinkSupport;

import cpw.mods.fml.common.FMLLog;

public class GuideSiteHrefResolver {

    private static final ThreadLocal<ExportContext> EXPORT_CONTEXT = new ThreadLocal<>();

    private GuideSiteHrefResolver() {}

    public static ContextScope exportContext(String namespace, String guidePath, String language,
        @Nullable Map<ResourceLocation, ResourceLocation> guideIdsByPageId) {
        ExportContext previous = EXPORT_CONTEXT.get();
        EXPORT_CONTEXT.set(new ExportContext(namespace, guidePath, language, guideIdsByPageId));
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
            ResourceLocation targetPageId = target.isEmpty() ? currentPageId
                : IdUtils.resolveLink(target, currentPageId);
            return resolvePageAnchor(currentPageId, new PageAnchor(targetPageId, fragment));
        } catch (IllegalArgumentException e) {
            FMLLog.getLogger()
                .debug(
                    "[GuideNH] [GuideSiteHrefResolver] Failed to resolve href {} from page {}",
                    href,
                    currentPageId,
                    e);
            return href;
        }
    }

    public static String resolvePageAnchor(@Nullable ResourceLocation currentPageId, PageAnchor anchor) {
        return resolvePageAnchor(currentPageId, null, anchor);
    }

    public static String resolveExternalConfirmHref(@Nullable ResourceLocation currentPageId, String targetUrl,
        @Nullable String label) {
        var externalUri = MediaWikiExternalLinkSupport.resolveExternalUri(targetUrl);
        if (externalUri == null) {
            return "";
        }
        String normalizedUrl = externalUri.toString();
        StringBuilder href = new StringBuilder(resolveSitePath(currentPageId, "_site/external-link.html"));
        href.append("?target=")
            .append(urlEncode(normalizedUrl));
        if (label != null && !label.trim()
            .isEmpty()) {
            href.append("&label=")
                .append(urlEncode(label.trim()));
        }
        ExportContext exportContext = EXPORT_CONTEXT.get();
        if (exportContext != null && exportContext.language != null && !exportContext.language.isEmpty()) {
            href.append("&lang=")
                .append(urlEncode(exportContext.language));
        }
        return href.toString();
    }

    public static String resolvePageAnchor(@Nullable ResourceLocation currentPageId, @Nullable ResourceLocation guideId,
        PageAnchor anchor) {
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

        String relative = resolvePagePath(currentPageId, guideId, targetPageId);
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
        return collapseWhitespaceToDashes(
            text.toLowerCase(Locale.ROOT)
                .trim());
    }

    private static String collapseWhitespaceToDashes(String text) {
        StringBuilder builder = new StringBuilder(text.length());
        boolean previousWhitespace = false;
        for (int index = 0; index < text.length(); index++) {
            char value = text.charAt(index);
            if (Character.isWhitespace(value)) {
                if (!previousWhitespace) {
                    builder.append('-');
                    previousWhitespace = true;
                }
            } else {
                builder.append(value);
                previousWhitespace = false;
            }
        }
        return builder.toString();
    }

    public static String outputPageFile(ResourceLocation pageId) {
        String path = pageId.getResourcePath();
        if (path.endsWith(".md")) {
            return path.substring(0, path.length() - 3) + ".html";
        }
        return path + ".html";
    }

    private static String resolvePagePath(@Nullable ResourceLocation currentPageId, @Nullable ResourceLocation guideId,
        ResourceLocation targetPageId) {
        ExportContext exportContext = EXPORT_CONTEXT.get();
        if (exportContext != null) {
            return exportContext.pageUrl(resolveTargetGuideId(guideId, targetPageId), targetPageId);
        }
        return relativizePagePath(currentPageId, targetPageId);
    }

    private static ResourceLocation resolveTargetGuideId(@Nullable ResourceLocation guideId, ResourceLocation pageId) {
        if (guideId != null) {
            return guideId;
        }

        ExportContext exportContext = EXPORT_CONTEXT.get();
        if (exportContext != null) {
            ResourceLocation mappedGuideId = exportContext.guideIdsByPageId.get(pageId);
            if (mappedGuideId != null) {
                return mappedGuideId;
            }
        }

        return new ResourceLocation(pageId.getResourceDomain(), "guidenh");
    }

    private static String resolveSitePath(@Nullable ResourceLocation currentPageId, String targetPath) {
        ExportContext exportContext = EXPORT_CONTEXT.get();
        if (exportContext != null) {
            return exportContext.siteUrl(currentPageId, targetPath);
        }
        return targetPath;
    }

    private static String relativizePagePath(@Nullable ResourceLocation currentPageId, ResourceLocation targetPageId) {
        Path target = Paths.get(outputPageFile(targetPageId));
        Path current = currentPageId != null ? Paths.get(outputPageFile(currentPageId)) : null;
        Path currentDir = current != null ? current.getParent() : null;
        String relative = currentDir != null ? currentDir.relativize(target)
            .toString() : target.toString();
        return relative.replace('\\', '/');
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 should always be supported", e);
        }
    }

    public static class ContextScope implements AutoCloseable {

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

    private static class ExportContext {

        private final String namespace;
        private final String guidePath;
        private final String language;
        private final Map<ResourceLocation, ResourceLocation> guideIdsByPageId;

        private ExportContext(String namespace, String guidePath, String language,
            @Nullable Map<ResourceLocation, ResourceLocation> guideIdsByPageId) {
            this.namespace = namespace;
            this.guidePath = guidePath;
            this.language = language;
            if (guideIdsByPageId == null || guideIdsByPageId.isEmpty()) {
                this.guideIdsByPageId = Collections.emptyMap();
            } else {
                this.guideIdsByPageId = Collections.unmodifiableMap(new LinkedHashMap<>(guideIdsByPageId));
            }
        }

        private String pageUrl(ResourceLocation guideId, ResourceLocation pageId) {
            String targetNamespace = guideId != null ? guideId.getResourceDomain() : namespace;
            String targetGuidePath = guideId != null ? guideId.getResourcePath() : guidePath;
            return "guides/" + targetNamespace
                + "/"
                + targetGuidePath
                + "/"
                + language
                + "/"
                + outputPageFile(pageId).replace('\\', '/');
        }

        private String siteUrl(@Nullable ResourceLocation currentPageId, String targetPath) {
            Path target = Paths.get(targetPath);
            if (currentPageId == null) {
                return target.toString()
                    .replace('\\', '/');
            }
            Path current = Paths.get("guides", namespace, guidePath, language, outputPageFile(currentPageId));
            Path currentDir = current.getParent();
            String relative = currentDir != null ? currentDir.relativize(target)
                .toString() : target.toString();
            return relative.replace('\\', '/');
        }
    }
}
