package com.hfstudio.guidenh.client.hotkey;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import org.lwjgl.input.Keyboard;

import com.hfstudio.guidenh.compat.Mods;
import com.hfstudio.guidenh.compat.betterquesting.BqCompat;
import com.hfstudio.guidenh.compat.betterquesting.QuestIndex;
import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.indices.ItemIndex;
import com.hfstudio.guidenh.guide.indices.OreIndex;
import com.hfstudio.guidenh.guide.internal.GuideMEProxy;
import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.GuideScreen;
import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class OpenGuideHotkey {

    public static final int TICKS_TO_OPEN = 10;

    public static final KeyBinding OPEN_GUIDE_KEY = new KeyBinding(
        "key.guidenh.open_guide",
        Keyboard.KEY_G,
        "key.categories.guidenh");

    public static String previousItemId;
    public static final List<FoundPage> guidebookPages = new ArrayList<>();
    public static int ticksKeyHeld;
    public static boolean holding;
    public static boolean newTick = true;

    // Parallel state for the BetterQuesting quest-hover hotkey path. Kept independent of the
    // item-tooltip flow so that hovering a BQ quest in the BQ GUI does not interfere with item
    // hover detection in the inventory and vice versa.
    public static UUID previousQuestId;
    public static final List<FoundPage> questGuidebookPages = new ArrayList<>();
    public static int questTicksKeyHeld;

    private OpenGuideHotkey() {}

    public static class FoundPage {

        final MutableGuide guide;
        final PageAnchor page;

        FoundPage(MutableGuide guide, PageAnchor page) {
            this.guide = guide;
            this.page = page;
        }
    }

    public static void init() {
        ClientRegistry.registerKeyBinding(OPEN_GUIDE_KEY);
        OpenGuideHotkey instance = new OpenGuideHotkey();
        FMLCommonHandler.instance()
            .bus()
            .register(instance);
        MinecraftForge.EVENT_BUS.register(instance);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            newTick = true;
            if (Mods.BetterQuesting.isModLoaded()) {
                tickQuestHover();
            }
        }
    }

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        var mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || event.entityPlayer != mc.thePlayer) {
            return;
        }
        handleTooltip(event.itemStack, event.toolTip);
    }

    public static void handleTooltip(ItemStack itemStack, List<String> lines) {
        if (newTick) {
            newTick = false;
            update(itemStack);
        }

        if (guidebookPages.isEmpty()) {
            return;
        }

        var found = guidebookPages.get(0);

        var current = GuideScreen.current();
        if (current != null && current.getCurrentPageId()
            .equals(found.page.pageId())) {
            return;
        }

        float progress = ticksKeyHeld / (float) TICKS_TO_OPEN;
        if (progress < 0f) progress = 0f;
        if (progress > 1f) progress = 1f;

        String hint = renderHint(progress);
        if (lines.isEmpty()) {
            lines.add(hint);
        } else {
            lines.add(1, hint);
        }
    }

    public static String renderHint(float progress) {
        var fr = Minecraft.getMinecraft().fontRenderer;
        String keyName = Keyboard.getKeyName(OPEN_GUIDE_KEY.getKeyCode());
        String holdLabel = GuidebookText.HoldToShow
            .text(EnumChatFormatting.GRAY + keyName + EnumChatFormatting.DARK_GRAY);

        if (progress <= 0f) {
            return EnumChatFormatting.DARK_GRAY + holdLabel;
        }

        int barChar = fr.getCharWidth('|');
        if (barChar <= 0) {
            barChar = 2;
        }
        int totalWidth = fr.getStringWidth(holdLabel);
        int totalChars = Math.max(1, totalWidth / barChar);
        int filled = (int) (progress * totalChars);
        if (filled < 0) filled = 0;
        if (filled > totalChars) filled = totalChars;

        var sb = new StringBuilder();
        sb.append(EnumChatFormatting.GRAY);
        for (int i = 0; i < filled; i++) sb.append('|');
        if (filled < totalChars) {
            sb.append(EnumChatFormatting.DARK_GRAY);
            for (int i = 0; i < totalChars - filled; i++) sb.append('|');
        }
        return sb.toString();
    }

    public static void update(ItemStack stack) {
        String itemId = resolveItemId(stack);
        if (!Objects.equals(itemId, previousItemId)) {
            previousItemId = itemId;
            guidebookPages.clear();
            ticksKeyHeld = 0;

            if (stack != null && stack.getItem() != null) {
                for (var guide : GuideRegistry.getAll()) {
                    if (!guide.isAvailableToOpenHotkey()) {
                        continue;
                    }
                    PageAnchor anchor;
                    try {
                        anchor = guide.getIndex(ItemIndex.class)
                            .findByStack(stack);
                        if (anchor == null) {
                            anchor = guide.getIndex(OreIndex.class)
                                .findByStack(stack);
                        }
                    } catch (IllegalArgumentException ignored) {
                        continue;
                    }
                    if (anchor != null) {
                        guidebookPages.add(new FoundPage(guide, anchor));
                    }
                }
            }
        }

        holding = isKeyHeld();
        if (holding) {
            if (ticksKeyHeld < TICKS_TO_OPEN && ++ticksKeyHeld == TICKS_TO_OPEN) {
                if (!guidebookPages.isEmpty()) {
                    var found = guidebookPages.get(0);
                    var mc = Minecraft.getMinecraft();
                    if (mc.currentScreen instanceof GuideUiHost) {
                        ((GuideUiHost) mc.currentScreen).navigateTo(found.page);
                    } else {
                        GuideMEProxy.instance()
                            .openGuide(mc.thePlayer, found.guide.getId(), found.page);
                    }
                    ticksKeyHeld = 0;
                    holding = false;
                }
            } else if (ticksKeyHeld > TICKS_TO_OPEN) {
                ticksKeyHeld = TICKS_TO_OPEN;
            }
        } else if (ticksKeyHeld > 0) {
            ticksKeyHeld = Math.max(0, ticksKeyHeld - 2);
        }
    }

    public static boolean isKeyHeld() {
        int code = OPEN_GUIDE_KEY.getKeyCode();
        if (code <= 0) {
            return false;
        }
        return Keyboard.isKeyDown(code);
    }

    public static String resolveItemId(ItemStack stack) {
        if (stack == null) return null;
        Item item = stack.getItem();
        if (item == null) return null;
        Object name = Item.itemRegistry.getNameForObject(item);
        return name != null ? name.toString() : null;
    }

    /**
     * Tick handler for the BetterQuesting quest-hover hotkey path. Reads the currently hovered
     * quest UUID published by a BQ-targeted mixin and, when held long enough, opens the guide
     * page indexed against that UUID.
     */
    public static void tickQuestHover() {
        UUID hovered = BqCompat.getCurrentHoveredQuestUuid();
        if (!Objects.equals(hovered, previousQuestId)) {
            previousQuestId = hovered;
            questGuidebookPages.clear();
            questTicksKeyHeld = 0;
            if (hovered != null) {
                for (var guide : GuideRegistry.getAll()) {
                    if (!guide.isAvailableToOpenHotkey()) {
                        continue;
                    }
                    PageAnchor anchor;
                    try {
                        anchor = guide.getIndex(QuestIndex.class)
                            .findByUuid(hovered);
                    } catch (IllegalArgumentException ignored) {
                        continue;
                    }
                    if (anchor != null) {
                        questGuidebookPages.add(new FoundPage(guide, anchor));
                    }
                }
            }
        }
        if (questGuidebookPages.isEmpty()) {
            questTicksKeyHeld = 0;
            return;
        }
        boolean held = isKeyHeld();
        if (held) {
            if (questTicksKeyHeld < TICKS_TO_OPEN && ++questTicksKeyHeld == TICKS_TO_OPEN) {
                var found = questGuidebookPages.get(0);
                var mc = Minecraft.getMinecraft();
                if (mc.currentScreen instanceof GuideUiHost) {
                    ((GuideUiHost) mc.currentScreen).navigateTo(found.page);
                } else {
                    GuideMEProxy.instance()
                        .openGuide(mc.thePlayer, found.guide.getId(), found.page);
                }
                questTicksKeyHeld = 0;
            } else if (questTicksKeyHeld > TICKS_TO_OPEN) {
                questTicksKeyHeld = TICKS_TO_OPEN;
            }
        } else if (questTicksKeyHeld > 0) {
            questTicksKeyHeld = Math.max(0, questTicksKeyHeld - 2);
        }
    }

    public static KeyBinding getHotkey() {
        return OPEN_GUIDE_KEY;
    }
}
