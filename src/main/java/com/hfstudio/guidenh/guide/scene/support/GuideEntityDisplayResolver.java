package com.hfstudio.guidenh.guide.scene.support;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;

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

        try {
            String name = entity.getCommandSenderName();
            if (name != null && !name.trim()
                .isEmpty()) {
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
}
