package com.hfstudio.guidenh.guide.compiler;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.integration.api.GuideNhIntegrationRegistry;

public class GuideItemReferenceResolver {

    private GuideItemReferenceResolver() {}

    @Desugar
    public record ResolvedItemReference(ResourceLocation registryId, ItemStack stack) {}

    @Desugar
    public record ResolvedBlockReference(ResourceLocation registryId, Block block, @Nullable ItemStack stack,
        boolean hasExplicitMeta, int explicitMeta) {}

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

        Item item = (Item) Item.itemRegistry.getObject(ref.rawKey());
        if (item == null) {
            return null;
        }

        ItemStack stack = new ItemStack(item, 1, ref.concreteMeta());
        if (ref.nbt() != null) {
            stack.stackTagCompound = (NBTTagCompound) ref.nbt()
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
            return new ResolvedBlockReference(registryId, block, stack, true, stack.getItemDamage());
        }

        String trimmedIdText = trimToNull(idText);
        if (trimmedIdText == null) {
            return null;
        }

        IdUtils.ParsedItemRef ref = IdUtils.parseItemRef(trimmedIdText, defaultNamespace);
        if (ref == null) {
            return null;
        }

        Block block = (Block) Block.blockRegistry.getObject(ref.rawKey());
        if (block == null) {
            return null;
        }

        Item item = Item.getItemFromBlock(block);
        ItemStack stack = item != null ? new ItemStack(item, 1, ref.hasExplicitMeta() ? ref.meta() : 0) : null;
        if (stack != null && ref.nbt() != null) {
            stack.stackTagCompound = (NBTTagCompound) ref.nbt()
                .copy();
        }
        return new ResolvedBlockReference(ref.id(), block, stack, ref.hasExplicitMeta(), ref.meta());
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
        ItemStack normalizedStack = GuideNhIntegrationRegistry.global()
            .normalizeItemStack(copiedStack);
        return normalizedStack != null && normalizedStack.getItem() != null ? normalizedStack : copiedStack;
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
    public static String trimToNull(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }
}
