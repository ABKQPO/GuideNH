package com.hfstudio.guidenh.compat.betterquesting.compiler;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.util.StatCollector;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.compat.betterquesting.BqHelpers;
import com.hfstudio.guidenh.compat.betterquesting.QuestDisplay;
import com.hfstudio.guidenh.compat.betterquesting.QuestIndex;
import com.hfstudio.guidenh.compat.betterquesting.QuestState;
import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.FlowTagCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.flow.LytFlowLink;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.guide.document.flow.LytTooltipSpan;
import com.hfstudio.guidenh.guide.document.interaction.TextTooltip;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

/**
 * Compiles {@code <QuestLink id="<uuid>" [text="<override>"]/>} into an inline link to the
 * BetterQuesting quest GUI. The displayed text and click behavior depend on the player's
 * progress at compile time.
 * <p/>
 * Hidden or locked quests render as a non-clickable placeholder span; missing UUIDs render as
 * an error span.
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
            parent.appendError(compiler, "QuestLink requires an 'id' attribute (quest UUID).", el);
            return;
        }
        UUID questId;
        try {
            questId = UUID.fromString(idAttr.trim());
        } catch (IllegalArgumentException e) {
            parent.appendError(compiler, "QuestLink id is not a valid UUID: " + idAttr, el);
            return;
        }

        String overrideText = MdxAttrs.getString(compiler, parent, el, "text", null);

        QuestDisplay display = BqHelpers.resolveDisplay(questId, Minecraft.getMinecraft().thePlayer);
        QuestState state = display.getState();
        String text = pickText(overrideText, display, questId);

        switch (state) {
            case VISIBLE, COMPLETED -> appendVisibleLink(compiler, parent, questId, text, display);
            case LOCKED -> appendPlaceholder(parent, text, SymbolicColor.GRAY, display.getDescription());
            case HIDDEN -> appendPlaceholder(parent, text, SymbolicColor.DARK_GRAY, null);
            case MISSING -> appendPlaceholder(parent, text, SymbolicColor.RED, null);
        }
    }

    private static void appendVisibleLink(PageCompiler compiler, LytFlowParent parent, UUID questId, String text,
        QuestDisplay display) {
        // Prefer linking inside the guide if a page indexes this quest, otherwise open the BQ GUI.
        PageAnchor pageAnchor = compiler.getIndex(QuestIndex.class)
            .findByUuid(questId);

        var link = new LytFlowLink();
        if (pageAnchor != null) {
            link.setPageLink(pageAnchor);
        } else {
            link.setClickCallback(screen -> BqHelpers.openQuestGui(questId));
        }

        if (display.getState() == QuestState.COMPLETED) {
            link.modifyStyle(style -> style.color(SymbolicColor.GREEN));
        }

        link.appendText(text);

        String description = display.getDescription();
        if (description != null && !description.isEmpty()) {
            link.setTooltip(new TextTooltip(description));
        }

        parent.append(link);
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
        return switch (state) {
            case VISIBLE -> nameOrFallback(display, questId);
            case COMPLETED -> nameOrFallback(display, questId) + " \u2713";
            case LOCKED -> "[" + StatCollector.translateToLocal("guidenh.compat.bq.locked") + "]";
            case HIDDEN -> "[" + StatCollector.translateToLocal("guidenh.compat.bq.hidden") + "]";
            case MISSING -> "[" + StatCollector.translateToLocal("guidenh.compat.bq.missing") + "]";
        };
    }

    private static String nameOrFallback(QuestDisplay display, UUID questId) {
        String name = display.getName();
        return name != null && !name.isEmpty() ? name : "Quest " + questId;
    }
}
