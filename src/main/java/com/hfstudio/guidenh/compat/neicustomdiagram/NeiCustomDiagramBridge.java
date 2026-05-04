package com.hfstudio.guidenh.compat.neicustomdiagram;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.ItemTooltip;
import com.hfstudio.guidenh.guide.document.interaction.TextTooltip;
import com.hfstudio.guidenh.guide.internal.recipe.NeiHandlerRenderer;
import com.hfstudio.guidenh.guide.internal.tooltip.AppendedItemTooltip;
import com.hfstudio.guidenh.guide.internal.util.DisplayScale;

public final class NeiCustomDiagramBridge {

    private static final Logger LOG = LoggerFactory.getLogger(NeiCustomDiagramBridge.class);

    private static final String DIAGRAM_GROUP_CLASS_NAME = "com.github.dcysteine.neicustomdiagram.api.diagram.DiagramGroup";
    private static final String DIAGRAM_CLASS_NAME = "com.github.dcysteine.neicustomdiagram.api.diagram.Diagram";
    private static final String DIAGRAM_STATE_CLASS_NAME = "com.github.dcysteine.neicustomdiagram.api.diagram.DiagramState";
    private static final String INTERACTABLE_CLASS_NAME = "com.github.dcysteine.neicustomdiagram.api.diagram.interactable.Interactable";
    private static final String SLOT_CLASS_NAME = "com.github.dcysteine.neicustomdiagram.api.diagram.layout.Slot";
    private static final String INTERACTIVE_COMPONENT_GROUP_CLASS_NAME = "com.github.dcysteine.neicustomdiagram.api.diagram.interactable.InteractiveComponentGroup";
    private static final String CUSTOM_INTERACTABLE_CLASS_NAME = "com.github.dcysteine.neicustomdiagram.api.diagram.interactable.CustomInteractable";
    private static final String DISPLAY_COMPONENT_CLASS_NAME = "com.github.dcysteine.neicustomdiagram.api.diagram.component.DisplayComponent";
    private static final String COMPONENT_LABEL_CLASS_NAME = "com.github.dcysteine.neicustomdiagram.api.diagram.layout.ComponentLabel";
    private static final String POINT_CLASS_NAME = "com.github.dcysteine.neicustomdiagram.api.draw.Point";
    private static final String DRAW_CLASS_NAME = "com.github.dcysteine.neicustomdiagram.api.draw.Draw";
    private static final String TOOLTIP_CLASS_NAME = "com.github.dcysteine.neicustomdiagram.api.diagram.tooltip.Tooltip";
    private static final String TOOLTIP_LINE_CLASS_NAME = "com.github.dcysteine.neicustomdiagram.api.diagram.tooltip.TooltipLine";
    private static final String TOOLTIP_ELEMENT_CLASS_NAME = "com.github.dcysteine.neicustomdiagram.api.diagram.tooltip.TooltipElement";
    private static final String COMPONENT_CLASS_NAME = "com.github.dcysteine.neicustomdiagram.api.diagram.component.Component";
    private static final String CONFIG_OPTIONS_CLASS_NAME = "com.github.dcysteine.neicustomdiagram.main.config.ConfigOptions";

    private static final boolean AVAILABLE;
    private static final Class<?> CLASS_DIAGRAM_GROUP;
    private static final Class<?> CLASS_DIAGRAM;
    private static final Class<?> CLASS_DIAGRAM_STATE;
    private static final Class<?> CLASS_INTERACTABLE;
    private static final Class<?> CLASS_SLOT;
    private static final Class<?> CLASS_INTERACTIVE_COMPONENT_GROUP;
    private static final Class<?> CLASS_CUSTOM_INTERACTABLE;
    private static final Class<?> CLASS_DISPLAY_COMPONENT;
    private static final Class<?> CLASS_COMPONENT_LABEL;
    private static final Class<?> CLASS_POINT;
    private static final Class<?> CLASS_DRAW;
    private static final Class<?> CLASS_TOOLTIP;
    private static final Class<?> CLASS_TOOLTIP_LINE;
    private static final Class<?> CLASS_TOOLTIP_ELEMENT;
    private static final Class<?> CLASS_COMPONENT;

