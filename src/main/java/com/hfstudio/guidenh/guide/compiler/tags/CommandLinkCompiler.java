package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.IndexingContext;
import com.hfstudio.guidenh.guide.compiler.IndexingSink;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.flow.LytFlowLink;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.guide.document.interaction.TextTooltip;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class CommandLinkCompiler extends FlowTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("CommandLink");
    }

    @Override
    protected void compile(PageCompiler compiler, LytFlowParent parent, MdxJsxElementFields el) {
        var command = el.getAttributeString("command", "");
        if (command.isEmpty()) {
            parent.appendError(compiler, "command attribute is required", el);
            return;
        } else if (!command.startsWith("/")) {
            parent.appendError(compiler, "command must start with /", el);
            return;
        }
        var closeGuide = MdxAttrs.getBoolean(compiler, parent, el, "close", false);
        var title = el.getAttributeString("title", "");
        var link = new LytFlowLink();
        link.setTooltip(buildTooltip(title, command));

        // Pure placeholder: mark for the script to materialize at mount time
        link.setStyleClass("CommandLink");
        link.setData("command", command);
        link.setData("close", closeGuide);
        link.setData("title", title);

        compiler.compileFlowContext(el.children(), link);
        parent.append(link);
    }

    public static TextTooltip buildTooltip(@Nullable String title, String command) {
        var tooltipTitle = title == null ? "" : title;
        var sb = new StringBuilder();
        if (!tooltipTitle.isEmpty()) {
            sb.append(tooltipTitle)
                .append("\n");
        }
        var displayCmd = command.length() > 25 ? command.substring(0, 25) + "..." : command;
        sb.append(displayCmd);
        return new TextTooltip(sb.toString());
    }

    @Override
    public void index(IndexingContext indexer, MdxJsxElementFields el, IndexingSink sink) {
        var title = el.getAttributeString("title", "");
        if (!title.trim()
            .isEmpty()) {
            sink.appendText(el, title);
        }
        indexer.indexContent(el.children(), sink);
    }
}
