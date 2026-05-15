package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;

public class TagNameProvider implements AutocompleteProvider {

    private static final Set<AutocompleteKey> KEYS = Collections.singleton(AutocompleteKey.forTag());
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
        List<AutocompleteCandidate> results = new ArrayList<>();
        for (String name : TAG_NAMES) {
            if (results.size() >= limit) break;
            if (lower.isEmpty() || name.toLowerCase()
                .startsWith(lower)) {
                results.add(new TextCandidate(name));
            }
        }
        return results;
    }
}