    private static final Field FIELD_DIAGRAMS;
    private static final Field FIELD_DIAGRAM_STATE;
    private static final Field FIELD_SLOT_TOOLTIP;
    private static final Field FIELD_CUSTOM_INTERACTABLE_DRAW_BACKGROUND;
    private static final Field FIELD_CUSTOM_INTERACTABLE_DRAW_FOREGROUND;
    private static final Field FIELD_CONFIG_SHOW_STACK_SIZE_ONE;

    private static final Method METHOD_POINT_CREATE;
    private static final Method METHOD_POINT_X;
    private static final Method METHOD_POINT_Y;
    private static final Method METHOD_DIAGRAM_DRAW_BACKGROUND;
    private static final Method METHOD_DIAGRAM_DRAW_FOREGROUND;
    private static final Method METHOD_DIAGRAM_INTERACTABLES;
    private static final Method METHOD_INTERACTABLE_DRAW;
    private static final Method METHOD_INTERACTABLE_POSITION;
    private static final Method METHOD_INTERACTABLE_CHECK_BOUNDING_BOX;
    private static final Method METHOD_INTERACTIVE_GROUP_CURRENT_COMPONENT;
    private static final Method METHOD_INTERACTIVE_GROUP_CYCLE_TOOLTIP;
    private static final Method METHOD_CUSTOM_INTERACTABLE_TOOLTIP;
    private static final Method METHOD_CUSTOM_INTERACTABLE_DRAWABLE;
    private static final Method METHOD_DISPLAY_COMPONENT_DRAW;
    private static final Method METHOD_DISPLAY_COMPONENT_STACK;
    private static final Method METHOD_DISPLAY_COMPONENT_STACK_SIZE;
    private static final Method METHOD_DISPLAY_COMPONENT_ADDITIONAL_INFO;
    private static final Method METHOD_DISPLAY_COMPONENT_DESCRIPTION_TOOLTIP;
    private static final Method METHOD_DISPLAY_COMPONENT_ADDITIONAL_TOOLTIP;
    private static final Method METHOD_COMPONENT_LABEL_COMPONENT;
    private static final Method METHOD_COMPONENT_DRAW;
    private static final Method METHOD_COMPONENT_STACK;
    private static final Method METHOD_TOOLTIP_LINES;
    private static final Method METHOD_TOOLTIP_LINE_ELEMENTS;
    private static final Method METHOD_TOOLTIP_ELEMENT_TYPE;
    private static final Method METHOD_TOOLTIP_ELEMENT_TEXT;
    private static final Method METHOD_TOOLTIP_ELEMENT_COMPONENT_DESCRIPTION;
    private static final Method METHOD_COMPONENT_DESCRIPTION;
    private static final Method METHOD_DRAW_STACK_SIZE;
    private static final Method METHOD_DRAW_ADDITIONAL_INFO;
    private static final Method METHOD_CONFIG_OPTION_GET;

