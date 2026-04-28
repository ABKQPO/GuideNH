package com.hfstudio.guidenh.guide.scene.support;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;

public class GuideEntityDisplayResolver {

    private GuideEntityDisplayResolver() {}

    @Nullable
    public static String resolveDisplayName(@Nullable Entity entity) {
        if (entity == null) {
            return null;
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
