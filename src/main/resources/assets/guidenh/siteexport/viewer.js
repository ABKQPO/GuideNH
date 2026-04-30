let modelViewerModulePromise;
let loadedModelViewerModule;

async function getModelViewerModule() {
  if (loadedModelViewerModule) {
    return loadedModelViewerModule;
  }

  modelViewerModulePromise ||= import("./model-viewer/modelViewer.js").then((module) => {
    loadedModelViewerModule = module;
    return module;
  });
  return modelViewerModulePromise;
}

export function hydrateVisibleScenes(root) {
  const sceneNodes = root.querySelectorAll("[data-scene-src]");
  if (!sceneNodes.length) {
    return;
  }

  const observer = new IntersectionObserver(async (entries) => {
    const module = await getModelViewerModule();
    for (const entry of entries) {
      if (!entry.isIntersecting) {
        continue;
      }
      const node = entry.target;
      if (node.dataset.sceneHydrated === "true") {
        continue;
      }
      if (!node.isConnected) {
        observer.unobserve(node);
        continue;
      }
      node.dataset.sceneHydrated = "true";
      module.setupGameScene(node);
      observer.unobserve(node);
    }
  }, { rootMargin: "256px 0px" });

  for (const node of sceneNodes) {
    observer.observe(node);
  }
}

export function disposeHydratedScenes(root) {
  if (!root || !loadedModelViewerModule) {
    return;
  }
  loadedModelViewerModule.disposeHydratedScenes?.(root);
}
