package com.hfstudio.structurelibexport;

import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class StructureLibExportManifestWriter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .disableHtmlEscaping()
        .create();

    public Path write(Path outputDirectory, StructureLibExportManifest manifest) throws Exception {
        Files.createDirectories(outputDirectory);
        Path target = outputDirectory.resolve("manifest.json");
        try (Writer writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            GSON.toJson(manifest, writer);
        }
        return target;
    }
}
