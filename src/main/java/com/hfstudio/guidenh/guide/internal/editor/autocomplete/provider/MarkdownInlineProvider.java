package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.*;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;

/** Suggests inline markdown syntax templates. Stub — currently no trigger context wired. */
public class MarkdownInlineProvider implements AutocompleteProvider {

    private static final Set<AutocompleteKey> KEYS =
        Collections.singleton(AutocompleteKey.forTag());

    private static final String[] TEMPLATES = {
        "**bold**", "*italic*", "~~strikethrough~~", "==highlight==",
        "++underline++", "[link](url)", "![image](url)", "`code`"
    };

    @Override
    public Set<AutocompleteKey> getSupportedKeys() { return KEYS; }

    @Override
    public List<AutocompleteCandidate> provide(AutocompleteContext ctx, int limit) {
        return Collections.emptyList(); // TODO: activate when WORD context triggers markdown mode
    }
}
