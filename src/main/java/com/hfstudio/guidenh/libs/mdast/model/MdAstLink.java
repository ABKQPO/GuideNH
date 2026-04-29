package com.hfstudio.guidenh.libs.mdast.model;

import java.io.IOException;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

/**
 * Link includes Resource
 * Link (Parent) represents a hyperlink.
 * Link can be used where phrasing content is expected. Its content model is static phrasing content.
 * Link includes the mixin Resource.
 * For example, the following markdown:
 * [alpha](https://example.com "bravo")
 * Yields:
 * { type: 'link', url: 'https://example.com', title: 'bravo', children: [{type: 'text', value: 'alpha'}] }
 */
public class MdAstLink extends MdAstParent<MdAstStaticPhrasingContent> implements MdAstPhrasingContent, MdAstResource {

    public static final String TYPE = "link";
    public String url = "";
    public String title;

    public MdAstLink() {
        super(TYPE);
    }

    @Override
    protected Class<MdAstStaticPhrasingContent> childClass() {
        return MdAstStaticPhrasingContent.class;
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
    protected void writeJson(JsonWriter writer) throws IOException {
        if (title != null) {
            writer.name("title")
                .value(title);
        }
        writer.name("url")
            .value(url);
        super.writeJson(writer);
    }

    @Override
    protected void readJson(JsonObject jsonObject) throws IOException {
        super.readJson(jsonObject);
        this.title = readJsonString(jsonObject, "title", null);
        this.url = readJsonString(jsonObject, "url");
    }
}
