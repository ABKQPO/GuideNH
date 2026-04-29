import { installSearchUi } from "./search.js";
import { hydrateVisibleScenes } from "./viewer.js";

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

function installIngredientCycling(root) {
  const cyclingBoxes = root.querySelectorAll(".ingredient-box.cycling");
  if (!cyclingBoxes.length) {
    return;
  }

  cyclingBoxes.forEach((box) => {
    const first = box.firstElementChild;
    if (first && !box.querySelector(".current")) {
      first.classList.add("current");
    }
  });

  window.setInterval(() => {
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
    return target instanceof Element ? target.closest(".guide-tooltip") : null;
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
    tooltipRoot.hidden = true;
    tooltipRoot.innerHTML = "";
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
    tooltipRoot.innerHTML = nextState.html;
    tooltipRoot.hidden = false;
    if (nextState.sourceType === "external") {
      tooltipRoot.dataset.externalTooltipOwner = String(nextState.sourceRef ?? "");
      tooltipRoot.dataset.externalTooltipTemplate = nextState.templateId ?? "";
    } else {
      delete tooltipRoot.dataset.externalTooltipOwner;
      delete tooltipRoot.dataset.externalTooltipTemplate;
    }
    position(pointer);
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
  hydrateVisibleScenes(document);
});
