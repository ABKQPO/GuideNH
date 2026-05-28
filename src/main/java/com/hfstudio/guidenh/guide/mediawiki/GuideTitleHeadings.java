package com.hfstudio.guidenh.guide.mediawiki;

import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.compiler.IndexingSink;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.internal.search.PageIndexer;
import com.hfstudio.guidenh.libs.mdast.model.MdAstHeading;
import com.hfstudio.guidenh.libs.unist.UnistNode;

public class GuideTitleHeadings {

    private GuideTitleHeadings() {}

    public static String resolveHeading1Title(Guide guide, ParsedGuidePage page) {
        for (Object child : page.getAstRoot()
            .children()) {
            if (!(child instanceof MdAstHeading heading) || heading.depth != 1) {
                continue;
            }
            StringBuilder title = new StringBuilder();
            IndexingSink sink = new IndexingSink() {

                @Override
                public void appendText(UnistNode parent, String text) {
                    title.append(text);
                }

                @Override
                public void appendBreak() {
                    title.append(' ');
                }
            };
            new PageIndexer(guide, guide.getExtensions(), page.getId()).indexContent(heading.children(), sink);
            if (!title.isEmpty()) {
                return title.toString();
            }
        }
        return page.getId()
            .toString();
    }
}
