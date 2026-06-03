package com.hfstudio.guidenh.guide.siteexport;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

import com.github.bsideup.jabel.Desugar;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;

import cpw.mods.fml.common.FMLLog;

public class ExportTask {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .disableHtmlEscaping()
        .serializeNulls()
        .create();

    private final Guide guide;
    private final Path outDir;
    private final Collector collector = new Collector();

    public ExportTask(Guide guide, Path outDir) {
        this.guide = guide;
        this.outDir = outDir;
    }

    public Result run() throws IOException {
        Files.createDirectories(outDir);
        Path pagesDir = outDir.resolve("pages");
        Files.createDirectories(pagesDir);

        List<String> pageIds = new ArrayList<>();
        int ok = 0;
        int failed = 0;

        for (ParsedGuidePage page : guide.getPages()) {
            Path rel = pagesDir.resolve(
                page.getId()
                    .getResourceDomain())
                .resolve(
                    page.getId()
                        .getResourcePath() + ".json");
            try {
                PageJsonWriter.write(page, rel);
                pageIds.add(
                    page.getId()
                        .toString());
                ok++;
            } catch (Throwable t) {
                FMLLog.getLogger()
                    .warn("[GuideNH] [ExportTask] Failed to export page {}", page.getId(), t);
                failed++;
            }
        }

        int assetsCopied = 0;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.getResourceManager() != null) {
            Path assetsDir = outDir.resolve("assets");
            for (ResourceLocation id : collector.textures) {
                try {
                    IResource res = mc.getResourceManager()
                        .getResource(id);
                    Path dest = assetsDir.resolve(id.getResourceDomain())
                        .resolve(id.getResourcePath());
                    Files.createDirectories(dest.getParent());
                    try (InputStream in = res.getInputStream()) {
                        Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                    }
                    assetsCopied++;
                } catch (IOException e) {
                    FMLLog.getLogger()
                        .debug("[GuideNH] [ExportTask] Skipping missing asset {}", id, e);
                }
            }
        }

        // index.json
        Map<String, Object> index = new LinkedHashMap<>();
        index.put(
            "guideId",
            guide.getId()
                .toString());
        index.put("pages", pageIds);
        index.put("assetsCopied", assetsCopied);
        Files.writeString(outDir.resolve("index.json"), GSON.toJson(index));

        return new Result(ok, failed, assetsCopied, outDir);
    }

    public ResourceExporter getExporter() {
        return collector;
    }

    @Desugar
    public record Result(int pagesExported, int pagesFailed, int assetsCopied, Path outDir) {}

    public class Collector implements ResourceExporter {

        final Set<ResourceLocation> textures = new HashSet<>();
        final Set<Item> items = new HashSet<>();

        @Override
        public Path getOutDir() {
            return outDir;
        }

        @Override
        public void referenceTexture(ResourceLocation textureId) {
            if (textureId != null) textures.add(textureId);
        }

        @Override
        public void referenceItem(Item item) {
            if (item != null) items.add(item);
        }
    }
}
