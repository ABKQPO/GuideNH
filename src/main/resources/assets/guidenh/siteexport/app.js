import { installSearchUi } from "./search.js";
import { disposeHydratedScenes, hydrateVisibleScenes } from "./viewer.js";

function installMediaWikiSpecialFilters(root) {
  const pages = root.querySelectorAll("[data-guide-special-page]");
  for (const page of pages) {
    const input = page.querySelector("[data-guide-special-filter]");
    const showMoreButton = page.querySelector("[data-guide-special-show-more]");
    if (!(input instanceof HTMLInputElement)) {
      continue;
    }
    const entries = Array.from(page.querySelectorAll("[data-guide-special-entry]"));
    const groups = Array.from(page.querySelectorAll("[data-guide-special-group]"));
    const mode = page.getAttribute("data-guide-special-mode") || "flat";
    const defaultVisibleRaw = Number(page.getAttribute("data-guide-special-default-visible") || "0");
    const defaultVisible = Number.isFinite(defaultVisibleRaw) && defaultVisibleRaw > 0 ? defaultVisibleRaw : Number.MAX_SAFE_INTEGER;
    let visibleCount = defaultVisible;
    const apply = () => {
      const query = input.value.trim().toLowerCase();
      if (!query) {
        if (mode === "grouped") {
          for (const entry of entries) {
            entry.hidden = false;
          }
          for (let index = 0; index < groups.length; index++) {
            groups[index].hidden = index >= visibleCount;
          }
        } else {
          for (let index = 0; index < entries.length; index++) {
            entries[index].hidden = index >= visibleCount;
          }
        }
      } else {
        for (const entry of entries) {
          const searchBlob = (entry.getAttribute("data-guide-special-search") || entry.textContent || "").toLowerCase();
          entry.hidden = !searchBlob.includes(query);
        }
      }
      for (const group of groups) {
        const visibleChildren = group.querySelector("[data-guide-special-entry]:not([hidden])");
        group.hidden = !visibleChildren;
      }
      if (showMoreButton instanceof HTMLElement) {
        const hasMore = !query && visibleCount < (mode === "grouped" ? groups.length : entries.length);
        showMoreButton.hidden = !hasMore;
      }
    };
    input.addEventListener("input", apply);
    input.addEventListener("keydown", (event) => {
      if (event.key === "Escape") {
        input.value = "";
        visibleCount = defaultVisible;
        apply();
      }
    });
    if (showMoreButton instanceof HTMLElement) {
      showMoreButton.addEventListener("click", () => {
        const total = mode === "grouped" ? groups.length : entries.length;
        visibleCount = Math.min(total, visibleCount + 60);
        apply();
      });
    }
    apply();
  }
}

function cycleChildren(container) {
  const current = container.querySelector(".current");
  if (current) {
    current.classList.remove("current");
  }
  const next = current && current.nextElementSibling ? current.nextElementSibling : container.firstElementChild;
  if (next) {
    next.classList.add("current");
  }
}

function stopIngredientCycling(root) {
  if (root?.__guideIngredientCyclingTimer) {
    window.clearInterval(root.__guideIngredientCyclingTimer);
    delete root.__guideIngredientCyclingTimer;
  }
}

function stopGuideSounds(root) {
  const sounds = window.GuideNHSounds;
  if (root && sounds && typeof sounds.stopWithin === "function") {
    sounds.stopWithin(root);
    return;
  }
  if (!root && sounds && typeof sounds.stopAll === "function") {
    sounds.stopAll();
  }
}

function installIngredientCycling(root) {
  stopIngredientCycling(root);
  const cyclingBoxes = root.querySelectorAll("[data-ingredient-cycling]");
  if (!cyclingBoxes.length) {
    return;
  }

  cyclingBoxes.forEach((box) => {
    const first = box.firstElementChild;
    if (first && !box.querySelector(".current")) {
      first.classList.add("current");
    }
  });

  root.__guideIngredientCyclingTimer = window.setInterval(() => {
    cyclingBoxes.forEach((box) => {
      cycleChildren(box);
    });
  }, 1000);
}

