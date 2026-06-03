package com.hfstudio.guidenh.guide.internal.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.GuideNH;
import com.hfstudio.guidenh.guide.internal.GuideScreen;

public class GuideItem extends Item {

    public GuideItem() {
        super();
        setUnlocalizedName("guide");
        setTextureName(GuideNH.MODID + ":" + "guide");
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.tabMisc);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote) {
            GuideScreen.openHomePage();
        }
        return stack;
    }

    @Nullable
    public static ResourceLocation getGuideId(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) return null;
        var nbt = stack.getTagCompound();
        if (nbt.hasKey("GuideId")) {
            return new ResourceLocation(nbt.getString("GuideId"));
        }
        if (nbt.hasKey("guideId")) {
            return new ResourceLocation(nbt.getString("guideId"));
        }
        return null;
    }
}
