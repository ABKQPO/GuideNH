package com.hfstudio.guidenh.guide.internal.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.guide.internal.GuidebookText;

public class GuideIconButton extends GuiButton {

    public static final int WIDTH = 16;
    public static final int HEIGHT = 16;
    public static final int TEXTURE_SIZE = 256;

    public static final ResourceLocation TEX = new ResourceLocation("guidenh", "textures/guide/buttons.png");

    public static final ResourceLocation PONDER_WIDGETS_TEX = new ResourceLocation(
        "guidenh",
        "textures/guide/ponder_widgets.png");

    private Role role;
    private boolean active;

    public GuideIconButton(int id, int x, int y, Role role) {
        super(id, x, y, WIDTH, HEIGHT, "");
        this.role = role;
        this.active = false;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getTooltip() {
        return role.tooltip();
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) return;
        this.field_146123_n = mouseX >= xPosition && mouseY >= yPosition
            && mouseX < xPosition + width
            && mouseY < yPosition + height;

        int color = resolveIconColor(enabled, field_146123_n, active);

        drawIcon(mc, role, xPosition, yPosition, width, height, color);
    }

    public static int resolveIconColor(boolean enabled, boolean hovered, boolean active) {
        if (!enabled) {
            return 0x60FFFFFF;
        }
        if (active || hovered) {
            return 0xFF00CAF2;
        }
        return 0xC0FFFFFF;
    }

