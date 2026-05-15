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
    CATEGORY_INDEX(GuidebookText.GuideEditorCategoryIndex),
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
        switch (this) {
            case HEADING_1:
                return GuideIconButton.Role.GUIDE_EDITOR_HEADING_1;
            case HEADING_2:
                return GuideIconButton.Role.GUIDE_EDITOR_HEADING_2;
            case HEADING_3:
                return GuideIconButton.Role.GUIDE_EDITOR_HEADING_3;
            case HEADING_4:
                return GuideIconButton.Role.GUIDE_EDITOR_HEADING_4;
            case HEADING_5:
                return GuideIconButton.Role.GUIDE_EDITOR_HEADING_5;
            case HEADING_6:
                return GuideIconButton.Role.GUIDE_EDITOR_HEADING_6;
            case BOLD:
                return GuideIconButton.Role.GUIDE_EDITOR_BOLD;
            case ITALIC:
                return GuideIconButton.Role.GUIDE_EDITOR_ITALIC;
            case STRIKETHROUGH:
                return GuideIconButton.Role.GUIDE_EDITOR_STRIKETHROUGH;
            case UNDERLINE:
                return GuideIconButton.Role.GUIDE_EDITOR_UNDERLINE;
            case KBD:
                return GuideIconButton.Role.GUIDE_EDITOR_KEYBOARD;
            case SUBSCRIPT:
                return GuideIconButton.Role.GUIDE_EDITOR_SUBSCRIPT;
            case SUPERSCRIPT:
                return GuideIconButton.Role.GUIDE_EDITOR_SUPERSCRIPT;
            case FOOTNOTE:
                return GuideIconButton.Role.GUIDE_EDITOR_FOOTNOTE;
            case TOOLTIP:
                return GuideIconButton.Role.GUIDE_EDITOR_TOOLTIP;
            case ITEM_IMAGE:
                return GuideIconButton.Role.GUIDE_EDITOR_ITEM_IMAGE;
            case BLOCK_IMAGE:
                return GuideIconButton.Role.GUIDE_EDITOR_BLOCK_IMAGE;
            case ITEM_LINK:
                return GuideIconButton.Role.GUIDE_EDITOR_ITEM_LINK;
            case LATEX:
                return GuideIconButton.Role.GUIDE_EDITOR_LATEX;
            case CSV_TABLE:
                return GuideIconButton.Role.GUIDE_EDITOR_CSV_TABLE;
            case COMMAND_LINK:
                return GuideIconButton.Role.GUIDE_EDITOR_COMMAND_LINK;
            case RECIPE:
                return GuideIconButton.Role.GUIDE_EDITOR_RECIPE;
            case RECIPE_FOR:
                return GuideIconButton.Role.GUIDE_EDITOR_RECIPE_FOR;
            case RECIPES_FOR:
                return GuideIconButton.Role.GUIDE_EDITOR_RECIPES_FOR;
            case FLOATING_IMAGE:
                return GuideIconButton.Role.GUIDE_EDITOR_FLOATING_IMAGE;
            case MERMAID:
                return GuideIconButton.Role.GUIDE_EDITOR_MERMAID;
            case FILE_TREE:
                return GuideIconButton.Role.GUIDE_EDITOR_FILE_TREE;
            case SUB_PAGES:
                return GuideIconButton.Role.GUIDE_EDITOR_SUB_PAGES;
            case CATEGORY_INDEX:
                return GuideIconButton.Role.GUIDE_EDITOR_CATEGORY_INDEX;
            case FOOTNOTE_LIST:
                return GuideIconButton.Role.GUIDE_EDITOR_FOOTNOTE_LIST;
            case ROW:
                return GuideIconButton.Role.GUIDE_EDITOR_ROW;
            case COLUMN:
                return GuideIconButton.Role.GUIDE_EDITOR_COLUMN;
            case DIV:
                return GuideIconButton.Role.GUIDE_EDITOR_DIV;
            case ITEM_GRID:
                return GuideIconButton.Role.GUIDE_EDITOR_ITEM_GRID;
            case CSV_TABLE_IMPORT:
                return GuideIconButton.Role.GUIDE_EDITOR_CSV_TABLE_IMPORT;
            case ANCHOR:
                return GuideIconButton.Role.GUIDE_EDITOR_ANCHOR;
            case COLUMN_CHART:
                return GuideIconButton.Role.GUIDE_EDITOR_COLUMN_CHART;
            case BAR_CHART:
                return GuideIconButton.Role.GUIDE_EDITOR_BAR_CHART;
            case LINE_CHART:
                return GuideIconButton.Role.GUIDE_EDITOR_LINE_CHART;
            case PIE_CHART:
                return GuideIconButton.Role.GUIDE_EDITOR_PIE_CHART;
            case SCATTER_CHART:
                return GuideIconButton.Role.GUIDE_EDITOR_SCATTER_CHART;
            case CHART_SERIES:
                return GuideIconButton.Role.GUIDE_EDITOR_CHART_SERIES;
            case CHART_LINE_SERIES:
                return GuideIconButton.Role.GUIDE_EDITOR_CHART_LINE_SERIES;
            case CHART_SLICE:
                return GuideIconButton.Role.GUIDE_EDITOR_CHART_SLICE;
            case CHART_PIE_INSET:
                return GuideIconButton.Role.GUIDE_EDITOR_CHART_PIE_INSET;
            case FUNCTION_GRAPH:
                return GuideIconButton.Role.GUIDE_EDITOR_FUNCTION_GRAPH;
            case FUNCTION:
                return GuideIconButton.Role.GUIDE_EDITOR_FUNCTION;
            case FUNCTION_PLOT:
                return GuideIconButton.Role.GUIDE_EDITOR_FUNCTION_PLOT;
            case FUNCTION_POINT:
                return GuideIconButton.Role.GUIDE_EDITOR_FUNCTION_POINT;
            case FUNCTION_GRAPH_FENCE:
                return GuideIconButton.Role.GUIDE_EDITOR_FUNCTION_GRAPH_FENCE;
            case STRUCTURE:
                return GuideIconButton.Role.GUIDE_EDITOR_STRUCTURE;
            case GAME_SCENE:
                return GuideIconButton.Role.GUIDE_EDITOR_GAME_SCENE;
            case SCENE_BLOCK:
                return GuideIconButton.Role.GUIDE_EDITOR_SCENE_BLOCK;
            case SCENE_ENTITY:
                return GuideIconButton.Role.GUIDE_EDITOR_SCENE_ENTITY;
            case ISOMETRIC_CAMERA:
                return GuideIconButton.Role.GUIDE_EDITOR_ISOMETRIC_CAMERA;
            case BOX_ANNOTATION:
                return GuideIconButton.Role.GUIDE_EDITOR_BOX_ANNOTATION;
            case BLOCK_ANNOTATION:
                return GuideIconButton.Role.GUIDE_EDITOR_BLOCK_ANNOTATION;
            case LINE_ANNOTATION:
                return GuideIconButton.Role.GUIDE_EDITOR_LINE_ANNOTATION;
            case DIAMOND_ANNOTATION:
                return GuideIconButton.Role.GUIDE_EDITOR_DIAMOND_ANNOTATION;
            case TEXT_ANNOTATION:
                return GuideIconButton.Role.GUIDE_EDITOR_TEXT_ANNOTATION;
            case BLOCK_ANNOTATION_TEMPLATE:
                return GuideIconButton.Role.GUIDE_EDITOR_BLOCK_ANNOTATION_TEMPLATE;
            case IMPORT_STRUCTURE:
                return GuideIconButton.Role.GUIDE_EDITOR_IMPORT_STRUCTURE;
            case IMPORT_STRUCTURE_LIB:
                return GuideIconButton.Role.GUIDE_EDITOR_IMPORT_STRUCTURE_LIB;
            case IMPORT_PONDER:
                return GuideIconButton.Role.GUIDE_EDITOR_IMPORT_PONDER;
            case PLACE_BLOCK:
                return GuideIconButton.Role.GUIDE_EDITOR_PLACE_BLOCK;
            case REPLACE_BLOCK:
                return GuideIconButton.Role.GUIDE_EDITOR_REPLACE_BLOCK;
            case REMOVE_BLOCKS:
                return GuideIconButton.Role.GUIDE_EDITOR_REMOVE_BLOCKS;
            case QUEST_LINK:
                return GuideIconButton.Role.GUIDE_EDITOR_QUEST_LINK;
            case QUEST_CARD:
                return GuideIconButton.Role.GUIDE_EDITOR_QUEST_CARD;
            case QUEST_IDS:
                return GuideIconButton.Role.GUIDE_EDITOR_QUEST_IDS;
            case NAV_POSITION:
                return GuideIconButton.Role.GUIDE_EDITOR_NAV_POSITION;
            case NAV_ICON:
                return GuideIconButton.Role.GUIDE_EDITOR_NAV_ICON;
            case NAV_ICON_TEXTURE:
                return GuideIconButton.Role.GUIDE_EDITOR_NAV_ICON_TEXTURE;
            case NAV_ICONS:
                return GuideIconButton.Role.GUIDE_EDITOR_NAV_ICONS;
            case NAV_ICON_TEXTURES:
                return GuideIconButton.Role.GUIDE_EDITOR_NAV_ICON_TEXTURES;
            case NAV_REQUIRED_MODS:
                return GuideIconButton.Role.GUIDE_EDITOR_NAV_REQUIRED_MODS;
            case PAGE_CATEGORIES:
                return GuideIconButton.Role.GUIDE_EDITOR_PAGE_CATEGORIES;
            case PAGE_ITEM_IDS:
                return GuideIconButton.Role.GUIDE_EDITOR_PAGE_ITEM_IDS;
            case PAGE_ORE_IDS:
                return GuideIconButton.Role.GUIDE_EDITOR_PAGE_ORE_IDS;
            case PAGE_METADATA:
                return GuideIconButton.Role.GUIDE_EDITOR_PAGE_METADATA;
            case QUOTE_CALLOUT:
                return GuideIconButton.Role.GUIDE_EDITOR_QUOTE_CALLOUT;
            case QUOTE_ICON_TEXT:
                return GuideIconButton.Role.GUIDE_EDITOR_QUOTE_ICON_TEXT;
            case QUOTE_ICON_ITEM:
                return GuideIconButton.Role.GUIDE_EDITOR_QUOTE_ICON_ITEM;
            case QUOTE_ICON_PNG:
                return GuideIconButton.Role.GUIDE_EDITOR_QUOTE_ICON_PNG;
            case LATEX_SHORTHAND:
                return GuideIconButton.Role.GUIDE_EDITOR_LATEX_SHORTHAND;
            case LINK:
                return GuideIconButton.Role.GUIDE_EDITOR_LINK;
            case IMAGE:
                return GuideIconButton.Role.GUIDE_EDITOR_IMAGE;
            case INLINE_CODE:
                return GuideIconButton.Role.GUIDE_EDITOR_INLINE_CODE;
            case CODE_BLOCK:
                return GuideIconButton.Role.GUIDE_EDITOR_CODE_BLOCK;
            case BLOCKQUOTE:
                return GuideIconButton.Role.GUIDE_EDITOR_QUOTE;
            case UNORDERED_LIST:
                return GuideIconButton.Role.GUIDE_EDITOR_BULLET_LIST;
            case ORDERED_LIST:
                return GuideIconButton.Role.GUIDE_EDITOR_NUMBERED_LIST;
            case TASK_LIST:
                return GuideIconButton.Role.GUIDE_EDITOR_TASK_LIST;
            case TABLE:
                return GuideIconButton.Role.GUIDE_EDITOR_TABLE;
            case ALERT_NOTE:
                return GuideIconButton.Role.GUIDE_EDITOR_ALERT_NOTE;
            case ALERT_TIP:
                return GuideIconButton.Role.GUIDE_EDITOR_ALERT_TIP;
            case ALERT_IMPORTANT:
                return GuideIconButton.Role.GUIDE_EDITOR_ALERT_IMPORTANT;
            case ALERT_WARNING:
                return GuideIconButton.Role.GUIDE_EDITOR_ALERT_WARNING;
            case ALERT_CAUTION:
                return GuideIconButton.Role.GUIDE_EDITOR_ALERT_CAUTION;
            case DETAILS:
                return GuideIconButton.Role.GUIDE_EDITOR_DETAILS;
            case KEY_BIND:
                return GuideIconButton.Role.GUIDE_EDITOR_KEY_BIND;
            case PLAYER_NAME:
                return GuideIconButton.Role.GUIDE_EDITOR_PLAYER_NAME;
            case COLOR:
                return GuideIconButton.Role.GUIDE_EDITOR_COLOR;
            case BREAK:
                return GuideIconButton.Role.GUIDE_EDITOR_BREAK;
            case REFERENCE_LINK:
                return GuideIconButton.Role.GUIDE_EDITOR_REFERENCE_LINK;
            case REFERENCE_IMAGE:
                return GuideIconButton.Role.GUIDE_EDITOR_REFERENCE_IMAGE;
            case THEMATIC_BREAK:
                return GuideIconButton.Role.GUIDE_EDITOR_RULE;
            case UNDO:
                return GuideIconButton.Role.GUIDE_EDITOR_UNDO;
            case REDO:
                return GuideIconButton.Role.GUIDE_EDITOR_REDO;
            case CUT:
                return GuideIconButton.Role.GUIDE_EDITOR_CUT;
            case COPY:
                return GuideIconButton.Role.GUIDE_EDITOR_COPY;
            case PASTE:
                return GuideIconButton.Role.GUIDE_EDITOR_PASTE;
            case SELECT_ALL:
                return GuideIconButton.Role.GUIDE_EDITOR_SELECT_ALL;
            case TOGGLE_ADVANCED:
            default:
                return GuideIconButton.Role.GUIDE_EDITOR_ADVANCED_TOGGLE;
        }
    }
}
