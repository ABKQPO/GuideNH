package com.hfstudio.guidenh.guide.internal.editor.guide;

import java.util.EnumSet;
import java.util.Set;

public record GuideScreenEditorActionSpec(GuideScreenEditorAction action, Set<GuideScreenEditorActionGroup> groups) {

    public GuideScreenEditorActionSpec {
        groups = groups == null || groups.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(groups));
    }

    public boolean belongsTo(GuideScreenEditorActionGroup group) {
        return groups.contains(group);
    }
}
