package com.hfstudio.guidenh.guide.scene.structurelib;

public interface StructureLibFacade {

    boolean isAvailable();

    StructureLibImportResult importScene(StructureLibImportRequest request);
}
