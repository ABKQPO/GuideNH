package com.hfstudio.guidenh.libs.micromark.commonmark;

import com.hfstudio.guidenh.libs.micromark.Assert;
import com.hfstudio.guidenh.libs.micromark.CharUtil;
import com.hfstudio.guidenh.libs.micromark.Construct;
import com.hfstudio.guidenh.libs.micromark.State;
import com.hfstudio.guidenh.libs.micromark.TokenizeContext;
import com.hfstudio.guidenh.libs.micromark.Tokenizer;
import com.hfstudio.guidenh.libs.micromark.Types;
import com.hfstudio.guidenh.libs.micromark.symbol.Codes;

public class HardBreakEscape {

    private HardBreakEscape() {}

    public static final Construct hardBreakEscape;

    static {
        hardBreakEscape = new Construct();
        hardBreakEscape.name = "hardBreakEscape";
        hardBreakEscape.tokenize = (context, effects, ok, nok) -> new StateMachine(context, effects, ok, nok)::start;
    }

    private static class StateMachine {

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
         * Start of a hard break (escape).
         *
         * <pre>
         * > | a\
         *      ^
         *   | b
         * </pre>
         */
        private State start(int code) {
            Assert.check(code == Codes.backslash, "expected `\\`");
            effects.enter(Types.hardBreakEscape);
            effects.consume(code);
            return this::open;
        }

        /**
         * At the end of a hard break (escape), after `\`.
         *
         * <pre>
         * > | a\
         *       ^
         *   | b
         * </pre>
         */
        private State open(int code) {
            if (CharUtil.markdownLineEnding(code)) {
                effects.exit(Types.hardBreakEscape);
                return ok.step(code);
            }

            return nok.step(code);
        }
    }
}
