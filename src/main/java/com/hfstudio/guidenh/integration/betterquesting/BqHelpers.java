package com.hfstudio.guidenh.integration.betterquesting;

import java.util.Map;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;

import org.jetbrains.annotations.Nullable;

import betterquesting.api.api.QuestingAPI;
import betterquesting.api.enums.EnumQuestVisibility;
import betterquesting.api.properties.NativeProps;
import betterquesting.api.questing.IQuest;
import betterquesting.api.storage.BQ_Settings;
import betterquesting.api2.cache.QuestCache;
import betterquesting.api2.client.gui.GuiScreenCanvas;
import betterquesting.api2.client.gui.themes.gui_args.GArgsNone;
import betterquesting.api2.client.gui.themes.presets.PresetGUIs;
import betterquesting.api2.utils.QuestTranslation;
import betterquesting.client.gui2.GuiHome;
import betterquesting.client.gui2.GuiQuest;
import betterquesting.client.gui2.GuiQuestLines;
import betterquesting.client.themes.ThemeRegistry;
import betterquesting.questing.QuestDatabase;

public class BqHelpers {

    /**
     * Resolves the per-player display attributes of a quest by its UUID. Never returns
     * {@code null}: missing/unparseable UUIDs and unloaded databases yield a {@code MISSING}
     * display.
     * <p/>
     * Visibility matches BetterQuesting's own GUI rules. A quest is considered visible when BQ
     * would show it in client UI, even if it is still locked behind prerequisites.
     *
     * @param questId The quest UUID, may be {@code null}.
     * @param player  The player whose progress is queried; {@code null} treats the quest as
     *                locked.
     */
    public static QuestDisplay resolveDisplay(@Nullable UUID questId, @Nullable EntityPlayer player) {
        return resolveDisplay(questId, player, true);
    }

    /**
     * Resolves the per-player display attributes of a quest by its UUID while allowing callers to
     * skip description translation when they know no tooltip or body text will be shown.
     */
    public static QuestDisplay resolveDisplay(@Nullable UUID questId, @Nullable EntityPlayer player,
        boolean includeDescription) {
        if (questId == null) {
            return new QuestDisplay(QuestState.MISSING, null, null);
        }

        IQuest quest = QuestDatabase.INSTANCE.get(questId);
        if (quest == null) {
            return new QuestDisplay(QuestState.MISSING, null, null);
        }

        QuestState state;
        if (player == null) {
            state = QuestState.LOCKED;
        } else {
            UUID playerId = playerUuid(player);
            if (quest.isComplete(playerId)) {
                state = QuestState.COMPLETED;
            } else if (QuestCache.isQuestShown(quest, playerId, player)) {
                state = QuestState.VISIBLE;
            } else {
                EnumQuestVisibility visibility = quest.getProperty(NativeProps.VISIBILITY);
                state = mapHiddenVisibility(visibility);
            }
        }

        if (state == QuestState.HIDDEN) {
            return new QuestDisplay(state, null, null);
        }

        String name = QuestTranslation.translateQuestName(questId, quest);
        String description = includeDescription ? QuestTranslation.translateQuestDescription(questId, quest) : null;
        return new QuestDisplay(state, name, description);
    }

    /**
     * Looks up the BetterQuesting questing UUID for the given player. Falls back to the
     * vanilla GameProfile UUID if the BQ name cache is unavailable.
     */
    public static UUID playerUuid(EntityPlayer player) {
        return QuestingAPI.getQuestingUUID(player);
    }

    /**
     * Opens BetterQuesting's quest GUI for the given quest UUID on the current screen. This
     * mirrors BetterQuesting's own quest-open path used by quest buttons. Must be called on the
     * client thread.
     */
    public static void openQuestGui(UUID questId) {
        Minecraft mc = Minecraft.getMinecraft();
        openQuestGui(questId, mc != null ? mc.currentScreen : null);
    }

    /**
     * Opens BetterQuesting's quest GUI using BetterQuesting's own parent-screen chain so the
     * quest book navigation behaves the same as native quest jumps.
     */
    public static void openQuestGui(UUID questId, @Nullable GuiScreen previousScreen) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return;
        }

        GuiQuest questScreen = new GuiQuest(resolveQuestParentScreen(), questId);
        if (previousScreen != null) {
            questScreen.setPreviousScreen(previousScreen);
        }
        mc.displayGuiScreen(questScreen);
        if (BQ_Settings.useBookmark) {
            GuiHome.bookmark = questScreen;
        }
    }

    /**
     * Iterates all quests in the BetterQuesting database. Used by lookup features that need to
     * resolve quests outside of a known UUID set (currently unused; provided for completeness).
     */
    public static Iterable<Map.Entry<UUID, IQuest>> allQuests() {
        return QuestDatabase.INSTANCE.entrySet();
    }

    private static QuestState mapHiddenVisibility(@Nullable EnumQuestVisibility visibility) {
        if (visibility == null) return QuestState.LOCKED;
        return switch (visibility) {
            case HIDDEN, SECRET -> QuestState.HIDDEN;
            default -> QuestState.LOCKED;
        };
    }

    private static GuiScreen resolveQuestParentScreen() {
        if (GuiHome.bookmark instanceof GuiQuest && BQ_Settings.useBookmark) {
            return ((GuiScreenCanvas) GuiHome.bookmark).parent;
        }
        if (GuiHome.bookmark instanceof GuiScreenCanvas && BQ_Settings.useBookmark) {
            return GuiHome.bookmark;
        }

        GuiScreen homeScreen = ThemeRegistry.INSTANCE.getGui(PresetGUIs.HOME, GArgsNone.NONE);
        if (BQ_Settings.useBookmark && BQ_Settings.skipHome) {
            return new GuiQuestLines(homeScreen);
        }
        return homeScreen;
    }
}