function layoutImageAnnotations(root) {
  const annotations = root.querySelectorAll(".guide-image-annotation[data-source-x]");
  for (const annotation of annotations) {
    const wrapper = annotation.closest(".guide-floating-image-wrap");
    const image = wrapper?.querySelector("img.guide-floating-image");
    if (!(image instanceof HTMLImageElement) || !image.naturalWidth || !image.naturalHeight) {
      continue;
    }
    const x = Number(annotation.dataset.sourceX || 0);
    const y = Number(annotation.dataset.sourceY || 0);
    const width = Number(annotation.dataset.sourceWidth || 1);
    const height = Number(annotation.dataset.sourceHeight || 1);
    annotation.style.left = `${(x / image.naturalWidth) * 100}%`;
    annotation.style.top = `${(y / image.naturalHeight) * 100}%`;
    annotation.style.width = `${(width / image.naturalWidth) * 100}%`;
    annotation.style.height = `${(height / image.naturalHeight) * 100}%`;
  }
}

function installImageAnnotations(root) {
  const images = root.querySelectorAll(".guide-floating-image-wrap img.guide-floating-image");
  for (const image of images) {
    if (!(image instanceof HTMLImageElement)) {
      continue;
    }
    if (image.complete) {
      layoutImageAnnotations(root);
    } else {
      image.addEventListener("load", () => layoutImageAnnotations(root), { once: true });
    }
  }
  window.addEventListener("resize", () => layoutImageAnnotations(root), { passive: true });
}

function installGuideSounds(root) {
  const lastPlayedAt = new Map();
  const activeAudio = new Map();

  function stopAudio(audio) {
    try {
      audio.pause();
      audio.removeAttribute("src");
      audio.load();
    } catch (_) {}
  }

  function stopAll() {
    for (const audio of activeAudio.keys()) {
      stopAudio(audio);
    }
    activeAudio.clear();
  }

  function stopWithin(container) {
    for (const [audio, owner] of Array.from(activeAudio.entries())) {
      if (!(owner instanceof Node) || !owner.isConnected || owner === container || container.contains(owner)) {
        stopAudio(audio);
        activeAudio.delete(audio);
      }
    }
  }

  window.GuideNHSounds = {
    stopAll,
    stopWithin,
  };

  function keyFor(element, sound) {
    return `${sound || ""}:${element.dataset.guideSoundSrc || ""}`;
  }

  function effectiveVolume(element, event, spatialElement = element) {
    const baseVolume = Number(element.dataset.guideSoundVolume || 1);
    const radius = Number(element.dataset.guideSoundRadius || -1);
    if (!Number.isFinite(baseVolume) || baseVolume <= 0 || !event || !Number.isFinite(radius) || radius <= 0) {
      return Math.max(0, baseVolume || 0);
    }
    const rect = spatialElement.getBoundingClientRect();
    const position = {
      x: rect.left + rect.width / 2,
      y: rect.top + rect.height / 2,
    };
    const eventX = Number.isFinite(event.clientX) ? event.clientX : rect.left + rect.width / 2;
    const eventY = Number.isFinite(event.clientY) ? event.clientY : rect.top + rect.height / 2;
    const dx = eventX - position.x;
    const dy = eventY - position.y;
    const minVolume = Math.max(0, Math.min(1, Number(element.dataset.guideSoundMinVolume || 0.15)));
    const factor = Math.max(minVolume, Math.min(1, 1 - Math.sqrt(dx * dx + dy * dy) / radius));
    return Math.max(0, baseVolume * factor);
  }

  function playElementSound(element, event, spatialElement = element) {
    const src = element.dataset.guideSoundSrc;
    if (!src) {
      return false;
    }
    const cooldown = Math.max(0, Number(element.dataset.guideSoundCooldown || 250));
    const key = keyFor(element, element.dataset.guideSound);
    const now = Date.now();
    const last = lastPlayedAt.get(key) || 0;
    if (cooldown > 0 && now - last < cooldown) {
      return true;
    }
    const audio = new Audio(src);
    activeAudio.set(audio, spatialElement);
    audio.addEventListener("ended", () => activeAudio.delete(audio), { once: true });
    audio.addEventListener("error", () => activeAudio.delete(audio), { once: true });
    audio.volume = Math.max(0, Math.min(1, effectiveVolume(element, event, spatialElement)));
    audio.playbackRate = Math.max(0.01, Number(element.dataset.guideSoundPitch || 1) || 1);
    const played = audio.play();
    if (played?.catch) {
      played.catch(() => {
        stopAudio(audio);
        activeAudio.delete(audio);
      });
    }
    lastPlayedAt.set(key, now);
    return true;
  }

  function soundTrigger(element) {
    return (element.dataset.guideSoundTrigger || "click").toLowerCase();
  }

  root.addEventListener("click", (event) => {
    const element = event.target instanceof Element ? event.target.closest("[data-guide-sound]") : null;
    if (element instanceof HTMLElement && soundTrigger(element) === "click" && playElementSound(element, event)) {
      event.preventDefault();
    }
  });

  root.addEventListener("keydown", (event) => {
    if (event.key !== "Enter" && event.key !== " ") {
      return;
    }
    const element = event.target instanceof Element ? event.target.closest("[data-guide-sound]") : null;
    if (element instanceof HTMLElement && soundTrigger(element) === "click" && playElementSound(element, event)) {
      event.preventDefault();
    }
  });

  root.addEventListener("mouseover", (event) => {
    const element = event.target instanceof Element ? event.target.closest("[data-guide-sound]") : null;
    if (element instanceof HTMLElement && soundTrigger(element) === "hover" && !element.dataset.guideSoundHovered) {
      element.dataset.guideSoundHovered = "true";
      playElementSound(element, event);
    }
  });

  root.addEventListener("mouseout", (event) => {
    const element = event.target instanceof Element ? event.target.closest("[data-guide-sound]") : null;
    const related = event.relatedTarget instanceof Node ? event.relatedTarget : null;
    if (element instanceof HTMLElement && (!related || !element.contains(related))) {
      delete element.dataset.guideSoundHovered;
    }
  });

  installSceneSounds(root, playElementSound);
  window.addEventListener("pagehide", stopAll);
  document.addEventListener("visibilitychange", () => {
    if (document.hidden) {
      stopAll();
    }
  });
  root.addEventListener("click", (event) => {
    const link = event.target instanceof Element ? event.target.closest("a[href]") : null;
    if (link instanceof HTMLAnchorElement && !link.href.startsWith("javascript:")) {
      stopAll();
    }
  }, true);
}

