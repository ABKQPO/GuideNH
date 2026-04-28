package com.hfstudio.guidenh.guide.internal.search;

import java.util.List;

import javax.annotation.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.GuidePageIcon;
import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.document.block.AlignItems;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytDocument;
import com.hfstudio.guidenh.guide.document.block.LytHBox;
import com.hfstudio.guidenh.guide.document.block.LytImage;
import com.hfstudio.guidenh.guide.document.block.LytItemImage;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.block.LytVBox;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowLink;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.guide.document.flow.LytFlowText;
import com.hfstudio.guidenh.guide.style.BorderStyle;
import com.hfstudio.guidenh.guide.style.TextAlignment;

public class GuideSearchResultDocumentBuilder {

    private static final ConstantColor SEARCH_TITLE_COLOR = new ConstantColor(0xFF00D2FC);
    private static final ConstantColor RESULT_DIVIDER_COLOR = new ConstantColor(0xFF3A3A3A);
    private static final int RESULT_ICON_SIZE = 16;
    private static final int RESULT_ICON_MARGIN_TOP = 2;
    private static final int RESULT_ICON_GAP = 6;

    private GuideSearchResultDocumentBuilder() {}

    public static LytDocument buildDocument(@Nullable String query, List<SearchPageResult> results,
        String emptyQueryMessage, String noResultsMessage) {

        var document = new LytDocument();
        if (GuideSearchPage.normalizeQuery(query)
            .isEmpty()) {
            document.append(buildCenteredMessage(emptyQueryMessage));
            return document;
        }

        if (results.isEmpty()) {
            document.append(buildCenteredMessage(noResultsMessage));
            return document;
        }

        for (var result : results) {
            document.append(buildResultRow(result));
        }

        return document;
    }

    public static boolean isCenteredStateDocument(@Nullable LytDocument document) {
        return document != null && document.getBlocks()
            .size() == 1
            && document.getBlocks()
                .get(0)
                .getClass() == CenteredStateBlock.class;
    }

    private static CenteredStateBlock buildCenteredMessage(String message) {
        var stateBlock = new CenteredStateBlock();
        var paragraph = new LytParagraph();
        paragraph.modifyStyle(style -> style.alignment(TextAlignment.CENTER));
        paragraph.appendText(message);
        stateBlock.append(paragraph);
        return stateBlock;
    }

    private static ResultRowBlock buildResultRow(SearchPageResult result) {
        var row = new ResultRowBlock();

        if (result.icon() != null) {
            row.append(buildResultIconSlot(result.icon()));
        }

        var content = new LytVBox();
        content.setFullWidth(true);
        content.setGap(2);
        content.setPaddingTop(2);

        var titleLine = new LytHBox();
        titleLine.setFullWidth(true);
        titleLine.setWrap(false);
        titleLine.setGap(4);
        titleLine.setAlignItems(AlignItems.START);

        var titleParagraph = new LytParagraph();
        titleParagraph.setPaddingTop(2);
        var link = new LytFlowLink();
        link.setPageLink(result.anchor());
        link.modifyStyle(
            style -> style.color(SEARCH_TITLE_COLOR)
                .underlined(false));
        link.appendText(result.title());
        titleParagraph.append(link);

        var pathParagraph = new LytParagraph();
        pathParagraph.setPaddingTop(2);
        pathParagraph.setPaddingRight(8);
        pathParagraph.setFullWidth(true);
        pathParagraph.modifyStyle(style -> style.alignment(TextAlignment.RIGHT));
        pathParagraph.appendText(result.pagePath());

        var snippetParagraph = new LytParagraph();
        snippetParagraph.append(copySnippetContent(result.snippet()));

        titleLine.append(titleParagraph);
        titleLine.append(pathParagraph);
        content.append(titleLine);
        content.append(snippetParagraph);
        row.append(content);
        return row;
    }

    private static LytBlock buildResultIconSlot(GuidePageIcon icon) {
        var slot = new LytHBox();
        slot.setWrap(false);
        slot.setPaddingRight(RESULT_ICON_GAP);
        slot.append(buildResultIcon(icon));
        return slot;
    }

    private static LytBlock buildResultIcon(GuidePageIcon icon) {
        if (icon.isTextureIcon()) {
            var image = new LytImage();
            image.setTexture(icon.textureId(), icon.texture());
            image.setExplicitWidth(RESULT_ICON_SIZE);
            image.setExplicitHeight(RESULT_ICON_SIZE);
            image.setMarginTop(RESULT_ICON_MARGIN_TOP);
            return image;
        }

        var item = new LytItemImage(icon.itemStack());
        item.setTooltipSuppressed(true);
        item.setMarginTop(RESULT_ICON_MARGIN_TOP);
        return item;
    }

    private static LytFlowContent copySnippetContent(LytFlowContent content) {
        if (content.getClass() == LytFlowText.class) {
            var text = (LytFlowText) content;
            var copy = copyFlowContent(text, new LytFlowText());
            copy.setText(text.getText());
            return copy;
        }

        if (content.getClass() == LytFlowSpan.class) {
            var span = (LytFlowSpan) content;
            var copy = copyFlowContent(span, new LytFlowSpan());
            for (var child : span.getChildren()) {
                copy.append(copySnippetContent(child));
            }
            return copy;
        }

        throw new IllegalArgumentException(
            "Unsupported search snippet content type: " + content.getClass()
                .getName()
                + ". GuideSearchResultDocumentBuilder only supports exact LytFlowSpan and LytFlowText nodes.");
    }

    private static <T extends LytFlowContent> T copyFlowContent(LytFlowContent source, T copy) {
        copy.setStyle(source.getStyle());
        copy.setHoverStyle(source.getHoverStyle());
        return copy;
    }

    @Desugar
    public record SearchPageResult(PageAnchor anchor, @Nullable GuidePageIcon icon, String title, String pagePath,
        LytFlowContent snippet) {}

    static public class CenteredStateBlock extends LytVBox {

        CenteredStateBlock() {
            setFullWidth(true);
            setAlignItems(AlignItems.CENTER);
        }
    }

    static public class ResultRowBlock extends LytHBox {

        ResultRowBlock() {
            setFullWidth(true);
            setWrap(false);
            setGap(0);
            setAlignItems(AlignItems.CENTER);
            setPaddingBottom(4);
            setBorderBottom(new BorderStyle(RESULT_DIVIDER_COLOR, 1));
        }
    }
}
