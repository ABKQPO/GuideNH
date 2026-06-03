package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.table.LytTable;
import com.hfstudio.guidenh.guide.document.block.table.LytTableCell;
import com.hfstudio.guidenh.guide.document.block.table.LytTableRow;
import com.hfstudio.guidenh.guide.style.TextAlignment;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;

public class TableCompiler extends BlockTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("table");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        LytTable table = new LytTable();
        table.setMarginBottom(PageCompiler.DEFAULT_ELEMENT_SPACING);

        // Parse align attribute back to list
        String alignStr = el.getAttributeString("align", "");

        boolean firstRow = true;
        int rowIndex = 0;
        for (var child : el.children()) {
            // Skip kramdown {: widths=... } meta lines
            if (child instanceof MdxJsxFlowElement meta && "table-meta".equals(meta.name())) {
                String content = meta.getAttributeString("content", "");
                if (!content.isEmpty()) {
                    java.util.List<Integer> widths = com.hfstudio.guidenh.guide.compiler.tags.CsvTableCompiler
                        .parseWidthHints(extractKramdownExpression(content));
                    var columns = table.getColumns();
                    for (int wi = 0; wi < widths.size() && wi < columns.size(); wi++) {
                        columns.get(wi)
                            .setPreferredWidth(widths.get(wi));
                    }
                }
                continue;
            }
            if (child instanceof MdxJsxFlowElement tr && "tr".equals(tr.name())) {
                LytTableRow row = table.appendRow();
                if (firstRow) {
                    row.modifyStyle(style -> style.bold(true));
                    firstRow = false;
                }

                int cellIndex = 0;
                for (var cellChild : tr.children()) {
                    if (cellChild instanceof MdxJsxFlowElement td && "td".equals(td.name())) {
                        LytTableCell cell = row.appendCell();

                        // Apply alignment from the parsed align list
                        if (!alignStr.isEmpty()) {
                            String[] parts = alignStr.split(",");
                            if (cellIndex < parts.length) {
                                switch (parts[cellIndex].trim()) {
                                    case "center" -> cell.modifyStyle(style -> style.alignment(TextAlignment.CENTER));
                                    case "right" -> cell.modifyStyle(style -> style.alignment(TextAlignment.RIGHT));
                                }
                            }
                        }

                        compiler.compileBlockContext(td.children(), cell);
                        cellIndex++;
                    }
                }
                rowIndex++;
            }
        }
        parent.append(table);
    }

    private static String extractKramdownExpression(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start + 1, end)
                .trim();
        }
        return "";
    }
}
