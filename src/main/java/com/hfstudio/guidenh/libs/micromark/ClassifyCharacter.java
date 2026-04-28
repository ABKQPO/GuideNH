package com.hfstudio.guidenh.libs.micromark;

import com.hfstudio.guidenh.libs.micromark.symbol.Codes;
import com.hfstudio.guidenh.libs.micromark.symbol.Constants;

public class ClassifyCharacter {

    private ClassifyCharacter() {}

    /**
     * Classify whether a character code represents whitespace, punctuation, or something else.
     * Used for attention (emphasis, strong), whose sequences can open or close based on the class of surrounding
     * characters.
     * Note that eof (`null`) is seen as whitespace.
     */
    public static int classifyCharacter(int code) {
        if (code == Codes.eof || CharUtil.markdownLineEndingOrSpace(code) || CharUtil.unicodeWhitespace(code)) {
            return Constants.characterGroupWhitespace;
        }

        if (CharUtil.unicodePunctuation(code)) {
            return Constants.characterGroupPunctuation;
        }

        return 0;
    }

}
