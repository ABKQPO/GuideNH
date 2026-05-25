package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver.TagStartContext;

public class TagNameProvider implements AutocompleteProvider {

    private static final Set<AutocompleteKey> KEYS = buildKeys();
    private static volatile boolean enabled = true;

    private static final String[] TAG_NAMES = { "a", "br", "Tooltip", "ItemImage", "ItemLink", "BlockImage", "Color",
        "CommandLink", "kbd", "KeyBind", "Latex", "mark", "PlayerName", "sub", "sup", "FloatingImage", "Row", "Column",
        "div", "details", "CategoryIndex", "CsvTable", "FileTree", "FootnoteList", "ItemGrid", "Mermaid", "Recipe",
        "RecipeFor", "RecipesFor", "Structure", "SubPages", "ColumnChart", "BarChart", "LineChart", "PieChart",
        "ScatterChart", "FunctionGraph", "Function", "GameScene", "Scene", "Block", "Entity", "PlaceBlock",
        "ReplaceBlock", "RemoveBlocks", "ImportStructure", "ImportStructureLib", "ImportPonder", "IsometricCamera",
        "Tier", "Channel", "Facing", "Rotation", "Flip", "Orientation", "GregTechActiveController",
        "GregTechPlaceHatches", "BlockAnnotation", "BoxAnnotation", "LineAnnotation", "DiamondAnnotation",
        "TextAnnotation", "BlockAnnotationTemplate" };

    private static final String[] GAME_SCENE_TAG_NAMES = { "ImportStructure", "ImportStructureLib", "RemoveBlocks",
        "BlockAnnotationTemplate", "BlockAnnotation", "BoxAnnotation", "LineAnnotation", "DiamondAnnotation",
        "TextAnnotation", "Block", "Entity", "PlaceBlock", "ReplaceBlock", "IsometricCamera", "ImportPonder", "Tier",
        "Channel", "Facing", "Rotation", "Flip", "Orientation", "GregTechActiveController", "GtActiveController",
        "GregTechPlaceHatches", "GtPlaceHatches" };

    private static Set<AutocompleteKey> buildKeys() {
        Set<AutocompleteKey> keys = new HashSet<>();
        keys.add(AutocompleteKey.forTag());
        keys.add(AutocompleteKey.forTag("GameScene"));
        keys.add(AutocompleteKey.forTag("Scene"));
        return Collections.unmodifiableSet(keys);
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    @Override
    public Set<AutocompleteKey> getSupportedKeys() {
        return KEYS;
    }

    @Override
    public List<AutocompleteCandidate> provide(AutocompleteContext ctx, int limit) {
        if (!enabled) return Collections.emptyList();
        String partial = ctx.getPartialText();
        String lower = partial != null ? partial.toLowerCase() : "";
        String[] names = TAG_NAMES;
        if (ctx instanceof TagStartContext) {
            String parent = ((TagStartContext) ctx).getParentTagName();
            if ("GameScene".equals(parent) || "Scene".equals(parent)) {
                names = GAME_SCENE_TAG_NAMES;
            }
        }
        List<AutocompleteCandidate> results = new ArrayList<>();
        for (String name : names) {
            if (results.size() >= limit) break;
            if (lower.isEmpty() || name.toLowerCase()
                .startsWith(lower)) {
                results.add(new TextCandidate(name));
            }
        }
        return results;
    }
}
