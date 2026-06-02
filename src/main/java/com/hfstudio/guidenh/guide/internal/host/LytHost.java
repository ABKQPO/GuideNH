package com.hfstudio.guidenh.guide.internal.host;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.GuidePage;
import com.hfstudio.guidenh.guide.PageCollection;
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
    @Nullable private PageCollection currentPageCollection;
    private final Map<String, LytScript> scripts = new HashMap<>();
    private final Map<String, PageCacheEntry> cachedDocuments = new LinkedHashMap<>();
    private final Map<String, AtomicInteger> pageNodeCounters = new HashMap<>();
    String currentPageId;

    private static final int MAX_CACHED_PAGES = 32;

    static class PageCacheEntry {
        final GuidePage guidePage;
        final Map<String, Object> nodeResults = new HashMap<>();
        PageCacheEntry(GuidePage guidePage) { this.guidePage = guidePage; }
    }

    private final ViewportState viewport = new ViewportState();
    private final NavigationState nav = new NavigationState();
    private final Deque<LytEvent> eventQueue = new ArrayDeque<>();
    private final Deque<DeferredTask> taskQueue = new ArrayDeque<>();
    private final Deque<String> preheatQueue = new ArrayDeque<>();
    private final java.util.Set<String> preheatScheduled = new java.util.HashSet<>();

    @FunctionalInterface
    public interface PreheatCompiler {
        @Nullable GuidePage compile(String pageId);
    }

    @Nullable private PreheatCompiler preheatCompiler;

    public void setPreheatCompiler(@Nullable PreheatCompiler compiler) {
        this.preheatCompiler = compiler;
    }

    // ===== Document =====

    /** Full processing: UID allocation, onAttach, MOUNT dispatch. Resets the node counter so the
     *  same page always gets the same UIDs across remounts (enabling node-level cache hits). */
    public void mountDocument(@Nullable LytDocument newDoc) {
        if (this.document != null && this.document != newDoc) {
            this.document.setLive(false);    // onDetach cascade on old doc
        }
        this.document = newDoc;
        if (newDoc != null) {
            pageNodeCounters.remove(currentPageId);  // reset for stable UIDs
            allocateNodeUids(newDoc);
            newDoc.setLive(true);            // onAttach cascade
            dispatchMountEvents(newDoc);     // MOUNT events → scripts materialize placeholders
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
    public GuidePage getCachedGuidePage(String pageId) {
        PageCacheEntry entry = cachedDocuments.get(pageId);
        return entry != null ? entry.guidePage : null;
    }

    public void recordNodeResult(String pageId, String nodeUid, Object result) {
        PageCacheEntry entry = cachedDocuments.get(pageId);
        if (entry != null) {
            entry.nodeResults.put(nodeUid, result);
        }
    }

    @Nullable
    Object getNodeResult(String pageId, String nodeUid) {
        PageCacheEntry entry = cachedDocuments.get(pageId);
        return entry != null ? entry.nodeResults.get(nodeUid) : null;
    }

    public void cachePage(String pageId, GuidePage guidePage) {
        while (cachedDocuments.size() >= MAX_CACHED_PAGES) {
            var oldest = cachedDocuments.keySet().iterator().next();
            cachedDocuments.remove(oldest);
            pageNodeCounters.remove(oldest);
            preheatScheduled.remove(oldest);
        }
        cachedDocuments.put(pageId, new PageCacheEntry(guidePage));
    }

    public void invalidatePage(String pageId) {
        cachedDocuments.remove(pageId);
        pageNodeCounters.remove(pageId);
        preheatScheduled.remove(pageId);
    }

    public void setCurrentPageId(String pageId) {
        this.currentPageId = pageId;
    }

    public void setCurrentPageCollection(@Nullable PageCollection pageCollection) {
        this.currentPageCollection = pageCollection;
    }

    @Nullable
    public PageCollection getCurrentPageCollection() {
        return currentPageCollection;
    }

    public void requestPreheat(String pageId) {
        if (preheatCompiler == null || pageId == null) return;
        if (cachedDocuments.containsKey(pageId)) return;
        if (preheatScheduled.add(pageId)) {
            preheatQueue.addLast(pageId);
        }
    }

    public void requestPreheatNeighbors(String currentPageId) {
        PageCollection pc = currentPageCollection;
        if (pc == null) return;
        var nav = pc.getNavigationTree();
        if (nav == null) return;
        var node = nav.getNodeById(new net.minecraft.util.ResourceLocation(currentPageId));
        if (node == null) return;
        for (var child : node.children()) {
            if (child.hasPage()) {
                requestPreheat(child.pageId().toString());
            }
        }
    }

    public boolean hasPreheatWork() {
        return !preheatQueue.isEmpty() && preheatCompiler != null;
    }

    public void preheatStep(long deadlineNs) {
        while (!preheatQueue.isEmpty() && System.nanoTime() < deadlineNs) {
            String pageId = preheatQueue.pollFirst();
            if (cachedDocuments.containsKey(pageId)) {
                preheatScheduled.remove(pageId);
                continue;
            }
            if (preheatCompiler == null) break;
            GuidePage compiled = preheatCompiler.compile(pageId);
            if (compiled != null) {
                cachePage(pageId, compiled);
            }
            preheatScheduled.remove(pageId);
        }
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
                dispatchScript(script, node);
            }
        }
        for (var child : node.getChildren()) {
            dispatchMountEvents(child);
        }
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
                dispatchScript(script, fc);
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
                    dispatchScript(script, inlineBlock);
                }
            }
        }
    }

    private void dispatchScript(LytScript script, Object node) {
        String nodeUid = nodeUidOf(node);
        if (nodeUid != null) {
            Object cached = getNodeResult(currentPageId, nodeUid);
            if (cached != null) {
                new ScriptContextImpl(node, this, document).replace(cached);
                return;
            }
        }
        if (script.isAsync()) {
            taskQueue.addLast(new MaterializeTask(script, node, new ScriptContextImpl(node, this, document)));
        } else {
            try {
                ScriptContextImpl ctx = new ScriptContextImpl(node, this, document);
                script.onEvent(node, new LytEvent(EventType.MOUNT, node), ctx);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Nullable
    private static String nodeUidOf(Object node) {
        if (node instanceof LytNode ln) return ln.getNodeUid();
        if (node instanceof LytFlowContent fc) return fc.getNodeUid();
        return null;
    }

    private static class MaterializeTask implements DeferredTask {
        private final LytScript script;
        private final Object node;
        private final ScriptContextImpl ctx;
        private final LytEvent event;
        private boolean firstCall = true;

        MaterializeTask(LytScript script, Object node, ScriptContextImpl ctx) {
            this.script = script;
            this.node = node;
            this.ctx = ctx;
            this.event = new LytEvent(EventType.MOUNT, node);
        }

        @Override
        public Priority priority() { return Priority.HIGH; }

        @Override
        public TaskResult step(long deadlineNs) {
            ctx.setYieldDeadline(deadlineNs);
            ctx.resetYieldState();
            try {
                script.onEvent(node, event, ctx);
            } catch (Exception e) {
                e.printStackTrace();
                ctx.markComplete(); // don't retry on error
            }
            if (ctx.isComplete()) return TaskResult.DONE;
            if (firstCall) {
                firstCall = false;
                // Script that yielded (called timeToYield which returned true): YIELD.
                // Script that finished without yielding: auto-complete.
                if (ctx.isYieldRequested()) return TaskResult.YIELD;
                ctx.markComplete();
                return TaskResult.DONE;
            }
            // Re-entry after yield — auto-complete on second call.
            ctx.markComplete();
            return TaskResult.DONE;
        }

        @Override
        public boolean isDone() { return ctx.isComplete(); }
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

    /** Recursively dispatch MOUNT events into a detached subtree. */
    void dispatchToSubtree(LytNode root) {
        allocateNodeUids(root);
        dispatchMountEvents(root);
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
        currentPageCollection = null;
        scripts.clear();
        cachedDocuments.clear();
        pageNodeCounters.clear();
        currentPageId = null;
        preheatQueue.clear();
        preheatScheduled.clear();
        eventQueue.clear();
        taskQueue.clear();
        nav.clear();
    }
}
