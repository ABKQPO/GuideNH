package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;

/** Suggests key binding descriptions for &lt;KeyBind id&gt; attributes. */
public class KeyBindProvider implements AutocompleteProvider {

    private static final Set<AutocompleteKey> KEYS = Collections.singleton(AutocompleteKey.forValue("KeyBind", "id"));

    @Override
    public Set<AutocompleteKey> getSupportedKeys() {
        return KEYS;
    }

    @Override
    public List<AutocompleteCandidate> provide(AutocompleteContext ctx, int limit) {
        String partial = ctx.getPartialText()
            .toLowerCase();
        List<AutocompleteCandidate> results = new ArrayList<>();
        for (KeyBinding kb : Minecraft.getMinecraft().gameSettings.keyBindings) {
            if (results.size() >= limit) break;
            String desc = kb.getKeyDescription();
            if (partial.isEmpty() || desc.toLowerCase()
                .contains(partial)) {
                results.add(new TextCandidate(desc));
            }
        }
        return results;
    }
}
