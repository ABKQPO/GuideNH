import { setupGameScene as setupVendorGameScene } from "./vendor/modelViewer-A42QTX7N.js";

const sceneStateManifestCache = new Map();
const ROOT_PREFIX_TOKEN = "{{root}}/";
const SCENE_CONTEXT_KEY = Symbol("guidenhSceneContext");
const SITE_SCENE_CONTROL_SCALE = 3;
const SCENE_BUTTON_ICONS = {
  previousKeyframe: [0, 0],
  playPause: [0, 64],
  restart: [0, 32],
  zoomIn: [48, 16],
  zoomOut: [32, 16],
  resetView: [0, 32],
  toggleGrid: [16, 64],
  toggleBlockStats: [0, 0],
};
const PONDER_INPUT_LABELS = {
  lmb: "LMB",
  rmb: "RMB",
  scroll: "Scroll",
};

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

function captureSceneDescriptor(node) {
  const attributes = {};
  for (const attribute of node.attributes) {
    attributes[attribute.name] = attribute.value;
  }
  return {
    attributes,
    interactive: node.dataset.sceneInteractive === "true",
    stateControls: node.dataset.sceneStateControls === "true" || node.dataset.sceneInteractive === "true",
    stateManifestSrc: node.dataset.sceneStateManifestSrc || "",
    gridToggle: node.dataset.sceneGridToggle === "true",
    gridVisible: node.dataset.sceneGridVisible === "true",
    blockStatsToggle: node.dataset.sceneBlockStatsToggle === "true",
    blockStatsVisible: node.dataset.sceneBlockStatsVisible === "true",
  };
}

function isAbsoluteAssetUrl(value) {
  return /^[a-z][a-z0-9+\-.]*:/i.test(value) || value.startsWith("//");
}

function normalizeSceneAssetUrl(descriptor, rawUrl) {
  if (typeof rawUrl !== "string" || rawUrl.length === 0) {
    return "";
  }
  if (
    isAbsoluteAssetUrl(rawUrl) ||
    rawUrl.startsWith("./") ||
    rawUrl.startsWith("../") ||
    rawUrl.startsWith("/")
  ) {
    return rawUrl;
  }

  const assetPrefix = descriptor?.attributes?.["data-scene-asset-prefix"] || "";
  const rootRelativePath = rawUrl.startsWith(ROOT_PREFIX_TOKEN)
    ? rawUrl.slice(ROOT_PREFIX_TOKEN.length)
    : rawUrl.replace(/^\/+/, "");
  return `${assetPrefix}${rootRelativePath}`;
}

function createSceneNode(documentRef, descriptor, variant) {
  const node = documentRef.createElement("img");
  for (const [name, value] of Object.entries(descriptor.attributes)) {
    node.setAttribute(name, value);
  }

  node.removeAttribute("data-scene-hydrated");

  if (variant?.placeholderSrc) {
    node.setAttribute("src", normalizeSceneAssetUrl(descriptor, variant.placeholderSrc));
  }
  if (variant?.sceneSrc) {
    node.setAttribute("data-scene-src", normalizeSceneAssetUrl(descriptor, variant.sceneSrc));
  }
  setOrRemoveAttribute(node, "data-scene-in-world-annotations", variant?.inWorldAnnotationsJson);
  setOrRemoveAttribute(node, "data-scene-overlay-annotations", variant?.overlayAnnotationsJson);
  setOrRemoveAttribute(node, "data-guide-scene-sounds", variant?.sceneSoundsJson);
  setOrRemoveAttribute(node, "data-scene-hover-targets", variant?.hoverTargetsJson);
  applySceneGridDescriptor(node, descriptor);
  return node;
}

function getSceneDisplayScale(descriptor) {
  const rawScale = Number(descriptor?.attributes?.["data-scene-display-scale"]);
  return Number.isFinite(rawScale) && rawScale > 0 ? rawScale : 1;
}

function splitOverlayAnnotations(annotations) {
  const vendorAnnotations = [];
  const htmlAnnotations = [];
  for (const annotation of Array.isArray(annotations) ? annotations : []) {
    if (annotation?.type === "text" || annotation?.type === "input") {
      htmlAnnotations.push(annotation);
      continue;
    }
    vendorAnnotations.push(annotation);
  }
  return { vendorAnnotations, htmlAnnotations };
}

function attachSceneContext(sceneContext) {
  const wrapper = sceneContext?.runtime?.wrapper;
  if (wrapper instanceof HTMLElement) {
    wrapper[SCENE_CONTEXT_KEY] = sceneContext;
    const sceneSounds = sceneContext.descriptor?.attributes?.["data-guide-scene-sounds"];
    if (sceneSounds) {
      wrapper.dataset.guideSceneSounds = sceneSounds;
    }
    normalizeVendorSceneControls(wrapper);
    mountSceneActionControls(sceneContext);
    mountSceneOverlayAnnotations(sceneContext);
  }
}

function clearSceneContext(wrapper) {
  if (wrapper instanceof HTMLElement && Object.prototype.hasOwnProperty.call(wrapper, SCENE_CONTEXT_KEY)) {
    delete wrapper[SCENE_CONTEXT_KEY];
  }
}

