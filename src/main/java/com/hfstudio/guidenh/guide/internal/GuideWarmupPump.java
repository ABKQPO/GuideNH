package com.hfstudio.guidenh.guide.internal;

import com.hfstudio.guidenh.guide.internal.search.GuideSearch;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class GuideWarmupPump {

    private static final GuideWarmupScheduler SCHEDULER = new GuideWarmupScheduler();

    private long currentTick;

    public static void init() {
        FMLCommonHandler.instance()
            .bus()
            .register(new GuideWarmupPump());
    }

    public static void clearScheduler() {
        SCHEDULER.clear();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        currentTick++;
        for (MutableGuide guide : GuideRegistry.getAll()) {
            guide.populateWarmupScheduler(SCHEDULER, currentTick);
        }
        SCHEDULER.processTick(currentTick);

        GuideME.getSearch()
            .processWork(GuideSearch.BACKGROUND_TIME_PER_TICK);
    }
}
