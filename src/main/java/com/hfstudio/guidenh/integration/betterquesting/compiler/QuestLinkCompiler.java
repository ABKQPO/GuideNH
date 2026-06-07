package com.hfstudio.guidenh.integration.betterquesting.compiler;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.FlowTagCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.integration.betterquesting.QuestIdParser;
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

        var placeholder = parent.appendText("");
        placeholder.setStyleClass("QuestLink");
        placeholder.setData("questId", questId);
        placeholder.setData("overrideText", overrideText);
        placeholder.setData("showTooltip", showTooltip);
    }
}
