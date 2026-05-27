package com.hfstudio.guidenh.guide.internal.host.scripts;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.util.StatCollector;

import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.block.LytQuoteBox;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.integration.betterquesting.BqHelpers;
import com.hfstudio.guidenh.integration.betterquesting.QuestDisplay;
import com.hfstudio.guidenh.integration.betterquesting.QuestState;
import com.hfstudio.guidenh.integration.betterquesting.compiler.QuestCardCompiler.QuestCardPlaceholder;
import com.hfstudio.guidenh.integration.betterquesting.compiler.QuestTagSupport;
import com.hfstudio.guidenh.guide.internal.host.EventType;
import com.hfstudio.guidenh.guide.internal.host.LytEvent;
import com.hfstudio.guidenh.guide.internal.host.LytScript;
import com.hfstudio.guidenh.guide.internal.host.ScriptContext;
import com.hfstudio.guidenh.guide.internal.host.ScriptType;

public class QuestCardScript implements LytScript {

    @Override
    public ScriptType type() { return ScriptType.JAVA; }

    @Override
    public String styleClass() { return "QuestCard"; }

    @Override
    public void onEvent(Object node, LytEvent event, ScriptContext ctx) {
        if (event.type() != EventType.MOUNT) return;
        if (!(node instanceof QuestCardPlaceholder ph)) return;

        QuestDisplay display = BqHelpers.resolveDisplay(ph.questId, Minecraft.getMinecraft().thePlayer,
            ph.showTooltip || ph.showDesc);
        QuestState state = display.getState();

        var box = new LytQuoteBox();
        SymbolicColor accent = pickAccentColor(state);
        box.setQuoteStyle(accent, null, null);

        var title = new LytParagraph();
        title.setMarginTop(0);
        title.setMarginBottom(2);

        String name = resolveTitleText(display, ph.questId);
        if (QuestTagSupport.isNavigable(state)) {
            title.append(QuestTagSupport.createQuestGuiLink(ph.questId, display, name, ph.showTooltip));
        } else {
            var span = new LytFlowSpan();
            span.modifyStyle(style -> style.color(pickPlaceholderColor(state)).italic(true));
            span.appendText(name);
            title.append(span);
        }
        box.append(title);

        if (ph.showDesc && isVisibleToPlayer(state)) {
            String description = display.getDescription();
            if (description != null && !description.isEmpty()) {
                var descPar = new LytParagraph();
                descPar.appendText(description);
                box.append(descPar);
            }
        }

        ctx.replace(box);
    }

    private static String resolveTitleText(QuestDisplay display, UUID questId) {
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

    private static SymbolicColor pickAccentColor(QuestState state) {
        if (state == QuestState.COMPLETED) return SymbolicColor.GREEN;
        if (state == QuestState.LOCKED || state == QuestState.HIDDEN) return SymbolicColor.GRAY;
        if (state == QuestState.MISSING) return SymbolicColor.RED;
        return SymbolicColor.LINK;
    }

    private static SymbolicColor pickPlaceholderColor(QuestState state) {
        if (state == QuestState.HIDDEN) return SymbolicColor.DARK_GRAY;
        if (state == QuestState.MISSING) return SymbolicColor.RED;
        return SymbolicColor.GRAY;
    }

    private static boolean isVisibleToPlayer(QuestState state) {
        return QuestTagSupport.isVisibleToPlayer(state);
    }
}