function disposeSceneContext(sceneContext, removeWrapper = true) {
  const runtime = sceneContext?.runtime;
  if (!runtime) {
    return;
  }
  const wrapper = runtime.wrapper;
  clearSceneContext(wrapper);
  runtime.controller?.dispose?.();
  runtime.tooltipBridge?.hide?.();
  runtime.abortController?.abort?.();
  sceneContext.overlayRuntime?.dispose?.();
  sceneContext.overlayRuntime = null;
  if (removeWrapper && wrapper?.isConnected) {
    wrapper.remove();
  }
  sceneContext.runtime = null;
}

function setOrRemoveAttribute(node, name, value) {
  if (typeof value === "string" && value.length > 0) {
    node.setAttribute(name, value);
  } else {
    node.removeAttribute(name);
  }
}

function parseSceneJsonAttribute(value, fallback) {
  if (typeof value !== "string" || value.length === 0) {
    return fallback;
  }
  try {
    const parsed = JSON.parse(value);
    return Array.isArray(parsed) ? parsed : fallback;
  } catch (error) {
    console.warn("Failed to parse scene JSON attribute", error);
    return fallback;
  }
}

function serializeSceneJsonAttribute(value) {
  return JSON.stringify(Array.isArray(value) ? value : []);
}

function vectorFromArray(value) {
  return Array.isArray(value) && value.length >= 3
    ? [Number(value[0]) || 0, Number(value[1]) || 0, Number(value[2]) || 0]
    : [0, 0, 0];
}

function normalizeLinePoints(annotation) {
  if (Array.isArray(annotation?.points) && annotation.points.length >= 2) {
    return annotation.points.map(vectorFromArray);
  }
  return [vectorFromArray(annotation?.from), vectorFromArray(annotation?.to)];
}

function subVector(a, b) {
  return [a[0] - b[0], a[1] - b[1], a[2] - b[2]];
}

function addScaledVector(a, b, scale) {
  return [a[0] + b[0] * scale, a[1] + b[1] * scale, a[2] + b[2] * scale];
}

function crossVector(a, b) {
  return [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]];
}

function normalizeVector(value, fallback = [1, 0, 0]) {
  const len = Math.hypot(value[0], value[1], value[2]);
  return len > 1e-6 ? [value[0] / len, value[1] / len, value[2] / len] : fallback;
}

function ensureSceneOverlayHost(wrapper) {
  if (!(wrapper instanceof HTMLElement)) {
    return null;
  }
  let host = wrapper.querySelector(":scope > .scene-annotation-overlay");
  if (!(host instanceof HTMLElement)) {
    host = wrapper.ownerDocument.createElement("div");
    host.className = "scene-annotation-overlay";
    wrapper.append(host);
  }
  return host;
}

function createTextAnnotationNode(documentRef, annotation, displayScale) {
  const bubble = documentRef.createElement("div");
  const connectorSide = annotation?.independent ? "none" : annotation?.connectorSide || "bottom";
  bubble.className = "scene-text-annotation";
  bubble.dataset.connectorSide = connectorSide;
  bubble.innerHTML = typeof annotation?.text === "string" ? annotation.text : "";
  bubble.style.setProperty("--annotation-border-color", annotation?.color || "rgba(255,255,255,1)");
  const alpha = Math.max(0, Math.min(255, Number(annotation?.backgroundAlpha) || 0));
  bubble.style.setProperty("--annotation-background", `rgba(14, 14, 32, ${alpha / 255})`);
  const width = Math.max(0, Number(annotation?.maxWidth) || 0);
  if (width > 0) {
    bubble.style.maxWidth = `${width * displayScale}px`;
  }
  bubble.style.setProperty(
    "--annotation-connector-length",
    `${Math.max(0, Number(annotation?.connectorLength) || 0) * displayScale}px`,
  );
  bubble.style.setProperty(
    "--annotation-connector-offset",
    `${(Number(annotation?.connectorOffset) || 0) * displayScale}px`,
  );
  return bubble;
}

function createInputAnnotationNode(documentRef, annotation) {
  const wrapper = documentRef.createElement("div");
  wrapper.className = "scene-input-annotation";

  const modifier = `${annotation?.modifier || ""}`.trim();
  if (modifier) {
    const modifierNode = documentRef.createElement("div");
    modifierNode.className = "scene-input-annotation-modifier";
    modifierNode.textContent = modifier === "sneak" ? "Sneak +" : modifier === "ctrl" ? "Ctrl +" : `${modifier} +`;
    wrapper.append(modifierNode);
  }

  const body = documentRef.createElement("div");
  body.className = "scene-input-annotation-body";
  wrapper.append(body);

  const item = annotation?.item;
  if (item?.iconSrc) {
    const icon = documentRef.createElement("img");
    icon.className = "scene-input-annotation-item";
    icon.src = item.iconSrc;
    icon.alt = item.displayName || item.itemId || "";
    icon.decoding = "async";
    body.append(icon);
  } else if (item?.displayName || item?.itemId) {
    const fallback = documentRef.createElement("span");
    fallback.className = "scene-input-annotation-item scene-input-annotation-item--fallback";
    fallback.textContent = item.displayName || item.itemId;
    body.append(fallback);
  }

  const input = documentRef.createElement("span");
  input.className = "scene-input-annotation-key";
  input.textContent = PONDER_INPUT_LABELS[`${annotation?.inputType || ""}`.toLowerCase()] || "LMB";
  body.append(input);

  return wrapper;
}