    static {
        boolean available = false;
        Class<?> diagramGroup = null;
        Class<?> diagram = null;
        Class<?> diagramState = null;
        Class<?> interactable = null;
        Class<?> slot = null;
        Class<?> interactiveComponentGroup = null;
        Class<?> customInteractable = null;
        Class<?> displayComponent = null;
        Class<?> componentLabel = null;
        Class<?> point = null;
        Class<?> draw = null;
        Class<?> tooltip = null;
        Class<?> tooltipLine = null;
        Class<?> tooltipElement = null;
        Class<?> component = null;

        Field diagrams = null;
        Field state = null;
        Field slotTooltip = null;
        Field customInteractableDrawBackground = null;
        Field customInteractableDrawForeground = null;
        Field showStackSizeOne = null;

        Method pointCreate = null;
        Method pointX = null;
        Method pointY = null;
        Method drawBackground = null;
        Method drawForeground = null;
        Method interactables = null;
        Method interactableDraw = null;
        Method interactablePosition = null;
        Method checkBoundingBox = null;
        Method currentComponent = null;
        Method cycleTooltip = null;
        Method customTooltip = null;
        Method customDrawable = null;
        Method displayComponentDraw = null;
        Method componentStack = null;
        Method displayStackSize = null;
        Method displayAdditionalInfo = null;
        Method descriptionTooltip = null;
        Method additionalTooltip = null;
        Method componentLabelComponent = null;
        Method componentDraw = null;
        Method componentStackMethod = null;
        Method tooltipLines = null;
        Method tooltipLineElements = null;
        Method tooltipElementType = null;
        Method tooltipElementText = null;
        Method tooltipElementComponentDescription = null;
        Method componentDescription = null;
        Method drawStackSize = null;
        Method drawAdditionalInfo = null;
        Method configOptionGet = null;

        try {
            if (!com.hfstudio.guidenh.compat.Mods.NeiCustomDiagram.isModLoaded()) {
                throw new ClassNotFoundException("nei-custom-diagram is not loaded");
            }
            diagramGroup = Class.forName(DIAGRAM_GROUP_CLASS_NAME);
            diagram = Class.forName(DIAGRAM_CLASS_NAME);
            diagramState = Class.forName(DIAGRAM_STATE_CLASS_NAME);
            interactable = Class.forName(INTERACTABLE_CLASS_NAME);
            slot = Class.forName(SLOT_CLASS_NAME);
            interactiveComponentGroup = Class.forName(INTERACTIVE_COMPONENT_GROUP_CLASS_NAME);
            customInteractable = Class.forName(CUSTOM_INTERACTABLE_CLASS_NAME);
            displayComponent = Class.forName(DISPLAY_COMPONENT_CLASS_NAME);
            componentLabel = Class.forName(COMPONENT_LABEL_CLASS_NAME);
            point = Class.forName(POINT_CLASS_NAME);
            draw = Class.forName(DRAW_CLASS_NAME);
            tooltip = Class.forName(TOOLTIP_CLASS_NAME);
            tooltipLine = Class.forName(TOOLTIP_LINE_CLASS_NAME);
            tooltipElement = Class.forName(TOOLTIP_ELEMENT_CLASS_NAME);
            component = Class.forName(COMPONENT_CLASS_NAME);
            Class<?> configOptions = Class.forName(CONFIG_OPTIONS_CLASS_NAME);

            diagrams = findField(diagramGroup, "diagrams");
            state = findField(diagramGroup, "diagramState");
            slotTooltip = findField(interactiveComponentGroup, "slotTooltip");
            customInteractableDrawBackground = findField(customInteractable, "drawBackground");
            customInteractableDrawForeground = findField(customInteractable, "drawForeground");
            showStackSizeOne = configOptions.getField("SHOW_STACK_SIZE_ONE");

            pointCreate = point.getMethod("create", int.class, int.class);
            pointX = point.getMethod("x");
            pointY = point.getMethod("y");
            drawBackground = diagram.getMethod("drawBackground", diagramState);
            drawForeground = diagram.getMethod("drawForeground", diagramState);
            interactables = diagram.getMethod("interactables", diagramState);
            interactableDraw = interactable.getMethod("draw", diagramState);
            interactablePosition = interactable.getMethod("position");
            checkBoundingBox = interactable.getMethod("checkBoundingBox", point);
            currentComponent = interactiveComponentGroup.getMethod("currentComponent", diagramState);
            cycleTooltip = interactiveComponentGroup.getMethod("cycleTooltip", diagramState);
            customTooltip = customInteractable.getMethod("tooltip");
            customDrawable = customInteractable.getMethod("drawable");
            displayComponentDraw = displayComponent.getMethod("draw", point);
            componentStack = displayComponent.getMethod("stack");
            displayStackSize = displayComponent.getMethod("stackSize");
            displayAdditionalInfo = displayComponent.getMethod("additionalInfo");
            descriptionTooltip = displayComponent.getMethod("descriptionTooltip");
            additionalTooltip = displayComponent.getMethod("additionalTooltip");
            componentLabelComponent = componentLabel.getMethod("component");
            componentDraw = component.getMethod("draw", point);
            componentStackMethod = component.getMethod("stack");
            tooltipLines = tooltip.getMethod("lines");
            tooltipLineElements = tooltipLine.getMethod("elements");
            tooltipElementType = tooltipElement.getMethod("type");
            tooltipElementText = tooltipElement.getMethod("text");
            tooltipElementComponentDescription = tooltipElement.getMethod("componentDescription");
            componentDescription = component.getMethod("description");
            drawStackSize = draw.getMethod("drawStackSize", int.class, point);
            drawAdditionalInfo = draw.getMethod("drawAdditionalInfo", String.class, point, boolean.class);
            configOptionGet = showStackSizeOne.getType()
                .getMethod("get");
            available = true;
        } catch (Throwable t) {
            LOG.debug("nei-custom-diagram bridge unavailable: {}", t.toString());
        }

        AVAILABLE = available;
        CLASS_DIAGRAM_GROUP = diagramGroup;
        CLASS_DIAGRAM = diagram;
        CLASS_DIAGRAM_STATE = diagramState;
        CLASS_INTERACTABLE = interactable;
        CLASS_SLOT = slot;
        CLASS_INTERACTIVE_COMPONENT_GROUP = interactiveComponentGroup;
        CLASS_CUSTOM_INTERACTABLE = customInteractable;
        CLASS_DISPLAY_COMPONENT = displayComponent;
        CLASS_COMPONENT_LABEL = componentLabel;
        CLASS_POINT = point;
        CLASS_DRAW = draw;
        CLASS_TOOLTIP = tooltip;
        CLASS_TOOLTIP_LINE = tooltipLine;
        CLASS_TOOLTIP_ELEMENT = tooltipElement;
        CLASS_COMPONENT = component;
        FIELD_DIAGRAMS = diagrams;
        FIELD_DIAGRAM_STATE = state;
        FIELD_SLOT_TOOLTIP = slotTooltip;
        FIELD_CUSTOM_INTERACTABLE_DRAW_BACKGROUND = customInteractableDrawBackground;
        FIELD_CUSTOM_INTERACTABLE_DRAW_FOREGROUND = customInteractableDrawForeground;
        FIELD_CONFIG_SHOW_STACK_SIZE_ONE = showStackSizeOne;
        METHOD_POINT_CREATE = pointCreate;
        METHOD_POINT_X = pointX;
        METHOD_POINT_Y = pointY;
        METHOD_DIAGRAM_DRAW_BACKGROUND = drawBackground;
        METHOD_DIAGRAM_DRAW_FOREGROUND = drawForeground;
        METHOD_DIAGRAM_INTERACTABLES = interactables;
        METHOD_INTERACTABLE_DRAW = interactableDraw;
        METHOD_INTERACTABLE_POSITION = interactablePosition;
        METHOD_INTERACTABLE_CHECK_BOUNDING_BOX = checkBoundingBox;
        METHOD_INTERACTIVE_GROUP_CURRENT_COMPONENT = currentComponent;
        METHOD_INTERACTIVE_GROUP_CYCLE_TOOLTIP = cycleTooltip;
        METHOD_CUSTOM_INTERACTABLE_TOOLTIP = customTooltip;
        METHOD_CUSTOM_INTERACTABLE_DRAWABLE = customDrawable;
        METHOD_DISPLAY_COMPONENT_DRAW = displayComponentDraw;
        METHOD_DISPLAY_COMPONENT_STACK = componentStack;
        METHOD_DISPLAY_COMPONENT_STACK_SIZE = displayStackSize;
        METHOD_DISPLAY_COMPONENT_ADDITIONAL_INFO = displayAdditionalInfo;
        METHOD_DISPLAY_COMPONENT_DESCRIPTION_TOOLTIP = descriptionTooltip;
        METHOD_DISPLAY_COMPONENT_ADDITIONAL_TOOLTIP = additionalTooltip;
        METHOD_COMPONENT_LABEL_COMPONENT = componentLabelComponent;
        METHOD_COMPONENT_DRAW = componentDraw;
        METHOD_COMPONENT_STACK = componentStackMethod;
        METHOD_TOOLTIP_LINES = tooltipLines;
        METHOD_TOOLTIP_LINE_ELEMENTS = tooltipLineElements;
        METHOD_TOOLTIP_ELEMENT_TYPE = tooltipElementType;
        METHOD_TOOLTIP_ELEMENT_TEXT = tooltipElementText;
        METHOD_TOOLTIP_ELEMENT_COMPONENT_DESCRIPTION = tooltipElementComponentDescription;
        METHOD_COMPONENT_DESCRIPTION = componentDescription;
        METHOD_DRAW_STACK_SIZE = drawStackSize;
        METHOD_DRAW_ADDITIONAL_INFO = drawAdditionalInfo;
        METHOD_CONFIG_OPTION_GET = configOptionGet;
    }

