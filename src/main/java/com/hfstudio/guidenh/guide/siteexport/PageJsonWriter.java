package com.hfstudio.guidenh.guide.siteexport;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hfstudio.guidenh.guide.compiler.Frontmatter;
import com.hfstudio.guidenh.guide.compiler.FrontmatterNavigation;
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
 *       "iconTextureId":"guidenh:test1.png"
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
            n.put(
                "iconItemId",
                nav.iconItemId() == null ? null
                    : nav.iconItemId()
                        .toString());
            n.put(
                "iconTextureId",
                nav.iconTextureId() == null ? null
                    : nav.iconTextureId()
                        .toString());
            out.put("navigation", n);
        }
        out.put("extra", fm.additionalProperties() == null ? new LinkedHashMap<>() : fm.additionalProperties());
        return out;
    }
}
