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
        return normalizeWhitespace(raw);
    }

    private static String normalizeWhitespace(String raw) {
        StringBuilder builder = new StringBuilder(raw.length());
        boolean previousWhitespace = true;
        for (int index = 0; index < raw.length(); index++) {
            char value = raw.charAt(index);
            if (Character.isWhitespace(value)) {
                if (!previousWhitespace) {
                    builder.append(' ');
                    previousWhitespace = true;
                }
            } else {
                builder.append(value);
                previousWhitespace = false;
            }
        }
        int length = builder.length();
        if (length > 0 && builder.charAt(length - 1) == ' ') {
            builder.setLength(length - 1);
        }
        return builder.toString();
    }
}
