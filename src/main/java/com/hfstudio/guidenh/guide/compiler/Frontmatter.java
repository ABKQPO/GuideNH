package com.hfstudio.guidenh.guide.compiler;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.util.ResourceLocation;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record Frontmatter(@Nullable FrontmatterNavigation navigationEntry, Map<String, Object> additionalProperties) {

    public static Frontmatter parse(ResourceLocation pageId, String yamlText) {
        var yaml = new Yaml(new SafeConstructor(new LoaderOptions()));

        FrontmatterNavigation navigation = null;
        Map<String, Object> data = yaml.load(yamlText);
        var navigationObj = data.remove("navigation");
        if (navigationObj != null) {
            if (!(navigationObj instanceof Map<?, ?>navigationMap)) {
                throw new IllegalArgumentException("The navigation key in the frontmatter has to be a map");
            }

            var title = getString(navigationMap, "title");
            if (title == null) {
                throw new IllegalArgumentException("title is missing in navigation frontmatter");
            }
            var parentIdStr = getString(navigationMap, "parent");
            var position = 0;
            if (navigationMap.containsKey("position")) {
                position = getInt(navigationMap, "position");
            }
            var iconIdStr = getString(navigationMap, "icon");
            var iconTextureStr = getString(navigationMap, "icon_texture");
            Map<?, ?> iconComponents = getCompound(navigationMap, "icon_components");

            ResourceLocation parentId = null;
            if (parentIdStr != null) {
                parentId = IdUtils.resolveId(parentIdStr, pageId.getResourceDomain());
            }

            ResourceLocation iconId = null;
            if (iconIdStr != null) {
                iconId = IdUtils.resolveId(iconIdStr, pageId.getResourceDomain());
            }

            ResourceLocation iconTextureId = null;
            if (iconTextureStr != null) {
                iconTextureId = IdUtils.resolveLink(iconTextureStr, pageId);
            }

            navigation = new FrontmatterNavigation(title, parentId, position, iconId, iconComponents, iconTextureId);
        }

        return new Frontmatter(navigation, Collections.unmodifiableMap(new HashMap<>(data)));
    }

    @Nullable
    private static String getString(Map<?, ?> map, String key) {
        var value = map.get(key);
        if (value != null && !(value instanceof String)) {
            throw new IllegalArgumentException("Key " + key + " has to be a String!");
        }
        return (String) value;
    }

    private static int getInt(Map<?, ?> map, String key) {
        var value = map.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Key " + key + " is missing in navigation frontmatter");
        }
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("Key " + key + " has to be a number!");
        }
        return number.intValue();
    }

    @Nullable
    private static Map<?, ?> getCompound(Map<?, ?> map, String key) {
        var value = map.get(key);
        if (value == null) {
            return null;
        }
        if (!(value instanceof Map<?, ?>mapValue)) {
            throw new IllegalArgumentException("Key " + key + " has to be a map!");
        }
        return mapValue;
    }
}
