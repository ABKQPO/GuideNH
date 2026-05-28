package com.hfstudio.guidenh.guide.internal.editor.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.editor.SceneEditorSession;

public class SceneEditorStructureCache {

    private final Path workingRoot;

    public SceneEditorStructureCache(Path workingRoot) {
        this.workingRoot = workingRoot;
    }

    public static SceneEditorStructureCache createDefault() {
        return new SceneEditorStructureCache(Paths.get(""));
    }

    public String createStructureSource() {
        return Paths.get("config", "guidenh", "scene-editor", UUID.randomUUID() + ".snbt")
            .toString()
            .replace('\\', '/');
    }

    public Optional<Path> resolveStructureCachePath(SceneEditorSession session) {
        return resolveStructureCachePath(
            session.getSceneModel()
                .getStructureSource());
    }

    public Optional<Path> resolveStructureCachePath(@Nullable String structureSource) {
        return resolveSceneStructurePath(workingRoot, structureSource);
    }

    public void writeStructureCache(Path path, String snbtText) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, snbtText);
    }

    public static Optional<Path> resolveSceneStructurePath(Path workingRoot, @Nullable String structureSource) {
        if (structureSource == null || structureSource.trim()
            .isEmpty()) {
            return Optional.empty();
        }

        Path path = Paths.get(structureSource.trim());
        if (path.isAbsolute()) {
            return Optional.of(
                path.toAbsolutePath()
                    .normalize());
        }

        Path normalizedRoot = workingRoot.toAbsolutePath()
            .normalize();
        Path resolved = normalizedRoot.resolve(path)
            .normalize();
        if (!resolved.startsWith(normalizedRoot)) {
            return Optional.empty();
        }
        return Optional.of(resolved);
    }
}
