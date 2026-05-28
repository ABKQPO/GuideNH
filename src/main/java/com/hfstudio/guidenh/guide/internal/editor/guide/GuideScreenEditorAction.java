package com.hfstudio.guidenh.guide.internal.editor.guide;

import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.internal.screen.GuideIconButton;

public enum GuideScreenEditorAction {

    HEADING_1(GuidebookText.GuideEditorHeading1),
    HEADING_2(GuidebookText.GuideEditorHeading2),
    HEADING_3(GuidebookText.GuideEditorHeading3),
    HEADING_4(GuidebookText.GuideEditorHeading4),
    HEADING_5(GuidebookText.GuideEditorHeading5),
    HEADING_6(GuidebookText.GuideEditorHeading6),
    BOLD(GuidebookText.GuideEditorBold),
    ITALIC(GuidebookText.GuideEditorItalic),
    STRIKETHROUGH(GuidebookText.GuideEditorStrikethrough),
    UNDERLINE(GuidebookText.GuideEditorUnderline),
    KBD(GuidebookText.GuideEditorKeyboard),
    SUBSCRIPT(GuidebookText.GuideEditorSubscript),
    SUPERSCRIPT(GuidebookText.GuideEditorSuperscript),
    FOOTNOTE(GuidebookText.GuideEditorFootnote),
    TOOLTIP(GuidebookText.GuideEditorTooltip),
    ITEM_IMAGE(GuidebookText.GuideEditorItemImage),
    BLOCK_IMAGE(GuidebookText.GuideEditorBlockImage),
    ITEM_LINK(GuidebookText.GuideEditorItemLink),
    LATEX(GuidebookText.GuideEditorLatex),
    CSV_TABLE(GuidebookText.GuideEditorCsvTable),
    COMMAND_LINK(GuidebookText.GuideEditorCommandLink),
    RECIPE(GuidebookText.GuideEditorRecipe),
    RECIPE_FOR(GuidebookText.GuideEditorRecipeFor),
    RECIPES_FOR(GuidebookText.GuideEditorRecipesFor),
    FLOATING_IMAGE(GuidebookText.GuideEditorFloatingImage),
    MERMAID(GuidebookText.GuideEditorMermaid),
    FILE_TREE(GuidebookText.GuideEditorFileTree),
    SUB_PAGES(GuidebookText.GuideEditorSubPages),
    CATEGORY(GuidebookText.GuideEditorCategory),
    FOOTNOTE_LIST(GuidebookText.GuideEditorFootnoteList),
    ROW(GuidebookText.GuideEditorRow),
    COLUMN(GuidebookText.GuideEditorColumn),
    DIV(GuidebookText.GuideEditorDiv),
    ITEM_GRID(GuidebookText.GuideEditorItemGrid),
    CSV_TABLE_IMPORT(GuidebookText.GuideEditorCsvTableImport),
    ANCHOR(GuidebookText.GuideEditorAnchor),
    COLUMN_CHART(GuidebookText.GuideEditorColumnChart),
    BAR_CHART(GuidebookText.GuideEditorBarChart),
    LINE_CHART(GuidebookText.GuideEditorLineChart),
    PIE_CHART(GuidebookText.GuideEditorPieChart),
    SCATTER_CHART(GuidebookText.GuideEditorScatterChart),
    CHART_SERIES(GuidebookText.GuideEditorChartSeries),
    CHART_LINE_SERIES(GuidebookText.GuideEditorChartLineSeries),
    CHART_SLICE(GuidebookText.GuideEditorChartSlice),
    CHART_PIE_INSET(GuidebookText.GuideEditorChartPieInset),
    FUNCTION_GRAPH(GuidebookText.GuideEditorFunctionGraph),
    FUNCTION(GuidebookText.GuideEditorFunction),
    FUNCTION_PLOT(GuidebookText.GuideEditorFunctionPlot),
    FUNCTION_POINT(GuidebookText.GuideEditorFunctionPoint),
    FUNCTION_GRAPH_FENCE(GuidebookText.GuideEditorFunctionGraphFence),
    STRUCTURE(GuidebookText.GuideEditorStructure),
    GAME_SCENE(GuidebookText.GuideEditorGameScene),
    SCENE_BLOCK(GuidebookText.GuideEditorSceneBlock),
    SCENE_ENTITY(GuidebookText.GuideEditorSceneEntity),
    ISOMETRIC_CAMERA(GuidebookText.GuideEditorIsometricCamera),
    BOX_ANNOTATION(GuidebookText.GuideEditorBoxAnnotation),
    BLOCK_ANNOTATION(GuidebookText.GuideEditorBlockAnnotation),
    LINE_ANNOTATION(GuidebookText.GuideEditorLineAnnotation),
    DIAMOND_ANNOTATION(GuidebookText.GuideEditorDiamondAnnotation),
    TEXT_ANNOTATION(GuidebookText.GuideEditorTextAnnotation),
    BLOCK_ANNOTATION_TEMPLATE(GuidebookText.GuideEditorBlockAnnotationTemplate),
    IMPORT_STRUCTURE(GuidebookText.GuideEditorImportStructure),
    IMPORT_STRUCTURE_LIB(GuidebookText.GuideEditorImportStructureLib),
    IMPORT_PONDER(GuidebookText.GuideEditorImportPonder),
    PLACE_BLOCK(GuidebookText.GuideEditorPlaceBlock),
    REPLACE_BLOCK(GuidebookText.GuideEditorReplaceBlock),
    REMOVE_BLOCKS(GuidebookText.GuideEditorRemoveBlocks),
    QUEST_LINK(GuidebookText.GuideEditorQuestLink),
    QUEST_CARD(GuidebookText.GuideEditorQuestCard),
    QUEST_IDS(GuidebookText.GuideEditorQuestIds),
    NAV_POSITION(GuidebookText.GuideEditorNavPosition),
    NAV_ICON(GuidebookText.GuideEditorNavIcon),
    NAV_ICON_TEXTURE(GuidebookText.GuideEditorNavIconTexture),
    NAV_ICONS(GuidebookText.GuideEditorNavIcons),
    NAV_ICON_TEXTURES(GuidebookText.GuideEditorNavIconTextures),
    NAV_REQUIRED_MODS(GuidebookText.GuideEditorNavRequiredMods),
    PAGE_CATEGORIES(GuidebookText.GuideEditorPageCategories),
    PAGE_ITEM_IDS(GuidebookText.GuideEditorPageItemIds),
    PAGE_ORE_IDS(GuidebookText.GuideEditorPageOreIds),
    PAGE_METADATA(GuidebookText.GuideEditorPageMetadata),
    QUOTE_CALLOUT(GuidebookText.GuideEditorQuoteCallout),
    QUOTE_ICON_TEXT(GuidebookText.GuideEditorQuoteIconText),
    QUOTE_ICON_ITEM(GuidebookText.GuideEditorQuoteIconItem),
    QUOTE_ICON_PNG(GuidebookText.GuideEditorQuoteIconPng),
    LATEX_SHORTHAND(GuidebookText.GuideEditorLatexShorthand),
    LINK(GuidebookText.GuideEditorLink),
    IMAGE(GuidebookText.GuideEditorImage),
    INLINE_CODE(GuidebookText.GuideEditorInlineCode),
    CODE_BLOCK(GuidebookText.GuideEditorCodeBlock),
    BLOCKQUOTE(GuidebookText.GuideEditorQuote),
    UNORDERED_LIST(GuidebookText.GuideEditorBulletList),
    ORDERED_LIST(GuidebookText.GuideEditorNumberedList),
    TASK_LIST(GuidebookText.GuideEditorTaskList),
    TABLE(GuidebookText.GuideEditorTable),
    ALERT_NOTE(GuidebookText.GuideEditorAlertNote),
    ALERT_TIP(GuidebookText.GuideEditorAlertTip),
    ALERT_IMPORTANT(GuidebookText.GuideEditorAlertImportant),
    ALERT_WARNING(GuidebookText.GuideEditorAlertWarning),
    ALERT_CAUTION(GuidebookText.GuideEditorAlertCaution),
    DETAILS(GuidebookText.GuideEditorDetails),
    KEY_BIND(GuidebookText.GuideEditorKeyBind),
    PLAYER_NAME(GuidebookText.GuideEditorPlayerName),
    COLOR(GuidebookText.GuideEditorColor),
    BREAK(GuidebookText.GuideEditorBreak),
    REFERENCE_LINK(GuidebookText.GuideEditorReferenceLink),
    REFERENCE_IMAGE(GuidebookText.GuideEditorReferenceImage),
    THEMATIC_BREAK(GuidebookText.GuideEditorRule),
    UNDO(GuidebookText.GuideEditorUndo),
    REDO(GuidebookText.GuideEditorRedo),
    CUT(GuidebookText.GuideEditorCut),
    COPY(GuidebookText.GuideEditorCopy),
    PASTE(GuidebookText.GuideEditorPaste),
    SELECT_ALL(GuidebookText.GuideEditorSelectAll),
    FORMAT_DOCUMENT(GuidebookText.GuideEditorFormatDocument),
    TOGGLE_ADVANCED(GuidebookText.GuideEditorAdvancedToggle);

