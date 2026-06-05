package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytContentTabsBlock;
import com.hfstudio.guidenh.guide.document.block.LytVBox;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.unist.UnistNode;

public class ContentTabsTagCompiler extends BlockTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("ContentTabs");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        ContentTabsSpec spec = parseSpec(compiler, el);
        for (ContentTabsSpec.ValidationIssue issue : spec.issues()) {
            parent.appendError(compiler, issue.message(), issue.sourceNode());
        }
        if (!spec.hasRenderableTabs()) {
            return;
        }
        parent.append(new LytContentTabsBlock(resolveInitialIndex(spec), spec.tabs()));
    }

    private ContentTabsSpec parseSpec(PageCompiler compiler, MdxJsxElementFields el) {
        List<? extends MdAstAnyContent> children = resolveChildren(compiler, el);
        List<ContentTabsSpec.TabEntry> tabs = new ArrayList<>();
        List<ContentTabsSpec.ValidationIssue> issues = new ArrayList<>();
        Integer defaultIndex = parseDefaultIndex(el, issues, el);
        String defaultTitle = el.getAttributeString("default", null);
        collectTabs(compiler, children, tabs, issues);
        validateDefaultTarget(defaultTitle, defaultIndex, tabs, issues, el);
        return new ContentTabsSpec(defaultTitle, defaultIndex, tabs, issues);
    }

    private List<? extends MdAstAnyContent> resolveChildren(PageCompiler compiler, MdxJsxElementFields el) {
        String source = compiler.getBlockTagChildrenSource(el);
        return source != null ? compiler.reparseBlockTagChildren(el) : el.children();
    }

    private void collectTabs(PageCompiler compiler, List<? extends MdAstAnyContent> children,
        List<ContentTabsSpec.TabEntry> tabs, List<ContentTabsSpec.ValidationIssue> issues) {
        for (MdAstAnyContent child : children) {
            if (!(child instanceof MdxJsxFlowElement element)) {
                issues.add(new ContentTabsSpec.ValidationIssue("ContentTabs only accepts <Tab> children.", child));
                continue;
            }
            if (!"Tab".equals(element.name())) {
                issues.add(new ContentTabsSpec.ValidationIssue("ContentTabs only accepts <Tab> children.", element));
                continue;
            }
            String title = element.getAttributeString("title", null);
            if (title == null || title.trim()
                .isEmpty()) {
                issues.add(new ContentTabsSpec.ValidationIssue("<Tab> requires a non-empty title attribute.", element));
                continue;
            }
            LytVBox body = new LytVBox();
            body.setGap(4);
            compiler.compileBlockContextInSourceContext(element.children(), body);
            tabs.add(new ContentTabsSpec.TabEntry(title.trim(), body, element));
        }
    }

    private @Nullable Integer parseDefaultIndex(MdxJsxElementFields el, List<ContentTabsSpec.ValidationIssue> issues,
        UnistNode sourceNode) {
        String raw = el.getAttributeString("defaultIndex", null);
        if (raw == null || raw.trim()
            .isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            issues.add(new ContentTabsSpec.ValidationIssue("defaultIndex must be an integer.", sourceNode));
            return null;
        }
    }

    private void validateDefaultTarget(@Nullable String defaultTitle, @Nullable Integer defaultIndex,
        List<ContentTabsSpec.TabEntry> tabs, List<ContentTabsSpec.ValidationIssue> issues, UnistNode sourceNode) {
        if (defaultIndex != null && (defaultIndex < 0 || defaultIndex >= tabs.size())) {
            issues
                .add(new ContentTabsSpec.ValidationIssue("defaultIndex is out of range for ContentTabs.", sourceNode));
            return;
        }
        if (defaultIndex == null && defaultTitle != null) {
            boolean matched = tabs.stream()
                .anyMatch(tab -> defaultTitle.equals(tab.title()));
            if (!matched) {
                issues.add(new ContentTabsSpec.ValidationIssue("default does not match any <Tab> title.", sourceNode));
            }
        }
    }

    static int resolveInitialIndex(ContentTabsSpec spec) {
        if (spec.tabs()
            .isEmpty()) {
            return 0;
        }
        if (spec.defaultIndex() != null && spec.defaultIndex() >= 0
            && spec.defaultIndex() < spec.tabs()
                .size()) {
            return spec.defaultIndex();
        }
        if (spec.defaultTitle() != null) {
            for (int index = 0; index < spec.tabs()
                .size(); index++) {
                if (spec.defaultTitle()
                    .equals(
                        spec.tabs()
                            .get(index)
                            .title())) {
                    return index;
                }
            }
        }
        return 0;
    }
}
