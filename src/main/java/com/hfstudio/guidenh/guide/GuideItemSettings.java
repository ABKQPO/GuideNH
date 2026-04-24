package com.hfstudio.guidenh.guide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import net.minecraft.util.ResourceLocation;

import com.github.bsideup.jabel.Desugar;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

@Desugar
public record GuideItemSettings(Optional<String> displayName, List<String> tooltipLines,
    Optional<ResourceLocation> itemModel) {

    public static GuideItemSettings DEFAULT = new GuideItemSettings(
        Optional.empty(),
        Collections.emptyList(),
        Optional.empty());

    public static GuideItemSettings fromJson(JsonObject json) {
        Optional<String> displayName = Optional.empty();
        if (json.has("display_name")) {
            displayName = Optional.of(
                json.get("display_name")
                    .getAsString());
        }

        List<String> tooltipLines = new ArrayList<>();
        if (json.has("tooltip_lines")) {
            for (JsonElement el : json.getAsJsonArray("tooltip_lines")) {
                tooltipLines.add(el.getAsString());
            }
        }

        Optional<ResourceLocation> model = Optional.empty();
        if (json.has("model")) {
            model = Optional.of(
                new ResourceLocation(
                    json.get("model")
                        .getAsString()));
        }

        return new GuideItemSettings(displayName, tooltipLines, model);
    }
}
