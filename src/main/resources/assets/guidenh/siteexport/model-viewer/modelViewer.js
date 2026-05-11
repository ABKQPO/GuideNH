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
    stateManifestSrc: node.dataset.sceneStateManifestSrc || "",
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
  setOrRemoveAttribute(node, "data-scene-hover-targets", variant?.hoverTargetsJson);
  return node;
}

function attachSceneContext(sceneContext) {
  const wrapper = sceneContext?.runtime?.wrapper;
  if (wrapper instanceof HTMLElement) {
    wrapper[SCENE_CONTEXT_KEY] = sceneContext;
    normalizeVendorSceneControls(wrapper);
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

function buildStateKey(state) {
  let key = `layer=${Math.max(0, Number(state.visibleLayer) || 0)}|ponder=${Math.max(0, Number(state.ponderTick) || 0)}|tier=${Math.max(1, Number(state.tier) || 1)}`;
  const channels = state.channels || {};
  for (const channelId of Object.keys(channels)) {
    key += `|channel:${channelId}=${Math.max(0, Number(channels[channelId]) || 0)}`;
  }
  return key;
}

function cloneState(state) {
  return {
    visibleLayer: Math.max(0, Number(state?.visibleLayer) || 0),
    ponderTick: Math.max(0, Number(state?.ponderTick) || 0),
    tier: Math.max(1, Number(state?.tier) || 1),
    channels: { ...(state?.channels || {}) },
  };
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

  const describeTick = (tick) => {
    const keyframe = Array.isArray(control.keyframes)
      ? control.keyframes.find((candidate) => Math.max(0, Number(candidate?.time) || 0) === tick)
      : null;
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
    for (const tick of uniqueTicks) {
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
  if (!sceneContext.runtime?.wrapper || !sceneContext.descriptor.interactive) {
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
}

async function updateSceneState(sceneContext, patch) {
  if (!sceneContext.manifest || sceneContext.transitioning) {
    return;
  }

  const nextState = cloneState({
    ...sceneContext.currentState,
    ...patch,
    channels: {
      ...(sceneContext.currentState?.channels || {}),
      ...(patch?.channels || {}),
    },
  });
  const key = buildStateKey(nextState);
  const variant = sceneContext.manifest.states[key];
  if (!variant) {
    console.warn("Missing exported scene variant for key %s", key);
    return;
  }

  sceneContext.transitioning = true;
  try {
    const parent = sceneContext.runtime?.wrapper?.parentNode;
    if (!parent) {
      return;
    }

    const replacement = createSceneNode(parent.ownerDocument, sceneContext.descriptor, variant);
    parent.insertBefore(replacement, sceneContext.runtime.wrapper);

    disposeSceneContext(sceneContext);
    sceneContext.currentState = nextState;
    sceneContext.runtime = await setupVendorGameScene(replacement);
    if (!sceneContext.runtime?.wrapper?.isConnected) {
      disposeSceneContext(sceneContext, false);
      return;
    }
    attachSceneContext(sceneContext);
    await mountSceneStateControls(sceneContext);
  } finally {
    sceneContext.transitioning = false;
  }
}

async function initializeScene(node) {
  if (!node?.dataset?.sceneSrc || !node.dataset.sceneAssetPrefix) {
    return null;
  }

  ensureBundledAssetCompat();
  ensureDetachedSceneSizeCompat();

  const descriptor = captureSceneDescriptor(node);
  const runtime = await setupVendorGameScene(node);
  if (!runtime) {
    return null;
  }

  const sceneContext = {
    descriptor,
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
