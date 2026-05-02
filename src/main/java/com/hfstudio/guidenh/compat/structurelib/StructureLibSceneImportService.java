package com.hfstudio.guidenh.compat.structurelib;

import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.compat.Mods;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

import cpw.mods.fml.common.Optional;

public class StructureLibSceneImportService {

    public static final Logger LOG = LogManager.getLogger("GuideNH/ScenePreview");

    private final StructureLibFacade facade;

    public StructureLibSceneImportService() {
        this((Supplier<StructureLibFacade>) null);
    }

    public StructureLibSceneImportService(@Nullable Supplier<StructureLibFacade> facadeFactory) {
        this(resolveFacade(facadeFactory));
    }

    public StructureLibSceneImportService(@Nullable StructureLibFacade facade) {
        this.facade = facade != null ? facade : createDefaultFacade();
    }

    public boolean isAvailable() {
        try {
            return facade.isAvailable();
        } catch (Throwable t) {
            GuideDebugLog.warn(LOG, "StructureLib facade availability check failed", t);
            return false;
        }
    }

    public StructureLibImportResult importScene(@Nullable StructureLibImportRequest request) {
        if (request == null) {
            return StructureLibImportResult.failure("StructureLib import request cannot be null");
        }

        try {
            StructureLibImportResult result = facade.importScene(request);
            if (result != null) {
                return result;
            }
            return StructureLibImportResult.failure("StructureLib facade returned no import result");
        } catch (Throwable t) {
            GuideDebugLog.warn(LOG, "StructureLib import failed for controller {}", request.getController(), t);
            return StructureLibImportResult.failure(resolveFailureMessage(t));
        }
    }

    public static String resolveFailureMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.trim()
            .isEmpty()) {
            return "StructureLib import failed";
        }
        return "StructureLib import failed: " + message.trim();
    }

    public static StructureLibFacade resolveFacade(@Nullable Supplier<StructureLibFacade> facadeFactory) {
        if (facadeFactory == null) {
            return createDefaultFacade();
        }
        try {
            StructureLibFacade facade = facadeFactory.get();
            return facade != null ? facade : createDefaultFacade();
        } catch (Throwable t) {
            GuideDebugLog.warn(LOG, "StructureLib facade factory failed, falling back to default facade", t);
            return createDefaultFacade();
        }
    }

    public static StructureLibFacade createDefaultFacade() {
        if (!Mods.StructureLib.isModLoaded()) {
            return new StructureLibUnavailableFacade();
        }
        try {
            return createRuntimeFacade();
        } catch (Throwable t) {
            GuideDebugLog
                .warn(LOG, "Failed to initialize StructureLib runtime facade, falling back to unavailable facade", t);
            return new StructureLibUnavailableFacade();
        }
    }

    @Optional.Method(modid = "structurelib")
    private static StructureLibFacade createRuntimeFacade() {
        return new StructureLibRuntimeFacade();
    }
}
