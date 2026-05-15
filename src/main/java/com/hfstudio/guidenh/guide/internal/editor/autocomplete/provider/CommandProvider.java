package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.minecraft.command.ICommand;
import net.minecraftforge.client.ClientCommandHandler;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;

/** Suggests registered client-side commands for &lt;CommandLink command&gt;. */
public class CommandProvider implements AutocompleteProvider {

    private static final Set<AutocompleteKey> KEYS = Collections
        .singleton(AutocompleteKey.forValue("CommandLink", "command"));

    @Override
    public Set<AutocompleteKey> getSupportedKeys() {
        return KEYS;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<AutocompleteCandidate> provide(AutocompleteContext ctx, int limit) {
        String partial = ctx.getPartialText()
            .toLowerCase();
        List<AutocompleteCandidate> results = new ArrayList<>();
        for (Object cmdObj : ClientCommandHandler.instance.getCommands()
            .values()) {
            if (results.size() >= limit) break;
            if (cmdObj instanceof ICommand) {
                ICommand cmd = (ICommand) cmdObj;
                String name = cmd.getCommandName();
                if (partial.isEmpty() || name.toLowerCase()
                    .contains(partial)) {
                    results.add(new TextCandidate("/" + name));
                }
            }
        }
        return results;
    }
}
