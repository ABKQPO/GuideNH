package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.*;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;

/** Suggests MDX tag names when cursor is right after '&lt;'. */
public class TagNameProvider implements AutocompleteProvider {

    private static final Set<AutocompleteKey> KEYS =
        Collections.singleton(AutocompleteKey.forTag());

    // Grouped by category for future grouping in UI
    private static final String[] TAG_NAMES = {
        // Inline/Flow
        "a", "br", "Tooltip", "ItemImage", "ItemLink", "BlockImage", "Color",
        "CommandLink", "kbd", "KeyBind", "Latex", "mark", "PlayerName",
        "sub", "sup", "FloatingImage",
        // Block/Container
        "Row", "Column", "div", "details", "CategoryIndex", "CsvTable",
        "FileTree", "FootnoteList", "ItemGrid", "Mermaid",
        "Recipe", "RecipeFor", "RecipesFor", "Structure", "SubPages",
        // Charts
        "ColumnChart", "BarChart", "LineChart", "PieChart", "ScatterChart",
        // Math
        "FunctionGraph", "Function",
        // Scene
        "GameScene", "Scene", "Block", "Entity", "PlaceBlock",
        "ReplaceBlock", "RemoveBlocks", "ImportStructure", "ImportStructureLib",
        "ImportPonder", "IsometricCamera",
        // Annotations
        "BlockAnnotation", "BoxAnnotation", "LineAnnotation",
        "DiamondAnnotation", "TextAnnotation", "BlockAnnotationTemplate"
    };

    @Override
    public Set<AutocompleteKey> getSupportedKeys() { return KEYS; }

    @Override
    public List<AutocompleteCandidate> provide(AutocompleteContext ctx, int limit) {
        String partial = ctx.getPartialText();
        String lower = partial != null ? partial.toLowerCase() : "";
        List<AutocompleteCandidate> results = new ArrayList<>();
        for (String name : TAG_NAMES) {
            if (results.size() >= limit) break;
            if (lower.isEmpty() || name.toLowerCase().startsWith(lower)) {
                results.add(new TextCandidate(name));
            }
        }
        return results;
    }
}