    private NeiCustomDiagramBridge() {}

    public static boolean isDiagramGroupHandler(Object handler) {
        return AVAILABLE && handler != null && CLASS_DIAGRAM_GROUP.isInstance(handler);
    }

    /**
     * Renders nei-custom-diagram {@code DiagramGroup} content in the Guidebook.
     * <p>{@code renderX}/{@code renderY} remain parent-local GUI coordinates (matching {@code Gui});
     * {@code guiScissorAbs*} are absolute GUI coords (viewport space), same convention as {@code VanillaRenderContext}
     * scissor — required because {@code GL_SCISSOR} ignores {@code GL_MODELVIEW}.
     *
     * <p>For wide diagrams, {@code guiScissorAbsW} may be smaller than intrinsic layout (NEI {@code HandlerInfo}
     * width defaults); clip width is inflated up to the scaled GUI bounds.
     */
    public static void renderEmbedded(Object handler, int recipeIndex, int renderX, int renderY, int guiScissorAbsX,
        int guiScissorAbsY, int guiScissorAbsW, int guiScissorAbsH) {
        if (!isDiagramGroupHandler(handler)) {
            return;
        }
        Object diagram = diagramAt(handler, recipeIndex);
        Object diagramState = diagramState(handler);
        if (diagram == null || diagramState == null || guiScissorAbsW <= 0 || guiScissorAbsH <= 0) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int scaledW = sr.getScaledWidth();
        int scaledH = sr.getScaledHeight();
        int gw = guiScissorAbsW;
        int gh = guiScissorAbsH;
        int maxGw = Math.max(1, scaledW - guiScissorAbsX);
        gw = Math.min(maxGw, Math.max(gw, gw + 400));
        int maxGh = Math.max(1, scaledH - guiScissorAbsY);
        gh = Math.min(maxGh, gh + 32);

        GL11.glPushAttrib(
            GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT
                | GL11.GL_COLOR_BUFFER_BIT
                | GL11.GL_DEPTH_BUFFER_BIT
                | GL11.GL_LIGHTING_BIT
                | GL11.GL_SCISSOR_BIT
                | GL11.GL_TEXTURE_BIT);
        GL11.glPushMatrix();
        try {
            applyAbsoluteGuiScissor(guiScissorAbsX, guiScissorAbsY, gw, gh);
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glTranslatef(renderX, renderY, 0f);
            GL11.glColor4f(1f, 1f, 1f, 1f);
            METHOD_DIAGRAM_DRAW_BACKGROUND.invoke(diagram, diagramState);
            renderForeground(diagram, diagramState, guiScissorAbsX, guiScissorAbsY, gw, gh);
        } catch (Throwable t) {
            LOG.debug("Embedded nei-custom-diagram render failed", t);
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(1f, 1f, 1f, 1f);
        }
    }


