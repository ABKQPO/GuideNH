package com.hfstudio.guidenh.compat.structurelib;

public interface StructureLibFacade {

    boolean isAvailable();

    StructureLibImportResult importScene(StructureLibImportRequest request);
}
