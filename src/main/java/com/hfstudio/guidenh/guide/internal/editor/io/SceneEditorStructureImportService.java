package com.hfstudio.guidenh.guide.internal.editor.io;

import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.jetbrains.annotations.Nullable;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

public class SceneEditorStructureImportService {

    public static final String SNBT_EXTENSION = ".snbt";
    public static final Executor IMPORT_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "GuideNH-SceneEditor-StructureImport");
        thread.setDaemon(true);
        return thread;
    });

    private final SceneEditorStructureCache structureCache;
    private final StructureFileChooser fileChooser;
    private final StructureTextReader structureTextReader;
    private final Executor importExecutor;

    public SceneEditorStructureImportService(SceneEditorStructureCache structureCache) {
        this.structureCache = structureCache;
        this.fileChooser = this::chooseStructureFileOnDialogThread;
        this.structureTextReader = path -> new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        this.importExecutor = IMPORT_EXECUTOR;
    }

    SceneEditorStructureImportService(SceneEditorStructureCache structureCache, StructureFileChooser fileChooser,
        StructureTextReader structureTextReader, Executor importExecutor) {
        this.structureCache = structureCache;
        this.fileChooser = fileChooser;
        this.structureTextReader = structureTextReader;
        this.importExecutor = importExecutor;
    }

    @Nullable
    public ImportResult chooseAndImport(String dialogTitle) throws Exception {
        Path selectedFile = fileChooser.choose(dialogTitle);
        if (selectedFile == null) {
            return null;
        }

        String snbtText = structureTextReader.read(selectedFile);
        validateSnbt(snbtText);
        return new ImportResult(
            structureCache.createStructureSource(),
            snbtText,
            selectedFile.toAbsolutePath()
                .normalize()
                .toString());
    }

    public CompletableFuture<ImportResult> chooseAndImportAsync(String dialogTitle) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return chooseAndImport(dialogTitle);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, importExecutor);
    }

    @Nullable
    private Path chooseStructureFileOnDialogThread(String dialogTitle) throws Exception {
        if (EventQueue.isDispatchThread()) {
            return chooseStructureFile(dialogTitle);
        }
        CompletableFuture<Path> future = new CompletableFuture<>();
        EventQueue.invokeLater(() -> {
            try {
                future.complete(chooseStructureFile(dialogTitle));
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future.get();
    }

    @Nullable
    private Path chooseStructureFile(String dialogTitle) {
        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException("Graphical file dialogs are not available");
        }
        return isMacOs() ? chooseWithFileDialog(dialogTitle) : chooseWithSwingChooser(dialogTitle);
    }

    @Nullable
    private Path chooseWithFileDialog(String dialogTitle) {
        FileDialog dialog = new FileDialog((Frame) null, dialogTitle, FileDialog.LOAD);
        dialog.setFilenameFilter((dir, name) -> isSnbtFileName(name));
        dialog.setFile("*" + SNBT_EXTENSION);
        dialog.setVisible(true);
        try {
            String directory = dialog.getDirectory();
            String fileName = dialog.getFile();
            if (directory == null || fileName == null || !isSnbtFileName(fileName)) {
                return null;
            }
            return new File(directory, fileName).toPath();
        } finally {
            dialog.dispose();
        }
    }

    @Nullable
    private Path chooseWithSwingChooser(String dialogTitle) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(dialogTitle);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(new FileNameExtensionFilter("SNBT (*.snbt)", "snbt"));
        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        File selectedFile = chooser.getSelectedFile();
        if (selectedFile == null || !isSnbtFileName(selectedFile.getName())) {
            return null;
        }
        return selectedFile.toPath();
    }

    private void validateSnbt(String snbtText) throws Exception {
        String normalized = snbtText;
        if (!normalized.isEmpty() && normalized.charAt(0) == '\uFEFF') {
            normalized = normalized.substring(1);
        }
        normalized = normalized.trim();
        NBTBase parsed = JsonToNBT.func_150315_a(normalized);
        if (!(parsed instanceof NBTTagCompound)) {
            throw new IllegalArgumentException("SNBT root must be a Compound");
        }
    }

    private boolean isMacOs() {
        String osName = System.getProperty("os.name", "");
        return osName.toLowerCase(Locale.ROOT)
            .contains("mac");
    }

    private boolean isSnbtFileName(String fileName) {
        return fileName != null && fileName.toLowerCase(Locale.ROOT)
            .endsWith(SNBT_EXTENSION);
    }

    @FunctionalInterface
    interface StructureFileChooser {

        @Nullable
        Path choose(String dialogTitle) throws Exception;
    }

    @FunctionalInterface
    interface StructureTextReader {

        String read(Path path) throws Exception;
    }

    public static class ImportResult {

        private final String structureSource;
        private final String structureText;
        private final String displayPath;

        private ImportResult(String structureSource, String structureText, String displayPath) {
            this.structureSource = structureSource;
            this.structureText = structureText;
            this.displayPath = displayPath;
        }

        public String getStructureSource() {
            return structureSource;
        }

        public String getStructureText() {
            return structureText;
        }

        public String getDisplayPath() {
            return displayPath;
        }
    }
}
