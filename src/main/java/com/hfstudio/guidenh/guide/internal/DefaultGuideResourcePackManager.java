package com.hfstudio.guidenh.guide.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.FileResourcePack;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.ResourcePackRepository;

import com.hfstudio.guidenh.guide.internal.datadriven.DataDrivenGuideLoader;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.mixins.early.fml.AccessorFMLClientHandler;

import cpw.mods.fml.client.FMLClientHandler;

public class DefaultGuideResourcePackManager {

    private static final String DEFAULT_GUIDE_FOLDER = "config/guidenh/DefaultGuide";
    private static final String DEFAULT_GUIDE_ZIP = "config/guidenh/DefaultGuide.zip";
    private static final String PACK_NAME = "GuideNH DefaultGuide";
    private static IResourcePack defaultGuidePack;
    private static boolean reloadPending;

    private DefaultGuideResourcePackManager() {}

    public static void init() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.mcDataDir == null) {
            return;
        }

        IResourcePack pack = ensureDefaultGuidePack(minecraft);
        if (inject(pack, minecraft)) {
            File packFile = DataDrivenGuideLoader.getLooseResourcePackRoot(pack);
            GuideDebugLog.infoAlways("Registered DefaultGuide resource pack at {}", packFile);
            reloadPending = true;
        }
    }

    public static void refreshIfPending() {
        if (!reloadPending) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null) {
            return;
        }

        reloadPending = false;
        GuideMEClientReloadDispatcher
            .dispatch(minecraft.func_152345_ab(), r -> minecraft.func_152344_a(r), minecraft::refreshResources);
    }

    private static void ensureDirectoryExists(Path root) {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create DefaultGuide directory: " + root, e);
        }
    }

    private static IResourcePack ensureDefaultGuidePack(Minecraft minecraft) {
        if (defaultGuidePack != null) {
            return defaultGuidePack;
        }

        Path zipPath = minecraft.mcDataDir.toPath()
            .resolve(DEFAULT_GUIDE_ZIP)
            .toAbsolutePath()
            .normalize();
        if (Files.isRegularFile(zipPath)) {
            defaultGuidePack = new NamedFileResourcePack(PACK_NAME, zipPath.toFile());
            return defaultGuidePack;
        }

        Path root = minecraft.mcDataDir.toPath()
            .resolve(DEFAULT_GUIDE_FOLDER)
            .toAbsolutePath()
            .normalize();
        ensureDirectoryExists(root);
        defaultGuidePack = new DirectoryResourcePack(PACK_NAME, root);
        return defaultGuidePack;
    }

    private static boolean inject(IResourcePack pack, Minecraft minecraft) {
        List<IResourcePack> basePacks = ((AccessorFMLClientHandler) FMLClientHandler.instance())
            .guidenh$getResourcePackList();
        File packFile = DataDrivenGuideLoader.getLooseResourcePackRoot(pack);
        if (basePacks == null || packFile == null || containsPack(basePacks, packFile.toPath())) {
            return false;
        }
        int index = resolveInsertIndex(basePacks, minecraft);
        if (index < 0) {
            basePacks.add(pack);
        } else {
            basePacks.add(index, pack);
        }
        return true;
    }

    private static int resolveInsertIndex(List<IResourcePack> basePacks, Minecraft minecraft) {
        ResourcePackRepository repository = minecraft.getResourcePackRepository();
        if (repository == null) {
            return -1;
        }

        List<ResourcePackRepository.Entry> assignedPacks = repository.getRepositoryEntries();
        if (assignedPacks.isEmpty()) {
            return -1;
        }

        IResourcePack firstAssignedPack = assignedPacks.get(0)
            .getResourcePack();
        return firstAssignedPack != null ? basePacks.indexOf(firstAssignedPack) : -1;
    }

    private static boolean containsPack(List<IResourcePack> packs, Path root) {
        for (IResourcePack candidate : packs) {
            File packRoot = DataDrivenGuideLoader.getLooseResourcePackRoot(candidate);
            if (packRoot != null && packRoot.toPath()
                .toAbsolutePath()
                .normalize()
                .equals(root)) {
                return true;
            }
        }
        return false;
    }

    private static class NamedFileResourcePack extends FileResourcePack {

        private final String packName;

        public NamedFileResourcePack(String packName, File file) {
            super(file);
            this.packName = packName;
        }

        @Override
        public String getPackName() {
            return packName;
        }
    }
}
