package com.hfstudio.guidenh.guide.internal.tooltip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.slprime.chromatictooltips.TooltipHandler;
import com.slprime.chromatictooltips.api.ITooltipComponent;
import com.slprime.chromatictooltips.component.DyncamicTextComponent;

public class GuideChromaticTooltipCompat {

    private static final String COMPONENT_PREFIX = "\u00A7z";

    protected GuideChromaticTooltipCompat() {}

    public static List<String> expandLine(@Nullable String line) {
        if (line == null) {
            return List.of("");
        }
        if (!line.startsWith(COMPONENT_PREFIX)) {
            return List.of(line);
        }

        ITooltipComponent component = TooltipHandler.getTooltipComponent(line);
        if (!(component instanceof DyncamicTextComponent dynamicTextComponent)) {
            return List.of(line);
        }
        String text = dynamicTextComponent.getHandler()
            .get();
        if (text == null) {
            return List.of(line);
        }
        return splitLines(text);
    }

    private static List<String> splitLines(String text) {
        String[] parts = text.split("\n", -1);
        List<String> lines = new ArrayList<>(parts.length);
        Collections.addAll(lines, parts);
        return lines.isEmpty() ? List.of("") : lines;
    }
}
