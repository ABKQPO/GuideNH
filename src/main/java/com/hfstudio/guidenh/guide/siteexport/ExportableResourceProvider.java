package com.hfstudio.guidenh.guide.siteexport;

import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.document.block.LytVisitor;

public interface ExportableResourceProvider {

    default void exportResources(ResourceExporter exporter) {}

    static void visit(LytNode root, ResourceExporter exporter) {
        root.visit(new LytVisitor() {

            @Override
            public Result beforeNode(LytNode node) {
                if (node instanceof ExportableResourceProvider provider) {
                    provider.exportResources(exporter);
                }
                return Result.CONTINUE;
            }
        }, true);
    }
}
