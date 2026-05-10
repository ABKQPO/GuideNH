package com.hfstudio.guidenh.integration.structurelib;

public interface StructureLibFacade {

    boolean isAvailable();

    StructureLibImportResult importScene(StructureLibImportRequest request);
}