function createOverlayAnnotationNode(documentRef, annotation, displayScale) {
  if (annotation?.type === "text") {
    return createTextAnnotationNode(documentRef, annotation, displayScale);
  }
  if (annotation?.type === "input") {
    return createInputAnnotationNode(documentRef, annotation);
  }
  return null;
}

function resolveOverlayAnchorPosition(sceneContext, annotation) {
  const projector = sceneContext?.runtime?.controller?.projectWorldPosition;
  const displayScale = getSceneDisplayScale(sceneContext?.descriptor);
  if (annotation?.independent) {
    const viewport = sceneContext?.runtime?.viewport;
    if (!(viewport instanceof HTMLElement)) {
      return null;
    }
    return {
      x: viewport.clientWidth * 0.5,
      y: viewport.clientHeight * 0.5 + (Number(annotation?.screenYOffset) || 0) * displayScale,
      visible: true,
    };
  }
  if (typeof projector !== "function") {
    return null;
  }
  const projected = projector(vectorFromArray(annotation?.position));
  return projected?.visible ? projected : null;
}

function updateOverlayAnnotationNode(node, annotation, anchor) {
  if (!(node instanceof HTMLElement) || !anchor?.visible) {
    if (node instanceof HTMLElement) {
      node.hidden = true;
    }
    return;
  }
  node.hidden = false;
  node.style.left = `${anchor.x}px`;
  node.style.top = `${anchor.y}px`;
}

function mountSceneOverlayAnnotations(sceneContext) {
  const wrapper = sceneContext?.runtime?.wrapper;
  const host = ensureSceneOverlayHost(wrapper);
  if (!(host instanceof HTMLElement)) {
    return;
  }
  host.textContent = "";
  const annotations = sceneContext?.htmlOverlayAnnotations;
  if (!Array.isArray(annotations) || annotations.length === 0) {
    sceneContext.overlayRuntime = {
      dispose() {
        host.textContent = "";
      },
    };
    return;
  }

  const documentRef = host.ownerDocument;
  const displayScale = getSceneDisplayScale(sceneContext?.descriptor);
  const entries = [];
  for (const annotation of annotations) {
    const node = createOverlayAnnotationNode(documentRef, annotation, displayScale);
    if (!(node instanceof HTMLElement)) {
      continue;
    }
    host.append(node);
    entries.push({ annotation, node });
  }

  let rafId = 0;
  let disposed = false;
  let resizeObserver = null;
  const needsAnimation = sceneContext?.descriptor?.interactive
    && entries.some((entry) => !entry.annotation?.independent);
  const update = () => {
    if (disposed) {
      return;
    }
    for (const entry of entries) {
      updateOverlayAnnotationNode(entry.node, entry.annotation, resolveOverlayAnchorPosition(sceneContext, entry.annotation));
    }
    if (needsAnimation) {
      rafId = window.requestAnimationFrame(update);
    }
  };
  update();

  if (typeof ResizeObserver !== "undefined") {
    resizeObserver = new ResizeObserver(() => {
      update();
    });
    resizeObserver.observe(host);
  }

  sceneContext.overlayRuntime = {
    dispose() {
      disposed = true;
      if (rafId) {
        window.cancelAnimationFrame(rafId);
      }
      resizeObserver?.disconnect?.();
      host.textContent = "";
    },
  };
}

function computeAnnotationLineHalfThickness(thickness) {
  return Math.max((Number(thickness) || 0) / 32, 1 / 256) * 0.5;
}

function computeAnnotationArrowBaseRadius(thickness) {
  return Math.max(computeAnnotationLineHalfThickness(thickness) * 1.35, 0.028);
}

function computeAnnotationArrowLength(thickness) {
  return Math.max(computeAnnotationArrowBaseRadius(thickness) * 3.5, 0.14);
}

function pointBoxAnnotation(point, color, size, alwaysOnTop) {
  const half = Math.max(Number(size) || 0, 1 / 256) * 0.5;
  return {
    type: "box",
    minCorner: [point[0] - half, point[1] - half, point[2] - half],
    maxCorner: [point[0] + half, point[1] + half, point[2] + half],
    color,
    thickness: half,
    alwaysOnTop,
  };
}

function arrowLineAnnotations(tip, interior, annotation) {
  const dir = normalizeVector(subVector(tip, interior));
  const up = Math.abs(dir[1]) < 0.9 ? [0, 1, 0] : [1, 0, 0];
  const n1 = normalizeVector(crossVector(dir, up), [0, 0, 1]);
  const n2 = normalizeVector(crossVector(dir, n1), [0, 1, 0]);
  const thickness = Number(annotation.thickness) || 1;
  const length = computeAnnotationArrowLength(thickness);
  const radius = computeAnnotationArrowBaseRadius(thickness);
  const base = addScaledVector(tip, dir, -length);
  const basePoints = [
    addScaledVector(base, n1, radius),
    addScaledVector(base, n2, radius),
    addScaledVector(base, n1, -radius),
    addScaledVector(base, n2, -radius),
  ];
  return basePoints.map((from) => ({
    ...annotation,
    type: "line",
    from,
    to: tip,
    points: undefined,
    arrow: undefined,
    showPoints: undefined,
    pointStyles: undefined,
    thickness: Math.max(thickness * 0.55, 0.02),
  }));
}

