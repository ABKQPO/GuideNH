package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.*;
import java.util.regex.*;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;

/** Suggests heading anchors from the current document for href="#..." attributes. */
public class AnchorProvider implements AutocompleteProvider {

    private static final Set<AutocompleteKey> KEYS =
        Collections.singleton(AutocompleteKey.forValue("a", "href"));

    private static final Pattern HEADING = Pattern.compile("^#{1,6}\\s+(.+)$", Pattern.MULTILINE);

    @Nullable
    private static volatile String documentText;

    /** Update the document text for heading extraction. */
    public static void setDocumentText(@Nullable String text) {
        documentText = text;
    }

    @Override
    public Set<AutocompleteKey> getSupportedKeys() { return KEYS; }

    @Override
    public List<AutocompleteCandidate> provide(AutocompleteContext ctx, int limit) {
        String partial = ctx.getPartialText();
        if (partial == null || !partial.startsWith("#")) return Collections.emptyList();
        String query = partial.substring(1).toLowerCase(); // strip leading #

        if (documentText == null) return Collections.emptyList();
        List<AutocompleteCandidate> results = new ArrayList<>();
        Matcher m = HEADING.matcher(documentText);
        while (m.find()) {
            if (results.size() >= limit) break;
            String heading = m.group(1).trim();
            String anchor = heading.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
            if (query.isEmpty() || anchor.contains(query)) {
                results.add(new RegistryCandidate("#" + anchor, heading));
            }
        }
        return results;
    }
}
