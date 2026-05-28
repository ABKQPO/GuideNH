package com.hfstudio.guidenh.guide.internal.editor.guide;

public class GuideScreenEditorTextActions {

    private GuideScreenEditorTextActions() {}

    public static class Result {

        private final String text;
        private final int selectionStart;
        private final int selectionEnd;

        public Result(String text, int selectionStart, int selectionEnd) {
            this.text = text != null ? text : "";
            this.selectionStart = Math.max(0, selectionStart);
            this.selectionEnd = Math.max(this.selectionStart, selectionEnd);
        }

        public String getText() {
            return text;
        }

        public int getSelectionStart() {
            return selectionStart;
        }

        public int getSelectionEnd() {
            return selectionEnd;
        }
    }

    public static Result apply(GuideScreenEditorAction action, String text, int selectionStart, int selectionEnd) {
        String source = text != null ? text : "";
        int start = clamp(selectionStart, 0, source.length());
        int end = clamp(selectionEnd, 0, source.length());
        if (start > end) {
            int swap = start;
            start = end;
            end = swap;
        }

        if (action == null) {
            return new Result(source, start, end);
        }
        return switch (action) {
            case HEADING_1 -> applyLinePrefix(source, start, end, "# ", 0, 0);
            case HEADING_2 -> applyLinePrefix(source, start, end, "## ", 0, 0);
            case HEADING_3 -> applyLinePrefix(source, start, end, "### ", 0, 0);
            case HEADING_4 -> applyLinePrefix(source, start, end, "#### ", 0, 0);
            case HEADING_5 -> applyLinePrefix(source, start, end, "##### ", 0, 0);
            case HEADING_6 -> applyLinePrefix(source, start, end, "###### ", 0, 0);
            case BOLD -> wrap(source, start, end, "**", "**", "", "");
            case ITALIC -> wrap(source, start, end, "*", "*", "", "");
            case STRIKETHROUGH -> wrap(source, start, end, "~~", "~~", "", "");
            case UNDERLINE -> wrap(source, start, end, "++", "++", "", "");
            case KBD -> wrap(source, start, end, "<kbd>", "</kbd>", "key", "");
            case SUBSCRIPT -> wrap(source, start, end, "<sub>", "</sub>", "1", "");
            case SUPERSCRIPT -> wrap(source, start, end, "<sup>", "</sup>", "2", "");
            case FOOTNOTE -> applyFootnote(source, start, end);
            case TOOLTIP -> applyTooltip(source, start, end);
            case ITEM_IMAGE -> applyItemTag(source, start, end, "ItemImage");
            case BLOCK_IMAGE -> applyItemTag(source, start, end, "BlockImage");
            case ITEM_LINK -> applyItemTag(source, start, end, "ItemLink");
            case LATEX -> applyAttributeTag(source, start, end, "Latex", "formula", "E=mc^2", true);
            case CSV_TABLE -> applyCsvTable(source, start, end);
            case COMMAND_LINK -> applyCommandLink(source, start, end);
            case RECIPE -> applyRecipeTag(source, start, end, "Recipe", " fallbackText=\"Recipe unavailable.\"");
            case RECIPE_FOR -> applyRecipeTag(source, start, end, "RecipeFor", "");
            case RECIPES_FOR -> applyRecipeTag(source, start, end, "RecipesFor", " limit=\"3\"");
            case FLOATING_IMAGE -> applyFloatingImage(source, start, end);
            case MERMAID -> applyNamedBlock(
                source,
                start,
                end,
                "<Mermaid width=\"320\" height=\"220\">",
                "</Mermaid>",
                "mindmap\n  root((GuideNH))");
            case FILE_TREE -> applyNamedBlock(
                source,
                start,
                end,
                "<FileTree indent=\"16\" gap=\"2\">",
                "</FileTree>",
                "project\n  src\n  docs");
            case SUB_PAGES -> applyAttributeTag(source, start, end, "SubPages", "id", "", true);
            case CATEGORY -> applyNamedTag(source, start, end, "Category", "name", "general", " rows=\"3\"", true);
            case FOOTNOTE_LIST -> applyNamedBlock(
                source,
                start,
                end,
                "<FootnoteList width=\"220\">",
                "</FootnoteList>",
                "[^note]: tooltip text");
            case ROW -> applyNamedBlock(
                source,
                start,
                end,
                "<Row gap=\"5\" alignItems=\"START\">",
                "</Row>",
                "Left\nRight");
            case COLUMN -> applyNamedBlock(
                source,
                start,
                end,
                "<Column gap=\"5\" alignItems=\"START\">",
                "</Column>",
                "Top\nBottom");
            case DIV -> applyNamedBlock(source, start, end, "<div>", "</div>", "Content");
            case ITEM_GRID -> applyNamedBlock(
                source,
                start,
                end,
                "<ItemGrid>",
                "</ItemGrid>",
                "  <ItemIcon id=\"minecraft:stone\" />\n  <ItemIcon id=\"minecraft:diamond\" />");
            case CSV_TABLE_IMPORT -> applyAttributeTag(source, start, end, "CsvTable", "src", "./data.csv", true);
            case ANCHOR -> applyAttributeTag(source, start, end, "a", "name", "anchor-name", true);
            case COLUMN_CHART -> applyNamedBlock(
                source,
                start,
                end,
                "<ColumnChart title=\"Quarterly Output\" categories=\"Q1,Q2,Q3,Q4\" labelPosition=\"above\">",
                "</ColumnChart>",
                "  <Series name=\"Iron\" data=\"120,180,150,210\" color=\"#4E79A7\" />");
            case BAR_CHART -> applyNamedBlock(
                source,
                start,
                end,
                "<BarChart title=\"Mod Downloads\" categories=\"GTNH,IC2,Thermal,Mekanism\" labelPosition=\"outside\">",
                "</BarChart>",
                "  <Series name=\"Downloads\" data=\"320,210,180,150\" />");
            case LINE_CHART -> applyNamedBlock(
                source,
                start,
                end,
                "<LineChart title=\"Temperature\" categories=\"Mon,Tue,Wed,Thu,Fri\" yAxisUnit=\"C\">",
                "</LineChart>",
                "  <Series name=\"Outdoor\" data=\"5,8,11,9,6\" color=\"#4E79A7\" />");
            case PIE_CHART -> applyNamedBlock(
                source,
                start,
                end,
                "<PieChart title=\"Resource Share\" labelPosition=\"outside\" legend=\"right\">",
                "</PieChart>",
                "  <Slice label=\"Iron\" value=\"45\" color=\"#4E79A7\" />\n"
                    + "  <Slice label=\"Copper\" value=\"25\" color=\"#F28E2B\" />");
            case SCATTER_CHART -> applyNamedBlock(
                source,
                start,
                end,
                "<ScatterChart title=\"Height-Weight\" xAxisLabel=\"Height\" yAxisLabel=\"Weight\">",
                "</ScatterChart>",
                "  <Series name=\"Sample\" points=\"160:55,170:65,180:78\" color=\"#4E79A7\" />");
            case CHART_SERIES -> applyChartSeries(source, start, end);
            case CHART_LINE_SERIES -> applyChartLineSeries(source, start, end);
            case CHART_SLICE -> applyChartSlice(source, start, end);
            case CHART_PIE_INSET -> applyNamedBlock(
                source,
                start,
                end,
                "<PieInset title=\"Share\" position=\"topRight\" size=\"56\">",
                "</PieInset>",
                "  <Slice label=\"Iron\" value=\"45\" color=\"#4E79A7\" />\n"
                    + "  <Slice label=\"Copper\" value=\"25\" color=\"#F28E2B\" />");
            case FUNCTION_GRAPH -> applyNamedBlock(
                source,
                start,
                end,
                "<FunctionGraph width=\"360\" height=\"220\" xRange=\"-6..6\" yRange=\"-3..3\" quadrants=\"all\">",
                "</FunctionGraph>",
                "  <Plot expr=\"sin(x)\" color=\"#ff5566\" label=\"sin x\" />\n  <Point x=\"0\" y=\"0\" />");
            case FUNCTION -> applyFunction(source, start, end);
            case FUNCTION_PLOT -> applyFunctionPlot(source, start, end);
            case FUNCTION_POINT -> applyFunctionPoint(source, start, end);
            case FUNCTION_GRAPH_FENCE -> applyFunctionGraphFence(source, start, end);
            case STRUCTURE -> applyNamedBlock(
                source,
                start,
                end,
                "<Structure width=\"192\" height=\"144\">",
                "</Structure>",
                "0 0 0 minecraft:stone");
            case GAME_SCENE -> applyNamedBlock(
                source,
                start,
                end,
                "<GameScene width=\"256\" height=\"160\" zoom={4} interactive={true}>",
                "</GameScene>",
                "  <Block id=\"minecraft:stone\" />");
            case SCENE_BLOCK -> applySceneBlock(source, start, end);
            case SCENE_ENTITY -> applySceneEntity(source, start, end);
            case ISOMETRIC_CAMERA -> applyIsometricCamera(source, start, end);
            case BOX_ANNOTATION -> applyNamedBlock(
                source,
                start,
                end,
                "<BoxAnnotation color=\"#ee3333\" min=\"0 0 0\" max=\"1 1 1\" thickness=\"0.04\">",
                "</BoxAnnotation>",
                "Important area");
            case BLOCK_ANNOTATION -> applyNamedBlock(
                source,
                start,
                end,
                "<BlockAnnotation color=\"#33ddee\" pos=\"0 0 0\" alwaysOnTop={true}>",
                "</BlockAnnotation>",
                "Block note");
            case LINE_ANNOTATION -> applyNamedBlock(
                source,
                start,
                end,
                "<LineAnnotation color=\"#ffd24c\" from=\"0.5 1.2 0.5\" to=\"2.5 1.2 2.5\" thickness=\"0.08\">",
                "</LineAnnotation>",
                "Line note");
            case DIAMOND_ANNOTATION -> applyNamedBlock(
                source,
                start,
                end,
                "<DiamondAnnotation pos=\"0.5 1.2 0.5\" color=\"#ffd24c\">",
                "</DiamondAnnotation>",
                "Point note");
            case TEXT_ANNOTATION -> applyNamedBlock(
                source,
                start,
                end,
                "<TextAnnotation pos=\"0.5 1.4 0.5\" color=\"#ffffff\" maxWidth=\"120\">",
                "</TextAnnotation>",
                "Scene note");
            case BLOCK_ANNOTATION_TEMPLATE -> applyNamedBlock(
                source,
                start,
                end,
                "<BlockAnnotationTemplate id=\"minecraft:stone\">",
                "</BlockAnnotationTemplate>",
                "  <DiamondAnnotation pos=\"0.5 0.5 0.5\" color=\"#ff0000\">\n    Stone\n  </DiamondAnnotation>");
            case IMPORT_STRUCTURE -> applyImportFile(
                source,
                start,
                end,
                "ImportStructure",
                "src",
                "./scene.snbt",
                " x=\"0\" y=\"0\" z=\"0\"");
            case IMPORT_STRUCTURE_LIB -> applyImportStructureLib(source, start, end);
            case IMPORT_PONDER -> applyImportFile(source, start, end, "ImportPonder", "src", "./scene.json", "");
            case PLACE_BLOCK -> applyPlaceBlock(source, start, end);
            case REPLACE_BLOCK -> applyReplaceBlock(source, start, end);
            case REMOVE_BLOCKS -> applyRemoveBlocks(source, start, end);
            case QUEST_LINK -> applyQuestTag(source, start, end, "QuestLink");
            case QUEST_CARD -> applyQuestTag(source, start, end, "QuestCard");
            case QUEST_IDS -> applyQuestIdsFrontmatter(source, start, end);
            case NAV_POSITION -> applyNavigationScalarFrontmatter(source, start, end, "position", "0", true);
            case NAV_ICON -> applyNavigationScalarFrontmatter(source, start, end, "icon", "minecraft:book", false);
            case NAV_ICON_TEXTURE -> applyNavigationScalarFrontmatter(
                source,
                start,
                end,
                "icon_texture",
                "test1.png",
                false);
            case NAV_ICONS -> applyNavigationListFrontmatter(source, start, end, "icons", "minecraft:book");
            case NAV_ICON_TEXTURES -> applyNavigationListFrontmatter(source, start, end, "icon_textures", "test1.png");
            case NAV_REQUIRED_MODS -> applyNavigationRequiredModsFrontmatter(source, start, end);
            case PAGE_CATEGORIES -> applyTopLevelListFrontmatter(source, start, end, "categories", "general");
            case PAGE_ITEM_IDS -> applyTopLevelListFrontmatter(source, start, end, "item_ids", "minecraft:stone");
            case PAGE_ORE_IDS -> applyTopLevelListFrontmatter(source, start, end, "ore_ids", "ingotIron");
            case PAGE_METADATA -> applyPageMetadataFrontmatter(source, start, end);
            case QUOTE_CALLOUT -> applyQuoteDirective(source, start, end, "title=\"Callout\" color=\"#7C8795\"");
            case QUOTE_ICON_TEXT -> applyQuoteDirective(
                source,
                start,
                end,
                "title=\"Callout\" color=\"#7C8795\" icon=\"i\"");
            case QUOTE_ICON_ITEM -> applyQuoteDirective(
                source,
                start,
                end,
                "title=\"Callout\" color=\"#7C8795\" iconItem=\"minecraft:stone\"");
            case QUOTE_ICON_PNG -> applyQuoteDirective(
                source,
                start,
                end,
                "title=\"Callout\" color=\"#7C8795\" iconPng=\"./icon.png\"");
            case LATEX_SHORTHAND -> wrap(source, start, end, "$$", "$$", "formula", "");
            case LINK -> wrap(source, start, end, "[", "]()", "", "");
            case IMAGE -> wrap(source, start, end, "![](", ")", "", "");
            case INLINE_CODE -> wrap(source, start, end, "`", "`", "", "");
            case CODE_BLOCK -> wrapBlock(source, start, end, "```", "```");
            case BLOCKQUOTE -> applyLinePrefix(source, start, end, "> ", 0, 0);
            case UNORDERED_LIST -> applyLinePrefix(source, start, end, "- ", 0, 0);
            case ORDERED_LIST -> applyOrderedList(source, start, end);
            case TASK_LIST -> applyLinePrefix(source, start, end, "- [ ] ", 0, 0);
            case TABLE -> applyTableTemplate(source, start, end);
            case ALERT_NOTE -> applyAlertBlock(source, start, end, "NOTE");
            case ALERT_TIP -> applyAlertBlock(source, start, end, "TIP");
            case ALERT_IMPORTANT -> applyAlertBlock(source, start, end, "IMPORTANT");
            case ALERT_WARNING -> applyAlertBlock(source, start, end, "WARNING");
            case ALERT_CAUTION -> applyAlertBlock(source, start, end, "CAUTION");
            case DETAILS -> applyDetailsBlock(source, start, end);
            case KEY_BIND -> applyAttributeTag(source, start, end, "KeyBind", "id", "key.jump", true);
            case PLAYER_NAME -> insertAt(source, start, end, "<PlayerName />", 0, 0);
            case COLOR -> applyColor(source, start, end);
            case BREAK -> insertAt(source, start, end, "<br clear=\"all\" />", 0, 0);
            case REFERENCE_LINK -> applyReference(source, start, end, false);
            case REFERENCE_IMAGE -> applyReference(source, start, end, true);
            case THEMATIC_BREAK -> insertAt(source, start, end, "\n---\n", 1, 4);
            default -> new Result(source, start, end);
        };
    }

