package com.hfstudio.guidenh.guide.internal.editor.gui;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.editor.md.SceneEditorMarkdownElementRangeIndex;

public class SceneEditorLinkedSelectionController {

    @Nullable
    public UUID resolveSelectedElementId(SceneEditorMarkdownElementRangeIndex index, int cursorIndex,
        @Nullable UUID currentSelectedElementId) {
        return index.findByCursor(cursorIndex)
            .orElse(currentSelectedElementId);
    }
}
