package com.hfstudio.guidenh.guide.document.block;

import java.util.Locale;

import com.hfstudio.guidenh.guide.compiler.tags.SerializedEnum;

public enum AlignItems implements SerializedEnum {

    CENTER,
    START,
    END;

    private final String serializedName;

    AlignItems() {
        this.serializedName = name().toLowerCase(Locale.ROOT);
    }

    public String getSerializedName() {
        return serializedName;
    }
}
