package com.hfstudio.guidenh.libs.mdx;

import static com.hfstudio.guidenh.libs.mdx.EcmaScriptIdentifiers.isCont;
import static com.hfstudio.guidenh.libs.mdx.EcmaScriptIdentifiers.isStart;

import com.hfstudio.guidenh.libs.micromark.Assert;
import com.hfstudio.guidenh.libs.micromark.CharUtil;
import com.hfstudio.guidenh.libs.micromark.Construct;
import com.hfstudio.guidenh.libs.micromark.Point;
import com.hfstudio.guidenh.libs.micromark.State;
import com.hfstudio.guidenh.libs.micromark.Token;
import com.hfstudio.guidenh.libs.micromark.TokenProperty;
import com.hfstudio.guidenh.libs.micromark.TokenizeContext;
import com.hfstudio.guidenh.libs.micromark.Tokenizer;
import com.hfstudio.guidenh.libs.micromark.Types;
import com.hfstudio.guidenh.libs.micromark.factory.FactorySpace;
import com.hfstudio.guidenh.libs.micromark.symbol.Codes;
import com.hfstudio.guidenh.libs.micromark.symbol.Constants;

public class FactoryTag {

    private FactoryTag() {}

    /**
     * Marker set on a tag token when recovery happens at EOF (where a separate
     * mdxJsxRecovery token cannot be emitted because consume(EOF) must be the
     * last event). Read by {@code MdxMdastExtension.exitMdxJsxTag}.
     */
    public static final TokenProperty<Boolean> RECOVERED_AT_EOF = new TokenProperty<>();

    public static final Construct lazyLineEnd;

    static {
        lazyLineEnd = new Construct();
        lazyLineEnd.tokenize = FactoryTag::tokenizeLazyLineEnd;
        lazyLineEnd.partial = true;
    }

