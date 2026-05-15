package com.hfstudio.guidenh.integration.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.snapshot.PreviewPrepareContributor;
import com.hfstudio.guidenh.guide.scene.support.GuideBlockStatsStackResolver;

public class GuideNhIntegrationRegistry {

    private static final GuideNhIntegrationRegistry GLOBAL = new GuideNhIntegrationRegistry();
    private final Map<String, IntegrationModDescriptor> modDescriptors = new LinkedHashMap<>();
    private final List<ItemStackNormalizationProvider> itemStackNormalizationProviders = new ArrayList<>();
    private final List<BlockDisplayProvider> blockDisplayProviders = new ArrayList<>();
    private final List<BlockDisplayNameProvider> blockDisplayNameProviders = new ArrayList<>();
    private final List<BlockExportIdProvider> blockExportIdProviders = new ArrayList<>();
    private final List<PreviewTileEntityProvider> previewTileEntityProviders = new ArrayList<>();
    private final List<PreviewTileEntityFinalizer> previewTileEntityFinalizers = new ArrayList<>();
    private final List<PreviewPrepareContributor> previewPrepareContributors = new ArrayList<>();
    private final List<GuideBuilderIntegrationHook> guideBuilderIntegrationHooks = new ArrayList<>();
    private final List<TagCompilerProvider> tagCompilerProviders = new ArrayList<>();
    private final List<RawRecipeHandlerProvider> rawRecipeHandlerProviders = new ArrayList<>();
    private final List<RecipeEntryProvider> recipeEntryProviders = new ArrayList<>();
    private final List<RecipeItemTooltipProvider> recipeItemTooltipProviders = new ArrayList<>();
    private final List<RecipeAnimationUpdateProvider> recipeAnimationUpdateProviders = new ArrayList<>();
    private final List<RecipeHandlerMetadataProvider> recipeHandlerMetadataProviders = new ArrayList<>();
    private final List<RecipeHandlerSlotProvider> recipeHandlerSlotProviders = new ArrayList<>();
    private final List<RecipeAvailabilityProvider> recipeAvailabilityProviders = new ArrayList<>();
    private final List<RecipeDrawableRenderProvider> recipeDrawableRenderProviders = new ArrayList<>();
    private final List<RecipeHandlerRenderProvider> recipeHandlerRenderProviders = new ArrayList<>();
    private final List<BlockStatsProvider> blockStatsProviders = new ArrayList<>();
    private final List<GuidebookFakeWorldIntegration> fakeWorldIntegrations = new ArrayList<>();

    public GuideNhIntegrationRegistry() {}

    public static GuideNhIntegrationRegistry global() {
        return GLOBAL;
    }

    public synchronized void registerModDescriptor(IntegrationModDescriptor descriptor) {
        if (descriptor == null) {
            throw new IllegalArgumentException("descriptor");
        }
        if (!modDescriptors.containsKey(descriptor.id())) {
            modDescriptors.put(descriptor.id(), descriptor);
        }
    }

    @Nullable
    public synchronized IntegrationModDescriptor modDescriptor(@Nullable String id) {
        String normalizedId = IntegrationModDescriptor.normalizeIdOrNull(id);
        return normalizedId != null ? modDescriptors.get(normalizedId) : null;
    }

    public synchronized List<IntegrationModDescriptor> modDescriptors() {
        return Collections.unmodifiableList(new ArrayList<>(modDescriptors.values()));
    }