    public static String formatDocument(String text) {
        String source = normalizeLineEndings(text);
        String[] lines = source.split("\n", -1);
        StringBuilder formatted = new StringBuilder(source.length());
        int mdxIndent = 0;
        boolean previousBlank = false;
        for (int i = 0; i < lines.length; i++) {
            String rawLine = trimRight(lines[i]);
            String trimmed = rawLine.trim();
            if (trimmed.isEmpty()) {
                if (!previousBlank && i + 1 < lines.length) {
                    formatted.append('\n');
                }
                previousBlank = true;
                continue;
            }
            previousBlank = false;
            if (isMdxClosingLine(trimmed)) {
                mdxIndent = Math.max(0, mdxIndent - 1);
            }
            if (shouldIndentLine(trimmed)) {
                appendSpaces(formatted, mdxIndent * 4);
            }
            formatted.append(trimmed)
                .append('\n');
            if (opensMdxBlock(trimmed)) {
                mdxIndent++;
            }
        }
        if (formatted.length() > 0 && formatted.charAt(formatted.length() - 1) == '\n' && !source.endsWith("\n")) {
            formatted.setLength(formatted.length() - 1);
        }
        return formatted.toString();
    }

    private static boolean shouldIndentLine(String trimmed) {
        return trimmed.startsWith("<") && !trimmed.startsWith("<!--") && !trimmed.startsWith("<!");
    }

