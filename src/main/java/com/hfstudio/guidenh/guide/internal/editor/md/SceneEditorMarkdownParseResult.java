package com.hfstudio.guidenh.guide.internal.editor.md;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorSceneModel;

public interface SceneEditorMarkdownParseResult {

    @Desugar
    record Success(SceneEditorSceneModel model) implements SceneEditorMarkdownParseResult {}

    @Desugar
    record SyntaxError(String message) implements SceneEditorMarkdownParseResult {}

    @Desugar
    record Unsupported(String message) implements SceneEditorMarkdownParseResult {}
}
