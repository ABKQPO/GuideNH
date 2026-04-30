
package com.hfstudio.guidenh.guide.internal.search;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.util.ResourceLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hfstudio.guidenh.guide.PageCollection;
import com.hfstudio.guidenh.guide.compiler.IndexingContext;
import com.hfstudio.guidenh.guide.compiler.IndexingSink;
import com.hfstudio.guidenh.guide.compiler.TagCompiler;
import com.hfstudio.guidenh.guide.extensions.Extension;
import com.hfstudio.guidenh.guide.extensions.ExtensionCollection;
import com.hfstudio.guidenh.guide.extensions.ExtensionPoint;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownListSemantics;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownRuntimeBlocks;
import com.hfstudio.guidenh.libs.mdast.MdAstYamlFrontmatter;
import com.hfstudio.guidenh.libs.mdast.gfm.model.GfmTable;
import com.hfstudio.guidenh.libs.mdast.gfmstrikethrough.MdAstDelete;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstBlockquote;
import com.hfstudio.guidenh.libs.mdast.model.MdAstBreak;
import com.hfstudio.guidenh.libs.mdast.model.MdAstCode;
import com.hfstudio.guidenh.libs.mdast.model.MdAstDefinition;
import com.hfstudio.guidenh.libs.mdast.model.MdAstEmphasis;
import com.hfstudio.guidenh.libs.mdast.model.MdAstHTML;
import com.hfstudio.guidenh.libs.mdast.model.MdAstHeading;
import com.hfstudio.guidenh.libs.mdast.model.MdAstImage;
import com.hfstudio.guidenh.libs.mdast.model.MdAstImageReference;
import com.hfstudio.guidenh.libs.mdast.model.MdAstInlineCode;
import com.hfstudio.guidenh.libs.mdast.model.MdAstLink;
import com.hfstudio.guidenh.libs.mdast.model.MdAstLinkReference;
import com.hfstudio.guidenh.libs.mdast.model.MdAstList;
import com.hfstudio.guidenh.libs.mdast.model.MdAstListItem;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParagraph;
import com.hfstudio.guidenh.libs.mdast.model.MdAstRoot;
import com.hfstudio.guidenh.libs.mdast.model.MdAstStrong;
import com.hfstudio.guidenh.libs.mdast.model.MdAstText;
import com.hfstudio.guidenh.libs.mdast.model.MdAstThematicBreak;

public class PageIndexer implements IndexingContext {

    public static final Logger LOG = LoggerFactory.getLogger(PageIndexer.class);

    private final PageCollection pages;
    private final ExtensionCollection extensions;
    private final ResourceLocation pageId;

    private final Map<String, TagCompiler> tagCompilers = new HashMap<>();

    public PageIndexer(PageCollection pages, ExtensionCollection extensions, ResourceLocation pageId) {
        this.pages = pages;
        this.extensions = extensions;
        this.pageId = pageId;

        // Index available tag-compilers
        for (var tagCompiler : extensions.get(TagCompiler.EXTENSION_POINT)) {
            for (String tagName : tagCompiler.getTagNames()) {
                tagCompilers.put(tagName, tagCompiler);
            }
        }
    }

    @Override
    public ExtensionCollection getExtensions() {
        return extensions;
    }

    @Override
    public <T extends Extension> List<T> getExtensions(ExtensionPoint<T> extensionPoint) {
        return extensions.get(extensionPoint);
    }

    public void index(MdAstRoot root, IndexingSink sink) {
        indexContent(root.children(), sink);
    }

    @Override
    public void indexContent(MdAstAnyContent content, IndexingSink sink) {
        if (content instanceof MdAstThematicBreak) {
            sink.appendBreak();
        } else if (content instanceof MdAstList astList) {
            indexList(astList, sink);
        } else if (content instanceof MdAstCode astCode) {
            sink.appendText(astCode, astCode.value);
        } else if (content instanceof MdAstHeading astHeading) {
            indexContent(astHeading.children(), sink);
        } else if (content instanceof MdAstBlockquote astBlockquote) {
            var alert = MarkdownRuntimeBlocks.extractGithubAlert(astBlockquote);
            if (alert != null) {
                sink.appendText(
                    astBlockquote,
                    alert.type()
                        .displayText());
                indexAlertChildren(alert, sink);
            } else {
                indexContent(astBlockquote.children(), sink);
            }
        } else if (content instanceof MdAstParagraph astParagraph) {
            indexContent(astParagraph.children(), sink);
        } else if (content instanceof MdAstDefinition) {
            // Definitions contribute through references when used.
        } else if (content instanceof MdAstYamlFrontmatter) {
            // This is handled by compile directly
        } else if (content instanceof GfmTable astTable) {
            indexTable(astTable, sink);
        } else if (content instanceof MdAstText astText) {
            sink.appendText(astText, astText.value);
        } else if (content instanceof MdAstInlineCode astCode) {
            sink.appendText(astCode, astCode.value);
        } else if (content instanceof MdAstStrong astStrong) {
            indexContent(astStrong.children(), sink);
        } else if (content instanceof MdAstEmphasis astEmphasis) {
            indexContent(astEmphasis.children(), sink);
        } else if (content instanceof MdAstDelete astDelete) {
            indexContent(astDelete.children(), sink);
        } else if (content instanceof MdAstBreak) {
            sink.appendBreak();
        } else if (content instanceof MdAstLink astLink) {
            indexLink(astLink, sink);
        } else if (content instanceof MdAstLinkReference astLinkReference) {
            indexContent(astLinkReference.children(), sink);
        } else if (content instanceof MdAstImage astImage) {
            indexImage(astImage, sink);
        } else if (content instanceof MdAstImageReference astImageReference) {
            if (astImageReference.alt != null && !astImageReference.alt.isEmpty()) {
                sink.appendText(astImageReference, astImageReference.alt);
            }
        } else if (content instanceof MdAstHTML astHtml) {
            sink.appendText(astHtml, stripHtmlTags(astHtml.value));
        } else if (content instanceof MdxJsxElementFields el) {
            var compiler = tagCompilers.get(el.name());
            if (compiler == null) {
                LOG.warn("Unhandled custom MDX element in guide search indexing: {}", el.name());
            } else {
                compiler.index(this, el, sink);
            }
        } else {
            LOG.warn("Unhandled node type in guide search indexing: {}", content.type());
        }
        sink.appendBreak();
    }