    private static boolean isMdxClosingLine(String trimmed) {
        return trimmed.startsWith("</");
    }

    private static boolean opensMdxBlock(String trimmed) {
        return trimmed.startsWith("<") && !trimmed.startsWith("</")
            && !trimmed.endsWith("/>")
            && !trimmed.contains("</")
            && !trimmed.startsWith("<!--");
    }

    private static String normalizeLineEndings(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.replace("\r\n", "\n")
            .replace('\r', '\n');
    }

    private static String trimRight(String line) {
        int end = line.length();
        while (end > 0 && Character.isWhitespace(line.charAt(end - 1)) && line.charAt(end - 1) != '\n') {
            end--;
        }
        return line.substring(0, end);
    }

    private static void appendSpaces(StringBuilder out, int count) {
        for (int i = 0; i < count; i++) {
            out.append(' ');
        }
    }

    private static Result applyRecipeTag(String source, int start, int end, String tagName, String extraAttributes) {
        String selected = start < end ? sanitizeAttributeValue(source.substring(start, end)) : "";
        String itemId = selected.isEmpty() ? "minecraft:stone" : selected;
        String replacement = "<" + tagName + " id=\"" + itemId + "\"" + extraAttributes + " />";
        int caretStart = start + replacement.indexOf(itemId);
        return new Result(
            source.substring(0, start) + replacement + source.substring(end),
            caretStart,
            caretStart + itemId.length());
    }

    private static Result applySceneBlock(String source, int start, int end) {
        String selected = start < end ? sanitizeAttributeValue(source.substring(start, end)) : "";
        String blockId = selected.isEmpty() ? "minecraft:stone" : selected;
        String replacement = "<Block id=\"" + blockId + "\" />";
        int caretStart = start + replacement.indexOf(blockId);
        return new Result(
            source.substring(0, start) + replacement + source.substring(end),
            caretStart,
            caretStart + blockId.length());
    }

    private static Result applySceneEntity(String source, int start, int end) {
        String selected = start < end ? sanitizeAttributeValue(source.substring(start, end)) : "";
        String entityId = selected.isEmpty() ? "minecraft:sheep" : selected;
        String replacement = "<Entity id=\"" + entityId + "\" x=\"0.5\" y=\"1\" z=\"0.5\" rotationY=\"-45\" />";
        int caretStart = start + replacement.indexOf(entityId);
        return new Result(
            source.substring(0, start) + replacement + source.substring(end),
            caretStart,
            caretStart + entityId.length());
    }

    private static Result applyIsometricCamera(String source, int start, int end) {
        String replacement = "<IsometricCamera yaw=\"225\" pitch=\"30\" />";
        int caretStart = start + replacement.indexOf("225");
        return new Result(source.substring(0, start) + replacement + source.substring(end), caretStart, caretStart + 3);
    }

    private static Result applyImportFile(String source, int start, int end, String tagName, String attribute,
        String defaultValue, String extraAttributes) {
        String selected = start < end ? sanitizeAttributeValue(source.substring(start, end)) : "";
        String value = selected.isEmpty() ? defaultValue : selected;
        String replacement = "<" + tagName + " " + attribute + "=\"" + value + "\"" + extraAttributes + " />";
        int caretStart = start + replacement.indexOf(value);
        return new Result(
            source.substring(0, start) + replacement + source.substring(end),
            caretStart,
            caretStart + value.length());
    }

    private static Result applyImportStructureLib(String source, int start, int end) {
        String selected = start < end ? sanitizeAttributeValue(source.substring(start, end)) : "";
        String controller = selected.isEmpty() ? "example" : selected;
        String replacement = "<ImportStructureLib controller=\"" + controller + "\" channel=\"0\" />";
        int caretStart = start + replacement.indexOf(controller);
        return new Result(
            source.substring(0, start) + replacement + source.substring(end),
            caretStart,
            caretStart + controller.length());
    }

    private static Result applyPlaceBlock(String source, int start, int end) {
        String selected = start < end ? sanitizeAttributeValue(source.substring(start, end)) : "";
        String blockId = selected.isEmpty() ? "minecraft:stone" : selected;
        String replacement = "<PlaceBlock id=\"" + blockId + "\" x=\"0\" y=\"0\" z=\"0\" dx=\"1\" dy=\"1\" dz=\"1\" />";
        int caretStart = start + replacement.indexOf(blockId);
        return new Result(
            source.substring(0, start) + replacement + source.substring(end),
            caretStart,
            caretStart + blockId.length());
    }

    private static Result applyReplaceBlock(String source, int start, int end) {
        String selected = start < end ? sanitizeAttributeValue(source.substring(start, end)) : "";
        String fromBlock = selected.isEmpty() ? "minecraft:stone" : selected;
        String toBlock = "minecraft:glass";
        String replacement = "<ReplaceBlock from=\"" + fromBlock
            + "\" to=\""
            + toBlock
            + "\" x=\"0\" y=\"0\" z=\"0\" dx=\"1\" dy=\"1\" dz=\"1\" />";
        int caretStart = start + replacement.indexOf(toBlock);
        return new Result(
            source.substring(0, start) + replacement + source.substring(end),
            caretStart,
            caretStart + toBlock.length());
    }

