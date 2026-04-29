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

  let activeTrigger = null;

  const hide = () => {
    activeTrigger = null;
    tooltipRoot.hidden = true;
    tooltipRoot.innerHTML = "";
  };

  const position = (event) => {
    if (tooltipRoot.hidden) {
      return;
    }
    const viewportWidth = window.innerWidth;
    const viewportHeight = window.innerHeight;
    const rect = tooltipRoot.getBoundingClientRect();
    const margin = 14;
    let left = event.clientX + 16;
    let top = event.clientY + 18;
    if (left + rect.width > viewportWidth - margin) {
      left = viewportWidth - rect.width - margin;
    }
    if (top + rect.height > viewportHeight - margin) {
      top = event.clientY - rect.height - 18;
    }
    if (left < margin) {
      left = margin;
    }
    if (top < margin) {
      top = margin;
    }
    tooltipRoot.style.left = `${left}px`;
    tooltipRoot.style.top = `${top}px`;
  };

  const resolveTooltipHtml = (trigger) => {
    const templateId = trigger.dataset.template;
    if (!templateId) {
      return "";
    }
    const template = document.getElementById(templateId);
    if (!template) {
      return "";
    }
    return template.innerHTML;
  };

  const show = (trigger, event) => {
    const html = resolveTooltipHtml(trigger);
    if (!html) {
      hide();
      return;
    }
    activeTrigger = trigger;
    tooltipRoot.innerHTML = html;
    tooltipRoot.hidden = false;
    position(event);
  };

  root.querySelectorAll(".guide-tooltip").forEach((trigger) => {
    trigger.addEventListener("mouseenter", (event) => {
      show(trigger, event);
    });
    trigger.addEventListener("mousemove", (event) => {
      if (activeTrigger === trigger) {
        position(event);
      }
    });
    trigger.addEventListener("mouseleave", hide);
    trigger.addEventListener("focus", () => {
      const rect = trigger.getBoundingClientRect();
      show(trigger, {
        clientX: rect.left + rect.width / 2,
        clientY: rect.bottom,
      });
    });
    trigger.addEventListener("blur", hide);
  });

  window.addEventListener("scroll", hide, { passive: true });
  window.addEventListener("keydown", (event) => {
    if (event.key === "Escape") {
      hide();
    }
  });
}

document.addEventListener("DOMContentLoaded", () => {
  installSearchUi(document);
  installTooltips(document);
  installIngredientCycling(document);
  hydrateVisibleScenes(document);
});
