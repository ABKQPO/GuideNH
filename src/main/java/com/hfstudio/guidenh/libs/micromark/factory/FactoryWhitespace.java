package com.hfstudio.guidenh.libs.micromark.factory;

import com.hfstudio.guidenh.libs.micromark.CharUtil;
import com.hfstudio.guidenh.libs.micromark.State;
import com.hfstudio.guidenh.libs.micromark.Tokenizer;
import com.hfstudio.guidenh.libs.micromark.Types;

public class FactoryWhitespace {

    private FactoryWhitespace() {}

    public static State create(Tokenizer.Effects effects, State ok) {
        return new StateMachine(effects, ok)::start;
    }

    private static class StateMachine {

        private final Tokenizer.Effects effects;
        private final State ok;
        private boolean seen;

        public StateMachine(Tokenizer.Effects effects, State ok) {
            this.effects = effects;
            this.ok = ok;
        }

        public State start(int code) {
            if (CharUtil.markdownLineEnding(code)) {
                effects.enter(Types.lineEnding);
                effects.consume(code);
                effects.exit(Types.lineEnding);
                seen = true;
                return this::start;
            }

            if (CharUtil.markdownSpace(code)) {
                return FactorySpace.create(effects, this::start, seen ? Types.linePrefix : Types.lineSuffix)
                    .step(code);
            }

            return ok.step(code);
        }
    }

}
