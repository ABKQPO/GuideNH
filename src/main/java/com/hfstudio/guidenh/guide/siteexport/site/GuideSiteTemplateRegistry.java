package com.hfstudio.guidenh.guide.siteexport.site;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GuideSiteTemplateRegistry {

    private int counter = 1;
    private final List<String> rendered = new ArrayList<>();
    private final Map<String, String> idsByHtml = new LinkedHashMap<>();

    public String create(String html) {
        String existing = idsByHtml.get(html);
        if (existing != null) {
            return existing;
        }

        String id = "tmpl-" + counter++;
        idsByHtml.put(html, id);
        rendered.add("<template id=\"" + id + "\">" + html + "</template>");
        return id;
    }

    public List<String> renderAll() {
        return rendered;
    }
}
