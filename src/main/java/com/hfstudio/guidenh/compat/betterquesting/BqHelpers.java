package com.hfstudio.guidenh.compat.betterquesting;

import java.util.Map;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import betterquesting.api.enums.EnumQuestVisibility;
import betterquesting.api.properties.NativeProps;
import betterquesting.api.questing.IQuest;
import betterquesting.api.utils.BigItemStack;
import betterquesting.api2.utils.QuestTranslation;
import betterquesting.client.gui2.GuiQuest;
import betterquesting.questing.QuestDatabase;

public final class BqHelpers {

    public static final Logger LOG = LoggerFactory.getLogger(BqHelpers.class);

    private BqHelpers() {}

    /**
     * Resolves the per-player display attributes of a quest by its UUID. Never returns
     * {@code null}: missing/unparseable UUIDs and unloaded databases yield a {@code MISSING}
     * display.
     *
     * @param questId The quest UUID, may be {@code null}.
     * @param player  The player whose progress is queried; {@code null} treats the quest as
     *                locked.
     */
    public static QuestDisplay resolveDisplay(@Nullable UUID questId, @Nullable EntityPlayer player) {
        if (questId == null) {
            return new QuestDisplay(QuestState.MISSING, null, null, null);
        }

        IQuest quest = QuestDatabase.INSTANCE.get(questId);
        if (quest == null) {
            return new QuestDisplay(QuestState.MISSING, null, null, null);
        }

        String name = QuestTranslation.translateQuestName(questId, quest);
        String description = QuestTranslation.translateQuestDescription(questId, quest);

        BigItemStack rawIcon = quest.getProperty(NativeProps.ICON);
        ItemStack icon = rawIcon != null ? rawIcon.getBaseStack() : null;

        QuestState state;
        if (player == null) {
            state = QuestState.LOCKED;
        } else {
            UUID playerId = playerUuid(player);
            if (quest.isComplete(playerId)) {
                state = QuestState.COMPLETED;
            } else if (quest.isUnlocked(playerId)) {
                state = QuestState.VISIBLE;
            } else {
                EnumQuestVisibility visibility = quest.getProperty(NativeProps.VISIBILITY);
                state = mapHiddenVisibility(visibility);
            }
        }

        return new QuestDisplay(state, name, description, icon);
    }

    /**
     * Looks up the BetterQuesting questing UUID for the given player. Falls back to the
     * vanilla GameProfile UUID if the BQ name cache is unavailable.
     */
    public static UUID playerUuid(EntityPlayer player) {
        return betterquesting.api.api.QuestingAPI.getQuestingUUID(player);
    }

    /**
     * Opens BetterQuesting's quest GUI for the given quest UUID on the current screen. Must be
     * called on the client thread.
     */
    public static void openQuestGui(UUID questId) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return;
        mc.displayGuiScreen(new GuiQuest(mc.currentScreen, questId));
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
}
