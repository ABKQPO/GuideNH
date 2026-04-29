package com.hfstudio.guidenh.guide.compiler;

import java.lang.reflect.Method;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;

public final class GuideItemReferenceResolver {

    public static final String GREGTECH_ORE_DICT_UNIFICATOR_CLASS = "gregtech.api.util.GTOreDictUnificator";

    private GuideItemReferenceResolver() {}

    @Desugar
    public record ResolvedItemReference(ResourceLocation registryId, ItemStack stack) {}

    @Desugar
    public record ResolvedBlockReference(ResourceLocation registryId, Block block, @Nullable ItemStack stack) {}

    @Nullable
    public static ResolvedItemReference resolveItemReference(String defaultNamespace, @Nullable String idText,
        @Nullable String oreName) {
        String trimmedOreName = trimToNull(oreName);
        if (trimmedOreName != null) {
            ItemStack stack = resolveOreDictionaryStack(trimmedOreName);
            ResourceLocation registryId = resolveItemRegistryId(stack);
            return stack == null || registryId == null ? null : new ResolvedItemReference(registryId, stack);
        }

        IdUtils.ParsedItemRef ref = IdUtils.parseItemRef(idText, defaultNamespace);
        if (ref == null) {
            return null;
        }

        Item item = (Item) Item.itemRegistry.getObject(
            ref.id()
                .toString());
        if (item == null) {
            return null;
        }

        ItemStack stack = new ItemStack(item, 1, ref.concreteMeta());
        if (ref.nbt() != null) {
            stack.stackTagCompound = (net.minecraft.nbt.NBTTagCompound) ref.nbt()
                .copy();
        }
        return new ResolvedItemReference(ref.id(), stack);
    }

    @Nullable
    public static ResolvedBlockReference resolveBlockReference(String defaultNamespace, @Nullable String idText,
        @Nullable String oreName) {
        String trimmedOreName = trimToNull(oreName);
        if (trimmedOreName != null) {
            ItemStack stack = resolveOreDictionaryStack(trimmedOreName);
            if (stack == null || stack.getItem() == null) {
                return null;
            }

            Block block = Block.getBlockFromItem(stack.getItem());
            ResourceLocation registryId = resolveBlockRegistryId(block);
            if (block == null || block == Blocks.air || registryId == null) {
                return null;
            }
            return new ResolvedBlockReference(registryId, block, stack);
        }

        String trimmedIdText = trimToNull(idText);
        if (trimmedIdText == null) {
            return null;
        }

        ResourceLocation blockId = IdUtils.resolveId(trimmedIdText, defaultNamespace);
        Block block = (Block) Block.blockRegistry.getObject(blockId.toString());
        if (block == null) {
            return null;
        }

        Item item = Item.getItemFromBlock(block);
        ItemStack stack = item != null ? new ItemStack(item) : null;
        return new ResolvedBlockReference(blockId, block, stack);
    }

    @Nullable
    public static ItemStack resolveOreDictionaryStack(@Nullable String oreName) {
        String trimmedOreName = trimToNull(oreName);
        if (trimmedOreName == null) {
            return null;
        }

        List<ItemStack> oreStacks = OreDictionary.getOres(trimmedOreName);
        if (oreStacks == null || oreStacks.isEmpty()) {
            return null;
        }

        ItemStack firstMatch = oreStacks.get(0);
        if (firstMatch == null || firstMatch.getItem() == null) {
            return null;
        }

        ItemStack copiedStack = firstMatch.copy();
        ItemStack unifiedStack = applyGregTechOreDictUnification(copiedStack);
        return unifiedStack != null && unifiedStack.getItem() != null ? unifiedStack : copiedStack;
    }

    @Nullable
    public static ResourceLocation resolveItemRegistryId(@Nullable ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return null;
        }

        Object rawName = Item.itemRegistry.getNameForObject(stack.getItem());
        return rawName == null ? null : new ResourceLocation(rawName.toString());
    }

    @Nullable
    public static ResourceLocation resolveBlockRegistryId(@Nullable Block block) {
        if (block == null || block == Blocks.air) {
            return null;
        }

        Object rawName = Block.blockRegistry.getNameForObject(block);
        return rawName == null ? null : new ResourceLocation(rawName.toString());
    }

    @Nullable
    public static ItemStack applyGregTechOreDictUnification(@Nullable ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return stack;
        }

        try {
            Class<?> unificatorClass = Class.forName(GREGTECH_ORE_DICT_UNIFICATOR_CLASS);
            Method setStackMethod = unificatorClass.getMethod("setStack", ItemStack.class);
            Object unifiedStack = setStackMethod.invoke(null, stack.copy());
            return unifiedStack instanceof ItemStack itemStack ? itemStack : stack;
        } catch (Throwable ignored) {
            return stack;
        }
    }

    @Nullable
    public static String trimToNull(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }
}
