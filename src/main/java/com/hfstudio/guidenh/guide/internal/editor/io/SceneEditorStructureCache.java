package com.hfstudio.guidenh.guide.internal.editor.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

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
        if (structureSource == null || structureSource.isEmpty()) {
            return Optional.empty();
        }

        Path path = Paths.get(structureSource);
        if (!path.isAbsolute()) {
            path = workingRoot.resolve(path);
        }
        return Optional.of(path.normalize());
    }

    public void writeStructureCache(Path path, String snbtText) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(path, snbtText.getBytes(StandardCharsets.UTF_8));
    }
}
