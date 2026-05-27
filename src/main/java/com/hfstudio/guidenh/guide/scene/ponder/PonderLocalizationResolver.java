package com.hfstudio.guidenh.guide.scene.ponder;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.localization.GuideResourceLanguageIndex;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;

public class PonderLocalizationResolver {

    private PonderLocalizationResolver() {}

    public static void localize(PonderSceneData data, @Nullable String language) {
        if (data == null) {
            return;
        }
        String resolvedLanguage = LangUtil
            .normalizeLanguage(language != null ? language : LangUtil.getCurrentLanguage());
        for (PonderKeyframe keyframe : data.getKeyframes()) {
            if (keyframe == null) {
                continue;
            }
            keyframe.applyLocalizedLabel(resolveValue(resolvedLanguage, keyframe.getLabelKey()));
            localizeAnnotations(keyframe.getAnnotations(), resolvedLanguage);
        }
    }

    private static void localizeAnnotations(List<PonderKeyframeAnnotation> annotations, String language) {
        for (PonderKeyframeAnnotation annotation : annotations) {
            if (annotation == null) {
                continue;
            }
            annotation.applyLocalizedText(resolveValue(language, annotation.getTextKey()));
            annotation.applyLocalizedTooltip(resolveValue(language, annotation.getTooltipKey()));
        }
    }

    private static @Nullable String resolveValue(String language, @Nullable String key) {
        if (key == null) {
            return null;
        }
        String normalizedKey = key.trim();
        if (normalizedKey.isEmpty()) {
            return null;
        }
        String localized = GuideResourceLanguageIndex.getValue(language, normalizedKey);
        return localized != null && !localized.isEmpty() ? localized : null;
    }
}
