package com.hfstudio.guidenh.libs.micromark.commonmark;

import com.hfstudio.guidenh.libs.micromark.Assert;
import com.hfstudio.guidenh.libs.micromark.CharUtil;
import com.hfstudio.guidenh.libs.micromark.Construct;
import com.hfstudio.guidenh.libs.micromark.NormalizeIdentifier;
import com.hfstudio.guidenh.libs.micromark.State;
import com.hfstudio.guidenh.libs.micromark.TokenizeContext;
import com.hfstudio.guidenh.libs.micromark.Tokenizer;
import com.hfstudio.guidenh.libs.micromark.Types;
import com.hfstudio.guidenh.libs.micromark.factory.FactoryDestination;
import com.hfstudio.guidenh.libs.micromark.factory.FactoryLabel;
import com.hfstudio.guidenh.libs.micromark.factory.FactorySpace;
import com.hfstudio.guidenh.libs.micromark.factory.FactoryTitle;
import com.hfstudio.guidenh.libs.micromark.factory.FactoryWhitespace;
import com.hfstudio.guidenh.libs.micromark.symbol.Codes;

public class Definition {

    private Definition() {}

    public static final Construct definition;
    public static final Construct titleConstruct;
    static {
        definition = new Construct();
        definition.name = "definition";
        definition.tokenize = (context, effects, ok, nok) -> new StateMachine(context, effects, ok, nok)::start;

        titleConstruct = new Construct();
        titleConstruct.tokenize = (context, effects, ok,
            nok) -> new TitleStateMachine(context, effects, ok, nok)::start;
        titleConstruct.partial = true;
    }

    private static class StateMachine {

        private final TokenizeContext context;
        private final Tokenizer.Effects effects;
        private final State ok;
        private final State nok;
        private String identifier;

        public StateMachine(TokenizeContext context, Tokenizer.Effects effects, State ok, State nok) {

            this.context = context;
            this.effects = effects;
            this.ok = ok;
            this.nok = nok;
        }

        private State start(int code) {
            Assert.check(code == Codes.leftSquareBracket, "expected `[`");
            effects.enter(Types.definition);
            return FactoryLabel
                .create(
                    context,
                    effects,
                    this::labelAfter,
                    nok,
                    Types.definitionLabel,
                    Types.definitionLabelMarker,
                    Types.definitionLabelString)
                .step(code);
        }

        private State labelAfter(int code) {
            var lastTokenText = context.sliceSerialize(
                context.getLastEvent()
                    .token());
            identifier = NormalizeIdentifier
                .normalizeIdentifier(lastTokenText.substring(1, lastTokenText.length() - 1));

            if (code == Codes.colon) {
                effects.enter(Types.definitionMarker);
                effects.consume(code);
                effects.exit(Types.definitionMarker);

                // Note: blank lines can’t exist in content.
                return FactoryWhitespace.create(
                    effects,
                    FactoryDestination.create(
                        effects,
                        effects.attempt.hook(
                            titleConstruct,
                            FactorySpace.create(effects, this::after, Types.whitespace),
                            FactorySpace.create(effects, this::after, Types.whitespace)),
                        nok,
                        Types.definitionDestination,
                        Types.definitionDestinationLiteral,
                        Types.definitionDestinationLiteralMarker,
                        Types.definitionDestinationRaw,
                        Types.definitionDestinationString));
            }

            return nok.step(code);
        }

        private State after(int code) {
            if (code == Codes.eof || CharUtil.markdownLineEnding(code)) {
                effects.exit(Types.definition);

                if (!context.getParser().defined.contains(identifier)) {
                    context.getParser().defined.add(identifier);
                }

                return ok.step(code);
            }

            return nok.step(code);
        }

    }

    private static class TitleStateMachine {

        private final TokenizeContext context;
        private final Tokenizer.Effects effects;
        private final State ok;
        private final State nok;

        public TitleStateMachine(TokenizeContext context, Tokenizer.Effects effects, State ok, State nok) {

            this.context = context;
            this.effects = effects;
            this.ok = ok;
            this.nok = nok;
        }

        private State start(int code) {
            return CharUtil.markdownLineEndingOrSpace(code) ? FactoryWhitespace.create(effects, this::before)
                .step(code) : nok.step(code);
        }

        private State before(int code) {
            if (code == Codes.quotationMark || code == Codes.apostrophe || code == Codes.leftParenthesis) {
                return FactoryTitle
                    .create(
                        effects,
                        FactorySpace.create(effects, this::after, Types.whitespace),
                        nok,
                        Types.definitionTitle,
                        Types.definitionTitleMarker,
                        Types.definitionTitleString)
                    .step(code);
            }

            return nok.step(code);
        }

        private State after(int code) {
            return code == Codes.eof || CharUtil.markdownLineEnding(code) ? ok.step(code) : nok.step(code);
        }
    }
}
