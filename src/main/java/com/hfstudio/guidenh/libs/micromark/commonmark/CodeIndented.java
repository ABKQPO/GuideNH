package com.hfstudio.guidenh.libs.micromark.commonmark;

import com.hfstudio.guidenh.libs.micromark.CharUtil;
import com.hfstudio.guidenh.libs.micromark.Construct;
import com.hfstudio.guidenh.libs.micromark.State;
import com.hfstudio.guidenh.libs.micromark.TokenizeContext;
import com.hfstudio.guidenh.libs.micromark.Tokenizer;
import com.hfstudio.guidenh.libs.micromark.Types;
import com.hfstudio.guidenh.libs.micromark.factory.FactorySpace;
import com.hfstudio.guidenh.libs.micromark.symbol.Codes;
import com.hfstudio.guidenh.libs.micromark.symbol.Constants;

public class CodeIndented {

    private CodeIndented() {}

    public static final Construct codeIndented;
    public static final Construct indentedContent;

    static {
        codeIndented = new Construct();
        codeIndented.name = "codeIndented";
        codeIndented.tokenize = (context, effects, ok, nok) -> new StateMachine(context, effects, ok, nok)::start;

        indentedContent = new Construct();
        indentedContent.tokenize = (context, effects, ok,
            nok) -> new IndentedContentStateMachine(context, effects, ok, nok)::start;
        indentedContent.partial = true;
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

        private State start(int code) {
            effects.enter(Types.codeIndented);
            return FactorySpace.create(effects, this::afterStartPrefix, Types.linePrefix, Constants.tabSize + 1)
                .step(code);
        }

        private State afterStartPrefix(int code) {
            var tail = context.getLastEvent();
            return tail != null && tail.token().type.equals(Types.linePrefix)
                && tail.context()
                    .sliceSerialize(tail.token(), true)
                    .length() >= Constants.tabSize ? afterPrefix(code) : nok.step(code);
        }

        private State afterPrefix(int code) {
            if (code == Codes.eof) {
                return after(code);
            }

            if (CharUtil.markdownLineEnding(code)) {
                return effects.attempt.hook(indentedContent, this::afterPrefix, this::after)
                    .step(code);
            }

            effects.enter(Types.codeFlowValue);
            return content(code);
        }

        private State content(int code) {
            if (code == Codes.eof || CharUtil.markdownLineEnding(code)) {
                effects.exit(Types.codeFlowValue);
                return afterPrefix(code);
            }

            effects.consume(code);
            return this::content;
        }

        private State after(int code) {
            effects.exit(Types.codeIndented);
            return ok.step(code);
        }

    }

    public static class IndentedContentStateMachine {

        private final TokenizeContext context;
        private final Tokenizer.Effects effects;
        private final State ok;
        private final State nok;

        public IndentedContentStateMachine(TokenizeContext context, Tokenizer.Effects effects, State ok, State nok) {

            this.context = context;
            this.effects = effects;
            this.ok = ok;
            this.nok = nok;
        }

        private State start(int code) {
            // If this is a lazy line, it can’t be code.
            if (context.isOnLazyLine()) {
                return nok.step(code);
            }

            if (CharUtil.markdownLineEnding(code)) {
                effects.enter(Types.lineEnding);
                effects.consume(code);
                effects.exit(Types.lineEnding);
                return this::start;
            }

            return FactorySpace.create(effects, this::afterPrefix, Types.linePrefix, Constants.tabSize + 1)
                .step(code);
        }

        private State afterPrefix(int code) {
            var tail = context.getLastEvent();
            return tail != null && tail.token().type.equals(Types.linePrefix)
                && tail.context()
                    .sliceSerialize(tail.token(), true)
                    .length() >= Constants.tabSize ? ok.step(code)
                        : CharUtil.markdownLineEnding(code) ? start(code) : nok.step(code);
        }
    }
}