function installSceneSounds(root, playElementSound) {
  const playedEnterSounds = new WeakMap();
  const buildElement = (sound) => {
    const element = document.createElement("span");
    element.dataset.guideSound = sound.sound || "";
    element.dataset.guideSoundSrc = sound.src || "";
    element.dataset.guideSoundTrigger = sound.trigger || "click";
    element.dataset.guideSoundVolume = String(sound.volume ?? 1);
    element.dataset.guideSoundPitch = String(sound.pitch ?? 1);
    element.dataset.guideSoundCooldown = String(sound.cooldown ?? 250);
    element.dataset.guideSoundRadius = String(sound.radius ?? -1);
    element.dataset.guideSoundMinVolume = String(sound.minVolume ?? 0.15);
    if (sound.x != null) {
      element.dataset.guideSoundX = String(sound.x);
    }
    if (sound.y != null) {
      element.dataset.guideSoundY = String(sound.y);
    }
    if (sound.z != null) {
      element.dataset.guideSoundZ = String(sound.z);
    }
    return element;
  };
  const parseSounds = (element) => {
    try {
      const parsed = JSON.parse(element.dataset.guideSceneSounds || "[]");
      return Array.isArray(parsed) ? parsed : [];
    } catch (_) {
      return [];
    }
  };
  const playMatching = (element, trigger, event) => {
    const elementSounds = parseSounds(element);
    for (const sound of elementSounds) {
      if ((sound.trigger || "click") !== trigger) {
        continue;
      }
      playElementSound(buildElement(sound), event, element);
    }
  };
  const playEnter = (element, event) => {
    const elementSounds = parseSounds(element);
    let played = playedEnterSounds.get(element);
    if (!played) {
      played = new Set();
      playedEnterSounds.set(element, played);
    }
    for (let i = 0; i < elementSounds.length; i++) {
      const sound = elementSounds[i];
      if ((sound.trigger || "click") === "enter" && !played.has(i)) {
        played.add(i);
        playElementSound(buildElement(sound), event, element);
      }
    }
  };
  root.addEventListener("click", (event) => {
    const element = event.target instanceof Element ? event.target.closest("[data-guide-scene-sounds]") : null;
    if (element instanceof HTMLElement) {
      playMatching(element, "click", event);
    }
  });
  root.addEventListener("mouseover", (event) => {
    const element = event.target instanceof Element ? event.target.closest("[data-guide-scene-sounds]") : null;
    if (!(element instanceof HTMLElement) || element.dataset.guideSceneSoundHovered) {
      return;
    }
    element.dataset.guideSceneSoundHovered = "true";
    playMatching(element, "hover", event);
    playEnter(element, event);
  });
  root.addEventListener("mouseout", (event) => {
    const element = event.target instanceof Element ? event.target.closest("[data-guide-scene-sounds]") : null;
    const related = event.relatedTarget instanceof Node ? event.relatedTarget : null;
    if (element instanceof HTMLElement && (!related || !element.contains(related))) {
      delete element.dataset.guideSceneSoundHovered;
    }
  });
}

