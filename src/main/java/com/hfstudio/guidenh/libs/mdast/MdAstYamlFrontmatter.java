package com.hfstudio.guidenh.libs.mdast;

import java.io.IOException;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstNode;

public class MdAstYamlFrontmatter extends MdAstNode implements MdAstAnyContent {

    public static final String TYPE = "yamlFrontmatter";
    public String value = "";

    public MdAstYamlFrontmatter() {
        super(TYPE);
    }

    @Override
    protected void writeJson(JsonWriter writer) throws IOException {
        writer.name("value")
            .value(value);
    }

    @Override
    protected void readJson(JsonObject jsonObject) throws IOException {
        value = readJsonString(jsonObject, "value", "");
    }

    @Override
    public void toText(StringBuilder buffer) {}
}
