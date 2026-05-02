package com.hfstudio.guidenh.libs.micromark.extensions.guideunderline;

import java.util.ArrayList;
import java.util.Collections;
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

/**
 * 行内三种自定义下划线语法：
 * <ul>
 * <li>{@code ++text++} → 直下划线（{@code guideUnderline}）</li>
 * <li>{@code ^^text^^} → 波浪下划线（{@code guideWavyUnderline}）</li>
 * <li>{@code ::text::} → 点状下划线 / 着重号（{@code guideDottedUnderline}）</li>
 * </ul>
 * 实现思路与 GFM 删除线 (~~) 一致：每种语法注册一个 {@link Construct}，要求恰好两个相同分隔符配对。
 */
public class GuideUnderlineSyntax extends Extension {

    public static final Extension INSTANCE = new GuideUnderlineSyntax();

    public static final TokenProperty<Boolean> OPEN = new TokenProperty<>();
    public static final TokenProperty<Boolean> CLOSE = new TokenProperty<>();

    public GuideUnderlineSyntax() {
        register(
            Codes.plusSign,
            "guideUnderlineSequenceTemporary",
            "guideUnderlineSequence",
            "guideUnderline",
            "guideUnderlineText");
        register(
            Codes.caret,
            "guideWavyUnderlineSequenceTemporary",
            "guideWavyUnderlineSequence",
            "guideWavyUnderline",
            "guideWavyUnderlineText");
        register(
            Codes.colon,
            "guideDottedUnderlineSequenceTemporary",
            "guideDottedUnderlineSequence",
            "guideDottedUnderline",
            "guideDottedUnderlineText");
    }

    private void register(int markerCode, String tempType, String sequenceType, String wrapperType, String textType) {
        var construct = new Construct();
        construct.name = wrapperType;
        construct.tokenize = (context, effects, ok, nok) -> tokenize(context, effects, ok, nok, markerCode, tempType);
        construct.resolveAll = (events,
            context) -> resolveAll(events, context, tempType, sequenceType, wrapperType, textType);

        text.put(markerCode, Collections.singletonList(construct));
        nullInsideSpan.add(construct.resolveAll);
        nullAttentionMarkers.add(markerCode);
    }

    private static List<Tokenizer.Event> resolveAll(List<Tokenizer.Event> events, TokenizeContext context,
        String tempType, String sequenceType, String wrapperType, String textType) {
        int index = -1;

        while (++index < events.size()) {
            var event = events.get(index);
            var token = event.token();

            if (event.isEnter() && token.type.equals(tempType) && Boolean.TRUE.equals(token.get(CLOSE))) {

                int open = index;

                while (open-- > 0) {
                    var openEvent = events.get(open);
                    var openToken = openEvent.token();

                    if (openEvent.isExit() && openToken.type.equals(tempType)
                        && Boolean.TRUE.equals(openToken.get(OPEN))
                        && token.size() == openToken.size()) {

                        events.get(index)
                            .token().type = sequenceType;
                        events.get(open)
                            .token().type = sequenceType;

                        var wrapper = new Token();
                        wrapper.type = wrapperType;
                        wrapper.start = openToken.start;
                        wrapper.end = token.end;

                        var inner = new Token();
                        inner.type = textType;
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

        for (var event : events) {
            if (event.token().type.equals(tempType)) {
                event.token().type = Types.data;
            }
        }

        return events;
    }

    private static State tokenize(TokenizeContext context, Tokenizer.Effects effects, State ok, State nok,
        int markerCode, String tempType) {
        class StateMachine {

            final int previous = context.getPrevious();
            final List<Tokenizer.Event> events = context.getEvents();
            int size = 0;

            State start(int code) {
                Assert.check(code == markerCode, "expected marker code");

                if (previous == markerCode && !events.get(events.size() - 1)
                    .token().type.equals(Types.characterEscape)) {
                    return nok.step(code);
                }

                effects.enter(tempType);
                return more(code);
            }

            State more(int code) {
                var before = ClassifyCharacter.classifyCharacter(previous);

                if (code == markerCode) {
                    if (size > 1) {
                        // No more than two markers allowed; a third aborts the construct.
                        return nok.step(code);
                    }
                    effects.consume(code);
                    size++;
                    return this::more;
                }

                if (size < 2) {
                    // Require exactly two markers (single marker is not the underline syntax).
                    return nok.step(code);
                }

                var token = effects.exit(tempType);
                var after = ClassifyCharacter.classifyCharacter(code);

                token.set(OPEN, after == 0 || (after == Constants.attentionSideAfter && before != 0));
                token.set(CLOSE, before == 0 || (before == Constants.attentionSideAfter && after != 0));

                return ok.step(code);
            }
        }

        return new StateMachine()::start;
    }
}
