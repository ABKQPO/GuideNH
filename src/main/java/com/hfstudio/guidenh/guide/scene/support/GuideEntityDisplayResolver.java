package com.hfstudio.guidenh.guide.scene.support;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import org.jetbrains.annotations.Nullable;

public class GuideEntityDisplayResolver {

    private GuideEntityDisplayResolver() {}

    @Nullable
    public static String resolveDisplayName(@Nullable Entity entity) {
        if (entity == null) {
            return null;
        }

        // EntityItem: use the held item's display name to avoid unlocalized "item.xxx" strings.
        if (entity instanceof EntityItem ei) {
            try {
                ItemStack stack = ei.getEntityItem();
                if (stack != null) {
                    String displayName = stack.getDisplayName();
                    if (displayName != null && !displayName.trim()
                        .isEmpty()) {
                        return displayName;
                    }
                }
            } catch (Throwable ignored) {}
        }

        String customName = resolveUsefulCustomName(entity);
        if (customName != null) {
            return customName;
        }

        String translatedEntityName = resolveTranslatedEntityName(entity);
        if (translatedEntityName != null) {
            return translatedEntityName;
        }

        try {
            String name = entity.getCommandSenderName();
            if (isUsefulDisplayName(name)) {
                return name;
            }
        } catch (Throwable ignored) {}

        try {
            String entityId = EntityList.getEntityString(entity);
            if (entityId != null && !entityId.trim()
                .isEmpty()) {
                return entityId;
            }
        } catch (Throwable ignored) {}

        try {
            String simpleName = entity.getClass()
                .getSimpleName();
            return simpleName != null && !simpleName.trim()
                .isEmpty() ? simpleName : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    public static String resolveUsefulCustomName(Entity entity) {
        if (!(entity instanceof EntityLiving living)) {
            return null;
        }
        try {
            if (!living.hasCustomNameTag()) {
                return null;
            }
            String customName = living.getCustomNameTag();
            return isUsefulDisplayName(customName) ? customName : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    public static String resolveTranslatedEntityName(Entity entity) {
        try {
            String entityId = EntityList.getEntityString(entity);
            if (entityId == null || entityId.trim()
                .isEmpty()) {
                return null;
            }
            String key = "entity." + entityId + ".name";
            String translated = StatCollector.translateToLocal(key);
            if (translated != null && !translated.equals(key)
                && !translated.trim()
                    .isEmpty()) {
                return translated;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public static boolean isUsefulDisplayName(@Nullable String value) {
        if (value == null) {
            return false;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        return !"\"\"".equals(trimmed) && !"''".equals(trimmed);
    }
}
