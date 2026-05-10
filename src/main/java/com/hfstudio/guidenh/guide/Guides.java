package com.hfstudio.guidenh.guide;

import java.util.Collection;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.GuideNH;
import com.hfstudio.guidenh.guide.internal.GuideRegistry;

/**
 * Global registry of GuideME guides.
 */
public class Guides {

    private Guides() {}

    public static Collection<? extends Guide> getAll() {
        return GuideRegistry.getAll();
    }

    @Nullable
    public static Guide getById(ResourceLocation id) {
        return GuideRegistry.getById(id);
    }

    /**
     * Create a generic guide item that will open the given guide.
     */
    public static ItemStack createGuideItem(ResourceLocation guideId) {
        var stack = new ItemStack(GuideNH.GUIDE_ITEM);
        if (guideId != null) {
            var tag = new NBTTagCompound();
            tag.setString("GuideId", guideId.toString());
            stack.setTagCompound(tag);
        }
        return stack;
    }
}
