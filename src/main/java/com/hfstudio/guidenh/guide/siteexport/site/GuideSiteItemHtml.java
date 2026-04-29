package com.hfstudio.guidenh.guide.siteexport.site;

import org.jetbrains.annotations.Nullable;

final class GuideSiteItemHtml {

    private GuideSiteItemHtml() {}

    static void appendIcon(StringBuilder html, GuideSiteExportedItem item, @Nullable String extraClass) {
        String classes = classes("item-icon", extraClass);
        String label = item.displayName()
            .isEmpty() ? item.itemId() : item.displayName();
        if (item.hasIcon()) {
            html.append("<img class=\"")
                .append(escapeHtml(classes))
                .append("\" src=\"")
                .append(escapeHtml(item.iconSrc()))
                .append("\" alt=\"")
                .append(escapeHtml(label))
                .append("\" data-item-id=\"")
                .append(escapeHtml(item.itemId()))
                .append("\" width=\"32\" height=\"32\" decoding=\"async\">");
            return;
        }

        html.append("<span class=\"")
            .append(escapeHtml(classes("item-icon item-icon-fallback", extraClass)))
            .append("\" data-item-id=\"")
            .append(escapeHtml(item.itemId()))
            .append("\">")
            .append(escapeHtml(label))
            .append("</span>");
    }

    static void appendSummaryContent(StringBuilder html, GuideSiteExportedItem item, @Nullable String iconClass,
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

    static String escapeHtml(String text) {
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