function expandLineAnnotation(annotation) {
  if (annotation?.type !== "line") {
    return [annotation];
  }
  const points = normalizeLinePoints(annotation);
  const expanded = [];
  for (let i = 0; i + 1 < points.length; i++) {
    expanded.push({
      ...annotation,
      from: points[i],
      to: points[i + 1],
      points: undefined,
      arrow: undefined,
      showPoints: undefined,
      pointStyles: undefined,
    });
  }
  if (annotation.arrow === "start") {
    expanded.push(...arrowLineAnnotations(points[0], points[1], annotation));
  } else if (annotation.arrow === "end") {
    expanded.push(...arrowLineAnnotations(points[points.length - 1], points[points.length - 2], annotation));
  }

  const styles = Array.isArray(annotation.pointStyles) ? annotation.pointStyles : [];
  for (let i = 0; i < points.length; i++) {
    let style = null;
    for (let styleIndex = styles.length - 1; styleIndex >= 0; styleIndex--) {
      if (Number(styles[styleIndex]?.index) === i) {
        style = styles[styleIndex];
        break;
      }
    }
    const show = style?.show ?? annotation.showPoints ?? false;
    if (!show) {
      continue;
    }
    expanded.push(
      pointBoxAnnotation(
        points[i],
        style?.color ?? annotation.pointColor ?? annotation.color,
        style?.size ?? annotation.pointSize ?? (Number(annotation.thickness) || 1) * 1.25,
        annotation.alwaysOnTop,
      ),
    );
  }
  return expanded;
}

function expandSceneAnnotations(annotations) {
  return annotations.flatMap(expandLineAnnotation);
}

function mergedGridAnnotations(descriptor, baseAnnotationsJson) {
  const baseAnnotations = expandSceneAnnotations(
    parseSceneJsonAttribute(baseAnnotationsJson, []).filter((annotation) => annotation?.siteControl !== "floorGrid"),
  );
  if (!descriptor.gridVisible) {
    return baseAnnotations;
  }
  const gridAnnotations = parseSceneJsonAttribute(descriptor.attributes["data-scene-grid-annotations"], []).map(
    (annotation) => ({
      ...annotation,
      siteControl: "floorGrid",
    }),
  );
  return [...baseAnnotations, ...gridAnnotations];
}

function applySceneGridDescriptor(node, descriptor) {
  if (!(node instanceof HTMLElement) || !descriptor.gridToggle) {
    return;
  }
  node.setAttribute("data-scene-grid-visible", descriptor.gridVisible ? "true" : "false");
  const annotations = mergedGridAnnotations(descriptor, node.getAttribute("data-scene-in-world-annotations"));
  node.setAttribute("data-scene-in-world-annotations", serializeSceneJsonAttribute(annotations));
}

function buildStateKey(state) {
  let key = `layer=${Math.max(0, Number(state.visibleLayer) || 0)}|ponder=${Math.max(0, Number(state.ponderTick) || 0)}`;
  const structures = normalizeStateStructures(state);
  if (Object.keys(structures).length === 0) {
    key += `|tier=${Math.max(1, Number(state.tier) || 1)}`;
    const channels = state.channels || {};
    for (const channelId of Object.keys(channels)) {
      key += `|channel:${channelId}=${Math.max(0, Number(channels[channelId]) || 0)}`;
    }
    return key;
  }
  for (const structureId of Object.keys(structures)) {
    const structureState = structures[structureId] || {};
    key += `|structure:${structureId}|tier=${Math.max(1, Number(structureState.tier) || 1)}`;
    const channels = structureState.channels || {};
    for (const channelId of Object.keys(channels)) {
      key += `|channel:${channelId}=${Math.max(0, Number(channels[channelId]) || 0)}`;
    }
  }
  return key;
}

function cloneState(state) {
  return {
    visibleLayer: Math.max(0, Number(state?.visibleLayer) || 0),
    ponderTick: Math.max(0, Number(state?.ponderTick) || 0),
    tier: Math.max(1, Number(state?.tier) || 1),
    channels: { ...(state?.channels || {}) },
    structures: normalizeStateStructures(state),
  };
}

function normalizeStateStructures(state) {
  const structures = state?.structures;
  if (!structures || typeof structures !== "object") {
    return {};
  }
  const normalized = {};
  for (const structureId of Object.keys(structures)) {
    const structureState = structures[structureId];
    normalized[structureId] = {
      tier: Math.max(1, Number(structureState?.tier) || 1),
      channels: { ...(structureState?.channels || {}) },
    };
  }
  return normalized;
}

function loadSceneStateManifest(src) {
  if (!src) {
    return Promise.resolve(null);
  }
  if (!sceneStateManifestCache.has(src)) {
    sceneStateManifestCache.set(
      src,
      fetch(src, { credentials: "same-origin" })
        .then((response) => {
          if (!response.ok) {
            throw new Error(`Failed to load scene manifest: ${response.status} ${response.statusText}`);
          }
          return response.json();
        })
        .catch((error) => {
          console.error(error);
          return null;
        }),
    );
  }
  return sceneStateManifestCache.get(src);
}

function ensureStateControlsHost(wrapper) {
  if (!(wrapper instanceof HTMLElement)) {
    return null;
  }
  let host = wrapper.querySelector(":scope > .scene-state-controls");
  if (!(host instanceof HTMLElement)) {
    host = wrapper.ownerDocument.createElement("div");
    host.className = "scene-state-controls";
    wrapper.append(host);
  }
  host.textContent = "";
  return host;
}