    public synchronized void registerItemStackNormalizationProvider(ItemStackNormalizationProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider");
        }
        if (!itemStackNormalizationProviders.contains(provider)) {
            itemStackNormalizationProviders.add(provider);
        }
    }

    public synchronized List<ItemStackNormalizationProvider> itemStackNormalizationProviders() {
        return Collections.unmodifiableList(new ArrayList<>(itemStackNormalizationProviders));
    }

    public synchronized void registerBlockDisplayProvider(BlockDisplayProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider");
        }
        if (!blockDisplayProviders.contains(provider)) {
            blockDisplayProviders.add(provider);
        }
    }

    public synchronized List<BlockDisplayProvider> blockDisplayProviders() {
        return Collections.unmodifiableList(new ArrayList<>(blockDisplayProviders));
    }

    public synchronized void registerBlockDisplayNameProvider(BlockDisplayNameProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider");
        }
        if (!blockDisplayNameProviders.contains(provider)) {
            blockDisplayNameProviders.add(provider);
        }
    }

    public synchronized List<BlockDisplayNameProvider> blockDisplayNameProviders() {
        return Collections.unmodifiableList(new ArrayList<>(blockDisplayNameProviders));
    }

    public synchronized void registerBlockExportIdProvider(BlockExportIdProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider");
        }
        if (!blockExportIdProviders.contains(provider)) {
            blockExportIdProviders.add(provider);
        }
    }

    public synchronized List<BlockExportIdProvider> blockExportIdProviders() {
        return Collections.unmodifiableList(new ArrayList<>(blockExportIdProviders));
    }

    public synchronized void registerPreviewTileEntityProvider(PreviewTileEntityProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider");
        }
        if (!previewTileEntityProviders.contains(provider)) {
            previewTileEntityProviders.add(provider);
        }
    }

    public synchronized List<PreviewTileEntityProvider> previewTileEntityProviders() {
        return Collections.unmodifiableList(new ArrayList<>(previewTileEntityProviders));
    }

    public synchronized void registerPreviewTileEntityFinalizer(PreviewTileEntityFinalizer finalizer) {
        if (finalizer == null) {
            throw new IllegalArgumentException("finalizer");
        }
        if (!previewTileEntityFinalizers.contains(finalizer)) {
            previewTileEntityFinalizers.add(finalizer);
        }
    }

    public synchronized List<PreviewTileEntityFinalizer> previewTileEntityFinalizers() {
        return Collections.unmodifiableList(new ArrayList<>(previewTileEntityFinalizers));
    }

    public synchronized void registerPreviewPrepareContributor(PreviewPrepareContributor contributor) {
        if (contributor == null) {
            throw new IllegalArgumentException("contributor");
        }
        if (!previewPrepareContributors.contains(contributor)) {
            previewPrepareContributors.add(contributor);
        }
    }

    public synchronized List<PreviewPrepareContributor> previewPrepareContributors() {
        return Collections.unmodifiableList(new ArrayList<>(previewPrepareContributors));
    }

    public synchronized void registerGuideBuilderIntegrationHook(GuideBuilderIntegrationHook hook) {
        if (hook == null) {
            throw new IllegalArgumentException("hook");
        }
        if (!guideBuilderIntegrationHooks.contains(hook)) {
            guideBuilderIntegrationHooks.add(hook);
        }
    }

    public synchronized List<GuideBuilderIntegrationHook> guideBuilderIntegrationHooks() {
        return Collections.unmodifiableList(new ArrayList<>(guideBuilderIntegrationHooks));
    }

    public synchronized void registerTagCompilerProvider(TagCompilerProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider");
        }
        if (!tagCompilerProviders.contains(provider)) {
            tagCompilerProviders.add(provider);
        }
    }

    public synchronized List<TagCompilerProvider> tagCompilerProviders() {
        return Collections.unmodifiableList(new ArrayList<>(tagCompilerProviders));
    }

    public synchronized void registerRawRecipeHandlerProvider(RawRecipeHandlerProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider");
        }
        if (!rawRecipeHandlerProviders.contains(provider)) {
            rawRecipeHandlerProviders.add(provider);
        }
    }

    public synchronized List<RawRecipeHandlerProvider> rawRecipeHandlerProviders() {
        return Collections.unmodifiableList(new ArrayList<>(rawRecipeHandlerProviders));
    }

    public synchronized void registerRecipeEntryProvider(RecipeEntryProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider");
        }
        if (!recipeEntryProviders.contains(provider)) {
            recipeEntryProviders.add(provider);
        }
    }

    public synchronized List<RecipeEntryProvider> recipeEntryProviders() {
        return Collections.unmodifiableList(new ArrayList<>(recipeEntryProviders));
    }

    public synchronized void registerRecipeItemTooltipProvider(RecipeItemTooltipProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider");
        }
        if (!recipeItemTooltipProviders.contains(provider)) {
            recipeItemTooltipProviders.add(provider);
        }
    }

    public synchronized List<RecipeItemTooltipProvider> recipeItemTooltipProviders() {
        return Collections.unmodifiableList(new ArrayList<>(recipeItemTooltipProviders));
    }

    public synchronized void registerRecipeAnimationUpdateProvider(RecipeAnimationUpdateProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider");
        }
        if (!recipeAnimationUpdateProviders.contains(provider)) {
            recipeAnimationUpdateProviders.add(provider);
        }
    }

    public synchronized List<RecipeAnimationUpdateProvider> recipeAnimationUpdateProviders() {
        return Collections.unmodifiableList(new ArrayList<>(recipeAnimationUpdateProviders));
    }

    public synchronized void registerRecipeHandlerMetadataProvider(RecipeHandlerMetadataProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider");
        }
        if (!recipeHandlerMetadataProviders.contains(provider)) {
            recipeHandlerMetadataProviders.add(provider);
        }
    }

    public synchronized List<RecipeHandlerMetadataProvider> recipeHandlerMetadataProviders() {
        return Collections.unmodifiableList(new ArrayList<>(recipeHandlerMetadataProviders));
    }

    public synchronized void registerRecipeHandlerSlotProvider(RecipeHandlerSlotProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider");
        }
        if (!recipeHandlerSlotProviders.contains(provider)) {
            recipeHandlerSlotProviders.add(provider);
        }
    }

    public synchronized List<RecipeHandlerSlotProvider> recipeHandlerSlotProviders() {
        return Collections.unmodifiableList(new ArrayList<>(recipeHandlerSlotProviders));
    }

    public synchronized void registerRecipeAvailabilityProvider(RecipeAvailabilityProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider");
        }
        if (!recipeAvailabilityProviders.contains(provider)) {
            recipeAvailabilityProviders.add(provider);
        }
    }

    public synchronized List<RecipeAvailabilityProvider> recipeAvailabilityProviders() {
        return Collections.unmodifiableList(new ArrayList<>(recipeAvailabilityProviders));
    }

    public synchronized void registerRecipeDrawableRenderProvider(RecipeDrawableRenderProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider");
        }
        if (!recipeDrawableRenderProviders.contains(provider)) {
            recipeDrawableRenderProviders.add(provider);
        }
    }

    public synchronized List<RecipeDrawableRenderProvider> recipeDrawableRenderProviders() {
        return Collections.unmodifiableList(new ArrayList<>(recipeDrawableRenderProviders));
    }

    public synchronized void registerRecipeHandlerRenderProvider(RecipeHandlerRenderProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider");
        }
        if (!recipeHandlerRenderProviders.contains(provider)) {
            recipeHandlerRenderProviders.add(provider);
        }
    }

    public synchronized List<RecipeHandlerRenderProvider> recipeHandlerRenderProviders() {
        return Collections.unmodifiableList(new ArrayList<>(recipeHandlerRenderProviders));
    }

    public List<Object> queryRawCraftingHandlers(@Nullable ItemStack target) {
        if (target == null) {
            return Collections.emptyList();
        }
        for (RawRecipeHandlerProvider provider : rawRecipeHandlerProviders()) {
            List<Object> handlers = provider.queryRawCraftingHandlers(target);
            if (handlers != null && !handlers.isEmpty()) {
                return handlers;
            }
        }
        return Collections.emptyList();
    }

    public List<Object> queryRawUsageHandlers(@Nullable ItemStack target) {
        if (target == null) {
            return Collections.emptyList();
        }
        for (RawRecipeHandlerProvider provider : rawRecipeHandlerProviders()) {
            List<Object> handlers = provider.queryRawUsageHandlers(target);
            if (handlers != null && !handlers.isEmpty()) {
                return handlers;
            }
        }
        return Collections.emptyList();
    }

    public List<RecipeEntry> findCraftingRecipeEntries(@Nullable ItemStack target) {
        if (target == null) {
            return Collections.emptyList();
        }
        for (RecipeEntryProvider provider : recipeEntryProviders()) {
            List<RecipeEntry> entries = provider.findCraftingRecipeEntries(target);
            if (entries != null && !entries.isEmpty()) {
                return entries;
            }
        }
        return Collections.emptyList();
    }

    public void appendRecipeItemTooltip(@Nullable Object handler, @Nullable ItemStack stack,
        @Nullable List<String> tooltip, int recipeIndex) {
        if (handler == null || stack == null || tooltip == null) {
            return;
        }
        for (RecipeItemTooltipProvider provider : recipeItemTooltipProviders()) {
            provider.appendTooltip(handler, stack, tooltip, recipeIndex);
        }
    }

    public boolean canUpdateRecipeAnimation(@Nullable Object handler) {
        if (handler == null) {
            return false;
        }
        for (RecipeAnimationUpdateProvider provider : recipeAnimationUpdateProviders()) {
            if (provider.canUpdateRecipeAnimation(handler)) {
                return true;
            }
        }
        return false;
    }

    public void updateRecipeAnimation(@Nullable Object handler) {
        if (handler == null) {
            return;
        }
        for (RecipeAnimationUpdateProvider provider : recipeAnimationUpdateProviders()) {
            provider.updateRecipeAnimation(handler);
        }
    }

    public boolean isRecipeIntegrationAvailable() {
        for (RecipeAvailabilityProvider provider : recipeAvailabilityProviders()) {
            if (provider.isRecipeIntegrationAvailable()) {
                return true;
            }
        }
        return false;
    }

    public boolean drawRecipeDrawable(@Nullable Object drawable, int x, int y) {
        if (drawable == null) {
            return false;
        }
        for (RecipeDrawableRenderProvider provider : recipeDrawableRenderProviders()) {
            if (provider.drawDrawable(drawable, x, y)) {
                return true;
            }
        }
        return false;
    }

    public boolean canRenderRecipeHandler(@Nullable Object handler) {
        if (handler == null) {
            return false;
        }
        for (RecipeHandlerRenderProvider provider : recipeHandlerRenderProviders()) {
            if (provider.canRenderRecipeHandler(handler)) {
                return true;
            }
        }
        return false;
    }

    public void renderRecipeHandler(@Nullable Object handler, int recipeIndex, boolean skipForeground) {
        if (handler == null) {
            return;
        }
        for (RecipeHandlerRenderProvider provider : recipeHandlerRenderProviders()) {
            if (provider.canRenderRecipeHandler(handler)) {
                provider.renderRecipeHandler(handler, recipeIndex, skipForeground);
            }
        }
    }

    public String lookupRecipeHandlerName(@Nullable Object handler) {
        if (handler == null) {
            return "";
        }
        for (RecipeHandlerMetadataProvider provider : recipeHandlerMetadataProviders()) {
            String name = provider.lookupHandlerName(handler);
            if (name != null) {
                return name;
            }
        }
        return "";
    }

    @Nullable
    public ItemStack lookupRecipeHandlerIcon(@Nullable Object handler) {
        if (handler == null) {
            return null;
        }
        for (RecipeHandlerMetadataProvider provider : recipeHandlerMetadataProviders()) {
            ItemStack icon = provider.lookupHandlerIcon(handler);
            if (icon != null) {
                return icon;
            }
        }
        return null;
    }

    @Nullable
    public Object lookupRecipeHandlerImage(@Nullable Object handler) {
        if (handler == null) {
            return null;
        }
        for (RecipeHandlerMetadataProvider provider : recipeHandlerMetadataProviders()) {
            Object image = provider.lookupHandlerImage(handler);
            if (image != null) {
                return image;
            }
        }
        return null;
    }

    public int lookupRecipeHandlerWidth(@Nullable Object handler) {
        if (handler == null) {
            return 166;
        }
        for (RecipeHandlerMetadataProvider provider : recipeHandlerMetadataProviders()) {
            Integer width = provider.lookupHandlerWidth(handler);
            if (width != null) {
                return width;
            }
        }
        return 166;
    }

    public int lookupRecipeHandlerHeight(@Nullable Object handler) {
        if (handler == null) {
            return 65;
        }
        for (RecipeHandlerMetadataProvider provider : recipeHandlerMetadataProviders()) {
            Integer height = provider.lookupHandlerHeight(handler);
            if (height != null) {
                return height;
            }
        }
        return 65;
    }

    public int lookupRecipeHeight(@Nullable Object handler, int recipeIndex) {
        if (handler == null) {
            return 0;
        }
        for (RecipeHandlerMetadataProvider provider : recipeHandlerMetadataProviders()) {
            Integer height = provider.lookupRecipeHeight(handler, recipeIndex);
            if (height != null) {
                return height;
            }
        }
        return 0;
    }

    public int lookupRecipeHandlerYShift(@Nullable Object handler) {
        if (handler == null) {
            return 0;
        }
        for (RecipeHandlerMetadataProvider provider : recipeHandlerMetadataProviders()) {
            Integer shift = provider.lookupHandlerYShift(handler);
            if (shift != null) {
                return shift;
            }
        }
        return 0;
    }

    public String lookupRecipeHandlerOverlayIdentifier(@Nullable Object handler) {
        if (handler == null) {
            return "";
        }
        for (RecipeHandlerMetadataProvider provider : recipeHandlerMetadataProviders()) {
            String overlayIdentifier = provider.lookupHandlerOverlayIdentifier(handler);
            if (overlayIdentifier != null) {
                return overlayIdentifier;
            }
        }
        return "";
    }

    public String lookupRecipeHandlerId(@Nullable Object handler) {
        if (handler == null) {
            return "";
        }
        for (RecipeHandlerMetadataProvider provider : recipeHandlerMetadataProviders()) {
            String id = provider.lookupHandlerId(handler);
            if (id != null) {
                return id;
            }
        }
        return "";
    }

    public int lookupRecipeHandlerRecipeCount(@Nullable Object handler) {
        if (handler == null) {
            return 0;
        }
        for (RecipeHandlerMetadataProvider provider : recipeHandlerMetadataProviders()) {
            Integer recipeCount = provider.lookupRecipeCount(handler);
            if (recipeCount != null) {
                return recipeCount;
            }
        }
        return 0;
    }

    public int lookupRecipeDrawableWidth(@Nullable Object drawable) {
        if (drawable == null) {
            return 0;
        }
        for (RecipeHandlerMetadataProvider provider : recipeHandlerMetadataProviders()) {
            Integer width = provider.lookupDrawableWidth(drawable);
            if (width != null) {
                return width;
            }
        }
        return 0;
    }

    public int lookupRecipeDrawableHeight(@Nullable Object drawable) {
        if (drawable == null) {
            return 0;
        }
        for (RecipeHandlerMetadataProvider provider : recipeHandlerMetadataProviders()) {
            Integer height = provider.lookupDrawableHeight(drawable);
            if (height != null) {
                return height;
            }
        }
        return 0;
    }

    public boolean recipeOtherStacksThrows(@Nullable Object handler, int recipeIndex) {
        if (handler == null) {
            return false;
        }
        for (RecipeHandlerMetadataProvider provider : recipeHandlerMetadataProviders()) {
            Boolean broken = provider.otherStacksThrows(handler, recipeIndex);
            if (broken != null) {
                return broken;
            }
        }
        return false;
    }

    public List<RecipeSlot> readRecipeIngredientSlots(@Nullable Object handler, int recipeIndex) {
        if (handler == null) {
            return Collections.emptyList();
        }
        for (RecipeHandlerSlotProvider provider : recipeHandlerSlotProviders()) {
            List<RecipeSlot> slots = provider.readIngredientSlots(handler, recipeIndex);
            if (slots != null) {
                return slots;
            }
        }
        return Collections.emptyList();
    }

    public List<RecipeSlot> readRecipeOtherSlots(@Nullable Object handler, int recipeIndex) {
        if (handler == null) {
            return Collections.emptyList();
        }
        for (RecipeHandlerSlotProvider provider : recipeHandlerSlotProviders()) {
            List<RecipeSlot> slots = provider.readOtherSlots(handler, recipeIndex);
            if (slots != null) {
                return slots;
            }
        }
        return Collections.emptyList();
    }

    @Nullable
    public RecipeSlot readRecipeResultSlot(@Nullable Object handler, int recipeIndex) {
        if (handler == null) {
            return null;
        }
        for (RecipeHandlerSlotProvider provider : recipeHandlerSlotProviders()) {
            RecipeSlot slot = provider.readResultSlot(handler, recipeIndex);
            if (slot != null) {
                return slot;
            }
        }
        return null;
    }

    public synchronized void registerBlockStatsProvider(BlockStatsProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider");
        }
        if (!blockStatsProviders.contains(provider)) {
            blockStatsProviders.add(provider);
        }
    }

    public synchronized List<BlockStatsProvider> blockStatsProviders() {
        return Collections.unmodifiableList(new ArrayList<>(blockStatsProviders));
    }

    public synchronized void registerFakeWorldIntegration(GuidebookFakeWorldIntegration integration) {
        if (integration == null) {
            throw new IllegalArgumentException("integration");
        }
        if (!fakeWorldIntegrations.contains(integration)) {
            fakeWorldIntegrations.add(integration);
        }
    }

    public synchronized List<GuidebookFakeWorldIntegration> fakeWorldIntegrations() {
        return Collections.unmodifiableList(new ArrayList<>(fakeWorldIntegrations));
    }

    public void registerDummyWorldIntegrations(Class<?> worldClass) {
        if (worldClass == null) {
            return;
        }
        for (GuidebookFakeWorldIntegration integration : fakeWorldIntegrations()) {
            integration.registerDummyWorld(worldClass);
        }
    }

    public boolean suppressMarkBlockForUpdateDescriptionResync(@Nullable TileEntity tileEntity,
        @Nullable GuidebookLevel level) {
        if (tileEntity == null || level == null) {
            return false;
        }
        for (GuidebookFakeWorldIntegration integration : fakeWorldIntegrations()) {
            if (integration.suppressMarkBlockForUpdateDescriptionResync(tileEntity, level)) {
                return true;
            }
        }
        return false;
    }

    public GuidebookFakeWorldIntegration.FakeWorldCreationScope openFakeWorldCreationScope() {
        ArrayList<GuidebookFakeWorldIntegration.FakeWorldCreationScope> scopes = new ArrayList<>();
        for (GuidebookFakeWorldIntegration integration : fakeWorldIntegrations()) {
            GuidebookFakeWorldIntegration.FakeWorldCreationScope scope = integration.openFakeWorldCreationScope();
            if (scope != null) {
                scopes.add(scope);
            }
        }
        return () -> closeReverse(scopes);
    }

    public static void closeReverse(List<GuidebookFakeWorldIntegration.FakeWorldCreationScope> scopes) {
        RuntimeException failure = null;
        for (int index = scopes.size() - 1; index >= 0; index--) {
            try {
                scopes.get(index)
                    .close();
            } catch (RuntimeException e) {
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    public List<GuideBlockStatsStackResolver.ResolvedStack> resolveBlockStatsEntries(GuidebookLevel level, Block block,
        @Nullable TileEntity tileEntity, int x, int y, int z, @Nullable AxisAlignedBB fallbackBounds) {
        if (level == null || block == null) {
            return Collections.emptyList();
        }
        ArrayList<GuideBlockStatsStackResolver.ResolvedStack> entries = new ArrayList<>(4);
        for (BlockStatsProvider provider : blockStatsProviders()) {
            provider.appendBlockStatsEntries(level, block, tileEntity, x, y, z, fallbackBounds, entries);
            if (!entries.isEmpty()) {
                return entries;
            }
        }
        return Collections.emptyList();
    }

    @Nullable
    public ItemStack normalizeItemStack(@Nullable ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return stack;
        }
        ItemStack current = stack;
        for (ItemStackNormalizationProvider provider : itemStackNormalizationProviders()) {
            ItemStack normalized = provider.normalize(current);
            if (normalized != null && normalized.getItem() != null) {
                current = normalized;
            }
        }
        return current;
    }

    @Nullable
    public ItemStack resolveBlockDisplayStack(GuidebookLevel level, Block block, int x, int y, int z,
        @Nullable MovingObjectPosition target) {
        if (level == null || block == null) {
            return null;
        }
        for (BlockDisplayProvider provider : blockDisplayProviders()) {
            ItemStack stack = provider.resolveDisplayStack(level, block, x, y, z, target);
            if (stack != null) {
                return stack;
            }
        }
        return null;
    }

    @Nullable
    public String resolveBlockDisplayName(GuidebookLevel level, Block block, int x, int y, int z,
        @Nullable MovingObjectPosition target) {
        if (level == null || block == null) {
            return null;
        }
        for (BlockDisplayNameProvider provider : blockDisplayNameProviders()) {
            String displayName = provider.resolveDisplayName(level, block, x, y, z, target);
            if (displayName != null) {
                return displayName;
            }
        }
        return null;
    }

    @Nullable
    public String resolveBlockExportId(GuidebookLevel level, Block block, @Nullable TileEntity tileEntity, int x, int y,
        int z, @Nullable String fallbackId) {
        if (level == null || block == null) {
            return fallbackId;
        }
        for (BlockExportIdProvider provider : blockExportIdProviders()) {
            String exportId = provider.resolveExportId(level, block, tileEntity, x, y, z, fallbackId);
            if (exportId != null) {
                return exportId;
            }
        }
        return fallbackId;
    }

    @Nullable
    public TileEntity loadPreviewTileEntity(@Nullable World world, Block block, int meta, int x, int y, int z,
        @Nullable NBTTagCompound tag) {
        if (block == null) {
            return null;
        }
        for (PreviewTileEntityProvider provider : previewTileEntityProviders()) {
            TileEntity tileEntity = provider.loadPreviewTile(world, block, meta, x, y, z, tag);
            if (tileEntity != null) {
                return tileEntity;
            }
        }
        return null;
    }

    @Nullable
    public TileEntity finalizePreviewTileEntity(GuidebookLevel level, int x, int y, int z,
        @Nullable TileEntity tileEntity) {
        if (level == null || tileEntity == null) {
            return tileEntity;
        }
        for (PreviewTileEntityFinalizer finalizer : previewTileEntityFinalizers()) {
            TileEntity finalizedTile = finalizer.finalizePreviewTile(level, x, y, z, tileEntity);
            if (finalizedTile != null) {
                return finalizedTile;
            }
        }
        return tileEntity;
    }
}