    public static State create(TokenizeContext context, Tokenizer.Effects effects, State ok, State nok,
        boolean allowLazy, String tagType, String tagMarkerType, String tagClosingMarkerType,
        String tagSelfClosingMarker, String tagNameType, String tagNamePrimaryType, String tagNameMemberMarkerType,
        String tagNameMemberType, String tagNamePrefixMarkerType, String tagNameLocalType,
        String tagExpressionAttributeType, String tagExpressionAttributeMarkerType,
        String tagExpressionAttributeValueType, String tagAttributeType, String tagAttributeNameType,
        String tagAttributeNamePrimaryType, String tagAttributeNamePrefixMarkerType, String tagAttributeNameLocalType,
        String tagAttributeInitializerMarkerType, String tagAttributeValueLiteralType,
        String tagAttributeValueLiteralMarkerType, String tagAttributeValueLiteralValueType,
        String tagAttributeValueExpressionType, String tagAttributeValueExpressionMarkerType,
        String tagAttributeValueExpressionValueType) {

        class StateMachine {

            State returnState;
            Integer marker;
            Point startPoint;
            Token tagToken;

            State start(int code) {
                Assert.check(code == Codes.lessThan, "expected `<`");
                startPoint = context.now();
                tagToken = effects.enter(tagType);
                effects.enter(tagMarkerType);
                effects.consume(code);
                effects.exit(tagMarkerType);
                return this::afterStart;
            }

            State afterStart(int code) {
                // Deviate from JSX, which allows arbitrary whitespace.
                // See: <https://github.com/micromark/micromark-extension-mdx-jsx/issues/7>.
                if (CharUtil.markdownLineEnding(code) || CharUtil.markdownSpace(code)) {
                    return nok.step(code);
                }

                // Any other ES whitespace does not get this treatment.
                returnState = this::beforeName;
                return optionalEsWhitespace(code);
            }

            // Right after `<`, before an optional name.
            State beforeName(int code) {
                // Closing tag.
                if (code == Codes.slash) {
                    effects.enter(tagClosingMarkerType);
                    effects.consume(code);
                    effects.exit(tagClosingMarkerType);
                    returnState = this::beforeClosingTagName;
                    return this::optionalEsWhitespace;
                }

                // Fragment opening tag.
                if (code == Codes.greaterThan) {
                    return tagEnd(code);
                }

                // Start of a name.
                if (isPascalTagStart(code)) {
                    effects.enter(tagNameType);
                    effects.enter(tagNamePrimaryType);
                    effects.consume(code);
                    return this::primaryName;
                }

                return nok.step(code);
            }

            // At the start of a closing tag, right after `</`.
            State beforeClosingTagName(int code) {
                // Fragment closing tag.
                if (code == Codes.greaterThan) {
                    return tagEnd(code);
                }

                // Start of a closing tag name.
                if (isPascalTagStart(code)) {
                    effects.enter(tagNameType);
                    effects.enter(tagNamePrimaryType);
                    effects.consume(code);
                    return this::primaryName;
                }

                return nok.step(code);
            }

            // Inside the primary name.
            State primaryName(int code) {
                // Continuation of name: remain.
                if (code == Codes.dash || (code != Codes.eof && isCont(code))) {
                    effects.consume(code);
                    return this::primaryName;
                }

                // End of name.
                if (code == Codes.dot || code == Codes.slash
                    || code == Codes.colon
                    || code == Codes.greaterThan
                    || code == Codes.leftCurlyBrace
                    || CharUtil.markdownLineEndingOrSpace(code)
                    || CharUtil.unicodeWhitespace(code)) {
                    effects.exit(tagNamePrimaryType);
                    returnState = this::afterPrimaryName;
                    return optionalEsWhitespace(code);
                }

                return recover(code, tagNamePrimaryType, tagNameType);
            }

            // After a name.
            State afterPrimaryName(int code) {
                // Start of a member name.
                if (code == Codes.dot) {
                    effects.enter(tagNameMemberMarkerType);
                    effects.consume(code);
                    effects.exit(tagNameMemberMarkerType);
                    returnState = this::beforeMemberName;
                    return this::optionalEsWhitespace;
                }

                // Start of a local name.
                if (code == Codes.colon) {
                    effects.enter(tagNamePrefixMarkerType);
                    effects.consume(code);
                    effects.exit(tagNamePrefixMarkerType);
                    returnState = this::beforeLocalName;
                    return this::optionalEsWhitespace;
                }

                // End of name.
                if (code == Codes.slash || code == Codes.greaterThan
                    || code == Codes.leftCurlyBrace
                    || (code != Codes.eof && isStart(code))) {
                    effects.exit(tagNameType);
                    return beforeAttribute(code);
                }

                return recover(code, tagNameType);
            }

            // We’ve seen a `.` and are expecting a member name.
            State beforeMemberName(int code) {
                // Start of a member name.
                if (code != Codes.eof && isStart(code)) {
                    effects.enter(tagNameMemberType);
                    effects.consume(code);
                    return this::memberName;
                }

                return recover(code, tagNameType);
            }

            // Inside the member name.
            State memberName(int code) {
                // Continuation of member name: stay in state
                if (code == Codes.dash || (code != Codes.eof && isCont(code))) {
                    effects.consume(code);
                    return this::memberName;
                }

                // End of member name (note that namespaces and members can’t be combined).
                if (code == Codes.dot || code == Codes.slash
                    || code == Codes.greaterThan
                    || code == Codes.leftCurlyBrace
                    || CharUtil.markdownLineEndingOrSpace(code)
                    || CharUtil.unicodeWhitespace(code)) {
                    effects.exit(tagNameMemberType);
                    returnState = this::afterMemberName;
                    return optionalEsWhitespace(code);
                }

                return recover(code, tagNameMemberType, tagNameType);
            }

            // After a member name: this is the same as `afterPrimaryName` but we don’t
            // expect colons.
            State afterMemberName(int code) {
                // Start another member name.
                if (code == Codes.dot) {
                    effects.enter(tagNameMemberMarkerType);
                    effects.consume(code);
                    effects.exit(tagNameMemberMarkerType);
                    returnState = this::beforeMemberName;
                    return this::optionalEsWhitespace;
                }

                // End of name.
                if (code == Codes.slash || code == Codes.greaterThan
                    || code == Codes.leftCurlyBrace
                    || (code != Codes.eof && isStart(code))) {
                    effects.exit(tagNameType);
                    return beforeAttribute(code);
                }

                return recover(code, tagNameType);
            }

            // We’ve seen a `:`, and are expecting a local name.
            State beforeLocalName(int code) {
                // Start of a local name.
                if (code != Codes.eof && isStart(code)) {
                    effects.enter(tagNameLocalType);
                    effects.consume(code);
                    return this::localName;
                }

                return recover(code, tagNameType);
            }

            // Inside the local name.
            State localName(int code) {
                // Continuation of local name: stay in state
                if (code == Codes.dash || (code != Codes.eof && isCont(code))) {
                    effects.consume(code);
                    return this::localName;
                }

                // End of local name (note that we don’t expect another colon, or a member).
                if (code == Codes.slash || code == Codes.greaterThan
                    || code == Codes.leftCurlyBrace
                    || CharUtil.markdownLineEndingOrSpace(code)
                    || CharUtil.unicodeWhitespace(code)) {
                    effects.exit(tagNameLocalType);
                    returnState = this::afterLocalName;
                    return optionalEsWhitespace(code);
                }

                return recover(code, tagNameLocalType, tagNameType);
            }

            // After a local name: this is the same as `afterPrimaryName` but we don’t
            // expect colons or periods.
            State afterLocalName(int code) {
                // End of name.
                if (code == Codes.slash || code == Codes.greaterThan
                    || code == Codes.leftCurlyBrace
                    || (code != Codes.eof && isStart(code))) {
                    effects.exit(tagNameType);
                    return beforeAttribute(code);
                }

                return recover(code, tagNameType);
            }

            State beforeAttribute(int code) {
                // Mark as self closing.
                if (code == Codes.slash) {
                    effects.enter(tagSelfClosingMarker);
                    effects.consume(code);
                    effects.exit(tagSelfClosingMarker);
                    returnState = this::selfClosing;
                    return this::optionalEsWhitespace;
                }

                // End of tag.
                if (code == Codes.greaterThan) {
                    return tagEnd(code);
                }

                // Attribute expression.
                if (code == Codes.leftCurlyBrace) {
                    Assert.check(startPoint != null, "expected `startPoint` to be defined");
                    return FactoryMdxExpression
                        .create(
                            context,
                            effects,
                            this::afterAttributeExpression,
                            tagExpressionAttributeType,
                            tagExpressionAttributeMarkerType,
                            tagExpressionAttributeValueType,
                            allowLazy,
                            startPoint.column())
                        .step(code);
                }

                // Start of an attribute name.
                if (code != Codes.eof && isStart(code)) {
                    effects.enter(tagAttributeType);
                    effects.enter(tagAttributeNameType);
                    effects.enter(tagAttributeNamePrimaryType);
                    effects.consume(code);
                    return this::attributePrimaryName;
                }

                return recover(code);
            }

            // At the start of an attribute expression.
            State afterAttributeExpression(int code) {
                returnState = this::beforeAttribute;
                return optionalEsWhitespace(code);
            }

            // In the attribute name.
            State attributePrimaryName(int code) {
                // Continuation of the attribute name.
                if (code == Codes.dash || (code != Codes.eof && isCont(code))) {
                    effects.consume(code);
                    return this::attributePrimaryName;
                }

                // End of attribute name or tag.
                if (code == Codes.slash || code == Codes.colon
                    || code == Codes.equalsTo
                    || code == Codes.greaterThan
                    || code == Codes.leftCurlyBrace
                    || CharUtil.markdownLineEndingOrSpace(code)
                    || CharUtil.unicodeWhitespace(code)) {
                    effects.exit(tagAttributeNamePrimaryType);
                    returnState = this::afterAttributePrimaryName;
                    return optionalEsWhitespace(code);
                }

                return recover(code, tagAttributeNamePrimaryType, tagAttributeNameType, tagAttributeType);
            }

            // After an attribute name, probably finding an equals.
            State afterAttributePrimaryName(int code) {
                // Start of a local name.
                if (code == Codes.colon) {
                    effects.enter(tagAttributeNamePrefixMarkerType);
                    effects.consume(code);
                    effects.exit(tagAttributeNamePrefixMarkerType);
                    returnState = this::beforeAttributeLocalName;
                    return this::optionalEsWhitespace;
                }

                // Start of an attribute value.
                if (code == Codes.equalsTo) {
                    effects.exit(tagAttributeNameType);
                    effects.enter(tagAttributeInitializerMarkerType);
                    effects.consume(code);
                    effects.exit(tagAttributeInitializerMarkerType);
                    returnState = this::beforeAttributeValue;
                    return this::optionalEsWhitespace;
                }

                // End of tag / new attribute.
                if (code == Codes.slash || code == Codes.greaterThan
                    || code == Codes.leftCurlyBrace
                    || CharUtil.markdownLineEndingOrSpace(code)
                    || CharUtil.unicodeWhitespace(code)
                    || (code != Codes.eof && isStart(code))) {
                    effects.exit(tagAttributeNameType);
                    effects.exit(tagAttributeType);
                    returnState = this::beforeAttribute;
                    return optionalEsWhitespace(code);
                }

                return recover(code, tagAttributeNameType, tagAttributeType);
            }

            // We’ve seen a `:`, and are expecting a local name.
            State beforeAttributeLocalName(int code) {
                // Start of a local name.
                if (code != Codes.eof && isStart(code)) {
                    effects.enter(tagAttributeNameLocalType);
                    effects.consume(code);
                    return this::attributeLocalName;
                }

                return recover(code, tagAttributeNameType, tagAttributeType);
            }

            // In the local attribute name.
            State attributeLocalName(int code) {
                // Continuation of the local attribute name.
                if (code == Codes.dash || (code != Codes.eof && isCont(code))) {
                    effects.consume(code);
                    return this::attributeLocalName;
                }

                // End of tag / attribute name.
                if (code == Codes.slash || code == Codes.equalsTo
                    || code == Codes.greaterThan
                    || code == Codes.leftCurlyBrace
                    || CharUtil.markdownLineEndingOrSpace(code)
                    || CharUtil.unicodeWhitespace(code)) {
                    effects.exit(tagAttributeNameLocalType);
                    effects.exit(tagAttributeNameType);
                    returnState = this::afterAttributeLocalName;
                    return optionalEsWhitespace(code);
                }

                return recover(code, tagAttributeNameLocalType, tagAttributeNameType, tagAttributeType);
            }

            // After a local attribute name, expecting an equals.
            State afterAttributeLocalName(int code) {
                // Start of an attribute value.
                if (code == Codes.equalsTo) {
                    effects.enter(tagAttributeInitializerMarkerType);
                    effects.consume(code);
                    effects.exit(tagAttributeInitializerMarkerType);
                    returnState = this::beforeAttributeValue;
                    return this::optionalEsWhitespace;
                }

                // End of tag / new attribute.
                if (code == Codes.slash || code == Codes.greaterThan
                    || code == Codes.leftCurlyBrace
                    || (code != Codes.eof && isStart(code))) {
                    effects.exit(tagAttributeType);
                    return beforeAttribute(code);
                }

                return recover(code, tagAttributeType);
            }

            // After an attribute value initializer, expecting quotes and such.
            State beforeAttributeValue(int code) {
                // Start of double- or single quoted value.
                if (code == Codes.quotationMark || code == Codes.apostrophe) {
                    effects.enter(tagAttributeValueLiteralType);
                    effects.enter(tagAttributeValueLiteralMarkerType);
                    effects.consume(code);
                    effects.exit(tagAttributeValueLiteralMarkerType);
                    marker = code;
                    return this::attributeValueQuotedStart;
                }

                // Start of an assignment expression.
                if (code == Codes.leftCurlyBrace) {
                    Assert.check(startPoint != null, "expected `startPoint` to be defined");
                    return FactoryMdxExpression
                        .create(
                            context,
                            effects,
                            this::afterAttributeValueExpression,
                            tagAttributeValueExpressionType,
                            tagAttributeValueExpressionMarkerType,
                            tagAttributeValueExpressionValueType,
                            allowLazy,
                            startPoint.column())
                        .step(code);
                }

                return recover(code, tagAttributeType);
            }

            State afterAttributeValueExpression(int code) {
                effects.exit(tagAttributeType);
                returnState = this::beforeAttribute;
                return optionalEsWhitespace(code);
            }

            // At the start of a quoted attribute value.
            State attributeValueQuotedStart(int code) {
                Assert.check(marker != null, "expected `marker` to be defined");

                if (code == Codes.eof) {
                    return recover(code, tagAttributeValueLiteralType, tagAttributeType);
                }

                if (code == marker) {
                    effects.enter(tagAttributeValueLiteralMarkerType);
                    effects.consume(code);
                    effects.exit(tagAttributeValueLiteralMarkerType);
                    effects.exit(tagAttributeValueLiteralType);
                    effects.exit(tagAttributeType);
                    marker = null;
                    returnState = this::beforeAttribute;
                    return this::optionalEsWhitespace;
                }

                if (CharUtil.markdownLineEnding(code)) {
                    returnState = this::attributeValueQuotedStart;
                    return optionalEsWhitespace(code);
                }

                effects.enter(tagAttributeValueLiteralValueType);
                return attributeValueQuoted(code);
            }

            // In a quoted attribute value.
            State attributeValueQuoted(int code) {
                if (code == Codes.eof || code == marker || CharUtil.markdownLineEnding(code)) {
                    effects.exit(tagAttributeValueLiteralValueType);
                    return attributeValueQuotedStart(code);
                }

                // Continuation.
                effects.consume(code);
                return this::attributeValueQuoted;
            }

            // Right after the slash on a tag, e.g., `<asd /`.
            private State selfClosing(int code) {
                // End of tag.
                if (code == Codes.greaterThan) {
                    return tagEnd(code);
                }

                return recover(code);
            }

            // At a `>`.
            State tagEnd(int code) {
                Assert.check(code == Codes.greaterThan, "expected `>`");
                effects.enter(tagMarkerType);
                effects.consume(code);
                effects.exit(tagMarkerType);
                effects.exit(tagType);
                return ok;
            }

            // Optionally start whitespace.
            State optionalEsWhitespace(int code) {
                if (CharUtil.markdownLineEnding(code)) {
                    effects.enter(Types.lineEnding);
                    effects.consume(code);
                    effects.exit(Types.lineEnding);
                    return FactorySpace
                        .create(effects, this::optionalEsWhitespace, Types.linePrefix, Constants.tabSize);
                }

                if (CharUtil.markdownSpace(code) || CharUtil.unicodeWhitespace(code)) {
                    effects.enter("esWhitespace");
                    return optionalEsWhitespaceContinue(code);
                }

                return returnState.step(code);
            }

            // Continue optional whitespace.
            State optionalEsWhitespaceContinue(int code) {
                if (CharUtil.markdownLineEnding(code)
                    || !(CharUtil.markdownSpace(code) || CharUtil.unicodeWhitespace(code))) {
                    effects.exit("esWhitespace");
                    return optionalEsWhitespace(code);
                }

                effects.consume(code);
                return this::optionalEsWhitespaceContinue;
            }

            // Recover from a nonconforming character: exit any open tokens, close the tag,
            // and return ok so parsing can continue. This makes the tokenizer error-tolerant
            // so autocomplete can always query the AST even for incomplete input.
            private State recover(int code, String... openTokens) {
                for (String openToken : openTokens) {
                    effects.exit(openToken);
                }
                if (code == Codes.eof) {
                    // Cannot emit mdxJsxRecovery here — consume(EOF) must be the
                    // final event (see Tokenizer.Effects.consume). Mark the tag
                    // token directly when the tag NAME is still in progress
                    // (openTokens > 1 means we're inside a name-content token).
                    // When openTokens <= 1 the name is complete and the cursor
                    // is in the attribute area — don't flag as recovered.
                    if (openTokens.length > 1) {
                        tagToken.set(RECOVERED_AT_EOF, true);
                    }
                    effects.exit(tagType);
                    effects.consume(code);
                } else {
                    // Emit recovery marker BEFORE exiting the tag so the mdast
                    // handler can set tag.recovered before the AST node is created.
                    effects.enter("mdxJsxRecovery");
                    effects.consume(code);
                    effects.exit("mdxJsxRecovery");
                    effects.exit(tagType);
                }
                return ok;
            }
        }

        return new StateMachine()::start;
    }

    public static State tokenizeLazyLineEnd(TokenizeContext context, Tokenizer.Effects effects, State ok, State nok) {
        class StateMachine {

            State start(int code) {
                Assert.check(CharUtil.markdownLineEnding(code), "expected eol");
                effects.enter(Types.lineEnding);
                effects.consume(code);
                effects.exit(Types.lineEnding);
                return this::lineStart;
            }

            private State lineStart(int code) {
                return context.isOnLazyLine() ? nok.step(code) : ok.step(code);
            }
        }

        return new StateMachine()::start;
    }

    private static boolean isPascalTagStart(int code) {
        return code >= Codes.uppercaseA && code <= Codes.uppercaseZ;
    }

}
