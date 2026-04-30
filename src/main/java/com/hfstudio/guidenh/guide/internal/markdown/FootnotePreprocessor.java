package com.hfstudio.guidenh.guide.internal.markdown;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FootnotePreprocessor {

    private static final Pattern DEFINITION_START = Pattern.compile("^\\[\\^([^\\]]+)]:(.*)$");
    private static final Pattern REFERENCE = Pattern.compile("\\[\\^([^\\]]+)]");

    private FootnotePreprocessor() {}

    public static String preprocess(String markdown) {
        if (markdown == null || markdown.indexOf("[^") < 0) {
            return markdown;
        }

        String normalized = markdown.replace("\r\n", "\n")
            .replace('\r', '\n');
        String[] lines = normalized.split("\n", -1);
        Map<String, String> definitions = new LinkedHashMap<>();
        StringBuilder body = new StringBuilder(normalized.length());

        for (int i = 0; i < lines.length; i++) {
            Matcher matcher = DEFINITION_START.matcher(lines[i]);
            if (!matcher.matches()) {
                appendLine(body, lines[i]);
                continue;
            }

            String id = matcher.group(1)
                .trim();
            StringBuilder definition = new StringBuilder(
                matcher.group(2)
                    .trim());
            while (i + 1 < lines.length) {
                String next = lines[i + 1];
                if (next.startsWith("    ") || next.startsWith("\t")) {
                    if (definition.length() > 0) {
                        definition.append('\n');
                    }
                    definition.append(trimDefinitionIndent(next));
                    i++;
                    continue;
                }
                if (next.isEmpty()) {
                    if (i + 2 < lines.length) {
                        String afterBlank = lines[i + 2];
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
        if (result.length() > 0 && result.charAt(result.length() - 1) != '\n') {
            result.append('\n');
        }
        if (result.length() > 0) {
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
        result.append("</FootnoteList>\n");
        return result.toString();
    }

    private static String replaceReferences(String body, Map<String, String> definitions) {
        Matcher matcher = REFERENCE.matcher(body);
        StringBuffer buffer = new StringBuffer(body.length());
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
        if (line.length() >= 4 && line.startsWith("    ")) {
            return line.substring(4);
        }
        return line;
    }

    private static void appendLine(StringBuilder builder, String line) {
        if (builder.length() > 0) {
            builder.append('\n');
        }
        builder.append(line);
    }
}
