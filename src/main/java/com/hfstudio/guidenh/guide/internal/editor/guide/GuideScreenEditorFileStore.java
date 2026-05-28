package com.hfstudio.guidenh.guide.internal.editor.guide;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.internal.datadriven.DataDrivenGuideLoader;
import com.hfstudio.guidenh.guide.internal.datadriven.GuidePageResourceSelector;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;

public class GuideScreenEditorFileStore {

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
        return resolvePagePath(guide, pageId, language, null);
    }

    public Path resolvePagePath(MutableGuide guide, ResourceLocation pageId, String language,
        @Nullable ResourceLocation sourcePageId) {
        return buildPagePath(resolveWritablePackRoot(guide, sourcePageId, language), guide, pageId, language);
    }

    private Path buildPagePath(Path root, MutableGuide guide, ResourceLocation pageId, String language) {
        String namespace = guide.getDefaultNamespace();
        String folder = guide.getContentRootFolder();
        String langFolder = "_" + LangUtil.normalizeLanguage(language != null ? language : guide.getDefaultLanguage());
        String pageFileName = normalizePageFileName(pageId.getResourcePath());
        return root.resolve("assets")
            .resolve(namespace)
            .resolve(folder)
            .resolve(langFolder)
            .resolve(pageFileName);
    }

    public Path buildPagePathInRoot(Path root, MutableGuide guide, ResourceLocation pageId, String language) {
        return buildPagePath(root, guide, pageId, language);
    }

    public void ensurePackStructure() throws IOException {
        Files.createDirectories(packRoot);
        Files.createDirectories(resourcePacksRoot);
        if (!Files.exists(packMetaPath)) {
            Files.writeString(packMetaPath, buildPackMeta());
        }
    }

    public void savePage(MutableGuide guide, ResourceLocation pageId, String language, String text) throws IOException {
        savePage(guide, pageId, language, text, null);
    }

    public void savePage(MutableGuide guide, ResourceLocation pageId, String language, String text,
        @Nullable ResourceLocation sourcePageId) throws IOException {
        Path targetPackRoot = resolveWritablePackRoot(guide, sourcePageId, language);
        if (targetPackRoot.equals(packRoot)) {
            ensurePackStructure();
        }
        Path pagePath = buildPagePath(targetPackRoot, guide, pageId, language);
        Files.createDirectories(pagePath.getParent());
        Files.writeString(pagePath, text);
    }

    public void savePageInRoot(Path root, MutableGuide guide, ResourceLocation pageId, String language, String text)
        throws IOException {
        Path pagePath = buildPagePath(root, guide, pageId, language);
        Files.createDirectories(pagePath.getParent());
        Files.writeString(pagePath, text);
    }

    public boolean canSaveBesideSource(MutableGuide guide, @Nullable ResourceLocation sourcePageId, String language) {
        return sourcePageId != null && findWritableResourcePackRootContaining(guide, sourcePageId, language) != null;
    }

    @Nullable
    public Path findWritablePageResourcePackRoot(MutableGuide guide, ResourceLocation pageId, String language) {
        return findWritableResourcePackRootContaining(guide, pageId, language);
    }

    @Nullable
    public String readPageText(MutableGuide guide, ResourceLocation pageId, String language) {
        Path pagePath = resolveExistingWritablePagePath(guide, pageId, language);
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
        return Files.isRegularFile(resolveExistingWritablePagePath(guide, pageId, language))
            || resourcePageExists(guide, pageId, language);
    }

    public boolean hasPage(MutableGuide guide, ResourceLocation pageId, String language,
        @Nullable ResourceLocation sourcePageId) {
        return Files.isRegularFile(resolvePagePath(guide, pageId, language, sourcePageId))
            || resourcePageExists(guide, pageId, language);
    }

    public boolean hasPageInRoot(Path root, MutableGuide guide, ResourceLocation pageId, String language) {
        return Files.isRegularFile(buildPagePath(root, guide, pageId, language))
            || resourcePageExists(guide, pageId, language);
    }

    public boolean hasWritablePage(MutableGuide guide, ResourceLocation pageId, String language) {
        return Files.isRegularFile(resolveExistingWritablePagePath(guide, pageId, language));
    }

    private Path resolveExistingWritablePagePath(MutableGuide guide, ResourceLocation pageId, String language) {
        Path selectedSourcePath = resolveExistingWritableSelectedSourcePath(guide, pageId, language);
        if (selectedSourcePath != null) {
            return selectedSourcePath;
        }
        Path fallbackPath = buildPagePath(packRoot, guide, pageId, language);
        Files.isRegularFile(fallbackPath);
        return fallbackPath;
    }

    @Nullable
    private Path resolveExistingWritableSelectedSourcePath(MutableGuide guide, ResourceLocation pageId,
        String language) {
        var resourcePacks = DataDrivenGuideLoader.getActiveResourcePacks();
        GuidePageResourceSelector.SelectedPageResource selected = GuidePageResourceSelector.selectFirstPresent(
            resourcePacks,
            toResourcePackPageId(guide, pageId, language),
            language != null && !language.equals(guide.getDefaultLanguage())
                ? toResourcePackPageId(guide, pageId, guide.getDefaultLanguage())
                : null,
            toNeutralPageId(guide, pageId));
        if (selected == null) {
            return null;
        }
        File resourcePackFile = DataDrivenGuideLoader.getResourcePackFile(selected.resourcePack());
        if (resourcePackFile == null || !resourcePackFile.isDirectory()) {
            return null;
        }
        Path root = resourcePackFile.toPath();
        if (!Files.isWritable(root)) {
            return null;
        }
        return root.resolve("assets")
            .resolve(
                selected.sourceId()
                    .getResourceDomain())
            .resolve(
                selected.sourceId()
                    .getResourcePath()
                    .replace('/', File.separatorChar));
    }

    private Path resolveWritablePackRoot(MutableGuide guide, @Nullable ResourceLocation sourcePageId, String language) {
        if (sourcePageId != null) {
            Path sourcePackRoot = findWritableResourcePackRootContaining(guide, sourcePageId, language);
            if (sourcePackRoot != null) {
                return sourcePackRoot;
            }
        }
        return packRoot;
    }

    @Nullable
    private Path findWritableResourcePackRootContaining(MutableGuide guide, ResourceLocation pageId, String language) {
        return findWritableResourcePackRootContainingAsset(
            toResourcePackPageId(guide, pageId, language),
            language != null && !language.equals(guide.getDefaultLanguage())
                ? toResourcePackPageId(guide, pageId, guide.getDefaultLanguage())
                : null,
            toNeutralPageId(guide, pageId));
    }

    @Nullable
    private Path findWritableResourcePackRootContainingAsset(ResourceLocation... assetIds) {
        var resourcePacks = DataDrivenGuideLoader.getActiveResourcePacks();
        GuidePageResourceSelector.SelectedPageResource selected = GuidePageResourceSelector
            .selectFirstPresent(resourcePacks, assetIds);
        if (selected == null) {
            return null;
        }
        IResourcePack resourcePack = selected.resourcePack();
        File resourcePackFile = DataDrivenGuideLoader.getResourcePackFile(resourcePack);
        if (resourcePackFile == null || !resourcePackFile.isDirectory()) {
            return null;
        }
        Path root = resourcePackFile.toPath();
        return Files.isWritable(root) ? root : null;
    }

    private boolean resourcePageExists(MutableGuide guide, ResourceLocation pageId, String language) {
        ResourceLocation localizedId = toResourcePackPageId(guide, pageId, language);
        ResourceLocation defaultId = language != null && !language.equals(guide.getDefaultLanguage())
            ? toResourcePackPageId(guide, pageId, guide.getDefaultLanguage())
            : null;
        return GuidePageResourceSelector.selectFirstPresent(
            DataDrivenGuideLoader.getActiveResourcePacks(),
            localizedId,
            defaultId,
            toNeutralPageId(guide, pageId)) != null;
    }

    private ResourceLocation toResourcePackPageId(MutableGuide guide, ResourceLocation pageId, String language) {
        String langFolder = "_" + LangUtil.normalizeLanguage(language != null ? language : guide.getDefaultLanguage());
        String path = guide.getContentRootFolder() + "/"
            + langFolder
            + "/"
            + normalizePageFileName(pageId.getResourcePath());
        return new ResourceLocation(guide.getDefaultNamespace(), path);
    }

    private ResourceLocation toNeutralPageId(MutableGuide guide, ResourceLocation pageId) {
        String path = guide.getContentRootFolder() + "/" + normalizePageFileName(pageId.getResourcePath());
        return new ResourceLocation(guide.getDefaultNamespace(), path);
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
