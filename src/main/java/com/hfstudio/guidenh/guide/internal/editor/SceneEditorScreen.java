package com.hfstudio.guidenh.guide.internal.editor;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BooleanSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.compat.structurelib.StructureLibPreviewSelection;
import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.color.LightDarkMode;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.interaction.ContentTooltip;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.ItemTooltip;
import com.hfstudio.guidenh.guide.document.interaction.TextTooltip;
import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorDraftTextController;
import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorElementContextMenuController;
import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorElementController;
import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorElementPropertyController;
import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorElementReorderController;
import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorHoverMenuState;
import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorLinkedSelectionController;
import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorMarkdownPanelState;
import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorMultilineTextArea;
import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorNumericFieldController;
import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorParameterController;
import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorPopupLayout;
import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorPreviewFrameOverlayLayout;
import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorScreenLayout;
import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorScreenshotMenuController;
import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorScrollState;
import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorSettingsTab;
import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorTextSyncController;
import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorUndoFieldState;
import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorUndoSnapshot;
import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorUndoUiState;
import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorVerticalScrollbar;
import com.hfstudio.guidenh.guide.internal.editor.io.SceneEditorClipboardExporter;
import com.hfstudio.guidenh.guide.internal.editor.io.SceneEditorSaveService;
import com.hfstudio.guidenh.guide.internal.editor.io.SceneEditorScreenshotExportService;
import com.hfstudio.guidenh.guide.internal.editor.io.SceneEditorScreenshotFormat;
import com.hfstudio.guidenh.guide.internal.editor.io.SceneEditorStructureCache;
import com.hfstudio.guidenh.guide.internal.editor.io.SceneEditorStructureImportService;
import com.hfstudio.guidenh.guide.internal.editor.md.SceneEditorMarkdownCodec;
import com.hfstudio.guidenh.guide.internal.editor.md.SceneEditorMarkdownElementRange;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorElementModel;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorElementType;
import com.hfstudio.guidenh.guide.internal.editor.preview.SceneEditorCameraMarkerOverlay;
import com.hfstudio.guidenh.guide.internal.editor.preview.SceneEditorHandleOverlay;
import com.hfstudio.guidenh.guide.internal.editor.preview.SceneEditorPickingService;
import com.hfstudio.guidenh.guide.internal.editor.preview.SceneEditorPointDragService;
import com.hfstudio.guidenh.guide.internal.editor.preview.SceneEditorPreviewBridge;
import com.hfstudio.guidenh.guide.internal.editor.preview.SceneEditorPreviewCameraController;
import com.hfstudio.guidenh.guide.internal.editor.preview.SceneEditorSnapModes;
import com.hfstudio.guidenh.guide.internal.editor.preview.SceneEditorSnapService;
import com.hfstudio.guidenh.guide.internal.screen.GuideIconButton;
import com.hfstudio.guidenh.guide.internal.tooltip.GuideItemTooltipLines;
import com.hfstudio.guidenh.guide.internal.tooltip.GuideItemTooltipRenderSupport;
import com.hfstudio.guidenh.guide.internal.ui.GuideSliderRenderer;
import com.hfstudio.guidenh.guide.internal.util.DisplayScale;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.layout.MinecraftFontMetrics;
import com.hfstudio.guidenh.guide.render.VanillaRenderContext;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.SavedCameraSettings;
import com.hfstudio.guidenh.guide.scene.support.GuideBlockDisplayResolver;
import com.hfstudio.guidenh.guide.scene.support.GuideEntityDisplayResolver;

public class SceneEditorScreen extends GuiScreen {

    public static final ResourceLocation BG_TEXTURE = new ResourceLocation(
        "guidenh",
        "textures/gui/sprites/background.png");
    public static final String MARKDOWN_UNDO_MERGE_KEY = "markdown";
    public static final long MARKDOWN_LIVE_SYNC_DEBOUNCE_MS = 120L;

    public static final int TOOLBAR_MARGIN_X = 10;
    public static final int TOOLBAR_Y = SceneEditorScreenLayout.TOOLBAR_Y;
    public static final int PANEL_COLOR = 0xB418181C;
    public static final int PANEL_INNER_COLOR = 0x70121216;
    public static final int PANEL_BORDER_COLOR = 0xFF5A5A5A;
    public static final int PANEL_HEADER_COLOR = 0xFFDEE6F0;
    public static final int PANEL_MUTED_TEXT = 0xFFB9C2CE;
    public static final int PANEL_SUBTLE_TEXT = 0xFF8F98A3;
    public static final int INPUT_BORDER_COLOR = 0xFF3E434A;
    public static final int INPUT_FOCUSED_BORDER_COLOR = 0xFF7FC8FF;
    public static final int INPUT_ERROR_BORDER_COLOR = 0xFFFF6767;
    public static final int INPUT_BACKGROUND_COLOR = 0x80101012;
    public static final int CHECKBOX_BACKGROUND_COLOR = 0xA0141418;
    public static final int CHECKBOX_CHECK_COLOR = 0xFF00CAF2;
    public static final int SETTINGS_BOX_PADDING = 8;
    public static final int PARAMETER_ROW_HEIGHT = 18;
    public static final int PARAMETER_LABEL_WIDTH = 52;
    public static final int PARAMETER_INPUT_WIDTH = 46;
    public static final int PARAMETER_INPUT_HEIGHT = 14;
    public static final int PARAMETER_GAP = 4;
    public static final int PARAMETER_SLIDER_HEIGHT = GuideSliderRenderer.TRACK_HEIGHT;
    public static final int PARAMETER_SLIDER_THUMB_WIDTH = GuideSliderRenderer.THUMB_WIDTH;
    public static final int PARAMETER_SLIDER_Y_OFFSET = 5;
    public static final int SETTINGS_TAB_HEIGHT = 18;
    public static final int SETTINGS_TAB_GAP = 4;
    public static final int SETTINGS_TAB_ACTIVE_COLOR = 0xD6202C36;
    public static final int SETTINGS_TAB_INACTIVE_COLOR = 0x6612181C;
    public static final int SETTINGS_TAB_HOVER_COLOR = 0xA61C252E;
    public static final int INTERACTIVE_ROW_HEIGHT = 18;
    public static final int INTERACTIVE_CHECKBOX_SIZE = 12;
    public static final int PREVIEW_FRAME_BUTTON_WIDTH = 74;
    public static final int PREVIEW_FRAME_BUTTON_HEIGHT = 14;
    public static final int ELEMENT_ROW_HEIGHT = 20;
    public static final int ELEMENT_ROW_GAP = 4;
    public static final int ELEMENT_EXPANDED_HEIGHT = 154;
    public static final int ELEMENT_ICON_SIZE = 14;
    public static final int ELEMENT_MENU_WIDTH = 102;
    public static final int ELEMENT_MENU_ROW_HEIGHT = 18;
    public static final int ELEMENT_ROW_BACKGROUND = 0x6A121418;
    public static final int ELEMENT_ROW_SELECTED = 0x9A1C222A;
    public static final int ELEMENT_ROW_EXPANDED = 0x7A101216;
    public static final int ELEMENT_MENU_BACKGROUND = 0xEE121418;
    public static final int ELEMENT_MENU_HOVER = 0xCC1A222A;
    public static final int ELEMENT_FIELD_ROW_HEIGHT = 18;
    public static final int ELEMENT_FIELD_LABEL_WIDTH = 62;
    public static final int ELEMENT_TOOLTIP_HEIGHT = 44;
    public static final int ELEMENT_VIEWPORT_TOP = 25;
    public static final int ELEMENT_VIEWPORT_BOTTOM_PADDING = 6;
    public static final int ELEMENT_SCROLLBAR_WIDTH = 5;
    public static final int ELEMENT_CONTEXT_MENU_WIDTH = 132;
    public static final int SNAP_MENU_WIDTH = 118;
    public static final int CLOSE_DIALOG_WIDTH = 248;
    public static final int CLOSE_DIALOG_HEIGHT = 104;
    public static final int CLOSE_DIALOG_BUTTON_WIDTH = 68;
    public static final int CLOSE_DIALOG_BUTTON_HEIGHT = 20;
    public static final int CLOSE_DIALOG_BUTTON_GAP = 10;
    public static final int CLOSE_DIALOG_OVERLAY_COLOR = 0x8A050608;
    public static final int CLOSE_DIALOG_COLOR = 0xF0181C22;
    public static final int CLOSE_DIALOG_HOVER = 0xCC24303A;

    public static final int CLOSE_BUTTON_ID = 0;
    public static final int RESET_PREVIEW_BUTTON_ID = 1;
    public static final int SNAP_BUTTON_ID = 2;
    public static final int AUTO_PICK_BUTTON_ID = 3;
    public static final int EXPORT_BUTTON_ID = 4;
    public static final int IMPORT_STRUCTURE_BUTTON_ID = 5;
    public static final int ADD_ELEMENT_BUTTON_ID = 6;
    public static final int SCREENSHOT_BUTTON_ID = 7;

    private final SceneEditorSession session;
    private final SceneEditorTextSyncController textSyncController;
    private final SceneEditorMarkdownCodec markdownCodec;
    private final SceneEditorParameterController parameterController;
    private final SceneEditorElementController elementController;
    private final SceneEditorElementContextMenuController elementContextMenuController;
    private final SceneEditorElementPropertyController elementPropertyController;
    private final SceneEditorElementReorderController elementReorderController;
    private final SceneEditorLinkedSelectionController linkedSelectionController;
    private final SceneEditorPreviewBridge previewBridge;
    private final SceneEditorPreviewCameraController previewCameraController;
    private final SceneEditorPickingService pickingService;
    private final SceneEditorHandleOverlay handleOverlay;
    private final SceneEditorCameraMarkerOverlay cameraMarkerOverlay;
    private final SceneEditorPointDragService pointDragService;
    private final SceneEditorSaveService saveService;
    private final SceneEditorScreenshotExportService screenshotExportService;
    private final SceneEditorScreenshotMenuController screenshotMenuController;
    private final SceneEditorStructureImportService structureImportService;
    private final LayoutContext previewLayoutContext;
    private final VanillaRenderContext previewRenderContext;
    private final VanillaRenderContext previewTooltipRenderContext;
    private final List<NumericParameterRow> numericParameterRows;
    private final SceneEditorScrollState elementPanelScrollState;
    private final Random elementColorRandom;
    private final SceneEditorHoverMenuState addElementMenuState;
    private final SceneEditorMarkdownPanelState markdownPanelState;

    private GuideIconButton closeButton;
    private GuideIconButton resetPreviewButton;
    private GuideIconButton snapButton;
    private GuideIconButton autoPickButton;
    private GuideIconButton exportButton;
    private GuideIconButton importStructureButton;
    private GuideIconButton screenshotButton;
    private GuideIconButton addElementButton;
    private SceneEditorMultilineTextArea markdownTextArea;
    @Nullable
    private GuiTextField screenshotScaleField;
    @Nullable
    private CompletableFuture<SceneEditorStructureImportService.ImportResult> pendingStructureImport;

    private int leftPanelX;
    private int leftPanelY;
    private int leftPanelWidth;
    private int leftPanelHeight;
    private int centerPanelX;
    private int centerPanelY;
    private int centerPanelWidth;
    private int centerPanelHeight;
    private int rightPanelX;
    private int rightPanelY;
    private int rightPanelWidth;
    private int rightPanelHeight;
    private int previewBoxX;
    private int previewBoxY;
    private int previewBoxWidth;
    private int previewBoxHeight;
    private int settingsBoxX;
    private int settingsBoxY;
    private int settingsBoxWidth;
    private int settingsBoxHeight;
    private int interactiveToggleX;
    private int interactiveToggleY;
    private int interactiveLabelX;
    private int previewFrameButtonX;
    private int previewFrameButtonY;
    private int elementsBoxX;
    private int elementsBoxY;
    private int elementsBoxWidth;
    private int elementsBoxHeight;

    @Nullable
    private LytGuidebookScene previewScene;
    @Nullable
    private LytGuidebookScene activePreviewScene;
    @Nullable
    private SceneEditorPointDragService.DragState activePointDrag;
    @Nullable
    private NumericParameterRow focusedParameterRow;
    @Nullable
    private NumericParameterRow activeSliderRow;
    @Nullable
    private UUID expandedElementId;
    @Nullable
    private ElementEditorPanel expandedElementEditor;
    @Nullable
    private UUID contextMenuElementId;
    private int contextMenuX;
    private int contextMenuY;
    private List<ElementContextMenuAction> contextMenuActions;
    private boolean addElementMenuOpen;
    private boolean snapModeMenuOpen;
    private boolean draggingScreenshotScaleSlider;
    private boolean elementListFocused;
    private boolean draggingMarkdownResize;
    private boolean draggingElementScrollbar;
    private int elementScrollbarGrabOffset;
    private boolean previewFrameOverlayVisible;
    private boolean previewDirty;
    private boolean preservePreviewCameraOnNextRebuild;
    @Nullable
    private Integer previewVisibleLayerOverride;
    @Nullable
    private StructureLibPreviewSelection previewStructureLibSelectionOverride;
    private boolean closeConfirmDialogOpen;
    @Nullable
    private String closeConfirmErrorText;
    private SceneEditorSettingsTab activeSettingsTab;
    private SceneEditorScreenLayout.Layout screenLayout;
    private boolean markdownLiveSyncPending;
    private long markdownLiveSyncAtMillis;
    @Nullable
    private String appliedUndoMergeKeyOverride;
    private boolean appliedUndoKeepOpenOverride;
    private boolean appliedUndoOverrideActive;

    public SceneEditorScreen() {
        this(SceneEditorSession.createBlank());
    }

    public SceneEditorScreen(SceneEditorSession session) {
        this.session = session;
        this.markdownCodec = new SceneEditorMarkdownCodec();
        String initialMarkdown = this.markdownCodec.serialize(session.getSceneModel());
        this.session.markSaved(initialMarkdown);
        this.textSyncController = new SceneEditorTextSyncController(this.session, this.markdownCodec);
        this.parameterController = new SceneEditorParameterController(this.session, this.markdownCodec);
        this.elementController = new SceneEditorElementController(this.session, this.markdownCodec);
        this.elementColorRandom = new Random();
        this.elementContextMenuController = new SceneEditorElementContextMenuController(
            this.session,
            this.markdownCodec,
            this.elementColorRandom::nextInt);
        this.elementPropertyController = new SceneEditorElementPropertyController(this.session, this.markdownCodec);
        this.elementReorderController = new SceneEditorElementReorderController();
        this.linkedSelectionController = new SceneEditorLinkedSelectionController();
        this.previewBridge = new SceneEditorPreviewBridge(Paths.get(""));
        this.previewCameraController = new SceneEditorPreviewCameraController();
        this.pickingService = new SceneEditorPickingService();
        this.handleOverlay = new SceneEditorHandleOverlay();
        this.cameraMarkerOverlay = new SceneEditorCameraMarkerOverlay();
        this.pointDragService = new SceneEditorPointDragService(new SceneEditorSnapService());
        SceneEditorStructureCache structureCache = SceneEditorStructureCache.createDefault();
        this.saveService = new SceneEditorSaveService(structureCache, new SceneEditorClipboardExporter());
        Minecraft minecraft = Minecraft.getMinecraft();
        Path screenshotRoot = minecraft != null ? minecraft.mcDataDir.toPath() : Paths.get(".");
        this.screenshotExportService = new SceneEditorScreenshotExportService(screenshotRoot);
        this.screenshotMenuController = new SceneEditorScreenshotMenuController(
            SceneEditorScreenshotFormat.fromConfigValue(ModConfig.ui.sceneEditorScreenshotFormat),
            ModConfig.ui.sceneEditorScreenshotScale,
            format -> {
                ModConfig.ui.sceneEditorScreenshotFormat = format.configValue();
                ModConfig.save();
            },
            scale -> {
                ModConfig.ui.sceneEditorScreenshotScale = scale;
                ModConfig.save();
            });
        this.structureImportService = new SceneEditorStructureImportService(structureCache);
        this.previewLayoutContext = new LayoutContext(new MinecraftFontMetrics());
        this.previewRenderContext = new VanillaRenderContext(LightDarkMode.LIGHT_MODE, LytRect.empty(), 0);
        this.previewTooltipRenderContext = new VanillaRenderContext(LightDarkMode.LIGHT_MODE, LytRect.empty(), 0);
        this.numericParameterRows = new ArrayList<>();
        this.elementPanelScrollState = new SceneEditorScrollState();
        this.addElementMenuState = new SceneEditorHoverMenuState();
        this.markdownPanelState = SceneEditorMarkdownPanelState.fromConfig(false);
        this.previewScene = null;
        this.activePreviewScene = null;
        this.activePointDrag = null;
        this.focusedParameterRow = null;
        this.activeSliderRow = null;
        this.expandedElementId = null;
        this.expandedElementEditor = null;
        this.contextMenuElementId = null;
        this.contextMenuX = 0;
        this.contextMenuY = 0;
        this.contextMenuActions = Collections.emptyList();
        this.addElementMenuOpen = false;
        this.snapModeMenuOpen = false;
        this.screenshotScaleField = null;
        this.draggingScreenshotScaleSlider = false;
        this.elementListFocused = false;
        this.draggingMarkdownResize = false;
        this.draggingElementScrollbar = false;
        this.elementScrollbarGrabOffset = 0;
        this.previewFrameOverlayVisible = false;
        this.previewDirty = true;
        this.preservePreviewCameraOnNextRebuild = false;
        this.previewVisibleLayerOverride = null;
        this.previewStructureLibSelectionOverride = null;
        this.closeConfirmDialogOpen = false;
        this.closeConfirmErrorText = null;
        this.activeSettingsTab = SceneEditorSettingsTab.CAMERA;
        this.screenLayout = SceneEditorScreenLayout
            .calculate(360, 220, this.markdownPanelState.isExpanded(), this.markdownPanelState.getOpenWidth());
    }

