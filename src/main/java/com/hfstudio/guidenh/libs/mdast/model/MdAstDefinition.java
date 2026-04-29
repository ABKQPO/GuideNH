package com.hfstudio.guidenh.libs.mdast.model;

import java.io.IOException;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

/**
 * Represents a resource.
 * Definition can be used where content is expected. It has no content model.
 * Definition should be associated with LinkReferences and ImageReferences.
 * For example, the following markdown:
 * [Alpha]: https://example.com
 * Yields:
 * { type: 'definition', identifier: 'alpha', label: 'Alpha', url: 'https://example.com', title: null }
 */
public class MdAstDefinition extends MdAstNode implements MdAstAssociation, MdAstResource, MdAstContent {

    public static final String TYPE = "definition";
    public String identifier = "";
    public String label;
    public String url = "";
    public String title;

    public MdAstDefinition() {
        super(TYPE);
    }

    @Override
    public String identifier() {
        return identifier;
    }

    @Override
    public @Nullable String label() {
        return label;
    }

    @Override
    public String url() {
        return url;
    }

    @Override
    public @Nullable String title() {
        return title;
    }

    @Override
    public void toText(StringBuilder buffer) {
        buffer.append(label);
    }

    @Override
    protected void writeJson(JsonWriter writer) throws IOException {
        if (identifier != null) {
            writer.name("identifier")
                .value(identifier);
        }
        if (label != null) {
            writer.name("label")
                .value(label);
        }
        if (title != null) {
            writer.name("title")
                .value(title);
        }
        if (url != null) {
            writer.name("url")
                .value(url);
        }

        super.writeJson(writer);
    }

    @Override
    protected void readJson(JsonObject jsonObject) throws IOException {
        super.readJson(jsonObject);
        this.identifier = readJsonString(jsonObject, "identifier", null);
        this.label = readJsonString(jsonObject, "label", null);
        this.title = readJsonString(jsonObject, "title", null);
        this.url = readJsonString(jsonObject, "url", null);
    }
}
