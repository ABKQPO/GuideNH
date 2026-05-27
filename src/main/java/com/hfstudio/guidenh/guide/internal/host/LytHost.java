package com.hfstudio.guidenh.guide.internal.host;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytDocument;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;

public class LytHost {

    @Nullable private LytDocument document;
    private final Map<String, LytScript> scripts = new HashMap<>();
    private final Map<String, PageCacheEntry> cachedDocuments = new LinkedHashMap<>();
    private final Map<String, AtomicInteger> pageNodeCounters = new HashMap<>();
    String currentPageId;

    static class PageCacheEntry {
        final LytDocument document;
        final Map<String, LytBlock> nodeResults = new HashMap<>();
        PageCacheEntry(LytDocument document) { this.document = document; }
    }

    private final ViewportState viewport = new ViewportState();
    private final NavigationState nav = new NavigationState();
    private final Deque<LytEvent> eventQueue = new ArrayDeque<>();
    private final Deque<DeferredTask> taskQueue = new ArrayDeque<>();

    // ===== Document =====

    public void setDocument(@Nullable LytDocument newDoc) {
        if (this.document != null && this.document != newDoc) {
            this.document.setLive(false);    // onDetach cascade on old doc
        }
        this.document = newDoc;
        if (newDoc != null) {
            allocateNodeUids(newDoc);
            newDoc.setLive(true);            // onAttach cascade — this triggers everything
            dispatchMountEvents(newDoc);     // MOUNT events for styleClass nodes
            viewport.updateContent(newDoc.getAvailableWidth(), newDoc.getContentHeight());
        }
    }

    @Nullable public LytDocument getDocument() { return document; }
    public ViewportState getViewport() { return viewport; }
    public NavigationState getNavigation() { return nav; }

    public void registerScript(String styleClass, LytScript script) {
        scripts.put(styleClass, script);
    }

    @Nullable
    public PageCacheEntry getCachedPage(String pageId) {
        return cachedDocuments.get(pageId);
    }

    public void cachePage(String pageId, LytDocument compiledDoc) {
        cachedDocuments.put(pageId, new PageCacheEntry(compiledDoc));
    }

    public void invalidatePage(String pageId) {
        cachedDocuments.remove(pageId);
        pageNodeCounters.remove(pageId);
    }

    public void setCurrentPageId(String pageId) {
        this.currentPageId = pageId;
    }

    public boolean hasPreheatWork() {
        return false; // placeholder, real impl later
    }

    public void preheatStep(long deadlineNs) {
        // placeholder, real impl later
    }

    String allocateNodeUid(String pageId, String prefix) {
        int seq = pageNodeCounters
            .computeIfAbsent(pageId, k -> new AtomicInteger()).incrementAndGet();
        return pageId + "::" + prefix + ":" + seq;
    }

    private void allocateNodeUids(LytNode node) {
        if (node.getStyleClass() != null && node.getNodeUid() == null) {
            String prefix = node.getStyleClass().toLowerCase();
            int seq = pageNodeCounters
                .computeIfAbsent(currentPageId, k -> new AtomicInteger()).incrementAndGet();
            node.setNodeUid(currentPageId + "::" + prefix + ":" + seq);
        }
        for (var child : node.getChildren()) {
            allocateNodeUids(child);
        }
        // Also traverse into flow content (LytParagraph, LytFlowSpan children)
        allocateFlowNodeUids(node);
    }

    private void allocateFlowNodeUids(LytNode node) {
        if (node instanceof LytParagraph para) {
            for (var fcChild : para.getContent()) {
                allocateFlowNodeUidsRecursive(fcChild);
            }
        }
    }

    private void allocateFlowNodeUidsRecursive(LytFlowContent fc) {
        if (fc.getStyleClass() != null && fc.getNodeUid() == null) {
            String prefix = fc.getStyleClass().toLowerCase();
            int seq = pageNodeCounters
                .computeIfAbsent(currentPageId, k -> new AtomicInteger()).incrementAndGet();
            fc.setNodeUid(currentPageId + "::" + prefix + ":" + seq);
        }
        if (fc instanceof LytFlowSpan span) {
            for (var child : span.getChildren()) {
                allocateFlowNodeUidsRecursive(child);
            }
        }
    }

