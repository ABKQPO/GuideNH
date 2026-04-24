package com.hfstudio.guidenh.guide.internal.recipe;

import java.util.WeakHashMap;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Drives {@code IRecipeHandler.onUpdate()} (used by some handlers for animation, e.g. the arrow
 * progress bar) once per client tick. Registered lazily on the first recipe box render and kept
 * alive as long as the handler object remains referenced anywhere. Handlers are held via
 * {@link WeakHashMap} so unused ones are garbage-collected with their entries.
 */
public final class NeiAnimationTicker {

    private static final WeakHashMap<Object, Boolean> TRACKED = new WeakHashMap<>();
    private static boolean registered;

    private NeiAnimationTicker() {}

    public static void ensureUpdating(Object handler) {
        if (handler == null || !NeiRecipeLookup.isAvailable()) return;
        synchronized (TRACKED) {
            TRACKED.put(handler, Boolean.TRUE);
            if (!registered) {
                registered = true;
                cpw.mods.fml.common.FMLCommonHandler.instance()
                    .bus()
                    .register(new NeiAnimationTicker());
            }
        }
    }

    public static void clear() {
        synchronized (TRACKED) {
            TRACKED.clear();
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Object[] snapshot;
        synchronized (TRACKED) {
            snapshot = TRACKED.keySet()
                .toArray();
        }
        for (Object o : snapshot) {
            NeiRecipeLookup.callOnUpdate(o);
        }
    }
}
