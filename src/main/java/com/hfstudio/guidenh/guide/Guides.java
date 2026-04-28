package com.hfstudio.guidenh.guide;

import java.util.Collection;

import org.jetbrains.annotations.Nullable;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

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
        // TODO: 1.7.10 port - needs item registration
        var stack = new ItemStack(Items.book);
        if (guideId != null) {
            var tag = new NBTTagCompound();
            tag.setString("GuideId", guideId.toString());
            stack.setTagCompound(tag);
        }
        return stack;
    }
}
