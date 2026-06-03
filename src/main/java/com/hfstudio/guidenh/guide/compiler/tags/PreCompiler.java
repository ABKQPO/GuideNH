package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.functiongraph.FunctionGraphFenceParser;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytCodeBlock;
import com.hfstudio.guidenh.guide.document.block.LytMermaidMindmap;
import com.hfstudio.guidenh.guide.internal.csv.CsvTableParser;
import com.hfstudio.guidenh.guide.internal.markdown.CodeBlockLanguage;
import com.hfstudio.guidenh.guide.internal.markdown.CodeBlockLanguageDetector;
import com.hfstudio.guidenh.guide.internal.markdown.FileTreeCompiler;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidMindmapParser;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstText;

import cpw.mods.fml.common.FMLLog;

public class PreCompiler extends BlockTagCompiler {

    private static final Pattern CODEBLOCK_META_WIDTH = Pattern.compile("(^|\\s)width=(\"([^\"]+)\"|'([^']+)'|(\\S+))");
    private static final Pattern CODEBLOCK_META_HEIGHT = Pattern
        .compile("(^|\\s)height=(\"([^\"]+)\"|'([^']+)'|(\\S+))");

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("pre");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        // Extract code text from children — should be a single MdAstText child
        String codeText = "";
        var children = el.children();
        if (!children.isEmpty() && children.get(0) instanceof MdAstText text) {
            codeText = text.value;
        }

        String lang = el.getAttributeString("lang", null);
        String meta = el.getAttributeString("meta", null);

        CodeBlockLanguage language = CodeBlockLanguageDetector.detect(lang, codeText);

        // CSV table
        if (lang != null && "csv".equals(language.id())) {
            LytBlock csvBlock = compileCsvCodeBlock(codeText, meta);
            parent.append(csvBlock);
            return;
        }

        // File tree
        if (isFileTreeFence(lang)) {
            parent.append(FileTreeCompiler.compile(compiler, codeText));
            return;
        }

        // Function graph
        if (isFunctionGraphFence(lang)) {
            parent.append(FunctionGraphFenceParser.parse(codeText));
            return;
        }

        // Mermaid
        if ("mermaid".equals(language.id())) {
            LytMermaidMindmap mermaidBlock = tryCompileMermaidMindmap(codeText);
            if (mermaidBlock != null) {
                parent.append(mermaidBlock);
                return;
            }
        }

