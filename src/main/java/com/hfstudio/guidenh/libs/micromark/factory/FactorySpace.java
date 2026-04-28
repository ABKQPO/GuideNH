package com.hfstudio.guidenh.libs.micromark.factory;

import com.hfstudio.guidenh.libs.micromark.CharUtil;
import com.hfstudio.guidenh.libs.micromark.State;
import com.hfstudio.guidenh.libs.micromark.Tokenizer;

public class FactorySpace {

    private FactorySpace() {}

    public static State create(Tokenizer.Effects effects, State ok, String type) {
        return create(effects, ok, type, null);
    }

    public static State create(Tokenizer.Effects effects, State ok, String type, Integer max) {
        var limit = max != null ? max - 1 : Integer.MAX_VALUE;

        return new StateMachine(effects, ok, type, limit)::start;
    }

    public static class StateMachine {

        private final Tokenizer.Effects effects;
        private final State ok;
        private final String type;
        private final int limit;
        private int size;

        public StateMachine(Tokenizer.Effects effects, State ok, String type, int limit) {
            this.effects = effects;
            this.ok = ok;
            this.type = type;
            this.limit = limit;
        }

        private State prefix(int code) {
            if (CharUtil.markdownSpace(code) && size++ < limit) {
                effects.consume(code);
                return this::prefix;
            }

            effects.exit(type);
            return ok.step(code);
        }

        public State start(int code) {
            if (CharUtil.markdownSpace(code)) {
                effects.enter(type);
                return prefix(code);
            }

            return ok.step(code);
        }

        ;
    }

}