function installTooltips(root) {
  const tooltipRoot = document.querySelector("[data-guide-tooltip-root]");
  if (!tooltipRoot) {
    return;
  }

  let activeState = null;
  let lastPointer = null;
  let restoreStack = [];

  function resolveTemplateHtml(templateId) {
    if (!templateId) {
      return "";
    }
    const template = document.getElementById(templateId);
    return template ? template.innerHTML : "";
  }

  function closestGuideTooltip(target) {
    return target instanceof Element ? target.closest("[data-template]") : null;
  }

  function isInsideTooltipRoot(target) {
    return target instanceof Node && tooltipRoot.contains(target);
  }

  function position(pointer) {
    const point = pointer || lastPointer;
    if (!point || tooltipRoot.hidden) {
      return;
    }
    const viewportWidth = window.innerWidth;
    const viewportHeight = window.innerHeight;
    const rect = tooltipRoot.getBoundingClientRect();
    const margin = 14;
    let left = point.clientX + 16;
    let top = point.clientY + 18;
    if (left + rect.width > viewportWidth - margin) {
      left = viewportWidth - rect.width - margin;
    }
    if (top + rect.height > viewportHeight - margin) {
      top = point.clientY - rect.height - 18;
    }
    if (left < margin) {
      left = margin;
    }
    if (top < margin) {
      top = margin;
    }
    tooltipRoot.style.left = `${left}px`;
    tooltipRoot.style.top = `${top}px`;
  }

  function hideAll() {
    activeState = null;
    restoreStack = [];
    stopGuideSounds(tooltipRoot);
    disposeHydratedScenes(tooltipRoot);
    tooltipRoot.hidden = true;
    tooltipRoot.innerHTML = "";
    stopIngredientCycling(tooltipRoot);
    delete tooltipRoot.dataset.externalTooltipOwner;
    delete tooltipRoot.dataset.externalTooltipTemplate;
  }

  function applyState(nextState, pointer, resetStack) {
    if (!nextState || !nextState.html) {
      hideAll();
      return;
    }
    if (resetStack) {
      restoreStack = [];
    }
    activeState = nextState;
    stopGuideSounds(tooltipRoot);
    disposeHydratedScenes(tooltipRoot);
    stopIngredientCycling(tooltipRoot);
    tooltipRoot.innerHTML = nextState.html;
    tooltipRoot.hidden = false;
    installIngredientCycling(tooltipRoot);
    hydrateVisibleScenes(tooltipRoot);
    if (nextState.sourceType === "external") {
      tooltipRoot.dataset.externalTooltipOwner = String(nextState.sourceRef ?? "");
      tooltipRoot.dataset.externalTooltipTemplate = nextState.templateId ?? "";
    } else {
      delete tooltipRoot.dataset.externalTooltipOwner;
      delete tooltipRoot.dataset.externalTooltipTemplate;
    }
    position(pointer);
    window.requestAnimationFrame(() => position(pointer));
  }

  function captureState() {
    if (!activeState) {
      return null;
    }
    return {
      sourceType: activeState.sourceType,
      sourceRef: activeState.sourceRef,
      templateId: activeState.templateId,
      html: activeState.html,
    };
  }

  function restorePrevious(pointer) {
    const previous = restoreStack.pop();
    if (!previous) {
      hideAll();
      return;
    }
    applyState(previous, pointer, false);
  }

  function showTemplate(templateId, sourceType, sourceRef, pointer, preserveCurrent) {
    const html = resolveTemplateHtml(templateId);
    if (!html) {
      if (preserveCurrent && restoreStack.length) {
        restorePrevious(pointer);
      } else {
        hideAll();
      }
      return;
    }

    if (preserveCurrent) {
      const snapshot = captureState();
      if (snapshot) {
        restoreStack.push(snapshot);
      }
    }

    applyState(
      {
        sourceType,
        sourceRef,
        templateId,
        html,
      },
      pointer,
      !preserveCurrent,
    );
  }

  function showTrigger(trigger, pointer) {
    if (!(trigger instanceof HTMLElement)) {
      return;
    }
    const templateId = trigger.dataset.template;
    const preserveCurrent = isInsideTooltipRoot(trigger) && activeState != null;
    showTemplate(templateId, "trigger", trigger, pointer, preserveCurrent);
  }

  function syntheticPointerFor(element) {
    const rect = element.getBoundingClientRect();
    return {
      clientX: rect.left + rect.width / 2,
      clientY: rect.bottom,
    };
  }

  root.addEventListener("mouseover", (event) => {
    const trigger = closestGuideTooltip(event.target);
    if (trigger) {
      showTrigger(trigger, event);
    }
  });

  root.addEventListener("mousemove", (event) => {
    lastPointer = event;
    if (!tooltipRoot.hidden) {
      position(event);
    }
  });

  root.addEventListener("mouseout", (event) => {
    const fromTrigger = closestGuideTooltip(event.target);
    const toTrigger = closestGuideTooltip(event.relatedTarget);

    if (fromTrigger && activeState?.sourceType === "trigger" && activeState.sourceRef === fromTrigger) {
      if (toTrigger && toTrigger !== fromTrigger) {
        return;
      }
      if (isInsideTooltipRoot(event.relatedTarget)) {
        return;
      }
      if (restoreStack.length && isInsideTooltipRoot(fromTrigger)) {
        restorePrevious(event);
        return;
      }
      hideAll();
      return;
    }

    if (isInsideTooltipRoot(event.target)) {
      if (isInsideTooltipRoot(event.relatedTarget)) {
        return;
      }
      if (toTrigger) {
        return;
      }
      if (activeState?.sourceType === "trigger"
        && activeState.sourceRef instanceof Element
        && activeState.sourceRef.contains(event.relatedTarget)) {
        return;
      }
      if (restoreStack.length) {
        restorePrevious(event);
        return;
      }
      if (activeState?.sourceType !== "external") {
        hideAll();
      }
    }
  });

  root.addEventListener("focusin", (event) => {
    const trigger = closestGuideTooltip(event.target);
    if (trigger) {
      showTrigger(trigger, syntheticPointerFor(trigger));
    }
  });

  root.addEventListener("focusout", (event) => {
    const fromTrigger = closestGuideTooltip(event.target);
    const toTrigger = closestGuideTooltip(event.relatedTarget);
    if (!fromTrigger || activeState?.sourceType !== "trigger" || activeState.sourceRef !== fromTrigger) {
      return;
    }
    if (toTrigger) {
      return;
    }
    if (restoreStack.length && isInsideTooltipRoot(fromTrigger)) {
      restorePrevious(syntheticPointerFor(fromTrigger));
      return;
    }
    hideAll();
  });

  window.GuideNHTooltips = {
    containsTooltip(target) {
      return isInsideTooltipRoot(target);
    },
    updatePointer(pointer) {
      lastPointer = pointer || lastPointer;
      if (!tooltipRoot.hidden) {
        position(pointer);
      }
    },
    showExternalTemplate(templateId, owner, pointer) {
      showTemplate(templateId, "external", owner, pointer || lastPointer, false);
    },
    hideExternal(owner) {
      if (!activeState || activeState.sourceType !== "external" || activeState.sourceRef !== owner) {
        return;
      }
      hideAll();
    },
  };

  window.addEventListener("scroll", hideAll, { passive: true });
  window.addEventListener("keydown", (event) => {
    if (event.key === "Escape") {
      hideAll();
    }
  });
}

