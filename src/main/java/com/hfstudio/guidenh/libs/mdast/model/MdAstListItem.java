package com.hfstudio.guidenh.libs.mdast.model;

import java.io.IOException;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

/**
 * An item in a {@link MdAstList}.
 * It can be used where list content is expected. Its content model is flow content.
 * For example, the following markdown:
 * * bar
 * Yields:
 * { type: 'listItem', spread: false, children: [{ type: 'paragraph', children: [{type: 'text', value: 'bar'}] }] }
 */
public class MdAstListItem extends MdAstParent<MdAstFlowContent> implements MdAstListContent {

    public static final String TYPE = "listItem";

    public MdAstListItem() {
        super(TYPE);
    }

    /**
     * Represents that the item contains two or more children separated by a blank line (when true), or not (when
     * false).
     */
    public boolean spread;

    @Override
    protected Class<MdAstFlowContent> childClass() {
        return MdAstFlowContent.class;
    }

    @Override
    protected void writeJson(JsonWriter writer) throws IOException {
        writer.name("spread")
            .value(spread);
        super.writeJson(writer);
    }

    @Override
    protected void readJson(JsonObject jsonObject) throws IOException {
        super.readJson(jsonObject);
        this.spread = readJsonBoolean(jsonObject, "spread");
    }
}
