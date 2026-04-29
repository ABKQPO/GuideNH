export function installSearchUi(root) {
  const input = root.querySelector("[data-guide-search-input]");
  const results = root.querySelector("[data-guide-search-results]");
  if (!input || !results) {
    return root;
  }

  const lang = (document.documentElement.lang || "en_us").toLowerCase().replace(/-/g, "_");
  const siteRoot = document.querySelector("base")?.href || document.baseURI;
  const searchIndexUrl = new URL(`_data/search/${lang}.json`, siteRoot);
  let entriesPromise = null;
  let debounceHandle = 0;

  const ensureEntries = () => {
    if (!entriesPromise) {
      entriesPromise = fetch(searchIndexUrl.toString()).then((response) => {
        if (!response.ok) {
          throw new Error(`Failed to load search index: ${response.status}`);
        }
        return response.json();
      }).then((payload) => {
        if (!Array.isArray(payload)) {
          return [];
        }
        payload.forEach((entry) => {
          const title = entry.title || "";
          const text = entry.text || "";
          entry._searchBlob = `${title}\n${text}`.toLowerCase();
        });
        return payload;
      }).catch(() => []);
    }
    return entriesPromise;
  };

  const clearResults = () => {
    results.innerHTML = "";
    results.hidden = true;
  };

  const emptyTemplate = results.dataset.guideSearchEmptyTemplate || "No matches for \"{{query}}\"";

  const snippet = (text, query) => {
    if (!text) {
      return "";
    }
    const lower = text.toLowerCase();
    const index = lower.indexOf(query);
    if (index < 0) {
      return text.slice(0, 140);
    }
    const start = Math.max(0, index - 40);
    const end = Math.min(text.length, index + 100);
    const prefix = start > 0 ? "..." : "";
    const suffix = end < text.length ? "..." : "";
    return `${prefix}${text.slice(start, end)}${suffix}`;
  };

  const renderResults = (matches, rawQuery, normalizedQuery) => {
    results.innerHTML = "";
    if (!matches.length) {
      const empty = document.createElement("div");
      empty.className = "guide-search-empty";
      empty.textContent = emptyTemplate.replace("{{query}}", rawQuery);
      results.appendChild(empty);
      results.hidden = false;
      return;
    }

    const fragment = document.createDocumentFragment();
    matches.slice(0, 20).forEach((entry) => {
      const link = document.createElement("a");
      link.className = "guide-search-result";
      link.href = new URL(entry.url, siteRoot).toString();

      const head = document.createElement("span");
      head.className = "guide-search-result-head";

      if (entry.iconUrl) {
        const icon = document.createElement("img");
        icon.className = "guide-search-result-icon";
        if (entry.iconKind === "texture") {
          icon.classList.add("guide-search-result-icon-texture");
        }
        icon.src = new URL(entry.iconUrl, siteRoot).toString();
        icon.alt = "";
        icon.width = 22;
        icon.height = 22;
        icon.decoding = "async";
        head.appendChild(icon);
      }

      const title = document.createElement("span");
      title.className = "guide-search-result-title";
      title.textContent = entry.title || entry.pageId || entry.url;
      head.appendChild(title);
      link.appendChild(head);

      const text = snippet(entry.text || "", normalizedQuery);
      if (text) {
        const detail = document.createElement("span");
        detail.className = "guide-search-result-snippet";
        detail.textContent = text;
        link.appendChild(detail);
      }

      fragment.appendChild(link);
    });
    results.appendChild(fragment);
    results.hidden = false;
  };

  const scoreEntry = (entry, query) => {
    const title = (entry.title || "").toLowerCase();
    if (title.startsWith(query)) {
      return 0;
    }
    if (title.indexOf(query) >= 0) {
      return 1;
    }
    const searchBlob = entry._searchBlob || "";
    const textIndex = searchBlob.indexOf(query);
    return textIndex >= 0 ? 2 + Math.min(textIndex, 10000) / 10000 : Number.POSITIVE_INFINITY;
  };

  input.addEventListener("focus", () => {
    ensureEntries();
  });

  input.addEventListener("input", () => {
    const rawQuery = input.value.trim();
    const query = rawQuery.toLowerCase();
    window.clearTimeout(debounceHandle);
    if (!query) {
      clearResults();
      return;
    }
    debounceHandle = window.setTimeout(async () => {
      const entries = await ensureEntries();
      const matches = entries.map((entry) => ({ entry, score: scoreEntry(entry, query) }))
        .filter((match) => Number.isFinite(match.score))
        .sort((left, right) => left.score - right.score)
        .map((match) => match.entry);
      renderResults(matches, rawQuery, query);
    }, 100);
  });

  input.addEventListener("keydown", (event) => {
    if (event.key === "Escape") {
      input.value = "";
      clearResults();
    }
  });

  document.addEventListener("click", (event) => {
    if (!results.hidden && !results.contains(event.target) && event.target !== input) {
      clearResults();
    }
  });

  return root;
}
