package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.*;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;

/** Suggests GuideNH commands for &lt;CommandLink command&gt; attributes. */
public class CommandProvider implements AutocompleteProvider {

    private static final Set<AutocompleteKey> KEYS =
        Collections.singleton(AutocompleteKey.forValue("CommandLink", "command"));

    private static final String[] COMMANDS = {
        "/guidenh open <page>", "/guidenh search <query>", "/guidenh reload",
        "/guidenh export", "/guidenh export-site", "/guidenh export-structure",
        "/guidenh import-structure", "/guidenh place-all-structures", "/guidenh list"
    };

    @Override
    public Set<AutocompleteKey> getSupportedKeys() { return KEYS; }

    @Override
    public List<AutocompleteCandidate> provide(AutocompleteContext ctx, int limit) {
        String partial = ctx.getPartialText().toLowerCase();
        List<AutocompleteCandidate> results = new ArrayList<>();
        for (String cmd : COMMANDS) {
            if (results.size() >= limit) break;
            if (partial.isEmpty() || cmd.toLowerCase().contains(partial)) {
                results.add(new TextCandidate(cmd));
            }
        }
        return results;
    }
}