    public static GuideTooltip getEmbeddedTooltip(Object handler, int recipeIndex, int localMouseX, int localMouseY) {
        if (!isDiagramGroupHandler(handler)) {
            return null;
        }
        Object diagram = diagramAt(handler, recipeIndex);
        Object diagramState = diagramState(handler);
        if (diagram == null || diagramState == null) {
            return null;
        }

        try {
            Object hovered = findHoveredInteractable(diagram, diagramState, localMouseX, localMouseY);
            if (hovered == null) {
                return null;
            }
            if (CLASS_INTERACTIVE_COMPONENT_GROUP.isInstance(hovered)) {
                return tooltipForInteractiveComponentGroup(hovered, diagramState);
            }
            if (CLASS_CUSTOM_INTERACTABLE.isInstance(hovered)) {
                List<String> lines = flattenTooltip(METHOD_CUSTOM_INTERACTABLE_TOOLTIP.invoke(hovered));
                return lines.isEmpty() ? null : new TextTooltip(String.join("\n", lines));
            }
        } catch (Throwable t) {
            LOG.debug("Embedded nei-custom-diagram tooltip lookup failed", t);
        }
        return null;
    }

    private static void renderForeground(Object diagram, Object diagramState, int clipX, int clipY, int clipWidth,
        int clipHeight) throws Exception {
        Object interactables = METHOD_DIAGRAM_INTERACTABLES.invoke(diagram, diagramState);
        if (!(interactables instanceof Iterable<?>iterable)) {
            METHOD_DIAGRAM_DRAW_FOREGROUND.invoke(diagram, diagramState);
            return;
        }

        for (Object interactable : iterable) {
            if (interactable == null || CLASS_SLOT.isInstance(interactable)) {
                continue;
            }

            reapplyClipState(clipX, clipY, clipWidth, clipHeight);
            if (CLASS_INTERACTIVE_COMPONENT_GROUP.isInstance(interactable)) {
                renderInteractiveComponentGroup(interactable, diagramState, clipX, clipY, clipWidth, clipHeight);
            } else if (CLASS_CUSTOM_INTERACTABLE.isInstance(interactable)) {
                renderCustomInteractable(interactable, diagramState, clipX, clipY, clipWidth, clipHeight);
            } else {
                METHOD_INTERACTABLE_DRAW.invoke(interactable, diagramState);
            }
        }
    }

