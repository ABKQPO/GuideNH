package com.hfstudio.guidenh.libs.mdx;

import com.hfstudio.guidenh.libs.micromark.Assert;
import com.hfstudio.guidenh.libs.micromark.CharUtil;

import com.hfstudio.guidenh.libs.micromark.Point;
import com.hfstudio.guidenh.libs.micromark.State;
import com.hfstudio.guidenh.libs.micromark.TokenizeContext;
import com.hfstudio.guidenh.libs.micromark.Tokenizer;
import com.hfstudio.guidenh.libs.micromark.Types;
import com.hfstudio.guidenh.libs.micromark.factory.FactorySpace;
import com.hfstudio.guidenh.libs.micromark.symbol.Codes;
import com.hfstudio.guidenh.libs.micromark.symbol.Constants;

public class FactoryMdxExpression {

    private FactoryMdxExpression() {}

    public static State create(TokenizeContext context, Tokenizer.Effects effects, State ok, String type,
        String markerType, String chunkType, boolean allowLazy, int startColumn) {

        class StateMachine {

            final Tokenizer.Event tail = context.getLastEvent();
            final int initialPrefix = tail != null && tail.token().type.equals(Types.linePrefix) ? tail.context()
                .sliceSerialize(tail.token(), true)
                .length() : 0;
            final int prefixExpressionIndent = initialPrefix != 0 ? initialPrefix + 1 : 0;
            int balance = 1;
            Point startPosition;

            State start(int code) {
                Assert.check(code == Codes.leftCurlyBrace, "expected `{`");
                effects.enter(type);
                effects.enter(markerType);
                effects.consume(code);
                effects.exit(markerType);
                startPosition = context.now();
                return this::atBreak;
            }

            State atBreak(int code) {
                if (code == Codes.eof) {
                    effects.exit(type);
                    effects.consume(code);
                    return ok;
                }

                if (code == Codes.rightCurlyBrace) {
                    return atClosingBrace(code);
                }

                if (CharUtil.markdownLineEnding(code)) {
                    effects.enter(Types.lineEnding);
                    effects.consume(code);
                    effects.exit(Types.lineEnding);
                    var prefixTagIndent = startColumn != 0 ? startColumn + Constants.tabSize
                        - context.now()
                            .column()
                        : 0;
                    var indent = Math.max(prefixExpressionIndent, prefixTagIndent);
                    return indent != 0 ? FactorySpace.create(effects, this::atBreak, Types.linePrefix, indent)
                        : this::atBreak;
                }

                var now = context.now();

                if (now.line() != startPosition.line() && !allowLazy && context.isOnLazyLine()) {
                    effects.exit(type);
                    effects.consume(code);
                    return ok;
                }

                effects.enter(chunkType);
                return inside(code);
            }

            State inside(int code) {
                if (code == Codes.eof || code == Codes.rightCurlyBrace || CharUtil.markdownLineEnding(code)) {
                    effects.exit(chunkType);
                    return atBreak(code);
                }

                if (code == Codes.leftCurlyBrace) {
                    effects.consume(code);
                    balance++;
                    return this::inside;
                }

                effects.consume(code);
                return this::inside;
            }

            State atClosingBrace(int code) {
                balance--;

                // Agnostic mode: count balanced braces.
                if (balance != 0) {
                    effects.enter(chunkType);
                    effects.consume(code);
                    return this::inside;
                }

                effects.enter(markerType);
                effects.consume(code);
                effects.exit(markerType);
                effects.exit(type);
                return ok;
            }
        }

        return new StateMachine()::start;
    }
}
