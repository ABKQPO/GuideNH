package com.hfstudio.guidenh.libs.mdast;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.hfstudio.guidenh.libs.micromark.Extension;
import com.hfstudio.guidenh.libs.micromark.html.ParseOptions;

public class MdastOptions extends ParseOptions {

    public final List<MdastExtension> mdastExtensions = new ArrayList<>();

    @Override
    public MdastOptions withSyntaxExtension(Extension extension) {
        super.withSyntaxExtension(extension);
        return this;
    }

    @Override
    public MdastOptions withSyntaxExtension(Consumer<Extension> customizer) {
        super.withSyntaxExtension(customizer);
        return this;
    }

    public MdastOptions withMdastExtension(MdastExtension extension) {
        mdastExtensions.add(extension);
        return this;
    }
}