    private static void renderInteractiveComponentGroup(Object interactable, Object diagramState, int clipX, int clipY,
        int clipWidth, int clipHeight) throws Exception {
        Object displayComponent = METHOD_INTERACTIVE_GROUP_CURRENT_COMPONENT.invoke(interactable, diagramState);
        Object position = METHOD_INTERACTABLE_POSITION.invoke(interactable);
        renderDisplayComponent(displayComponent, position, clipX, clipY, clipWidth, clipHeight);
    }

    private static void renderCustomInteractable(Object interactable, Object diagramState, int clipX, int clipY,
        int clipWidth, int clipHeight) throws Exception {
        Object drawable = METHOD_CUSTOM_INTERACTABLE_DRAWABLE.invoke(interactable);
        if (drawable != null && CLASS_COMPONENT_LABEL.isInstance(drawable)) {
            Object position = METHOD_INTERACTABLE_POSITION.invoke(interactable);
            runPointConsumer(FIELD_CUSTOM_INTERACTABLE_DRAW_BACKGROUND.get(interactable), position);
            Object component = METHOD_COMPONENT_LABEL_COMPONENT.invoke(drawable);
            renderComponent(component, position, clipX, clipY, clipWidth, clipHeight);
            runPointConsumer(FIELD_CUSTOM_INTERACTABLE_DRAW_FOREGROUND.get(interactable), position);
            return;
        }

        METHOD_INTERACTABLE_DRAW.invoke(interactable, diagramState);
    }

    private static void renderDisplayComponent(Object displayComponent, Object position, int clipX, int clipY,
        int clipWidth, int clipHeight) throws Exception {
        Object stackObject = METHOD_DISPLAY_COMPONENT_STACK.invoke(displayComponent);
        if (stackObject instanceof ItemStack stack) {
            reapplyClipState(clipX, clipY, clipWidth, clipHeight);
            NeiHandlerRenderer.drawItemIcon(stack, pointX(position) - 8, pointY(position) - 8);
            renderDisplayComponentDecorations(displayComponent, position);
            return;
        }

        METHOD_DISPLAY_COMPONENT_DRAW.invoke(displayComponent, position);
    }

    private static void renderComponent(Object component, Object position, int clipX, int clipY, int clipWidth,
        int clipHeight) throws Exception {
        Object stackObject = METHOD_COMPONENT_STACK.invoke(component);
        if (stackObject instanceof ItemStack stack) {
            reapplyClipState(clipX, clipY, clipWidth, clipHeight);
            NeiHandlerRenderer.drawItemIcon(stack, pointX(position) - 8, pointY(position) - 8);
            return;
        }

        METHOD_COMPONENT_DRAW.invoke(component, position);
    }

