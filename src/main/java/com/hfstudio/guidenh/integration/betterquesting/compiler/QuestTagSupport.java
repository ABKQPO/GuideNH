package com.hfstudio.guidenh.integration.betterquesting.compiler;

import java.util.Locale;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.document.flow.LytFlowLink;
import com.hfstudio.guidenh.guide.document.interaction.TextTooltip;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;
import com.hfstudio.guidenh.integration.betterquesting.BqHelpers;
import com.hfstudio.guidenh.integration.betterquesting.QuestDisplay;
import com.hfstudio.guidenh.integration.betterquesting.QuestIndex;
import com.hfstudio.guidenh.integration.betterquesting.QuestState;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxAttribute;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class QuestTagSupport {

    private QuestTagSupport() {}

    public static boolean resolveShowTooltip(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el) {
        try {
            Boolean camelCaseValue = parseOptionalBoolean(el.getAttribute("showTooltip"), "showTooltip");
            if (camelCaseValue != null) {
                return camelCaseValue;
            }
            Boolean snakeCaseValue = parseOptionalBoolean(el.getAttribute("show_tooltip"), "show_tooltip");
            return snakeCaseValue != null ? snakeCaseValue : true;
        } catch (IllegalArgumentException e) {
            errorSink.appendError(compiler, e.getMessage(), el);
            return true;
        }
    }

    public static boolean isNavigable(QuestState state) {
        return state == QuestState.VISIBLE || state == QuestState.COMPLETED || state == QuestState.LOCKED;
    }

    public static boolean isVisibleToPlayer(QuestState state) {
        return state == QuestState.VISIBLE || state == QuestState.COMPLETED;
    }

    public static LytFlowLink createQuestLink(PageCompiler compiler, UUID questId, QuestDisplay display, String text,
        boolean showTooltip) {
        var link = new LytFlowLink();
        attachQuestNavigation(compiler, questId, link);
        if (display.getState() == QuestState.COMPLETED) {
            link.modifyStyle(style -> style.color(SymbolicColor.GREEN));
        }
        link.appendText(text);
        if (showTooltip) {
            applyTooltip(link, display.getDescription());
        }
        return link;
    }

    public static String nameOrFallback(QuestDisplay display, UUID questId) {
        String name = display.getName();
        return name != null && !name.isEmpty() ? name : "Quest " + questId;
    }

    private static void attachQuestNavigation(PageCompiler compiler, UUID questId, LytFlowLink link) {
        PageAnchor pageAnchor = compiler.getIndex(QuestIndex.class)
            .findByUuid(questId);
        if (pageAnchor != null) {
            link.setPageLink(pageAnchor);
            return;
        }
        link.setClickCallback(screen -> openBetterQuestingQuest(screen, questId));
    }

    private static void openBetterQuestingQuest(GuideUiHost screen, UUID questId) {
        GuiScreen originScreen = screen instanceof GuiScreen guiScreen ? guiScreen
            : Minecraft.getMinecraft().currentScreen;
        BqHelpers.openQuestGui(questId, originScreen);
    }

    private static void applyTooltip(LytFlowLink link, @Nullable String tooltipText) {
        if (tooltipText == null || tooltipText.isEmpty()) {
            return;
        }
        link.setTooltip(new TextTooltip(tooltipText));
    }

    private static @Nullable Boolean parseOptionalBoolean(@Nullable MdxJsxAttribute attribute, String attributeName) {
        if (attribute == null) {
            return null;
        }
        if (!attribute.hasExpressionValue() && !attribute.hasStringValue()) {
            return true;
        }
        String rawValue = attribute.hasExpressionValue() ? attribute.getExpressionValue() : attribute.getStringValue();
        String normalizedValue = rawValue == null ? ""
            : rawValue.trim()
                .toLowerCase(Locale.ROOT);
        return switch (normalizedValue) {
            case "true", "1", "yes", "on" -> true;
            case "false", "0", "no", "off" -> false;
            default -> throw new IllegalArgumentException(attributeName + " should be true or false");
        };
    }
}
