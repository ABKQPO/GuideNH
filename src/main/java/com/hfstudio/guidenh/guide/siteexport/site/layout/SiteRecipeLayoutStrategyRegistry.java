package com.hfstudio.guidenh.guide.siteexport.site.layout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Ordered strategy chain: first strategy that {@link SiteRecipeLayoutStrategy#supports} the context
 * and returns non-empty HTML wins; {@link DefaultSiteRecipeLayoutStrategy} should be registered
 * last as unconditional fallback.
 */
public final class SiteRecipeLayoutStrategyRegistry {

    private final List<SiteRecipeLayoutStrategy> strategies;

    public SiteRecipeLayoutStrategyRegistry(List<SiteRecipeLayoutStrategy> strategies) {
        this.strategies = Collections.unmodifiableList(new ArrayList<>(strategies));
    }

    public static SiteRecipeLayoutStrategyRegistry createDefault() {
        return new SiteRecipeLayoutStrategyRegistry(
            Arrays.asList(new PositionedNeiSiteRecipeLayoutStrategy(), new DefaultSiteRecipeLayoutStrategy()));
    }

    public String render(SiteRecipeLayoutContext ctx) {
        for (SiteRecipeLayoutStrategy strategy : strategies) {
            if (!strategy.supports(ctx)) {
                continue;
            }
            String html = strategy.render(ctx);
            if (html != null && !html.isEmpty()) {
                return html;
            }
        }
        return "";
    }
}
