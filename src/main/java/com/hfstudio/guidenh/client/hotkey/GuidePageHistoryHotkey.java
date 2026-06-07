package com.hfstudio.guidenh.client.hotkey;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import com.hfstudio.guidenh.guide.internal.GuideScreen;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuidePageHistoryHotkey {

    public static final KeyBinding GUIDE_PAGE_BACK_KEY = new KeyBinding(
        "key.guidenh.guide_page_back",
        Keyboard.KEY_NONE,
        "key.categories.guidenh");

    public static final KeyBinding GUIDE_PAGE_FORWARD_KEY = new KeyBinding(
        "key.guidenh.guide_page_forward",
        Keyboard.KEY_NONE,
        "key.categories.guidenh");

    private GuidePageHistoryHotkey() {}

    public static void init() {
        ClientRegistry.registerKeyBinding(GUIDE_PAGE_BACK_KEY);
        ClientRegistry.registerKeyBinding(GUIDE_PAGE_FORWARD_KEY);
        FMLCommonHandler.instance()
            .bus()
            .register(new GuidePageHistoryHotkey());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!(Minecraft.getMinecraft().currentScreen instanceof GuideScreen guideScreen)) {
            return;
        }

        while (GUIDE_PAGE_BACK_KEY.isPressed()) {
            guideScreen.navigateBackFromHotkey();
        }

        while (GUIDE_PAGE_FORWARD_KEY.isPressed()) {
            guideScreen.navigateForwardFromHotkey();
        }
    }
}
