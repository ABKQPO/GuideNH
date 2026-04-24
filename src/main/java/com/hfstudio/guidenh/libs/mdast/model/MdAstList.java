package com.hfstudio.guidenh.libs.mdast.model;

import java.io.IOException;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

/**
 * List (Parent) represents a list of items.
 * List can be used where flow content is expected. Its content model is list content.
 * For example, the following markdown:
 * 1. foo
 * Yields:
 * { type: 'list', ordered: true, start: 1, spread: false, children: [{ type: 'listItem', spread: false, children: [{
 * type: 'paragraph', children: [{type: 'text', value: 'foo'}] }] }] }
 */
public class MdAstList extends MdAstParent<MdAstListContent> implements MdAstFlowContent {

    public static final String TYPE = "list";

    /**
     * Represents that the items have been intentionally ordered (when true), or that the order of items is not
     * important (when false).
     */
    public boolean ordered;
    /**
     * Represents, when the ordered field is true, the starting number of the list.
     */
    public int start = 1;
    /**
     * Represents that one or more of its children are separated with a blank line from its siblings (when true), or not
     * (when false).
     */
    public boolean spread;

    @Override
    protected Class<MdAstListContent> childClass() {
        return MdAstListContent.class;
    }

    public MdAstList() {
        super(TYPE);
    }

    @Override
    protected void writeJson(JsonWriter writer) throws IOException {
        writer.name("ordered")
            .value(ordered);
        if (ordered) {
            writer.name("start")
                .value(start);
        }
        writer.name("spread")
            .value(spread);

        super.writeJson(writer);
    }

    @Override
    protected void readJson(JsonObject jsonObject) throws IOException {
        super.readJson(jsonObject);

        this.ordered = readJsonBoolean(jsonObject, "ordered");
        if (ordered) {
            this.start = readJsonInt(jsonObject, "start");
        } else {
            this.start = 1;
        }
        this.spread = readJsonBoolean(jsonObject, "spread");
    }
}
