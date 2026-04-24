package com.hfstudio.guidenh.guide.siteexport;

public interface ExportableResourceProvider {

    default void exportResources(ResourceExporter exporter) {}
}
