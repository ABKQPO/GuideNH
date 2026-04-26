package com.hfstudio.guidenh.client.hotkey;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import com.hfstudio.guidenh.guide.internal.editor.SceneEditorScreen;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class OpenSceneEditorHotkey {

    private static final KeyBinding OPEN_SCENE_EDITOR_KEY = new KeyBinding(
        "key.guidenh.open_scene_editor",
        Keyboard.KEY_NONE,
        "key.categories.guidenh");

    private OpenSceneEditorHotkey() {}

    public static void init() {
        ClientRegistry.registerKeyBinding(OPEN_SCENE_EDITOR_KEY);
        FMLCommonHandler.instance()
            .bus()
            .register(new OpenSceneEditorHotkey());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            return;
        }

        while (OPEN_SCENE_EDITOR_KEY.isPressed()) {
            SceneEditorScreen.open();
        }
    }

    public static KeyBinding getHotkey() {
        return OPEN_SCENE_EDITOR_KEY;
    }
}
