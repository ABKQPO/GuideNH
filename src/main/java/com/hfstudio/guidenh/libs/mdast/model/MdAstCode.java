package com.hfstudio.guidenh.libs.mdast.model;

import java.io.IOException;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

/**
 * Code (Literal) represents a block of preformatted text, such as ASCII art or computer code.
 * Code can be used where flow content is expected. Its content is represented by its value field.
 * This node relates to the phrasing content concept InlineCode.
 * For example, the following markdown:
 * foo()
 * Yields:
 * { type: 'code', lang: null, meta: null, value: 'foo()' }
 * And the following markdown:
 * ```js highlight-line="2" foo() bar() baz() ```
 * Yields:
 * { type: 'code', lang: 'javascript', meta: 'highlight-line="2"', value: 'foo()\nbar()\nbaz()' }
 */
public class MdAstCode extends MdAstLiteral implements MdAstFlowContent {

    public static final String TYPE = "code";

    public MdAstCode() {
        super(TYPE);
    }

    /**
     * The language of the code, if not-null.
     */
    @Nullable
    public String lang;

    /**
     * Can be not-null if lang is not-null. It represents custom information relating to the node.
     */
    @Nullable
    public String meta;

    @Override
    public void writeJson(JsonWriter writer) throws IOException {
        if (lang != null) {
            writer.name("lang")
                .value(lang);
        }
        if (meta != null) {
            writer.name("meta")
                .value(meta);
        }
        super.writeJson(writer);
    }

    @Override
    protected void readJson(JsonObject jsonObject) throws IOException {
        super.readJson(jsonObject);
        this.lang = readJsonString(jsonObject, "lang", null);
        this.meta = readJsonString(jsonObject, "meta", null);
    }
}