        // Default code block with syntax highlighting
        LytCodeBlock codeBlock = new LytCodeBlock();
        codeBlock.setLanguageFenceName(lang != null ? lang : language.id());
        codeBlock.applyLanguage(language);
        codeBlock.setCodeText(codeText);
        Integer preferredWidth = parseCodeBlockWidth(meta);
        if (preferredWidth != null) {
            codeBlock.setPreferredBodyWidth(preferredWidth);
        }
        Integer forcedHeight = parseCodeBlockHeight(meta);
        if (forcedHeight != null) {
            codeBlock.setForcedBodyHeight(forcedHeight);
        }
        parent.append(codeBlock);
    }

    // ---- CSV code block compilation ----

    private LytBlock compileCsvCodeBlock(String source, @Nullable String meta) {
        List<List<String>> rows = CsvTableParser.parse(source);
        if (rows.isEmpty()) {
            LytCodeBlock codeBlock = new LytCodeBlock();
            codeBlock.setLanguageFenceName("csv");
            codeBlock.applyLanguage(new CodeBlockLanguage("csv", "CSV"));
            codeBlock.setCodeText(source);
            return codeBlock;
        }

        CsvFenceMeta csvMeta = parseCsvFenceMeta(meta);
        return CsvTableCompiler.buildTable(rows, csvMeta.header(), csvMeta.widthHints());
    }

    private CsvFenceMeta parseCsvFenceMeta(@Nullable String meta) {
        if (meta == null || meta.trim()
            .isEmpty()) {
            return new CsvFenceMeta(true, Collections.emptyList());
        }

        boolean header = true;
        List<Integer> widthHints = Collections.emptyList();
        for (String token : splitMetaTokens(meta)) {
            int equalsIndex = token.indexOf('=');
            if (equalsIndex <= 0 || equalsIndex == token.length() - 1) {
                continue;
            }

            String key = token.substring(0, equalsIndex);
            String value = stripOptionalQuotes(token.substring(equalsIndex + 1));
            if ("widths".equals(key)) {
                widthHints = CsvTableCompiler.parseWidthHints(value);
            } else if ("header".equals(key)) {
                header = !"false".equalsIgnoreCase(value);
            }
        }

        return new CsvFenceMeta(header, widthHints);
    }

    private List<String> splitMetaTokens(String meta) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char quote = 0;
        for (int i = 0; i < meta.length(); i++) {
            char ch = meta.charAt(i);
            if ((ch == '"' || ch == '\'') && (!inQuotes || ch == quote)) {
                if (inQuotes && ch == quote) {
                    inQuotes = false;
                    quote = 0;
                } else if (!inQuotes) {
                    inQuotes = true;
                    quote = ch;
                }
                current.append(ch);
                continue;
            }
            if (Character.isWhitespace(ch) && !inQuotes) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }
            current.append(ch);
        }
        if (current.length() > 0) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    private String stripOptionalQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    @Desugar
    private record CsvFenceMeta(boolean header, List<Integer> widthHints) {}

    // ---- Mermaid ----

    private @Nullable LytMermaidMindmap tryCompileMermaidMindmap(String source) {
        try {
            String normalized = MermaidMindmapParser.normalize(source);
            LytMermaidMindmap block = new LytMermaidMindmap(MermaidMindmapParser.parse(normalized), normalized);
            FMLLog.getLogger()
                .info("[GuideNH] [PreCompiler] Compiled fenced Mermaid runtime block ({} chars)", normalized.length());
            return block;
        } catch (IllegalArgumentException e) {
            FMLLog.getLogger()
                .warn(
                    "[GuideNH] [PreCompiler] Failed to compile fenced Mermaid runtime block from source: {}",
                    source,
                    e);
            return null;
        }
    }

    // ---- Static helpers (copied from PageCompiler) ----

    private static boolean isFileTreeFence(@Nullable String fenceLanguage) {
        if (fenceLanguage == null) {
            return false;
        }
        String trimmed = fenceLanguage.trim();
        return "tree".equalsIgnoreCase(trimmed) || "filetree".equalsIgnoreCase(trimmed);
    }

    private static boolean isFunctionGraphFence(@Nullable String fenceLanguage) {
        if (fenceLanguage == null) {
            return false;
        }
        String trimmed = fenceLanguage.trim();
        return "funcgraph".equalsIgnoreCase(trimmed) || "function".equalsIgnoreCase(trimmed)
            || "functiongraph".equalsIgnoreCase(trimmed);
    }

    private static @Nullable Integer parseCodeBlockWidth(@Nullable String meta) {
        if (meta == null || meta.trim()
            .isEmpty()) {
            return null;
        }
        Matcher matcher = CODEBLOCK_META_WIDTH.matcher(meta);
        if (!matcher.find()) {
            return null;
        }
        String value = matcher.group(3) != null ? matcher.group(3)
            : matcher.group(4) != null ? matcher.group(4) : matcher.group(5);
        if (value == null || value.trim()
            .isEmpty()) {
            return null;
        }
        try {
            return Math.max(0, Integer.parseInt(value.trim()));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static @Nullable Integer parseCodeBlockHeight(@Nullable String meta) {
        if (meta == null || meta.trim()
            .isEmpty()) {
            return null;
        }
        Matcher matcher = CODEBLOCK_META_HEIGHT.matcher(meta);
        if (!matcher.find()) {
            return null;
        }
        String value = matcher.group(3) != null ? matcher.group(3)
            : matcher.group(4) != null ? matcher.group(4) : matcher.group(5);
        if (value == null || value.trim()
            .isEmpty()) {
            return null;
        }
        try {
            return Math.max(0, Integer.parseInt(value.trim()));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
