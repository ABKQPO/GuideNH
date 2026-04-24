package com.hfstudio.guidenh.libs.mdast.mdx.model;

import java.io.IOException;

import com.google.gson.stream.JsonWriter;
import com.hfstudio.guidenh.libs.unist.UnistNode;

/**
 * Potential attributes of {@link MdxJsxElementFields}
 */
public interface MdxJsxAttributeNode extends UnistNode {

    void toJson(JsonWriter writer) throws IOException;
}
