package com.hfstudio.guidenh.libs.mdast.mdx;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.libs.mdast.MdastContext;
import com.hfstudio.guidenh.libs.mdast.MdastContextProperty;
import com.hfstudio.guidenh.libs.mdast.MdastExtension;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxAttribute;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxAttributeNode;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxExpressionAttribute;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxTextElement;
import com.hfstudio.guidenh.libs.mdast.model.MdAstNode;
import com.hfstudio.guidenh.libs.mdast.model.MdAstPosition;
import com.hfstudio.guidenh.libs.micromark.ListUtils;
import com.hfstudio.guidenh.libs.micromark.Point;
import com.hfstudio.guidenh.libs.micromark.Token;

public class MdxMdastExtension {

    public static final MdastContextProperty<List<Tag>> TAG_STACK = new MdastContextProperty<>();
    public static final MdastContextProperty<Tag> TAG = new MdastContextProperty<>();

    public static final MdastExtension INSTANCE = MdastExtension.builder()
        .canContainEol("mdxJsxTextElement")
        .enter("mdxJsxFlowTag", MdxMdastExtension::enterMdxJsxTag)
        .enter("mdxJsxRecovery", MdxMdastExtension::enterMdxJsxRecovery)
        .enter("mdxJsxFlowTagClosingMarker", MdxMdastExtension::enterMdxJsxTagClosingMarker)
        .enter("mdxJsxFlowTagAttribute", MdxMdastExtension::enterMdxJsxTagAttribute)
        .enter("mdxJsxFlowTagExpressionAttribute", MdxMdastExtension::enterMdxJsxTagExpressionAttribute)
        .enter("mdxJsxFlowTagAttributeValueLiteral", MdxMdastExtension::buffer)
        .enter("mdxJsxFlowTagAttributeValueExpression", MdxMdastExtension::buffer)
        .enter("mdxJsxFlowTagSelfClosingMarker", MdxMdastExtension::enterMdxJsxTagSelfClosingMarker)
        .enter("mdxJsxTextTag", MdxMdastExtension::enterMdxJsxTag)
        .enter("mdxJsxTextTagClosingMarker", MdxMdastExtension::enterMdxJsxTagClosingMarker)
        .enter("mdxJsxTextTagAttribute", MdxMdastExtension::enterMdxJsxTagAttribute)
        .enter("mdxJsxTextTagExpressionAttribute", MdxMdastExtension::enterMdxJsxTagExpressionAttribute)
        .enter("mdxJsxTextTagAttributeValueLiteral", MdxMdastExtension::buffer)
        .enter("mdxJsxTextTagAttributeValueExpression", MdxMdastExtension::buffer)
        .enter("mdxJsxTextTagSelfClosingMarker", MdxMdastExtension::enterMdxJsxTagSelfClosingMarker)
        .exit("mdxJsxFlowTagClosingMarker", MdxMdastExtension::exitMdxJsxTagClosingMarker)
        .exit("mdxJsxFlowTagNamePrimary", MdxMdastExtension::exitMdxJsxTagNamePrimary)
        .exit("mdxJsxFlowTagNameMember", MdxMdastExtension::exitMdxJsxTagNameMember)
        .exit("mdxJsxFlowTagNameLocal", MdxMdastExtension::exitMdxJsxTagNameLocal)
        .exit("mdxJsxFlowTagExpressionAttribute", MdxMdastExtension::exitMdxJsxTagExpressionAttribute)
        .exit("mdxJsxFlowTagExpressionAttributeValue", MdxMdastExtension::data)
        .exit("mdxJsxFlowTagAttributeNamePrimary", MdxMdastExtension::exitMdxJsxTagAttributeNamePrimary)
        .exit("mdxJsxFlowTagAttributeNameLocal", MdxMdastExtension::exitMdxJsxTagAttributeNameLocal)
        .exit("mdxJsxFlowTagAttributeValueLiteral", MdxMdastExtension::exitMdxJsxTagAttributeValueLiteral)
        .exit("mdxJsxFlowTagAttributeValueLiteralValue", MdxMdastExtension::data)
        .exit("mdxJsxFlowTagAttributeValueExpression", MdxMdastExtension::exitMdxJsxTagAttributeValueExpression)
        .exit("mdxJsxFlowTagAttributeValueExpressionValue", MdxMdastExtension::data)
        .exit("mdxJsxFlowTagSelfClosingMarker", MdxMdastExtension::exitMdxJsxTagSelfClosingMarker)
        .exit("mdxJsxFlowTag", MdxMdastExtension::exitMdxJsxTag)
        .exit("mdxJsxTextTagClosingMarker", MdxMdastExtension::exitMdxJsxTagClosingMarker)
        .exit("mdxJsxTextTagNamePrimary", MdxMdastExtension::exitMdxJsxTagNamePrimary)
        .exit("mdxJsxTextTagNameMember", MdxMdastExtension::exitMdxJsxTagNameMember)
        .exit("mdxJsxTextTagNameLocal", MdxMdastExtension::exitMdxJsxTagNameLocal)
        .exit("mdxJsxTextTagExpressionAttribute", MdxMdastExtension::exitMdxJsxTagExpressionAttribute)
        .exit("mdxJsxTextTagExpressionAttributeValue", MdxMdastExtension::data)
        .exit("mdxJsxTextTagAttributeNamePrimary", MdxMdastExtension::exitMdxJsxTagAttributeNamePrimary)
        .exit("mdxJsxTextTagAttributeNameLocal", MdxMdastExtension::exitMdxJsxTagAttributeNameLocal)
        .exit("mdxJsxTextTagAttributeValueLiteral", MdxMdastExtension::exitMdxJsxTagAttributeValueLiteral)
        .exit("mdxJsxTextTagAttributeValueLiteralValue", MdxMdastExtension::data)
        .exit("mdxJsxTextTagAttributeValueExpression", MdxMdastExtension::exitMdxJsxTagAttributeValueExpression)
        .exit("mdxJsxTextTagAttributeValueExpressionValue", MdxMdastExtension::data)
        .exit("mdxJsxTextTagSelfClosingMarker", MdxMdastExtension::exitMdxJsxTagSelfClosingMarker)
        .exit("mdxJsxTextTag", MdxMdastExtension::exitMdxJsxTag)
        .build();

