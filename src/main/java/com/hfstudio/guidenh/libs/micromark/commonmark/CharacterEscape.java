package com.hfstudio.guidenh.libs.micromark.commonmark;

import com.hfstudio.guidenh.libs.micromark.Assert;
import com.hfstudio.guidenh.libs.micromark.CharUtil;
import com.hfstudio.guidenh.libs.micromark.Construct;
import com.hfstudio.guidenh.libs.micromark.State;
import com.hfstudio.guidenh.libs.micromark.TokenizeContext;
import com.hfstudio.guidenh.libs.micromark.Tokenizer;
import com.hfstudio.guidenh.libs.micromark.Types;
import com.hfstudio.guidenh.libs.micromark.symbol.Codes;

public class CharacterEscape {

    private CharacterEscape() {}

    public static final Construct characterEscape;

    static {
        characterEscape = new Construct();
        characterEscape.name = "characterEscape";
        characterEscape.tokenize = (context, effects, ok, nok) -> new StateMachine(context, effects, ok, nok)::start;
    }

    public static class StateMachine {

        private final TokenizeContext context;
        private final Tokenizer.Effects effects;
        private final State ok;
        private final State nok;

        public StateMachine(TokenizeContext context, Tokenizer.Effects effects, State ok, State nok) {
            this.context = context;
            this.effects = effects;
            this.ok = ok;
            this.nok = nok;
        }

        /**
         * Start of a character escape.
         *
         * <pre>
         * > | a\*b
         *      ^
         * </pre>
         */
        private State start(int code) {
            Assert.check(code == Codes.backslash, "expected `\\`");
            effects.enter(Types.characterEscape);
            effects.enter(Types.escapeMarker);
            effects.consume(code);
            effects.exit(Types.escapeMarker);
            return this::open;
        }

        /**
         * Inside a character escape, after `\`.
         *
         * <pre>
         * > | a\*b
         *       ^
         * </pre>
         */
        private State open(int code) {
            if (CharUtil.asciiPunctuation(code)) {
                effects.enter(Types.characterEscapeValue);
                effects.consume(code);
                effects.exit(Types.characterEscapeValue);
                effects.exit(Types.characterEscape);
                return ok;
            }

            return nok.step(code);
        }

    }
}