    private static Result applyRemoveBlocks(String source, int start, int end) {
        String selected = start < end ? sanitizeAttributeValue(source.substring(start, end)) : "";
        String blockId = selected.isEmpty() ? "minecraft:stone" : selected;
        String replacement = "<RemoveBlocks id=\"" + blockId + "\" />";
        int caretStart = start + replacement.indexOf(blockId);
        return new Result(
            source.substring(0, start) + replacement + source.substring(end),
            caretStart,
            caretStart + blockId.length());
    }

    private static Result applyQuestTag(String source, int start, int end, String tagName) {
        String selected = start < end ? sanitizeAttributeValue(source.substring(start, end)) : "";
        String questId = isUuidLike(selected) ? selected : "00000000-0000-0000-0000-000000000000";
        String replacement = "<" + tagName + " id=\"" + questId + "\" />";
        int caretStart = start + replacement.indexOf(questId);
        return new Result(
            source.substring(0, start) + replacement + source.substring(end),
            caretStart,
            caretStart + questId.length());
    }

    private static Result applyQuestIdsFrontmatter(String source, int start, int end) {
        Range range = resolveTargetRange(source, start, end);
        String selected = sanitizeInline(source.substring(range.start, range.end));
        String questId = isUuidLike(selected) ? selected : "00000000-0000-0000-0000-000000000000";
        Range removal = resolveQuestIdsRemovalRange(source, start, end, selected);
        int frontmatterEnd = findFrontmatterEnd(source);
        String updatedSource;
        if (frontmatterEnd >= 0) {
            String frontmatter = source.substring(0, frontmatterEnd);
            String body = source.substring(frontmatterEnd);
            String updatedFrontmatter = insertQuestIdIntoFrontmatter(frontmatter, questId);
            String updatedBody = removeRange(body, removal, frontmatterEnd);
            updatedSource = updatedFrontmatter + updatedBody;
            int caretStart = updatedFrontmatter.indexOf(questId);
            return new Result(updatedSource, caretStart, caretStart + questId.length());
        }

        String frontmatter = "---\nquest_ids:\n  - " + questId + "\n---\n\n";
        String body = removeRange(source, removal, 0);
        updatedSource = frontmatter + body;
        int caretStart = frontmatter.indexOf(questId);
        return new Result(updatedSource, caretStart, caretStart + questId.length());
    }

    private static Result applyPageMetadataFrontmatter(String source, int start, int end) {
        Range range = resolveTargetRange(source, start, end);
        String selected = sanitizeInline(source.substring(range.start, range.end));
        String author = selected.isEmpty() ? "GuideNH" : selected;
        Range removal = resolveWholeLineRemovalRange(source, start, end, selected);
        int frontmatterEnd = findFrontmatterEnd(source);
        String metadata = "author: " + author + "\n" + "date: 2024-01-01\n" + "updated: 2024-01-01\n" + "zoom: 1.0\n";
        String updatedSource;
        if (frontmatterEnd >= 0) {
            String frontmatter = source.substring(0, frontmatterEnd);
            String body = source.substring(frontmatterEnd);
            String updatedFrontmatter = insertMissingFrontmatterMetadata(frontmatter, metadata);
            String updatedBody = removeRange(body, removal, frontmatterEnd);
            updatedSource = updatedFrontmatter + updatedBody;
            int caretStart = updatedFrontmatter.indexOf(author);
            return new Result(updatedSource, caretStart, caretStart + author.length());
        }

        String frontmatter = "---\n" + metadata + "---\n\n";
        String body = removeRange(source, removal, 0);
        updatedSource = frontmatter + body;
        int caretStart = frontmatter.indexOf(author);
        return new Result(updatedSource, caretStart, caretStart + author.length());
    }

    private static Result applyTopLevelListFrontmatter(String source, int start, int end, String key,
        String defaultValue) {
        Range range = resolveTargetRange(source, start, end);
        String selected = sanitizeInline(source.substring(range.start, range.end));
        String value = selected.isEmpty() ? defaultValue : selected;
        Range removal = resolveWholeLineRemovalRange(source, start, end, selected);
        int frontmatterEnd = findFrontmatterEnd(source);
        String updatedSource;
        if (frontmatterEnd >= 0) {
            String frontmatter = source.substring(0, frontmatterEnd);
            String body = source.substring(frontmatterEnd);
            String updatedFrontmatter = insertTopLevelListValueIntoFrontmatter(frontmatter, key, value);
            String updatedBody = removeRange(body, removal, frontmatterEnd);
            updatedSource = updatedFrontmatter + updatedBody;
            int caretStart = updatedFrontmatter.indexOf(value);
            return new Result(updatedSource, caretStart, caretStart + value.length());
        }

        String frontmatter = "---\n" + key + ":\n  - " + value + "\n---\n\n";
        String body = removeRange(source, removal, 0);
        updatedSource = frontmatter + body;
        int caretStart = frontmatter.indexOf(value);
        return new Result(updatedSource, caretStart, caretStart + value.length());
    }

    private static Result applyNavigationRequiredModsFrontmatter(String source, int start, int end) {
        Range range = resolveTargetRange(source, start, end);
        String selected = sanitizeInline(source.substring(range.start, range.end));
        String modId = selected.isEmpty() ? "minecraft" : selected;
        Range removal = resolveWholeLineRemovalRange(source, start, end, selected);
        int frontmatterEnd = findFrontmatterEnd(source);
        String updatedSource;
        if (frontmatterEnd >= 0) {
            String frontmatter = source.substring(0, frontmatterEnd);
            String body = source.substring(frontmatterEnd);
            String updatedFrontmatter = insertRequiredModIntoFrontmatter(frontmatter, modId);
            String updatedBody = removeRange(body, removal, frontmatterEnd);
            updatedSource = updatedFrontmatter + updatedBody;
            int caretStart = updatedFrontmatter.indexOf(modId);
            return new Result(updatedSource, caretStart, caretStart + modId.length());
        }

        String frontmatter = "---\nnavigation:\n  title: New Page\n  required_mods:\n    - " + modId + "\n---\n\n";
        String body = removeRange(source, removal, 0);
        updatedSource = frontmatter + body;
        int caretStart = frontmatter.indexOf(modId);
        return new Result(updatedSource, caretStart, caretStart + modId.length());
    }

    private static Result applyNavigationScalarFrontmatter(String source, int start, int end, String key,
        String defaultValue, boolean integerOnly) {
        Range range = resolveTargetRange(source, start, end);
        String selected = sanitizeInline(source.substring(range.start, range.end));
        String value = integerOnly ? sanitizeIntegerValue(selected, defaultValue) : selected;
        if (value.isEmpty()) {
            value = defaultValue;
        }
        Range removal = resolveWholeLineRemovalRange(source, start, end, selected);
        int frontmatterEnd = findFrontmatterEnd(source);
        String updatedSource;
        if (frontmatterEnd >= 0) {
            String frontmatter = source.substring(0, frontmatterEnd);
            String body = source.substring(frontmatterEnd);
            String updatedFrontmatter = insertNavigationScalarIntoFrontmatter(frontmatter, key, value);
            String updatedBody = removeRange(body, removal, frontmatterEnd);
            updatedSource = updatedFrontmatter + updatedBody;
            int caretStart = updatedFrontmatter.indexOf(value);
            return new Result(updatedSource, caretStart, caretStart + value.length());
        }

        String frontmatter = "---\nnavigation:\n  title: New Page\n  " + key + ": " + value + "\n---\n\n";
        String body = removeRange(source, removal, 0);
        updatedSource = frontmatter + body;
        int caretStart = frontmatter.indexOf(value);
        return new Result(updatedSource, caretStart, caretStart + value.length());
    }

