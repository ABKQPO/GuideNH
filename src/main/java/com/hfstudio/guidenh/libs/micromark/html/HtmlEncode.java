package com.hfstudio.guidenh.libs.micromark.html;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlEncode {

    private static final Map<String, String> characterReferences;
    static {
        HashMap<String, String> m = new HashMap<>();
        m.put("\"", "quot");
        m.put("&", "amp");
        m.put("<", "lt");
        m.put(">", "gt");
        characterReferences = m;
    }

    private static final Pattern ESCAPE_PATTERN = Pattern.compile("[\"&<>]");

    private HtmlEncode() {}

    /**
     * Encode only the dangerous HTML characters.
     * This ensures that certain characters which have special meaning in HTML are dealt with. Technically, we can skip
     * `>` and `"` in many cases, but CM includes them.
     */
    public static String encode(String value) {
        Matcher mt = ESCAPE_PATTERN.matcher(value);
        StringBuffer sb = new StringBuffer();
        while (mt.find()) {
            String repl = "&" + characterReferences.get(mt.group()) + ";";
            mt.appendReplacement(sb, Matcher.quoteReplacement(repl));
        }
        mt.appendTail(sb);
        return sb.toString();
    }

}
