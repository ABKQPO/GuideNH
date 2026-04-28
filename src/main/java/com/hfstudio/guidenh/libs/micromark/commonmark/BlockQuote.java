package com.hfstudio.guidenh.libs.micromark.commonmark;

import com.hfstudio.guidenh.libs.micromark.Assert;
import com.hfstudio.guidenh.libs.micromark.CharUtil;
import com.hfstudio.guidenh.libs.micromark.Construct;
import com.hfstudio.guidenh.libs.micromark.State;
import com.hfstudio.guidenh.libs.micromark.Token;
import com.hfstudio.guidenh.libs.micromark.TokenizeContext;
import com.hfstudio.guidenh.libs.micromark.Tokenizer;
import com.hfstudio.guidenh.libs.micromark.Types;
import com.hfstudio.guidenh.libs.micromark.factory.FactorySpace;
import com.hfstudio.guidenh.libs.micromark.symbol.Codes;
import com.hfstudio.guidenh.libs.micromark.symbol.Constants;

public class BlockQuote {

    private BlockQuote() {}

    public static final Construct blockQuote;

    static {
        blockQuote = new Construct();
        blockQuote.name = "blockQuote";
        blockQuote.tokenize = (context, effects, ok, nok) -> new StateMachine(context, effects, ok, nok)::start;
        blockQuote.continuation = new Construct();
        blockQuote.continuation.tokenize = (context, effects, ok, nok) -> {
            return FactorySpace.create(
                effects,
                effects.attempt.hook(blockQuote, ok, nok),
                Types.linePrefix,
                context.getParser().constructs.nullDisable.contains("codeIndented") ? Integer.MAX_VALUE
                    : Constants.tabSize);
        };
        blockQuote.exit = BlockQuote::exit;
    }

    static class StateMachine {

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

        State start(int code) {
            if (code == Codes.greaterThan) {
                var state = context.getContainerState();

                Assert.check(state != null, "expected `containerState` to be defined in container");

                if (!state.containsKey("open")) {
                    var token = new Token();
                    token._container = true;
                    effects.enter(Types.blockQuote, token);
                    state.put("open", true);
                }

                effects.enter(Types.blockQuotePrefix);
                effects.enter(Types.blockQuoteMarker);
                effects.consume(code);
                effects.exit(Types.blockQuoteMarker);
                return this::after;
            }

            return nok.step(code);
        }

        State after(int code) {
            if (CharUtil.markdownSpace(code)) {
                effects.enter(Types.blockQuotePrefixWhitespace);
                effects.consume(code);
                effects.exit(Types.blockQuotePrefixWhitespace);
                effects.exit(Types.blockQuotePrefix);
                return ok;
            }

            effects.exit(Types.blockQuotePrefix);
            return ok.step(code);
        }
    }

    private static void exit(TokenizeContext context, Tokenizer.Effects effects) {
        effects.exit(Types.blockQuote);
    }

}
