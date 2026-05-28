package com.hfstudio.guidenh.guide.compiler.tags;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.compiler.IdUtils;
import com.hfstudio.guidenh.guide.compiler.IndexingContext;
import com.hfstudio.guidenh.guide.compiler.IndexingSink;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.block.table.LytTable;
import com.hfstudio.guidenh.guide.document.block.table.LytTableCell;
import com.hfstudio.guidenh.guide.internal.csv.CsvTableParser;
import com.hfstudio.guidenh.guide.style.WhiteSpaceMode;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class CsvTableCompiler extends BlockTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Set.of("CsvTable");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        String src = MdxAttrs.getString(compiler, parent, el, "src", null);
        if (src == null || src.trim()
            .isEmpty()) {
            parent.appendError(compiler, "CsvTable requires a non-empty src attribute.", el);
            return;
        }

        ResourceLocation csvId;
        try {
            csvId = IdUtils.resolveLink(src.trim(), compiler.getPageId());
        } catch (IllegalArgumentException e) {
            parent.appendError(compiler, "Malformed CsvTable src: " + src, el);
            return;
        }

        byte[] data = compiler.loadAsset(csvId);
        if (data == null) {
            parent.appendError(compiler, "Missing CSV asset: " + csvId, el);
            return;
        }

        List<List<String>> rows = CsvTableParser.parse(new String(data, StandardCharsets.UTF_8));
        if (rows.isEmpty()) {
            parent.appendError(compiler, "CsvTable source is empty: " + csvId, el);
            return;
        }

        boolean header = MdxAttrs.getBoolean(compiler, parent, el, "header", true);
        parent.append(
            buildTable(rows, header, parseWidthHints(MdxAttrs.getString(compiler, parent, el, "widths", null))));
    }

    @Override
    public void index(IndexingContext indexer, MdxJsxElementFields el, IndexingSink sink) {
        String src;
        try {
            src = MdxAttrs.getString(el, "src", null);
        } catch (MdxAttrs.AttributeException e) {
            return;
        }
        if (src == null || src.trim()
            .isEmpty()) {
            return;
        }

        ResourceLocation csvId;
        try {
            csvId = IdUtils.resolveLink(src.trim(), indexer.getPageId());
        } catch (IllegalArgumentException e) {
            return;
        }

        byte[] data = indexer.loadAsset(csvId);
        if (data == null) {
            return;
        }

        List<List<String>> rows = CsvTableParser.parse(new String(data, StandardCharsets.UTF_8));
        for (List<String> row : rows) {
            for (String cell : row) {
                if (!cell.isEmpty()) {
                    sink.appendText(el, cell);
                }
            }
            sink.appendBreak();
        }
    }

    public static LytTable buildTable(List<List<String>> rows, boolean header, List<Integer> widthHints) {
        LytTable table = new LytTable();
        table.setMarginTop(PageCompiler.DEFAULT_ELEMENT_SPACING);
        table.setMarginBottom(PageCompiler.DEFAULT_ELEMENT_SPACING);

        boolean firstRow = true;
        int rowIndex = 0;
        for (List<String> values : rows) {
            var row = table.appendRow();
            if (firstRow && header) {
                row.modifyStyle(style -> style.bold(true));
            }
            for (int columnIndex = 0; columnIndex < values.size(); columnIndex++) {
                if (rowIndex == 0 && columnIndex < widthHints.size() && widthHints.get(columnIndex) > 0) {
                    table.getOrCreateColumn(columnIndex)
                        .setPreferredWidth(widthHints.get(columnIndex));
                }
                appendCellContent(row.appendCell(), values.get(columnIndex));
            }
            firstRow = false;
            rowIndex++;
        }

        return table;
    }

    public static List<Integer> parseWidthHints(String rawWidths) {
        if (rawWidths == null || rawWidths.trim()
            .isEmpty()) {
            return List.of();
        }

        List<Integer> result = new ArrayList<>();
        int length = rawWidths.length();
        int start = 0;
        while (start <= length) {
            int end = rawWidths.indexOf(',', start);
            if (end < 0) {
                end = length;
            }
            String trimmed = rawWidths.substring(start, end)
                .trim();
            if (trimmed.isEmpty()) {
                result.add(0);
            } else {
                try {
                    result.add(Math.max(0, Integer.parseInt(trimmed)));
                } catch (NumberFormatException e) {
                    result.add(0);
                }
            }
            if (end == length) {
                break;
            }
            start = end + 1;
        }
        return result;
    }

    private static void appendCellContent(LytTableCell cell, String value) {
        LytParagraph paragraph = new LytParagraph();
        paragraph.setMarginTop(0);
        paragraph.setMarginBottom(0);
        paragraph.modifyStyle(style -> style.whiteSpace(WhiteSpaceMode.PRE_WRAP));

        int start = 0;
        while (start <= value.length()) {
            int end = value.indexOf('\n', start);
            if (end < 0) {
                end = value.length();
            }
            paragraph.appendText(value.substring(start, end));
            if (end == value.length()) {
                break;
            }
            paragraph.appendBreak();
            start = end + 1;
        }

        cell.append(paragraph);
    }
}