    private static Result applyNavigationListFrontmatter(String source, int start, int end, String key,
        String defaultValue) {
        Range range = resolveTargetRange(source, start, end);
        String selected = sanitizeInline(source.substring(range.start, range.end));
        String value = selected.isEmpty() ? defaultValue : selected;
        Range removal = resolveWholeLineRemovalRange(source, start, end, selected);
        int frontmatterEnd = findFrontmatterEnd(source);
        String updatedSource;
        if (frontmatterEnd >= 0) {
            String frontmatter = source.substring(0, frontmatterEnd);
            String body = source.substring(frontmatterEnd);
            String updatedFrontmatter = insertNavigationListValueIntoFrontmatter(frontmatter, key, value);
            String updatedBody = removeRange(body, removal, frontmatterEnd);
            updatedSource = updatedFrontmatter + updatedBody;
            int caretStart = updatedFrontmatter.indexOf(value);
            return new Result(updatedSource, caretStart, caretStart + value.length());
        }

        String frontmatter = "---\nnavigation:\n  title: New Page\n  " + key + ":\n    - " + value + "\n---\n\n";
        String body = removeRange(source, removal, 0);
        updatedSource = frontmatter + body;
        int caretStart = frontmatter.indexOf(value);
        return new Result(updatedSource, caretStart, caretStart + value.length());
    }

    private static Result applyQuoteDirective(String source, int start, int end, String directive) {
        Range range = resolveTargetRange(source, start, end);
        String body = source.substring(range.start, range.end)
            .trim();
        String replacement = "> {:" + directive + "}\n> ";
        int caret;
        if (!body.isEmpty()) {
            replacement += body.replace("\n", "\n> ");
        }
        caret = range.start + replacement.length();
        return new Result(source.substring(0, range.start) + replacement + source.substring(range.end), caret, caret);
    }

    private static int findFrontmatterEnd(String source) {
        if (source == null || !source.startsWith("---\n")) {
            return -1;
        }
        int lineStart = 4;
        while (lineStart <= source.length()) {
            int lineEnd = findLineEnd(source, lineStart);
            String line = source.substring(lineStart, Math.min(lineEnd, source.length()))
                .trim();
            if ("---".equals(line)) {
                return lineEnd;
            }
            if (lineEnd <= lineStart) {
                break;
            }
            lineStart = lineEnd;
        }
        return -1;
    }

    private static String insertQuestIdIntoFrontmatter(String frontmatter, String questId) {
        int questIdsLineStart = findLineStartWithPrefix(frontmatter, "quest_ids:");
        if (questIdsLineStart >= 0) {
            if (frontmatter.contains(questId)) {
                return frontmatter;
            }
            int questIdsBlockEnd = findYamlListBlockEnd(frontmatter, questIdsLineStart);
            String insertion = "  - " + questId + "\n";
            return frontmatter.substring(0, questIdsBlockEnd) + insertion + frontmatter.substring(questIdsBlockEnd);
        }

        int closingStart = frontmatter.lastIndexOf("\n---");
        if (closingStart < 0) {
            closingStart = Math.max(0, frontmatter.length() - 4);
        }
        String insertion = "quest_ids:\n  - " + questId + "\n";
        return frontmatter.substring(0, closingStart + 1) + insertion + frontmatter.substring(closingStart + 1);
    }

    private static String insertTopLevelListValueIntoFrontmatter(String frontmatter, String key, String value) {
        int listLineStart = findLineStartWithPrefix(frontmatter, key + ":");
        if (listLineStart >= 0) {
            if (containsYamlListValue(frontmatter.substring(listLineStart), value)) {
                return frontmatter;
            }
            int listBlockEnd = findYamlListBlockEnd(frontmatter, listLineStart);
            String insertion = "  - " + value + "\n";
            return frontmatter.substring(0, listBlockEnd) + insertion + frontmatter.substring(listBlockEnd);
        }

        return insertLineBeforeFrontmatterClose(frontmatter, key + ":\n  - " + value + "\n");
    }

    private static String insertRequiredModIntoFrontmatter(String frontmatter, String modId) {
        int requiredModsLineStart = findLineStartWithPrefix(frontmatter, "required_mods:");
        if (requiredModsLineStart >= 0) {
            if (containsYamlListValue(frontmatter, modId)) {
                return frontmatter;
            }
            int requiredModsBlockEnd = findYamlListBlockEnd(frontmatter, requiredModsLineStart);
            String insertion = "    - " + modId + "\n";
            return frontmatter.substring(0, requiredModsBlockEnd) + insertion
                + frontmatter.substring(requiredModsBlockEnd);
        }

        int navigationLineStart = findLineStartWithPrefix(frontmatter, "navigation:");
        if (navigationLineStart >= 0) {
            int navigationEnd = findNavigationBlockEnd(frontmatter, navigationLineStart);
            String insertion = "  required_mods:\n    - " + modId + "\n";
            return frontmatter.substring(0, navigationEnd) + insertion + frontmatter.substring(navigationEnd);
        }

        String navigation = "navigation:\n  title: New Page\n  required_mods:\n    - " + modId + "\n";
        return insertLineBeforeFrontmatterClose(frontmatter, navigation);
    }

    private static String insertNavigationScalarIntoFrontmatter(String frontmatter, String key, String value) {
        int navigationLineStart = findLineStartWithPrefix(frontmatter, "navigation:");
        if (navigationLineStart < 0) {
            return insertLineBeforeFrontmatterClose(
                frontmatter,
                "navigation:\n  title: New Page\n  " + key + ": " + value + "\n");
        }

        int navigationEnd = findNavigationBlockEnd(frontmatter, navigationLineStart);
        int existingLineStart = findLineStartWithPrefixInRange(
            frontmatter,
            key + ":",
            navigationLineStart,
            navigationEnd);
        if (existingLineStart >= 0) {
            int existingLineEnd = findLineEnd(frontmatter, existingLineStart);
            String replacement = "  " + key + ": " + value + "\n";
            return frontmatter.substring(0, existingLineStart) + replacement + frontmatter.substring(existingLineEnd);
        }

        String insertion = "  " + key + ": " + value + "\n";
        return frontmatter.substring(0, navigationEnd) + insertion + frontmatter.substring(navigationEnd);
    }

    private static String insertNavigationListValueIntoFrontmatter(String frontmatter, String key, String value) {
        int navigationLineStart = findLineStartWithPrefix(frontmatter, "navigation:");
        if (navigationLineStart < 0) {
            String navigation = "navigation:\n  title: New Page\n  " + key + ":\n    - " + value + "\n";
            return insertLineBeforeFrontmatterClose(frontmatter, navigation);
        }

        int navigationEnd = findNavigationBlockEnd(frontmatter, navigationLineStart);
        int listLineStart = findLineStartWithPrefixInRange(frontmatter, key + ":", navigationLineStart, navigationEnd);
        if (listLineStart >= 0) {
            if (containsYamlListValue(frontmatter.substring(listLineStart, navigationEnd), value)) {
                return frontmatter;
            }
            int listBlockEnd = findYamlListBlockEnd(frontmatter, listLineStart);
            String insertion = "    - " + value + "\n";
            return frontmatter.substring(0, listBlockEnd) + insertion + frontmatter.substring(listBlockEnd);
        }

        String insertion = "  " + key + ":\n    - " + value + "\n";
        return frontmatter.substring(0, navigationEnd) + insertion + frontmatter.substring(navigationEnd);
    }

