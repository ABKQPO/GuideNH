package com.hfstudio.guidenh.integration.betterquesting.compiler;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.BlockTagCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.integration.betterquesting.QuestIdParser;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

/**
 * Compiles {@code <QuestCard id="<uuid>" [show_desc="false"] [show_tooltip="false"]/>} into a
 * block-level summary card for a BetterQuesting quest. Renders the quest title with state-aware
 * styling, keeps the title clickable for non-hidden quests, and renders the quest description as
 * a body paragraph when {@code show_desc} is not disabled.
 * <p/>
 * For hidden or missing quests, the card collapses to a single placeholder line.
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
            parent.appendError(compiler, "QuestCard requires an 'id' attribute (BetterQuesting quest id).", el);
            return;
        }
        UUID questId = QuestIdParser.parse(idAttr);
        if (questId == null) {
            parent.appendError(compiler, "QuestCard id is not a valid BetterQuesting quest id: " + idAttr, el);
            return;
        }
        boolean showDesc = !"false".equalsIgnoreCase(MdxAttrs.getString(compiler, parent, el, "show_desc", "true"));
        boolean showTooltip = QuestTagSupport.resolveShowTooltip(compiler, parent, el);

        QuestCardPlaceholder ph = new QuestCardPlaceholder(questId, showDesc, showTooltip);
        ph.setStyleClass("QuestCard");
        ph.setStyle(LytParagraph.LOADING_STYLE);
        ph.appendText("Loading quest...");
        parent.append(ph);
    }

    public static class QuestCardPlaceholder extends LytParagraph {

        public final UUID questId;
        public final boolean showDesc;
        public final boolean showTooltip;

        QuestCardPlaceholder(UUID questId, boolean showDesc, boolean showTooltip) {
            this.questId = questId;
            this.showDesc = showDesc;
            this.showTooltip = showTooltip;
        }
    }
}
