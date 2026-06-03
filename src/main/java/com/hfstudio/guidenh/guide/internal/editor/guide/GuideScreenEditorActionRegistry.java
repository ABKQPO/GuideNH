package com.hfstudio.guidenh.guide.internal.editor.guide;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import com.hfstudio.guidenh.guide.internal.GuidebookText;

public class GuideScreenEditorActionRegistry {

    private static final List<GuideScreenEditorActionSpec> SPECS = createSpecs();
    private static final Map<GuideScreenEditorActionGroup, List<GuideScreenEditorAction>> ACTIONS_BY_GROUP = createActionsByGroup(
        SPECS);

    private GuideScreenEditorActionRegistry() {}

    public static List<GuideScreenEditorAction> actionsIn(GuideScreenEditorActionGroup group) {
        return ACTIONS_BY_GROUP.getOrDefault(group, List.of());
    }

    public static List<GuideScreenEditorAction> toolbarActions(boolean advanced) {
        ArrayList<GuideScreenEditorAction> actions = new ArrayList<>(
            actionsIn(GuideScreenEditorActionGroup.TOOLBAR_BASE));
        if (advanced) {
            actions.addAll(actionsIn(GuideScreenEditorActionGroup.TOOLBAR_ADVANCED));
        }
        return actions;
    }

