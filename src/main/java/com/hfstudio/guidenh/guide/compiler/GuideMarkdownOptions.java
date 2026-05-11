package com.hfstudio.guidenh.guide.compiler;

import com.hfstudio.guidenh.libs.mdast.MdastOptions;
import com.hfstudio.guidenh.libs.mdast.YamlFrontmatterExtension;
import com.hfstudio.guidenh.libs.mdast.gfm.GfmTableMdastExtension;
import com.hfstudio.guidenh.libs.mdast.gfmstrikethrough.GfmStrikethroughMdastExtension;
import com.hfstudio.guidenh.libs.mdast.guidemark.GuideMarkMdastExtension;
import com.hfstudio.guidenh.libs.mdast.guideunderline.GuideUnderlineMdastExtension;
import com.hfstudio.guidenh.libs.mdast.mdx.MdxMdastExtension;
import com.hfstudio.guidenh.libs.mdx.MdxSyntax;
import com.hfstudio.guidenh.libs.micromark.extensions.YamlFrontmatterSyntax;
import com.hfstudio.guidenh.libs.micromark.extensions.gfm.GfmTableSyntax;
import com.hfstudio.guidenh.libs.micromark.extensions.gfmstrikethrough.GfmStrikethroughSyntax;
import com.hfstudio.guidenh.libs.micromark.extensions.guidemark.GuideMarkSyntax;
import com.hfstudio.guidenh.libs.micromark.extensions.guideunderline.GuideUnderlineSyntax;

public class GuideMarkdownOptions {

    private static final MdastOptions RUNTIME = createBaseOptions();
    private static final MdastOptions SCENE_EDITOR = createBaseOptions();

    protected GuideMarkdownOptions() {}

    public static MdastOptions runtime() {
        return RUNTIME;
    }

    public static MdastOptions sceneEditor() {
        return SCENE_EDITOR;
    }

    private static MdastOptions createBaseOptions() {
        return new MdastOptions().withSyntaxExtension(MdxSyntax.INSTANCE)
            .withSyntaxExtension(YamlFrontmatterSyntax.INSTANCE)
            .withSyntaxExtension(GfmTableSyntax.INSTANCE)
            .withSyntaxExtension(GfmStrikethroughSyntax.INSTANCE)
            .withSyntaxExtension(GuideMarkSyntax.INSTANCE)
            .withSyntaxExtension(GuideUnderlineSyntax.INSTANCE)
            .withMdastExtension(MdxMdastExtension.INSTANCE)
            .withMdastExtension(YamlFrontmatterExtension.INSTANCE)
            .withMdastExtension(GfmTableMdastExtension.INSTANCE)
            .withMdastExtension(GfmStrikethroughMdastExtension.INSTANCE)
            .withMdastExtension(GuideMarkMdastExtension.INSTANCE)
            .withMdastExtension(GuideUnderlineMdastExtension.INSTANCE);
    }
}
