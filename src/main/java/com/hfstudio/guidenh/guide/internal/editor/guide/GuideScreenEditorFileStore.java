package com.hfstudio.guidenh.guide.internal.editor.guide;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;

public final class GuideScreenEditorFileStore {

    private final Path resourcePacksRoot;
    private final Path packRoot;
    private final Path packMetaPath;

    public GuideScreenEditorFileStore(Path resourcePacksRoot, Path packRoot) {
        this.resourcePacksRoot = resourcePacksRoot;
        this.packRoot = packRoot;
        this.packMetaPath = packRoot.resolve("pack.mcmeta");
    }

    public static GuideScreenEditorFileStore createDefault() {
        Path resourcePacks = Minecraft.getMinecraft().mcDataDir.toPath()
            .resolve("resourcepacks");
        Path packRoot = resourcePacks.resolve("NewGuide");
        return new GuideScreenEditorFileStore(resourcePacks, packRoot);
    }

    public Path getPackRoot() {
        return packRoot;
    }

    public Path resolvePagePath(MutableGuide guide, ResourceLocation pageId, String language) {
        String namespace = guide.getDefaultNamespace();
        String folder = guide.getContentRootFolder();
        String langFolder = "_" + LangUtil.normalizeLanguage(language != null ? language : guide.getDefaultLanguage());
        String pageFileName = normalizePageFileName(pageId.getResourcePath());
        return packRoot.resolve("assets")
            .resolve(namespace)
            .resolve(folder)
            .resolve(langFolder)
            .resolve(pageFileName);
    }

    public void ensurePackStructure() throws IOException {
        Files.createDirectories(packRoot);
        Files.createDirectories(resourcePacksRoot);
        if (!Files.exists(packMetaPath)) {
            Files.write(packMetaPath, buildPackMeta().getBytes(StandardCharsets.UTF_8));
        }
    }

    public void savePage(MutableGuide guide, ResourceLocation pageId, String language, String text) throws IOException {
        ensurePackStructure();
        Path pagePath = resolvePagePath(guide, pageId, language);
        Files.createDirectories(pagePath.getParent());
        Files.write(pagePath, text.getBytes(StandardCharsets.UTF_8));
    }

    @Nullable
    public String readPageText(MutableGuide guide, ResourceLocation pageId, String language) {
        Path pagePath = resolvePagePath(guide, pageId, language);
        if (!Files.isRegularFile(pagePath)) {
            return null;
        }
        try {
            byte[] bytes = Files.readAllBytes(pagePath);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    public boolean hasPage(MutableGuide guide, ResourceLocation pageId, String language) {
        return Files.isRegularFile(resolvePagePath(guide, pageId, language));
    }

    private String buildPackMeta() {
        return "{\n" + "  \"pack\": {\n"
            + "    \"pack_format\": 1,\n"
            + "    \"description\": \"NewGuide\"\n"
            + "  }\n"
            + "}\n";
    }

    private String normalizePageFileName(String pagePath) {
        if (pagePath == null || pagePath.trim()
            .isEmpty()) {
            return "index.md";
        }
        String normalized = pagePath.replace('\\', '/');
        if (normalized.endsWith(".md")) {
            return normalized;
        }
        return normalized + ".md";
    }
}
