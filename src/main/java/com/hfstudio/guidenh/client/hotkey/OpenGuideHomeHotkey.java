package com.hfstudio.guidenh.client.hotkey;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import com.hfstudio.guidenh.guide.internal.GuideScreen;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class OpenGuideHomeHotkey {

    public static final KeyBinding OPEN_GUIDE_HOME_KEY = new KeyBinding(
        "key.guidenh.open_guide_home",
        Keyboard.KEY_NONE,
        "key.categories.guidenh");

    private OpenGuideHomeHotkey() {}

    public static void init() {
        ClientRegistry.registerKeyBinding(OPEN_GUIDE_HOME_KEY);
        FMLCommonHandler.instance()
            .bus()
            .register(new OpenGuideHomeHotkey());
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            return;
        }
        if (mc.currentScreen != null) {
            return;
        }
        if (OPEN_GUIDE_HOME_KEY.isPressed()) {
            GuideScreen.openFromHomeHotkey();
        }
    }

    public static KeyBinding getHotkey() {
        return OPEN_GUIDE_HOME_KEY;
    }
}
