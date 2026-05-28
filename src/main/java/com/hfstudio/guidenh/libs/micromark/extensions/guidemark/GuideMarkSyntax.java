package com.hfstudio.guidenh.libs.micromark.extensions.guidemark;

import java.util.ArrayList;
import java.util.List;

import com.hfstudio.guidenh.libs.micromark.Assert;
import com.hfstudio.guidenh.libs.micromark.ClassifyCharacter;
import com.hfstudio.guidenh.libs.micromark.Construct;
import com.hfstudio.guidenh.libs.micromark.Extension;
import com.hfstudio.guidenh.libs.micromark.ListUtils;
import com.hfstudio.guidenh.libs.micromark.State;
import com.hfstudio.guidenh.libs.micromark.Token;
import com.hfstudio.guidenh.libs.micromark.TokenProperty;
import com.hfstudio.guidenh.libs.micromark.TokenizeContext;
import com.hfstudio.guidenh.libs.micromark.Tokenizer;
import com.hfstudio.guidenh.libs.micromark.Types;
import com.hfstudio.guidenh.libs.micromark.symbol.Codes;
import com.hfstudio.guidenh.libs.micromark.symbol.Constants;

public class GuideMarkSyntax extends Extension {

    public static final Extension INSTANCE = new GuideMarkSyntax();

    public static final TokenProperty<Boolean> OPEN = new TokenProperty<>();
    public static final TokenProperty<Boolean> CLOSE = new TokenProperty<>();

    public GuideMarkSyntax() {
        Construct construct = new Construct();
        construct.name = "guideMark";
        construct.tokenize = GuideMarkSyntax::tokenize;
        construct.resolveAll = GuideMarkSyntax::resolveAll;
        text.put(Codes.equalsTo, List.of(construct));
        nullInsideSpan.add(construct.resolveAll);
        nullAttentionMarkers.add(Codes.equalsTo);
    }

    private static List<Tokenizer.Event> resolveAll(List<Tokenizer.Event> events, TokenizeContext context) {
        int index = -1;
        while (++index < events.size()) {
            Tokenizer.Event event = events.get(index);
            Token token = event.token();
            if (event.isEnter() && token.type.equals("guideMarkSequenceTemporary")
                && Boolean.TRUE.equals(token.get(CLOSE))) {
                int open = index;
                while (open-- > 0) {
                    Tokenizer.Event openEvent = events.get(open);
                    Token openToken = openEvent.token();
                    if (openEvent.isExit() && openToken.type.equals("guideMarkSequenceTemporary")
                        && Boolean.TRUE.equals(openToken.get(OPEN))
                        && token.size() == openToken.size()) {
                        events.get(index)
                            .token().type = "guideMarkSequence";
                        events.get(open)
                            .token().type = "guideMarkSequence";

                        Token wrapper = new Token();
                        wrapper.type = "guideMark";
                        wrapper.start = openToken.start;
                        wrapper.end = token.end;

                        Token inner = new Token();
                        inner.type = "guideMarkText";
                        inner.start = openToken.end;
                        inner.end = token.start;

                        List<Tokenizer.Event> nextEvents = new ArrayList<>();
                        nextEvents.add(Tokenizer.Event.enter(wrapper, context));
                        nextEvents.add(Tokenizer.Event.enter(openToken, context));
                        nextEvents.add(Tokenizer.Event.exit(openToken, context));
                        nextEvents.add(Tokenizer.Event.enter(inner, context));

                        var insideSpan = context.getParser().constructs.nullInsideSpan;
                        if (insideSpan != null) {
                            nextEvents.addAll(
                                Construct.resolveAll(insideSpan, ListUtils.slice(events, open + 1, index), context));
                        }

                        nextEvents.add(Tokenizer.Event.exit(inner, context));
                        nextEvents.add(Tokenizer.Event.enter(token, context));
                        nextEvents.add(Tokenizer.Event.exit(token, context));
                        nextEvents.add(Tokenizer.Event.exit(wrapper, context));

                        ListUtils.splice(events, open - 1, index - open + 3, nextEvents);
                        index = open + nextEvents.size() - 2;
                        break;
                    }
                }
            }
        }
        for (Tokenizer.Event event : events) {
            if (event.token().type.equals("guideMarkSequenceTemporary")) {
                event.token().type = Types.data;
            }
        }
        return events;
    }

    private static State tokenize(TokenizeContext context, Tokenizer.Effects effects, State ok, State nok) {
        class StateMachine {

            final int previous = context.getPrevious();
            final List<Tokenizer.Event> events = context.getEvents();
            int size;

            State start(int code) {
                Assert.check(code == Codes.equalsTo, "expected equals code");
                if (previous == Codes.equalsTo && !events.get(events.size() - 1)
                    .token().type.equals(Types.characterEscape)) {
                    return nok.step(code);
                }
                effects.enter("guideMarkSequenceTemporary");
                return more(code);
            }

            State more(int code) {
                int before = ClassifyCharacter.classifyCharacter(previous);
                if (code == Codes.equalsTo) {
                    if (size > 1) {
                        return nok.step(code);
                    }
                    effects.consume(code);
                    size++;
                    return this::more;
                }
                if (size != 2) {
                    return nok.step(code);
                }
                Token token = effects.exit("guideMarkSequenceTemporary");
                int after = ClassifyCharacter.classifyCharacter(code);
                token.set(OPEN, after == 0 || (after == Constants.attentionSideAfter && before != 0));
                token.set(CLOSE, before == 0 || (before == Constants.attentionSideAfter && after != 0));
                return ok.step(code);
            }
        }
        return new StateMachine()::start;
    }
}
