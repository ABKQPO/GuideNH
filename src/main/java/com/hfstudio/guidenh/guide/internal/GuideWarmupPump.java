package com.hfstudio.guidenh.guide.internal;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Drives two background tasks each client tick to reduce first-open latency:
 * <ol>
 * <li><b>Page warm-up</b> – pre-compiles the start page of every registered guide so that the first
 * call to {@link MutableGuide#getPage} returns immediately instead of freezing the client for 5-6 s.
 * Warm-up is deferred until an active server connection is available (required for
 * {@code GuidebookFakeWorld} scene rendering).</li>
 * <li><b>Search index</b> – advances the incremental Lucene indexing work that was queued during
 * the last resource reload, spending at most 5 ms per tick so it completes in the background
 * without impacting frame rate.</li>
 * </ol>
 */
public class GuideWarmupPump {

    public static void init() {
        FMLCommonHandler.instance()
            .bus()
            .register(new GuideWarmupPump());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        for (MutableGuide guide : GuideRegistry.getAll()) {
            guide.tickWarmup();
        }

        // Advance incremental search indexing (5 ms budget per tick via GuideSearch.TIME_PER_TICK).
        GuideME.getSearch()
            .processWork();
    }
}
