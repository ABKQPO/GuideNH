package com.hfstudio.guidenh.integration.betterquesting.compiler;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.util.StatCollector;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.FlowTagCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.guide.document.flow.LytTooltipSpan;
import com.hfstudio.guidenh.guide.document.interaction.TextTooltip;
import com.hfstudio.guidenh.integration.betterquesting.BqHelpers;
import com.hfstudio.guidenh.integration.betterquesting.QuestDisplay;
import com.hfstudio.guidenh.integration.betterquesting.QuestIdParser;
import com.hfstudio.guidenh.integration.betterquesting.QuestState;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

/**
 * Compiles {@code <QuestLink id="<uuid>" [text="<override>"] [show_tooltip="false"]/>} into an
 * inline BetterQuesting quest link. The displayed text and click behavior depend on the player's
 * progress at compile time.
 * <p/>
 * Hidden quests render as a non-clickable placeholder span; missing UUIDs render as an error
 * span.
 */
public class QuestLinkCompiler extends FlowTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("QuestLink");
    }

    @Override
    protected void compile(PageCompiler compiler, LytFlowParent parent, MdxJsxElementFields el) {
        String idAttr = MdxAttrs.getString(compiler, parent, el, "id", null);
        if (idAttr == null) {
            parent.appendError(compiler, "QuestLink requires an 'id' attribute (BetterQuesting quest id).", el);
            return;
        }
        UUID questId = QuestIdParser.parse(idAttr);
        if (questId == null) {
            parent.appendError(compiler, "QuestLink id is not a valid BetterQuesting quest id: " + idAttr, el);
            return;
        }

        String overrideText = MdxAttrs.getString(compiler, parent, el, "text", null);
        boolean showTooltip = QuestTagSupport.resolveShowTooltip(compiler, parent, el);

        QuestDisplay display = BqHelpers.resolveDisplay(questId, Minecraft.getMinecraft().thePlayer, showTooltip);
        QuestState state = display.getState();
        String text = pickText(overrideText, display, questId);

        if (QuestTagSupport.isNavigable(state)) {
            appendNavigableLink(compiler, parent, questId, text, display, showTooltip);
        } else if (state == QuestState.HIDDEN) {
            appendPlaceholder(parent, text, SymbolicColor.DARK_GRAY, null);
        } else if (state == QuestState.MISSING) {
            appendPlaceholder(parent, text, SymbolicColor.RED, null);
        } else {
            appendPlaceholder(parent, text, SymbolicColor.GRAY, null);
        }
    }

    private static void appendNavigableLink(PageCompiler compiler, LytFlowParent parent, UUID questId, String text,
        QuestDisplay display, boolean showTooltip) {
        parent.append(QuestTagSupport.createQuestGuiLink(questId, display, text, showTooltip));
    }

    private static void appendPlaceholder(LytFlowParent parent, String text, SymbolicColor color,
        @Nullable String tooltipText) {
        LytFlowSpan span;
        if (tooltipText != null && !tooltipText.isEmpty()) {
            var tooltipSpan = new LytTooltipSpan();
            tooltipSpan.setTooltip(new TextTooltip(tooltipText));
            span = tooltipSpan;
        } else {
            span = new LytFlowSpan();
        }
        span.modifyStyle(
            style -> style.color(color)
                .italic(true));
        span.appendText(text);
        parent.append(span);
    }

    private static String pickText(@Nullable String overrideText, QuestDisplay display, UUID questId) {
        if (overrideText != null && !overrideText.isEmpty()) {
            return overrideText;
        }
        QuestState state = display.getState();
        if (state == QuestState.COMPLETED) {
            return nameOrFallback(display, questId) + " \u2713";
        }
        if (QuestTagSupport.isNavigable(state)) {
            return nameOrFallback(display, questId);
        }
        if (state == QuestState.HIDDEN) {
            return "[" + StatCollector.translateToLocal("guidenh.compat.bq.hidden") + "]";
        }
        if (state == QuestState.MISSING) {
            return "[" + StatCollector.translateToLocal("guidenh.compat.bq.missing") + "]";
        }
        return "[" + StatCollector.translateToLocal("guidenh.compat.bq.hidden") + "]";
    }

    private static String nameOrFallback(QuestDisplay display, UUID questId) {
        return QuestTagSupport.nameOrFallback(display, questId);
    }
}