document.addEventListener("DOMContentLoaded", () => {
  installSearchUi(document);
  installMediaWikiSpecialFilters(document);
  installTooltips(document);
  installIngredientCycling(document);
  installImageAnnotations(document);
  installGuideSounds(document);
  installMermaidLayout(document);
  installMermaidPanZoom(document);
  installChartHoverTooltips(document);
  hydrateVisibleScenes(document);
});

function installMermaidLayout(root) {
  const stages = root.querySelectorAll(".guide-mermaid-stage[data-guide-mermaid-stage]");
  if (!stages.length) {
    return;
  }
  const PADDING = 12;
  const GAP_X = 32;
  const GAP_Y = 18;
  let rafId = 0;
  const scheduleLayout = () => {
    if (rafId) {
      return;
    }
    rafId = window.requestAnimationFrame(() => {
      rafId = 0;
      stages.forEach((stage) => layoutMermaidStage(stage, PADDING, GAP_X, GAP_Y));
    });
  };
  if (!window.__guideMermaidLayoutResizeInstalled) {
    window.__guideMermaidLayoutResizeInstalled = true;
    window.addEventListener("resize", scheduleLayout, { passive: true });
  }
  stages.forEach((stage) => {
    if (stage.__guideMermaidLayoutInstalled) {
      return;
    }
    stage.__guideMermaidLayoutInstalled = true;
    if (window.ResizeObserver) {
      const observer = new window.ResizeObserver(() => scheduleLayout());
      observer.observe(stage);
      stage.querySelectorAll(".guide-mermaid-node").forEach((node) => observer.observe(node));
      stage.__guideMermaidResizeObserver = observer;
    }
    stage.querySelectorAll("img").forEach((image) => {
      if (image.complete) {
        return;
      }
      image.addEventListener("load", scheduleLayout, { once: true });
    });
  });
  scheduleLayout();
}

