package com.hfstudio.guidenh.libs.mdast.model;

import java.io.IOException;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

/**
 * LinkReference (Parent) represents a hyperlink through association, or its original source if there is no association.
 * LinkReference can be used where phrasing content is expected. Its content model is static phrasing content.
 * LinkReferences should be associated with a Definition.
 * For example, the following markdown:
 * [alpha][Bravo]
 * Yields:
 * 
 * <pre>
 * {type:'linkReference',identifier:'bravo',label:'Bravo',referenceType:'full',children:[{type:'text',value:'alpha'}]}
 * </pre>
 */
public class MdAstLinkReference extends MdAstParent<MdAstStaticPhrasingContent>
    implements MdAstReference, MdAstPhrasingContent {

    public static final String TYPE = "linkReference";
    public String identifier;
    public String label;
    public MdAstReferenceType referenceType;

    public MdAstLinkReference() {
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
    public MdAstReferenceType referenceType() {
        return referenceType;
    }

    @Override
    protected Class<MdAstStaticPhrasingContent> childClass() {
        return MdAstStaticPhrasingContent.class;
    }

    @Override
    protected void writeJson(JsonWriter writer) throws IOException {
        writer.name("identifier")
            .value(identifier);
        writer.name("label")
            .value(label);
        writer.name("referenceType")
            .value(referenceType.getSerializedName());

        super.writeJson(writer);
    }

    @Override
    protected void readJson(JsonObject jsonObject) throws IOException {
        super.readJson(jsonObject);

        this.identifier = readJsonString(jsonObject, "identifier");
        this.label = readJsonString(jsonObject, "label");
        this.referenceType = MdAstReferenceType.fromSerializedName(readJsonString(jsonObject, "referenceType"));
    }
}
