package com.hfstudio.guidenh.guide.scene.support;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3f;

import com.hfstudio.guidenh.guide.scene.annotation.DiamondAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldBoxAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldLineAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.SceneAnnotation;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

public class BlockAnnotationTemplateExpander {

    private BlockAnnotationTemplateExpander() {}

    public static List<SceneAnnotation> expand(GuidebookLevel level, GuideBlockMatcher matcher,
        List<? extends SceneAnnotation> templateAnnotations) {
        List<SceneAnnotation> expanded = new ArrayList<>();
        for (int[] pos : level.getFilledBlocks()) {
            int meta = level.getBlockMetadata(pos[0], pos[1], pos[2]);
            String explicitBlockId = level.getExplicitBlockId(pos[0], pos[1], pos[2]);
            if (!matcher.matchesResolvedBlockId(explicitBlockId, meta)
                && !matcher.matches(level.getBlock(pos[0], pos[1], pos[2]), meta)) {
                continue;
            }

            for (SceneAnnotation templateAnnotation : templateAnnotations) {
                expanded.add(translate(templateAnnotation, pos[0], pos[1], pos[2]));
            }
        }
        return expanded;
    }

    private static SceneAnnotation translate(SceneAnnotation templateAnnotation, int x, int y, int z) {
        if (templateAnnotation instanceof DiamondAnnotation diamondAnnotation) {
            DiamondAnnotation translated = new DiamondAnnotation(
                new Vector3f(diamondAnnotation.getPos()).add(x, y, z),
                diamondAnnotation.getColor());
            translated.setAlwaysOnTop(diamondAnnotation.isAlwaysOnTop());
            translated.setTooltip(templateAnnotation.getTooltip());
            return translated;
        }

        if (templateAnnotation instanceof InWorldBoxAnnotation boxAnnotation) {
            InWorldBoxAnnotation translated = new InWorldBoxAnnotation(
                new Vector3f(boxAnnotation.min()).add(x, y, z),
                new Vector3f(boxAnnotation.max()).add(x, y, z),
                boxAnnotation.color(),
                boxAnnotation.thickness());
            copyInWorldState(boxAnnotation, translated);
            return translated;
        }

        if (templateAnnotation instanceof InWorldLineAnnotation lineAnnotation) {
            InWorldLineAnnotation translated = new InWorldLineAnnotation(
                new Vector3f(lineAnnotation.from()).add(x, y, z),
                new Vector3f(lineAnnotation.to()).add(x, y, z),
                lineAnnotation.color(),
                lineAnnotation.thickness());
            copyInWorldState(lineAnnotation, translated);
            return translated;
        }

        throw new IllegalArgumentException(
            "Unsupported block annotation template type: " + templateAnnotation.getClass()
                .getName());
    }

    private static void copyInWorldState(SceneAnnotation source, InWorldAnnotation target) {
        target.setTooltip(source.getTooltip());
        if (source instanceof InWorldAnnotation inWorldAnnotation) {
            target.setAlwaysOnTop(inWorldAnnotation.isAlwaysOnTop());
        }
    }
}
