package com.hfstudio.guidenh.integration.preview;

import com.hfstudio.guidenh.guide.scene.snapshot.ServerPreviewSupplementRegistry;
import com.hfstudio.guidenh.integration.ae2.Ae2ServerPreviewRegistration;

/**
 * Registers all integration-layer server-preview supplement strategies into
 * {@link ServerPreviewSupplementRegistry}.
 */
public class GuideCompatStructurePreviewBootstrap {

    public GuideCompatStructurePreviewBootstrap() {}

    public static void registerServerPreviewSupplements() {
        Ae2ServerPreviewRegistration.register();
    }
}
