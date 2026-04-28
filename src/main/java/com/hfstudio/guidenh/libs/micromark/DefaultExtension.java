package com.hfstudio.guidenh.libs.micromark;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hfstudio.guidenh.libs.micromark.commonmark.Attention;
import com.hfstudio.guidenh.libs.micromark.commonmark.AutoLink;
import com.hfstudio.guidenh.libs.micromark.commonmark.BlockQuote;
import com.hfstudio.guidenh.libs.micromark.commonmark.CharacterEscape;
import com.hfstudio.guidenh.libs.micromark.commonmark.CharacterReference;
import com.hfstudio.guidenh.libs.micromark.commonmark.CodeFenced;
import com.hfstudio.guidenh.libs.micromark.commonmark.CodeIndented;
import com.hfstudio.guidenh.libs.micromark.commonmark.CodeText;
import com.hfstudio.guidenh.libs.micromark.commonmark.Definition;
import com.hfstudio.guidenh.libs.micromark.commonmark.HardBreakEscape;
import com.hfstudio.guidenh.libs.micromark.commonmark.HeadingAtx;
import com.hfstudio.guidenh.libs.micromark.commonmark.HtmlFlow;
import com.hfstudio.guidenh.libs.micromark.commonmark.HtmlText;
import com.hfstudio.guidenh.libs.micromark.commonmark.LabelEnd;
import com.hfstudio.guidenh.libs.micromark.commonmark.LabelStartImage;
import com.hfstudio.guidenh.libs.micromark.commonmark.LabelStartLink;
import com.hfstudio.guidenh.libs.micromark.commonmark.LineEnding;
import com.hfstudio.guidenh.libs.micromark.commonmark.ListConstruct;
import com.hfstudio.guidenh.libs.micromark.commonmark.SetextUnderline;
import com.hfstudio.guidenh.libs.micromark.commonmark.ThematicBreak;
import com.hfstudio.guidenh.libs.micromark.symbol.Codes;

public class DefaultExtension {

    private DefaultExtension() {}

    private static List<Construct> l(Construct... constructs) {
        return Arrays.asList(constructs);
    }

    public static Extension create() {
        var extension = new Extension();

        extension.document = new HashMap<>();
        extension.document.put(Codes.asterisk, l(ListConstruct.list));
        extension.document.put(Codes.plusSign, l(ListConstruct.list));
        extension.document.put(Codes.dash, l(ListConstruct.list));
        extension.document.put(Codes.digit0, l(ListConstruct.list));
        extension.document.put(Codes.digit1, l(ListConstruct.list));
        extension.document.put(Codes.digit2, l(ListConstruct.list));
        extension.document.put(Codes.digit3, l(ListConstruct.list));
        extension.document.put(Codes.digit4, l(ListConstruct.list));
        extension.document.put(Codes.digit5, l(ListConstruct.list));
        extension.document.put(Codes.digit6, l(ListConstruct.list));
        extension.document.put(Codes.digit7, l(ListConstruct.list));
        extension.document.put(Codes.digit8, l(ListConstruct.list));
        extension.document.put(Codes.digit9, l(ListConstruct.list));
        extension.document.put(Codes.greaterThan, l(BlockQuote.blockQuote));

        Map<Integer, List<Construct>> contentInitial = new HashMap<>();
        contentInitial.put(Codes.leftSquareBracket, l(Definition.definition));
        extension.contentInitial = contentInitial;

        Map<Integer, List<Construct>> flowInitial = new HashMap<>();
        flowInitial.put(Codes.horizontalTab, l(CodeIndented.codeIndented));
        flowInitial.put(Codes.virtualSpace, l(CodeIndented.codeIndented));
        flowInitial.put(Codes.space, l(CodeIndented.codeIndented));
        extension.flowInitial = flowInitial;

        Map<Integer, List<Construct>> flow = new HashMap<>();
        flow.put(Codes.numberSign, l(HeadingAtx.headingAtx));
        flow.put(Codes.asterisk, l(ThematicBreak.thematicBreak));
        flow.put(Codes.dash, l(SetextUnderline.setextUnderline, ThematicBreak.thematicBreak));
        flow.put(Codes.lessThan, l(HtmlFlow.htmlFlow));
        flow.put(Codes.equalsTo, l(SetextUnderline.setextUnderline));
        flow.put(Codes.underscore, l(ThematicBreak.thematicBreak));
        flow.put(Codes.graveAccent, l(CodeFenced.codeFenced));
        flow.put(Codes.tilde, l(CodeFenced.codeFenced));
        extension.flow = flow;

        Map<Integer, List<Construct>> string = new HashMap<>();
        string.put(Codes.ampersand, l(CharacterReference.characterReference));
        string.put(Codes.backslash, l(CharacterEscape.characterEscape));
        extension.string = string;

        extension.text = new HashMap<>();
        extension.text.put(Codes.carriageReturn, l(LineEnding.lineEnding));
        extension.text.put(Codes.lineFeed, l(LineEnding.lineEnding));
        extension.text.put(Codes.carriageReturnLineFeed, l(LineEnding.lineEnding));
        extension.text.put(Codes.exclamationMark, l(LabelStartImage.labelStartImage));
        extension.text.put(Codes.ampersand, l(CharacterReference.characterReference));
        extension.text.put(Codes.asterisk, l(Attention.attention));
        extension.text.put(Codes.lessThan, l(AutoLink.autolink, HtmlText.htmlText));
        extension.text.put(Codes.leftSquareBracket, l(LabelStartLink.labelStartLink));
        extension.text.put(Codes.backslash, l(HardBreakEscape.hardBreakEscape, CharacterEscape.characterEscape));
        extension.text.put(Codes.rightSquareBracket, l(LabelEnd.labelEnd));
        extension.text.put(Codes.underscore, l(Attention.attention));
        extension.text.put(Codes.graveAccent, l(CodeText.codeText));

        extension.nullInsideSpan = Arrays.asList(Attention.attention.resolveAll, InitializeText.resolver);

        extension.nullAttentionMarkers = Arrays.asList(Codes.asterisk, Codes.underscore);

        extension.nullDisable = Collections.emptyList();

        return extension;
    }

}
