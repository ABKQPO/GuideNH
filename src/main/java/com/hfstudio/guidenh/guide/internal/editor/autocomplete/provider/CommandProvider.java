package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.*;

import net.minecraft.client.Minecraft;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;

/** Suggests registered client-side commands for &lt;CommandLink command&gt;. */
public class CommandProvider implements AutocompleteProvider {

    private static final Set<AutocompleteKey> KEYS =
        Collections.singleton(AutocompleteKey.forValue("CommandLink", "command"));

    @Override
    public Set<AutocompleteKey> getSupportedKeys() { return KEYS; }

    @Override
    @SuppressWarnings("unchecked")
    public List<AutocompleteCandidate> provide(AutocompleteContext ctx, int limit) {
        String partial = ctx.getPartialText().toLowerCase();
        List<AutocompleteCandidate> results = new ArrayList<>();
        for (Object cmdObj : net.minecraftforge.client.ClientCommandHandler.instance.getCommands().values()) {
            if (results.size() >= limit) break;
            if (cmdObj instanceof net.minecraft.command.ICommand) {
                net.minecraft.command.ICommand cmd = (net.minecraft.command.ICommand) cmdObj;
                String name = cmd.getCommandName();
                if (partial.isEmpty() || name.toLowerCase().contains(partial)) {
                    results.add(new TextCandidate("/" + name));
                }
            }
        }
        return results;
    }
}
