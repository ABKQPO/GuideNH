package com.hfstudio.guidenh.guide.internal.item;

import javax.annotation.Nullable;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import com.hfstudio.guidenh.GuideNH;
import com.hfstudio.guidenh.guide.internal.GuideMEProxy;

public class GuideItem extends Item {

    public GuideItem() {
        super();
        setUnlocalizedName("guide");
        setTextureName(GuideNH.MODID + ":" + "guide");
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.tabMisc);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        var guideId = getGuideId(stack);
        if (guideId != null) {
            var name = GuideMEProxy.instance()
                .getGuideDisplayName(guideId);
            if (name != null) {
                return name;
            }
        }
        return super.getItemStackDisplayName(stack);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        var guideId = getGuideId(stack);
        if (guideId == null) {
            guideId = new ResourceLocation("guidenh", "guidenh");
        }
        if (world.isRemote) {
            GuideMEProxy.instance()
                .openGuide(player, guideId, null);
        }
        return stack;
    }

    @Nullable
    public static ResourceLocation getGuideId(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) return null;
        var nbt = stack.getTagCompound();
        if (!nbt.hasKey("GuideId")) return null;
        return new ResourceLocation(nbt.getString("GuideId"));
    }
}
