package com.hfstudio.guidenh.guide.internal.host;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.PageCollection;
import com.hfstudio.guidenh.guide.document.block.LytDocument;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.guide.indices.PageIndex;

class ScriptContextImpl implements ScriptContext {
    private final Map<String, Object> data = new HashMap<>();
    private final Object node;
    private final LytHost host;
    private final LytDocument document;

    ScriptContextImpl(Object node, LytHost host, LytDocument document) {
        this.node = node;
        this.host = host;
        this.document = document;
    }

    @Override
    public Map<String, Object> data() { return data; }

    @Override
    @SuppressWarnings("unchecked")
    public void replace(Object newNode) {
        if (node instanceof LytNode ln && newNode instanceof LytNode newLn) {
            LytNode parent = ln.getParent();
            if (parent != null) {
                parent.replaceChild(ln, newLn);
            }
            return;
        }
        if (node instanceof LytFlowContent fc && newNode instanceof LytFlowContent newFc) {
            LytFlowParent parent = fc.getParent();
            if (parent instanceof LytFlowSpan span) {
                List<LytFlowContent> children = span.getChildren();
                int idx = children.indexOf(fc);
                if (idx >= 0) {
                    fc.setParent(null);
                    newFc.setParent(span);
                    children.set(idx, newFc);
                    document.invalidateLayout();
                }
                return;
            }
            // Handle LytParagraph and other LytFlowContainer parents
            if (parent instanceof LytParagraph para) {
                Iterable<LytFlowContent> iterable = para.getContent();
                if (iterable instanceof List) {
                    List<LytFlowContent> list = (List<LytFlowContent>) iterable;
                    int idx = list.indexOf(fc);
                    if (idx >= 0) {
                        fc.setParent(null);
                        newFc.setParent(para);
                        list.set(idx, newFc);
                        document.invalidateLayout();
                    }
                }
            }
        }
    }

    @Override
    public String allocateId(String prefix) {
        return host.allocateNodeUid(host.currentPageId, prefix);
    }

    @Override
    public LytDocument document() { return document; }

    @Override
    @Nullable
    public byte[] loadAsset(ResourceLocation id) {
        PageCollection pc = host.getCurrentPageCollection();
        return pc != null ? pc.loadAsset(id) : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends PageIndex> T getIndex(Class<T> indexClass) {
        PageCollection pc = host.getCurrentPageCollection();
        return pc != null ? pc.getIndex(indexClass) : null;
    }

    @Override
    @Nullable
    public PageCollection getPageCollection() {
        return host.getCurrentPageCollection();
    }

    @Override
    public void submitTask(DeferredTask task) {
        host.submitTask(task);
    }
}
