import { setupGameScene as setupVendorGameScene } from "./vendor/modelViewer-A42QTX7N.js";

function findDescriptor(target, property) {
  let current = target;
  while (current) {
    const descriptor = Object.getOwnPropertyDescriptor(current, property);
    if (descriptor) {
      return descriptor;
    }
    current = Object.getPrototypeOf(current);
  }
  return null;
}

function ensureBundledAssetCompat() {
  const descriptor = Object.getOwnPropertyDescriptor(String.prototype, "src");
  if (descriptor) {
    return;
  }
  Object.defineProperty(String.prototype, "src", {
    configurable: true,
    get() {
      const value = String(this);
      if (value.startsWith("./")) {
        return new URL(`vendor/${value.slice(2)}`, import.meta.url).toString();
      }
      return value;
    },
  });
}

function parseDetachedScenePixels(element, property) {
  if (!(element instanceof HTMLElement) || element.isConnected || !element.classList) {
    return null;
  }
  if (!element.classList.contains("root") && !element.classList.contains("viewport")) {
    return null;
  }
  const wrapper = element.closest(".game-scene-wrapper");
  if (!(wrapper instanceof HTMLElement)) {
    return null;
  }
  const variable = property === "width" ? "--modelviewer-width" : "--modelviewer-height";
  const value = wrapper.style.getPropertyValue(variable);
  if (!value) {
    return null;
  }
  const match = value.match(/([0-9]+(?:\.[0-9]+)?)px/);
  if (!match) {
    return null;
  }
  const pixels = Number.parseFloat(match[1]);
  return Number.isFinite(pixels) ? Math.max(0, Math.round(pixels)) : null;
}

function ensureDetachedSceneSizeCompat() {
  if (window.__guidenhDetachedSceneSizeCompatInstalled) {
    return;
  }
  window.__guidenhDetachedSceneSizeCompatInstalled = true;

  const properties = [
    ["offsetWidth", "width"],
    ["offsetHeight", "height"],
    ["clientWidth", "width"],
    ["clientHeight", "height"],
  ];

  for (const [propertyName, dimension] of properties) {
    const descriptor = findDescriptor(HTMLElement.prototype, propertyName);
    if (!descriptor?.get) {
      continue;
    }
    Object.defineProperty(HTMLElement.prototype, propertyName, {
      configurable: true,
      get() {
        const detachedPixels = parseDetachedScenePixels(this, dimension);
        if (detachedPixels != null) {
          return detachedPixels;
        }
        return descriptor.get.call(this);
      },
    });
  }
}

export function setupGameScene(node) {
  if (!node?.dataset?.sceneSrc || !node.dataset.sceneAssetPrefix) {
    return;
  }
  ensureBundledAssetCompat();
  ensureDetachedSceneSizeCompat();
  return setupVendorGameScene(node);
}
