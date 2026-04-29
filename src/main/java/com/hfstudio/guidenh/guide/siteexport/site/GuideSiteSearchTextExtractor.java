package com.hfstudio.guidenh.guide.siteexport.site;

import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.internal.search.GuideSearch;

public class GuideSiteSearchTextExtractor {

    public String title(Guide guide, ParsedGuidePage page) {
        return GuideSearch.getPageTitle(guide, page);
    }

    public String searchableText(Guide guide, ParsedGuidePage page) {
        String raw = GuideSearch.getSearchableText(guide, page);
        return raw.replace("\r", "")
            .replace('\n', ' ')
            .replaceAll("\\s+", " ")
            .trim();
    }
}
