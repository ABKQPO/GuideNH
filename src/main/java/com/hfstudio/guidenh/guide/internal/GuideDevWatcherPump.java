package com.hfstudio.guidenh.guide.internal;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class GuideDevWatcherPump {

    public static final int DEFAULT_INTERVAL_TICKS = 20;

    public interface TickableGuide {

        boolean hasDevelopmentSources();

        void tickDevelopmentSources();
    }

    private final int intervalTicks;
    private int tickCounter;

    public GuideDevWatcherPump(int intervalTicks) {
        this.intervalTicks = Math.max(1, intervalTicks);
    }

    public static void init() {
        if (!hasAnyDevelopmentSources(GuideRegistry.getAll())) {
            return;
        }
        FMLCommonHandler.instance()
            .bus()
            .register(new GuideDevWatcherPump(DEFAULT_INTERVAL_TICKS));
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        tick(GuideRegistry.getAll());
    }

    public void tick(Iterable<? extends TickableGuide> guides) {
        tickCounter++;
        if (tickCounter < intervalTicks) {
            return;
        }
        tickCounter = 0;

        for (TickableGuide guide : guides) {
            if (guide.hasDevelopmentSources()) {
                guide.tickDevelopmentSources();
            }
        }
    }

    public static boolean hasAnyDevelopmentSources(Iterable<? extends TickableGuide> guides) {
        for (TickableGuide guide : guides) {
            if (guide.hasDevelopmentSources()) {
                return true;
            }
        }
        return false;
    }
}
