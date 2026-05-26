package com.hfstudio.guidenh.guide.internal.host;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.hfstudio.guidenh.guide.document.block.LytNode;

public class LytEvent {

    private final EventType type;
    private final LytNode target;
    private LytNode currentTarget;
    private final Map<String, Object> data;
    private boolean propagationStopped;

    public LytEvent(EventType type, LytNode target) {
        this(type, target, null);
    }

    public LytEvent(EventType type, LytNode target, Map<String, Object> data) {
        this.type = type;
        this.target = target;
        this.currentTarget = target;
        this.data = data != null
            ? Collections.unmodifiableMap(new LinkedHashMap<>(data))
            : Collections.emptyMap();
    }

    public EventType type() { return type; }
    public LytNode target() { return target; }
    public LytNode currentTarget() { return currentTarget; }
    public Map<String, Object> data() { return data; }

    public void stopPropagation() { propagationStopped = true; }
    public boolean isPropagationStopped() { return propagationStopped; }

    void setCurrentTarget(LytNode node) { this.currentTarget = node; }
}