    private static int findNavigationBlockEnd(String frontmatter, int navigationLineStart) {
        int lineStart = findLineEnd(frontmatter, navigationLineStart);
        while (lineStart < frontmatter.length()) {
            int lineEnd = findLineEnd(frontmatter, lineStart);
            String line = frontmatter.substring(lineStart, Math.min(lineEnd, frontmatter.length()));
            String trimmed = line.trim();
            if ("---".equals(trimmed) || (!line.startsWith(" ") && !line.startsWith("\t") && !trimmed.isEmpty())) {
                return lineStart;
            }
            if (lineEnd <= lineStart) {
                break;
            }
            lineStart = lineEnd;
        }
        return frontmatter.length();
    }

    private static int findYamlListBlockEnd(String text, int listLineStart) {
        int keyIndent = countLeadingIndent(text, listLineStart);
        int lineStart = findLineEnd(text, listLineStart);
        while (lineStart < text.length()) {
            int lineEnd = findLineEnd(text, lineStart);
            String line = text.substring(lineStart, Math.min(lineEnd, text.length()));
            String trimmed = line.trim();
            if ("---".equals(trimmed)) {
                return lineStart;
            }
            if (!trimmed.isEmpty() && countLeadingIndent(text, lineStart) <= keyIndent) {
                return lineStart;
            }
            if (lineEnd <= lineStart) {
                break;
            }
            lineStart = lineEnd;
        }
        return text.length();
    }

    private static int countLeadingIndent(String text, int lineStart) {
        int count = 0;
        int index = Math.max(0, Math.min(lineStart, text.length()));
        while (index < text.length()) {
            char c = text.charAt(index);
            if (c == ' ') {
                count++;
            } else if (c == '\t') {
                count += 4;
            } else {
                break;
            }
            index++;
        }
        return count;
    }

    private static int findLineStartWithPrefixInRange(String text, String prefix, int start, int end) {
        if (text == null || prefix == null || prefix.isEmpty()) {
            return -1;
        }
        int lineStart = clamp(start, 0, text.length());
        int rangeEnd = clamp(end, lineStart, text.length());
        while (lineStart < rangeEnd) {
            int lineEnd = findLineEnd(text, lineStart);
            String line = text.substring(lineStart, Math.min(lineEnd, rangeEnd))
                .trim();
            if (line.startsWith(prefix)) {
                return lineStart;
            }
            if (lineEnd <= lineStart) {
                break;
            }
            lineStart = lineEnd;
        }
        return -1;
    }

    private static boolean containsYamlListValue(String frontmatter, String value) {
        String marker = "- " + value;
        return frontmatter.contains(marker + "\n") || frontmatter.endsWith(marker);
    }

    private static String insertMissingFrontmatterMetadata(String frontmatter, String metadata) {
        String updated = frontmatter;
        String[] lines = metadata.split("\n");
        for (String line : lines) {
            int separator = line.indexOf(':');
            if (separator <= 0) {
                continue;
            }
            String key = line.substring(0, separator + 1);
            if (findLineStartWithPrefix(updated, key) >= 0) {
                continue;
            }
            updated = insertLineBeforeFrontmatterClose(updated, line + "\n");
        }
        return updated;
    }

    private static String insertLineBeforeFrontmatterClose(String frontmatter, String insertion) {
        int closingStart = frontmatter.lastIndexOf("\n---");
        if (closingStart < 0) {
            closingStart = Math.max(0, frontmatter.length() - 4);
        }
        return frontmatter.substring(0, closingStart + 1) + insertion + frontmatter.substring(closingStart + 1);
    }

    private static int findLineStartWithPrefix(String text, String prefix) {
        if (text == null || prefix == null || prefix.isEmpty()) {
            return -1;
        }
        int lineStart = 0;
        while (lineStart < text.length()) {
            int lineEnd = findLineEnd(text, lineStart);
            String line = text.substring(lineStart, Math.min(lineEnd, text.length()))
                .trim();
            if (line.startsWith(prefix)) {
                return lineStart;
            }
            if (lineEnd <= lineStart) {
                break;
            }
            lineStart = lineEnd;
        }
        return -1;
    }

    private static Range resolveQuestIdsRemovalRange(String source, int start, int end, String selected) {
        return resolveWholeLineRemovalRange(source, start, end, selected);
    }

    private static Range resolveWholeLineRemovalRange(String source, int start, int end, String selected) {
        Range range = resolveTargetRange(source, start, end);
        int lineStart = findLineStart(source, start);
        int lineEnd = findLineEnd(source, start);
        String line = source.substring(lineStart, lineEnd)
            .trim();
        if (!selected.isEmpty() && line.equals(selected)) {
            return new Range(lineStart, lineEnd);
        }
        return range;
    }

    private static String removeRange(String text, Range removal, int offset) {
        if (text.isEmpty()) {
            return text;
        }
        int start = Math.max(0, removal.start - offset);
        int end = Math.max(start, removal.end - offset);
        start = clamp(start, 0, text.length());
        end = clamp(end, 0, text.length());
        if (start >= end) {
            return text;
        }
        return text.substring(0, start) + text.substring(end);
    }

    private static Result applyFunction(String source, int start, int end) {
        String selected = start < end ? sanitizeAttributeValue(source.substring(start, end)) : "";
        String expression = selected.isEmpty() ? "x^2" : selected;
        String replacement = "<Function expr=\"" + expression
            + "\" xRange=\"-4..4\" yRange=\"-2..8\" color=\"#3399ff\" />";
        int caretStart = start + replacement.indexOf(expression);
        return new Result(
            source.substring(0, start) + replacement + source.substring(end),
            caretStart,
            caretStart + expression.length());
    }

    private static Result applyChartSeries(String source, int start, int end) {
        String selected = start < end ? sanitizeAttributeValue(source.substring(start, end)) : "";
        String data = selected.isEmpty() ? "10,20,30" : selected;
        String replacement = "<Series name=\"Series\" data=\"" + data + "\" color=\"#4E79A7\" />";
        int caretStart = start + replacement.indexOf(data);
        return new Result(
            source.substring(0, start) + replacement + source.substring(end),
            caretStart,
            caretStart + data.length());
    }

    private static Result applyChartLineSeries(String source, int start, int end) {
        String selected = start < end ? sanitizeAttributeValue(source.substring(start, end)) : "";
        String data = selected.isEmpty() ? "12,18,25" : selected;
        String replacement = "<LineSeries name=\"Trend\" data=\"" + data + "\" color=\"#59A14F\" />";
        int caretStart = start + replacement.indexOf(data);
        return new Result(
            source.substring(0, start) + replacement + source.substring(end),
            caretStart,
            caretStart + data.length());
    }