    private static void renderDisplayComponentDecorations(Object displayComponent, Object position) throws Exception {
        Object rawStackSize = METHOD_DISPLAY_COMPONENT_STACK_SIZE.invoke(displayComponent);
        if (rawStackSize instanceof Optional<?>optional && optional.isPresent()) {
            Object stackSize = optional.get();
            if (stackSize instanceof Integer size && shouldDrawStackSize(size)) {
                METHOD_DRAW_STACK_SIZE.invoke(null, size, position);
            }
        }

        Object rawAdditionalInfo = METHOD_DISPLAY_COMPONENT_ADDITIONAL_INFO.invoke(displayComponent);
        if (rawAdditionalInfo instanceof Optional<?>optional && optional.isPresent()) {
            Object additionalInfo = optional.get();
            if (additionalInfo != null && !additionalInfo.toString()
                .isEmpty()) {
                METHOD_DRAW_ADDITIONAL_INFO.invoke(null, additionalInfo.toString(), position, true);
            }
        }
    }

    private static boolean shouldDrawStackSize(int stackSize) {
        return stackSize != 1 || isShowStackSizeOneEnabled();
    }

    private static boolean isShowStackSizeOneEnabled() {
        if (FIELD_CONFIG_SHOW_STACK_SIZE_ONE == null || METHOD_CONFIG_OPTION_GET == null) {
            return false;
        }
        try {
            Object option = FIELD_CONFIG_SHOW_STACK_SIZE_ONE.get(null);
            return Boolean.TRUE.equals(METHOD_CONFIG_OPTION_GET.invoke(option));
        } catch (Throwable ignored) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static void runPointConsumer(Object consumerObject, Object point) {
        if (!(consumerObject instanceof Consumer<?>consumer) || point == null) {
            return;
        }
        ((Consumer<Object>) consumer).accept(point);
    }

    private static void reapplyClipState(int absGuiX, int absGuiY, int absGuiW, int absGuiH) {
        applyAbsoluteGuiScissor(absGuiX, absGuiY, absGuiW, absGuiH);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }

    private static GuideTooltip tooltipForInteractiveComponentGroup(Object hovered, Object diagramState)
        throws Exception {
        Object displayComponent = METHOD_INTERACTIVE_GROUP_CURRENT_COMPONENT.invoke(hovered, diagramState);
        List<String> extraLines = new ArrayList<>();

        Object descriptionTooltip = METHOD_DISPLAY_COMPONENT_DESCRIPTION_TOOLTIP.invoke(displayComponent);
        Object slotTooltip = FIELD_SLOT_TOOLTIP.get(hovered);
        Object additionalTooltip = METHOD_DISPLAY_COMPONENT_ADDITIONAL_TOOLTIP.invoke(displayComponent);
        Object cycleTooltip = METHOD_INTERACTIVE_GROUP_CYCLE_TOOLTIP.invoke(hovered, diagramState);

        Object stackObject = METHOD_DISPLAY_COMPONENT_STACK.invoke(displayComponent);
        if (stackObject instanceof ItemStack stack && stack.stackSize > 0) {
            appendTooltipLines(extraLines, descriptionTooltip, stack.getDisplayName());
            appendTooltipLines(extraLines, slotTooltip, null);
            appendTooltipLines(extraLines, additionalTooltip, null);
            appendTooltipLines(extraLines, cycleTooltip, null);
            return extraLines.isEmpty() ? new ItemTooltip(stack) : new AppendedItemTooltip(stack, extraLines);
        }

        appendTooltipLines(extraLines, descriptionTooltip, null);
        appendTooltipLines(extraLines, slotTooltip, null);
        appendTooltipLines(extraLines, additionalTooltip, null);
        appendTooltipLines(extraLines, cycleTooltip, null);
        return extraLines.isEmpty() ? null : new TextTooltip(String.join("\n", extraLines));
    }

    private static Object findHoveredInteractable(Object diagram, Object diagramState, int localMouseX, int localMouseY)
        throws Exception {
        Object point = METHOD_POINT_CREATE.invoke(null, localMouseX, localMouseY);
        Object interactables = METHOD_DIAGRAM_INTERACTABLES.invoke(diagram, diagramState);
        if (!(interactables instanceof Iterable<?>iterable)) {
            return null;
        }
        for (Object interactable : iterable) {
            if (interactable != null
                && Boolean.TRUE.equals(METHOD_INTERACTABLE_CHECK_BOUNDING_BOX.invoke(interactable, point))) {
                return interactable;
            }
        }
        return null;
    }

    private static void appendTooltipLines(List<String> output, Object tooltip, String firstLineToSkip) {
        for (String line : flattenTooltip(tooltip)) {
            if (line == null || line.isEmpty()) {
                continue;
            }
            if (firstLineToSkip != null && firstLineToSkip.equals(line)) {
                firstLineToSkip = null;
                continue;
            }
            firstLineToSkip = null;
            if (!output.contains(line)) {
                output.add(line);
            }
        }
    }

    private static List<String> flattenTooltip(Object tooltip) {
        if (tooltip == null || !CLASS_TOOLTIP.isInstance(tooltip)) {
            return Collections.emptyList();
        }
        try {
            Object rawLines = METHOD_TOOLTIP_LINES.invoke(tooltip);
            if (!(rawLines instanceof Iterable<?>iterable)) {
                return Collections.emptyList();
            }
            List<String> lines = new ArrayList<>();
            for (Object line : iterable) {
                if (line == null || !CLASS_TOOLTIP_LINE.isInstance(line)) {
                    continue;
                }
                String flattened = flattenTooltipLine(line);
                if (!flattened.isEmpty()) {
                    lines.add(flattened);
                }
            }
            return lines;
        } catch (Throwable t) {
            return Collections.emptyList();
        }
    }

    private static String flattenTooltipLine(Object line) throws Exception {
        Object rawElements = METHOD_TOOLTIP_LINE_ELEMENTS.invoke(line);
        if (!(rawElements instanceof Iterable<?>iterable)) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Object element : iterable) {
            if (element == null || !CLASS_TOOLTIP_ELEMENT.isInstance(element)) {
                continue;
            }
            Object type = METHOD_TOOLTIP_ELEMENT_TYPE.invoke(element);
            String name = type instanceof Enum<?>enumValue ? enumValue.name() : String.valueOf(type);
            if ("TEXT".equals(name)) {
                appendToken(builder, String.valueOf(METHOD_TOOLTIP_ELEMENT_TEXT.invoke(element)));
            } else if ("COMPONENT_DESCRIPTION".equals(name)) {
                Object component = METHOD_TOOLTIP_ELEMENT_COMPONENT_DESCRIPTION.invoke(element);
                Object description = component != null ? METHOD_COMPONENT_DESCRIPTION.invoke(component) : null;
                appendToken(builder, description != null ? description.toString() : "");
            } else if ("SPACING".equals(name) && builder.length() > 0 && builder.charAt(builder.length() - 1) != ' ') {
                builder.append(' ');
            }
        }
        return builder.toString()
            .trim();
    }

