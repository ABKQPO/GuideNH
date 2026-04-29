package com.hfstudio.guidenh.guide.internal.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.GuideNH;
import com.hfstudio.guidenh.guide.internal.GuideMEProxy;
import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.GuidebookText;

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
        var target = GuideItemTargetResolver.resolve(stack, GuideRegistry.getAll());
        if (world.isRemote) {
            if (target != null) {
                GuideMEProxy.instance()
                    .openGuide(player, target.guideId(), target.anchor());
            } else {
                player.addChatMessage(new ChatComponentTranslation(GuidebookText.ItemNoGuidePage.getTranslationKey()));
            }
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
