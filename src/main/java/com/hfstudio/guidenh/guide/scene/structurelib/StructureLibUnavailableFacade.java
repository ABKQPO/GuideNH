package com.hfstudio.guidenh.guide.scene.structurelib;

public final class StructureLibUnavailableFacade implements StructureLibFacade {

    static final String UNAVAILABLE_MESSAGE = "StructureLib is not available in this environment";

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public StructureLibImportResult importScene(StructureLibImportRequest request) {
        return StructureLibImportResult.failure(UNAVAILABLE_MESSAGE);
    }
}
