package com.hfstudio.guidenh.guide.internal.search;

public class IndexSchema {

    static final String FIELD_GUIDE_ID = "guide_id";
    static final String FIELD_PAGE_ID = "page_id";
    static final String FIELD_TEXT = "page_content";
    static final String FIELD_TITLE = "page_title";
    static final String FIELD_LANG = "lang";
    static final String FIELD_SEARCH_LANG = "search_lang";

    private IndexSchema() {}

    public static String getTitleField(String language) {
        return "page_title_" + language;
    }

    public static String getTextField(String language) {
        return "page_text_" + language;
    }
}