function applyIconButton(button, icon, labelText) {
  if (!(button instanceof HTMLElement) || !Array.isArray(icon)) {
    return button;
  }
  button.classList.add("scene-icon-button");
  button.style.setProperty("--scene-icon-x", `${-icon[0] * SITE_SCENE_CONTROL_SCALE}px`);
  button.style.setProperty("--scene-icon-y", `${-icon[1] * SITE_SCENE_CONTROL_SCALE}px`);
  if (labelText) {
    button.setAttribute("aria-label", labelText);
    button.title = labelText;
  }
  return button;
}

function normalizeVendorSceneControls(wrapper) {
  const controls = wrapper.querySelector(":scope > .controls");
  if (!(controls instanceof HTMLElement)) {
    return;
  }
  for (const button of controls.querySelectorAll("button")) {
    const text = button.textContent?.trim();
    if (text === "+") {
      applyIconButton(button, SCENE_BUTTON_ICONS.zoomIn, button.dataset.tooltipText || "Zoom in");
      button.textContent = "";
    } else if (text === "-") {
      applyIconButton(button, SCENE_BUTTON_ICONS.zoomOut, button.dataset.tooltipText || "Zoom out");
      button.textContent = "";
    } else if (text === "R") {
      applyIconButton(button, SCENE_BUTTON_ICONS.resetView, button.dataset.tooltipText || "Reset view");
      button.textContent = "";
    }
  }
}

function ensureSceneActionControlsHost(wrapper) {
  if (!(wrapper instanceof HTMLElement)) {
    return null;
  }
  let host = wrapper.querySelector(":scope > .controls");
  if (!(host instanceof HTMLElement)) {
    host = wrapper.ownerDocument.createElement("div");
    host.className = "controls";
    wrapper.append(host);
  }
  return host;
}

function createSceneActionButton(documentRef, icon, labelText, active, onClick) {
  const button = documentRef.createElement("button");
  button.type = "button";
  button.className = "minecraft-tooltip";
  applyIconButton(button, icon, labelText);
  button.setAttribute("aria-pressed", active ? "true" : "false");
  button.addEventListener("click", (event) => {
    event.preventDefault();
    onClick(button);
  });
  return button;
}

function mountSceneActionControls(sceneContext) {
  const wrapper = sceneContext?.runtime?.wrapper;
  if (!(wrapper instanceof HTMLElement)) {
    return;
  }
  const descriptor = sceneContext.descriptor;
  if (!descriptor?.gridToggle && !descriptor?.blockStatsToggle) {
    return;
  }

  const host = ensureSceneActionControlsHost(wrapper);
  if (!host || host.dataset.siteActionsMounted === "true") {
    return;
  }
  host.dataset.siteActionsMounted = "true";
  const documentRef = wrapper.ownerDocument;

  if (descriptor.gridToggle) {
    const gridButton = createSceneActionButton(
      documentRef,
      SCENE_BUTTON_ICONS.toggleGrid,
      "Toggle Floor Grid",
      descriptor.gridVisible,
      () => {
        toggleSceneGrid(sceneContext);
      },
    );
    gridButton.dataset.siteSceneAction = "grid";
    host.append(gridButton);
  }

  if (descriptor.blockStatsToggle) {
    const blockStatsButton = createSceneActionButton(
      documentRef,
      SCENE_BUTTON_ICONS.toggleBlockStats,
      "Toggle Block Stats",
      descriptor.blockStatsVisible,
      (button) => {
        descriptor.blockStatsVisible = !descriptor.blockStatsVisible;
        button.setAttribute("aria-pressed", descriptor.blockStatsVisible ? "true" : "false");
        syncBlockStatsVisibility(wrapper, descriptor.blockStatsVisible);
      },
    );
    blockStatsButton.dataset.siteSceneAction = "block-stats";
    host.append(blockStatsButton);
    syncBlockStatsVisibility(wrapper, descriptor.blockStatsVisible);
  }
}

async function toggleSceneGrid(sceneContext) {
  if (!sceneContext?.descriptor || sceneContext.transitioning) {
    return;
  }
  sceneContext.descriptor.gridVisible = !sceneContext.descriptor.gridVisible;
  const currentState = sceneContext.currentState ? cloneState(sceneContext.currentState) : null;
  const variant = currentState && sceneContext.manifest?.states ? sceneContext.manifest.states[buildStateKey(currentState)]
    : null;
  await recreateSceneRuntime(sceneContext, variant);
  if (currentState) {
    sceneContext.currentState = currentState;
  }
}

function syncBlockStatsVisibility(wrapper, visible) {
  const frame = wrapper?.closest?.(".guide-scene-export-frame");
  if (!(frame instanceof HTMLElement)) {
    return;
  }
  frame.classList.toggle("guide-scene-export-frame--block-stats-hidden", !visible);
}

