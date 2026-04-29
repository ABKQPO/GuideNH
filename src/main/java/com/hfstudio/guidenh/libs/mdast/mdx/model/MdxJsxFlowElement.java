package com.hfstudio.guidenh.libs.mdast.mdx.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonWriter;
import com.hfstudio.guidenh.libs.mdast.model.MdAstFlowContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstNode;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParent;

public class MdxJsxFlowElement extends MdAstParent<MdAstFlowContent> implements MdxJsxElementFields, MdAstFlowContent {

    public static final String TYPE = "mdxJsxFlowElement";
    public String name;
    public List<MdxJsxAttributeNode> attributes;

    public MdxJsxFlowElement() {
        this("", new ArrayList<>());
    }

    public MdxJsxFlowElement(String name, List<MdxJsxAttributeNode> attributes) {
        super(TYPE);
        this.name = name;
        this.attributes = attributes;
    }

    @Override
    public @Nullable String name() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public List<MdxJsxAttributeNode> attributes() {
        return attributes;
    }

    @Override
    protected Class<MdAstFlowContent> childClass() {
        return MdAstFlowContent.class;
    }

    @Override
    protected void writeJson(JsonWriter writer) throws IOException {
        super.writeJson(writer);
        writer.name("name")
            .value(name);
        writer.name("attributes");
        writer.beginArray();
        for (var attribute : attributes) {
            attribute.toJson(writer);
        }
        writer.endArray();
    }

    @Override
    protected void readJson(JsonObject jsonObject) throws IOException {
        super.readJson(jsonObject);
        this.name = readJsonString(jsonObject, "name");
        this.attributes.clear();
        for (var attributeEl : jsonObject.getAsJsonArray("attributes")) {
            var mdxNode = MdAstNode.fromJson(attributeEl.getAsJsonObject());
            if (mdxNode instanceof MdxJsxAttributeNode attributeNode) {
                this.attributes.add(attributeNode);
            } else {
                throw new JsonSyntaxException("Unexpected attribute node " + mdxNode.type());
            }
        }
    }
}