    private MdxMdastExtension() {}

    public static void buffer(MdastContext context, Token token) {
        context.buffer();
    }

    public static void data(MdastContext context, Token token) {
        context.getExtension().enter.get("data")
            .handle(context, token);
        context.getExtension().exit.get("data")
            .handle(context, token);
    }

    public static void enterMdxJsxTag(MdastContext context, Token token) {
        var tag = new Tag(token);
        if (!context.has(TAG_STACK)) {
            context.set(TAG_STACK, new ArrayList<>());
        }
        context.set(TAG, tag);
        context.buffer();
    }

    public static void enterMdxJsxRecovery(MdastContext context, Token token) {
        var tag = context.get(TAG);
        if (tag != null) {
            tag.recovered = true;
        }
    }

    public static void enterMdxJsxTagClosingMarker(MdastContext context, Token token) {
        var stack = getStack(context);

        if (stack.isEmpty()) {
            return;
        }
    }

    public static void enterMdxJsxTagAnyAttribute(MdastContext context, Token token) {
        var tag = getTag(context);

        if (tag.close) {
            return;
        }
    }

    public static void enterMdxJsxTagSelfClosingMarker(MdastContext context, Token token) {
        var tag = getTag(context);

        if (tag.close) {
            return;
        }
    }

    public static void exitMdxJsxTagClosingMarker(MdastContext context, Token token) {
        var tag = getTag(context);
        tag.close = true;
    }

    public static void exitMdxJsxTagNamePrimary(MdastContext context, Token token) {
        var tag = getTag(context);
        tag.name = context.sliceSerialize(token);
    }

    public static void exitMdxJsxTagNameMember(MdastContext context, Token token) {
        var tag = getTag(context);
        tag.name += '.' + context.sliceSerialize(token);
    }

    public static void exitMdxJsxTagNameLocal(MdastContext context, Token token) {
        var tag = getTag(context);
        tag.name += ':' + context.sliceSerialize(token);
    }

    public static void enterMdxJsxTagAttribute(MdastContext context, Token token) {
        var tag = getTag(context);
        enterMdxJsxTagAnyAttribute(context, token);
        var node = new MdxJsxAttribute();
        node.position = new MdAstPosition().withStart(token.start);
        tag.attributes.add(node);
    }

    public static void enterMdxJsxTagExpressionAttribute(MdastContext context, Token token) {
        var tag = getTag(context);
        enterMdxJsxTagAnyAttribute(context, token);
        var node = new MdxJsxExpressionAttribute();
        node.position = new MdAstPosition().withStart(token.start);
        tag.attributes.add(node);
        context.buffer();
    }

    public static void exitMdxJsxTagExpressionAttribute(MdastContext context, Token token) {
        var tag = getTag(context);
        var tail = (MdxJsxExpressionAttribute) tag.attributes.get(tag.attributes.size() - 1);
        tail.value = context.resume();
        if (tail.position != null) {
            tail.position.end = token.end;
        }
    }

