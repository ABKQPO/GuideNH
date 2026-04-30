package com.hfstudio.guidenh.guide.internal.markdown;

public final class CodeBlockLanguage {

    private final String id;
    private final String displayName;

    public CodeBlockLanguage(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }
}