function createSliderVisual(documentRef, range) {
  const visual = documentRef.createElement("span");
  visual.className = "scene-state-slider-visual";

  const track = documentRef.createElement("span");
  track.className = "scene-state-slider-track";
  visual.append(track);

  const fill = documentRef.createElement("span");
  fill.className = "scene-state-slider-fill";
  visual.append(fill);

  const thumb = documentRef.createElement("span");
  thumb.className = "scene-state-slider-thumb";
  visual.append(thumb);

  const syncFraction = () => {
    const min = Number(range.min) || 0;
    const max = Number(range.max) || min;
    const value = Number(range.value) || min;
    const fraction = max > min ? (value - min) / (max - min) : 0;
    visual.style.setProperty("--scene-state-fraction", String(Math.max(0, Math.min(1, fraction))));
  };
  range.addEventListener("input", syncFraction);
  range.addEventListener("change", syncFraction);
  syncFraction();
  return visual;
}

function createRangeControl(documentRef, labelText, min, max, currentValue, formatValue, onChange) {
  const wrapper = documentRef.createElement("label");
  wrapper.className = "scene-state-control";

  const header = documentRef.createElement("span");
  header.className = "scene-state-control-header";
  wrapper.append(header);

  const caption = documentRef.createElement("span");
  caption.className = "scene-state-control-label";
  caption.textContent = labelText;
  header.append(caption);

  const value = documentRef.createElement("span");
  value.className = "scene-state-control-value";
  header.append(value);

  const range = documentRef.createElement("input");
  range.className = "scene-state-range";
  range.type = "range";
  range.min = String(min);
  range.max = String(max);
  range.step = "1";

  const initialValue = Math.min(max, Math.max(min, Number(currentValue) || min));
  range.value = String(initialValue);

  const syncValue = () => {
    const numericValue = Number(range.value) || min;
    const displayValue = formatValue(numericValue);
    value.textContent = displayValue;
    range.setAttribute("aria-valuetext", displayValue);
  };

  range.addEventListener("input", syncValue);
  range.addEventListener("change", () => onChange(Number(range.value) || min));
  syncValue();
  const sliderWrap = documentRef.createElement("span");
  sliderWrap.className = "scene-state-slider-wrap";
  sliderWrap.append(createSliderVisual(documentRef, range));
  sliderWrap.append(range);
  wrapper.append(sliderWrap);
  return wrapper;
}

function createPonderControl(documentRef, control, currentTick, onChange) {
  const wrapper = documentRef.createElement("div");
  wrapper.className = "scene-state-control scene-ponder-control";

  const header = documentRef.createElement("div");
  header.className = "scene-state-control-header";
  wrapper.append(header);

  const caption = documentRef.createElement("span");
  caption.className = "scene-state-control-label";
  caption.textContent = control.label || "Ponder";
  header.append(caption);

  const value = documentRef.createElement("span");
  value.className = "scene-state-control-value";
  header.append(value);

  const buttons = documentRef.createElement("div");
  buttons.className = "scene-ponder-buttons";
  wrapper.append(buttons);

  const previousButton = documentRef.createElement("button");
  previousButton.type = "button";
  previousButton.className = "scene-ponder-button scene-icon-button";
  applyIconButton(previousButton, SCENE_BUTTON_ICONS.previousKeyframe, control.previousLabel || "Previous Keyframe");
  buttons.append(previousButton);

  const playButton = documentRef.createElement("button");
  playButton.type = "button";
  playButton.className = "scene-ponder-button scene-icon-button";
  applyIconButton(playButton, SCENE_BUTTON_ICONS.playPause, control.playPauseLabel || "Play / Pause");
  buttons.append(playButton);

  const restartButton = documentRef.createElement("button");
  restartButton.type = "button";
  restartButton.className = "scene-ponder-button scene-icon-button";
  applyIconButton(restartButton, SCENE_BUTTON_ICONS.restart, control.restartLabel || "Restart");
  buttons.append(restartButton);

  const range = documentRef.createElement("input");
  range.className = "scene-state-range";
  range.type = "range";
  range.min = "0";
  range.max = String(Math.max(0, Number(control.totalTime) || 0));
  range.step = "1";
  const sliderWrap = documentRef.createElement("span");
  sliderWrap.className = "scene-state-slider-wrap scene-ponder-slider-wrap";
  sliderWrap.append(createSliderVisual(documentRef, range));
  sliderWrap.append(range);
  wrapper.append(sliderWrap);

  const ticks = Array.isArray(control.ticks)
    ? control.ticks.map((tick) => Math.max(0, Number(tick) || 0))
    : Array.isArray(control.keyframes)
      ? control.keyframes.map((keyframe) => Math.max(0, Number(keyframe?.time) || 0))
      : [];
  const uniqueTicks = [...new Set([0, ...ticks, Math.max(0, Number(control.totalTime) || 0)])].sort((a, b) => a - b);
  const keyframeByTick = new Map();
  const visibleKeyframeTicks = [];
  if (Array.isArray(control.keyframes)) {
    for (const keyframe of control.keyframes) {
      const tick = Math.max(0, Number(keyframe?.time) || 0);
      if (!keyframeByTick.has(tick)) {
        keyframeByTick.set(tick, keyframe);
        visibleKeyframeTicks.push(tick);
      }
    }
    visibleKeyframeTicks.sort((a, b) => a - b);
  }

  const describeTick = (tick) => {
    const keyframe = keyframeByTick.get(tick) ?? null;
    const label = typeof keyframe?.label === "string" && keyframe.label.length > 0 ? keyframe.label : "";
    const tickLabel = `${control.timeLabel || "Tick"} ${tick}`;
    return label ? `${label} - ${tickLabel}` : tickLabel;
  };

  const nearestExportedTick = (tick) => {
    let nearest = uniqueTicks[0] ?? 0;
    let nearestDistance = Math.abs(nearest - tick);
    for (const candidate of uniqueTicks) {
      const distance = Math.abs(candidate - tick);
      if (distance < nearestDistance || (distance === nearestDistance && candidate < nearest)) {
        nearest = candidate;
        nearestDistance = distance;
      }
    }
    return nearest;
  };

  const setDisplayedTick = (tick) => {
    const normalized = Math.max(0, Math.min(Number(range.max) || 0, Number(tick) || 0));
    range.value = String(normalized);
    range.dispatchEvent(new Event("input"));
    const displayValue = describeTick(normalized);
    value.textContent = displayValue;
    range.setAttribute("aria-valuetext", displayValue);
  };

  previousButton.addEventListener("click", () => {
    const current = Number(range.value) || 0;
    let previous = 0;
    for (const tick of visibleKeyframeTicks) {
      if (tick < current) {
        previous = tick;
      } else {
        break;
      }
    }
    setDisplayedTick(previous);
    onChange(previous);
  });
  playButton.addEventListener("click", () => {
    const current = Number(range.value) || 0;
    let next = uniqueTicks[uniqueTicks.length - 1] ?? current;
    for (const tick of uniqueTicks) {
      if (tick > current) {
        next = tick;
        break;
      }
    }
    setDisplayedTick(next);
    onChange(next);
  });
  restartButton.addEventListener("click", () => {
    setDisplayedTick(0);
    onChange(0);
  });
  range.addEventListener("input", () => {
    const normalized = Math.max(0, Math.min(Number(range.max) || 0, Number(range.value) || 0));
    const displayValue = describeTick(normalized);
    value.textContent = displayValue;
    range.setAttribute("aria-valuetext", displayValue);
  });
  range.addEventListener("change", () => {
    const tick = nearestExportedTick(Number(range.value) || 0);
    setDisplayedTick(tick);
    onChange(tick);
  });

  setDisplayedTick(nearestExportedTick(currentTick));
  return wrapper;
}

