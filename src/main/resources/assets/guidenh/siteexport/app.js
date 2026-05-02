import { installSearchUi } from "./search.js";
import { disposeHydratedScenes, hydrateVisibleScenes } from "./viewer.js";

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
  installTooltips(document);
  installIngredientCycling(document);
  installMermaidPanZoom(document);
  installChartHoverTooltips(document);
  hydrateVisibleScenes(document);
});

/**
 * Pan + zoom for mindmap canvases. Each `.guide-mermaid-pan` gains drag-to-pan
 * (pointerdown/move/up) plus wheel-to-zoom around the cursor. The transform is
 * applied to the inner SVG via CSS `transform: translate(tx,ty) scale(s)`.
 */
function installMermaidPanZoom(root) {
  const containers = root.querySelectorAll(".guide-mermaid-pan[data-guide-pannable]");
  for (const container of containers) {
    const svg = container.querySelector("svg");
    if (!svg) continue;
    const state = { tx: 0, ty: 0, scale: 1, dragging: false, startX: 0, startY: 0, startTx: 0, startTy: 0 };
    const apply = () => {
      svg.style.transform = `translate(${state.tx}px, ${state.ty}px) scale(${state.scale})`;
    };
    apply();
    container.addEventListener("pointerdown", (event) => {
      if (event.button !== 0) return;
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
      // Zoom relative to cursor: keep the world-point under the cursor stationary.
      const factor = event.deltaY < 0 ? 1.15 : 1 / 1.15;
      const newScale = Math.max(0.2, Math.min(8, state.scale * factor));
      const ratio = newScale / state.scale;
      state.tx = cx - (cx - state.tx) * ratio;
      state.ty = cy - (cy - state.ty) * ratio;
      state.scale = newScale;
      apply();
    }, { passive: false });
    // Double-click resets the view.
    container.addEventListener("dblclick", () => {
      state.tx = 0;
      state.ty = 0;
      state.scale = 1;
      apply();
    });
  }
}

/**
 * Replace the SVG's native `<title>` tooltips with the GuideNH tooltip overlay so
 * chart shapes show their label in the same MC-styled popup as item tooltips,
 * follow the cursor, and (for function graphs) report the live (x, y) under the
 * pointer instead of just the static expression.
 */
function installChartHoverTooltips(root) {
  const svgs = root.querySelectorAll("svg.guide-chart, svg.guide-function-graph");
  for (const svg of svgs) {
    const isFunctionGraph = svg.classList.contains("guide-function-graph");
    // Read plot domain from the SVG viewBox attributes when available so we can
    // map cursor pixels back to data x/y for function graphs.
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
    // Per-shape hover: use the existing <title> child as the body.
    svg.querySelectorAll(".guide-chart-shape").forEach((shape) => {
      const titleEl = shape.querySelector("title");
      const text = titleEl?.textContent ?? "";
      if (titleEl) titleEl.remove();  // suppress native browser tooltip
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
      // Cache parsed polyline points + their associated label (the original <title>) per plot.
      const plotData = [];
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
      svg.addEventListener("mousemove", (ev) => {
        if (!dom || !plotData.length) {
          const closest = findClosestShape(svg, ev);
          const text = closest?.querySelector("title")?.textContent;
          if (text) showText(text, ev); else hide();
          return;
        }
        // Convert mouse → SVG user-space coordinates (we authored the SVG with viewBox = 0..w/0..h
        // so we can use the rect ratio directly).
        const rect = svg.getBoundingClientRect();
        const sx = (ev.clientX - rect.left) * (svg.viewBox.baseVal.width || rect.width) / rect.width;
        const sy = (ev.clientY - rect.top) * (svg.viewBox.baseVal.height || rect.height) / rect.height;
        if (sx < dom.left || sx > dom.right || sy < dom.top || sy > dom.bottom) {
          hide();
          return;
        }
        const dataX = dom.xMin + (sx - dom.left) / (dom.right - dom.left) * (dom.xMax - dom.xMin);
        // For each plot, interpolate the Y at this svg X and pick the curve nearest to the cursor.
        let best = null;
        let bestDist = Infinity;
        for (const plot of plotData) {
          const y = interpolateAtX(plot.pts, sx);
          if (y === null) continue;
          const d = Math.abs(y - sy);
          if (d < bestDist) { bestDist = d; best = { plot, svgY: y }; }
        }
        if (!best) { hide(); return; }
        // Only show tooltip when the cursor is within a visual threshold of the nearest curve.
        // We compare in SVG user-space then convert to CSS pixels so the sensitivity stays
        // constant regardless of how the SVG is scaled by max-width / zoom.
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
  // pts is an array of [px, py] in ascending px (mostly); do a linear scan.
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
  // Offset the popup slightly off the cursor so it doesn't flicker on hover.
  const x = ev.clientX + 14;
  const y = ev.clientY + 14;
  el.style.left = `${x}px`;
  el.style.top = `${y}px`;
}
