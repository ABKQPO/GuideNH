package com.hfstudio.guidenh.compat.structurelib;

public class StructureLibUnavailableFacade implements StructureLibFacade {

    public static final String UNAVAILABLE_MESSAGE = "StructureLib is not available in this environment";

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public StructureLibImportResult importScene(StructureLibImportRequest request) {
        return StructureLibImportResult.failure(UNAVAILABLE_MESSAGE);
    }
}