    private static void appendToken(StringBuilder builder, String token) {
        if (token == null) {
            return;
        }
        String trimmed = token.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        if (builder.length() > 0 && builder.charAt(builder.length() - 1) != ' ') {
            builder.append(' ');
        }
        builder.append(trimmed);
    }

    private static Object diagramAt(Object handler, int recipeIndex) {
        try {
            Object value = FIELD_DIAGRAMS.get(handler);
            if (value instanceof List<?>diagrams && recipeIndex >= 0 && recipeIndex < diagrams.size()) {
                Object diagram = diagrams.get(recipeIndex);
                return diagram != null && CLASS_DIAGRAM.isInstance(diagram) ? diagram : null;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static Object diagramState(Object handler) {
        try {
            Object state = FIELD_DIAGRAM_STATE.get(handler);
            return state != null && CLASS_DIAGRAM_STATE.isInstance(state) ? state : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException(type.getName() + "#" + name);
    }

    private static int pointX(Object point) throws Exception {
        return ((Number) METHOD_POINT_X.invoke(point)).intValue();
    }

    private static int pointY(Object point) throws Exception {
        return ((Number) METHOD_POINT_Y.invoke(point)).intValue();
    }

    private static void applyAbsoluteGuiScissor(int guiX, int guiY, int guiW, int guiH) {
        Minecraft mc = Minecraft.getMinecraft();
        int scale = DisplayScale.scaleFactor();
        int sx = guiX * scale;
        int sy = mc.displayHeight - (guiY + guiH) * scale;
        int sw = guiW * scale;
        int sh = guiH * scale;
        GL11.glScissor(sx, Math.max(0, sy), Math.max(0, sw), Math.max(0, sh));
    }
}