function layoutMermaidStage(stage, padding, gapX, gapY) {
  const svg = stage.querySelector("svg.guide-mermaid-canvas");
  const layer = stage.querySelector(".guide-mermaid-node-layer");
  if (!svg || !layer) {
    return;
  }
  const nodeElements = Array.from(layer.querySelectorAll(".guide-mermaid-node[data-node-id]"));
  if (!nodeElements.length) {
    svg.setAttribute("width", "100");
    svg.setAttribute("height", "40");
    svg.setAttribute("viewBox", "0 0 100 40");
    svg.innerHTML = "";
    return;
  }

  const nodes = new Map();
  nodeElements.forEach((el) => {
    nodes.set(el.dataset.nodeId || "", {
      id: el.dataset.nodeId || "",
      parentId: el.dataset.parentId || "",
      el,
      children: [],
      width: Math.max(64, Math.ceil(el.offsetWidth)),
      height: Math.max(32, Math.ceil(el.offsetHeight)),
      subtreeWidth: 0,
      subtreeHeight: 0,
      x: 0,
      y: 0,
    });
  });

  let rootNode = null;
  nodes.forEach((node) => {
    const parent = node.parentId ? nodes.get(node.parentId) : null;
    if (parent) {
      parent.children.push(node);
    } else if (!rootNode) {
      rootNode = node;
    }
  });
  if (!rootNode) {
    rootNode = nodes.values().next().value;
  }
  if (!rootNode) {
    return;
  }

  const measure = (node) => {
    if (!node.children.length) {
      node.subtreeWidth = node.width;
      node.subtreeHeight = node.height;
      return;
    }
    let childrenWidth = 0;
    let childrenHeight = 0;
    node.children.forEach((child) => {
      measure(child);
      childrenWidth += child.subtreeWidth;
      childrenHeight = Math.max(childrenHeight, child.subtreeHeight);
    });
    childrenWidth += gapX * (node.children.length - 1);
    node.subtreeWidth = Math.max(node.width, childrenWidth);
    node.subtreeHeight = node.height + gapY + childrenHeight;
  };

  const place = (node, x, y) => {
    node.x = x + (node.subtreeWidth - node.width) / 2;
    node.y = y;
    if (!node.children.length) {
      return;
    }
    let childrenWidth = 0;
    node.children.forEach((child) => {
      childrenWidth += child.subtreeWidth;
    });
    childrenWidth += gapX * (node.children.length - 1);
    let cursorX = x + (node.subtreeWidth - childrenWidth) / 2;
    const childY = y + node.height + gapY;
    node.children.forEach((child) => {
      place(child, cursorX, childY);
      cursorX += child.subtreeWidth + gapX;
    });
  };

  measure(rootNode);
  place(rootNode, 0, 0);

  const totalWidth = Math.ceil(rootNode.subtreeWidth + padding * 2);
  const totalHeight = Math.ceil(rootNode.subtreeHeight + padding * 2);
  stage.style.width = `${totalWidth}px`;
  stage.style.height = `${totalHeight}px`;
  svg.setAttribute("width", String(totalWidth));
  svg.setAttribute("height", String(totalHeight));
  svg.setAttribute("viewBox", `0 0 ${totalWidth} ${totalHeight}`);

  const paths = [];
  const drawConnectors = (node) => {
    const parentCx = padding + node.x + node.width / 2;
    const parentBottom = padding + node.y + node.height;
    node.children.forEach((child) => {
      const childCx = padding + child.x + child.width / 2;
      const childTop = padding + child.y;
      const midY = (parentBottom + childTop) / 2;
      paths.push(`M${parentCx} ${parentBottom} V${midY} H${childCx} V${childTop}`);
      drawConnectors(child);
    });
  };
  drawConnectors(rootNode);
  svg.innerHTML = `
    <rect x="0.5" y="0.5" width="${Math.max(1, totalWidth - 1)}" height="${Math.max(1, totalHeight - 1)}"
      fill="rgba(12,17,23,0.94)" stroke="rgba(67,76,87,0.4)" stroke-width="1"></rect>
    ${paths
      .map(
        (path) =>
          `<path d="${path}" stroke="rgba(93,108,124,1)" stroke-width="1" fill="none" shape-rendering="crispEdges"></path>`,
      )
      .join("")}
  `;

  nodes.forEach((node) => {
    node.el.style.transform = `translate(${padding + node.x}px, ${padding + node.y}px)`;
    node.el.style.setProperty("--guide-mermaid-accent", node.el.dataset.accent || "#7AA2F7");
  });
}

