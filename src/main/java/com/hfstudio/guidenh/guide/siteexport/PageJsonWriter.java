package com.hfstudio.guidenh.guide.siteexport;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hfstudio.guidenh.guide.compiler.Frontmatter;
import com.hfstudio.guidenh.guide.compiler.FrontmatterNavigation;
import com.hfstudio.guidenh.guide.compiler.NavigationIconEntry;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;

/**
 *
 * <pre>
 * {
 *   "id":        "guidenh:index.md",
 *   "pack":      "guidenh",
 *   "language":  "en_us",
 *   "source":    "…raw markdown…",
 *   "frontmatter": {
 *     "navigation": {
 *       "title":       "...",
 *       "parent":      "guidenh:category",
 *       "position":    10,
 *       "iconItemId":  "minecraft:chest",
 *       "iconItemMeta": 0,
 *       "iconTextureId":"guidenh:test1.png",
 *       "icons":       [{"itemId":"minecraft:wool","meta":1},{"itemId":"minecraft:wool","meta":14}],
 *       "iconTextures":["guidenh:tex1.png","guidenh:tex2.png"]
 *     },
 *     "extra": { … additionalProperties … }
 *   }
 * }
 * </pre>
 */
public class PageJsonWriter {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .disableHtmlEscaping()
        .serializeNulls()
        .create();

    private PageJsonWriter() {}

    public static void write(ParsedGuidePage page, Path out) throws IOException {
        Files.createDirectories(out.getParent());
        try (BufferedWriter w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            GSON.toJson(toJson(page), w);
        }
    }

    public static Map<String, Object> toJson(ParsedGuidePage page) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put(
            "id",
            page.getId()
                .toString());
        root.put("pack", page.getSourcePack());
        root.put("language", page.getLanguage());
        root.put("source", page.getSource());
        root.put("frontmatter", frontmatterToJson(page.getFrontmatter()));
        return root;
    }

    public static Map<String, Object> frontmatterToJson(@Nullable Frontmatter fm) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (fm == null) {
            out.put("navigation", null);
            out.put("extra", new LinkedHashMap<>());
            return out;
        }
        FrontmatterNavigation nav = fm.navigationEntry();
        if (nav == null) {
            out.put("navigation", null);
        } else {
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("title", nav.title());
            n.put(
                "parent",
                nav.parent() == null ? null
                    : nav.parent()
                        .toString());
            n.put("position", nav.position());
            n.put("iconItemId", nav.iconItemId());
            n.put("iconItemMeta", nav.iconItemMeta());
            n.put(
                "iconTextureId",
                nav.iconTextureId() == null ? null
                    : nav.iconTextureId()
                        .toString());

            if (nav.iconEntries() != null && !nav.iconEntries()
                .isEmpty()) {
                List<Map<String, Object>> iconsList = new ArrayList<>();
                for (NavigationIconEntry entry : nav.iconEntries()) {
                    Map<String, Object> e = new LinkedHashMap<>();
                    e.put("itemId", entry.itemId());
                    e.put("meta", entry.meta());
                    iconsList.add(e);
                }
                n.put("icons", iconsList);
            } else {
                n.put("icons", null);
            }

            if (nav.iconTextureEntries() != null && !nav.iconTextureEntries()
                .isEmpty()) {
                List<String> texList = new ArrayList<>();
                for (var texId : nav.iconTextureEntries()) {
                    texList.add(texId.toString());
                }
                n.put("iconTextures", texList);
            } else {
                n.put("iconTextures", null);
            }

            out.put("navigation", n);
        }
        out.put("extra", fm.additionalProperties() == null ? new LinkedHashMap<>() : fm.additionalProperties());
        return out;
    }
}
