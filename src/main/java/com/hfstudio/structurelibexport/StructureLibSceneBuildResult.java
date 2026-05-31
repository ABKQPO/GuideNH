package com.hfstudio.structurelibexport;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.integration.structurelib.StructureLibSceneMetadata;

import lombok.Getter;

@Getter
public class StructureLibSceneBuildResult {

    private final boolean success;
    private final GuidebookLevel level;
    private final List<String> warnings;
    private final List<String> errors;
    @Nullable
    private final StructureLibSceneMetadata metadata;

    public StructureLibSceneBuildResult(boolean success, GuidebookLevel level, List<String> warnings,
        List<String> errors, @Nullable StructureLibSceneMetadata metadata) {
        this.success = success;
        this.level = level != null ? level : new GuidebookLevel();
        this.warnings = immutableCopy(warnings);
        this.errors = immutableCopy(errors);
        this.metadata = metadata;
    }

    public static StructureLibSceneBuildResult success(GuidebookLevel level, List<String> warnings,
        @Nullable StructureLibSceneMetadata metadata) {
        return new StructureLibSceneBuildResult(true, level, warnings, List.of(), metadata);
    }

    public static StructureLibSceneBuildResult failure(List<String> warnings, List<String> errors) {
        return new StructureLibSceneBuildResult(false, new GuidebookLevel(), warnings, errors, null);
    }

    private static List<String> immutableCopy(List<String> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return List.copyOf(source);
    }
}
