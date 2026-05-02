package com.hfstudio.guidenh.guide.siteexport.site;

import org.jetbrains.annotations.Nullable;

public final class GuideSiteItemHtml {

    /** Default rendered icon size in pixels at scale=1.0 (matches the in-game 32px nav icon). */
    public static final int BASE_ICON_PX = 32;

    private GuideSiteItemHtml() {}

    public static void appendIcon(StringBuilder html, GuideSiteExportedItem item, @Nullable String extraClass) {
        appendIcon(html, item, extraClass, 1f, false);
    }

    public static void appendIcon(StringBuilder html, GuideSiteExportedItem item, @Nullable String extraClass,
        float scale) {
        appendIcon(html, item, extraClass, scale, false);
    }

    public static void appendIcon(StringBuilder html, GuideSiteExportedItem item, @Nullable String extraClass,
        float scale, boolean nativeTooltip) {
        String classes = classes("item-icon", extraClass);
        String label = item.displayName()
            .isEmpty() ? item.itemId() : item.displayName();
        // Honor the MDX `scale` attribute on <ItemImage>: the static export emits an explicit
        // pixel size on the <img>/<span> so that scale="2" actually doubles the rendered icon
        // instead of being silently ignored. Floor at 1px to avoid invisible icons.
        float effectiveScale = scale > 0f ? scale : 1f;
        int size = Math.max(1, Math.round(BASE_ICON_PX * effectiveScale));
        String titleAttr = nativeTooltip ? " title=\"" + escapeHtml(label) + "\"" : "";
        if (item.hasIcon()) {
            // Only emit an inline `style` override when the caller supplied an explicit non-default
            // scale. At scale=1 we let the CSS `.item-icon { width: calc(16px * var(--gui-scale)) }`
            // rule control the rendered size so that recipe-slot icons automatically match the
            // gui-scale together with the surrounding slot border. An inline style would win over
            // the class rule by specificity, overriding the GUI scale and making items appear
            // smaller in high-DPI/scaled layouts.
            html.append("<img class=\"")
                .append(escapeHtml(classes))
                .append("\" src=\"")
                .append(escapeHtml(item.iconSrc()))
                .append("\" alt=\"")
                .append(escapeHtml(label))
                .append("\" data-item-id=\"")
                .append(escapeHtml(item.itemId()))
                .append("\" width=\"")
                .append(size)
                .append("\" height=\"")
                .append(size)
                .append("\"");
            if (effectiveScale != 1f) {
                html.append(" style=\"width:")
                    .append(size)
                    .append("px;height:")
                    .append(size)
                    .append("px;\"");
            }
            html.append(titleAttr)
                .append(" decoding=\"async\">");
            return;
        }

        html.append("<span class=\"")
            .append(escapeHtml(classes("item-icon item-icon-fallback", extraClass)))
            .append("\" data-item-id=\"")
            .append(escapeHtml(item.itemId()));
        if (effectiveScale != 1f) {
            html.append("\" style=\"width:")
                .append(size)
                .append("px;height:")
                .append(size)
                .append("px;font-size:")
                .append(size)
                .append("px;line-height:")
                .append(size)
                .append("px;");
        }
        html.append("\"")
            .append(titleAttr)
            .append(">")
            .append(escapeHtml(label))
            .append("</span>");
    }

    public static void appendSummaryContent(StringBuilder html, GuideSiteExportedItem item, @Nullable String iconClass,
        String textClass) {
        if (item.hasIcon()) {
            appendIcon(html, item, iconClass);
        }
        html.append("<span class=\"")
            .append(escapeHtml(textClass))
            .append("\">")
            .append(
                escapeHtml(
                    item.displayName()
                        .isEmpty() ? item.itemId() : item.displayName()))
            .append("</span>");
    }

    public static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }

    private static String classes(String baseClass, @Nullable String extraClass) {
        if (extraClass == null || extraClass.isEmpty()) {
            return baseClass;
        }
        return baseClass + " " + extraClass;
    }
}
