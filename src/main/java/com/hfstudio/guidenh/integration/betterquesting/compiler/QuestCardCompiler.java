package com.hfstudio.guidenh.integration.betterquesting.compiler;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.util.StatCollector;

import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.BlockTagCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.block.LytQuoteBox;
import com.hfstudio.guidenh.guide.document.flow.LytFlowLink;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.integration.betterquesting.BqHelpers;
import com.hfstudio.guidenh.integration.betterquesting.QuestDisplay;
import com.hfstudio.guidenh.integration.betterquesting.QuestIndex;
import com.hfstudio.guidenh.integration.betterquesting.QuestState;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

/**
 * Compiles {@code <QuestCard id="<uuid>" [show_desc="false"]/>} into a block-level summary card
 * for a BetterQuesting quest. Renders the quest title (with state-aware styling), a clickable
 * link when visible, and the quest description as a body paragraph when {@code show_desc} is
 * not disabled.
 * <p/>
 * For hidden, locked or missing quests, the card collapses to a single placeholder line.
 */
public class QuestCardCompiler extends BlockTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("QuestCard");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        String idAttr = MdxAttrs.getString(compiler, parent, el, "id", null);
        if (idAttr == null) {
            parent.appendError(compiler, "QuestCard requires an 'id' attribute (quest UUID).", el);
            return;
        }
        UUID questId;
        try {
            questId = UUID.fromString(idAttr.trim());
        } catch (IllegalArgumentException e) {
            parent.appendError(compiler, "QuestCard id is not a valid UUID: " + idAttr, el);
            return;
        }
        boolean showDesc = !"false".equalsIgnoreCase(MdxAttrs.getString(compiler, parent, el, "show_desc", "true"));

        QuestDisplay display = BqHelpers.resolveDisplay(questId, Minecraft.getMinecraft().thePlayer);
        QuestState state = display.getState();

        var box = new LytQuoteBox();
        SymbolicColor accent = pickAccentColor(state);
        box.setQuoteStyle(accent, null, null);

        // Title line: clickable link for visible/completed states, placeholder text otherwise.
        var title = new LytParagraph();
        title.setMarginTop(0);
        title.setMarginBottom(2);
        appendTitle(compiler, title, questId, display);
        box.append(title);

        // Description body: only shown when the player can actually see the quest.
        if (showDesc && isVisibleToPlayer(state)) {
            String description = display.getDescription();
            if (description != null && !description.isEmpty()) {
                var descPar = new LytParagraph();
                descPar.appendText(description);
                box.append(descPar);
            }
        }

        parent.append(box);
    }

    private static void appendTitle(PageCompiler compiler, LytParagraph title, UUID questId, QuestDisplay display) {
        QuestState state = display.getState();
        String name = resolveTitleText(display, questId);

        if (isVisibleToPlayer(state)) {
            PageAnchor pageAnchor = compiler.getIndex(QuestIndex.class)
                .findByUuid(questId);
            var link = new LytFlowLink();
            if (pageAnchor != null) {
                link.setPageLink(pageAnchor);
            } else {
                link.setClickCallback(screen -> BqHelpers.openQuestGui(questId));
            }
            if (state == QuestState.COMPLETED) {
                link.modifyStyle(style -> style.color(SymbolicColor.GREEN));
                link.appendText(name + " \u2713");
            } else {
                link.appendText(name);
            }
            title.append(link);
        } else {
            var span = new LytFlowSpan();
            span.modifyStyle(
                style -> style.color(pickPlaceholderColor(state))
                    .italic(true));
            span.appendText(name);
            title.append(span);
        }
    }

    private static String resolveTitleText(QuestDisplay display, UUID questId) {
        QuestState state = display.getState();
        if (isVisibleToPlayer(state)) {
            String name = display.getName();
            return name != null && !name.isEmpty() ? name : "Quest " + questId;
        }
        if (state == QuestState.LOCKED) {
            return "[" + StatCollector.translateToLocal("guidenh.compat.bq.locked") + "]";
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
        if (state == QuestState.COMPLETED) {
            return SymbolicColor.GREEN;
        }
        if (state == QuestState.LOCKED || state == QuestState.HIDDEN) {
            return SymbolicColor.GRAY;
        }
        if (state == QuestState.MISSING) {
            return SymbolicColor.RED;
        }
        return SymbolicColor.LINK;
    }

    private static SymbolicColor pickPlaceholderColor(QuestState state) {
        if (state == QuestState.HIDDEN) {
            return SymbolicColor.DARK_GRAY;
        }
        if (state == QuestState.MISSING) {
            return SymbolicColor.RED;
        }
        return SymbolicColor.GRAY;
    }

    private static boolean isVisibleToPlayer(QuestState state) {
        return state == QuestState.VISIBLE || state == QuestState.COMPLETED;
    }
}
