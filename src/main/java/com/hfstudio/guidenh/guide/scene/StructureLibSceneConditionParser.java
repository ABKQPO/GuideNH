package com.hfstudio.guidenh.guide.scene;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class StructureLibSceneConditionParser {

    private StructureLibSceneConditionParser() {}

    @Nullable
    public static StructureLibSceneCondition parse(PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields element) {
        String structureName = MdxAttrs.getString(compiler, errorSink, element, "showWhenStructure", null);
        String tierExpression = MdxAttrs.getString(compiler, errorSink, element, "showWhenTier", null);
        String channelExpression = MdxAttrs.getString(compiler, errorSink, element, "showWhenChannels", null);
        try {
            return StructureLibSceneCondition.parse(structureName, tierExpression, channelExpression);
        } catch (IllegalArgumentException e) {
            if (errorSink != null) {
                errorSink.appendError(compiler, e.getMessage(), element);
            }
            return null;
        }
    }
}