    private void dispatchMountEvents(LytNode node) {
        String cls = node.getStyleClass();
        if (cls != null) {
            LytScript script = scripts.get(cls);
            if (script != null) {
                try {
                    ScriptContextImpl ctx = new ScriptContextImpl(node, this, document);
                    script.onEvent(node, new LytEvent(EventType.MOUNT, node), ctx);
                } catch (Exception e) {
                    // Error boundary: log and continue
                    e.printStackTrace();
                }
            }
        }
        for (var child : node.getChildren()) {
            dispatchMountEvents(child);
        }
        // Also traverse into flow content for inline-level styleClass nodes
        dispatchMountEventsFlow(node);
    }

    private void dispatchMountEventsFlow(LytNode node) {
        if (node instanceof LytParagraph para) {
            for (var fcChild : para.getContent()) {
                dispatchMountEventsFlowRecursive(fcChild);
            }
        }
    }

    private void dispatchMountEventsFlowRecursive(LytFlowContent fc) {
        String cls = fc.getStyleClass();
        if (cls != null) {
            LytScript script = scripts.get(cls);
            if (script != null) {
                try {
                    ScriptContextImpl ctx = new ScriptContextImpl(fc, this, document);
                    script.onEvent(fc, new LytEvent(EventType.MOUNT, fc), ctx);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (fc instanceof LytFlowSpan span) {
            for (var child : span.getChildren()) {
                dispatchMountEventsFlowRecursive(child);
            }
        } else if (fc instanceof LytFlowInlineBlock inlineBlock && inlineBlock.getBlock() != null) {
            LytBlock inner = inlineBlock.getBlock();
            String innerCls = inner.getStyleClass();
            if (innerCls != null) {
                LytScript script = scripts.get(innerCls);
                if (script != null) {
                    try {
                        ScriptContextImpl ctx = new ScriptContextImpl(inlineBlock, this, document);
                        script.onEvent(inlineBlock, new LytEvent(EventType.MOUNT, inlineBlock), ctx);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    // ===== Sync events =====

    public void pushEvent(LytEvent event) {
        eventQueue.addLast(event);
        processEventsNow();
    }

    private void processEventsNow() {
        while (!eventQueue.isEmpty()) {
            LytEvent event = eventQueue.pollFirst();
            if (document == null || event.target() == null) continue;
            Object rawTarget = event.target();
            if (rawTarget instanceof InteractiveElement interactive) {
                switch (event.type()) {
                    case CLICK:
                    case DOUBLE_CLICK:
                        if (event.data().containsKey("x") && event.data().containsKey("y")) {
                            interactive.mouseClicked(null,
                                ((Number) event.data().get("x")).intValue(),
                                ((Number) event.data().get("y")).intValue(),
                                event.data().containsKey("button")
                                    ? ((Number) event.data().get("button")).intValue() : 0,
                                event.type() == EventType.DOUBLE_CLICK);
                        }
                        break;
                    case MOUSE_SCROLL:
                        // InteractiveElement does not expose mouseScrolled yet
                        break;
                    default:
                        break;
                }
            }
        }
    }

    // ===== Async tasks =====

    public void submitTask(DeferredTask task) {
        taskQueue.addLast(task);
    }

    public boolean hasWork() {
        return !taskQueue.isEmpty();
    }

    public void step(long deadlineNs) {
        while (!taskQueue.isEmpty() && System.nanoTime() < deadlineNs) {
            DeferredTask task = taskQueue.peekFirst();
            DeferredTask.TaskResult result = task.step(deadlineNs);
            if (result == DeferredTask.TaskResult.DONE) {
                taskQueue.pollFirst();
            }
            if (result == DeferredTask.TaskResult.YIELD) {
                break;
            }
        }
    }

    public int pendingTaskCount() {
        return taskQueue.size();
    }

    public void clear() {
        document = null;
        scripts.clear();
        cachedDocuments.clear();
        pageNodeCounters.clear();
        currentPageId = null;
        eventQueue.clear();
        taskQueue.clear();
        nav.clear();
    }
}
