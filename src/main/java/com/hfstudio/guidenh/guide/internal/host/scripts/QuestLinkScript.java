package com.hfstudio.guidenh.guide.internal.host.scripts;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.util.StatCollector;

import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.guide.document.flow.LytFlowText;
import com.hfstudio.guidenh.guide.document.flow.LytTooltipSpan;
import com.hfstudio.guidenh.guide.document.interaction.TextTooltip;
import com.hfstudio.guidenh.integration.betterquesting.BqHelpers;
import com.hfstudio.guidenh.integration.betterquesting.QuestDisplay;
import com.hfstudio.guidenh.integration.betterquesting.QuestState;
import com.hfstudio.guidenh.integration.betterquesting.compiler.QuestTagSupport;
import com.hfstudio.guidenh.guide.internal.host.EventType;
import com.hfstudio.guidenh.guide.internal.host.LytEvent;
import com.hfstudio.guidenh.guide.internal.host.LytScript;
import com.hfstudio.guidenh.guide.internal.host.ScriptContext;
import com.hfstudio.guidenh.guide.internal.host.ScriptType;

public class QuestLinkScript implements LytScript {

    @Override
    public ScriptType type() { return ScriptType.JAVA; }

    @Override
    public String styleClass() { return "QuestLink"; }

    @Override
    public void onEvent(Object node, LytEvent event, ScriptContext ctx) {
        if (event.type() != EventType.MOUNT) return;
        if (!(node instanceof LytFlowText placeholder)) return;

        UUID questId = (UUID) placeholder.getData("questId");
        if (questId == null) return;

        Boolean showTooltip = (Boolean) placeholder.getData("showTooltip");
        String overrideText = (String) placeholder.getData("overrideText");

        QuestDisplay display;
        try {
            display = BqHelpers.resolveDisplay(questId, Minecraft.getMinecraft().thePlayer,
                Boolean.TRUE.equals(showTooltip));
        } catch (Throwable t) {
            ctx.replace(LytParagraph.error("[QuestLink] BetterQuesting integration not available"));
            return;
        }
        if (display == null) {
            LytFlowSpan errorSpan = new LytFlowSpan();
            errorSpan.modifyStyle(style -> style.color(SymbolicColor.ERROR_TEXT));
            errorSpan.appendText("[QuestLink] Quest not found: " + questId);
            ctx.replace(errorSpan);
            return;
        }
        QuestState state = display.getState();
        String text = overrideText != null && !overrideText.isEmpty()
            ? overrideText
            : pickText(display, questId);

        LytFlowContent replacement;
        if (QuestTagSupport.isNavigable(state)) {
            replacement = QuestTagSupport.createQuestGuiLink(questId, display, text,
                Boolean.TRUE.equals(showTooltip));
        } else {
            SymbolicColor color = state == QuestState.HIDDEN ? SymbolicColor.DARK_GRAY
                : state == QuestState.MISSING ? SymbolicColor.RED : SymbolicColor.GRAY;
            LytFlowSpan span = new LytFlowSpan();
            span.modifyStyle(style -> style.color(color).italic(true));
            span.appendText(text);
            replacement = span;
        }

        ctx.replace(replacement);
    }

    private static String pickText(QuestDisplay display, UUID questId) {
        QuestState state = display.getState();
        if (state == QuestState.COMPLETED) {
            return QuestTagSupport.nameOrFallback(display, questId) + " ✓";
        }
        if (QuestTagSupport.isNavigable(state)) {
            return QuestTagSupport.nameOrFallback(display, questId);
        }
        if (state == QuestState.HIDDEN) {
            return "[" + StatCollector.translateToLocal("guidenh.compat.bq.hidden") + "]";
        }
        if (state == QuestState.MISSING) {
            return "[" + StatCollector.translateToLocal("guidenh.compat.bq.missing") + "]";
        }
        return "[" + StatCollector.translateToLocal("guidenh.compat.bq.hidden") + "]";
    }
}