    public static void open() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return;
        }
        SceneEditorOpenService.OpenResult openResult = new SceneEditorOpenService().createInitialSession(mc.thePlayer);
        if (openResult.getOpenFeedbackMessage() != null && mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(
                new ChatComponentTranslation(
                    openResult.getOpenFeedbackMessage()
                        .getTranslationKey()));
        }
        mc.displayGuiScreen(new SceneEditorScreen(openResult.getSession()));
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        recalculateLayout();
        if (markdownTextArea == null) {
            markdownTextArea = new SceneEditorMultilineTextArea(this.fontRendererObj);
        }
        markdownTextArea.setWrapEnabled(markdownPanelState.isWrapEnabled());
        LytRect markdownBounds = screenLayout.markdownContent();
        markdownTextArea
            .setBounds(markdownBounds.x(), markdownBounds.y(), markdownBounds.width(), markdownBounds.height());
        markdownTextArea.setText(session.getRawText());

        initParameterRows();
        syncParameterRowsFromModel();
        layoutParameterRows();

        this.buttonList.clear();
        int toolbarX = TOOLBAR_MARGIN_X;
        closeButton = new GuideIconButton(
            CLOSE_BUTTON_ID,
            toolbarX,
            TOOLBAR_Y,
            GuideIconButton.Role.SCENE_EDITOR_CLOSE);
        resetPreviewButton = new GuideIconButton(
            RESET_PREVIEW_BUTTON_ID,
            toolbarX + 20,
            TOOLBAR_Y,
            GuideIconButton.Role.SCENE_EDITOR_RESET_PREVIEW);
        snapButton = new GuideIconButton(
            SNAP_BUTTON_ID,
            toolbarX + 40,
            TOOLBAR_Y,
            GuideIconButton.Role.SCENE_EDITOR_SNAP);
        autoPickButton = new GuideIconButton(
            AUTO_PICK_BUTTON_ID,
            toolbarX + 60,
            TOOLBAR_Y,
            GuideIconButton.Role.SCENE_EDITOR_AUTO_PICK);
        importStructureButton = new GuideIconButton(
            IMPORT_STRUCTURE_BUTTON_ID,
            toolbarX + 80,
            TOOLBAR_Y,
            GuideIconButton.Role.SCENE_EDITOR_IMPORT_STRUCTURE);
        exportButton = new GuideIconButton(
            EXPORT_BUTTON_ID,
            toolbarX + 100,
            TOOLBAR_Y,
            GuideIconButton.Role.SCENE_EDITOR_EXPORT);
        screenshotButton = new GuideIconButton(
            SCREENSHOT_BUTTON_ID,
            toolbarX + 120,
            TOOLBAR_Y,
            GuideIconButton.Role.SCENE_EDITOR_SCREENSHOT);

        if (screenshotScaleField == null) {
            screenshotScaleField = new GuiTextField(this.fontRendererObj, 0, 0, 0, PARAMETER_INPUT_HEIGHT);
            screenshotScaleField.setEnableBackgroundDrawing(false);
            screenshotScaleField.setMaxStringLength(2);
        }
        screenshotScaleField.setText(screenshotMenuController.getScaleDraftText());

        this.buttonList.add(closeButton);
        this.buttonList.add(resetPreviewButton);
        this.buttonList.add(snapButton);
        this.buttonList.add(autoPickButton);
        this.buttonList.add(importStructureButton);
        this.buttonList.add(exportButton);
        this.buttonList.add(screenshotButton);
        addElementButton = new GuideIconButton(
            ADD_ELEMENT_BUTTON_ID,
            elementsBoxX + elementsBoxWidth - GuideIconButton.WIDTH - 6,
            elementsBoxY + 3,
            GuideIconButton.Role.SCENE_EDITOR_ADD_ELEMENT);
        this.buttonList.add(addElementButton);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        for (NumericParameterRow row : numericParameterRows) {
            row.updateCursorCounter();
        }
        if (expandedElementEditor != null) {
            expandedElementEditor.updateCursorCounter();
        }
        if (screenshotScaleField != null) {
            screenshotScaleField.updateCursorCounter();
        }
        processPendingStructureImport();
        processPendingMarkdownLiveSync();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == CLOSE_BUTTON_ID) {
            requestCloseEditor();
            return;
        }
        if (button.id == RESET_PREVIEW_BUTTON_ID) {
            ensurePreviewScene();
            if (previewScene != null) {
                previewCameraController.resetPreviewView(previewScene, session.getSceneModel());
            }
            return;
        }
        if (button.id == SNAP_BUTTON_ID) {
            ModConfig.ui.sceneEditorSnapEnabled = !ModConfig.ui.sceneEditorSnapEnabled;
            ModConfig.save();
            snapModeMenuOpen = false;
            return;
        }
        if (button.id == AUTO_PICK_BUTTON_ID) {
            ModConfig.ui.sceneEditorAutoPickEnabled = !ModConfig.ui.sceneEditorAutoPickEnabled;
            ModConfig.save();
            return;
        }
        if (button.id == IMPORT_STRUCTURE_BUTTON_ID) {
            importStructureFromFile();
            return;
        }
        if (button.id == EXPORT_BUTTON_ID) {
            attemptSaveWithoutClose();
            return;
        }
        if (button.id == SCREENSHOT_BUTTON_ID) {
            exportPreviewScreenshot();
            return;
        }
        if (button.id == ADD_ELEMENT_BUTTON_ID) {
            return;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (closeConfirmDialogOpen) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_E) {
                closeConfirmDialogOpen = false;
                closeConfirmErrorText = null;
            }
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_E && !hasFocusedTextInput()) {
            requestCloseEditor();
            return;
        }
        if (handleUndoRedoShortcut(keyCode)) {
            return;
        }
        String previousMarkdownText = markdownTextArea != null ? markdownTextArea.getText() : null;
        if (markdownTextArea != null && markdownTextArea.keyTyped(typedChar, keyCode)) {
            textSyncController.setDraftText(markdownTextArea.getText());
            if (!markdownTextArea.getText()
                .equals(previousMarkdownText)) {
                textSyncController.recordCurrentSnapshot(MARKDOWN_UNDO_MERGE_KEY, captureCurrentUiUndoState());
                scheduleMarkdownLiveSync();
            }
            refreshLinkedSelectionFromMarkdownCursor();
            refreshMarkdownHighlightFromSelectedElement();
            return;
        }
        if (focusedParameterRow != null && focusedParameterRow.keyTyped(typedChar, keyCode)) {
            return;
        }
        if (expandedElementEditor != null && expandedElementEditor.keyTyped(typedChar, keyCode)) {
            return;
        }
        if (handleScreenshotScaleKeyTyped(typedChar, keyCode)) {
            return;
        }
        if (handleElementShortcutKey(keyCode)) {
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        recalculateLayout();
        if (markdownTextArea != null) {
            LytRect markdownBounds = screenLayout.markdownContent();
            markdownTextArea
                .setBounds(markdownBounds.x(), markdownBounds.y(), markdownBounds.width(), markdownBounds.height());
        }
        refreshLinkedSelectionFromMarkdownCursor();
        refreshMarkdownHighlightFromSelectedElement();
        layoutParameterRows();
        if (addElementButton != null) {
            addElementButton.xPosition = elementsBoxX + elementsBoxWidth - GuideIconButton.WIDTH - 6;
            addElementButton.yPosition = elementsBoxY + 3;
            addElementButton.visible = true;
        }
        syncToolbarToggleState();

        drawTiledBackground();
        drawCenterPanel(mouseX, mouseY);
        drawPanel(leftPanelX, leftPanelY, leftPanelWidth, leftPanelHeight);
        drawPanel(rightPanelX, rightPanelY, rightPanelWidth, rightPanelHeight);

        drawLeftPanel(mouseX, mouseY);
        drawRightPanel(mouseX, mouseY);
        drawMarkdownToggle(mouseX, mouseY);
        drawToolbarTitle();

        super.drawScreen(mouseX, mouseY, partialTicks);
        if (snapModeMenuOpen) {
            drawSnapModeMenu(mouseX, mouseY);
        }
        if (screenshotMenuController.isOpen()) {
            drawScreenshotMenu(mouseX, mouseY);
        }

        if (closeConfirmDialogOpen) {
            drawCloseConfirmDialog(mouseX, mouseY);
            return;
        }

        GuideIconButton hoveredButton = hoveredButton(mouseX, mouseY);
        if (hoveredButton != null) {
            this.drawHoveringText(
                Collections.singletonList(hoveredButton.getTooltip()),
                mouseX,
                mouseY,
                this.fontRendererObj);
        } else if (isInsideMarkdownToggle(mouseX, mouseY)) {
            this.drawHoveringText(
                Collections.singletonList(GuidebookText.SceneEditorMarkdownPanel.text()),
                mouseX,
                mouseY,
                this.fontRendererObj);
        } else if (isInsideMarkdownFooter(mouseX, mouseY)) {
            this.drawHoveringText(
                Collections.singletonList(GuidebookText.SceneEditorMarkdownWrap.text()),
                mouseX,
                mouseY,
                this.fontRendererObj);
        } else {
            drawPreviewSceneHoverTooltip(mouseX, mouseY);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        if (closeConfirmDialogOpen) {
            return;
        }
        int wheelDelta = Mouse.getEventDWheel();
        if (wheelDelta != 0 && markdownTextArea != null) {
            int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
            int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
            if (screenshotMenuController.isOpen() && (isInsideScreenshotMenu(mouseX, mouseY)
                || screenshotScaleField != null && screenshotScaleField.isFocused())) {
                screenshotMenuController.nudgeScale(wheelDelta);
                if (screenshotScaleField != null) {
                    screenshotScaleField.setText(screenshotMenuController.getScaleDraftText());
                }
                return;
            }
            if (focusedParameterRow != null && focusedParameterRow.applyMouseWheel(wheelDelta)) {
                return;
            }
            if (expandedElementEditor != null && expandedElementEditor.handleScrollWheel(mouseX, mouseY, wheelDelta)) {
                return;
            }
            if (isInsideElementViewport(mouseX, mouseY)) {
                int step = GuiScreen.isShiftKeyDown() ? 48 : 16;
                elementPanelScrollState.scrollPixels(-Integer.signum(wheelDelta) * step);
                return;
            }
            if (markdownPanelState.isExpanded() && markdownTextArea.contains(mouseX, mouseY)) {
                markdownTextArea.scrollWheel(wheelDelta);
                return;
            }
            ensurePreviewScene();
            if (previewScene != null && isInsidePreviewInteractionArea(mouseX, mouseY)
                && (previewScene.containsSceneViewport(mouseX, mouseY)
                    || previewScene.containsBottomControlSlider(mouseX, mouseY))) {
                previewScene.scroll(mouseX, mouseY, wheelDelta);
                return;
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        if (closeConfirmDialogOpen) {
            handleCloseConfirmDialogClick(mouseX, mouseY, button);
            return;
        }
        if (!isInsideElementViewport(mouseX, mouseY) && !isInsideAddElementButton(mouseX, mouseY)
            && !isInsideAddElementMenu(mouseX, mouseY)
            && !isInsideElementContextMenu(mouseX, mouseY)) {
            elementListFocused = false;
        }
        if (markdownTextArea != null) {
            boolean insideEditor = markdownTextArea.contains(mouseX, mouseY);
            if (markdownTextArea.isFocused() && !insideEditor) {
                if (!commitMarkdownEditor()) {
                    return;
                }
                markdownTextArea.setFocused(false);
            }
            String previousMarkdownText = markdownTextArea.getText();
            if (markdownTextArea.mouseClicked(mouseX, mouseY, button)) {
                textSyncController.setDraftText(markdownTextArea.getText());
                if (!previousMarkdownText.equals(markdownTextArea.getText())) {
                    textSyncController.recordCurrentSnapshot(MARKDOWN_UNDO_MERGE_KEY, captureCurrentUiUndoState());
                    scheduleMarkdownLiveSync();
                }
                refreshLinkedSelectionFromMarkdownCursor();
                refreshMarkdownHighlightFromSelectedElement();
                return;
            }
        }

        if (!commitFocusedParameterRow(mouseX, mouseY)) {
            return;
        }
        if (!commitExpandedElementEditor(mouseX, mouseY)) {
            return;
        }
        if (handleScreenshotMenuClick(mouseX, mouseY, button)) {
            return;
        }
        if (button == 1 && isInsideScreenshotButton(mouseX, mouseY)) {
            screenshotMenuController.toggleOpen();
            snapModeMenuOpen = false;
            return;
        }
        if (handleSnapModeMenuClick(mouseX, mouseY, button)) {
            return;
        }
        if (button == 1 && isInsideSnapButton(mouseX, mouseY)) {
            snapModeMenuOpen = !snapModeMenuOpen;
            screenshotMenuController.close();
            return;
        }
        if (snapModeMenuOpen && !isInsideSnapButton(mouseX, mouseY) && !isInsideSnapModeMenu(mouseX, mouseY)) {
            snapModeMenuOpen = false;
        }
        if (button == 0 && isInsideMarkdownToggle(mouseX, mouseY)) {
            toggleMarkdownPanel();
            return;
        }
        if (button == 0 && isInsideMarkdownFooter(mouseX, mouseY)) {
            toggleMarkdownWrap();
            return;
        }
        if (button == 0 && isInsideMarkdownResizeHandle(mouseX, mouseY)) {
            draggingMarkdownResize = true;
            closeElementContextMenu();
            updateMarkdownResize(mouseX);
            return;
        }
        if (handleElementContextMenuClick(mouseX, mouseY, button)) {
            return;
        }
        if (button == 0 && handleSettingsTabClick(mouseX, mouseY)) {
            return;
        }
        if (handleParameterMouseClick(mouseX, mouseY, button)) {
            return;
        }
        if (button == 0 && isInsidePreviewFrameButton(mouseX, mouseY)) {
            previewFrameOverlayVisible = !previewFrameOverlayVisible;
            return;
        }
        if (button == 0 && isInsideInteractiveToggle(mouseX, mouseY)) {
            parameterController.setInteractive(
                !session.getSceneModel()
                    .isInteractive());
            onDocumentOnlyApplied();
            return;
        }
        if (handleElementScrollbarClick(mouseX, mouseY, button)) {
            return;
        }
        if (handleElementPanelClick(mouseX, mouseY, button)) {
            return;
        }

        ensurePreviewScene();
        if (previewScene != null) {
            GuideIconButton.Role sceneButtonRole = isInsidePreviewInteractionArea(mouseX, mouseY)
                ? previewScene.sceneButtonAt(mouseX, mouseY)
                : null;
            if (button == 0 && sceneButtonRole != null) {
                previewScene.activateSceneButton(sceneButtonRole);
                return;
            }
            if (button == 0 && isInsidePreviewInteractionArea(mouseX, mouseY)
                && previewScene.containsBottomControlSlider(mouseX, mouseY)) {
                activePreviewScene = previewScene;
                activePreviewScene.startDrag(mouseX, mouseY, button);
                return;
            }
            if (button == 0 && isInsidePreviewInteractionArea(mouseX, mouseY)
                && previewScene.containsSceneViewport(mouseX, mouseY)) {
                SceneEditorElementModel selectedPointElement = getSelectedPointHandleElement();
                if (selectedPointElement != null) {
                    String handleId = handleOverlay.pickHandle(
                        selectedPointElement,
                        previewScene.getCamera(),
                        previewScene.getScreenRect(),
                        mouseX,
                        mouseY);
                    if (handleId != null) {
                        activePointDrag = pointDragService.beginHandleDrag(
                            selectedPointElement,
                            handleId,
                            previewScene.getCamera(),
                            previewScene.getScreenRect(),
                            mouseX,
                            mouseY);
                        if (activePointDrag != null) {
                            session.getSelectionState()
                                .setSelectedHandleId(handleId);
                            session.getSelectionState()
                                .setDragging(true);
                            closeElementContextMenu();
                            return;
                        }
                    }
                }
            }
            if ((button == 0 || button == 1) && isInsidePreviewInteractionArea(mouseX, mouseY)
                && previewScene.containsSceneViewport(mouseX, mouseY)) {
                if (button == 0 && pickingService.applyPreviewClickSelection(
                    session,
                    previewScene,
                    mouseX,
                    mouseY,
                    ModConfig.ui.sceneEditorAutoPickEnabled)) {
                    closeElementContextMenu();
                }
                activePreviewScene = previewScene;
                activePreviewScene.startDrag(mouseX, mouseY, button);
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (closeConfirmDialogOpen) {
            return;
        }
        if (draggingMarkdownResize && clickedMouseButton == 0) {
            updateMarkdownResize(mouseX);
            return;
        }
        if (markdownTextArea != null && markdownTextArea.mouseDragged(mouseX, mouseY, clickedMouseButton)) {
            return;
        }
        if (activeSliderRow != null && clickedMouseButton == 0) {
            activeSliderRow.applySliderAt(mouseX);
            return;
        }
        if (clickedMouseButton == 0 && draggingElementScrollbar) {
            dragElementScrollbar(mouseY);
            return;
        }
        if (clickedMouseButton == 0 && elementReorderController.isDragging()) {
            elementReorderController.updateDrag(mouseY, buildElementRowMetrics());
            return;
        }
        if (clickedMouseButton == 0 && draggingScreenshotScaleSlider) {
            LytRect sliderBounds = screenshotMenuController.scaleSliderBounds(getScreenshotMenuBounds());
            screenshotMenuController.applyScaleFraction(
                GuideSliderRenderer.fractionFromMouse(mouseX, sliderBounds.x(), sliderBounds.width()));
            if (screenshotScaleField != null) {
                screenshotScaleField.setText(screenshotMenuController.getScaleDraftText());
            }
            return;
        }
        if (clickedMouseButton == 0 && activePointDrag != null && previewScene != null) {
            if (pointDragService.updateHandleDrag(
                elementPropertyController,
                previewScene.getLevel(),
                previewScene.getCamera(),
                previewScene.getScreenRect(),
                activePointDrag,
                mouseX,
                mouseY,
                ModConfig.ui.sceneEditorSnapEnabled,
                currentSnapModes())) {
                onElementModelApplied();
            }
            return;
        }
        if (activePreviewScene != null) {
            activePreviewScene.drag(mouseX, mouseY);
            return;
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int state) {
        if (markdownTextArea != null && state != -1) {
            markdownTextArea.mouseReleased(state);
        }
        if (closeConfirmDialogOpen) {
            if (state != -1) {
                activeSliderRow = null;
            }
            return;
        }
        if (activeSliderRow != null && state != -1) {
            activeSliderRow = null;
            return;
        }
        if (draggingScreenshotScaleSlider && state != -1) {
            draggingScreenshotScaleSlider = false;
            return;
        }
        if (draggingMarkdownResize && state != -1) {
            draggingMarkdownResize = false;
            markdownPanelState.persistOpenWidth();
            return;
        }
        if (draggingElementScrollbar && state != -1) {
            draggingElementScrollbar = false;
            return;
        }
        if (elementReorderController.isDragging() && state != -1) {
            SceneEditorElementReorderController.MoveOperation move = elementReorderController
                .finishDrag(mouseY, buildElementRowMetrics());
            if (move.hasMove()) {
                elementController.moveElement(move.getFromIndex(), move.getToIndex());
                onElementModelApplied();
            }
            return;
        }
        if (activePointDrag != null && state != -1) {
            activePointDrag = null;
            session.getSelectionState()
                .setSelectedHandleId(null);
            session.getSelectionState()
                .setDragging(false);
            return;
        }
        if (activePreviewScene != null && state != -1) {
            activePreviewScene.endDrag();
            activePreviewScene = null;
            return;
        }
        super.mouseMovedOrUp(mouseX, mouseY, state);
    }

    private void recalculateLayout() {
        screenLayout = SceneEditorScreenLayout
            .calculate(this.width, this.height, markdownPanelState.isExpanded(), markdownPanelState.getOpenWidth());

        leftPanelX = screenLayout.leftPanel()
            .x();
        leftPanelY = screenLayout.leftPanel()
            .y();
        leftPanelWidth = screenLayout.leftPanel()
            .width();
        leftPanelHeight = screenLayout.leftPanel()
            .height();

        centerPanelX = screenLayout.previewRender()
            .x();
        centerPanelY = screenLayout.previewRender()
            .y();
        centerPanelWidth = screenLayout.previewRender()
            .width();
        centerPanelHeight = screenLayout.previewRender()
            .height();

        rightPanelX = screenLayout.rightPanel()
            .x();
        rightPanelY = screenLayout.rightPanel()
            .y();
        rightPanelWidth = screenLayout.rightPanel()
            .width();
        rightPanelHeight = screenLayout.rightPanel()
            .height();

        previewBoxX = screenLayout.previewRender()
            .x();
        previewBoxY = screenLayout.previewRender()
            .y();
        previewBoxWidth = screenLayout.previewRender()
            .width();
        previewBoxHeight = screenLayout.previewRender()
            .height();
    }

    private void drawToolbarTitle() {
        String structureSource = session.getSceneModel()
            .getStructureSource();
        boolean hasImportedStructure = structureSource != null && !structureSource.isEmpty();
        int titleX = TOOLBAR_MARGIN_X + 152;
        int sessionLabelY = TOOLBAR_Y + 16;
        this.drawString(this.fontRendererObj, GuidebookText.SceneEditorTitle.text(), titleX, TOOLBAR_Y + 4, 0xFFFFFF);
        String sessionLabel = hasImportedStructure ? GuidebookText.SceneEditorImportedSession.text()
            : GuidebookText.SceneEditorBlankSession.text();
        this.drawString(this.fontRendererObj, sessionLabel, titleX, sessionLabelY, 0xFF8FC7FF);
    }

    private void syncToolbarToggleState() {
        if (snapButton != null) {
            snapButton.setActive(ModConfig.ui.sceneEditorSnapEnabled);
        }
        if (autoPickButton != null) {
            autoPickButton.setActive(ModConfig.ui.sceneEditorAutoPickEnabled);
        }
    }

    private void drawLeftPanel(int mouseX, int mouseY) {
        if (!markdownPanelState.isExpanded()) {
            return;
        }
        int headerX = screenLayout.markdownContent()
            .x();
        int headerY = screenLayout.markdownContent()
            .y() - this.fontRendererObj.FONT_HEIGHT
            - 1;
        this.drawString(
            this.fontRendererObj,
            GuidebookText.SceneEditorMarkdownPanel.text(),
            headerX,
            headerY,
            PANEL_HEADER_COLOR);

        if (markdownTextArea != null) {
            markdownTextArea.draw(textSyncController.hasValidationError());
        }
        LytRect footerBounds = screenLayout.markdownFooter();
        if (textSyncController.hasValidationError()) {
            String title = textSyncController.getValidationKind()
                == SceneEditorTextSyncController.ValidationKind.UNSUPPORTED
                    ? GuidebookText.SceneEditorUnsupportedSyntax.text()
                    : GuidebookText.SceneEditorSyntaxError.text();
            this.drawString(this.fontRendererObj, title, headerX, footerBounds.y() - 26, 0xFFFF8484);
            this.drawString(
                this.fontRendererObj,
                GuidebookText.SceneEditorTextSyncHint.text(),
                headerX,
                footerBounds.y() - 14,
                PANEL_SUBTLE_TEXT);
        } else {
            this.drawString(
                this.fontRendererObj,
                GuidebookText.SceneEditorTextSyncHint.text(),
                headerX,
                footerBounds.y() - 14,
                PANEL_SUBTLE_TEXT);
        }
        drawMarkdownFooter(mouseX, mouseY);
        drawMarkdownResizeHandle(mouseX, mouseY);
    }

    private void drawMarkdownFooter(int mouseX, int mouseY) {
        if (!markdownPanelState.isExpanded()) {
            return;
        }
        LytRect footerBounds = screenLayout.markdownFooter();
        boolean hovered = footerBounds.contains(mouseX, mouseY);
        boolean wrapEnabled = markdownPanelState.isWrapEnabled();
        int backgroundColor = wrapEnabled ? hovered ? 0xD01CB4E9 : 0xA81CB4E9 : hovered ? 0xA61C252E : 0x6612181C;
        drawRect(footerBounds.x(), footerBounds.y(), footerBounds.right(), footerBounds.bottom(), backgroundColor);
        drawBorder(
            footerBounds.x(),
            footerBounds.y(),
            footerBounds.width(),
            footerBounds.height(),
            hovered ? 0xFF00CAF2 : INPUT_BORDER_COLOR);
        String label = wrapEnabled ? GuidebookText.SceneEditorMarkdownWrapOn.text()
            : GuidebookText.SceneEditorMarkdownWrapOff.text();
        this.drawString(this.fontRendererObj, label, footerBounds.x() + 6, footerBounds.y() + 5, PANEL_HEADER_COLOR);
    }

    private void drawMarkdownResizeHandle(int mouseX, int mouseY) {
        if (!markdownPanelState.isExpanded()) {
            return;
        }
        LytRect handleBounds = screenLayout.markdownResizeHandle();
        if (handleBounds.isEmpty()) {
            return;
        }
        boolean highlighted = draggingMarkdownResize || handleBounds.contains(mouseX, mouseY);
        int lineLeft = handleBounds.x() + handleBounds.width() / 2 - 1;
        drawRect(
            lineLeft,
            handleBounds.y() + 8,
            lineLeft + 2,
            handleBounds.bottom() - 8,
            highlighted ? 0xFF00CAF2 : 0x66464A50);
    }

    private void drawCenterPanel(int mouseX, int mouseY) {
        ensurePreviewScene();
        if (previewScene != null) {
            int reservedBottomHeight = previewScene.isReserveBottomControlArea()
                ? previewScene.getBottomControlAreaHeight()
                : 0;
            int previewSceneHeight = Math.max(16, previewBoxHeight - reservedBottomHeight);
            previewScene.setSceneSize(previewBoxWidth, previewSceneHeight);
            previewScene.layout(previewLayoutContext, previewBoxX, previewBoxY, previewBoxWidth);
            previewRenderContext.setScreenHeight(this.height);
            previewRenderContext.setViewport(new LytRect(previewBoxX, previewBoxY, previewBoxWidth, previewBoxHeight));
            previewRenderContext.setDocumentOrigin(0, 0);
            previewRenderContext.setScrollOffsetY(0);
            if (isInsidePreviewInteractionArea(mouseX, mouseY)
                && !previewScene.containsBottomControlSlider(mouseX, mouseY)) {
                var hoveredAnnotation = previewScene.updateAnnotationHover(mouseX, mouseY);
                if (hoveredAnnotation != null) {
                    previewScene.setHoveredStructureLibHatch(null);
                    previewScene.setHoveredBlock(null);
                    previewScene.setHoveredEntity(null);
                } else {
                    previewScene.setHoveredStructureLibHatch(previewScene.pickStructureLibHatch(mouseX, mouseY));
                    previewScene.updateHoveredSceneTarget(mouseX, mouseY);
                }
            } else {
                previewScene.clearAnnotationHover();
                previewScene.setHoveredStructureLibHatch(null);
                previewScene.setHoveredBlock(null);
                previewScene.setHoveredEntity(null);
            }
            previewScene.render(previewRenderContext);
            drawPreviewStructureSource(previewScene);
            LytRect previewViewport = previewScene.getScreenRect();
            if (previewFrameOverlayVisible) {
                drawPreviewFrameOverlay();
            }
            previewRenderContext.pushScissor(previewViewport);
            try {
                cameraMarkerOverlay.render(session.getSceneModel(), previewScene.getCamera(), previewViewport);
            } finally {
                previewRenderContext.popScissor();
            }
            SceneEditorElementModel selectedPointElement = getSelectedPointHandleElement();
            if (selectedPointElement != null) {
                previewRenderContext.pushScissor(previewViewport);
                try {
                    handleOverlay.render(selectedPointElement, previewScene.getCamera(), previewViewport);
                } finally {
                    previewRenderContext.popScissor();
                }
            }
        } else {
            int centerX = screenLayout.previewInteraction()
                .x()
                + screenLayout.previewInteraction()
                    .width() / 2;
            int centerY = screenLayout.previewInteraction()
                .y()
                + screenLayout.previewInteraction()
                    .height() / 2
                - 12;
            this.drawCenteredString(
                this.fontRendererObj,
                GuidebookText.SceneEditorPreviewPlaceholder.text(),
                centerX,
                centerY,
                PANEL_MUTED_TEXT);
        }
    }

    private void drawPreviewStructureSource(LytGuidebookScene scene) {
        String structureSource = session.getSceneModel()
            .getStructureSource();
        if (structureSource == null || structureSource.isEmpty()) {
            return;
        }
        int pathX = screenLayout.previewInteraction()
            .x() + 8;
        int maxPathWidth = Math.max(
            0,
            screenLayout.previewInteraction()
                .width() - 16);
        if (maxPathWidth <= 0) {
            return;
        }
        int bottomControlsHeight = scene.getBottomControlAreaHeight();
        int pathY = previewBoxY + previewBoxHeight - bottomControlsHeight - this.fontRendererObj.FONT_HEIGHT - 6;
        if (pathY < previewBoxY + 6) {
            pathY = previewBoxY + 6;
        }
        String visiblePath = this.fontRendererObj.trimStringToWidth(structureSource, maxPathWidth);
        this.drawString(this.fontRendererObj, visiblePath, pathX, pathY, PANEL_SUBTLE_TEXT);
    }

    private void drawPreviewSceneHoverTooltip(int mouseX, int mouseY) {
        if (previewScene == null || !previewScene.containsSceneInteractiveTarget(mouseX, mouseY)) {
            return;
        }

        for (var annotation : previewScene.getAnnotations()) {
            if (annotation.isHovered() && annotation.getTooltip() != null) {
                renderPreviewGuideTooltip(annotation.getTooltip(), mouseX, mouseY);
                return;
            }
        }

        int[] hoveredHatch = previewScene.getHoveredStructureLibHatch();
        if (hoveredHatch != null) {
            String name = GuideBlockDisplayResolver.resolveDisplayName(
                previewScene.getLevel(),
                hoveredHatch[0],
                hoveredHatch[1],
                hoveredHatch[2],
                previewScene.getHoveredBlockHitResult());
            if (name != null) {
                GuideTooltip structureLibTooltip = previewScene
                    .createStructureLibTooltipForHoveredBlock(name, isShiftKeyDown());
                if (structureLibTooltip != null) {
                    renderPreviewGuideTooltip(structureLibTooltip, mouseX, mouseY);
                    return;
                }
                drawPreviewTooltipText(name, mouseX, mouseY);
                return;
            }
        }

        int[] hoveredBlock = previewScene.getHoveredBlock();
        var hoveredEntity = previewScene.getHoveredEntity();
        if (hoveredEntity != null) {
            String entityName = GuideEntityDisplayResolver.resolveDisplayName(hoveredEntity);
            if (entityName != null) {
                drawPreviewTooltipText(entityName, mouseX, mouseY);
                return;
            }
        }
        if (hoveredBlock == null) {
            return;
        }

        String name = GuideBlockDisplayResolver.resolveDisplayName(
            previewScene.getLevel(),
            hoveredBlock[0],
            hoveredBlock[1],
            hoveredBlock[2],
            previewScene.getHoveredBlockHitResult());
        if (name != null) {
            GuideTooltip structureLibTooltip = previewScene
                .createStructureLibTooltipForHoveredBlock(name, isShiftKeyDown());
            if (structureLibTooltip != null) {
                renderPreviewGuideTooltip(structureLibTooltip, mouseX, mouseY);
                return;
            }
            drawPreviewTooltipText(name, mouseX, mouseY);
        }
    }

    private void renderPreviewGuideTooltip(GuideTooltip tooltip, int mouseX, int mouseY) {
        if (tooltip instanceof ItemTooltip itemTooltip) {
            renderPreviewItemTooltip(itemTooltip, mouseX, mouseY);
            return;
        }

        if (tooltip instanceof TextTooltip textTooltip) {
            drawPreviewTooltipText(textTooltip.getText(), mouseX, mouseY);
            return;
        }

        if (tooltip instanceof ContentTooltip contentTooltip) {
            drawPreviewContentTooltip(contentTooltip, mouseX, mouseY);
        }
    }

    private void renderPreviewItemTooltip(ItemTooltip tooltip, int mouseX, int mouseY) {
        ItemStack stack = tooltip.getStack();
        if (stack == null || stack.stackSize == 0) {
            return;
        }

        if (GuideItemTooltipRenderSupport.shouldUseVanillaRenderer(tooltip)) {
            renderToolTip(stack, mouseX, mouseY);
            return;
        }

        List<String> lines = GuideItemTooltipLines.build(tooltip, mc);
        FontRenderer font = GuideItemTooltipRenderSupport.resolveFont(stack, mc.fontRenderer);
        drawHoveringText(lines, mouseX, mouseY, font);
    }

    private void drawPreviewContentTooltip(ContentTooltip tooltip, int mouseX, int mouseY) {
        int pad = 4;
        int maxWidth = Math.max(80, (this.width * 4) / 5);
        var box = tooltip.layout(maxWidth);
        int tooltipWidth = box.width();
        int tooltipHeight = box.height();
        int tooltipX = mouseX + 12;
        int tooltipY = mouseY - 12;
        if (tooltipX + tooltipWidth + pad > this.width) {
            tooltipX = mouseX - tooltipWidth - 12;
        }
        if (tooltipX - pad < 0) {
            tooltipX = pad;
        }
        if (tooltipY + tooltipHeight + pad > this.height) {
            tooltipY = this.height - tooltipHeight - pad;
        }
        if (tooltipY - pad < 0) {
            tooltipY = pad;
        }

        drawRect(
            tooltipX - pad,
            tooltipY - pad,
            tooltipX + tooltipWidth + pad,
            tooltipY + tooltipHeight + pad,
            0xF0100010);
        drawBorder(tooltipX - pad, tooltipY - pad, tooltipWidth + pad * 2, tooltipHeight + pad * 2, 0xFF5000FF);

        previewTooltipRenderContext.setLightDarkMode(LightDarkMode.LIGHT_MODE);
        previewTooltipRenderContext.setViewport(new LytRect(0, 0, tooltipWidth, tooltipHeight));
        previewTooltipRenderContext.setScreenHeight(this.height);
        previewTooltipRenderContext.setDocumentOrigin(tooltipX, tooltipY);
        previewTooltipRenderContext.setScrollOffsetY(0);

        GL11.glPushMatrix();
        GL11.glTranslatef(tooltipX, tooltipY, 300f);
        try {
            tooltip.getContent()
                .render(previewTooltipRenderContext);
        } catch (Throwable ignored) {
            // Keep preview hover robust even if rich tooltip content fails.
        } finally {
            GL11.glPopMatrix();
        }
    }

    private void drawPreviewTooltipText(String text, int mouseX, int mouseY) {
        FontRenderer fontRenderer = mc.fontRenderer;
        String normalizedText = text.indexOf('\\') >= 0 ? text.replace("\\n", "\n") : text;
        int pad = 3;
        int hardMaxWidth = Math.max(40, this.width - 24);
        int preferredWrapWidth = Math.max(80, this.width / 2);
        int wrapWidth = Math.min(hardMaxWidth, preferredWrapWidth);
        List<String> lines = new ArrayList<>();
        for (String rawLine : normalizedText.split("\n", -1)) {
            if (rawLine.isEmpty()) {
                lines.add("");
                continue;
            }
            if (fontRenderer.getStringWidth(rawLine) <= wrapWidth) {
                lines.add(rawLine);
                continue;
            }
            lines.addAll(fontRenderer.listFormattedStringToWidth(rawLine, wrapWidth));
        }

        int tooltipWidth = 0;
        for (String line : lines) {
            tooltipWidth = Math.max(tooltipWidth, fontRenderer.getStringWidth(line));
        }
        int tooltipHeight = lines.size() * (fontRenderer.FONT_HEIGHT + 1) - 1;
        int tooltipX = mouseX + 12;
        int tooltipY = mouseY - 12;
        if (tooltipX + tooltipWidth + pad > this.width) {
            tooltipX = mouseX - tooltipWidth - 12;
        }
        if (tooltipX - pad < 0) {
            tooltipX = pad;
        }
        if (tooltipY + tooltipHeight + pad > this.height) {
            tooltipY = this.height - tooltipHeight - pad;
        }
        if (tooltipY - pad < 0) {
            tooltipY = pad;
        }

        drawRect(
            tooltipX - pad,
            tooltipY - pad,
            tooltipX + tooltipWidth + pad,
            tooltipY + tooltipHeight + pad,
            0xF0100010);
        drawBorder(tooltipX - pad, tooltipY - pad, tooltipWidth + pad * 2, tooltipHeight + pad * 2, 0xFF5000FF);
        int lineY = tooltipY;
        for (String line : lines) {
            fontRenderer.drawStringWithShadow(line, tooltipX, lineY, 0xFFFFFFFF);
            lineY += fontRenderer.FONT_HEIGHT + 1;
        }
    }

    private void drawPreviewFrameOverlay() {
        if (previewScene == null) {
            return;
        }
        SceneEditorPreviewFrameOverlayLayout.Layout overlayLayout = SceneEditorPreviewFrameOverlayLayout.resolve(
            previewScene.getScreenRect(),
            session.getSceneModel()
                .getPreviewWidth(),
            session.getSceneModel()
                .getPreviewHeight());
        LytRect frameRect = overlayLayout.frameRect();
        if (frameRect.isEmpty()) {
            return;
        }
        drawRect(frameRect.x(), frameRect.y(), frameRect.right(), frameRect.y() + 1, 0xFF00CAF2);
        drawRect(frameRect.x(), frameRect.bottom() - 1, frameRect.right(), frameRect.bottom(), 0xFF00CAF2);
        drawRect(frameRect.x(), frameRect.y(), frameRect.x() + 1, frameRect.bottom(), 0xFF00CAF2);
        drawRect(frameRect.right() - 1, frameRect.y(), frameRect.right(), frameRect.bottom(), 0xFF00CAF2);
        this.drawString(
            this.fontRendererObj,
            session.getSceneModel()
                .getPreviewWidth() + " x "
                + session.getSceneModel()
                    .getPreviewHeight(),
            overlayLayout.labelX(),
            overlayLayout.labelY(),
            PANEL_HEADER_COLOR);
    }

    private void drawRightPanel(int mouseX, int mouseY) {
        this.drawString(
            this.fontRendererObj,
            GuidebookText.SceneEditorSettingsPanel.text(),
            rightPanelX + 10,
            rightPanelY + SceneEditorScreenLayout.PANEL_HEADER_Y - 6,
            PANEL_HEADER_COLOR);
        drawRect(
            settingsBoxX,
            settingsBoxY,
            settingsBoxX + settingsBoxWidth,
            settingsBoxY + settingsBoxHeight,
            PANEL_INNER_COLOR);
        drawBorder(settingsBoxX, settingsBoxY, settingsBoxWidth, settingsBoxHeight, 0xFF464A50);

        drawSettingsTabs(mouseX, mouseY);
        for (NumericParameterRow row : getVisibleParameterRows()) {
            row.draw(mouseX, mouseY);
        }
        drawInteractiveToggle();
        drawElementPanel(mouseX, mouseY);
    }

    private void drawPanel(int x, int y, int width, int height) {
        drawRect(x, y, x + width, y + height, PANEL_COLOR);
        drawBorder(x, y, width, height, PANEL_BORDER_COLOR);
    }

    private void drawPanelHeader(int x, int y, String title) {
        this.drawString(
            this.fontRendererObj,
            title,
            x + 10,
            y + SceneEditorScreenLayout.PANEL_HEADER_Y,
            PANEL_HEADER_COLOR);
    }

    private void drawMarkdownToggle(int mouseX, int mouseY) {
        LytRect toggleBounds = screenLayout.markdownToggle();
        boolean hovered = toggleBounds.contains(mouseX, mouseY);
        int backgroundColor = hovered ? 0xC824303A : 0xA014161A;
        drawRect(toggleBounds.x(), toggleBounds.y(), toggleBounds.right(), toggleBounds.bottom(), backgroundColor);
        drawBorder(
            toggleBounds.x(),
            toggleBounds.y(),
            toggleBounds.width(),
            toggleBounds.height(),
            hovered ? 0xFF00CAF2 : INPUT_BORDER_COLOR);
        String arrow = markdownPanelState.isExpanded() ? "<" : ">";
        int arrowX = toggleBounds.x() + (toggleBounds.width() - this.fontRendererObj.getStringWidth(arrow)) / 2;
        int arrowY = toggleBounds.y() + toggleBounds.height() / 2 - 4;
        this.drawString(this.fontRendererObj, arrow, arrowX, arrowY, PANEL_HEADER_COLOR);
    }

    private void drawSettingsTabs(int mouseX, int mouseY) {
        int tabY = getSettingsTabY();
        for (SceneEditorSettingsTab tab : SceneEditorSettingsTab.values()) {
            int tabX = getSettingsTabX(tab);
            int tabWidth = getSettingsTabWidth(tab);
            boolean active = activeSettingsTab == tab;
            boolean hovered = mouseX >= tabX && mouseX < tabX + tabWidth
                && mouseY >= tabY
                && mouseY < tabY + SETTINGS_TAB_HEIGHT;
            int backgroundColor = active ? SETTINGS_TAB_ACTIVE_COLOR
                : hovered ? SETTINGS_TAB_HOVER_COLOR : SETTINGS_TAB_INACTIVE_COLOR;
            drawRect(tabX, tabY, tabX + tabWidth, tabY + SETTINGS_TAB_HEIGHT, backgroundColor);
            drawBorder(tabX, tabY, tabWidth, SETTINGS_TAB_HEIGHT, active ? 0xFF00CAF2 : 0xFF3E434A);
            String label = this.fontRendererObj.trimStringToWidth(
                tab.getTextKey()
                    .text(),
                Math.max(0, tabWidth - 8));
            int textX = tabX + Math.max(3, (tabWidth - this.fontRendererObj.getStringWidth(label)) / 2);
            this.drawString(
                this.fontRendererObj,
                label,
                textX,
                tabY + 5,
                active ? PANEL_HEADER_COLOR : PANEL_MUTED_TEXT);
        }
    }

    private void drawTiledBackground() {
        drawRect(0, 0, this.width, this.height, 0x34101018);
        mc.getTextureManager()
            .bindTexture(BG_TEXTURE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1f, 1f, 1f, 0.7f);
        float tile = 16f;
        float uMax = this.width / tile;
        float vMax = this.height / tile;
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.addVertexWithUV(0, this.height, 0, 0, vMax);
        tess.addVertexWithUV(this.width, this.height, 0, uMax, vMax);
        tess.addVertexWithUV(this.width, 0, 0, uMax, 0);
        tess.addVertexWithUV(0, 0, 0, 0, 0);
        tess.draw();
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }

    private void drawBorder(int x, int y, int width, int height, int color) {
        drawRect(x, y, x + width, y + 1, color);
        drawRect(x, y + height - 1, x + width, y + height, color);
        drawRect(x, y, x + 1, y + height, color);
        drawRect(x + width - 1, y, x + width, y + height, color);
    }

    private void drawCompactTextFieldValue(GuiTextField inputField, String text) {
        if (inputField.isFocused()) {
            inputField.drawTextBox();
            return;
        }
        if (text == null || text.isEmpty()) {
            return;
        }
        String visibleText = this.fontRendererObj.trimStringToWidth(text, Math.max(0, inputField.width));
        this.fontRendererObj.drawString(visibleText, inputField.xPosition, inputField.yPosition, 0xF0F0F0);
    }

    private void requestCloseEditor() {
        if (!commitPendingEditorsForClose()) {
            return;
        }
        closeElementContextMenu();
        if (!session.isDirty()) {
            closeEditorNow();
            return;
        }
        closeConfirmDialogOpen = true;
        closeConfirmErrorText = null;
    }

    private boolean commitPendingEditorsForClose() {
        if (markdownTextArea != null && markdownTextArea.isFocused()) {
            if (!commitMarkdownEditor()) {
                return false;
            }
            markdownTextArea.setFocused(false);
        }
        if (focusedParameterRow != null) {
            if (!focusedParameterRow.commitDraft()) {
                return false;
            }
            focusedParameterRow.setFocused(false);
            focusedParameterRow = null;
        }
        if (expandedElementEditor != null
            && !expandedElementEditor.commitFocusedDraft(Integer.MIN_VALUE, Integer.MIN_VALUE)) {
            return false;
        }
        if (!commitScreenshotScaleDraft()) {
            return false;
        }
        return true;
    }

    private void closeEditorNow() {
        closeConfirmDialogOpen = false;
        closeConfirmErrorText = null;
        this.mc.displayGuiScreen(null);
    }

    private void attemptSaveAndClose() {
        if (performSave()) {
            closeEditorNow();
        }
    }

    private void attemptSaveWithoutClose() {
        performSave();
    }

    private boolean performSave() {
        if (!commitPendingEditorsForClose()) {
            return false;
        }
        SceneEditorSaveService.SaveResult result = saveService.save(session, this.mc.thePlayer);
        if (result.isSuccess()) {
            if (textSyncController.hasValidationError() && this.mc.thePlayer != null) {
                this.mc.thePlayer.addChatMessage(
                    new ChatComponentTranslation(GuidebookText.SceneEditorMarkdownInvalidSaveHint.getTranslationKey()));
            }
            closeConfirmErrorText = null;
            return true;
        }
        closeConfirmErrorText = GuidebookText.SceneEditorSaveFailure.text(extractErrorMessage(result.getError()));
        return false;
    }

    private String extractErrorMessage(@Nullable Throwable throwable) {
        if (throwable == null) {
            return "unknown";
        }
        String message = throwable.getMessage();
        return message != null && !message.isEmpty() ? message
            : throwable.getClass()
                .getSimpleName();
    }

    private void exportPreviewScreenshot() {
        if (!commitScreenshotScaleDraft()) {
            return;
        }
        ensurePreviewScene();
        if (previewScene == null) {
            return;
        }
        SceneEditorScreenshotExportService.ExportResult result = screenshotExportService.export(
            previewScene,
            screenshotMenuController.getFormat(),
            screenshotMenuController.getScale(),
            session.getSceneModel()
                .getPreviewWidth(),
            session.getSceneModel()
                .getPreviewHeight());
        if (mc == null || mc.thePlayer == null) {
            return;
        }
        if (result.isSuccess()) {
            mc.thePlayer.addChatMessage(
                new ChatComponentTranslation(
                    GuidebookText.SceneEditorScreenshotSuccess.getTranslationKey(),
                    result.getSavedPath()
                        .toAbsolutePath()
                        .normalize()
                        .toString()));
        } else {
            mc.thePlayer.addChatMessage(
                new ChatComponentTranslation(
                    GuidebookText.SceneEditorScreenshotFailure.getTranslationKey(),
                    extractErrorMessage(result.getError())));
        }
    }

    private void importStructureFromFile() {
        if (pendingStructureImport != null) {
            return;
        }
        pendingStructureImport = structureImportService
            .chooseAndImportAsync(GuidebookText.SceneEditorImportStructure.text());
        if (importStructureButton != null) {
            importStructureButton.enabled = false;
        }
    }

    private void processPendingStructureImport() {
        if (pendingStructureImport == null || !pendingStructureImport.isDone()) {
            return;
        }
        CompletableFuture<SceneEditorStructureImportService.ImportResult> future = pendingStructureImport;
        pendingStructureImport = null;
        if (importStructureButton != null) {
            importStructureButton.enabled = true;
        }
        try {
            SceneEditorStructureImportService.ImportResult importResult = future.join();
            if (importResult == null) {
                return;
            }
            applyImportedStructure(importResult);
        } catch (CompletionException e) {
            handleImportStructureFailure(e.getCause() != null ? e.getCause() : e);
        } catch (Exception e) {
            handleImportStructureFailure(e);
        }
    }

    private void applyImportedStructure(SceneEditorStructureImportService.ImportResult importResult) {
        session.getSceneModel()
            .setStructureSource(importResult.getStructureSource());
        session.setImportedStructureSnbt(importResult.getStructureText());
        String serialized = markdownCodec.serialize(session.getSceneModel());
        session.setRawText(serialized);
        onDocumentOnlyApplied();
        previewDirty = true;
        preservePreviewCameraOnNextRebuild = false;
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(
                new ChatComponentTranslation(
                    GuidebookText.SceneEditorImportSuccess.getTranslationKey(),
                    importResult.getDisplayPath()));
        }
    }

    private void handleImportStructureFailure(Throwable throwable) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(
                new ChatComponentTranslation(
                    GuidebookText.SceneEditorImportFailure.getTranslationKey(),
                    extractErrorMessage(throwable)));
        }
    }

    private boolean hasFocusedTextInput() {
        return markdownTextArea != null && markdownTextArea.isFocused() || focusedParameterRow != null
            || screenshotScaleField != null && screenshotScaleField.isFocused()
            || expandedElementEditor != null && expandedElementEditor.hasFocusedInput();
    }

    private SceneEditorSnapModes currentSnapModes() {
        return new SceneEditorSnapModes(
            ModConfig.ui.sceneEditorSnapPointEnabled,
            ModConfig.ui.sceneEditorSnapLineEnabled,
            ModConfig.ui.sceneEditorSnapFaceEnabled,
            ModConfig.ui.sceneEditorSnapCenterEnabled);
    }

    private void drawCloseConfirmDialog(int mouseX, int mouseY) {
        drawRect(0, 0, this.width, this.height, CLOSE_DIALOG_OVERLAY_COLOR);
        LytRect dialogBounds = getCloseDialogBounds();
        drawRect(dialogBounds.x(), dialogBounds.y(), dialogBounds.right(), dialogBounds.bottom(), CLOSE_DIALOG_COLOR);
        drawBorder(dialogBounds.x(), dialogBounds.y(), dialogBounds.width(), dialogBounds.height(), PANEL_BORDER_COLOR);

        int x = dialogBounds.x() + dialogBounds.width() / 2;
        this.drawCenteredString(
            this.fontRendererObj,
            GuidebookText.SceneEditorClose.text(),
            x,
            dialogBounds.y() + 12,
            PANEL_HEADER_COLOR);
        this.drawCenteredString(
            this.fontRendererObj,
            GuidebookText.SceneEditorClosePrompt.text(),
            x,
            dialogBounds.y() + 30,
            PANEL_MUTED_TEXT);

        if (closeConfirmErrorText != null) {
            this.drawCenteredString(
                this.fontRendererObj,
                closeConfirmErrorText,
                x,
                dialogBounds.y() + 46,
                INPUT_ERROR_BORDER_COLOR);
        }

        drawCloseConfirmButton(CloseConfirmAction.SAVE, mouseX, mouseY);
        drawCloseConfirmButton(CloseConfirmAction.DISCARD, mouseX, mouseY);
        drawCloseConfirmButton(CloseConfirmAction.CANCEL, mouseX, mouseY);
    }

    private void drawCloseConfirmButton(CloseConfirmAction action, int mouseX, int mouseY) {
        LytRect bounds = getCloseConfirmButtonBounds(action);
        boolean hovered = bounds.contains(mouseX, mouseY);
        drawRect(
            bounds.x(),
            bounds.y(),
            bounds.right(),
            bounds.bottom(),
            hovered ? CLOSE_DIALOG_HOVER : ELEMENT_ROW_BACKGROUND);
        drawBorder(bounds.x(), bounds.y(), bounds.width(), bounds.height(), hovered ? 0xFF00CAF2 : INPUT_BORDER_COLOR);
        this.drawCenteredString(
            this.fontRendererObj,
            action.text()
                .text(),
            bounds.x() + bounds.width() / 2,
            bounds.y() + 6,
            PANEL_HEADER_COLOR);
    }

    private void handleCloseConfirmDialogClick(int mouseX, int mouseY, int button) {
        if (button != 0) {
            return;
        }
        CloseConfirmAction action = getCloseConfirmActionAt(mouseX, mouseY);
        if (action == null) {
            return;
        }
        switch (action) {
            case SAVE:
                attemptSaveAndClose();
                break;
            case DISCARD:
                closeEditorNow();
                break;
            case CANCEL:
                closeConfirmDialogOpen = false;
                closeConfirmErrorText = null;
                break;
            default:
                break;
        }
    }

    @Nullable
    private CloseConfirmAction getCloseConfirmActionAt(int mouseX, int mouseY) {
        for (CloseConfirmAction action : CloseConfirmAction.values()) {
            if (getCloseConfirmButtonBounds(action).contains(mouseX, mouseY)) {
                return action;
            }
        }
        return null;
    }

    private LytRect getCloseDialogBounds() {
        return new LytRect(
            (this.width - CLOSE_DIALOG_WIDTH) / 2,
            (this.height - CLOSE_DIALOG_HEIGHT) / 2,
            CLOSE_DIALOG_WIDTH,
            CLOSE_DIALOG_HEIGHT);
    }

    private LytRect getCloseConfirmButtonBounds(CloseConfirmAction action) {
        LytRect dialogBounds = getCloseDialogBounds();
        int totalWidth = CLOSE_DIALOG_BUTTON_WIDTH * 3 + CLOSE_DIALOG_BUTTON_GAP * 2;
        int startX = dialogBounds.x() + (dialogBounds.width() - totalWidth) / 2;
        int y = dialogBounds.bottom() - CLOSE_DIALOG_BUTTON_HEIGHT - 14;
        int index = action.ordinal();
        return new LytRect(
            startX + index * (CLOSE_DIALOG_BUTTON_WIDTH + CLOSE_DIALOG_BUTTON_GAP),
            y,
            CLOSE_DIALOG_BUTTON_WIDTH,
            CLOSE_DIALOG_BUTTON_HEIGHT);
    }

    private boolean commitMarkdownEditor() {
        if (markdownTextArea == null) {
            return true;
        }
        clearPendingMarkdownLiveSync();
        textSyncController.setDraftText(markdownTextArea.getText());
        if (!textSyncController.commitDraftText(MARKDOWN_UNDO_MERGE_KEY, captureCurrentUiUndoState())) {
            return true;
        }
        markdownTextArea.setText(session.getRawText());
        syncParameterRowsFromModel();
        session.getSelectionState()
            .setSelectedElementId(null);
        expandedElementId = null;
        previewDirty = true;
        return true;
    }

    private void ensurePreviewScene() {
        if (previewDirty || previewScene == null) {
            rebuildPreviewScene(preservePreviewCameraOnNextRebuild);
        }
    }

    private void rebuildPreviewScene(boolean preserveCurrentView) {
        SavedCameraSettings savedCamera = null;
        boolean annotationsVisible = previewScene == null || previewScene.isAnnotationsVisible();
        Integer visibleLayerOverride = previewScene != null && previewScene.hasVisibleLayerData()
            ? Integer.valueOf(previewScene.getCurrentVisibleLayer())
            : previewVisibleLayerOverride;
        if (preserveCurrentView && previewScene != null) {
            savedCamera = previewScene.getCamera()
                .save();
        }
        previewScene = previewBridge.buildScene(session, previewStructureLibSelectionOverride);
        bindPreviewScene(previewScene);
        previewScene.setAnnotationsVisible(annotationsVisible);
        if (visibleLayerOverride != null) {
            previewScene.setVisibleLayerSilently(visibleLayerOverride);
        }
        if (savedCamera != null) {
            previewScene.getCamera()
                .restore(savedCamera);
        }
        updatePreviewVisibleLayerOverride(visibleLayerOverride);
        updatePreviewStructureLibSelectionOverride();
        previewDirty = false;
        preservePreviewCameraOnNextRebuild = false;
    }

    private void bindPreviewScene(@Nullable LytGuidebookScene scene) {
        if (scene != null) {
            scene.setStructureLibSelectionChangeListener(this::handlePreviewSceneStructureLibSelectionChanged);
        }
    }

    private void updatePreviewStructureLibSelectionOverride() {
        previewStructureLibSelectionOverride = previewScene != null && previewScene.hasStructureLibSceneMetadata()
            ? previewScene.getStructureLibPreviewSelection()
            : null;
    }

    private void updatePreviewVisibleLayerOverride(@Nullable Integer fallbackValue) {
        previewVisibleLayerOverride = previewScene != null && previewScene.hasVisibleLayerData()
            ? Integer.valueOf(previewScene.getCurrentVisibleLayer())
            : fallbackValue;
    }

    private void handlePreviewSceneStructureLibSelectionChanged(StructureLibPreviewSelection selection) {
        if (previewScene == null) {
            return;
        }
        SavedCameraSettings savedCamera = previewScene.getCamera()
            .save();
        boolean annotationsVisible = previewScene.isAnnotationsVisible();
        Integer visibleLayerOverride = previewScene.hasVisibleLayerData()
            ? Integer.valueOf(previewScene.getCurrentVisibleLayer())
            : previewVisibleLayerOverride;
        previewStructureLibSelectionOverride = selection;
        previewBridge.rebuildScene(session, previewScene, previewStructureLibSelectionOverride);
        bindPreviewScene(previewScene);
        previewScene.setAnnotationsVisible(annotationsVisible);
        if (visibleLayerOverride != null) {
            previewScene.setVisibleLayerSilently(visibleLayerOverride);
        }
        previewScene.getCamera()
            .restore(savedCamera);
        updatePreviewVisibleLayerOverride(visibleLayerOverride);
        updatePreviewStructureLibSelectionOverride();
        previewDirty = false;
        preservePreviewCameraOnNextRebuild = false;
    }

    private void initParameterRows() {
        if (!numericParameterRows.isEmpty()) {
            return;
        }

        numericParameterRows.add(
            createDecimalParameterRow(
                GuidebookText.SceneEditorCameraX,
                session.getSceneModel()
                    .getCenterX(),
                -256f,
                256f,
                parameterController::setCenterX,
                () -> session.getSceneModel()
                    .getCenterX()));
        numericParameterRows.add(
            createDecimalParameterRow(
                GuidebookText.SceneEditorCameraY,
                session.getSceneModel()
                    .getCenterY(),
                -256f,
                256f,
                parameterController::setCenterY,
                () -> session.getSceneModel()
                    .getCenterY()));
        numericParameterRows.add(
            createDecimalParameterRow(
                GuidebookText.SceneEditorCameraZ,
                session.getSceneModel()
                    .getCenterZ(),
                -256f,
                256f,
                parameterController::setCenterZ,
                () -> session.getSceneModel()
                    .getCenterZ()));
        numericParameterRows.add(
            createDecimalParameterRow(
                GuidebookText.SceneEditorRotateX,
                session.getSceneModel()
                    .getRotationX(),
                -180f,
                180f,
                parameterController::setRotationX,
                () -> session.getSceneModel()
                    .getRotationX()));
        numericParameterRows.add(
            createDecimalParameterRow(
                GuidebookText.SceneEditorRotateY,
                session.getSceneModel()
                    .getRotationY(),
                -180f,
                180f,
                parameterController::setRotationY,
                () -> session.getSceneModel()
                    .getRotationY()));
        numericParameterRows.add(
            createDecimalParameterRow(
                GuidebookText.SceneEditorRotateZ,
                session.getSceneModel()
                    .getRotationZ(),
                -180f,
                180f,
                parameterController::setRotationZ,
                () -> session.getSceneModel()
                    .getRotationZ()));
        numericParameterRows.add(
            createDecimalParameterRow(
                GuidebookText.SceneEditorZoom,
                session.getSceneModel()
                    .getZoom(),
                0.1f,
                8f,
                parameterController::setZoom,
                () -> session.getSceneModel()
                    .getZoom()));
        numericParameterRows.add(
            createIntegerParameterRow(
                GuidebookText.SceneEditorPreviewWidth,
                session.getSceneModel()
                    .getPreviewWidth(),
                16,
                512,
                parameterController::setPreviewWidth,
                () -> session.getSceneModel()
                    .getPreviewWidth()));
        numericParameterRows.add(
            createIntegerParameterRow(
                GuidebookText.SceneEditorPreviewHeight,
                session.getSceneModel()
                    .getPreviewHeight(),
                16,
                512,
                parameterController::setPreviewHeight,
                () -> session.getSceneModel()
                    .getPreviewHeight()));
    }

    private NumericParameterRow createDecimalParameterRow(GuidebookText label, float initialValue, float minValue,
        float maxValue, FloatValueSetter valueSetter, FloatModelValueProvider modelValueProvider) {
        SceneEditorNumericFieldController controller = SceneEditorNumericFieldController
            .decimal(initialValue, minValue, maxValue, value -> {
                valueSetter.setValue(value);
                onParameterValueApplied();
            });
        return new NumericParameterRow(label, controller, minValue, maxValue, modelValueProvider);
    }

    private NumericParameterRow createIntegerParameterRow(GuidebookText label, int initialValue, int minValue,
        int maxValue, IntValueSetter valueSetter, FloatModelValueProvider modelValueProvider) {
        SceneEditorNumericFieldController controller = SceneEditorNumericFieldController
            .integer(initialValue, minValue, maxValue, value -> {
                valueSetter.setValue(Math.round(value));
                onParameterValueApplied();
            });
        return new NumericParameterRow(label, controller, minValue, maxValue, modelValueProvider);
    }

    private void layoutParameterRows() {
        settingsBoxX = screenLayout.rightContent()
            .x();
        settingsBoxY = screenLayout.rightContent()
            .y() - 5;
        settingsBoxWidth = screenLayout.rightContent()
            .width();

        int contentLeft = settingsBoxX + SETTINGS_BOX_PADDING;
        int contentRight = settingsBoxX + settingsBoxWidth - SETTINGS_BOX_PADDING;
        int inputX = contentLeft + PARAMETER_LABEL_WIDTH + PARAMETER_GAP;
        int sliderX = inputX + PARAMETER_INPUT_WIDTH + PARAMETER_GAP;
        int sliderWidth = Math.max(24, contentRight - sliderX);
        int rowY = getSettingsContentY();

        for (NumericParameterRow row : getVisibleParameterRows()) {
            row.setBounds(contentLeft, rowY, inputX, PARAMETER_INPUT_WIDTH, sliderX, sliderWidth);
            rowY += PARAMETER_ROW_HEIGHT;
        }

        interactiveLabelX = contentLeft;
        interactiveToggleX = contentLeft + PARAMETER_LABEL_WIDTH + 2;
        interactiveToggleY = rowY + 2;
        previewFrameButtonX = contentRight - PREVIEW_FRAME_BUTTON_WIDTH;
        previewFrameButtonY = interactiveToggleY - 1;
        settingsBoxHeight = interactiveToggleY + INTERACTIVE_CHECKBOX_SIZE + 8 - settingsBoxY;

        elementsBoxX = settingsBoxX;
        elementsBoxY = settingsBoxY + settingsBoxHeight + 16;
        elementsBoxWidth = settingsBoxWidth;
        elementsBoxHeight = Math.max(
            80,
            screenLayout.rightPanel()
                .bottom() - elementsBoxY
                - 10);
    }

    private void syncParameterRowsFromModel() {
        for (NumericParameterRow row : numericParameterRows) {
            row.syncFromModel();
        }
    }

    private void syncMarkdownTextFromAppliedModel() {
        textSyncController.replaceDraftFromAppliedModel();
        if (markdownTextArea != null) {
            markdownTextArea.setText(session.getRawText());
        }
        refreshMarkdownHighlightFromSelectedElement();
    }

    private void onParameterValueApplied() {
        syncMarkdownTextFromAppliedModel();
        String mergeKey = resolveAppliedUndoMergeKey(currentParameterUndoMergeKey());
        textSyncController
            .recordAppliedSnapshot(mergeKey, captureCurrentUiUndoState(), resolveAppliedUndoKeepOpen(mergeKey != null));
        if (previewScene != null && !previewDirty) {
            previewCameraController.applyModelCamera(previewScene, session.getSceneModel());
        } else {
            previewDirty = true;
        }
        preservePreviewCameraOnNextRebuild = false;
    }

    private void onDocumentOnlyApplied() {
        clearPendingMarkdownLiveSync();
        syncMarkdownTextFromAppliedModel();
        textSyncController.recordAppliedSnapshot(null, captureCurrentUiUndoState(), false);
    }

    private boolean commitFocusedParameterRow(int mouseX, int mouseY) {
        if (focusedParameterRow == null || focusedParameterRow.containsInput(mouseX, mouseY)) {
            return true;
        }
        if (!focusedParameterRow.commitDraft()) {
            return false;
        }
        focusedParameterRow.setFocused(false);
        focusedParameterRow = null;
        return true;
    }

    private boolean handleParameterMouseClick(int mouseX, int mouseY, int button) {
        for (NumericParameterRow row : getVisibleParameterRows()) {
            if (row.containsInput(mouseX, mouseY)) {
                if (button != 0 && button != 1) {
                    return true;
                }
                setFocusedParameterRow(row);
                row.mouseClicked(mouseX, mouseY, button);
                return true;
            }
            if (button == 0 && row.containsSlider(mouseX, mouseY)) {
                clearFocusedParameterRow();
                activeSliderRow = row;
                row.applySliderAt(mouseX);
                return true;
            }
        }
        return false;
    }

    private List<NumericParameterRow> getVisibleParameterRows() {
        return activeSettingsTab.visibleRows(numericParameterRows);
    }

    private int getSettingsTabY() {
        return settingsBoxY + SETTINGS_BOX_PADDING;
    }

    private int getSettingsTabWidth() {
        return getSettingsTabWidth(SceneEditorSettingsTab.CAMERA);
    }

    private int getSettingsTabWidth(SceneEditorSettingsTab tab) {
        int cameraWidth = 50;
        int rotationWidth = 42;
        int previewWidth = 42;
        int available = Math.max(
            0,
            settingsBoxWidth - SETTINGS_BOX_PADDING * 2
                - SETTINGS_TAB_GAP * (SceneEditorSettingsTab.values().length - 1));
        int totalWidth = cameraWidth + rotationWidth + previewWidth;
        int overflow = Math.max(0, totalWidth - available);
        if (overflow > 0) {
            int shrink = Math.min(overflow, 6);
            rotationWidth -= shrink;
            overflow -= shrink;
        }
        if (overflow > 0) {
            int shrink = Math.min(overflow, 6);
            previewWidth -= shrink;
            overflow -= shrink;
        }
        if (overflow > 0) {
            cameraWidth = Math.max(42, cameraWidth - overflow);
        }
        return switch (tab) {
            case ROTATION -> rotationWidth;
            case PREVIEW -> previewWidth;
            default -> cameraWidth;
        };
    }

    private int getSettingsTabX(SceneEditorSettingsTab tab) {
        int x = settingsBoxX + SETTINGS_BOX_PADDING;
        for (SceneEditorSettingsTab current : SceneEditorSettingsTab.values()) {
            if (current == tab) {
                return x;
            }
            x += getSettingsTabWidth(current) + SETTINGS_TAB_GAP;
        }
        return x;
    }

    private int getSettingsContentY() {
        return getSettingsTabY() + SETTINGS_TAB_HEIGHT + 8;
    }

    @Nullable
    private SceneEditorSettingsTab getSettingsTabAt(int mouseX, int mouseY) {
        int tabY = getSettingsTabY();
        if (mouseY < tabY || mouseY >= tabY + SETTINGS_TAB_HEIGHT) {
            return null;
        }
        for (SceneEditorSettingsTab tab : SceneEditorSettingsTab.values()) {
            int tabX = getSettingsTabX(tab);
            int tabWidth = getSettingsTabWidth(tab);
            if (mouseX >= tabX && mouseX < tabX + tabWidth) {
                return tab;
            }
        }
        return null;
    }

    private boolean handleSettingsTabClick(int mouseX, int mouseY) {
        SceneEditorSettingsTab clickedTab = getSettingsTabAt(mouseX, mouseY);
        if (clickedTab == null) {
            return false;
        }
        if (activeSettingsTab != clickedTab) {
            clearFocusedParameterRow();
            activeSliderRow = null;
            activeSettingsTab = clickedTab;
        }
        return true;
    }

    private void clearFocusedParameterRow() {
        if (focusedParameterRow != null) {
            focusedParameterRow.setFocused(false);
            focusedParameterRow = null;
        }
    }

    private void setFocusedParameterRow(NumericParameterRow row) {
        if (focusedParameterRow != null && focusedParameterRow != row) {
            focusedParameterRow.setFocused(false);
        }
        focusedParameterRow = row;
        row.setFocused(true);
    }

    private void drawInteractiveToggle() {
        int boxRight = interactiveToggleX + INTERACTIVE_CHECKBOX_SIZE;
        int boxBottom = interactiveToggleY + INTERACTIVE_CHECKBOX_SIZE;
        drawRect(interactiveToggleX, interactiveToggleY, boxRight, boxBottom, CHECKBOX_BACKGROUND_COLOR);
        drawBorder(
            interactiveToggleX,
            interactiveToggleY,
            INTERACTIVE_CHECKBOX_SIZE,
            INTERACTIVE_CHECKBOX_SIZE,
            INPUT_BORDER_COLOR);
        if (session.getSceneModel()
            .isInteractive()) {
            drawRect(interactiveToggleX + 3, interactiveToggleY + 3, boxRight - 3, boxBottom - 3, CHECKBOX_CHECK_COLOR);
        }
        this.drawString(
            this.fontRendererObj,
            this.fontRendererObj
                .trimStringToWidth(GuidebookText.SceneEditorInteractive.text(), Math.max(0, PARAMETER_LABEL_WIDTH - 2)),
            interactiveLabelX,
            interactiveToggleY + 2,
            PANEL_MUTED_TEXT);
        if (activeSettingsTab == SceneEditorSettingsTab.PREVIEW) {
            int mouseX = Mouse.getX() * this.width / this.mc.displayWidth;
            int mouseY = this.height - Mouse.getY() * this.height / this.mc.displayHeight - 1;
            boolean hovered = isInsidePreviewFrameButton(mouseX, mouseY);
            int backgroundColor = previewFrameOverlayVisible ? 0xA61C252E : hovered ? 0x7A1C252E : 0x5512181C;
            drawRect(
                previewFrameButtonX,
                previewFrameButtonY,
                previewFrameButtonX + PREVIEW_FRAME_BUTTON_WIDTH,
                previewFrameButtonY + PREVIEW_FRAME_BUTTON_HEIGHT,
                backgroundColor);
            drawBorder(
                previewFrameButtonX,
                previewFrameButtonY,
                PREVIEW_FRAME_BUTTON_WIDTH,
                PREVIEW_FRAME_BUTTON_HEIGHT,
                previewFrameOverlayVisible ? 0xFF00CAF2 : INPUT_BORDER_COLOR);
            this.drawCenteredString(
                this.fontRendererObj,
                GuidebookText.SceneEditorPreviewFrame.text(),
                previewFrameButtonX + PREVIEW_FRAME_BUTTON_WIDTH / 2,
                previewFrameButtonY + 3,
                PANEL_HEADER_COLOR);
        }
    }

    private boolean isInsideInteractiveToggle(int mouseX, int mouseY) {
        int rowRight = activeSettingsTab == SceneEditorSettingsTab.PREVIEW ? previewFrameButtonX - 8
            : settingsBoxX + settingsBoxWidth - SETTINGS_BOX_PADDING;
        int rowBottom = interactiveToggleY + INTERACTIVE_ROW_HEIGHT;
        return mouseX >= interactiveLabelX && mouseX < rowRight && mouseY >= interactiveToggleY && mouseY < rowBottom;
    }

    private boolean isInsideMarkdownToggle(int mouseX, int mouseY) {
        return screenLayout.markdownToggle()
            .contains(mouseX, mouseY);
    }

    private boolean isInsideMarkdownFooter(int mouseX, int mouseY) {
        return markdownPanelState.isExpanded() && screenLayout.markdownFooter()
            .contains(mouseX, mouseY);
    }

    private boolean isInsideMarkdownResizeHandle(int mouseX, int mouseY) {
        return markdownPanelState.isExpanded() && screenLayout.markdownResizeHandle()
            .contains(mouseX, mouseY);
    }

    private void toggleMarkdownPanel() {
        markdownPanelState.setExpanded(!markdownPanelState.isExpanded());
        draggingMarkdownResize = false;
        if (!markdownPanelState.isExpanded() && markdownTextArea != null) {
            markdownTextArea.setFocused(false);
        }
        recalculateLayout();
    }

    private void toggleMarkdownWrap() {
        markdownPanelState.setWrapEnabled(!markdownPanelState.isWrapEnabled());
        if (markdownTextArea != null) {
            markdownTextArea.setWrapEnabled(markdownPanelState.isWrapEnabled());
        }
    }

    private void updateMarkdownResize(int mouseX) {
        if (!markdownPanelState.isExpanded()) {
            return;
        }
        int requestedWidth = mouseX - leftPanelX;
        markdownPanelState
            .setOpenWidth(requestedWidth, SceneEditorScreenLayout.MIN_LEFT_OPEN_WIDTH, computeMarkdownMaxWidth());
        recalculateLayout();
    }

    private int computeMarkdownMaxWidth() {
        int rightReserved = Math.max(172, rightPanelWidth);
        int previewReserved = 220;
        int toggleReserved = SceneEditorScreenLayout.MARKDOWN_TOGGLE_WIDTH + 2;
        int computed = this.width - rightReserved - previewReserved - toggleReserved;
        return Math.max(
            SceneEditorScreenLayout.MIN_LEFT_OPEN_WIDTH,
            Math.min(SceneEditorScreenLayout.MAX_LEFT_OPEN_WIDTH, computed));
    }

    private boolean isInsidePreviewInteractionArea(int mouseX, int mouseY) {
        return screenLayout.previewInteraction()
            .contains(mouseX, mouseY);
    }

    private boolean isInsidePreviewFrameButton(int mouseX, int mouseY) {
        return activeSettingsTab == SceneEditorSettingsTab.PREVIEW && mouseX >= previewFrameButtonX
            && mouseX < previewFrameButtonX + PREVIEW_FRAME_BUTTON_WIDTH
            && mouseY >= previewFrameButtonY
            && mouseY < previewFrameButtonY + PREVIEW_FRAME_BUTTON_HEIGHT;
    }

    private void drawElementPanel(int mouseX, int mouseY) {
        ensureElementPanelStateValid();
        ensureExpandedElementEditor();
        this.drawString(
            this.fontRendererObj,
            GuidebookText.SceneEditorElementsPanel.text(),
            rightPanelX + 10,
            elementsBoxY - 14,
            PANEL_HEADER_COLOR);
        drawRect(
            elementsBoxX,
            elementsBoxY,
            elementsBoxX + elementsBoxWidth,
            elementsBoxY + elementsBoxHeight,
            PANEL_INNER_COLOR);
        drawBorder(elementsBoxX, elementsBoxY, elementsBoxWidth, elementsBoxHeight, 0xFF464A50);

        this.drawString(
            this.fontRendererObj,
            session.getSceneModel()
                .getElements()
                .size() + " element(s)",
            elementsBoxX + 8,
            elementsBoxY + 7,
            PANEL_MUTED_TEXT);

        addElementMenuState.update(isInsideAddElementButton(mouseX, mouseY), isInsideAddElementMenu(mouseX, mouseY));
        addElementMenuOpen = addElementMenuState.isOpen();

        int viewportHeight = getElementViewportHeight();
        elementPanelScrollState.setViewportPixels(viewportHeight);
        elementPanelScrollState.setContentPixels(getElementContentHeight());

        beginScissor(elementsBoxX + 1, getElementViewportTop(), elementsBoxWidth - 2, viewportHeight);
        List<ElementRowLayout> layouts = buildElementRowLayouts();
        for (ElementRowLayout layout : layouts) {
            if (layout.rowY + layout.totalHeight >= getElementViewportTop()
                && layout.rowY <= getElementViewportBottom()) {
                drawElementRow(
                    layout.element,
                    layout.rowX,
                    layout.rowY,
                    layout.rowWidth,
                    layout.totalHeight,
                    mouseX,
                    mouseY);
            }
        }
        if (elementReorderController.isDragging()) {
            drawElementReorderIndicator(layouts);
        }
        endScissor();

        if (session.getSceneModel()
            .getElements()
            .isEmpty()) {
            this.drawString(
                this.fontRendererObj,
                GuidebookText.SceneEditorAddElement.text(),
                elementsBoxX + 8,
                getElementViewportTop() + 4,
                PANEL_SUBTLE_TEXT);
        }

        drawElementScrollbar();

        if (addElementMenuOpen) {
            drawAddElementMenu(mouseX, mouseY);
        }
        if (isContextMenuOpen()) {
            drawElementContextMenu(mouseX, mouseY);
        }
    }

    private void drawElementRow(SceneEditorElementModel element, int x, int y, int width, int totalHeight, int mouseX,
        int mouseY) {
        boolean selected = element.getId()
            .equals(
                session.getSelectionState()
                    .getSelectedElementId());
        boolean expanded = element.getId()
            .equals(expandedElementId);
        int rowColor = selected ? ELEMENT_ROW_SELECTED : ELEMENT_ROW_BACKGROUND;
        drawRect(x, y, x + width, y + ELEMENT_ROW_HEIGHT, rowColor);
        drawBorder(x, y, width, ELEMENT_ROW_HEIGHT, selected ? 0xFF00CAF2 : 0xFF3E434A);

        int arrowX = x + 4;
        int arrowY = y + 6;
        this.drawString(this.fontRendererObj, expanded ? "v" : ">", arrowX, arrowY, PANEL_MUTED_TEXT);

        int iconX = x + 16;
        int iconY = y + 3;
        drawElementTypeIcon(element.getType(), iconX, iconY, selected);

        this.drawString(
            this.fontRendererObj,
            element.getType()
                .getDisplayText(),
            x + 34,
            y + 6,
            element.isVisible() ? PANEL_HEADER_COLOR : PANEL_SUBTLE_TEXT);

        int deleteX = x + width - 30;
        int eyeX = x + width - 14;
        GuideIconButton
            .drawIcon(this.mc, GuideIconButton.Role.SCENE_EDITOR_DELETE_ELEMENT, deleteX, y + 2, 12, 12, 0xC0FFFFFF);
        GuideIconButton.drawIcon(
            this.mc,
            element.isVisible() ? GuideIconButton.Role.SCENE_EDITOR_HIDE_ELEMENT
                : GuideIconButton.Role.SCENE_EDITOR_SHOW_ELEMENT,
            eyeX,
            y + 2,
            12,
            12,
            0xC0FFFFFF);

        if (!expanded) {
            return;
        }

        int expandedY = y + ELEMENT_ROW_HEIGHT;
        int expandedHeight = Math.max(0, totalHeight - ELEMENT_ROW_HEIGHT);
        drawRect(x, expandedY, x + width, expandedY + expandedHeight, ELEMENT_ROW_EXPANDED);
        drawBorder(x, expandedY, width, expandedHeight, 0xFF2D3137);
        if (expandedElementEditor != null && element.getId()
            .equals(expandedElementEditor.elementId)) {
            expandedElementEditor.setBounds(x, expandedY, width, expandedHeight);
            expandedElementEditor.draw(mouseX, mouseY);
        }
    }

    private boolean handleElementPanelClick(int mouseX, int mouseY, int button) {
        ensureExpandedElementEditor();
        if (expandedElementEditor != null && expandedElementEditor.mouseClicked(mouseX, mouseY, button)) {
            elementListFocused = true;
            return true;
        }
        if (button == 1) {
            ElementHit rightClickHit = findElementHit(mouseX, mouseY);
            if (rightClickHit == null) {
                closeElementContextMenu();
                return false;
            }
            elementListFocused = true;
            session.getSelectionState()
                .setSelectedElementId(rightClickHit.element.getId());
            openElementContextMenu(rightClickHit.element, mouseX, mouseY);
            return true;
        }
        if (button != 0) {
            return false;
        }
        if (isInsideAddElementButton(mouseX, mouseY)) {
            elementListFocused = true;
            return true;
        }
        SceneEditorElementType addType = addElementMenuTypeAt(mouseX, mouseY);
        if (addType != null) {
            SceneEditorElementModel element = elementController.addElement(addType);
            elementListFocused = true;
            session.getSelectionState()
                .setSelectedElementId(element.getId());
            expandedElementId = element.getId();
            addElementMenuState.keepOpenAfterAction();
            addElementMenuOpen = true;
            onElementModelApplied();
            return true;
        }

        ElementHit hit = findElementHit(mouseX, mouseY);
        if (hit == null) {
            if (isInsideElementViewport(mouseX, mouseY)) {
                elementListFocused = true;
                closeElementContextMenu();
                session.getSelectionState()
                    .setSelectedElementId(null);
                session.getSelectionState()
                    .setSelectedHandleId(null);
                return true;
            }
            return false;
        }

        elementListFocused = true;
        session.getSelectionState()
            .setSelectedElementId(hit.element.getId());
        if (hit.part == ElementHitPart.DELETE) {
            elementController.removeElement(hit.element.getId());
            if (hit.element.getId()
                .equals(expandedElementId)) {
                expandedElementId = null;
            }
            onElementModelApplied();
            return true;
        }
        if (hit.part == ElementHitPart.VISIBILITY) {
            elementController.setVisible(hit.element.getId(), !hit.element.isVisible());
            onElementModelApplied();
            return true;
        }
        if (hit.part == ElementHitPart.ARROW) {
            expandedElementId = hit.element.getId()
                .equals(expandedElementId) ? null : hit.element.getId();
            if (expandedElementId == null) {
                expandedElementEditor = null;
            }
            return true;
        }
        if (hit.part == ElementHitPart.ROW) {
            closeElementContextMenu();
            elementReorderController.beginDrag(hit.index);
            return true;
        }
        return true;
    }

    private void onElementModelApplied() {
        syncMarkdownTextFromAppliedModel();
        String mergeKey = resolveAppliedUndoMergeKey(currentElementUndoMergeKey());
        textSyncController
            .recordAppliedSnapshot(mergeKey, captureCurrentUiUndoState(), resolveAppliedUndoKeepOpen(mergeKey != null));
        ensureElementPanelStateValid();
        syncExpandedElementEditorFromModel();
        previewDirty = true;
        preservePreviewCameraOnNextRebuild = true;
    }

    private boolean handleUndoRedoShortcut(int keyCode) {
        if (isCtrlShiftKeyCombo(keyCode, Keyboard.KEY_Z) || isCtrlKeyCombo(keyCode, Keyboard.KEY_Y)) {
            return restoreUndoSnapshot(true);
        }
        if (isCtrlKeyCombo(keyCode, Keyboard.KEY_Z)) {
            return restoreUndoSnapshot(false);
        }
        return false;
    }

    private boolean restoreUndoSnapshot(boolean redo) {
        if (redo) {
            if (!session.getUndoHistory()
                .canRedo()) {
                return false;
            }
            applyUndoSnapshot(
                session.getUndoHistory()
                    .redo());
            return true;
        }
        if (!session.getUndoHistory()
            .canUndo()) {
            return false;
        }
        applyUndoSnapshot(
            session.getUndoHistory()
                .undo());
        return true;
    }

    private void applyUndoSnapshot(SceneEditorUndoSnapshot snapshot) {
        textSyncController.restoreSnapshot(snapshot);
        reloadUiFromSessionContent(snapshot.getUiState());
    }

    private void reloadUiFromSessionContent(SceneEditorUndoUiState uiState) {
        clearPendingMarkdownLiveSync();
        if (markdownTextArea != null) {
            markdownTextArea.setText(session.getRawText());
        }
        syncParameterRowsFromModel();
        ensureElementPanelStateValid();
        syncExpandedElementEditorFromModel();
        restoreCurrentUiUndoState(uiState);
        closeElementContextMenu();
        activeSliderRow = null;
        draggingMarkdownResize = false;
        draggingElementScrollbar = false;
        activePointDrag = null;
        elementReorderController.cancelDrag();
        if (activePreviewScene != null) {
            activePreviewScene.endDrag();
        }
        activePreviewScene = null;
        session.getSelectionState()
            .setSelectedHandleId(null);
        session.getSelectionState()
            .setDragging(false);
        previewDirty = true;
        preservePreviewCameraOnNextRebuild = false;
        refreshLinkedSelectionFromMarkdownCursor();
        refreshMarkdownHighlightFromSelectedElement();
    }

    private SceneEditorUndoUiState captureCurrentUiUndoState() {
        SceneEditorUndoUiState.Builder builder = SceneEditorUndoUiState.builder();
        for (NumericParameterRow row : numericParameterRows) {
            row.captureUndoState(builder);
        }
        if (expandedElementEditor != null) {
            expandedElementEditor.captureUndoState(builder);
        }
        return builder.build();
    }

    private void restoreCurrentUiUndoState(SceneEditorUndoUiState uiState) {
        if (uiState == null || uiState.isEmpty()) {
            return;
        }
        for (NumericParameterRow row : numericParameterRows) {
            row.restoreUndoState(uiState);
        }
        if (expandedElementEditor != null) {
            expandedElementEditor.restoreUndoState(uiState);
        }
    }

    private void recordCurrentUiDraftSnapshot(String mergeKey) {
        recordCurrentUiDraftSnapshot(mergeKey, true);
    }

    private void recordCurrentUiDraftSnapshot(String mergeKey, boolean keepOpen) {
        textSyncController.recordCurrentSnapshot(mergeKey, captureCurrentUiUndoState(), keepOpen);
    }

    private void scheduleMarkdownLiveSync() {
        markdownLiveSyncPending = true;
        markdownLiveSyncAtMillis = System.currentTimeMillis() + MARKDOWN_LIVE_SYNC_DEBOUNCE_MS;
    }

    private void clearPendingMarkdownLiveSync() {
        markdownLiveSyncPending = false;
        markdownLiveSyncAtMillis = 0L;
    }

    private void processPendingMarkdownLiveSync() {
        if (!markdownLiveSyncPending) {
            return;
        }
        if (markdownTextArea == null || !markdownTextArea.isFocused()) {
            clearPendingMarkdownLiveSync();
            return;
        }
        if (System.currentTimeMillis() < markdownLiveSyncAtMillis) {
            return;
        }
        clearPendingMarkdownLiveSync();
        SceneEditorTextSyncController.LiveApplyResult result = textSyncController.applyLiveDraftText();
        if (result == SceneEditorTextSyncController.LiveApplyResult.NO_CHANGE) {
            return;
        }
        recordCurrentUiDraftSnapshot(MARKDOWN_UNDO_MERGE_KEY);
        if (result == SceneEditorTextSyncController.LiveApplyResult.APPLIED) {
            applyLiveMarkdownState();
            return;
        }
        refreshLinkedSelectionFromMarkdownCursor();
        refreshMarkdownHighlightFromSelectedElement();
    }

    private void applyLiveMarkdownState() {
        syncParameterRowsFromModel();
        refreshLinkedSelectionFromMarkdownCursor();
        if (expandedElementId != null) {
            expandedElementId = session.getSelectionState()
                .getSelectedElementId();
        }
        ensureElementPanelStateValid();
        syncExpandedElementEditorFromModel();
        previewDirty = true;
        preservePreviewCameraOnNextRebuild = true;
        refreshMarkdownHighlightFromSelectedElement();
    }

    private boolean commitWithAppliedUndoContext(@Nullable String mergeKey, boolean keepOpen, BooleanSupplier action) {
        String previousMergeKey = appliedUndoMergeKeyOverride;
        boolean previousKeepOpen = appliedUndoKeepOpenOverride;
        boolean previousOverrideActive = appliedUndoOverrideActive;
        appliedUndoMergeKeyOverride = mergeKey;
        appliedUndoKeepOpenOverride = keepOpen;
        appliedUndoOverrideActive = true;
        try {
            return action.getAsBoolean();
        } finally {
            appliedUndoMergeKeyOverride = previousMergeKey;
            appliedUndoKeepOpenOverride = previousKeepOpen;
            appliedUndoOverrideActive = previousOverrideActive;
        }
    }

    @Nullable
    private String resolveAppliedUndoMergeKey(@Nullable String fallbackMergeKey) {
        return appliedUndoOverrideActive ? appliedUndoMergeKeyOverride : fallbackMergeKey;
    }

    private boolean resolveAppliedUndoKeepOpen(boolean fallbackKeepOpen) {
        return appliedUndoOverrideActive ? appliedUndoKeepOpenOverride : fallbackKeepOpen;
    }

    @Nullable
    private String currentParameterUndoMergeKey() {
        if (activeSliderRow == null) {
            return null;
        }
        return "parameter-slider:" + activeSettingsTab.name() + ":" + activeSliderRow.label.name();
    }

    @Nullable
    private String currentElementUndoMergeKey() {
        if (activePointDrag == null) {
            return null;
        }
        String selectedHandleId = session.getSelectionState()
            .getSelectedHandleId();
        return selectedHandleId == null ? "element-handle-drag" : "element-handle-drag:" + selectedHandleId;
    }

    private boolean handleElementShortcutKey(int keyCode) {
        if (hasFocusedTextInput()) {
            return false;
        }
        SceneEditorElementModel selectedElement = getSelectedElementModel();
        if (selectedElement != null && isCtrlKeyCombo(keyCode, Keyboard.KEY_C)) {
            return elementContextMenuController.copyElement(selectedElement.getId());
        }
        if (selectedElement != null && isCtrlKeyCombo(keyCode, Keyboard.KEY_X)) {
            if (!elementContextMenuController.cutElement(selectedElement.getId())) {
                return false;
            }
            if (selectedElement.getId()
                .equals(expandedElementId)) {
                expandedElementId = null;
            }
            onElementModelApplied();
            return true;
        }
        if (isCtrlKeyCombo(keyCode, Keyboard.KEY_V)) {
            boolean pasted;
            if (selectedElement != null) {
                pasted = elementContextMenuController.pasteAfter(selectedElement.getId());
            } else if (elementListFocused) {
                pasted = elementContextMenuController.pasteAtTop();
            } else {
                pasted = false;
            }
            if (!pasted) {
                return false;
            }
            expandedElementId = session.getSelectionState()
                .getSelectedElementId();
            onElementModelApplied();
            return true;
        }
        if (selectedElement != null && keyCode == Keyboard.KEY_DELETE) {
            if (!elementContextMenuController.deleteElement(selectedElement.getId())) {
                return false;
            }
            if (selectedElement.getId()
                .equals(expandedElementId)) {
                expandedElementId = null;
            }
            onElementModelApplied();
            return true;
        }
        return false;
    }

    private boolean commitExpandedElementEditor(int mouseX, int mouseY) {
        ensureExpandedElementEditor();
        return expandedElementEditor == null || expandedElementEditor.commitFocusedDraft(mouseX, mouseY);
    }

    private void ensureExpandedElementEditor() {
        if (expandedElementId == null) {
            expandedElementEditor = null;
            return;
        }
        if (expandedElementEditor != null && expandedElementId.equals(expandedElementEditor.elementId)) {
            return;
        }
        SceneEditorElementModel element = session.getSceneModel()
            .getElement(expandedElementId)
            .orElse(null);
        if (element == null) {
            expandedElementEditor = null;
            return;
        }
        expandedElementEditor = new ElementEditorPanel(element);
    }

    private void syncExpandedElementEditorFromModel() {
        if (expandedElementId == null || expandedElementEditor == null) {
            return;
        }
        SceneEditorElementModel element = session.getSceneModel()
            .getElement(expandedElementId)
            .orElse(null);
        if (element == null) {
            expandedElementEditor = null;
            return;
        }
        expandedElementEditor.syncFromModel(element);
    }

    private void ensureElementPanelStateValid() {
        UUID selectedElementId = session.getSelectionState()
            .getSelectedElementId();
        if (selectedElementId != null && !session.getSceneModel()
            .getElement(selectedElementId)
            .isPresent()) {
            session.getSelectionState()
                .setSelectedElementId(null);
        }
        if (expandedElementId != null && !session.getSceneModel()
            .getElement(expandedElementId)
            .isPresent()) {
            expandedElementId = null;
        }
        if (expandedElementId == null) {
            expandedElementEditor = null;
        }
        if (contextMenuElementId != null && !session.getSceneModel()
            .getElement(contextMenuElementId)
            .isPresent()) {
            closeElementContextMenu();
        }
        if (!elementReorderController.isDragging() && session.getSceneModel()
            .getElements()
            .size() <= 1) {
            elementReorderController.cancelDrag();
        }
    }

    @Nullable
    private SceneEditorElementModel getSelectedElementModel() {
        UUID selectedElementId = session.getSelectionState()
            .getSelectedElementId();
        if (selectedElementId == null) {
            return null;
        }
        return session.getSceneModel()
            .getElement(selectedElementId)
            .orElse(null);
    }

    @Nullable
    private SceneEditorElementModel getSelectedPointHandleElement() {
        SceneEditorElementModel selectedElement = getSelectedElementModel();
        return handleOverlay.supportsPointHandle(selectedElement) ? selectedElement : null;
    }

    private void refreshLinkedSelectionFromMarkdownCursor() {
        if (markdownTextArea == null || !markdownTextArea.isFocused()) {
            return;
        }
        UUID nextSelectedElementId = linkedSelectionController.resolveSelectedElementId(
            textSyncController.getDisplayRangeIndex(),
            markdownTextArea.getCursorIndex(),
            session.getSelectionState()
                .getSelectedElementId());
        if (nextSelectedElementId == null) {
            return;
        }
        session.getSelectionState()
            .setSelectedElementId(nextSelectedElementId);
    }

    private void refreshMarkdownHighlightFromSelectedElement() {
        if (markdownTextArea == null) {
            return;
        }
        UUID selectedElementId = session.getSelectionState()
            .getSelectedElementId();
        if (selectedElementId == null) {
            markdownTextArea.clearBackgroundHighlight();
            return;
        }
        SceneEditorMarkdownElementRange range = textSyncController.getDisplayRangeIndex()
            .findByElementId(selectedElementId)
            .orElse(null);
        if (range == null) {
            markdownTextArea.clearBackgroundHighlight();
            return;
        }
        markdownTextArea.setBackgroundHighlight(range.getStartIndex(), range.getEndIndex());
    }

    private int getElementTotalHeight(SceneEditorElementModel element) {
        return element.getId()
            .equals(expandedElementId) ? ELEMENT_ROW_HEIGHT + getExpandedElementHeight(element.getType())
                : ELEMENT_ROW_HEIGHT;
    }

    private int getExpandedElementHeight(SceneEditorElementType type) {
        int rowCount = 1 + 3 + (supportsSecondaryVector(type) ? 3 : 0) + (supportsElementThickness(type) ? 1 : 0) + 1;
        return 6 + rowCount * ELEMENT_FIELD_ROW_HEIGHT + 14 + ELEMENT_TOOLTIP_HEIGHT;
    }

    private boolean supportsSecondaryVector(SceneEditorElementType type) {
        return type == SceneEditorElementType.BOX || type == SceneEditorElementType.LINE;
    }

    private boolean supportsElementThickness(SceneEditorElementType type) {
        return type != SceneEditorElementType.DIAMOND;
    }

    private LytRect getAddElementMenuBounds() {
        if (addElementButton == null) {
            return new LytRect(elementsBoxX, elementsBoxY, ELEMENT_MENU_WIDTH, 0);
        }
        int menuHeight = SceneEditorElementType.values().length * ELEMENT_MENU_ROW_HEIGHT;
        return SceneEditorPopupLayout.placeBelowAnchor(
            new LytRect(
                addElementButton.xPosition,
                addElementButton.yPosition,
                addElementButton.width,
                addElementButton.height),
            ELEMENT_MENU_WIDTH,
            menuHeight,
            this.width,
            this.height,
            4);
    }

    private boolean isInsideAddElementButton(int mouseX, int mouseY) {
        return addElementButton != null && mouseX >= addElementButton.xPosition
            && mouseX < addElementButton.xPosition + addElementButton.width
            && mouseY >= addElementButton.yPosition
            && mouseY < addElementButton.yPosition + addElementButton.height;
    }

    private boolean isInsideAddElementMenu(int mouseX, int mouseY) {
        LytRect menuBounds = getAddElementMenuBounds();
        return menuBounds.contains(mouseX, mouseY);
    }

    @Nullable
    private SceneEditorElementType addElementMenuTypeAt(int mouseX, int mouseY) {
        if (!addElementMenuOpen || !isInsideAddElementMenu(mouseX, mouseY)) {
            return null;
        }
        LytRect menuBounds = getAddElementMenuBounds();
        int index = (mouseY - menuBounds.y()) / ELEMENT_MENU_ROW_HEIGHT;
        SceneEditorElementType[] elementTypes = SceneEditorElementType.values();
        if (index < 0 || index >= elementTypes.length) {
            return null;
        }
        return elementTypes[index];
    }

    private void drawAddElementMenu(int mouseX, int mouseY) {
        LytRect menuBounds = getAddElementMenuBounds();
        drawRect(menuBounds.x(), menuBounds.y(), menuBounds.right(), menuBounds.bottom(), ELEMENT_MENU_BACKGROUND);
        drawBorder(menuBounds.x(), menuBounds.y(), menuBounds.width(), menuBounds.height(), 0xFF3E434A);
        for (int i = 0; i < SceneEditorElementType.values().length; i++) {
            SceneEditorElementType type = SceneEditorElementType.values()[i];
            int rowY = menuBounds.y() + i * ELEMENT_MENU_ROW_HEIGHT;
            if (mouseX >= menuBounds.x() && mouseX < menuBounds.right()
                && mouseY >= rowY
                && mouseY < rowY + ELEMENT_MENU_ROW_HEIGHT) {
                drawRect(
                    menuBounds.x() + 1,
                    rowY,
                    menuBounds.right() - 1,
                    rowY + ELEMENT_MENU_ROW_HEIGHT,
                    ELEMENT_MENU_HOVER);
            }
            drawElementTypeIcon(type, menuBounds.x() + 4, rowY + 2, false);
            this.drawString(
                this.fontRendererObj,
                type.getDisplayText(),
                menuBounds.x() + 22,
                rowY + 5,
                PANEL_HEADER_COLOR);
        }
    }

    private boolean isInsideScreenshotButton(int mouseX, int mouseY) {
        return screenshotButton != null && mouseX >= screenshotButton.xPosition
            && mouseX < screenshotButton.xPosition + screenshotButton.width
            && mouseY >= screenshotButton.yPosition
            && mouseY < screenshotButton.yPosition + screenshotButton.height;
    }

    private LytRect getScreenshotMenuBounds() {
        if (screenshotButton == null) {
            return new LytRect(
                TOOLBAR_MARGIN_X + 120,
                TOOLBAR_Y + GuideIconButton.HEIGHT,
                SceneEditorScreenshotMenuController.MENU_WIDTH,
                0);
        }
        return SceneEditorPopupLayout.placeBelowAnchor(
            new LytRect(
                screenshotButton.xPosition,
                screenshotButton.yPosition,
                screenshotButton.width,
                screenshotButton.height),
            SceneEditorScreenshotMenuController.MENU_WIDTH,
            screenshotMenuController.menuHeight(),
            this.width,
            this.height,
            4);
    }

    private boolean isInsideScreenshotMenu(int mouseX, int mouseY) {
        return screenshotMenuController.isOpen() && getScreenshotMenuBounds().contains(mouseX, mouseY);
    }

    private boolean commitScreenshotScaleDraft() {
        if (screenshotScaleField == null) {
            return true;
        }
        boolean committed = screenshotMenuController.commitScaleDraft(screenshotScaleField.getText());
        screenshotScaleField.setText(screenshotMenuController.getScaleDraftText());
        return committed;
    }

    private boolean handleScreenshotScaleKeyTyped(char typedChar, int keyCode) {
        if (screenshotScaleField == null || !screenshotScaleField.isFocused()) {
            return false;
        }
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            return commitScreenshotScaleDraft();
        }
        boolean handled = screenshotScaleField.textboxKeyTyped(typedChar, keyCode);
        if (handled) {
            screenshotMenuController.setScaleDraftText(screenshotScaleField.getText());
        }
        return handled;
    }

    private boolean handleScreenshotMenuClick(int mouseX, int mouseY, int button) {
        boolean insideButton = isInsideScreenshotButton(mouseX, mouseY);
        boolean insideMenu = screenshotMenuController.isOpen() && getScreenshotMenuBounds().contains(mouseX, mouseY);
        if (!screenshotMenuController.isOpen()) {
            return false;
        }
        if (button == 0 && insideMenu) {
            LytRect menuBounds = getScreenshotMenuBounds();
            SceneEditorScreenshotFormat format = screenshotMenuController.formatAt(menuBounds, mouseX, mouseY);
            if (format != null) {
                screenshotMenuController.selectFormat(format);
                return true;
            }
            if (screenshotScaleField != null) {
                LytRect inputBounds = screenshotMenuController.scaleInputBounds(menuBounds);
                if (inputBounds.contains(mouseX, mouseY)) {
                    screenshotScaleField.xPosition = inputBounds.x() + 4;
                    screenshotScaleField.yPosition = inputBounds.y() + 4;
                    screenshotScaleField.width = Math.max(8, inputBounds.width() - 8);
                    screenshotScaleField.height = inputBounds.height() - 4;
                    screenshotScaleField.mouseClicked(mouseX, mouseY, button);
                    return true;
                }
            }
            LytRect sliderBounds = screenshotMenuController.scaleSliderBounds(menuBounds);
            if (sliderBounds.contains(mouseX, mouseY)) {
                screenshotMenuController.applyScaleFraction(
                    GuideSliderRenderer.fractionFromMouse(mouseX, sliderBounds.x(), sliderBounds.width()));
                if (screenshotScaleField != null) {
                    screenshotScaleField.setText(screenshotMenuController.getScaleDraftText());
                }
                draggingScreenshotScaleSlider = true;
                return true;
            }
        }
        if (!insideMenu && screenshotScaleField != null && screenshotScaleField.isFocused()) {
            if (!commitScreenshotScaleDraft()) {
                return true;
            }
            screenshotScaleField.setFocused(false);
        }
        screenshotMenuController.handleOutsideClick(insideButton, insideMenu);
        return false;
    }

    private void drawScreenshotMenu(int mouseX, int mouseY) {
        LytRect menuBounds = getScreenshotMenuBounds();
        drawRect(menuBounds.x(), menuBounds.y(), menuBounds.right(), menuBounds.bottom(), ELEMENT_MENU_BACKGROUND);
        drawBorder(menuBounds.x(), menuBounds.y(), menuBounds.width(), menuBounds.height(), 0xFF3E434A);

        int rowTop = menuBounds.y() + SceneEditorScreenshotMenuController.MENU_PADDING;
        for (int i = 0; i < SceneEditorScreenshotFormat.values().length; i++) {
            SceneEditorScreenshotFormat format = SceneEditorScreenshotFormat.values()[i];
            int rowY = rowTop + i * SceneEditorScreenshotMenuController.FORMAT_ROW_HEIGHT;
            boolean hovered = mouseX >= menuBounds.x() && mouseX < menuBounds.right()
                && mouseY >= rowY
                && mouseY < rowY + SceneEditorScreenshotMenuController.FORMAT_ROW_HEIGHT;
            if (hovered) {
                drawRect(
                    menuBounds.x() + 1,
                    rowY,
                    menuBounds.right() - 1,
                    rowY + SceneEditorScreenshotMenuController.FORMAT_ROW_HEIGHT,
                    ELEMENT_MENU_HOVER);
            }
            boolean selected = format == screenshotMenuController.getFormat();
            int boxX = menuBounds.x() + 6;
            int boxY = rowY + 3;
            drawRect(boxX, boxY, boxX + 10, boxY + 10, CHECKBOX_BACKGROUND_COLOR);
            drawBorder(boxX, boxY, 10, 10, selected ? CHECKBOX_CHECK_COLOR : INPUT_BORDER_COLOR);
            if (selected) {
                drawRect(boxX + 2, boxY + 2, boxX + 8, boxY + 8, CHECKBOX_CHECK_COLOR);
            }
            this.drawString(this.fontRendererObj, format.name(), menuBounds.x() + 22, rowY + 5, PANEL_HEADER_COLOR);
        }

        this.drawString(
            this.fontRendererObj,
            GuidebookText.SceneEditorScreenshotScale.text(),
            menuBounds.x() + SceneEditorScreenshotMenuController.MENU_PADDING,
            rowTop + SceneEditorScreenshotFormat.values().length * SceneEditorScreenshotMenuController.FORMAT_ROW_HEIGHT
                + SceneEditorScreenshotMenuController.SECTION_GAP,
            PANEL_MUTED_TEXT);

        LytRect inputBounds = screenshotMenuController.scaleInputBounds(menuBounds);
        int borderColor = screenshotMenuController.hasScaleValidationError() ? INPUT_ERROR_BORDER_COLOR
            : screenshotScaleField != null && screenshotScaleField.isFocused() ? INPUT_FOCUSED_BORDER_COLOR
                : INPUT_BORDER_COLOR;
        drawRect(inputBounds.x(), inputBounds.y(), inputBounds.right(), inputBounds.bottom(), INPUT_BACKGROUND_COLOR);
        drawBorder(inputBounds.x(), inputBounds.y(), inputBounds.width(), inputBounds.height(), borderColor);
        if (screenshotScaleField != null) {
            screenshotScaleField.xPosition = inputBounds.x() + 4;
            screenshotScaleField.yPosition = inputBounds.y() + 4;
            screenshotScaleField.width = Math.max(8, inputBounds.width() - 8);
            screenshotScaleField.height = inputBounds.height() - 4;
            drawCompactTextFieldValue(screenshotScaleField, screenshotMenuController.getScaleDraftText());
        }

        LytRect sliderBounds = screenshotMenuController.scaleSliderBounds(menuBounds);
        GuideSliderRenderer.render(
            Gui::drawRect,
            GuideSliderRenderer.layout(
                sliderBounds.x(),
                sliderBounds.y(),
                sliderBounds.width(),
                screenshotMenuController.scaleFraction()),
            draggingScreenshotScaleSlider || sliderBounds.contains(mouseX, mouseY));
    }

    private boolean isInsideSnapButton(int mouseX, int mouseY) {
        return snapButton != null && mouseX >= snapButton.xPosition
            && mouseX < snapButton.xPosition + snapButton.width
            && mouseY >= snapButton.yPosition
            && mouseY < snapButton.yPosition + snapButton.height;
    }

    private LytRect getSnapModeMenuBounds() {
        if (snapButton == null) {
            return new LytRect(TOOLBAR_MARGIN_X, TOOLBAR_Y + GuideIconButton.HEIGHT, SNAP_MENU_WIDTH, 0);
        }
        int menuHeight = SnapModeOption.values().length * ELEMENT_MENU_ROW_HEIGHT;
        return SceneEditorPopupLayout.placeBelowAnchor(
            new LytRect(snapButton.xPosition, snapButton.yPosition, snapButton.width, snapButton.height),
            SNAP_MENU_WIDTH,
            menuHeight,
            this.width,
            this.height,
            4);
    }

    private boolean isInsideSnapModeMenu(int mouseX, int mouseY) {
        return snapModeMenuOpen && getSnapModeMenuBounds().contains(mouseX, mouseY);
    }

    @Nullable
    private SnapModeOption snapModeOptionAt(int mouseX, int mouseY) {
        if (!isInsideSnapModeMenu(mouseX, mouseY)) {
            return null;
        }
        LytRect menuBounds = getSnapModeMenuBounds();
        int index = (mouseY - menuBounds.y()) / ELEMENT_MENU_ROW_HEIGHT;
        SnapModeOption[] values = SnapModeOption.values();
        if (index < 0 || index >= values.length) {
            return null;
        }
        return values[index];
    }

    private boolean handleSnapModeMenuClick(int mouseX, int mouseY, int button) {
        if (!snapModeMenuOpen) {
            return false;
        }
        if (button == 0) {
            SnapModeOption option = snapModeOptionAt(mouseX, mouseY);
            if (option != null) {
                option.toggle();
                ModConfig.save();
                return true;
            }
        }
        if (!isInsideSnapButton(mouseX, mouseY) && !isInsideSnapModeMenu(mouseX, mouseY)) {
            snapModeMenuOpen = false;
        }
        return false;
    }

    private void drawSnapModeMenu(int mouseX, int mouseY) {
        LytRect menuBounds = getSnapModeMenuBounds();
        drawRect(menuBounds.x(), menuBounds.y(), menuBounds.right(), menuBounds.bottom(), ELEMENT_MENU_BACKGROUND);
        drawBorder(menuBounds.x(), menuBounds.y(), menuBounds.width(), menuBounds.height(), 0xFF3E434A);
        for (int i = 0; i < SnapModeOption.values().length; i++) {
            SnapModeOption option = SnapModeOption.values()[i];
            int rowY = menuBounds.y() + i * ELEMENT_MENU_ROW_HEIGHT;
            boolean hovered = mouseX >= menuBounds.x() && mouseX < menuBounds.right()
                && mouseY >= rowY
                && mouseY < rowY + ELEMENT_MENU_ROW_HEIGHT;
            if (hovered) {
                drawRect(
                    menuBounds.x() + 1,
                    rowY,
                    menuBounds.right() - 1,
                    rowY + ELEMENT_MENU_ROW_HEIGHT,
                    ELEMENT_MENU_HOVER);
            }
            int boxX = menuBounds.x() + 6;
            int boxY = rowY + 3;
            drawRect(boxX, boxY, boxX + 10, boxY + 10, CHECKBOX_BACKGROUND_COLOR);
            drawBorder(boxX, boxY, 10, 10, option.isEnabled() ? CHECKBOX_CHECK_COLOR : INPUT_BORDER_COLOR);
            if (option.isEnabled()) {
                drawRect(boxX + 2, boxY + 2, boxX + 8, boxY + 8, CHECKBOX_CHECK_COLOR);
            }
            this.drawString(
                this.fontRendererObj,
                option.text()
                    .text(),
                menuBounds.x() + 22,
                rowY + 5,
                PANEL_HEADER_COLOR);
        }
    }

    private boolean handleElementContextMenuClick(int mouseX, int mouseY, int button) {
        if (!isContextMenuOpen()) {
            return false;
        }
        if (button == 0) {
            ElementContextMenuAction action = elementContextMenuActionAt(mouseX, mouseY);
            if (action != null) {
                executeElementContextMenuAction(action);
                closeElementContextMenu();
                return true;
            }
        }
        if (!isInsideElementContextMenu(mouseX, mouseY)) {
            closeElementContextMenu();
        }
        return false;
    }

    private void openElementContextMenu(SceneEditorElementModel element, int mouseX, int mouseY) {
        contextMenuElementId = element.getId();
        contextMenuActions = buildContextMenuActions(element);
        int menuHeight = contextMenuActions.size() * ELEMENT_MENU_ROW_HEIGHT;
        LytRect menuBounds = SceneEditorPopupLayout
            .clampToViewport(mouseX, mouseY, ELEMENT_CONTEXT_MENU_WIDTH, menuHeight, this.width, this.height, 4);
        contextMenuX = menuBounds.x();
        contextMenuY = menuBounds.y();
    }

    private void closeElementContextMenu() {
        contextMenuElementId = null;
        contextMenuActions = Collections.emptyList();
    }

    private boolean isContextMenuOpen() {
        return contextMenuElementId != null && !contextMenuActions.isEmpty();
    }

    private List<ElementContextMenuAction> buildContextMenuActions(SceneEditorElementModel element) {
        List<ElementContextMenuAction> actions = new ArrayList<>();
        actions.add(ElementContextMenuAction.CUT);
        actions.add(ElementContextMenuAction.COPY);
        if (elementContextMenuController.hasClipboard()) {
            actions.add(ElementContextMenuAction.PASTE);
        }
        actions.add(ElementContextMenuAction.TOGGLE_VISIBILITY);
        actions.add(ElementContextMenuAction.RANDOMIZE_COLOR);
        actions.add(ElementContextMenuAction.DELETE);
        return actions;
    }

    @Nullable
    private ElementContextMenuAction elementContextMenuActionAt(int mouseX, int mouseY) {
        if (!isInsideElementContextMenu(mouseX, mouseY)) {
            return null;
        }
        int index = (mouseY - contextMenuY) / ELEMENT_MENU_ROW_HEIGHT;
        if (index < 0 || index >= contextMenuActions.size()) {
            return null;
        }
        return contextMenuActions.get(index);
    }

    private boolean isInsideElementContextMenu(int mouseX, int mouseY) {
        if (!isContextMenuOpen()) {
            return false;
        }
        int menuHeight = contextMenuActions.size() * ELEMENT_MENU_ROW_HEIGHT;
        return mouseX >= contextMenuX && mouseX < contextMenuX + ELEMENT_CONTEXT_MENU_WIDTH
            && mouseY >= contextMenuY
            && mouseY < contextMenuY + menuHeight;
    }

    private void drawElementContextMenu(int mouseX, int mouseY) {
        SceneEditorElementModel element = session.getSceneModel()
            .getElement(contextMenuElementId)
            .orElse(null);
        if (element == null) {
            closeElementContextMenu();
            return;
        }
        int menuHeight = contextMenuActions.size() * ELEMENT_MENU_ROW_HEIGHT;
        drawRect(
            contextMenuX,
            contextMenuY,
            contextMenuX + ELEMENT_CONTEXT_MENU_WIDTH,
            contextMenuY + menuHeight,
            ELEMENT_MENU_BACKGROUND);
        drawBorder(contextMenuX, contextMenuY, ELEMENT_CONTEXT_MENU_WIDTH, menuHeight, 0xFF3E434A);
        for (int i = 0; i < contextMenuActions.size(); i++) {
            int rowY = contextMenuY + i * ELEMENT_MENU_ROW_HEIGHT;
            if (mouseX >= contextMenuX && mouseX < contextMenuX + ELEMENT_CONTEXT_MENU_WIDTH
                && mouseY >= rowY
                && mouseY < rowY + ELEMENT_MENU_ROW_HEIGHT) {
                drawRect(
                    contextMenuX + 1,
                    rowY,
                    contextMenuX + ELEMENT_CONTEXT_MENU_WIDTH - 1,
                    rowY + ELEMENT_MENU_ROW_HEIGHT,
                    ELEMENT_MENU_HOVER);
            }
            String text = contextMenuActions.get(i)
                .getText(element)
                .text();
            this.drawString(this.fontRendererObj, text, contextMenuX + 6, rowY + 5, PANEL_HEADER_COLOR);
        }
    }

    private void executeElementContextMenuAction(ElementContextMenuAction action) {
        if (contextMenuElementId == null) {
            return;
        }
        boolean changed = false;
        switch (action) {
            case CUT:
                changed = elementContextMenuController.cutElement(contextMenuElementId);
                if (changed && contextMenuElementId.equals(expandedElementId)) {
                    expandedElementId = null;
                    expandedElementEditor = null;
                }
                break;
            case COPY:
                elementContextMenuController.copyElement(contextMenuElementId);
                break;
            case PASTE:
                changed = elementContextMenuController.pasteAfter(contextMenuElementId);
                if (changed) {
                    expandedElementId = session.getSelectionState()
                        .getSelectedElementId();
                }
                break;
            case TOGGLE_VISIBILITY:
                changed = elementContextMenuController.toggleVisibility(contextMenuElementId);
                break;
            case RANDOMIZE_COLOR:
                changed = elementContextMenuController.randomizeColor(contextMenuElementId);
                break;
            case DELETE:
                changed = elementContextMenuController.deleteElement(contextMenuElementId);
                if (changed && contextMenuElementId.equals(expandedElementId)) {
                    expandedElementId = null;
                    expandedElementEditor = null;
                }
                break;
            default:
                break;
        }
        if (changed) {
            onElementModelApplied();
        }
    }

    @Nullable
    private ElementHit findElementHit(int mouseX, int mouseY) {
        for (ElementRowLayout layout : buildElementRowLayouts()) {
            if (mouseX >= layout.rowX && mouseX < layout.rowX + layout.rowWidth
                && mouseY >= layout.rowY
                && mouseY < layout.rowY + layout.totalHeight) {
                int deleteX = layout.rowX + layout.rowWidth - 30;
                int eyeX = layout.rowX + layout.rowWidth - 14;
                if (mouseY < layout.rowY + ELEMENT_ROW_HEIGHT) {
                    if (mouseX >= layout.rowX + 2 && mouseX < layout.rowX + 14) {
                        return new ElementHit(layout.element, layout.index, ElementHitPart.ARROW);
                    }
                    if (mouseX >= deleteX && mouseX < deleteX + 12) {
                        return new ElementHit(layout.element, layout.index, ElementHitPart.DELETE);
                    }
                    if (mouseX >= eyeX && mouseX < eyeX + 12) {
                        return new ElementHit(layout.element, layout.index, ElementHitPart.VISIBILITY);
                    }
                    return new ElementHit(layout.element, layout.index, ElementHitPart.ROW);
                }
                return new ElementHit(layout.element, layout.index, ElementHitPart.BODY);
            }
        }
        return null;
    }

    private int getElementContentHeight() {
        int contentHeight = 0;
        for (SceneEditorElementModel element : session.getSceneModel()
            .getElements()) {
            contentHeight += getElementTotalHeight(element) + ELEMENT_ROW_GAP;
        }
        return contentHeight > 0 ? contentHeight - ELEMENT_ROW_GAP : 0;
    }

    private List<ElementRowLayout> buildElementRowLayouts() {
        List<ElementRowLayout> layouts = new ArrayList<>();
        int rowX = elementsBoxX + 6;
        int rowWidth = getElementRowWidth();
        int rowY = getElementViewportTop() - elementPanelScrollState.getOffsetPixels();
        List<SceneEditorElementModel> elements = session.getSceneModel()
            .getElements();
        for (int i = 0; i < elements.size(); i++) {
            SceneEditorElementModel element = elements.get(i);
            int totalHeight = getElementTotalHeight(element);
            layouts.add(new ElementRowLayout(element, i, rowX, rowY, rowWidth, totalHeight));
            rowY += totalHeight + ELEMENT_ROW_GAP;
        }
        return layouts;
    }

    private List<SceneEditorElementReorderController.RowMetrics> buildElementRowMetrics() {
        List<SceneEditorElementReorderController.RowMetrics> metrics = new ArrayList<>();
        for (ElementRowLayout layout : buildElementRowLayouts()) {
            metrics.add(new SceneEditorElementReorderController.RowMetrics(layout.rowY, layout.totalHeight));
        }
        return metrics;
    }

    private void drawElementReorderIndicator(List<ElementRowLayout> layouts) {
        if (layouts.isEmpty()) {
            return;
        }
        int insertionIndex = elementReorderController.getInsertionIndex();
        int draggedIndex = elementReorderController.getDraggedIndex();
        if (insertionIndex < 0 || insertionIndex == draggedIndex || insertionIndex == draggedIndex + 1) {
            return;
        }
        int lineY;
        if (insertionIndex <= 0) {
            lineY = layouts.get(0).rowY;
        } else if (insertionIndex >= layouts.size()) {
            ElementRowLayout last = layouts.get(layouts.size() - 1);
            lineY = last.rowY + last.totalHeight;
        } else {
            lineY = layouts.get(insertionIndex).rowY;
        }
        int lineX = layouts.get(0).rowX;
        int lineWidth = layouts.get(0).rowWidth;
        drawRect(lineX, lineY - 1, lineX + lineWidth, lineY + 1, 0xFF00CAF2);
    }

    private boolean isInsideElementViewport(int mouseX, int mouseY) {
        return mouseX >= elementsBoxX + 1 && mouseX < elementsBoxX + elementsBoxWidth - 1
            && mouseY >= getElementViewportTop()
            && mouseY < getElementViewportBottom();
    }

    private int getElementViewportTop() {
        return elementsBoxY + ELEMENT_VIEWPORT_TOP;
    }

    private int getElementViewportBottom() {
        return elementsBoxY + elementsBoxHeight - ELEMENT_VIEWPORT_BOTTOM_PADDING;
    }

    private int getElementViewportHeight() {
        return Math.max(0, getElementViewportBottom() - getElementViewportTop());
    }

    private int getElementRowWidth() {
        int width = elementsBoxWidth - 12;
        if (elementPanelScrollState.getMaxOffsetPixels() > 0) {
            width -= ELEMENT_SCROLLBAR_WIDTH + 2;
        }
        return Math.max(40, width);
    }

    private boolean handleElementScrollbarClick(int mouseX, int mouseY, int button) {
        if (button != 0) {
            return false;
        }
        LytRect scrollbarBounds = getElementScrollbarBounds();
        if (scrollbarBounds.isEmpty() || !scrollbarBounds.contains(mouseX, mouseY)) {
            return false;
        }
        SceneEditorVerticalScrollbar.Thumb thumb = getElementScrollbarThumb();
        if (thumb == null) {
            return false;
        }
        if (mouseY >= thumb.start() && mouseY < thumb.end()) {
            elementScrollbarGrabOffset = mouseY - thumb.start();
        } else {
            elementScrollbarGrabOffset = thumb.size() / 2;
            elementPanelScrollState.setOffsetPixels(
                SceneEditorVerticalScrollbar.offsetFromDrag(
                    mouseY,
                    elementScrollbarGrabOffset,
                    scrollbarBounds.y(),
                    scrollbarBounds.height(),
                    elementPanelScrollState.getContentPixels(),
                    elementPanelScrollState.getViewportPixels()));
        }
        draggingElementScrollbar = true;
        return true;
    }

    private void dragElementScrollbar(int mouseY) {
        LytRect scrollbarBounds = getElementScrollbarBounds();
        if (scrollbarBounds.isEmpty()) {
            draggingElementScrollbar = false;
            return;
        }
        elementPanelScrollState.setOffsetPixels(
            SceneEditorVerticalScrollbar.offsetFromDrag(
                mouseY,
                elementScrollbarGrabOffset,
                scrollbarBounds.y(),
                scrollbarBounds.height(),
                elementPanelScrollState.getContentPixels(),
                elementPanelScrollState.getViewportPixels()));
    }

    private LytRect getElementScrollbarBounds() {
        if (elementPanelScrollState.getMaxOffsetPixels() <= 0) {
            return LytRect.empty();
        }
        return new LytRect(
            elementsBoxX + elementsBoxWidth - ELEMENT_SCROLLBAR_WIDTH - 2,
            getElementViewportTop(),
            ELEMENT_SCROLLBAR_WIDTH,
            Math.max(0, getElementViewportBottom() - getElementViewportTop()));
    }

    @Nullable
    private SceneEditorVerticalScrollbar.Thumb getElementScrollbarThumb() {
        LytRect scrollbarBounds = getElementScrollbarBounds();
        return scrollbarBounds.isEmpty() ? null
            : SceneEditorVerticalScrollbar.computeThumb(
                scrollbarBounds.y(),
                scrollbarBounds.height(),
                elementPanelScrollState.getContentPixels(),
                elementPanelScrollState.getViewportPixels(),
                elementPanelScrollState.getOffsetPixels());
    }

    private void drawElementScrollbar() {
        LytRect scrollbarBounds = getElementScrollbarBounds();
        if (scrollbarBounds.isEmpty()) {
            return;
        }
        drawRect(
            scrollbarBounds.x(),
            scrollbarBounds.y(),
            scrollbarBounds.right(),
            scrollbarBounds.bottom(),
            0x35101010);
        SceneEditorVerticalScrollbar.Thumb thumb = getElementScrollbarThumb();
        if (thumb != null) {
            drawRect(scrollbarBounds.x(), thumb.start(), scrollbarBounds.right(), thumb.end(), 0xA0D8D8D8);
        }
    }

    private void beginScissor(int x, int y, int width, int height) {
        int scaleFactor = DisplayScale.scaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(
            x * scaleFactor,
            DisplayScale.scaledHeight() * scaleFactor - (y + height) * scaleFactor,
            width * scaleFactor,
            height * scaleFactor);
    }

    private void endScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private void drawElementTypeIcon(SceneEditorElementType type, int x, int y, boolean selected) {
        int borderColor = selected ? 0xFF00CAF2 : 0xFF46505A;
        drawRect(x, y, x + ELEMENT_ICON_SIZE, y + ELEMENT_ICON_SIZE, 0x33101012);
        drawBorder(x, y, ELEMENT_ICON_SIZE, ELEMENT_ICON_SIZE, borderColor);
        switch (type) {
            case BLOCK:
                drawRect(x + 3, y + 3, x + 11, y + 11, type.getAccentColor());
                break;
            case BOX:
                drawBorder(x + 2, y + 2, 10, 10, type.getAccentColor());
                drawBorder(x + 4, y + 4, 6, 6, 0x88FFFFFF);
                break;
            case LINE:
                for (int i = 0; i < 8; i++) {
                    drawRect(x + 3 + i, y + 10 - i, x + 4 + i, y + 11 - i, type.getAccentColor());
                }
                break;
            case DIAMOND:
                mc.getTextureManager()
                    .bindTexture(new ResourceLocation(type.getIconPngPath()));
                GL11.glColor4f(1f, 1f, 1f, 1f);
                Tessellator tess = Tessellator.instance;
                float uMax = 16f / 32f;
                tess.startDrawingQuads();
                tess.addVertexWithUV(x + 2, y + 12, 0, 0, 1);
                tess.addVertexWithUV(x + 12, y + 12, 0, uMax, 1);
                tess.addVertexWithUV(x + 12, y + 2, 0, uMax, 0);
                tess.addVertexWithUV(x + 2, y + 2, 0, 0, 0);
                tess.draw();
                break;
        }
    }

    private String formatVector(float x, float y, float z) {
        return formatFloat(x) + " " + formatFloat(y) + " " + formatFloat(z);
    }

    private String formatFloat(float value) {
        int rounded = Math.round(value);
        if (Math.abs(value - rounded) < 0.0001f) {
            return Integer.toString(rounded);
        }
        return Float.toString(value);
    }

    private GuideIconButton hoveredButton(int mouseX, int mouseY) {
        for (Object buttonObject : this.buttonList) {
            if (buttonObject instanceof GuideIconButton button && mouseX >= button.xPosition
                && mouseX < button.xPosition + button.width
                && mouseY >= button.yPosition
                && mouseY < button.yPosition + button.height) {
                return button;
            }
        }
        return null;
    }

    public class ElementEditorPanel {

        private final UUID elementId;
        private final SceneEditorElementType type;
        private final ElementInputField colorField;
        private final ElementInputField[] primaryFields;
        @Nullable
        private final ElementInputField[] secondaryFields;
        @Nullable
        private final ElementInputField thicknessField;
        private final SceneEditorDraftTextController tooltipController;
        private final SceneEditorMultilineTextArea tooltipArea;

        @Nullable
        private ElementInputField focusedField;
        private boolean tooltipFocused;
        private int x;
        private int y;
        private int width;
        private int height;
        private int alwaysOnTopX;
        private int alwaysOnTopY;
        private int alwaysOnTopLabelX;
        private int tooltipLabelY;

        private ElementEditorPanel(SceneEditorElementModel element) {
            this.elementId = element.getId();
            this.type = element.getType();
            this.colorField = new ElementInputField(
                GuidebookText.SceneEditorElementColor.text(),
                elementFieldKey("color"),
                new SceneEditorDraftTextController(
                    () -> readCurrentElement().getColorLiteral(),
                    true,
                    this::applyColorDraft));
            this.primaryFields = createVectorFields(primaryVectorLabel(type), true);
            this.secondaryFields = secondaryVectorLabel(type) == null ? null
                : createVectorFields(secondaryVectorLabel(type), false);
            this.thicknessField = supportsElementThickness(type)
                ? new ElementInputField(
                    GuidebookText.SceneEditorElementThickness.text(),
                    elementFieldKey("thickness"),
                    new SceneEditorDraftTextController(
                        () -> formatFloat(readCurrentElement().getThickness()),
                        true,
                        this::applyThicknessDraft))
                : null;
            this.tooltipController = new SceneEditorDraftTextController(
                () -> readCurrentElement().getTooltipMarkdown(),
                false,
                draft -> {
                    boolean applied = elementPropertyController.setTooltip(elementId, draft);
                    if (applied) {
                        onElementModelApplied();
                    }
                    return applied;
                });
            this.tooltipArea = new SceneEditorMultilineTextArea(SceneEditorScreen.this.fontRendererObj);
            this.focusedField = null;
            this.tooltipFocused = false;
            syncFromModel(element);
        }

        private void setBounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;

            int contentX = x + 6;
            int inputBoxX = contentX + ELEMENT_FIELD_LABEL_WIDTH;
            int inputWidth = Math.max(32, width - (inputBoxX - x) - 6);
            int rowY = y + 6;

            colorField.setBounds(contentX, rowY, inputBoxX, inputWidth);
            rowY += ELEMENT_FIELD_ROW_HEIGHT;

            for (ElementInputField field : primaryFields) {
                field.setBounds(contentX, rowY, inputBoxX, inputWidth);
                rowY += ELEMENT_FIELD_ROW_HEIGHT;
            }
            if (secondaryFields != null) {
                for (ElementInputField field : secondaryFields) {
                    field.setBounds(contentX, rowY, inputBoxX, inputWidth);
                    rowY += ELEMENT_FIELD_ROW_HEIGHT;
                }
            }
            if (thicknessField != null) {
                thicknessField.setBounds(contentX, rowY, inputBoxX, inputWidth);
                rowY += ELEMENT_FIELD_ROW_HEIGHT;
            }

            alwaysOnTopX = inputBoxX;
            alwaysOnTopY = rowY + 2;
            alwaysOnTopLabelX = contentX;
            rowY += ELEMENT_FIELD_ROW_HEIGHT;

            tooltipLabelY = rowY + 2;
            tooltipArea.setBounds(contentX, rowY + 12, width - 12, ELEMENT_TOOLTIP_HEIGHT);
        }

        private void draw(int mouseX, int mouseY) {
            colorField.draw();
            for (ElementInputField field : primaryFields) {
                field.draw();
            }
            if (secondaryFields != null) {
                for (ElementInputField field : secondaryFields) {
                    field.draw();
                }
            }
            if (thicknessField != null) {
                thicknessField.draw();
            }
            drawAlwaysOnTop();
            SceneEditorScreen.this.drawString(
                SceneEditorScreen.this.fontRendererObj,
                GuidebookText.SceneEditorElementTooltip.text(),
                x + 6,
                tooltipLabelY,
                PANEL_MUTED_TEXT);
            tooltipArea.draw(tooltipController.hasValidationError());
        }

        private void updateCursorCounter() {
            colorField.updateCursorCounter();
            for (ElementInputField field : primaryFields) {
                field.updateCursorCounter();
            }
            if (secondaryFields != null) {
                for (ElementInputField field : secondaryFields) {
                    field.updateCursorCounter();
                }
            }
            if (thicknessField != null) {
                thicknessField.updateCursorCounter();
            }
        }

        private boolean keyTyped(char typedChar, int keyCode) {
            if (tooltipFocused && tooltipArea.keyTyped(typedChar, keyCode)) {
                tooltipController.setDraftText(tooltipArea.getText());
                recordCurrentUiDraftSnapshot(tooltipUndoFieldKey());
                return true;
            }
            if (colorField.keyTyped(typedChar, keyCode)) {
                return true;
            }
            for (ElementInputField field : primaryFields) {
                if (field.keyTyped(typedChar, keyCode)) {
                    return true;
                }
            }
            if (secondaryFields != null) {
                for (ElementInputField field : secondaryFields) {
                    if (field.keyTyped(typedChar, keyCode)) {
                        return true;
                    }
                }
            }
            if (thicknessField != null && thicknessField.keyTyped(typedChar, keyCode)) {
                return true;
            }
            return false;
        }

        private boolean handleScrollWheel(int mouseX, int mouseY, int wheelDelta) {
            if (!tooltipArea.contains(mouseX, mouseY)) {
                return false;
            }
            tooltipArea.scrollWheel(wheelDelta);
            return true;
        }

        private boolean commitFocusedDraft(int mouseX, int mouseY) {
            if (focusedField != null && !focusedField.containsInput(mouseX, mouseY)) {
                if (!focusedField.commitDraft()) {
                    return false;
                }
                focusedField.setFocused(false);
                focusedField = null;
            }
            if (tooltipFocused && !tooltipArea.contains(mouseX, mouseY)) {
                tooltipController.setDraftText(tooltipArea.getText());
                if (!commitWithAppliedUndoContext(tooltipUndoFieldKey(), false, tooltipController::commitDraftText)) {
                    recordCurrentUiDraftSnapshot(tooltipUndoFieldKey(), false);
                    return false;
                }
                tooltipArea.setText(tooltipController.getDraftText());
                tooltipArea.setFocused(false);
                tooltipFocused = false;
            }
            return true;
        }

        private boolean hasFocusedInput() {
            return focusedField != null || tooltipFocused;
        }

        private boolean mouseClicked(int mouseX, int mouseY, int button) {
            if (colorField.containsInput(mouseX, mouseY)) {
                focusField(colorField, mouseX, mouseY, button);
                return true;
            }
            for (ElementInputField field : primaryFields) {
                if (field.containsInput(mouseX, mouseY)) {
                    focusField(field, mouseX, mouseY, button);
                    return true;
                }
            }
            if (secondaryFields != null) {
                for (ElementInputField field : secondaryFields) {
                    if (field.containsInput(mouseX, mouseY)) {
                        focusField(field, mouseX, mouseY, button);
                        return true;
                    }
                }
            }
            if (thicknessField != null && thicknessField.containsInput(mouseX, mouseY)) {
                focusField(thicknessField, mouseX, mouseY, button);
                return true;
            }
            if (button == 0 && isInsideAlwaysOnTopToggle(mouseX, mouseY)) {
                boolean nextValue = !readCurrentElement().isAlwaysOnTop();
                if (elementPropertyController.setAlwaysOnTop(elementId, nextValue)) {
                    onElementModelApplied();
                }
                return true;
            }
            if (tooltipArea.contains(mouseX, mouseY)) {
                clearFocusedField();
                tooltipFocused = true;
                tooltipArea.setFocused(true);
                if (button == 1) {
                    tooltipArea.setText("");
                    tooltipController.setDraftText("");
                    recordCurrentUiDraftSnapshot(tooltipUndoFieldKey());
                    return true;
                }
                if (button == 0) {
                    tooltipArea.mouseClicked(mouseX, mouseY, button);
                    tooltipController.setDraftText(tooltipArea.getText());
                    return true;
                }
                return true;
            }
            return false;
        }

        private void syncFromModel(SceneEditorElementModel element) {
            colorField.syncFromController();
            for (ElementInputField field : primaryFields) {
                field.syncFromController();
            }
            if (secondaryFields != null) {
                for (ElementInputField field : secondaryFields) {
                    field.syncFromController();
                }
            }
            if (thicknessField != null) {
                thicknessField.syncFromController();
            }
            tooltipController.syncFromAppliedValue();
            tooltipArea.setText(tooltipController.getDraftText());
        }

        private void captureUndoState(SceneEditorUndoUiState.Builder builder) {
            colorField.captureUndoState(builder);
            for (ElementInputField field : primaryFields) {
                field.captureUndoState(builder);
            }
            if (secondaryFields != null) {
                for (ElementInputField field : secondaryFields) {
                    field.captureUndoState(builder);
                }
            }
            if (thicknessField != null) {
                thicknessField.captureUndoState(builder);
            }
            builder.put(
                tooltipUndoFieldKey(),
                new SceneEditorUndoFieldState(
                    tooltipController.getDraftText(),
                    tooltipController.hasValidationError()));
        }

        private void restoreUndoState(SceneEditorUndoUiState uiState) {
            colorField.restoreUndoState(uiState);
            for (ElementInputField field : primaryFields) {
                field.restoreUndoState(uiState);
            }
            if (secondaryFields != null) {
                for (ElementInputField field : secondaryFields) {
                    field.restoreUndoState(uiState);
                }
            }
            if (thicknessField != null) {
                thicknessField.restoreUndoState(uiState);
            }
            uiState.getField(tooltipUndoFieldKey())
                .ifPresent(state -> {
                    tooltipController.restoreDraftState(state.getDraftText(), state.hasValidationError());
                    tooltipArea.setText(tooltipController.getDraftText());
                });
        }

        private SceneEditorElementModel readCurrentElement() {
            return session.getSceneModel()
                .getElement(elementId)
                .orElseThrow(() -> new IllegalStateException("Expanded element no longer exists"));
        }

        private boolean applyColorDraft(String draft) {
            boolean applied = elementPropertyController.setColor(elementId, draft);
            if (applied) {
                onElementModelApplied();
            }
            return applied;
        }

        private ElementInputField[] createVectorFields(GuidebookText baseLabel, boolean primary) {
            return new ElementInputField[] { createAxisField(baseLabel.text() + " X", primary, 0),
                createAxisField(baseLabel.text() + " Y", primary, 1),
                createAxisField(baseLabel.text() + " Z", primary, 2) };
        }

        private ElementInputField createAxisField(String label, boolean primary, int axisIndex) {
            String axisKey = primary ? "primary-" : "secondary-";
            axisKey += axisIndex == 0 ? "x" : axisIndex == 1 ? "y" : "z";
            return new ElementInputField(
                label,
                elementFieldKey(axisKey),
                new SceneEditorDraftTextController(
                    () -> formatFloat(getAxisValue(readCurrentElement(), primary, axisIndex)),
                    true,
                    draft -> applyAxisDraft(primary, axisIndex, draft)));
        }

        private float getAxisValue(SceneEditorElementModel element, boolean primary, int axisIndex) {
            if (primary) {
                if (axisIndex == 0) {
                    return element.getPrimaryX();
                }
                if (axisIndex == 1) {
                    return element.getPrimaryY();
                }
                return element.getPrimaryZ();
            }
            if (axisIndex == 0) {
                return element.getSecondaryX();
            }
            if (axisIndex == 1) {
                return element.getSecondaryY();
            }
            return element.getSecondaryZ();
        }

        private boolean applyAxisDraft(boolean primary, int axisIndex, String draft) {
            float value;
            try {
                value = Float.parseFloat(draft.trim());
            } catch (NumberFormatException e) {
                return false;
            }
            SceneEditorElementModel element = readCurrentElement();
            float x = primary ? element.getPrimaryX() : element.getSecondaryX();
            float y = primary ? element.getPrimaryY() : element.getSecondaryY();
            float z = primary ? element.getPrimaryZ() : element.getSecondaryZ();
            if (axisIndex == 0) {
                x = value;
            } else if (axisIndex == 1) {
                y = value;
            } else {
                z = value;
            }
            boolean applied = primary ? elementPropertyController.setPrimaryVector(elementId, x, y, z)
                : elementPropertyController.setSecondaryVector(elementId, x, y, z);
            if (applied) {
                onElementModelApplied();
            }
            return applied;
        }

        private boolean applyThicknessDraft(String draft) {
            float value;
            try {
                value = Float.parseFloat(draft.trim());
            } catch (NumberFormatException e) {
                return false;
            }
            boolean applied = elementPropertyController.setThickness(elementId, value);
            if (applied) {
                onElementModelApplied();
            }
            return applied;
        }

        private String elementFieldKey(String fieldName) {
            return "element:" + elementId + ":" + fieldName;
        }

        private String tooltipUndoFieldKey() {
            return elementFieldKey("tooltip");
        }

        private void focusField(ElementInputField field, int mouseX, int mouseY, int button) {
            tooltipFocused = false;
            tooltipArea.setFocused(false);
            if (focusedField != null && focusedField != field) {
                focusedField.setFocused(false);
            }
            focusedField = field;
            field.mouseClicked(mouseX, mouseY, button);
        }

        private void clearFocusedField() {
            if (focusedField != null) {
                focusedField.setFocused(false);
                focusedField = null;
            }
        }

        private boolean isInsideAlwaysOnTopToggle(int mouseX, int mouseY) {
            int rowRight = x + width - 6;
            int rowBottom = alwaysOnTopY + INTERACTIVE_ROW_HEIGHT;
            return mouseX >= alwaysOnTopLabelX && mouseX < rowRight && mouseY >= alwaysOnTopY && mouseY < rowBottom;
        }

        private void drawAlwaysOnTop() {
            int boxRight = alwaysOnTopX + INTERACTIVE_CHECKBOX_SIZE;
            int boxBottom = alwaysOnTopY + INTERACTIVE_CHECKBOX_SIZE;
            drawRect(alwaysOnTopX, alwaysOnTopY, boxRight, boxBottom, CHECKBOX_BACKGROUND_COLOR);
            drawBorder(
                alwaysOnTopX,
                alwaysOnTopY,
                INTERACTIVE_CHECKBOX_SIZE,
                INTERACTIVE_CHECKBOX_SIZE,
                INPUT_BORDER_COLOR);
            if (readCurrentElement().isAlwaysOnTop()) {
                drawRect(alwaysOnTopX + 3, alwaysOnTopY + 3, boxRight - 3, boxBottom - 3, CHECKBOX_CHECK_COLOR);
            }
            SceneEditorScreen.this.drawString(
                SceneEditorScreen.this.fontRendererObj,
                GuidebookText.SceneEditorElementAlwaysOnTop.text(),
                alwaysOnTopLabelX,
                alwaysOnTopY + 2,
                PANEL_MUTED_TEXT);
        }

        private GuidebookText primaryVectorLabel(SceneEditorElementType elementType) {
            return switch (elementType) {
                case BOX -> GuidebookText.SceneEditorElementMin;
                case LINE -> GuidebookText.SceneEditorElementFrom;
                default -> GuidebookText.SceneEditorElementPosition;
            };
        }

        @Nullable
        private GuidebookText secondaryVectorLabel(SceneEditorElementType elementType) {
            return switch (elementType) {
                case BOX -> GuidebookText.SceneEditorElementMax;
                case LINE -> GuidebookText.SceneEditorElementTo;
                default -> null;
            };
        }
    }

    public class ElementInputField {

        private final String label;
        private final String undoFieldKey;
        private final SceneEditorDraftTextController controller;
        private final GuiTextField inputField;
        private int labelX;
        private int rowY;
        private int inputBoxX;
        private int inputBoxY;
        private int inputBoxWidth;

        private ElementInputField(String label, String undoFieldKey, SceneEditorDraftTextController controller) {
            this.label = label;
            this.undoFieldKey = undoFieldKey;
            this.controller = controller;
            this.inputField = new GuiTextField(SceneEditorScreen.this.fontRendererObj, 0, 0, 0, PARAMETER_INPUT_HEIGHT);
            this.inputField.setEnableBackgroundDrawing(false);
            this.inputField.setMaxStringLength(128);
            this.inputField.setText(controller.getDraftText());
        }

        private void setBounds(int labelX, int rowY, int inputBoxX, int inputBoxWidth) {
            this.labelX = labelX;
            this.rowY = rowY;
            this.inputBoxX = inputBoxX;
            this.inputBoxY = rowY - 1;
            this.inputBoxWidth = inputBoxWidth;
            this.inputField.xPosition = inputBoxX + 4;
            this.inputField.yPosition = rowY + 3;
            this.inputField.width = Math.max(8, inputBoxWidth - 8);
            this.inputField.height = PARAMETER_INPUT_HEIGHT;
        }

        private void draw() {
            SceneEditorScreen.this
                .drawString(SceneEditorScreen.this.fontRendererObj, label, labelX, rowY + 2, PANEL_MUTED_TEXT);

            int borderColor = controller.hasValidationError() ? INPUT_ERROR_BORDER_COLOR
                : inputField.isFocused() ? INPUT_FOCUSED_BORDER_COLOR : INPUT_BORDER_COLOR;
            drawRect(
                inputBoxX,
                inputBoxY,
                inputBoxX + inputBoxWidth,
                inputBoxY + PARAMETER_INPUT_HEIGHT + 2,
                INPUT_BACKGROUND_COLOR);
            drawBorder(inputBoxX, inputBoxY, inputBoxWidth, PARAMETER_INPUT_HEIGHT + 2, borderColor);
            drawCompactTextFieldValue(inputField, controller.getDraftText());
        }

        private void updateCursorCounter() {
            inputField.updateCursorCounter();
        }

        private boolean keyTyped(char typedChar, int keyCode) {
            if (!inputField.isFocused()) {
                return false;
            }
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                return commitDraft();
            }
            boolean handled = inputField.textboxKeyTyped(typedChar, keyCode);
            if (handled) {
                controller.setDraftText(inputField.getText());
                recordCurrentUiDraftSnapshot(undoFieldKey);
            }
            return handled;
        }

        private void mouseClicked(int mouseX, int mouseY, int button) {
            if (button == 1) {
                inputField.setFocused(true);
                inputField.setText("");
                controller.setDraftText("");
                recordCurrentUiDraftSnapshot(undoFieldKey);
                return;
            }
            inputField.mouseClicked(mouseX, mouseY, button);
            controller.setDraftText(inputField.getText());
        }

        private boolean commitDraft() {
            controller.setDraftText(inputField.getText());
            if (!commitWithAppliedUndoContext(undoFieldKey, false, controller::commitDraftText)) {
                recordCurrentUiDraftSnapshot(undoFieldKey, false);
                return false;
            }
            inputField.setText(controller.getDraftText());
            return true;
        }

        private void syncFromController() {
            controller.syncFromAppliedValue();
            inputField.setText(controller.getDraftText());
        }

        private void captureUndoState(SceneEditorUndoUiState.Builder builder) {
            builder.put(
                undoFieldKey,
                new SceneEditorUndoFieldState(controller.getDraftText(), controller.hasValidationError()));
        }

        private void restoreUndoState(SceneEditorUndoUiState uiState) {
            uiState.getField(undoFieldKey)
                .ifPresent(state -> {
                    controller.restoreDraftState(state.getDraftText(), state.hasValidationError());
                    inputField.setText(controller.getDraftText());
                });
        }

        private void setFocused(boolean focused) {
            inputField.setFocused(focused);
        }

        private boolean containsInput(int mouseX, int mouseY) {
            return mouseX >= inputBoxX && mouseX < inputBoxX + inputBoxWidth
                && mouseY >= inputBoxY
                && mouseY < inputBoxY + PARAMETER_INPUT_HEIGHT + 2;
        }
    }

    @FunctionalInterface
    private interface FloatValueSetter {

        void setValue(float value);
    }

    @FunctionalInterface
    private interface IntValueSetter {

        void setValue(int value);
    }

    private interface FloatModelValueProvider {

        float getValue();
    }

    public static boolean isCtrlKeyCombo(int keyCode, int expectedKeyCode) {
        return keyCode == expectedKeyCode
            && (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL));
    }

    public static boolean isCtrlShiftKeyCombo(int keyCode, int expectedKeyCode) {
        return isCtrlKeyCombo(keyCode, expectedKeyCode) && GuiScreen.isShiftKeyDown();
    }

    private enum ElementContextMenuAction {

        CUT,
        COPY,
        PASTE,
        TOGGLE_VISIBILITY,
        RANDOMIZE_COLOR,
        DELETE;

        private GuidebookText getText(SceneEditorElementModel element) {
            return switch (this) {
                case CUT -> GuidebookText.SceneEditorCutElement;
                case COPY -> GuidebookText.SceneEditorCopyElement;
                case PASTE -> GuidebookText.SceneEditorPasteElement;
                case TOGGLE_VISIBILITY -> element.isVisible() ? GuidebookText.SceneEditorHideElement
                    : GuidebookText.SceneEditorShowElement;
                case RANDOMIZE_COLOR -> GuidebookText.SceneEditorRandomizeElementColor;
                default -> GuidebookText.SceneEditorDeleteElement;
            };
        }
    }

    private enum SnapModeOption {

        LINE(GuidebookText.SceneEditorSnapLine) {

            @Override
            boolean isEnabled() {
                return ModConfig.ui.sceneEditorSnapLineEnabled;
            }

            @Override
            void toggle() {
                ModConfig.ui.sceneEditorSnapLineEnabled = !ModConfig.ui.sceneEditorSnapLineEnabled;
            }
        },
        POINT(GuidebookText.SceneEditorSnapPoint) {

            @Override
            boolean isEnabled() {
                return ModConfig.ui.sceneEditorSnapPointEnabled;
            }

            @Override
            void toggle() {
                ModConfig.ui.sceneEditorSnapPointEnabled = !ModConfig.ui.sceneEditorSnapPointEnabled;
            }
        },
        FACE(GuidebookText.SceneEditorSnapFace) {

            @Override
            boolean isEnabled() {
                return ModConfig.ui.sceneEditorSnapFaceEnabled;
            }

            @Override
            void toggle() {
                ModConfig.ui.sceneEditorSnapFaceEnabled = !ModConfig.ui.sceneEditorSnapFaceEnabled;
            }
        },
        CENTER(GuidebookText.SceneEditorSnapCenter) {

            @Override
            boolean isEnabled() {
                return ModConfig.ui.sceneEditorSnapCenterEnabled;
            }

            @Override
            void toggle() {
                ModConfig.ui.sceneEditorSnapCenterEnabled = !ModConfig.ui.sceneEditorSnapCenterEnabled;
            }
        };

        private final GuidebookText text;

        SnapModeOption(GuidebookText text) {
            this.text = text;
        }

        private GuidebookText text() {
            return text;
        }

        abstract boolean isEnabled();

        abstract void toggle();
    }

    private enum CloseConfirmAction {

        SAVE(GuidebookText.SceneEditorSave),
        DISCARD(GuidebookText.SceneEditorDiscard),
        CANCEL(GuidebookText.SceneEditorCancel);

        private final GuidebookText text;

        CloseConfirmAction(GuidebookText text) {
            this.text = text;
        }

        private GuidebookText text() {
            return text;
        }
    }

    private enum ElementHitPart {
        ARROW,
        DELETE,
        VISIBILITY,
        ROW,
        BODY
    }

    public static class ElementHit {

        private final SceneEditorElementModel element;
        private final int index;
        private final ElementHitPart part;

        private ElementHit(SceneEditorElementModel element, int index, ElementHitPart part) {
            this.element = element;
            this.index = index;
            this.part = part;
        }
    }

    public static class ElementRowLayout {

        private final SceneEditorElementModel element;
        private final int index;
        private final int rowX;
        private final int rowY;
        private final int rowWidth;
        private final int totalHeight;

        private ElementRowLayout(SceneEditorElementModel element, int index, int rowX, int rowY, int rowWidth,
            int totalHeight) {
            this.element = element;
            this.index = index;
            this.rowX = rowX;
            this.rowY = rowY;
            this.rowWidth = rowWidth;
            this.totalHeight = totalHeight;
        }
    }

    public class NumericParameterRow {

        private final GuidebookText label;
        private final SceneEditorNumericFieldController controller;
        private final FloatModelValueProvider modelValueProvider;
        private final GuiTextField inputField;
        private final float minValue;
        private final float maxValue;
        private int labelX;
        private int rowY;
        private int inputBoxX;
        private int inputBoxY;
        private int inputBoxWidth;
        private int sliderX;
        private int sliderY;
        private int sliderWidth;

        private NumericParameterRow(GuidebookText label, SceneEditorNumericFieldController controller, float minValue,
            float maxValue, FloatModelValueProvider modelValueProvider) {
            this.label = label;
            this.controller = controller;
            this.modelValueProvider = modelValueProvider;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.inputField = new GuiTextField(SceneEditorScreen.this.fontRendererObj, 0, 0, 0, PARAMETER_INPUT_HEIGHT);
            this.inputField.setEnableBackgroundDrawing(false);
            this.inputField.setMaxStringLength(64);
            this.inputField.setText(controller.getDraftText());
        }

        private void setBounds(int labelX, int rowY, int inputBoxX, int inputBoxWidth, int sliderX, int sliderWidth) {
            this.labelX = labelX;
            this.rowY = rowY;
            this.inputBoxX = inputBoxX;
            this.inputBoxY = rowY - 1;
            this.inputBoxWidth = inputBoxWidth;
            this.sliderX = sliderX;
            this.sliderY = rowY + PARAMETER_SLIDER_Y_OFFSET;
            this.sliderWidth = sliderWidth;
            this.inputField.xPosition = inputBoxX + 4;
            this.inputField.yPosition = rowY + 3;
            this.inputField.width = Math.max(8, inputBoxWidth - 8);
            this.inputField.height = PARAMETER_INPUT_HEIGHT;
        }

        private void draw(int mouseX, int mouseY) {
            SceneEditorScreen.this
                .drawString(SceneEditorScreen.this.fontRendererObj, label.text(), labelX, rowY + 2, PANEL_MUTED_TEXT);

            int borderColor = controller.hasValidationError() ? INPUT_ERROR_BORDER_COLOR
                : inputField.isFocused() ? INPUT_FOCUSED_BORDER_COLOR : INPUT_BORDER_COLOR;
            drawRect(
                inputBoxX,
                inputBoxY,
                inputBoxX + inputBoxWidth,
                inputBoxY + PARAMETER_INPUT_HEIGHT + 2,
                INPUT_BACKGROUND_COLOR);
            drawBorder(inputBoxX, inputBoxY, inputBoxWidth, PARAMETER_INPUT_HEIGHT + 2, borderColor);
            drawCompactTextFieldValue(inputField, controller.getDraftText());

            GuideSliderRenderer.render(
                Gui::drawRect,
                GuideSliderRenderer.layout(sliderX, sliderY, sliderWidth, controller.getSliderFraction()),
                containsSlider(mouseX, mouseY) || activeSliderRow == this);
        }

        private void updateCursorCounter() {
            inputField.updateCursorCounter();
        }

        private boolean applyMouseWheel(int wheelDelta) {
            if (!inputField.isFocused() || wheelDelta == 0) {
                return false;
            }
            controller.nudgeByWheel(wheelDelta);
            inputField.setText(controller.getDraftText());
            return true;
        }

        private boolean keyTyped(char typedChar, int keyCode) {
            if (!inputField.isFocused()) {
                return false;
            }
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                commitDraft();
                return true;
            }
            boolean handled = inputField.textboxKeyTyped(typedChar, keyCode);
            if (handled) {
                controller.setDraftText(inputField.getText());
                recordCurrentUiDraftSnapshot(undoFieldKey());
            }
            return handled;
        }

        private void mouseClicked(int mouseX, int mouseY, int button) {
            if (button == 1) {
                inputField.setFocused(true);
                inputField.setText("");
                controller.setDraftText("");
                recordCurrentUiDraftSnapshot(undoFieldKey());
                return;
            }
            inputField.mouseClicked(mouseX, mouseY, button);
            controller.setDraftText(inputField.getText());
        }

        private boolean commitDraft() {
            controller.setDraftText(inputField.getText());
            if (!commitWithAppliedUndoContext(undoFieldKey(), false, controller::commitDraftText)) {
                recordCurrentUiDraftSnapshot(undoFieldKey(), false);
                return false;
            }
            inputField.setText(controller.getDraftText());
            return true;
        }

        private void syncFromModel() {
            controller.syncFromModel(modelValueProvider.getValue());
            inputField.setText(controller.getDraftText());
        }

        private void captureUndoState(SceneEditorUndoUiState.Builder builder) {
            builder.put(
                undoFieldKey(),
                new SceneEditorUndoFieldState(controller.getDraftText(), controller.hasValidationError()));
        }

        private void restoreUndoState(SceneEditorUndoUiState uiState) {
            uiState.getField(undoFieldKey())
                .ifPresent(state -> {
                    controller.restoreDraftState(state.getDraftText(), state.hasValidationError());
                    inputField.setText(controller.getDraftText());
                });
        }

        private void setFocused(boolean focused) {
            inputField.setFocused(focused);
        }

        private boolean containsInput(int mouseX, int mouseY) {
            return mouseX >= inputBoxX && mouseX < inputBoxX + inputBoxWidth
                && mouseY >= inputBoxY
                && mouseY < inputBoxY + PARAMETER_INPUT_HEIGHT + 2;
        }

        private boolean containsSlider(int mouseX, int mouseY) {
            return GuideSliderRenderer.layout(sliderX, sliderY, sliderWidth, controller.getSliderFraction())
                .hitRect()
                .contains(mouseX, mouseY);
        }

        private void applySliderAt(int mouseX) {
            float value = minValue
                + (maxValue - minValue) * GuideSliderRenderer.fractionFromMouse(mouseX, sliderX, sliderWidth);
            controller.applySliderValue(value);
            inputField.setText(controller.getDraftText());
        }

        private String undoFieldKey() {
            return "parameter:" + label.name();
        }
    }
}
