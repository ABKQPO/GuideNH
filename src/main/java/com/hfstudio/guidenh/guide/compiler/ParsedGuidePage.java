package com.hfstudio.guidenh.guide.compiler;

import java.util.Objects;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.libs.mdast.model.MdAstRoot;
import com.hfstudio.guidenh.libs.unist.UnistPoint;

public class ParsedGuidePage {

    private final String sourcePack;
    private final ResourceLocation id;
    private final String source;
    private volatile MdAstRoot astRoot;
    private final Frontmatter frontmatter;
    private final String language;
    private final @Nullable String parseFailureMessage;
    private final @Nullable UnistPoint parseFailureFrom;
    private final @Nullable UnistPoint parseFailureTo;

    @Deprecated
    public ParsedGuidePage(String sourcePack, ResourceLocation id, String source, MdAstRoot astRoot,
        Frontmatter frontmatter) {
        this(sourcePack, id, source, astRoot, frontmatter, "en_us", null);
    }

    public ParsedGuidePage(String sourcePack, ResourceLocation id, String source, MdAstRoot astRoot,
        Frontmatter frontmatter, String language) {
        this(sourcePack, id, source, astRoot, frontmatter, language, null, null, null);
    }

    public ParsedGuidePage(String sourcePack, ResourceLocation id, String source, MdAstRoot astRoot,
        Frontmatter frontmatter, String language, @Nullable String parseFailureMessage) {
        this(sourcePack, id, source, astRoot, frontmatter, language, parseFailureMessage, null, null);
    }

    public ParsedGuidePage(String sourcePack, ResourceLocation id, String source, MdAstRoot astRoot,
        Frontmatter frontmatter, String language, @Nullable String parseFailureMessage,
        @Nullable UnistPoint parseFailureFrom, @Nullable UnistPoint parseFailureTo) {
        this.sourcePack = sourcePack;
        this.id = id;
        this.source = source;
        this.astRoot = astRoot;
        this.frontmatter = frontmatter;
        this.language = Objects.requireNonNull(language, "language");
        this.parseFailureMessage = parseFailureMessage;
        this.parseFailureFrom = parseFailureFrom;
        this.parseFailureTo = parseFailureTo;
    }

    public String getSourcePack() {
        return sourcePack;
    }

    public ResourceLocation getId() {
        return id;
    }

    public Frontmatter getFrontmatter() {
        return frontmatter;
    }

    public MdAstRoot getAstRoot() {
        MdAstRoot r = astRoot;
        if (r != null) {
            return r;
        }
        synchronized (this) {
            r = astRoot;
            if (r != null) {
                return r;
            }
            ParsedGuidePage full = PageCompiler.parse(sourcePack, language, id, source);
            astRoot = full.astRoot;
            return astRoot;
        }
    }

    public String getLanguage() {
        return language;
    }

    public String getSource() {
        return source;
    }

    public @Nullable String getParseFailureMessage() {
        return parseFailureMessage;
    }

    public @Nullable UnistPoint getParseFailureFrom() {
        return parseFailureFrom;
    }

    public @Nullable UnistPoint getParseFailureTo() {
        return parseFailureTo;
    }

    public boolean hasParseFailure() {
        return parseFailureMessage != null && !parseFailureMessage.isEmpty();
    }

    @Override
    public String toString() {
        if (id.getResourceDomain()
            .equals(sourcePack)) {
            return id.toString();
        } else {
            return id + " (from " + sourcePack + ")";
        }
    }
}
