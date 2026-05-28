package com.hfstudio.guidenh.guide.internal.markdown;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hfstudio.guidenh.guide.internal.util.GuideStringLines;

public class FootnotePreprocessor {

    private static final Pattern DEFINITION_START = Pattern.compile("^\\[\\^([^\\]]+)]:(.*)$");
    private static final Pattern REFERENCE = Pattern.compile("\\[\\^([^\\]]+)]");

    protected FootnotePreprocessor() {}

    public static String preprocess(String markdown) {
        if (markdown == null || !markdown.contains("[^")) {
            return markdown;
        }

        List<String> lines = GuideStringLines.splitLines(markdown);
        Map<String, String> definitions = new LinkedHashMap<>();
        StringBuilder body = new StringBuilder(markdown.length());

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Matcher matcher = DEFINITION_START.matcher(line);
            if (!matcher.matches()) {
                appendLine(body, line);
                continue;
            }

            String id = matcher.group(1)
                .trim();
            StringBuilder definition = new StringBuilder(
                matcher.group(2)
                    .trim());
            while (i + 1 < lines.size()) {
                String next = lines.get(i + 1);
                if (next.startsWith("    ") || next.startsWith("\t")) {
                    if (!definition.isEmpty()) {
                        definition.append('\n');
                    }
                    definition.append(trimDefinitionIndent(next));
                    i++;
                    continue;
                }
                if (next.isEmpty()) {
                    if (i + 2 < lines.size()) {
                        String afterBlank = lines.get(i + 2);
                        if (afterBlank.startsWith("    ") || afterBlank.startsWith("\t")) {
                            definition.append("\n\n");
                            i += 2;
                            definition.append(trimDefinitionIndent(afterBlank));
                            continue;
                        }
                    }
                }
                break;
            }
            definitions.put(id, definition.toString());
        }

        String transformedBody = replaceReferences(body.toString(), definitions);
        if (definitions.isEmpty()) {
            return transformedBody;
        }

        StringBuilder result = new StringBuilder(transformedBody.length() + 64);
        result.append(transformedBody);
        if (!result.isEmpty() && result.charAt(result.length() - 1) != '\n') {
            result.append('\n');
        }
        if (!result.isEmpty()) {
            result.append('\n');
        }
        result.append("<FootnoteList width=\"220\">\n\n");
        result.append("## Footnotes\n\n");
        int index = 1;
        for (var entry : definitions.entrySet()) {
            result.append(index)
                .append(". ")
                .append(entry.getValue())
                .append('\n');
            index++;
        }
        result.append('\n');
        result.append("</FootnoteList>\n");
        return result.toString();
    }

    private static String replaceReferences(String body, Map<String, String> definitions) {
        Matcher matcher = REFERENCE.matcher(body);
        StringBuilder buffer = new StringBuilder(body.length());
        int nextNumber = 1;
        Map<String, Integer> numbers = new LinkedHashMap<>();
        while (matcher.find()) {
            String id = matcher.group(1)
                .trim();
            Integer number = numbers.get(id);
            if (number == null) {
                number = nextNumber++;
                numbers.put(id, number);
            }

            if (!definitions.containsKey(id)) {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }

            String replacement = "<Tooltip label=\"[" + number + "]\">" + definitions.get(id) + "</Tooltip>";
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String trimDefinitionIndent(String line) {
        if (line.startsWith("\t")) {
            return line.substring(1);
        }
        if (line.startsWith("    ")) {
            return line.substring(4);
        }
        return line;
    }

    private static void appendLine(StringBuilder builder, String line) {
        if (!builder.isEmpty()) {
            builder.append('\n');
        }
        builder.append(line);
    }
}