/**
 * Pan + zoom for mindmap canvases. Each `.guide-mermaid-pan` gains drag-to-pan
 * (pointerdown/move/up) plus wheel-to-zoom around the cursor. The transform is
 * applied to the inner stage via CSS `transform: translate(tx,ty) scale(s)`.
 */
function installMermaidPanZoom(root) {
  const containers = root.querySelectorAll(".guide-mermaid-pan[data-guide-pannable]");
  for (const container of containers) {
    const stage = container.querySelector(".guide-mermaid-stage") || container.querySelector("svg");
    if (!stage) continue;
    const state = { tx: 0, ty: 0, scale: 1, dragging: false, startX: 0, startY: 0, startTx: 0, startTy: 0 };
    const apply = () => {
      stage.style.transform = `translate(${state.tx}px, ${state.ty}px) scale(${state.scale})`;
    };
    apply();
    container.addEventListener("pointerdown", (event) => {
      if (event.button !== 0) return;
      if (event.target instanceof Element
        && event.target.closest("a, button, input, textarea, select, summary, [role='button'], [data-guide-sound]")) {
        return;
      }
      state.dragging = true;
      state.startX = event.clientX;
      state.startY = event.clientY;
      state.startTx = state.tx;
      state.startTy = state.ty;
      container.classList.add("is-grabbing");
      container.setPointerCapture?.(event.pointerId);
      event.preventDefault();
    });
    container.addEventListener("pointermove", (event) => {
      if (!state.dragging) return;
      state.tx = state.startTx + (event.clientX - state.startX);
      state.ty = state.startTy + (event.clientY - state.startY);
      apply();
    });
    const stopDrag = (event) => {
      if (!state.dragging) return;
      state.dragging = false;
      container.classList.remove("is-grabbing");
      try { container.releasePointerCapture?.(event.pointerId); } catch (_) {}
    };
    container.addEventListener("pointerup", stopDrag);
    container.addEventListener("pointercancel", stopDrag);
    container.addEventListener("wheel", (event) => {
      event.preventDefault();
      const rect = container.getBoundingClientRect();
      const cx = event.clientX - rect.left;
      const cy = event.clientY - rect.top;
      const factor = event.deltaY < 0 ? 1.15 : 1 / 1.15;
      const newScale = Math.max(0.2, Math.min(8, state.scale * factor));
      const ratio = newScale / state.scale;
      state.tx = cx - (cx - state.tx) * ratio;
      state.ty = cy - (cy - state.ty) * ratio;
      state.scale = newScale;
      apply();
    }, { passive: false });
    container.addEventListener("dblclick", () => {
      state.tx = 0;
      state.ty = 0;
      state.scale = 1;
      apply();
    });
  }
}

