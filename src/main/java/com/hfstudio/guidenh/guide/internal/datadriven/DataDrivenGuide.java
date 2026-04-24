package com.hfstudio.guidenh.guide.internal.datadriven;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.util.ResourceLocation;

import com.github.bsideup.jabel.Desugar;
import com.google.gson.JsonObject;
import com.hfstudio.guidenh.guide.GuideItemSettings;
import com.hfstudio.guidenh.guide.color.Colors;
import com.hfstudio.guidenh.guide.color.ConstantColor;

@Desugar
public record DataDrivenGuide(GuideItemSettings itemSettings, String defaultLanguage,
    Map<ResourceLocation, ConstantColor> customColors) {

    public DataDrivenGuide(GuideItemSettings itemSettings) {
        this(itemSettings, "en_us", new HashMap<>());
    }

    public static DataDrivenGuide fromJson(JsonObject json) {
        GuideItemSettings itemSettings = GuideItemSettings.DEFAULT;
        if (json.has("item_settings")) {
            itemSettings = GuideItemSettings.fromJson(json.getAsJsonObject("item_settings"));
        }

        String defaultLanguage = "en_us";
        if (json.has("default_language")) {
            defaultLanguage = json.get("default_language")
                .getAsString();
        }

        Map<ResourceLocation, ConstantColor> customColors = new HashMap<>();
        if (json.has("custom_colors")) {
            var colorsJson = json.getAsJsonObject("custom_colors");
            for (var entry : colorsJson.entrySet()) {
                var colorObj = entry.getValue()
                    .getAsJsonObject();
                int dark = Colors.hexToRgb(
                    colorObj.get("dark_mode")
                        .getAsString());
                int light = Colors.hexToRgb(
                    colorObj.get("light_mode")
                        .getAsString());
                customColors.put(new ResourceLocation(entry.getKey()), new ConstantColor(dark, light));
            }
        }

        return new DataDrivenGuide(itemSettings, defaultLanguage, customColors);
    }
}
