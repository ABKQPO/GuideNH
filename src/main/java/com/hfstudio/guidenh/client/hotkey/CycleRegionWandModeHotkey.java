package com.hfstudio.guidenh.client.hotkey;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentTranslation;

import org.lwjgl.input.Keyboard;

import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.internal.item.RegionWandExportMode;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class CycleRegionWandModeHotkey {

    public static final KeyBinding CYCLE_REGION_WAND_MODE_KEY = new KeyBinding(
        "key.guidenh.cycle_region_wand_mode",
        Keyboard.KEY_NONE,
        "key.categories.guidenh");

    private CycleRegionWandModeHotkey() {}

    public static void init() {
        ClientRegistry.registerKeyBinding(CYCLE_REGION_WAND_MODE_KEY);
        FMLCommonHandler.instance()
            .bus()
            .register(new CycleRegionWandModeHotkey());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        while (CYCLE_REGION_WAND_MODE_KEY.isPressed()) {
            RegionWandExportMode currentMode = ModConfig.ui.regionWandExportMode != null
                ? ModConfig.ui.regionWandExportMode
                : RegionWandExportMode.SNBT;
            RegionWandExportMode nextMode = currentMode.next();
            ModConfig.ui.regionWandExportMode = nextMode;
            ModConfig.save();
            if (mc.thePlayer != null) {
                mc.thePlayer.addChatMessage(
                    new ChatComponentTranslation(
                        GuidebookText.RegionWandModeSwitched.getTranslationKey(),
                        nextMode.getDisplayName()));
            }
        }
    }
}
