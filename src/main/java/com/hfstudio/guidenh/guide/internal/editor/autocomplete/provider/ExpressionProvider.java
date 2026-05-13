package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.*;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;

/** Suggests common math expressions for &lt;Function expr&gt; and &lt;Plot expr&gt;. */
public class ExpressionProvider implements AutocompleteProvider {

    private static final Set<AutocompleteKey> KEYS = new HashSet<>(Arrays.asList(
        AutocompleteKey.forValue("Function", "expr"),
        AutocompleteKey.forValue("Plot", "expr")
    ));

    private static final String[] EXPRESSIONS = {
        "sin(x)", "cos(x)", "tan(x)", "x^2", "x^3", "sqrt(x)",
        "abs(x)", "log(x)", "ln(x)", "exp(x)", "1/x",
        "sin(x)*cos(x)", "x*sin(x)", "floor(x)", "ceil(x)"
    };

    @Override
    public Set<AutocompleteKey> getSupportedKeys() { return KEYS; }

    @Override
    public List<AutocompleteCandidate> provide(AutocompleteContext ctx, int limit) {
        String partial = ctx.getPartialText().toLowerCase();
        List<AutocompleteCandidate> results = new ArrayList<>();
        for (String expr : EXPRESSIONS) {
            if (results.size() >= limit) break;
            if (partial.isEmpty() || expr.toLowerCase().contains(partial)) {
                results.add(new TextCandidate(expr));
            }
        }
        return results;
    }
}