function installChartHoverTooltips(root) {
  const svgs = root.querySelectorAll("svg.guide-chart, svg.guide-function-graph");
  for (const svg of svgs) {
    const isFunctionGraph = svg.classList.contains("guide-function-graph");
    const owner = `chart-${Math.random().toString(36).slice(2, 9)}`;
    let popupEl = null;
    const ensurePopup = () => {
      if (popupEl) return popupEl;
      popupEl = document.createElement("div");
      popupEl.className = "guide-tooltip-popup guide-chart-tooltip-popup";
      popupEl.hidden = true;
      document.body.appendChild(popupEl);
      return popupEl;
    };
    const showText = (text, ev) => {
      const el = ensurePopup();
      el.textContent = text;
      el.hidden = false;
      positionPopup(el, ev);
    };
    const hide = () => {
      if (popupEl) popupEl.hidden = true;
    };
    const plotData = [];
    if (isFunctionGraph) {
      svg.querySelectorAll("polyline.guide-chart-shape").forEach((poly) => {
        const titleEl = poly.querySelector("title");
        const label = titleEl?.textContent ?? "";
        const raw = poly.getAttribute("points") || "";
        const pts = [];
        for (const tok of raw.trim().split(/\s+/)) {
          const [px, py] = tok.split(",");
          const fx = parseFloat(px), fy = parseFloat(py);
          if (Number.isFinite(fx) && Number.isFinite(fy)) pts.push([fx, fy]);
        }
        if (pts.length) plotData.push({ pts, label });
      });
    }
    svg.querySelectorAll(".guide-chart-shape").forEach((shape) => {
      const titleEl = shape.querySelector("title");
      const text = titleEl?.textContent ?? "";
      if (titleEl) titleEl.remove();
      shape.addEventListener("mouseenter", (ev) => showText(text, ev));
      shape.addEventListener("mousemove", (ev) => positionPopup(popupEl, ev));
      shape.addEventListener("mouseleave", hide);
    });
    if (isFunctionGraph) {
      const meta = svg.querySelector("metadata[data-plot-domain]");
      const dom = meta ? {
        xMin: parseFloat(meta.getAttribute("data-x-min")),
        xMax: parseFloat(meta.getAttribute("data-x-max")),
        yMin: parseFloat(meta.getAttribute("data-y-min")),
        yMax: parseFloat(meta.getAttribute("data-y-max")),
        left: parseFloat(meta.getAttribute("data-plot-left")),
        right: parseFloat(meta.getAttribute("data-plot-right")),
        top: parseFloat(meta.getAttribute("data-plot-top")),
        bottom: parseFloat(meta.getAttribute("data-plot-bottom")),
      } : null;
      svg.addEventListener("mousemove", (ev) => {
        if (!dom || !plotData.length) {
          const closest = findClosestShape(svg, ev);
          const text = closest?.querySelector("title")?.textContent;
          if (text) showText(text, ev); else hide();
          return;
        }
        const rect = svg.getBoundingClientRect();
        const sx = (ev.clientX - rect.left) * (svg.viewBox.baseVal.width || rect.width) / rect.width;
        const sy = (ev.clientY - rect.top) * (svg.viewBox.baseVal.height || rect.height) / rect.height;
        if (sx < dom.left || sx > dom.right || sy < dom.top || sy > dom.bottom) {
          hide();
          return;
        }
        const dataX = dom.xMin + (sx - dom.left) / (dom.right - dom.left) * (dom.xMax - dom.xMin);
        let best = null;
        let bestDist = Infinity;
        for (const plot of plotData) {
          const y = interpolateAtX(plot.pts, sx);
          if (y === null) continue;
          const d = Math.abs(y - sy);
          if (d < bestDist) { bestDist = d; best = { plot, svgY: y }; }
        }
        if (!best) { hide(); return; }
        const svgUPerCssPx = (svg.viewBox.baseVal.width || rect.width) / rect.width;
        const THRESHOLD_CSS_PX = 10;
        if (bestDist > THRESHOLD_CSS_PX * svgUPerCssPx) { hide(); return; }
        const dataY = dom.yMin + (dom.bottom - best.svgY) / (dom.bottom - dom.top) * (dom.yMax - dom.yMin);
        const expr = best.plot.label || "f(x)";
        showText(`${expr}\nx = ${dataX.toFixed(3)}\ny = ${dataY.toFixed(3)}`, ev);
      });
      svg.addEventListener("mouseleave", hide);
    }
  }
}

function interpolateAtX(pts, x) {
  for (let i = 1; i < pts.length; i++) {
    const a = pts[i - 1], b = pts[i];
    const lo = Math.min(a[0], b[0]);
    const hi = Math.max(a[0], b[0]);
    if (x >= lo && x <= hi && hi !== lo) {
      const t = (x - a[0]) / (b[0] - a[0]);
      return a[1] + (b[1] - a[1]) * t;
    }
  }
  return null;
}

function findClosestShape(svg, ev) {
  let best = null;
  let bestDist = Infinity;
  const rect = svg.getBoundingClientRect();
  const cx = ev.clientX - rect.left;
  const cy = ev.clientY - rect.top;
  for (const shape of svg.querySelectorAll(".guide-chart-shape")) {
    const r = shape.getBoundingClientRect();
    const sx = r.left - rect.left + r.width / 2;
    const sy = r.top - rect.top + r.height / 2;
    const d = (sx - cx) * (sx - cx) + (sy - cy) * (sy - cy);
    if (d < bestDist) { bestDist = d; best = shape; }
  }
  return best;
}

function positionPopup(el, ev) {
  if (!el) return;
  const x = ev.clientX + 14;
  const y = ev.clientY + 14;
  el.style.left = `${x}px`;
  el.style.top = `${y}px`;
}