    public static void exitMdxJsxTagAttributeNamePrimary(MdastContext context, Token token) {
        var tag = getTag(context);
        var node = (MdxJsxAttribute) tag.attributes.get(tag.attributes.size() - 1);
        node.name = context.sliceSerialize(token);
        if (node.position != null) {
            node.position.end = token.end;
        }
    }

    public static void exitMdxJsxTagAttributeNameLocal(MdastContext context, Token token) {
        var tag = getTag(context);
        var node = (MdxJsxAttribute) tag.attributes.get(tag.attributes.size() - 1);
        node.name += ':' + context.sliceSerialize(token);
        if (node.position != null) {
            node.position.end = token.end;
        }
    }

    public static void exitMdxJsxTagAttributeValueLiteral(MdastContext context, Token token) {
        var tag = getTag(context);
        var value = ParseEntities.parseEntities(context.resume());

        var lastAttr = tag.attributes.get(tag.attributes.size() - 1);
        if (lastAttr instanceof MdxJsxAttribute attribute) {
            attribute.setValue(value);
            if (attribute.position != null) {
                attribute.position.end = token.end;
            }
        } else if (lastAttr instanceof MdxJsxExpressionAttribute attribute) {
            attribute.value = value;
            if (attribute.position != null) {
                attribute.position.end = token.end;
            }
        } else {
            throw new IllegalStateException();
        }
    }

    public static void exitMdxJsxTagAttributeValueExpression(MdastContext context, Token token) {
        var tag = getTag(context);
        var tail = (MdxJsxAttribute) tag.attributes.get(tag.attributes.size() - 1);
        tail.setExpression(context.resume());
        if (tail.position != null) {
            tail.position.end = token.end;
        }
    }

    public static void exitMdxJsxTagSelfClosingMarker(MdastContext context, Token token) {
        var tag = getTag(context);

        tag.selfClosing = true;
    }

    public static void exitMdxJsxTag(MdastContext context, Token token) {
        var tag = getTag(context);
        var stack = getStack(context);
        var tail = stack.isEmpty() ? null : stack.get(stack.size() - 1);

        if (tag.close && tail != null && !Objects.equals(tail.name, tag.name)) {
            // Mismatched closing tag — ignore and continue.
        }

        // End of a tag, so drop the buffer.
        context.resume();

        if (tag.close) {
            ListUtils.pop(stack);
        } else {
            MdAstNode node;
            if (Objects.equals(token.type, "mdxJsxTextTag")) {
                var el = new MdxJsxTextElement(tag.name, tag.attributes);
                el.recovered = tag.recovered;
                node = el;
            } else {
                var el = new MdxJsxFlowElement(tag.name, tag.attributes);
                el.recovered = tag.recovered;
                node = el;
            }

            context.enter(node, token, MdxMdastExtension::onErrorRightIsTag);
        }

        if (tag.selfClosing || tag.close) {
            context.exit(token, MdxMdastExtension::onErrorLeftIsTag);
        } else {
            stack.add(tag);
        }
    }

    public static void onErrorRightIsTag(MdastContext context, @Nullable Token closing, Token open) {
        // Unclosed tag found when a parent (e.g. paragraph) is trying to close.
        // Recursively unwind the stack: each context.exit(closing) pops one more
        // mismatched pair, until the correct parent is reached and closed.
        if (closing != null) {
            context.exit(closing, (ctx, left, right) -> {
                if (left != null) {
                    ctx.exit(left);
                }
            });
        }
    }

    public static void onErrorLeftIsTag(MdastContext context, @Nullable Token a, Token b) {
        if (a != null) {
            context.exit(a, (ctx, left, right) -> {
                if (left != null) {
                    ctx.exit(left);
                }
            });
        }
    }

    /**
     * Serialize a tag, excluding attributes. `self-closing` is not supported, because we don’t need it yet.
     */
    public static String serializeAbbreviatedTag(Tag tag) {
        return "<" + (tag.close ? '/' : "") + ((tag.name != null ? (tag.name) : (""))) + ">";
    }

    public static class Tag {

        @Nullable
        String name;
        List<MdxJsxAttributeNode> attributes = new ArrayList<>();
        boolean close;
        boolean selfClosing;
        boolean recovered;
        Point start;
        Point end;

        public Tag(Token token) {
            start = token.start;
            end = token.end;
        }

        public MdAstPosition position() {
            return new MdAstPosition(start, end);
        }
    }

    public static List<Tag> getStack(MdastContext context) {
        return Objects.requireNonNull(context.get(TAG_STACK), "stack is missing from context");
    }

    public static Tag getTag(MdastContext context) {
        return Objects.requireNonNull(context.get(TAG), "tag is missing from context");
    }
}
