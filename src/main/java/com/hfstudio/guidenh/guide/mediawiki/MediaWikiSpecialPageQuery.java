package com.hfstudio.guidenh.guide.mediawiki;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record MediaWikiSpecialPageQuery(String searchText, int visibleCount, Map<String, String> parameters) {

    public static final String PARAM_PAGE = "page";
    public static final String PARAM_PREFIX = "prefix";
    public static final String PARAM_LANGUAGE = "language";
    public static final int PAGE_SIZE = 60;
    public static final MediaWikiSpecialPageQuery DEFAULT = new MediaWikiSpecialPageQuery("", PAGE_SIZE);
    public static final MediaWikiSpecialPageQuery UNLIMITED = new MediaWikiSpecialPageQuery("", Integer.MAX_VALUE);

    public MediaWikiSpecialPageQuery(String searchText, int visibleCount) {
        this(searchText, visibleCount, Collections.<String, String>emptyMap());
    }

    public MediaWikiSpecialPageQuery {
        searchText = searchText != null ? searchText : "";
        parameters = parameters == null || parameters.isEmpty() ? Collections.<String, String>emptyMap()
            : Collections.unmodifiableMap(new LinkedHashMap<>(parameters));
    }

    public MediaWikiSpecialPageQuery loadMore() {
        return new MediaWikiSpecialPageQuery(searchText, visibleCount + PAGE_SIZE, parameters);
    }

    public MediaWikiSpecialPageQuery withSearchText(String nextSearchText) {
        return new MediaWikiSpecialPageQuery(nextSearchText != null ? nextSearchText : "", visibleCount, parameters);
    }

    public MediaWikiSpecialPageQuery withParameter(String key, String value) {
        if (key == null || key.trim()
            .isEmpty()
            || value == null
            || value.trim()
                .isEmpty()) {
            return this;
        }
        LinkedHashMap<String, String> nextParameters = new LinkedHashMap<>(parameters);
        nextParameters.put(key, value.trim());
        return new MediaWikiSpecialPageQuery(searchText, visibleCount, nextParameters);
    }

    public String parameter(String key) {
        return key != null ? parameters.get(key) : null;
    }

    public MediaWikiSpecialPageQuery withVisibleCount(int nextVisibleCount) {
        return new MediaWikiSpecialPageQuery(searchText, nextVisibleCount, parameters);
    }
}
