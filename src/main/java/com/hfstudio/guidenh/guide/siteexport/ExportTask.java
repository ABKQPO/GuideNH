package com.hfstudio.guidenh.guide.siteexport;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;

public class ExportTask {

    private static final Logger LOG = LoggerFactory.getLogger(ExportTask.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
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
                LOG.warn("Failed to export page {}", page.getId(), t);
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
                    LOG.debug("Skipping missing asset {}", id, e);
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
        Files.write(
            outDir.resolve("index.json"),
            GSON.toJson(index)
                .getBytes(StandardCharsets.UTF_8));

        return new Result(ok, failed, assetsCopied, outDir);
    }

    public ResourceExporter getExporter() {
        return collector;
    }

    public static class Result {

        public final int pagesExported;
        public final int pagesFailed;
        public final int assetsCopied;
        public final Path outDir;

        Result(int pagesExported, int pagesFailed, int assetsCopied, Path outDir) {
            this.pagesExported = pagesExported;
            this.pagesFailed = pagesFailed;
            this.assetsCopied = assetsCopied;
            this.outDir = outDir;
        }
    }

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
