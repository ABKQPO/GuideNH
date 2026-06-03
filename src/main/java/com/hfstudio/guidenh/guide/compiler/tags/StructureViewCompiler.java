package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstLiteral;

/**
 * 
 * <pre>
 * &lt;Structure width="200" height="160"&gt;
 *   0 0 0 minecraft:stone
 *   1 0 0 minecraft:cobblestone
 *   0 1 0 minecraft:dirt
 *   0 0 1 minecraft:planks:2
 * &lt;/Structure&gt;
 * </pre>
 */
public class StructureViewCompiler extends BlockTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("Structure");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        int width = MdxAttrs.getInt(compiler, parent, el, "width", 192);
        int height = MdxAttrs.getInt(compiler, parent, el, "height", 144);

        StringBuilder text = new StringBuilder();
        for (var child : el.children()) {
            if (child instanceof MdAstLiteral literal) {
                text.append(literal.value)
                    .append('\n');
            }
        }

        List<StructureEntry> entries = new ArrayList<>();
        parseLines(compiler, parent, entries, text, el);

        StructurePlaceholder placeholder = new StructurePlaceholder(width, height, entries);
        parent.append(placeholder);
    }

    private static void parseLine(PageCompiler compiler, LytBlockContainer parent, List<StructureEntry> entries,
        String line, MdxJsxElementFields el) {
        String[] parts = firstTokens(line, 4);
        if (parts == null) {
            parent.appendError(compiler, "Structure entry needs '<x> <y> <z> <modid:name>': " + line, el);
            return;
        }

        int x, y, z;
        try {
            x = Integer.parseInt(parts[0]);
            y = Integer.parseInt(parts[1]);
            z = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            parent.appendError(compiler, "Structure entry has non-integer coords: " + line, el);
            return;
        }

        String idSpec = parts[3];
        entries.add(new StructureEntry(x, y, z, idSpec));
    }

    private static void parseLines(PageCompiler compiler, LytBlockContainer parent, List<StructureEntry> entries,
        StringBuilder text, MdxJsxElementFields el) {
        int lineStart = 0;
        for (int i = 0; i <= text.length(); i++) {
            if (i < text.length() && text.charAt(i) != '\n' && text.charAt(i) != '\r') {
                continue;
            }
            parseRawLine(
                compiler,
                parent,
                entries,
                text.substring(lineStart, i)
                    .trim(),
                el);
            if (i + 1 < text.length() && text.charAt(i) == '\r' && text.charAt(i + 1) == '\n') {
                i++;
            }
            lineStart = i + 1;
        }
    }

    private static void parseRawLine(PageCompiler compiler, LytBlockContainer parent, List<StructureEntry> entries,
        String line, MdxJsxElementFields el) {
        if (line.isEmpty() || line.startsWith("#")) {
            return;
        }
        parseLine(compiler, parent, entries, line, el);
    }

    private static String[] firstTokens(String line, int count) {
        String[] tokens = new String[count];
        int offset = 0;
        for (int i = 0; i < count; i++) {
            offset = skipWhitespace(line, offset);
            if (offset >= line.length()) {
                return null;
            }
            int end = findWhitespace(line, offset);
            tokens[i] = line.substring(offset, end);
            offset = end;
        }
        return tokens;
    }

    private static int skipWhitespace(String line, int offset) {
        int current = offset;
        while (current < line.length() && Character.isWhitespace(line.charAt(current))) {
            current++;
        }
        return current;
    }

    private static int findWhitespace(String line, int offset) {
        int current = offset;
        while (current < line.length() && !Character.isWhitespace(line.charAt(current))) {
            current++;
        }
        return current;
    }

    public static class StructureEntry {

        public final int x;
        public final int y;
        public final int z;
        public final String idSpec;

        public StructureEntry(int x, int y, int z, String idSpec) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.idSpec = idSpec;
        }
    }

    public static class StructurePlaceholder extends LytParagraph {

        public final int width;
        public final int height;
        public final List<StructureEntry> entries;

        public StructurePlaceholder(int width, int height, List<StructureEntry> entries) {
            this.width = width;
            this.height = height;
            this.entries = entries;
            setStyleClass("Structure");
            setStyle(LytParagraph.PLACEHOLDER_STYLE);
            appendText("[Structure]");
        }
    }
}
