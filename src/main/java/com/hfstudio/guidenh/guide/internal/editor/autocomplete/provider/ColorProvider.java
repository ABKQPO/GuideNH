package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.*;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AttributeSpec;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AttrType;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.TagAttributeRegistry;

/** Suggests hex color values and SymbolicColor names for color-typed attributes. */
public class ColorProvider implements AutocompleteProvider {

    private static final String[] SYMBOLIC_NAMES = {
        "LINK", "BODY_TEXT", "ERROR_TEXT", "CRAFTING_RECIPE_TYPE", "THEMATIC_BREAK",
        "HEADER1_SEPARATOR", "HEADER2_SEPARATOR", "NAVBAR_BG_TOP", "NAVBAR_BG_BOTTOM",
        "NAVBAR_ROW_HOVER", "NAVBAR_EXPAND_ARROW", "TABLE_BORDER",
        "ICON_BUTTON_NORMAL", "ICON_BUTTON_DISABLED", "ICON_BUTTON_HOVER",
        "IN_WORLD_BLOCK_HIGHLIGHT", "SCENE_BACKGROUND", "GUIDE_SCREEN_BACKGROUND",
        "BLOCKQUOTE_BACKGROUND", "BLACK", "DARK_BLUE", "DARK_GREEN", "DARK_AQUA",
        "DARK_RED", "DARK_PURPLE", "GOLD", "GRAY", "DARK_GRAY", "BLUE", "GREEN",
        "AQUA", "RED", "LIGHT_PURPLE", "YELLOW", "WHITE"
    };

    @Override
    public Set<AutocompleteKey> getSupportedKeys() {
        Set<AutocompleteKey> keys = new HashSet<>();
        for (String tag : TagAttributeRegistry.getRegisteredTags()) {
            for (AttributeSpec spec : TagAttributeRegistry.get(tag)) {
                if (spec.getType() == AttrType.COLOR) {
                    keys.add(AutocompleteKey.forValue(tag, spec.getName()));
                }
            }
        }
        return Collections.unmodifiableSet(keys);
    }

    @Override
    public List<AutocompleteCandidate> provide(AutocompleteContext ctx, int limit) {
        String partial = ctx.getPartialText().toLowerCase();
        List<AutocompleteCandidate> results = new ArrayList<>();
        for (String name : SYMBOLIC_NAMES) {
            if (results.size() >= limit) break;
            if (partial.isEmpty() || name.toLowerCase().contains(partial)) {
                results.add(new TextCandidate(name));
            }
        }
        return results;
    }
}
