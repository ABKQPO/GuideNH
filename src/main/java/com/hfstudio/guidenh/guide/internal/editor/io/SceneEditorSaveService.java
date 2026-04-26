package com.hfstudio.guidenh.guide.internal.editor.io;

import java.nio.file.Path;
import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;

import com.hfstudio.guidenh.guide.internal.editor.SceneEditorSession;

public final class SceneEditorSaveService {

    private final SceneEditorStructureCache structureCache;
    private final SceneEditorClipboardExporter clipboardExporter;

    public SceneEditorSaveService(SceneEditorStructureCache structureCache,
        SceneEditorClipboardExporter clipboardExporter) {
        this.structureCache = structureCache;
        this.clipboardExporter = clipboardExporter;
    }

    public SaveResult save(SceneEditorSession session, @Nullable EntityPlayer player) {
        String serialized = session.getLastAppliedText();
        Optional<Path> writtenStructurePath = Optional.empty();
        try {
            String importedStructureSnbt = session.getImportedStructureSnbt();
            if (importedStructureSnbt != null && !importedStructureSnbt.isEmpty()) {
                writtenStructurePath = structureCache.resolveStructureCachePath(session);
                if (writtenStructurePath.isPresent()) {
                    structureCache.writeStructureCache(writtenStructurePath.get(), importedStructureSnbt);
                }
            }
            clipboardExporter.export(player, serialized);
            if (serialized.equals(session.getRawText())) {
                session.markSaved(serialized);
            } else {
                session.markAppliedSaved(serialized);
            }
            return SaveResult.success(serialized, writtenStructurePath);
        } catch (Exception e) {
            clipboardExporter.notifyFailure(player, e);
            return SaveResult.failure(e, writtenStructurePath);
        }
    }

    public static final class SaveResult {

        private final boolean success;
        private final String savedText;
        private final Optional<Path> structurePath;
        @Nullable
        private final Throwable error;

        private SaveResult(boolean success, String savedText, Optional<Path> structurePath, @Nullable Throwable error) {
            this.success = success;
            this.savedText = savedText;
            this.structurePath = structurePath;
            this.error = error;
        }

        private static SaveResult success(String savedText, Optional<Path> structurePath) {
            return new SaveResult(true, savedText, structurePath, null);
        }

        private static SaveResult failure(Throwable error, Optional<Path> structurePath) {
            return new SaveResult(false, "", structurePath, error);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getSavedText() {
            return savedText;
        }

        public Optional<Path> getStructurePath() {
            return structurePath;
        }

        @Nullable
        public Throwable getError() {
            return error;
        }
    }
}
