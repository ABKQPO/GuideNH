package com.hfstudio.guidenh.guide.siteexport.site.layout;

/**
 * Pluggable layout for exporting a single recipe block to site HTML.
 */
public interface SiteRecipeLayoutStrategy {

    boolean supports(SiteRecipeLayoutContext ctx);

    String render(SiteRecipeLayoutContext ctx);
}