    private static Result applyChartSlice(String source, int start, int end) {
        String selected = start < end ? sanitizeAttributeValue(source.substring(start, end)) : "";
        String label = selected.isEmpty() ? "Slice" : selected;
        String replacement = "<Slice label=\"" + label + "\" value=\"40\" color=\"#4E79A7\" />";
        int caretStart = start + replacement.indexOf(label);
        return new Result(
            source.substring(0, start) + replacement + source.substring(end),
            caretStart,
            caretStart + label.length());
    }

    private static Result applyFunctionPlot(String source, int start, int end) {
        String selected = start < end ? sanitizeAttributeValue(source.substring(start, end)) : "";
        String expression = selected.isEmpty() ? "sin(x)" : selected;
        String replacement = "<Plot expr=\"" + expression + "\" color=\"#ff5566\" label=\"plot\" />";
        int caretStart = start + replacement.indexOf(expression);
        return new Result(
            source.substring(0, start) + replacement + source.substring(end),
            caretStart,
            caretStart + expression.length());
    }

    private static Result applyFunctionPoint(String source, int start, int end) {
        String selected = start < end ? sanitizeAttributeValue(source.substring(start, end)) : "";
        String label = selected.isEmpty() ? "Point" : selected;
        String replacement = "<Point x=\"0\" y=\"0\" label=\"" + label + "\" />";
        int caretStart = start + replacement.indexOf(label);
        return new Result(
            source.substring(0, start) + replacement + source.substring(end),
            caretStart,
            caretStart + label.length());
    }

    private static Result applyFunctionGraphFence(String source, int start, int end) {
        Range range = resolveTargetRange(source, start, end);
        String body = source.substring(range.start, range.end)
            .trim();
        if (body.isEmpty()) {
            body = "title=\"Function\" width=360 height=220 xRange=-6..6 yRange=-3..3\n"
                + "sin(x) | color=#ff5566 label=\"sin x\"\n"
                + ":0,0 label=\"Origin\"";
        }
        String replacement = "```funcgraph\n" + body + "\n```";
        int caretStart = range.start + replacement.indexOf(body);
        return new Result(
            source.substring(0, range.start) + replacement + source.substring(range.end),
            caretStart,
            caretStart + body.length());
    }

    private static Result applyFloatingImage(String source, int start, int end) {
        String selected = start < end ? sanitizeAttributeValue(source.substring(start, end)) : "";
        String imagePath = selected.isEmpty() ? "test1.png" : selected;
        String replacement = "<FloatingImage src=\"" + imagePath
            + "\" align=\"left\" width=\"128\" title=\"Example\" />";
        int caretStart = start + replacement.indexOf(imagePath);
        return new Result(
            source.substring(0, start) + replacement + source.substring(end),
            caretStart,
            caretStart + imagePath.length());
    }

    private static Result applyNamedBlock(String source, int start, int end, String open, String close,
        String emptyBody) {
        Range range = resolveTargetRange(source, start, end);
        String body = source.substring(range.start, range.end)
            .trim();
        if (body.isEmpty()) {
            body = emptyBody;
        }
        String replacement = open + "\n" + body + "\n" + close;
        int caretStart = range.start + replacement.indexOf(body);
        return new Result(
            source.substring(0, range.start) + replacement + source.substring(range.end),
            caretStart,
            caretStart + body.length());
    }

    private static Result applyColor(String source, int start, int end) {
        String selected = start < end ? source.substring(start, end) : "";
        String body = selected.isEmpty() ? "text" : selected;
        String replacement = "<Color color=\"#FF0000\">" + body + "</Color>";
        int selectionStart = start + replacement.indexOf(body);
        return new Result(
            source.substring(0, start) + replacement + source.substring(end),
            selectionStart,
            selectionStart + body.length());
    }

    private static Result applyReference(String source, int start, int end, boolean image) {
        String id = nextReferenceId(source, image ? "img" : "ref");
        String selected = start < end ? sanitizeInline(source.substring(start, end)) : "";
        String label = selected.isEmpty() ? (image ? "Image" : "Link") : selected;
        String target = image ? "./image.png" : "./index.md";
        String marker = image ? "![" + label + "][" + id + "]" : "[" + label + "][" + id + "]";
        String replacement = marker + "\n\n[" + id + "]: " + target;
        int selectionStart = start + replacement.indexOf(target);
        return new Result(
            source.substring(0, start) + replacement + source.substring(end),
            selectionStart,
            selectionStart + target.length());
    }

    private static Result applyFootnote(String source, int start, int end) {
        String id = nextFootnoteId(source);
        String selected = start < end ? source.substring(start, end) : "";
        String definition = "\n\n[^" + id + "]: tooltip text";
        String replacement;
        int caretStart;
        int caretEnd;
        if (selected.isEmpty()) {
            replacement = "[^" + id + "]" + definition;
        } else {
            replacement = selected + "[^" + id + "]" + definition;
        }
        caretStart = start + replacement.indexOf("tooltip text");
        caretEnd = caretStart + "tooltip text".length();
        return new Result(source.substring(0, start) + replacement + source.substring(end), caretStart, caretEnd);
    }

    private static Result applyTooltip(String source, int start, int end) {
        Range range = resolveTargetRange(source, start, end);
        String body = source.substring(range.start, range.end)
            .trim();
        String replacement;
        int caretStart;
        int caretEnd;
        if (body.isEmpty()) {
            replacement = "<Tooltip label=\"Hover\">\n  Tooltip content\n</Tooltip>";
        } else {
            replacement = "<Tooltip label=\"Hover\">\n  " + body.replace("\n", "\n  ") + "\n</Tooltip>";
        }
        caretStart = range.start + replacement.indexOf("Hover");
        caretEnd = caretStart + "Hover".length();
        return new Result(
            source.substring(0, range.start) + replacement + source.substring(range.end),
            caretStart,
            caretEnd);
    }

    private static Result applyItemTag(String source, int start, int end, String tagName) {
        String selected = start < end ? sanitizeAttributeValue(source.substring(start, end)) : "";
        String itemId = selected.isEmpty() ? "minecraft:stone" : selected;
        String replacement = "<" + tagName + " id=\"" + itemId + "\" />";
        int caretStart = start + replacement.indexOf(itemId);
        return new Result(
            source.substring(0, start) + replacement + source.substring(end),
            caretStart,
            caretStart + itemId.length());
    }

    private static Result applyAttributeTag(String source, int start, int end, String tagName, String attribute,
        String defaultValue, boolean selfClosing) {
        return applyNamedTag(source, start, end, tagName, attribute, defaultValue, "", selfClosing);
    }

    private static Result applyNamedTag(String source, int start, int end, String tagName, String attribute,
        String defaultValue, String extraAttributes, boolean selfClosing) {
        String selected = start < end ? sanitizeAttributeValue(source.substring(start, end)) : "";
        String value = selected.isEmpty() ? defaultValue : selected;
        String close = selfClosing ? " />" : "></" + tagName + ">";
        String replacement = "<" + tagName + " " + attribute + "=\"" + value + "\"" + extraAttributes + close;
        int caretStart = start + replacement.indexOf(value);
        return new Result(
            source.substring(0, start) + replacement + source.substring(end),
            caretStart,
            caretStart + value.length());
    }

    private static Result applyCsvTable(String source, int start, int end) {
        Range range = resolveTargetRange(source, start, end);
        String body = source.substring(range.start, range.end)
            .trim();
        if (body.isEmpty()) {
            body = "name,value\niron,42\ngold,17";
        }
        String replacement = "```csv\n" + body + "\n```";
        int caretStart = range.start + replacement.indexOf(body);
        return new Result(
            source.substring(0, range.start) + replacement + source.substring(range.end),
            caretStart,
            caretStart + body.length());
    }