async function mountSceneStateControls(sceneContext) {
  if (!sceneContext.runtime?.wrapper || !sceneContext.descriptor.stateControls) {
    return;
  }

  const manifest = await loadSceneStateManifest(sceneContext.descriptor.stateManifestSrc);
  if (!manifest?.states || !manifest?.controls) {
    return;
  }

  sceneContext.manifest = manifest;
  sceneContext.currentState = sceneContext.currentState || cloneState(manifest.initialState);

  const host = ensureStateControlsHost(sceneContext.runtime.wrapper);
  if (!host) {
    return;
  }

  const documentRef = sceneContext.runtime.wrapper.ownerDocument;
  const controls = manifest.controls;

  if (controls.ponder) {
    host.append(
      createPonderControl(documentRef, controls.ponder, sceneContext.currentState.ponderTick, (value) =>
        updateSceneState(sceneContext, { ponderTick: value }),
      ),
    );
  }

  if (controls.tier && Number.isFinite(Number(controls.tier.min)) && Number.isFinite(Number(controls.tier.max))) {
    const min = Math.max(1, Number(controls.tier.min) || 1);
    const max = Math.max(min, Number(controls.tier.max) || min);
    host.append(
      createRangeControl(
        documentRef,
        controls.tier.label || "Tier",
        min,
        max,
        sceneContext.currentState.tier,
        (value) => String(value),
        (value) => updateSceneState(sceneContext, { tier: value }),
      ),
    );
  }

  if (controls.visibleLayer && Number.isFinite(Number(controls.visibleLayer.max))) {
    const maxLayer = Math.max(0, Number(controls.visibleLayer.max) || 0);
    host.append(
      createRangeControl(
        documentRef,
        controls.visibleLayer.label || "Layer",
        0,
        maxLayer,
        sceneContext.currentState.visibleLayer,
        (value) => (value === 0 ? controls.visibleLayer.allLabel || "All" : String(value)),
        (value) => updateSceneState(sceneContext, { visibleLayer: value }),
      ),
    );
  }

  if (Array.isArray(controls.channels)) {
    for (const channel of controls.channels) {
      const min = Math.max(0, Number(channel?.min) || 0);
      const max = Math.max(0, Number(channel?.max) || 0);
      host.append(
        createRangeControl(
          documentRef,
          channel.label || channel.id || "Channel",
          min,
          max,
          sceneContext.currentState.channels?.[channel.id] ?? min,
          (value) => (value === 0 && min === 0 ? channel.unsetLabel || "Not set" : String(value)),
          (value) => updateSceneState(sceneContext, {
            channels: {
              ...sceneContext.currentState.channels,
              [channel.id]: value,
            },
          }),
        ),
      );
    }
  }

  if (Array.isArray(controls.structures)) {
    for (const structure of controls.structures) {
      const structureId = structure?.id;
      if (!structureId) {
        continue;
      }
      const structureLabel = structure.label || structureId;
      const currentStructureState = sceneContext.currentState.structures?.[structureId] || { tier: 1, channels: {} };
      if (structure.tier && Number.isFinite(Number(structure.tier.min)) && Number.isFinite(Number(structure.tier.max))) {
        const min = Math.max(1, Number(structure.tier.min) || 1);
        const max = Math.max(min, Number(structure.tier.max) || min);
        host.append(
          createRangeControl(
            documentRef,
            `${structureLabel} ${structure.tier.label || "Tier"}`,
            min,
            max,
            currentStructureState.tier,
            (value) => String(value),
            (value) =>
              updateSceneState(sceneContext, {
                structures: {
                  [structureId]: {
                    tier: value,
                  },
                },
              }),
          ),
        );
      }
      if (Array.isArray(structure.channels)) {
        for (const channel of structure.channels) {
          const min = Math.max(0, Number(channel?.min) || 0);
          const max = Math.max(0, Number(channel?.max) || 0);
          host.append(
            createRangeControl(
              documentRef,
              `${structureLabel} ${channel.label || channel.id || "Channel"}`,
              min,
              max,
              currentStructureState.channels?.[channel.id] ?? min,
              (value) => (value === 0 && min === 0 ? channel.unsetLabel || "Not set" : String(value)),
              (value) =>
                updateSceneState(sceneContext, {
                  structures: {
                    [structureId]: {
                      channels: {
                        [channel.id]: value,
                      },
                    },
                  },
                }),
            ),
          );
        }
      }
    }
  }
}