    private final GuidebookText tooltipKey;

    GuideScreenEditorAction(GuidebookText tooltipKey) {
        this.tooltipKey = tooltipKey;
    }

    public String getTooltip() {
        return tooltipKey.text();
    }

    public GuideIconButton.Role toRole() {
        return switch (this) {
            case HEADING_1 -> GuideIconButton.Role.GUIDE_EDITOR_HEADING_1;
            case HEADING_2 -> GuideIconButton.Role.GUIDE_EDITOR_HEADING_2;
            case HEADING_3 -> GuideIconButton.Role.GUIDE_EDITOR_HEADING_3;
            case HEADING_4 -> GuideIconButton.Role.GUIDE_EDITOR_HEADING_4;
            case HEADING_5 -> GuideIconButton.Role.GUIDE_EDITOR_HEADING_5;
            case HEADING_6 -> GuideIconButton.Role.GUIDE_EDITOR_HEADING_6;
            case BOLD -> GuideIconButton.Role.GUIDE_EDITOR_BOLD;
            case ITALIC -> GuideIconButton.Role.GUIDE_EDITOR_ITALIC;
            case STRIKETHROUGH -> GuideIconButton.Role.GUIDE_EDITOR_STRIKETHROUGH;
            case UNDERLINE -> GuideIconButton.Role.GUIDE_EDITOR_UNDERLINE;
            case KBD -> GuideIconButton.Role.GUIDE_EDITOR_KEYBOARD;
            case SUBSCRIPT -> GuideIconButton.Role.GUIDE_EDITOR_SUBSCRIPT;
            case SUPERSCRIPT -> GuideIconButton.Role.GUIDE_EDITOR_SUPERSCRIPT;
            case FOOTNOTE -> GuideIconButton.Role.GUIDE_EDITOR_FOOTNOTE;
            case TOOLTIP -> GuideIconButton.Role.GUIDE_EDITOR_TOOLTIP;
            case ITEM_IMAGE -> GuideIconButton.Role.GUIDE_EDITOR_ITEM_IMAGE;
            case BLOCK_IMAGE -> GuideIconButton.Role.GUIDE_EDITOR_BLOCK_IMAGE;
            case ITEM_LINK -> GuideIconButton.Role.GUIDE_EDITOR_ITEM_LINK;
            case LATEX -> GuideIconButton.Role.GUIDE_EDITOR_LATEX;
            case CSV_TABLE -> GuideIconButton.Role.GUIDE_EDITOR_CSV_TABLE;
            case COMMAND_LINK -> GuideIconButton.Role.GUIDE_EDITOR_COMMAND_LINK;
            case RECIPE -> GuideIconButton.Role.GUIDE_EDITOR_RECIPE;
            case RECIPE_FOR -> GuideIconButton.Role.GUIDE_EDITOR_RECIPE_FOR;
            case RECIPES_FOR -> GuideIconButton.Role.GUIDE_EDITOR_RECIPES_FOR;
            case FLOATING_IMAGE -> GuideIconButton.Role.GUIDE_EDITOR_FLOATING_IMAGE;
            case MERMAID -> GuideIconButton.Role.GUIDE_EDITOR_MERMAID;
            case FILE_TREE -> GuideIconButton.Role.GUIDE_EDITOR_FILE_TREE;
            case SUB_PAGES -> GuideIconButton.Role.GUIDE_EDITOR_SUB_PAGES;
            case CATEGORY -> GuideIconButton.Role.GUIDE_EDITOR_CATEGORY;
            case FOOTNOTE_LIST -> GuideIconButton.Role.GUIDE_EDITOR_FOOTNOTE_LIST;
            case ROW -> GuideIconButton.Role.GUIDE_EDITOR_ROW;
            case COLUMN -> GuideIconButton.Role.GUIDE_EDITOR_COLUMN;
            case DIV -> GuideIconButton.Role.GUIDE_EDITOR_DIV;
            case ITEM_GRID -> GuideIconButton.Role.GUIDE_EDITOR_ITEM_GRID;
            case CSV_TABLE_IMPORT -> GuideIconButton.Role.GUIDE_EDITOR_CSV_TABLE_IMPORT;
            case ANCHOR -> GuideIconButton.Role.GUIDE_EDITOR_ANCHOR;
            case COLUMN_CHART -> GuideIconButton.Role.GUIDE_EDITOR_COLUMN_CHART;
            case BAR_CHART -> GuideIconButton.Role.GUIDE_EDITOR_BAR_CHART;
            case LINE_CHART -> GuideIconButton.Role.GUIDE_EDITOR_LINE_CHART;
            case PIE_CHART -> GuideIconButton.Role.GUIDE_EDITOR_PIE_CHART;
            case SCATTER_CHART -> GuideIconButton.Role.GUIDE_EDITOR_SCATTER_CHART;
            case CHART_SERIES -> GuideIconButton.Role.GUIDE_EDITOR_CHART_SERIES;
            case CHART_LINE_SERIES -> GuideIconButton.Role.GUIDE_EDITOR_CHART_LINE_SERIES;
            case CHART_SLICE -> GuideIconButton.Role.GUIDE_EDITOR_CHART_SLICE;
            case CHART_PIE_INSET -> GuideIconButton.Role.GUIDE_EDITOR_CHART_PIE_INSET;
            case FUNCTION_GRAPH -> GuideIconButton.Role.GUIDE_EDITOR_FUNCTION_GRAPH;
            case FUNCTION -> GuideIconButton.Role.GUIDE_EDITOR_FUNCTION;
            case FUNCTION_PLOT -> GuideIconButton.Role.GUIDE_EDITOR_FUNCTION_PLOT;
            case FUNCTION_POINT -> GuideIconButton.Role.GUIDE_EDITOR_FUNCTION_POINT;
            case FUNCTION_GRAPH_FENCE -> GuideIconButton.Role.GUIDE_EDITOR_FUNCTION_GRAPH_FENCE;
            case STRUCTURE -> GuideIconButton.Role.GUIDE_EDITOR_STRUCTURE;
            case GAME_SCENE -> GuideIconButton.Role.GUIDE_EDITOR_GAME_SCENE;
            case SCENE_BLOCK -> GuideIconButton.Role.GUIDE_EDITOR_SCENE_BLOCK;
            case SCENE_ENTITY -> GuideIconButton.Role.GUIDE_EDITOR_SCENE_ENTITY;
            case ISOMETRIC_CAMERA -> GuideIconButton.Role.GUIDE_EDITOR_ISOMETRIC_CAMERA;
            case BOX_ANNOTATION -> GuideIconButton.Role.GUIDE_EDITOR_BOX_ANNOTATION;
            case BLOCK_ANNOTATION -> GuideIconButton.Role.GUIDE_EDITOR_BLOCK_ANNOTATION;
            case LINE_ANNOTATION -> GuideIconButton.Role.GUIDE_EDITOR_LINE_ANNOTATION;
            case DIAMOND_ANNOTATION -> GuideIconButton.Role.GUIDE_EDITOR_DIAMOND_ANNOTATION;
            case TEXT_ANNOTATION -> GuideIconButton.Role.GUIDE_EDITOR_TEXT_ANNOTATION;
            case BLOCK_ANNOTATION_TEMPLATE -> GuideIconButton.Role.GUIDE_EDITOR_BLOCK_ANNOTATION_TEMPLATE;
            case IMPORT_STRUCTURE -> GuideIconButton.Role.GUIDE_EDITOR_IMPORT_STRUCTURE;
            case IMPORT_STRUCTURE_LIB -> GuideIconButton.Role.GUIDE_EDITOR_IMPORT_STRUCTURE_LIB;
            case IMPORT_PONDER -> GuideIconButton.Role.GUIDE_EDITOR_IMPORT_PONDER;
            case PLACE_BLOCK -> GuideIconButton.Role.GUIDE_EDITOR_PLACE_BLOCK;
            case REPLACE_BLOCK -> GuideIconButton.Role.GUIDE_EDITOR_REPLACE_BLOCK;
            case REMOVE_BLOCKS -> GuideIconButton.Role.GUIDE_EDITOR_REMOVE_BLOCKS;
            case QUEST_LINK -> GuideIconButton.Role.GUIDE_EDITOR_QUEST_LINK;
            case QUEST_CARD -> GuideIconButton.Role.GUIDE_EDITOR_QUEST_CARD;
            case QUEST_IDS -> GuideIconButton.Role.GUIDE_EDITOR_QUEST_IDS;
            case NAV_POSITION -> GuideIconButton.Role.GUIDE_EDITOR_NAV_POSITION;
            case NAV_ICON -> GuideIconButton.Role.GUIDE_EDITOR_NAV_ICON;
            case NAV_ICON_TEXTURE -> GuideIconButton.Role.GUIDE_EDITOR_NAV_ICON_TEXTURE;
            case NAV_ICONS -> GuideIconButton.Role.GUIDE_EDITOR_NAV_ICONS;
            case NAV_ICON_TEXTURES -> GuideIconButton.Role.GUIDE_EDITOR_NAV_ICON_TEXTURES;
            case NAV_REQUIRED_MODS -> GuideIconButton.Role.GUIDE_EDITOR_NAV_REQUIRED_MODS;
            case PAGE_CATEGORIES -> GuideIconButton.Role.GUIDE_EDITOR_PAGE_CATEGORIES;
            case PAGE_ITEM_IDS -> GuideIconButton.Role.GUIDE_EDITOR_PAGE_ITEM_IDS;
            case PAGE_ORE_IDS -> GuideIconButton.Role.GUIDE_EDITOR_PAGE_ORE_IDS;
            case PAGE_METADATA -> GuideIconButton.Role.GUIDE_EDITOR_PAGE_METADATA;
            case QUOTE_CALLOUT -> GuideIconButton.Role.GUIDE_EDITOR_QUOTE_CALLOUT;
            case QUOTE_ICON_TEXT -> GuideIconButton.Role.GUIDE_EDITOR_QUOTE_ICON_TEXT;
            case QUOTE_ICON_ITEM -> GuideIconButton.Role.GUIDE_EDITOR_QUOTE_ICON_ITEM;
            case QUOTE_ICON_PNG -> GuideIconButton.Role.GUIDE_EDITOR_QUOTE_ICON_PNG;
            case LATEX_SHORTHAND -> GuideIconButton.Role.GUIDE_EDITOR_LATEX_SHORTHAND;
            case LINK -> GuideIconButton.Role.GUIDE_EDITOR_LINK;
            case IMAGE -> GuideIconButton.Role.GUIDE_EDITOR_IMAGE;
            case INLINE_CODE -> GuideIconButton.Role.GUIDE_EDITOR_INLINE_CODE;
            case CODE_BLOCK -> GuideIconButton.Role.GUIDE_EDITOR_CODE_BLOCK;
            case BLOCKQUOTE -> GuideIconButton.Role.GUIDE_EDITOR_QUOTE;
            case UNORDERED_LIST -> GuideIconButton.Role.GUIDE_EDITOR_BULLET_LIST;
            case ORDERED_LIST -> GuideIconButton.Role.GUIDE_EDITOR_NUMBERED_LIST;
            case TASK_LIST -> GuideIconButton.Role.GUIDE_EDITOR_TASK_LIST;
            case TABLE -> GuideIconButton.Role.GUIDE_EDITOR_TABLE;
            case ALERT_NOTE -> GuideIconButton.Role.GUIDE_EDITOR_ALERT_NOTE;
            case ALERT_TIP -> GuideIconButton.Role.GUIDE_EDITOR_ALERT_TIP;
            case ALERT_IMPORTANT -> GuideIconButton.Role.GUIDE_EDITOR_ALERT_IMPORTANT;
            case ALERT_WARNING -> GuideIconButton.Role.GUIDE_EDITOR_ALERT_WARNING;
            case ALERT_CAUTION -> GuideIconButton.Role.GUIDE_EDITOR_ALERT_CAUTION;
            case DETAILS -> GuideIconButton.Role.GUIDE_EDITOR_DETAILS;
            case KEY_BIND -> GuideIconButton.Role.GUIDE_EDITOR_KEY_BIND;
            case PLAYER_NAME -> GuideIconButton.Role.GUIDE_EDITOR_PLAYER_NAME;
            case COLOR -> GuideIconButton.Role.GUIDE_EDITOR_COLOR;
            case BREAK -> GuideIconButton.Role.GUIDE_EDITOR_BREAK;
            case REFERENCE_LINK -> GuideIconButton.Role.GUIDE_EDITOR_REFERENCE_LINK;
            case REFERENCE_IMAGE -> GuideIconButton.Role.GUIDE_EDITOR_REFERENCE_IMAGE;
            case THEMATIC_BREAK -> GuideIconButton.Role.GUIDE_EDITOR_RULE;
            case UNDO -> GuideIconButton.Role.GUIDE_EDITOR_UNDO;
            case REDO -> GuideIconButton.Role.GUIDE_EDITOR_REDO;
            case CUT -> GuideIconButton.Role.GUIDE_EDITOR_CUT;
            case COPY -> GuideIconButton.Role.GUIDE_EDITOR_COPY;
            case PASTE -> GuideIconButton.Role.GUIDE_EDITOR_PASTE;
            case SELECT_ALL -> GuideIconButton.Role.GUIDE_EDITOR_SELECT_ALL;
            default -> GuideIconButton.Role.GUIDE_EDITOR_ADVANCED_TOGGLE;
        };
    }
}
