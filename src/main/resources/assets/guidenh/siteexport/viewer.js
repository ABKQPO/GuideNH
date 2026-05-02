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

// Maximum number of game scenes that may be live (hydrated) at the same time.
// Each scene allocates its own WebGL context; browsers cap that at ~16 globally,
// and stacking too many causes severe lag and the early scenes to crash.
const MAX_ACTIVE_SCENES = 2;
// Tracks the order in which scenes were activated so we can evict the oldest
// when the live set exceeds the cap.
const activeScenes = [];
// Nodes whose viewport status entry is "intersecting" but that have been
// throttled because the live set is full. They will be promoted as slots free.
const pendingScenes = new Set();

function markActive(node) {
  const idx = activeScenes.indexOf(node);
  if (idx >= 0) {
    activeScenes.splice(idx, 1);
  }
  activeScenes.push(node);
}

async function disposeNode(node) {
  const idx = activeScenes.indexOf(node);
  if (idx >= 0) {
    activeScenes.splice(idx, 1);
  }
  if (loadedModelViewerModule?.disposeHydratedScenes) {
    loadedModelViewerModule.disposeHydratedScenes(node);
  }
}

async function hydrateNode(node, module) {
  if (node.dataset.sceneHydrated === "true") {
    return;
  }
  // Evict the oldest live scene while the cap is reached so newcomers can run.
  while (activeScenes.length >= MAX_ACTIVE_SCENES) {
    const oldest = activeScenes.shift();
    if (oldest && oldest !== node) {
      await disposeNode(oldest);
    }
  }
  node.dataset.sceneHydrated = "true";
  markActive(node);
  module.setupGameScene(node);
}

export function hydrateVisibleScenes(root) {
  const sceneNodes = root.querySelectorAll("[data-scene-src]");
  if (!sceneNodes.length) {
    return;
  }

  const observer = new IntersectionObserver(async (entries) => {
    const module = await getModelViewerModule();
    for (const entry of entries) {
      const node = entry.target;
      if (!node.isConnected) {
        observer.unobserve(node);
        pendingScenes.delete(node);
        await disposeNode(node);
        continue;
      }
      if (entry.isIntersecting) {
        if (node.dataset.sceneHydrated === "true") {
          markActive(node);
          continue;
        }
        if (activeScenes.length >= MAX_ACTIVE_SCENES) {
          // Defer hydration until a scene leaves the viewport. The observer keeps
          // watching so the next scroll update triggers another evaluation.
          pendingScenes.add(node);
          continue;
        }
        pendingScenes.delete(node);
        await hydrateNode(node, module);
      } else {
        // Scene scrolled out of view: dispose it so its WebGL context is freed
        // (this is what previously made repeated scrolls crash early scenes).
        pendingScenes.delete(node);
        if (node.dataset.sceneHydrated === "true") {
          await disposeNode(node);
        }
        // Promote one queued scene now that we may have a free slot.
        if (pendingScenes.size && activeScenes.length < MAX_ACTIVE_SCENES) {
          const next = pendingScenes.values()
            .next().value;
          pendingScenes.delete(next);
          await hydrateNode(next, module);
        }
      }
    }
  }, { rootMargin: "128px 0px" });

  for (const node of sceneNodes) {
    observer.observe(node);
  }
}

export function disposeHydratedScenes(root) {
  if (!root || !loadedModelViewerModule) {
    return;
  }
  // Drop any nodes inside `root` from our bookkeeping so they don't leak across navigations.
  if (root.querySelectorAll) {
    for (const node of root.querySelectorAll("[data-scene-src]")) {
      const idx = activeScenes.indexOf(node);
      if (idx >= 0) {
        activeScenes.splice(idx, 1);
      }
      pendingScenes.delete(node);
    }
  }
  loadedModelViewerModule.disposeHydratedScenes?.(root);
}
