package com.hfstudio.guidenh.integration.structurelib;

import java.util.ArrayList;
import java.util.List;

public class StructureLibControllerIntegrationRegistry {

    private static final StructureLibControllerIntegrationRegistry GLOBAL = new StructureLibControllerIntegrationRegistry();

    private final List<StructureLibControllerDiscoveryIntegration> discoveryIntegrations = new ArrayList<>();
    private final List<StructureLibControllerPlacementIntegration> placementIntegrations = new ArrayList<>();
    private final List<StructureLibPreviewItemProvider> previewItemProviders = new ArrayList<>();
    private final List<StructureLibPreviewStateSynchronizer> previewStateSynchronizers = new ArrayList<>();

    public StructureLibControllerIntegrationRegistry() {}

    public static StructureLibControllerIntegrationRegistry global() {
        return GLOBAL;
    }

    public synchronized void registerDiscoveryIntegration(StructureLibControllerDiscoveryIntegration integration) {
        if (integration != null && !containsIntegrationType(discoveryIntegrations, integration.getClass())) {
            discoveryIntegrations.add(integration);
        }
    }

    public synchronized void registerPlacementIntegration(StructureLibControllerPlacementIntegration integration) {
        if (integration != null && !containsIntegrationType(placementIntegrations, integration.getClass())) {
            placementIntegrations.add(integration);
        }
    }

    public synchronized void registerPreviewItemProvider(StructureLibPreviewItemProvider provider) {
        if (provider != null && !containsIntegrationType(previewItemProviders, provider.getClass())) {
            previewItemProviders.add(provider);
        }
    }

    public synchronized void registerPreviewStateSynchronizer(StructureLibPreviewStateSynchronizer synchronizer) {
        if (synchronizer != null && !containsIntegrationType(previewStateSynchronizers, synchronizer.getClass())) {
            previewStateSynchronizers.add(synchronizer);
        }
    }

    public synchronized List<StructureLibControllerDiscoveryIntegration> discoveryIntegrations() {
        return List.copyOf(discoveryIntegrations);
    }

    public synchronized List<StructureLibControllerPlacementIntegration> placementIntegrations() {
        return List.copyOf(placementIntegrations);
    }

    public synchronized List<StructureLibPreviewItemProvider> previewItemProviders() {
        return List.copyOf(previewItemProviders);
    }

    public synchronized List<StructureLibPreviewStateSynchronizer> previewStateSynchronizers() {
        return List.copyOf(previewStateSynchronizers);
    }

    private boolean containsIntegrationType(List<?> integrations, Class<?> integrationType) {
        for (Object integration : integrations) {
            if (integration.getClass() == integrationType) {
                return true;
            }
        }
        return false;
    }
}
