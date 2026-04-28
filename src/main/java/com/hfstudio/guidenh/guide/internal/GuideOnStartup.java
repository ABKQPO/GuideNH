package com.hfstudio.guidenh.guide.internal;

import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.util.ResourceLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hfstudio.guidenh.guide.PageAnchor;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class GuideOnStartup {

    private static final Logger LOG = LoggerFactory.getLogger(GuideOnStartup.class);

    public static final class TriggerState {

        private boolean triggered;

        boolean tryMarkTriggered() {
            if (triggered) {
                return false;
            }
            triggered = true;
            return true;
        }
    }

    public final GuideStartupOptions.ShowOnStartup showOnStartup;
    public final Set<ResourceLocation> guidesToValidate;
    public final TriggerState triggerState = new TriggerState();

    public GuideOnStartup(GuideStartupOptions.ShowOnStartup showOnStartup, Set<ResourceLocation> guidesToValidate) {
        this.showOnStartup = showOnStartup;
        this.guidesToValidate = guidesToValidate;
    }

    public static void init() {
        GuideStartupOptions.ShowOnStartup showOnStartup = GuideStartupOptions
            .parseShowOnStartup(System.getProperty("guideme.showOnStartup"));
        Set<ResourceLocation> guidesToValidate = GuideStartupOptions
            .parseValidateAtStartup(System.getProperty("guideme.validateAtStartup"));
        if (showOnStartup == null && guidesToValidate.isEmpty()) {
            return;
        }

        FMLCommonHandler.instance()
            .bus()
            .register(new GuideOnStartup(showOnStartup, guidesToValidate));
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        if (!(minecraft.currentScreen instanceof GuiMainMenu)) {
            return;
        }
        if (!triggerState.tryMarkTriggered()) {
            return;
        }

        for (ResourceLocation guideId : guidesToValidate) {
            MutableGuide guide = GuideRegistry.getById(guideId);
            if (guide == null) {
                LOG.error("Cannot validate guide '{}' because it is not registered.", guideId);
                continue;
            }

            try {
                guide.validateAll();
            } catch (RuntimeException e) {
                LOG.error("Failed to validate guide '{}'", guideId, e);
            }
        }

        if (showOnStartup == null) {
            return;
        }

        MutableGuide guide = GuideRegistry.getById(showOnStartup.guideId());
        if (guide == null) {
            LOG.error("Cannot open guide '{}' because it is not registered.", showOnStartup.guideId());
            return;
        }

        try {
            PageAnchor anchor = showOnStartup.anchor();
            GuideScreen.open(showOnStartup.guideId(), anchor);
        } catch (RuntimeException e) {
            LOG.error("Failed to open guide '{}'", showOnStartup.guideId(), e);
        }
    }
}