    public static List<GuideScreenEditorContextMenu.Entry> contextMenuEntries() {
        List<GuideScreenEditorContextMenu.Entry> editEntries = actionEntries(GuideScreenEditorActionGroup.EDIT);
        List<GuideScreenEditorContextMenu.Entry> insertEntries = new ArrayList<>();
        append(insertEntries, GuideScreenEditorActionGroup.ROOT_INSERT);
        insertEntries.add(GuideScreenEditorContextMenu.Entry.separator());
        insertEntries.add(
            GuideScreenEditorContextMenu.Entry.submenu(
                GuidebookText.GuideEditorContextMenuInline.text(),
                actionEntries(GuideScreenEditorActionGroup.INLINE)));
        insertEntries.add(GuideScreenEditorContextMenu.Entry.separator());
        append(insertEntries, GuideScreenEditorActionGroup.LINK_AND_TEXT);
        insertEntries.add(GuideScreenEditorContextMenu.Entry.separator());
        append(insertEntries, GuideScreenEditorActionGroup.LIST_AND_TABLE);
        insertEntries.add(GuideScreenEditorContextMenu.Entry.separator());

        List<GuideScreenEditorContextMenu.Entry> blockEntries = new ArrayList<>();
        append(blockEntries, GuideScreenEditorActionGroup.BLOCK);
        blockEntries.add(GuideScreenEditorContextMenu.Entry.separator());
        append(blockEntries, GuideScreenEditorActionGroup.SCENE);
        blockEntries.add(GuideScreenEditorContextMenu.Entry.separator());
        append(blockEntries, GuideScreenEditorActionGroup.SCENE_MUTATION);
        insertEntries.add(
            GuideScreenEditorContextMenu.Entry
                .submenu(GuidebookText.GuideEditorContextMenuBlocks.text(), blockEntries));

        insertEntries.add(
            GuideScreenEditorContextMenu.Entry.submenu(
                GuidebookText.GuideEditorContextMenuEmbeds.text(),
                actionEntries(GuideScreenEditorActionGroup.EMBED)));
        insertEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.THEMATIC_BREAK));

        List<GuideScreenEditorContextMenu.Entry> entries = new ArrayList<>();
        entries.add(
            GuideScreenEditorContextMenu.Entry.submenu(GuidebookText.GuideEditorContextMenuEdit.text(), editEntries));
        entries.add(
            GuideScreenEditorContextMenu.Entry
                .submenu(GuidebookText.GuideEditorContextMenuInsert.text(), insertEntries));
        return entries;
    }

    private static List<GuideScreenEditorContextMenu.Entry> actionEntries(GuideScreenEditorActionGroup group) {
        ArrayList<GuideScreenEditorContextMenu.Entry> entries = new ArrayList<>();
        append(entries, group);
        return entries;
    }

    private static void append(List<GuideScreenEditorContextMenu.Entry> entries, GuideScreenEditorActionGroup group) {
        for (GuideScreenEditorAction action : actionsIn(group)) {
            entries.add(GuideScreenEditorContextMenu.Entry.action(action));
        }
    }

    private static List<GuideScreenEditorActionSpec> createSpecs() {
        ArrayList<GuideScreenEditorActionSpec> specs = new ArrayList<>();
        add(
            specs,
            GuideScreenEditorAction.UNDO,
            GuideScreenEditorActionGroup.EDIT,
            GuideScreenEditorActionGroup.TOOLBAR_ADVANCED);
        add(
            specs,
            GuideScreenEditorAction.REDO,
            GuideScreenEditorActionGroup.EDIT,
            GuideScreenEditorActionGroup.TOOLBAR_ADVANCED);
        add(
            specs,
            GuideScreenEditorAction.CUT,
            GuideScreenEditorActionGroup.EDIT,
            GuideScreenEditorActionGroup.TOOLBAR_ADVANCED);
        add(
            specs,
            GuideScreenEditorAction.COPY,
            GuideScreenEditorActionGroup.EDIT,
            GuideScreenEditorActionGroup.TOOLBAR_ADVANCED);
        add(
            specs,
            GuideScreenEditorAction.PASTE,
            GuideScreenEditorActionGroup.EDIT,
            GuideScreenEditorActionGroup.TOOLBAR_ADVANCED);
        add(
            specs,
            GuideScreenEditorAction.SELECT_ALL,
            GuideScreenEditorActionGroup.EDIT,
            GuideScreenEditorActionGroup.TOOLBAR_ADVANCED);

        add(
            specs,
            GuideScreenEditorAction.HEADING_1,
            GuideScreenEditorActionGroup.ROOT_INSERT,
            GuideScreenEditorActionGroup.TOOLBAR_BASE);
        add(
            specs,
            GuideScreenEditorAction.HEADING_2,
            GuideScreenEditorActionGroup.ROOT_INSERT,
            GuideScreenEditorActionGroup.TOOLBAR_BASE);
        add(
            specs,
            GuideScreenEditorAction.HEADING_3,
            GuideScreenEditorActionGroup.ROOT_INSERT,
            GuideScreenEditorActionGroup.TOOLBAR_BASE);
        add(
            specs,
            GuideScreenEditorAction.HEADING_4,
            GuideScreenEditorActionGroup.ROOT_INSERT,
            GuideScreenEditorActionGroup.TOOLBAR_ADVANCED);
        add(
            specs,
            GuideScreenEditorAction.HEADING_5,
            GuideScreenEditorActionGroup.ROOT_INSERT,
            GuideScreenEditorActionGroup.TOOLBAR_ADVANCED);
        add(
            specs,
            GuideScreenEditorAction.HEADING_6,
            GuideScreenEditorActionGroup.ROOT_INSERT,
            GuideScreenEditorActionGroup.TOOLBAR_ADVANCED);
        add(
            specs,
            GuideScreenEditorAction.BOLD,
            GuideScreenEditorActionGroup.ROOT_INSERT,
            GuideScreenEditorActionGroup.TOOLBAR_BASE);
        add(
            specs,
            GuideScreenEditorAction.ITALIC,
            GuideScreenEditorActionGroup.ROOT_INSERT,
            GuideScreenEditorActionGroup.TOOLBAR_BASE);
        add(
            specs,
            GuideScreenEditorAction.STRIKETHROUGH,
            GuideScreenEditorActionGroup.ROOT_INSERT,
            GuideScreenEditorActionGroup.TOOLBAR_ADVANCED);
        add(
            specs,
            GuideScreenEditorAction.UNDERLINE,
            GuideScreenEditorActionGroup.ROOT_INSERT,
            GuideScreenEditorActionGroup.TOOLBAR_ADVANCED);

        add(
            specs,
            GuideScreenEditorAction.KBD,
            GuideScreenEditorActionGroup.INLINE,
            GuideScreenEditorActionGroup.TOOLBAR_BASE);
        add(
            specs,
            GuideScreenEditorAction.SUBSCRIPT,
            GuideScreenEditorActionGroup.INLINE,
            GuideScreenEditorActionGroup.TOOLBAR_BASE);
        add(
            specs,
            GuideScreenEditorAction.SUPERSCRIPT,
            GuideScreenEditorActionGroup.INLINE,
            GuideScreenEditorActionGroup.TOOLBAR_BASE);
        add(
            specs,
            GuideScreenEditorAction.FOOTNOTE,
            GuideScreenEditorActionGroup.INLINE,
            GuideScreenEditorActionGroup.TOOLBAR_BASE);
        add(
            specs,
            GuideScreenEditorAction.SPOILER,
            GuideScreenEditorActionGroup.INLINE,
            GuideScreenEditorActionGroup.TOOLBAR_ADVANCED);
        add(
            specs,
            GuideScreenEditorAction.MARK,
            GuideScreenEditorActionGroup.INLINE,
            GuideScreenEditorActionGroup.TOOLBAR_ADVANCED);
        add(
            specs,
            GuideScreenEditorAction.COMMENT,
            GuideScreenEditorActionGroup.INLINE,
            GuideScreenEditorActionGroup.TOOLBAR_ADVANCED);
        add(
            specs,
            GuideScreenEditorAction.LATEX,
            GuideScreenEditorActionGroup.INLINE,
            GuideScreenEditorActionGroup.TOOLBAR_BASE);
        add(
            specs,
            GuideScreenEditorAction.LATEX_SHORTHAND,
            GuideScreenEditorActionGroup.INLINE,
            GuideScreenEditorActionGroup.TOOLBAR_ADVANCED);
        add(
            specs,
            GuideScreenEditorAction.KEY_BIND,
            GuideScreenEditorActionGroup.INLINE,
            GuideScreenEditorActionGroup.TOOLBAR_ADVANCED);
        add(
            specs,
            GuideScreenEditorAction.PLAYER_NAME,
            GuideScreenEditorActionGroup.INLINE,
            GuideScreenEditorActionGroup.TOOLBAR_ADVANCED);
        add(
            specs,
            GuideScreenEditorAction.COLOR,
            GuideScreenEditorActionGroup.INLINE,
            GuideScreenEditorActionGroup.TOOLBAR_BASE);
        add(
            specs,
            GuideScreenEditorAction.BREAK,
            GuideScreenEditorActionGroup.INLINE,
            GuideScreenEditorActionGroup.TOOLBAR_ADVANCED);

        add(
            specs,
            GuideScreenEditorAction.LINK,
            GuideScreenEditorActionGroup.LINK_AND_TEXT,
            GuideScreenEditorActionGroup.TOOLBAR_BASE);
        add(
            specs,
            GuideScreenEditorAction.REFERENCE_LINK,
            GuideScreenEditorActionGroup.LINK_AND_TEXT,
            GuideScreenEditorActionGroup.TOOLBAR_ADVANCED);
        add(
            specs,
            GuideScreenEditorAction.IMAGE,
            GuideScreenEditorActionGroup.LINK_AND_TEXT,
            GuideScreenEditorActionGroup.TOOLBAR_ADVANCED);
        add(
            specs,
            GuideScreenEditorAction.REFERENCE_IMAGE,
            GuideScreenEditorActionGroup.LINK_AND_TEXT,
            GuideScreenEditorActionGroup.TOOLBAR_ADVANCED);
        add(
            specs,
            GuideScreenEditorAction.INLINE_CODE,
            GuideScreenEditorActionGroup.LINK_AND_TEXT,
            GuideScreenEditorActionGroup.TOOLBAR_BASE);
        add(
            specs,
            GuideScreenEditorAction.CODE_BLOCK,
            GuideScreenEditorActionGroup.LINK_AND_TEXT,
            GuideScreenEditorActionGroup.TOOLBAR_BASE);
        add(
            specs,
            GuideScreenEditorAction.BLOCKQUOTE,
            GuideScreenEditorActionGroup.LINK_AND_TEXT,
            GuideScreenEditorActionGroup.TOOLBAR_BASE);

        add(
            specs,
            GuideScreenEditorAction.UNORDERED_LIST,
            GuideScreenEditorActionGroup.LIST_AND_TABLE,
            GuideScreenEditorActionGroup.TOOLBAR_BASE);
        add(
            specs,
            GuideScreenEditorAction.ORDERED_LIST,
            GuideScreenEditorActionGroup.LIST_AND_TABLE,
            GuideScreenEditorActionGroup.TOOLBAR_BASE);
        add(
            specs,
            GuideScreenEditorAction.TASK_LIST,
            GuideScreenEditorActionGroup.LIST_AND_TABLE,
            GuideScreenEditorActionGroup.TOOLBAR_BASE);
        add(
            specs,
            GuideScreenEditorAction.TABLE,
            GuideScreenEditorActionGroup.LIST_AND_TABLE,
            GuideScreenEditorActionGroup.TOOLBAR_BASE);

        addBlockActions(specs);
        addSceneActions(specs);
        addEmbedActions(specs);
        return List.copyOf(specs);
    }

    private static void addBlockActions(List<GuideScreenEditorActionSpec> specs) {
        addAdvanced(specs, GuideScreenEditorAction.ALERT_NOTE, GuideScreenEditorActionGroup.BLOCK);
        addAdvanced(specs, GuideScreenEditorAction.ALERT_TIP, GuideScreenEditorActionGroup.BLOCK);
        addAdvanced(specs, GuideScreenEditorAction.ALERT_IMPORTANT, GuideScreenEditorActionGroup.BLOCK);
        addAdvanced(specs, GuideScreenEditorAction.ALERT_WARNING, GuideScreenEditorActionGroup.BLOCK);
        addAdvanced(specs, GuideScreenEditorAction.ALERT_CAUTION, GuideScreenEditorActionGroup.BLOCK);
        addAdvanced(specs, GuideScreenEditorAction.DETAILS, GuideScreenEditorActionGroup.BLOCK);
        addAdvanced(specs, GuideScreenEditorAction.QUOTE_CALLOUT, GuideScreenEditorActionGroup.BLOCK);
        addAdvanced(specs, GuideScreenEditorAction.QUOTE_ICON_TEXT, GuideScreenEditorActionGroup.BLOCK);
        addAdvanced(specs, GuideScreenEditorAction.QUOTE_ICON_ITEM, GuideScreenEditorActionGroup.BLOCK);
        addAdvanced(specs, GuideScreenEditorAction.QUOTE_ICON_PNG, GuideScreenEditorActionGroup.BLOCK);
        addAdvanced(specs, GuideScreenEditorAction.CSV_TABLE, GuideScreenEditorActionGroup.BLOCK);
        addAdvanced(specs, GuideScreenEditorAction.MERMAID, GuideScreenEditorActionGroup.BLOCK);
        addAdvanced(specs, GuideScreenEditorAction.FILE_TREE, GuideScreenEditorActionGroup.BLOCK);
        addAdvanced(specs, GuideScreenEditorAction.SUB_PAGES, GuideScreenEditorActionGroup.BLOCK);
        addAdvanced(specs, GuideScreenEditorAction.CATEGORY, GuideScreenEditorActionGroup.BLOCK);
        addAdvanced(specs, GuideScreenEditorAction.SPECIAL, GuideScreenEditorActionGroup.BLOCK);
        addAdvanced(specs, GuideScreenEditorAction.FOOTNOTE_LIST, GuideScreenEditorActionGroup.BLOCK);
        addAdvanced(specs, GuideScreenEditorAction.ROW, GuideScreenEditorActionGroup.BLOCK);
        addAdvanced(specs, GuideScreenEditorAction.COLUMN, GuideScreenEditorActionGroup.BLOCK);
        addAdvanced(specs, GuideScreenEditorAction.DIV, GuideScreenEditorActionGroup.BLOCK);
        addAdvanced(specs, GuideScreenEditorAction.ITEM_GRID, GuideScreenEditorActionGroup.BLOCK);
        addAdvanced(specs, GuideScreenEditorAction.CSV_TABLE_IMPORT, GuideScreenEditorActionGroup.BLOCK);
        addAdvanced(specs, GuideScreenEditorAction.ANCHOR, GuideScreenEditorActionGroup.BLOCK);
        addAdvanced(specs, GuideScreenEditorAction.COLUMN_CHART, GuideScreenEditorActionGroup.BLOCK);
        addAdvanced(specs, GuideScreenEditorAction.BAR_CHART, GuideScreenEditorActionGroup.BLOCK);
        addAdvanced(specs, GuideScreenEditorAction.LINE_CHART, GuideScreenEditorActionGroup.BLOCK);
        addAdvanced(specs, GuideScreenEditorAction.PIE_CHART, GuideScreenEditorActionGroup.BLOCK);
        addAdvanced(specs, GuideScreenEditorAction.SCATTER_CHART, GuideScreenEditorActionGroup.BLOCK);
        addAdvanced(specs, GuideScreenEditorAction.CHART_SERIES, GuideScreenEditorActionGroup.BLOCK);
        addAdvanced(specs, GuideScreenEditorAction.CHART_LINE_SERIES, GuideScreenEditorActionGroup.BLOCK);
        addAdvanced(specs, GuideScreenEditorAction.CHART_SLICE, GuideScreenEditorActionGroup.BLOCK);
        addAdvanced(specs, GuideScreenEditorAction.CHART_PIE_INSET, GuideScreenEditorActionGroup.BLOCK);
        addAdvanced(specs, GuideScreenEditorAction.FUNCTION_GRAPH, GuideScreenEditorActionGroup.BLOCK);
        addAdvanced(specs, GuideScreenEditorAction.FUNCTION, GuideScreenEditorActionGroup.BLOCK);
        addAdvanced(specs, GuideScreenEditorAction.FUNCTION_PLOT, GuideScreenEditorActionGroup.BLOCK);
        addAdvanced(specs, GuideScreenEditorAction.FUNCTION_POINT, GuideScreenEditorActionGroup.BLOCK);
        addAdvanced(specs, GuideScreenEditorAction.FUNCTION_GRAPH_FENCE, GuideScreenEditorActionGroup.BLOCK);
        addAdvanced(specs, GuideScreenEditorAction.STRUCTURE, GuideScreenEditorActionGroup.BLOCK);
    }

    private static void addSceneActions(List<GuideScreenEditorActionSpec> specs) {
        addAdvanced(specs, GuideScreenEditorAction.GAME_SCENE, GuideScreenEditorActionGroup.SCENE);
        addAdvanced(specs, GuideScreenEditorAction.SCENE_BLOCK, GuideScreenEditorActionGroup.SCENE);
        addAdvanced(specs, GuideScreenEditorAction.SCENE_ENTITY, GuideScreenEditorActionGroup.SCENE);
        addAdvanced(specs, GuideScreenEditorAction.SCENE_PARTICLE, GuideScreenEditorActionGroup.SCENE);
        addAdvanced(specs, GuideScreenEditorAction.SCENE_WEATHER, GuideScreenEditorActionGroup.SCENE);
        addAdvanced(specs, GuideScreenEditorAction.SCENE_PLAY_SOUND, GuideScreenEditorActionGroup.SCENE);
        addAdvanced(specs, GuideScreenEditorAction.ISOMETRIC_CAMERA, GuideScreenEditorActionGroup.SCENE);
        addAdvanced(specs, GuideScreenEditorAction.BOX_ANNOTATION, GuideScreenEditorActionGroup.SCENE);
        addAdvanced(specs, GuideScreenEditorAction.BLOCK_ANNOTATION, GuideScreenEditorActionGroup.SCENE);
        addAdvanced(specs, GuideScreenEditorAction.LINE_ANNOTATION, GuideScreenEditorActionGroup.SCENE);
        addAdvanced(specs, GuideScreenEditorAction.DIAMOND_ANNOTATION, GuideScreenEditorActionGroup.SCENE);
        addAdvanced(specs, GuideScreenEditorAction.TEXT_ANNOTATION, GuideScreenEditorActionGroup.SCENE);
        addAdvanced(specs, GuideScreenEditorAction.BLOCK_ANNOTATION_TEMPLATE, GuideScreenEditorActionGroup.SCENE);

        addAdvanced(specs, GuideScreenEditorAction.IMPORT_STRUCTURE, GuideScreenEditorActionGroup.SCENE_MUTATION);
        addAdvanced(specs, GuideScreenEditorAction.IMPORT_STRUCTURE_LIB, GuideScreenEditorActionGroup.SCENE_MUTATION);
        addAdvanced(specs, GuideScreenEditorAction.IMPORT_PONDER, GuideScreenEditorActionGroup.SCENE_MUTATION);
        addAdvanced(specs, GuideScreenEditorAction.PLACE_BLOCK, GuideScreenEditorActionGroup.SCENE_MUTATION);
        addAdvanced(specs, GuideScreenEditorAction.REPLACE_BLOCK, GuideScreenEditorActionGroup.SCENE_MUTATION);
        addAdvanced(specs, GuideScreenEditorAction.REMOVE_BLOCKS, GuideScreenEditorActionGroup.SCENE_MUTATION);
        addAdvanced(specs, GuideScreenEditorAction.SCENE_REMOVE_ENTITY, GuideScreenEditorActionGroup.SCENE_MUTATION);
    }

    private static void addEmbedActions(List<GuideScreenEditorActionSpec> specs) {
        addAdvanced(specs, GuideScreenEditorAction.TOOLTIP, GuideScreenEditorActionGroup.EMBED);
        addAdvanced(specs, GuideScreenEditorAction.ITEM_IMAGE, GuideScreenEditorActionGroup.EMBED);
        addAdvanced(specs, GuideScreenEditorAction.BLOCK_IMAGE, GuideScreenEditorActionGroup.EMBED);
        addAdvanced(specs, GuideScreenEditorAction.ITEM_LINK, GuideScreenEditorActionGroup.EMBED);
        addAdvanced(specs, GuideScreenEditorAction.COMMAND_LINK, GuideScreenEditorActionGroup.EMBED);
        addAdvanced(specs, GuideScreenEditorAction.SOUND_LINK, GuideScreenEditorActionGroup.EMBED);
        addAdvanced(specs, GuideScreenEditorAction.RECIPE, GuideScreenEditorActionGroup.EMBED);
        addAdvanced(specs, GuideScreenEditorAction.RECIPE_FOR, GuideScreenEditorActionGroup.EMBED);
        addAdvanced(specs, GuideScreenEditorAction.RECIPES_FOR, GuideScreenEditorActionGroup.EMBED);
        addAdvanced(specs, GuideScreenEditorAction.FLOATING_IMAGE, GuideScreenEditorActionGroup.EMBED);
        addAdvanced(specs, GuideScreenEditorAction.QUEST_LINK, GuideScreenEditorActionGroup.EMBED);
        addAdvanced(specs, GuideScreenEditorAction.QUEST_CARD, GuideScreenEditorActionGroup.EMBED);
        addAdvanced(specs, GuideScreenEditorAction.QUEST_IDS, GuideScreenEditorActionGroup.EMBED);
        addAdvanced(specs, GuideScreenEditorAction.NAV_POSITION, GuideScreenEditorActionGroup.EMBED);
        addAdvanced(specs, GuideScreenEditorAction.NAV_ICON, GuideScreenEditorActionGroup.EMBED);
        addAdvanced(specs, GuideScreenEditorAction.NAV_ICON_TEXTURE, GuideScreenEditorActionGroup.EMBED);
        addAdvanced(specs, GuideScreenEditorAction.NAV_ICONS, GuideScreenEditorActionGroup.EMBED);
        addAdvanced(specs, GuideScreenEditorAction.NAV_ICON_TEXTURES, GuideScreenEditorActionGroup.EMBED);
        addAdvanced(specs, GuideScreenEditorAction.NAV_REQUIRED_MODS, GuideScreenEditorActionGroup.EMBED);
        addAdvanced(specs, GuideScreenEditorAction.PAGE_CATEGORIES, GuideScreenEditorActionGroup.EMBED);
        addAdvanced(specs, GuideScreenEditorAction.PAGE_ITEM_IDS, GuideScreenEditorActionGroup.EMBED);
        addAdvanced(specs, GuideScreenEditorAction.PAGE_ORE_IDS, GuideScreenEditorActionGroup.EMBED);
        addAdvanced(specs, GuideScreenEditorAction.PAGE_METADATA, GuideScreenEditorActionGroup.EMBED);
    }

    private static void addAdvanced(List<GuideScreenEditorActionSpec> specs, GuideScreenEditorAction action,
        GuideScreenEditorActionGroup group) {
        add(specs, action, group, GuideScreenEditorActionGroup.TOOLBAR_ADVANCED);
    }

    private static void add(List<GuideScreenEditorActionSpec> specs, GuideScreenEditorAction action,
        GuideScreenEditorActionGroup firstGroup, GuideScreenEditorActionGroup... additionalGroups) {
        EnumSet<GuideScreenEditorActionGroup> groups = EnumSet.of(firstGroup, additionalGroups);
        specs.add(new GuideScreenEditorActionSpec(action, groups));
    }

    private static Map<GuideScreenEditorActionGroup, List<GuideScreenEditorAction>> createActionsByGroup(
        List<GuideScreenEditorActionSpec> specs) {
        EnumMap<GuideScreenEditorActionGroup, List<GuideScreenEditorAction>> actionsByGroup = new EnumMap<>(
            GuideScreenEditorActionGroup.class);
        for (GuideScreenEditorActionSpec spec : specs) {
            for (GuideScreenEditorActionGroup group : spec.groups()) {
                actionsByGroup.computeIfAbsent(group, ignored -> new ArrayList<>())
                    .add(spec.action());
            }
        }
        actionsByGroup.replaceAll((ignored, actions) -> List.copyOf(actions));
        return actionsByGroup;
    }
}