    private static Result applyCommandLink(String source, int start, int end) {
        String selected = start < end ? source.substring(start, end)
            .trim() : "";
        String label = selected.isEmpty() ? "Run command" : selected;
        String replacement = "<CommandLink command=\"/say hello\" title=\"Run command\" close={true}>" + label
            + "</CommandLink>";
        int caretStart = start + replacement.indexOf("/say hello");
        return new Result(
            source.substring(0, start) + replacement + source.substring(end),
            caretStart,
            caretStart + "/say hello".length());
    }

    private static Result applyOrderedList(String source, int start, int end) {
        return applyLinePrefix(source, start, end, "1. ", 0, 0);
    }

    private static Result applyTableTemplate(String source, int start, int end) {
        Range range = resolveTargetRange(source, start, end);
        String body = sanitizeInline(source.substring(range.start, range.end));
        if (body.isEmpty()) {
            String template = "\n| Header | Value |\n| --- | --- |\n|  |  |\n";
            int caret = range.start + template.indexOf("Header");
            return new Result(
                source.substring(0, range.start) + template + source.substring(range.end),
                caret,
                caret + 6);
        }

        String replacement = "\n| Header | Value |\n| --- | --- |\n| " + body + " |  |\n";
        int caret = range.start + replacement.indexOf(body);
        return new Result(
            source.substring(0, range.start) + replacement + source.substring(range.end),
            caret,
            caret + body.length());
    }

    private static Result applyAlertBlock(String source, int start, int end, String type) {
        Range range = resolveTargetRange(source, start, end);
        String body = source.substring(range.start, range.end)
            .trim();
        String replacement = "> [!" + type + "]\n> ";
        int caret;
        if (body.isEmpty()) {
            replacement += "\n";
            caret = range.start + replacement.length() - 1;
        } else {
            replacement += body.replace("\n", "\n> ") + "\n";
            caret = range.start + replacement.length();
        }
        return new Result(source.substring(0, range.start) + replacement + source.substring(range.end), caret, caret);
    }

    private static Result applyDetailsBlock(String source, int start, int end) {
        Range range = resolveTargetRange(source, start, end);
        String body = source.substring(range.start, range.end)
            .trim();
        StringBuilder replacement = new StringBuilder();
        replacement.append("<details>\n");
        replacement.append("<summary>Details</summary>\n\n");
        int caret;
        if (!body.isEmpty()) {
            replacement.append(body);
            if (!body.endsWith("\n")) {
                replacement.append('\n');
            }
        }
        caret = range.start + replacement.length();
        replacement.append("</details>");
        return new Result(source.substring(0, range.start) + replacement + source.substring(range.end), caret, caret);
    }

    private static Result wrapBlock(String source, int start, int end, String open, String close) {
        String selection = start < end ? source.substring(start, end) : "";
        String replacement = open + "\n" + selection + (selection.endsWith("\n") ? "" : "\n") + close;
        int caret = start + open.length() + 1;
        return new Result(source.substring(0, start) + replacement + source.substring(end), caret, caret);
    }

    private static Result wrap(String source, int start, int end, String before, String after, String emptyBefore,
        String emptyAfter) {
        if (start == end) {
            String insertion = before + emptyBefore + after + emptyAfter;
            int caret = start + before.length() + emptyBefore.length();
            return new Result(source.substring(0, start) + insertion + source.substring(end), caret, caret);
        }
        String selected = source.substring(start, end);
        String replacement = before + selected + after;
        int caretStart = start + before.length();
        int caretEnd = caretStart + selected.length();
        return new Result(source.substring(0, start) + replacement + source.substring(end), caretStart, caretEnd);
    }

    private static Result insertAt(String source, int start, int end, String insertion, int caretStartOffset,
        int caretEndOffset) {
        String replacement = insertion;
        int caret = start + caretStartOffset;
        return new Result(
            source.substring(0, start) + replacement + source.substring(end),
            caret,
            caret + caretEndOffset);
    }

    private static Result applyLinePrefix(String source, int start, int end, String prefix, int caretStartOffset,
        int caretEndOffset) {
        int lineStart = findLineStart(source, start);
        int lineEnd = findLineEnd(source, end);
        String target = source.substring(lineStart, lineEnd);
        String[] lines = target.split("\n", -1);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                builder.append('\n');
            }
            String line = lines[i];
            if (!line.startsWith(prefix)) {
                builder.append(prefix);
            }
            builder.append(line);
        }
        String replacement = builder.toString();
        int caret = lineStart + prefix.length() + caretStartOffset;
        return new Result(
            source.substring(0, lineStart) + replacement + source.substring(lineEnd),
            caret,
            caret + caretEndOffset);
    }

    private static int findLineStart(String text, int index) {
        int pos = Math.max(0, Math.min(index, text.length()));
        while (pos > 0 && text.charAt(pos - 1) != '\n') {
            pos--;
        }
        return pos;
    }

    private static int findLineEnd(String text, int index) {
        int pos = Math.max(0, Math.min(index, text.length()));
        while (pos < text.length() && text.charAt(pos) != '\n') {
            pos++;
        }
        if (pos < text.length()) {
            pos++;
        }
        return pos;
    }

    private static Range resolveTargetRange(String source, int start, int end) {
        if (start < end) {
            return new Range(start, end);
        }
        int lineStart = findLineStart(source, start);
        int lineEnd = findLineEnd(source, start);
        return new Range(lineStart, lineEnd);
    }

    private static String sanitizeInline(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.replace('\n', ' ')
            .trim();
    }

    private static String sanitizeAttributeValue(String text) {
        return sanitizeInline(text).replace("\\", "\\\\")
            .replace("\"", "\\\"");
    }

    private static String sanitizeIntegerValue(String text, String defaultValue) {
        String selected = sanitizeInline(text);
        if (selected.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.toString(Integer.parseInt(selected));
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private static String nextFootnoteId(String source) {
        String base = "note";
        if (!containsFootnoteId(source, base)) {
            return base;
        }
        int index = 2;
        while (containsFootnoteId(source, base + index)) {
            index++;
        }
        return base + index;
    }

    private static boolean containsFootnoteId(String source, String id) {
        String marker = "[^" + id + "]";
        return source != null && source.contains(marker);
    }

    private static String nextReferenceId(String source, String base) {
        if (!containsReferenceId(source, base)) {
            return base;
        }
        int index = 2;
        while (containsReferenceId(source, base + index)) {
            index++;
        }
        return base + index;
    }

    private static boolean containsReferenceId(String source, String id) {
        String marker = "[" + id + "]:";
        return source != null && source.contains(marker);
    }

    private static boolean isUuidLike(String text) {
        if (text == null || text.length() != 36) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (i == 8 || i == 13 || i == 18 || i == 23) {
                if (c != '-') {
                    return false;
                }
                continue;
            }
            boolean digit = c >= '0' && c <= '9';
            boolean lowerHex = c >= 'a' && c <= 'f';
            boolean upperHex = c >= 'A' && c <= 'F';
            if (!digit && !lowerHex && !upperHex) {
                return false;
            }
        }
        return true;
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }

    private static class Range {

        private final int start;
        private final int end;

        private Range(int start, int end) {
            this.start = Math.max(0, start);
            this.end = Math.max(this.start, end);
        }
    }
}
