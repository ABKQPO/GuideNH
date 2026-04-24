package com.hfstudio.guidenh.guide.internal.data;

import net.minecraft.util.StatCollector;

public interface LocalizationEnum {

    String getTranslationKey();

    default String text() {
        return StatCollector.translateToLocal(getTranslationKey());
    }

    default String text(Object... args) {
        return StatCollector.translateToLocalFormatted(getTranslationKey(), args);
    }

    default String withSuffix(String suffix) {
        return text() + suffix;
    }
}
