package com.hfstudio.guidenh.guide.internal.editor.gui;

import java.util.UUID;

import javax.annotation.Nullable;

import com.hfstudio.guidenh.guide.internal.editor.md.SceneEditorMarkdownElementRangeIndex;

public final class SceneEditorLinkedSelectionController {

    @Nullable
    public UUID resolveSelectedElementId(SceneEditorMarkdownElementRangeIndex index, int cursorIndex,
        @Nullable UUID currentSelectedElementId) {
        return index.findByCursor(cursorIndex)
            .orElse(currentSelectedElementId);
    }
}