    public static void drawIcon(Minecraft mc, Role role, int x, int y, int width, int height, int color) {
        if (mc == null || role == null) {
            return;
        }

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT);
        try {
            mc.getTextureManager()
                .bindTexture(TEX);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            int a = (color >>> 24) & 0xFF;
            int r = (color >>> 16) & 0xFF;
            int g = (color >>> 8) & 0xFF;
            int b = color & 0xFF;
            GL11.glColor4f(r / 255f, g / 255f, b / 255f, a / 255f);

            float texSize = GuideIconButton.TEXTURE_SIZE;
            float u0 = role.iconSrcX / texSize;
            float v0 = role.iconSrcY / texSize;
            float u1 = (role.iconSrcX + 16) / texSize;
            float v1 = (role.iconSrcY + 16) / texSize;

            var tess = Tessellator.instance;
            tess.startDrawingQuads();
            tess.addVertexWithUV(x, y + height, 0, u0, v1);
            tess.addVertexWithUV(x + width, y + height, 0, u1, v1);
            tess.addVertexWithUV(x + width, y, 0, u1, v0);
            tess.addVertexWithUV(x, y, 0, u0, v0);
            tess.draw();
        } finally {
            GL11.glColor4f(1f, 1f, 1f, 1f);
            GL11.glPopAttrib();
        }
    }

    public enum Role {

        BACK(GuidebookText.HistoryGoBack, 0, 0),
        OPEN_NEI_RECIPE(GuidebookText.OpenRecipeInNei, 0, 0),
        FORWARD(GuidebookText.HistoryGoForward, 16, 0),
        CLOSE(GuidebookText.Close, 32, 0),
        SCENE_EDITOR_CLOSE(GuidebookText.SceneEditorClose, 32, 0),
        SEARCH(GuidebookText.Search, 48, 0),
        HOMEPAGE(GuidebookText.HomePage, 0, 32),
        BOOKMARK(GuidebookText.Bookmark, 128, 0),
        BOOKMARKED(GuidebookText.Bookmarked, 144, 0),
        SCENE_EDITOR_AUTO_PICK(GuidebookText.SceneEditorAutoPick, 48, 0),
        HIDE_ANNOTATIONS(GuidebookText.HideAnnotations, 0, 16),
        SHOW_ANNOTATIONS(GuidebookText.ShowAnnotations, 16, 16),
        HIGHLIGHT_STRUCTURELIB_HATCHES(GuidebookText.HighlightStructureLibHatches, 32, 48),
        SCENE_EDITOR_HIDE_ELEMENT(GuidebookText.SceneEditorHideElement, 0, 16),
        SCENE_EDITOR_SHOW_ELEMENT(GuidebookText.SceneEditorShowElement, 16, 16),
        ZOOM_OUT(GuidebookText.ZoomOut, 32, 16),
        ZOOM_IN(GuidebookText.ZoomIn, 48, 16),
        SCENE_EDITOR_ADD_ELEMENT(GuidebookText.SceneEditorAddElement, 48, 16),
        RESET_VIEW(GuidebookText.ResetView, 0, 32),
        SCENE_EDITOR_RESET_PREVIEW(GuidebookText.SceneEditorResetPreview, 0, 32),
        OPEN_FULL_WIDTH_VIEW(GuidebookText.FullWidthView, 16, 32),
        SCENE_EDITOR_EXPORT(GuidebookText.SceneEditorExport, 0, 48),
        SCENE_EDITOR_IMPORT_STRUCTURE(GuidebookText.SceneEditorImportStructure, 16, 48),
        SCENE_EDITOR_SCREENSHOT(GuidebookText.SceneEditorScreenshot, 32, 48),
        CLOSE_FULL_WIDTH_VIEW(GuidebookText.CloseFullWidthView, 32, 32),
        SCENE_EDITOR_SNAP(GuidebookText.SceneEditorSnap, 48, 48),
        SCENE_EDITOR_DELETE_ELEMENT(GuidebookText.SceneEditorDeleteElement, 32, 0),
        PONDER_PREV_KEYFRAME(GuidebookText.PonderPrevKeyframe, 0, 0),
        PONDER_PLAY_PAUSE(GuidebookText.PonderPlayPause, 0, 64),
        PONDER_RESTART(GuidebookText.PonderRestart, 0, 32),
        TOGGLE_GRID(GuidebookText.ToggleGrid, 16, 64),
        TOGGLE_BLOCK_STATS(GuidebookText.ToggleBlockStats, 0, 0),
        NAVIGATION_PIN(GuidebookText.NavigationPin, 0, 0),
        GUIDE_EDITOR_TOGGLE(GuidebookText.GuideEditorToggle, 0, 0),
        GUIDE_EDITOR_NEW_PAGE(GuidebookText.GuideEditorNewPage, 0, 0),
        GUIDE_EDITOR_AUTOSAVE(GuidebookText.GuideEditorAutosave, 0, 0),
        GUIDE_EDITOR_SAVE(GuidebookText.GuideEditorSave, 0, 0),
        GUIDE_EDITOR_LAYOUT_SPLIT(GuidebookText.GuideEditorLayoutSplit, 0, 0),
        GUIDE_EDITOR_LAYOUT_EDITOR_ONLY(GuidebookText.GuideEditorLayoutEditorOnly, 0, 0),
        GUIDE_EDITOR_LAYOUT_PREVIEW_ONLY(GuidebookText.GuideEditorLayoutPreviewOnly, 0, 0),
        GUIDE_EDITOR_ADVANCED_TOGGLE(GuidebookText.GuideEditorAdvancedToggle, 0, 0),
        GUIDE_EDITOR_HEADING_1(GuidebookText.GuideEditorHeading1, 0, 0),
        GUIDE_EDITOR_HEADING_2(GuidebookText.GuideEditorHeading2, 0, 0),
        GUIDE_EDITOR_HEADING_3(GuidebookText.GuideEditorHeading3, 0, 0),
        GUIDE_EDITOR_HEADING_4(GuidebookText.GuideEditorHeading4, 0, 0),
        GUIDE_EDITOR_HEADING_5(GuidebookText.GuideEditorHeading5, 0, 0),
        GUIDE_EDITOR_HEADING_6(GuidebookText.GuideEditorHeading6, 0, 0),
        GUIDE_EDITOR_BOLD(GuidebookText.GuideEditorBold, 112, 0),
        GUIDE_EDITOR_ITALIC(GuidebookText.GuideEditorItalic, 0, 0),
        GUIDE_EDITOR_STRIKETHROUGH(GuidebookText.GuideEditorStrikethrough, 0, 0),
        GUIDE_EDITOR_UNDERLINE(GuidebookText.GuideEditorUnderline, 0, 0),
        GUIDE_EDITOR_KEYBOARD(GuidebookText.GuideEditorKeyboard, 0, 0),
        GUIDE_EDITOR_SUBSCRIPT(GuidebookText.GuideEditorSubscript, 0, 0),
        GUIDE_EDITOR_SUPERSCRIPT(GuidebookText.GuideEditorSuperscript, 0, 0),
        GUIDE_EDITOR_FOOTNOTE(GuidebookText.GuideEditorFootnote, 0, 0),
        GUIDE_EDITOR_TOOLTIP(GuidebookText.GuideEditorTooltip, 0, 0),
        GUIDE_EDITOR_ITEM_IMAGE(GuidebookText.GuideEditorItemImage, 0, 0),
        GUIDE_EDITOR_BLOCK_IMAGE(GuidebookText.GuideEditorBlockImage, 96, 0),
        GUIDE_EDITOR_ITEM_LINK(GuidebookText.GuideEditorItemLink, 0, 0),
        GUIDE_EDITOR_LATEX(GuidebookText.GuideEditorLatex, 0, 0),
        GUIDE_EDITOR_CSV_TABLE(GuidebookText.GuideEditorCsvTable, 80, 16),
        GUIDE_EDITOR_COMMAND_LINK(GuidebookText.GuideEditorCommandLink, 0, 0),
        GUIDE_EDITOR_RECIPE(GuidebookText.GuideEditorRecipe, 0, 0),
        GUIDE_EDITOR_RECIPE_FOR(GuidebookText.GuideEditorRecipeFor, 0, 0),
        GUIDE_EDITOR_RECIPES_FOR(GuidebookText.GuideEditorRecipesFor, 0, 0),
        GUIDE_EDITOR_FLOATING_IMAGE(GuidebookText.GuideEditorFloatingImage, 0, 0),
        GUIDE_EDITOR_MERMAID(GuidebookText.GuideEditorMermaid, 0, 0),
        GUIDE_EDITOR_FILE_TREE(GuidebookText.GuideEditorFileTree, 144, 16),
        GUIDE_EDITOR_SUB_PAGES(GuidebookText.GuideEditorSubPages, 0, 0),
        GUIDE_EDITOR_CATEGORY(GuidebookText.GuideEditorCategory, 0, 0),
        GUIDE_EDITOR_FOOTNOTE_LIST(GuidebookText.GuideEditorFootnoteList, 0, 0),
        GUIDE_EDITOR_ROW(GuidebookText.GuideEditorRow, 0, 0),
        GUIDE_EDITOR_COLUMN(GuidebookText.GuideEditorColumn, 0, 0),
        GUIDE_EDITOR_DIV(GuidebookText.GuideEditorDiv, 128, 16),
        GUIDE_EDITOR_ITEM_GRID(GuidebookText.GuideEditorItemGrid, 0, 0),
        GUIDE_EDITOR_CSV_TABLE_IMPORT(GuidebookText.GuideEditorCsvTableImport, 96, 16),
        GUIDE_EDITOR_ANCHOR(GuidebookText.GuideEditorAnchor, 32, 64),
        GUIDE_EDITOR_COLUMN_CHART(GuidebookText.GuideEditorColumnChart, 0, 0),
        GUIDE_EDITOR_BAR_CHART(GuidebookText.GuideEditorBarChart, 48, 64),
        GUIDE_EDITOR_LINE_CHART(GuidebookText.GuideEditorLineChart, 176, 0),
        GUIDE_EDITOR_PIE_CHART(GuidebookText.GuideEditorPieChart, 192, 0),
        GUIDE_EDITOR_SCATTER_CHART(GuidebookText.GuideEditorScatterChart, 0, 0),
        GUIDE_EDITOR_CHART_SERIES(GuidebookText.GuideEditorChartSeries, 0, 0),
        GUIDE_EDITOR_CHART_LINE_SERIES(GuidebookText.GuideEditorChartLineSeries, 0, 0),
        GUIDE_EDITOR_CHART_SLICE(GuidebookText.GuideEditorChartSlice, 0, 0),
        GUIDE_EDITOR_CHART_PIE_INSET(GuidebookText.GuideEditorChartPieInset, 0, 0),
        GUIDE_EDITOR_FUNCTION_GRAPH(GuidebookText.GuideEditorFunctionGraph, 0, 0),
        GUIDE_EDITOR_FUNCTION(GuidebookText.GuideEditorFunction, 0, 0),
        GUIDE_EDITOR_FUNCTION_PLOT(GuidebookText.GuideEditorFunctionPlot, 0, 0),
        GUIDE_EDITOR_FUNCTION_POINT(GuidebookText.GuideEditorFunctionPoint, 0, 0),
        GUIDE_EDITOR_FUNCTION_GRAPH_FENCE(GuidebookText.GuideEditorFunctionGraphFence, 0, 0),
        GUIDE_EDITOR_STRUCTURE(GuidebookText.GuideEditorStructure, 0, 0),
        GUIDE_EDITOR_GAME_SCENE(GuidebookText.GuideEditorGameScene, 0, 0),
        GUIDE_EDITOR_SCENE_BLOCK(GuidebookText.GuideEditorSceneBlock, 0, 0),
        GUIDE_EDITOR_SCENE_ENTITY(GuidebookText.GuideEditorSceneEntity, 0, 0),
        GUIDE_EDITOR_ISOMETRIC_CAMERA(GuidebookText.GuideEditorIsometricCamera, 0, 0),
        GUIDE_EDITOR_BOX_ANNOTATION(GuidebookText.GuideEditorBoxAnnotation, 0, 0),
        GUIDE_EDITOR_BLOCK_ANNOTATION(GuidebookText.GuideEditorBlockAnnotation, 64, 0),
        GUIDE_EDITOR_LINE_ANNOTATION(GuidebookText.GuideEditorLineAnnotation, 0, 0),
        GUIDE_EDITOR_DIAMOND_ANNOTATION(GuidebookText.GuideEditorDiamondAnnotation, 240, 0),
        GUIDE_EDITOR_TEXT_ANNOTATION(GuidebookText.GuideEditorTextAnnotation, 0, 0),
        GUIDE_EDITOR_BLOCK_ANNOTATION_TEMPLATE(GuidebookText.GuideEditorBlockAnnotationTemplate, 80, 0),
        GUIDE_EDITOR_IMPORT_STRUCTURE(GuidebookText.GuideEditorImportStructure, 0, 0),
        GUIDE_EDITOR_IMPORT_STRUCTURE_LIB(GuidebookText.GuideEditorImportStructureLib, 0, 0),
        GUIDE_EDITOR_IMPORT_PONDER(GuidebookText.GuideEditorImportPonder, 0, 0),
        GUIDE_EDITOR_PLACE_BLOCK(GuidebookText.GuideEditorPlaceBlock, 0, 0),
        GUIDE_EDITOR_REPLACE_BLOCK(GuidebookText.GuideEditorReplaceBlock, 0, 0),
        GUIDE_EDITOR_REMOVE_BLOCKS(GuidebookText.GuideEditorRemoveBlocks, 0, 0),
        GUIDE_EDITOR_QUEST_LINK(GuidebookText.GuideEditorQuestLink, 0, 0),
        GUIDE_EDITOR_QUEST_CARD(GuidebookText.GuideEditorQuestCard, 0, 0),
        GUIDE_EDITOR_QUEST_IDS(GuidebookText.GuideEditorQuestIds, 0, 0),
        GUIDE_EDITOR_NAV_POSITION(GuidebookText.GuideEditorNavPosition, 0, 0),
        GUIDE_EDITOR_NAV_ICON(GuidebookText.GuideEditorNavIcon, 0, 0),
        GUIDE_EDITOR_NAV_ICON_TEXTURE(GuidebookText.GuideEditorNavIconTexture, 0, 0),
        GUIDE_EDITOR_NAV_ICONS(GuidebookText.GuideEditorNavIcons, 0, 0),
        GUIDE_EDITOR_NAV_ICON_TEXTURES(GuidebookText.GuideEditorNavIconTextures, 0, 0),
        GUIDE_EDITOR_NAV_REQUIRED_MODS(GuidebookText.GuideEditorNavRequiredMods, 0, 0),
        GUIDE_EDITOR_PAGE_CATEGORIES(GuidebookText.GuideEditorPageCategories, 0, 0),
        GUIDE_EDITOR_PAGE_ITEM_IDS(GuidebookText.GuideEditorPageItemIds, 0, 0),
        GUIDE_EDITOR_PAGE_ORE_IDS(GuidebookText.GuideEditorPageOreIds, 0, 0),
        GUIDE_EDITOR_PAGE_METADATA(GuidebookText.GuideEditorPageMetadata, 0, 0),
        GUIDE_EDITOR_QUOTE_CALLOUT(GuidebookText.GuideEditorQuoteCallout, 0, 0),
        GUIDE_EDITOR_QUOTE_ICON_TEXT(GuidebookText.GuideEditorQuoteIconText, 0, 0),
        GUIDE_EDITOR_QUOTE_ICON_ITEM(GuidebookText.GuideEditorQuoteIconItem, 0, 0),
        GUIDE_EDITOR_QUOTE_ICON_PNG(GuidebookText.GuideEditorQuoteIconPng, 0, 0),
        GUIDE_EDITOR_LATEX_SHORTHAND(GuidebookText.GuideEditorLatexShorthand, 0, 0),
        GUIDE_EDITOR_LINK(GuidebookText.GuideEditorLink, 0, 0),
        GUIDE_EDITOR_IMAGE(GuidebookText.GuideEditorImage, 0, 0),
        GUIDE_EDITOR_INLINE_CODE(GuidebookText.GuideEditorInlineCode, 0, 0),
        GUIDE_EDITOR_CODE_BLOCK(GuidebookText.GuideEditorCodeBlock, 208, 0),
        GUIDE_EDITOR_QUOTE(GuidebookText.GuideEditorQuote, 0, 0),
        GUIDE_EDITOR_BULLET_LIST(GuidebookText.GuideEditorBulletList, 224, 0),
        GUIDE_EDITOR_NUMBERED_LIST(GuidebookText.GuideEditorNumberedList, 0, 0),
        GUIDE_EDITOR_TASK_LIST(GuidebookText.GuideEditorTaskList, 0, 0),
        GUIDE_EDITOR_TABLE(GuidebookText.GuideEditorTable, 0, 0),
        GUIDE_EDITOR_ALERT_NOTE(GuidebookText.GuideEditorAlertNote, 0, 0),
        GUIDE_EDITOR_ALERT_TIP(GuidebookText.GuideEditorAlertTip, 0, 0),
        GUIDE_EDITOR_ALERT_IMPORTANT(GuidebookText.GuideEditorAlertImportant, 0, 0),
        GUIDE_EDITOR_ALERT_WARNING(GuidebookText.GuideEditorAlertWarning, 0, 0),
        GUIDE_EDITOR_ALERT_CAUTION(GuidebookText.GuideEditorAlertCaution, 0, 0),
        GUIDE_EDITOR_DETAILS(GuidebookText.GuideEditorDetails, 112, 16),
        GUIDE_EDITOR_KEY_BIND(GuidebookText.GuideEditorKeyBind, 0, 0),
        GUIDE_EDITOR_PLAYER_NAME(GuidebookText.GuideEditorPlayerName, 0, 0),
        GUIDE_EDITOR_COLOR(GuidebookText.GuideEditorColor, 0, 0),
        GUIDE_EDITOR_BREAK(GuidebookText.GuideEditorBreak, 160, 0),
        GUIDE_EDITOR_REFERENCE_LINK(GuidebookText.GuideEditorReferenceLink, 0, 0),
        GUIDE_EDITOR_REFERENCE_IMAGE(GuidebookText.GuideEditorReferenceImage, 0, 0),
        GUIDE_EDITOR_RULE(GuidebookText.GuideEditorRule, 0, 0),
        GUIDE_EDITOR_UNDO(GuidebookText.GuideEditorUndo, 0, 0),
        GUIDE_EDITOR_REDO(GuidebookText.GuideEditorRedo, 0, 0),
        GUIDE_EDITOR_CUT(GuidebookText.GuideEditorCut, 0, 0),
        GUIDE_EDITOR_COPY(GuidebookText.GuideEditorCopy, 64, 16),
        GUIDE_EDITOR_PASTE(GuidebookText.GuideEditorPaste, 0, 0),
        GUIDE_EDITOR_SELECT_ALL(GuidebookText.GuideEditorSelectAll, 0, 0);

        private final GuidebookText textKey;
        final int iconSrcX;
        final int iconSrcY;

        Role(GuidebookText textKey, int iconSrcX, int iconSrcY) {
            this.textKey = textKey;
            this.iconSrcX = iconSrcX;
            this.iconSrcY = iconSrcY;
        }

        public String tooltip() {
            return textKey.text();
        }

        public int iconSrcX() {
            return iconSrcX;
        }

        public int iconSrcY() {
            return iconSrcY;
        }
    }
}