    private void indexList(MdAstList astList, IndexingSink sink) {
        for (var listContent : astList.children()) {
            if (listContent instanceof MdAstListItem astListItem) {
                var taskMarker = MarkdownListSemantics.extractTaskMarker(astListItem.children());
                if (taskMarker != null) {
                    indexListItemChildren(astListItem, taskMarker, sink);
                    for (int i = 1; i < astListItem.children()
                        .size(); i++) {
                        indexContent(
                            astListItem.children()
                                .get(i),
                            sink);
                    }
                } else {
                    indexContent(astListItem.children(), sink);
                }
            } else {
                LOG.warn("Cannot handle list content: {}", listContent);
            }
        }
    }

    private void indexListItemChildren(MdAstListItem astListItem, MarkdownListSemantics.TaskMarker taskMarker,
        IndexingSink sink) {
        if (astListItem.children()
            .isEmpty()
            || !(astListItem.children()
                .get(0) instanceof MdAstParagraph paragraph)) {
            sink.appendText(astListItem, taskMarker.remainingText());
            sink.appendBreak();
            return;
        }

        indexParagraphWithLeadingTextOverride(paragraph, taskMarker.remainingText(), sink);
    }

    private void indexAlertChildren(MarkdownRuntimeBlocks.GithubAlertBlock alert, IndexingSink sink) {
        if (!alert.children()
            .isEmpty()
            && alert.children()
                .get(0) instanceof MdAstParagraph paragraph) {
            if (!alert.remainingText()
                .isEmpty()) {
                indexParagraphWithLeadingTextOverride(paragraph, alert.remainingText(), sink);
            }
            for (int i = 1; i < alert.children()
                .size(); i++) {
                indexContent(
                    alert.children()
                        .get(i),
                    sink);
            }
            return;
        }

        indexContent(alert.children(), sink);
    }

    private void indexParagraphWithLeadingTextOverride(MdAstParagraph paragraph, String leadingText,
        IndexingSink sink) {
        boolean replaced = false;
        for (var child : paragraph.children()) {
            if (!replaced && child instanceof MdAstText) {
                if (!leadingText.isEmpty()) {
                    sink.appendText(paragraph, leadingText);
                    sink.appendBreak();
                }
                replaced = true;
                continue;
            }
            indexContent(child, sink);
        }
    }

    private void indexTable(GfmTable astTable, IndexingSink sink) {
        for (var astRow : astTable.children()) {
            var astCells = astRow.children();
            for (var astCell : astCells) {
                indexContent(astCell.children(), sink);
            }
        }
    }

    private void indexLink(MdAstLink astLink, IndexingSink sink) {
        if (astLink.title != null && !astLink.title.isEmpty()) {
            sink.appendText(astLink, astLink.title);
        }
        indexContent(astLink.children(), sink);
    }

    private void indexImage(MdAstImage astImage, IndexingSink sink) {
        if (astImage.title != null && !astImage.title.isEmpty()) {
            sink.appendText(astImage, astImage.title);
        }
        if (astImage.alt != null && !astImage.alt.isEmpty()) {
            sink.appendText(astImage, astImage.alt);
        }
    }

    private String stripHtmlTags(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        StringBuilder stripped = new StringBuilder(html.length());
        boolean inTag = false;
        for (int i = 0; i < html.length(); i++) {
            char current = html.charAt(i);
            if (current == '<') {
                inTag = true;
                continue;
            }
            if (current == '>') {
                inTag = false;
                continue;
            }
            if (!inTag) {
                stripped.append(current);
            }
        }
        return stripped.toString();
    }

    /**
     * Get the current page id.
     */
    @Override
    public ResourceLocation getPageId() {
        return pageId;
    }

    @Override
    public PageCollection getPageCollection() {
        return pages;
    }
}