async function updateSceneState(sceneContext, patch) {
  if (!sceneContext.manifest || sceneContext.transitioning) {
    return;
  }

  const currentStructures = sceneContext.currentState?.structures || {};
  const patchedStructures = patch?.structures || {};
  const mergedStructures = { ...currentStructures };
  for (const structureId of Object.keys(patchedStructures)) {
    mergedStructures[structureId] = {
      ...(currentStructures[structureId] || {}),
      ...(patchedStructures[structureId] || {}),
      channels: {
        ...(currentStructures[structureId]?.channels || {}),
        ...(patchedStructures[structureId]?.channels || {}),
      },
    };
  }

  const nextState = cloneState({
    ...sceneContext.currentState,
    ...patch,
    channels: {
      ...(sceneContext.currentState?.channels || {}),
      ...(patch?.channels || {}),
    },
    structures: mergedStructures,
  });
  const key = buildStateKey(nextState);
  const variant = sceneContext.manifest.states[key];
  if (!variant) {
    console.warn("Missing exported scene variant for key %s", key);
    return;
  }

  sceneContext.transitioning = true;
  try {
    sceneContext.currentState = nextState;
    await recreateSceneRuntime(sceneContext, variant);
  } finally {
    sceneContext.transitioning = false;
  }
}

async function recreateSceneRuntime(sceneContext, variant) {
  const parent = sceneContext.runtime?.wrapper?.parentNode;
  if (!parent) {
    return;
  }

  const replacement = createSceneNode(parent.ownerDocument, sceneContext.descriptor, variant);
  sceneContext.descriptor = captureSceneDescriptor(replacement);
  const split = splitOverlayAnnotations(
    parseSceneJsonAttribute(replacement.getAttribute("data-scene-overlay-annotations"), []),
  );
  replacement.setAttribute("data-scene-overlay-annotations", serializeSceneJsonAttribute(split.vendorAnnotations));
  sceneContext.htmlOverlayAnnotations = split.htmlAnnotations;
  parent.insertBefore(replacement, sceneContext.runtime.wrapper);

  disposeSceneContext(sceneContext);
  sceneContext.runtime = await setupVendorGameScene(replacement);
  if (!sceneContext.runtime?.wrapper?.isConnected) {
    disposeSceneContext(sceneContext, false);
    return;
  }
  attachSceneContext(sceneContext);
  await mountSceneStateControls(sceneContext);
}

async function initializeScene(node) {
  if (!node?.dataset?.sceneSrc || !node.dataset.sceneAssetPrefix) {
    return null;
  }

  ensureBundledAssetCompat();
  ensureDetachedSceneSizeCompat();

  const descriptor = captureSceneDescriptor(node);
  applySceneGridDescriptor(node, descriptor);
  const split = splitOverlayAnnotations(parseSceneJsonAttribute(node.getAttribute("data-scene-overlay-annotations"), []));
  node.setAttribute("data-scene-overlay-annotations", serializeSceneJsonAttribute(split.vendorAnnotations));
  const runtime = await setupVendorGameScene(node);
  if (!runtime) {
    return null;
  }

  const sceneContext = {
    descriptor,
    htmlOverlayAnnotations: split.htmlAnnotations,
    overlayRuntime: null,
    runtime,
    manifest: null,
    currentState: null,
    transitioning: false,
  };

  if (!sceneContext.runtime?.wrapper?.isConnected) {
    disposeSceneContext(sceneContext, false);
    return null;
  }

  attachSceneContext(sceneContext);
  await mountSceneStateControls(sceneContext);
  return sceneContext;
}

export function setupGameScene(node) {
  return initializeScene(node);
}

export function disposeHydratedScenes(root) {
  if (!root?.querySelectorAll) {
    return;
  }

  const wrappers = [];
  if (root instanceof HTMLElement && root.classList.contains("game-scene-wrapper")) {
    wrappers.push(root);
  }
  wrappers.push(...root.querySelectorAll(".game-scene-wrapper"));

  for (const wrapper of wrappers) {
    const sceneContext = wrapper?.[SCENE_CONTEXT_KEY];
    if (sceneContext) {
      disposeSceneContext(sceneContext);
    }
  }
}
