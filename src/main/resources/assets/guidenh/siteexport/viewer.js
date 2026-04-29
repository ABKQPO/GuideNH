let modelViewerModulePromise;

async function getModelViewerModule() {
  modelViewerModulePromise ||= import("./model-viewer/modelViewer.js");
  return modelViewerModulePromise;
}

export function hydrateVisibleScenes(root) {
  const sceneNodes = root.querySelectorAll("img.game-scene, [data-scene-src]");
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
      node.dataset.sceneHydrated = "true";
      module.setupGameScene(node);
      observer.unobserve(node);
    }
  }, { rootMargin: "256px 0px" });

  for (const node of sceneNodes) {
    observer.observe(node);
  }
}
