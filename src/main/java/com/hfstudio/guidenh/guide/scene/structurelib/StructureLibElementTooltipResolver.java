package com.hfstudio.guidenh.guide.scene.structurelib;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.structurelib.structure.AutoPlaceEnvironment;
import com.gtnewhorizon.structurelib.structure.IItemSource;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.IStructureElementChain;

public class StructureLibElementTooltipResolver {

    public static final int MAX_TIER_SCAN = 50;
    public static final String LAZY_ELEMENT_CLASS_NAME = "com.gtnewhorizon.structurelib.structure.LazyStructureElement";
    public static final IItemSource EMPTY_ITEM_SOURCE = (predicate, simulate, count) -> Collections.emptyMap();

    private final HatchSupport hatchSupport;

    public StructureLibElementTooltipResolver() {
        this(ReflectiveGregTechHatchSupport.tryCreate());
    }

    public StructureLibElementTooltipResolver(HatchSupport hatchSupport) {
        this.hatchSupport = hatchSupport;
    }

    public TooltipDetails resolve(Object constructable, IStructureElement<?> element, @Nullable World world, int x,
        int y, int z, ItemStack trigger) {
        return resolve(constructable, element, world, x, y, z, trigger, null);
    }

    public TooltipDetails resolve(Object constructable, IStructureElement<?> element, @Nullable World world, int x,
        int y, int z, ItemStack trigger, @Nullable EntityPlayer actor) {
        if (element == null || trigger == null) {
            return TooltipDetails.empty();
        }

        HatchLeafMatch hatchLeafMatch = findFirstHatchLeaf(
            constructable,
            element,
            world,
            x,
            y,
            z,
            trigger,
            actor,
            new IdentityHashMap<>());
        List<ItemStack> blockCandidates = collectStackCandidatesAcrossTiers(
            constructable,
            cast(element),
            world,
            x,
            y,
            z,
            trigger,
            actor);
        List<ItemStack> hatchCandidates = hatchLeafMatch != null
            ? collectHatchCandidatesAcrossTiers(constructable, hatchLeafMatch.element, world, x, y, z, trigger, actor)
            : Collections.emptyList();
        if (hatchLeafMatch != null && !blockCandidates.isEmpty()) {
            blockCandidates = filterOutHatchCandidates(blockCandidates);
        }
        List<StructureLibHatchDescriptionLine> hatchDescriptionLines = hatchLeafMatch != null
            ? buildHatchDescriptionLines(hatchLeafMatch.details)
            : Collections.emptyList();
        return new TooltipDetails(blockCandidates, hatchDescriptionLines, hatchCandidates);
    }

    private List<ItemStack> collectStackCandidatesAcrossTiers(Object constructable, IStructureElement<Object> element,
        @Nullable World world, int x, int y, int z, ItemStack trigger, @Nullable EntityPlayer actor) {
        Map<String, ItemStack> candidatesByKey = new LinkedHashMap<>();
        String previousFingerprint = null;
        for (int tier = 1; tier <= MAX_TIER_SCAN; tier++) {
            ItemStack tierTrigger = copyTrigger(trigger, tier);
            IStructureElement.BlocksToPlace blocksToPlace = getBlocksToPlace(
                element,
                constructable,
                world,
                x,
                y,
                z,
                tierTrigger,
                actor);
            if (blocksToPlace == null || blocksToPlace.getStacks() == null) {
                break;
            }
            List<ItemStack> currentStacks = normalizeStacks(blocksToPlace.getStacks());
            if (currentStacks.isEmpty()) {
                break;
            }
            String fingerprint = fingerprint(currentStacks);
            if (previousFingerprint != null && previousFingerprint.equals(fingerprint)) {
                break;
            }
            previousFingerprint = fingerprint;
            appendStacks(candidatesByKey, currentStacks);
        }
        return immutableStacks(candidatesByKey);
    }

    private List<ItemStack> collectHatchCandidatesAcrossTiers(Object constructable, IStructureElement<Object> element,
        @Nullable World world, int x, int y, int z, ItemStack trigger, @Nullable EntityPlayer actor) {
        Map<String, ItemStack> candidatesByKey = new LinkedHashMap<>();
        String previousFingerprint = null;
        for (int tier = 1; tier <= MAX_TIER_SCAN; tier++) {
            ItemStack tierTrigger = copyTrigger(trigger, tier);
            List<ItemStack> currentStacks = normalizeStacks(
                hatchSupport.enumerateHatchCandidates(
                    constructable,
                    element,
                    world,
                    x,
                    y,
                    z,
                    tierTrigger,
                    createEnvironment(actor)));
            if (currentStacks.isEmpty()) {
                break;
            }
            String fingerprint = fingerprint(currentStacks);
            if (previousFingerprint != null && previousFingerprint.equals(fingerprint)) {
                break;
            }
            previousFingerprint = fingerprint;
            appendStacks(candidatesByKey, currentStacks);
        }
        return immutableStacks(candidatesByKey);
    }

    private HatchLeafMatch findFirstHatchLeaf(Object constructable, IStructureElement<?> element, @Nullable World world,
        int x, int y, int z, ItemStack trigger, @Nullable EntityPlayer actor,
        IdentityHashMap<IStructureElement<?>, Boolean> visited) {
        if (element == null || visited.put(element, Boolean.TRUE) != null) {
            return null;
        }

        IStructureElement<Object> typedElement = cast(element);
        HatchDetails details = hatchSupport
            .inspectLeaf(constructable, typedElement, world, x, y, z, trigger, createEnvironment(actor));
        if (details != null) {
            return new HatchLeafMatch(typedElement, details);
        }

        if (element instanceof IStructureElementChain<?>chain) {
            for (IStructureElement<?> fallback : chain.fallbacks()) {
                HatchLeafMatch match = findFirstHatchLeaf(
                    constructable,
                    fallback,
                    world,
                    x,
                    y,
                    z,
                    trigger,
                    actor,
                    visited);
                if (match != null) {
                    return match;
                }
            }
        }

        IStructureElement<?> lazyElement = unwrapLazyElement(element, constructable);
        if (lazyElement != null) {
            HatchLeafMatch match = findFirstHatchLeaf(
                constructable,
                lazyElement,
                world,
                x,
                y,
                z,
                trigger,
                actor,
                visited);
            if (match != null) {
                return match;
            }
        }

        for (IStructureElement<?> wrappedElement : unwrapCapturedElements(element)) {
            HatchLeafMatch match = findFirstHatchLeaf(
                constructable,
                wrappedElement,
                world,
                x,
                y,
                z,
                trigger,
                actor,
                visited);
            if (match != null) {
                return match;
            }
        }

        return null;
    }

    @Nullable
    public static IStructureElement<?> unwrapLazyElement(IStructureElement<?> element, Object constructable) {
        if (!LAZY_ELEMENT_CLASS_NAME.equals(
            element.getClass()
                .getName())) {
            return null;
        }
        try {
            Method getter = element.getClass()
                .getMethod("get", Object.class);
            getter.setAccessible(true);
            Object resolved = getter.invoke(element, constructable);
            return resolved instanceof IStructureElement<?>structureElement ? structureElement : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    public static List<IStructureElement<?>> unwrapCapturedElements(IStructureElement<?> element) {
        List<IStructureElement<?>> wrappedElements = new ArrayList<>();
        for (Field field : element.getClass()
            .getDeclaredFields()) {
            if (!IStructureElement.class.isAssignableFrom(field.getType())) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = field.get(element);
                if (value instanceof IStructureElement<?>structureElement && structureElement != element) {
                    wrappedElements.add(structureElement);
                }
            } catch (IllegalAccessException ignored) {}
        }
        return wrappedElements;
    }

    public static IStructureElement.BlocksToPlace getBlocksToPlace(IStructureElement<Object> element,
        Object constructable, @Nullable World world, int x, int y, int z, ItemStack trigger,
        @Nullable EntityPlayer actor) {
        try {
            return element.getBlocksToPlace(constructable, world, x, y, z, trigger, createEnvironment(actor));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public static AutoPlaceEnvironment createEnvironment(@Nullable EntityPlayer actor) {
        return AutoPlaceEnvironment
            .fromLegacy(EMPTY_ITEM_SOURCE, actor, StructureLibElementTooltipResolver::ignoreChat);
    }

    public static void ignoreChat(@Nullable IChatComponent ignored) {}

    public static ItemStack copyTrigger(ItemStack trigger, int tier) {
        ItemStack copied = trigger.copy();
        copied.stackSize = Math.max(1, tier);
        return copied;
    }

    private List<ItemStack> filterOutHatchCandidates(List<ItemStack> candidates) {
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }
        List<ItemStack> filtered = new ArrayList<>(candidates.size());
        for (ItemStack candidate : candidates) {
            if (candidate != null && !hatchSupport.isHatchItem(candidate)) {
                filtered.add(candidate.copy());
            }
        }
        return filtered.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(filtered);
    }

    private List<StructureLibHatchDescriptionLine> buildHatchDescriptionLines(HatchDetails details) {
        List<StructureLibHatchDescriptionLine> lines = new ArrayList<>(2);
        if (details.getHintDot() > 0) {
            lines.add(StructureLibHatchDescriptionLine.hintBlock(details.getHintDot()));
        }
        if (details.getHintText() != null && !details.getHintText()
            .trim()
            .isEmpty()) {
            lines.add(StructureLibHatchDescriptionLine.validHatches(normalizeHatchHintText(details.getHintText())));
        }
        return lines.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(lines);
    }

    public static String normalizeHatchHintText(String hintText) {
        String normalized = hintText.trim();
        if (normalized.isEmpty()) {
            return normalized;
        }
        return normalized.replaceAll("\\bor(?=\\S)", "or ");
    }

    private void appendStacks(Map<String, ItemStack> candidatesByKey, List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            String key = stackKey(stack);
            if (!candidatesByKey.containsKey(key)) {
                candidatesByKey.put(key, stack.copy());
            }
        }
    }

    public static List<ItemStack> normalizeStacks(Iterable<ItemStack> stacks) {
        if (stacks == null) {
            return Collections.emptyList();
        }
        List<ItemStack> normalized = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (stack == null || stack.getItem() == null) {
                continue;
            }
            ItemStack copied = stack.copy();
            copied.stackSize = 1;
            normalized.add(copied);
        }
        return normalized;
    }

    public static String fingerprint(List<ItemStack> stacks) {
        if (stacks.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(stacks.size() * 24);
        for (ItemStack stack : stacks) {
            builder.append(stackKey(stack))
                .append(';');
        }
        return builder.toString();
    }

    public static String stackKey(ItemStack stack) {
        return stack.getItem()
            .getUnlocalizedName() + '@'
            + stack.getItemDamage();
    }

    public static List<ItemStack> immutableStacks(Map<String, ItemStack> candidatesByKey) {
        if (candidatesByKey.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(candidatesByKey.values()));
    }

    @SuppressWarnings("unchecked")
    public static IStructureElement<Object> cast(IStructureElement<?> element) {
        return (IStructureElement<Object>) element;
    }

    public interface HatchSupport {

        @Nullable
        HatchDetails inspectLeaf(Object constructable, IStructureElement<Object> element, @Nullable World world, int x,
            int y, int z, ItemStack trigger, AutoPlaceEnvironment environment);

        List<ItemStack> enumerateHatchCandidates(Object constructable, IStructureElement<Object> element,
            @Nullable World world, int x, int y, int z, ItemStack trigger, AutoPlaceEnvironment environment);

        boolean isHatchItem(ItemStack stack);
    }

    public static class HatchDetails {

        private final int hintDot;
        @Nullable
        private final String hintText;

        public HatchDetails(int hintDot, @Nullable String hintText) {
            this.hintDot = hintDot;
            this.hintText = hintText;
        }

        public int getHintDot() {
            return hintDot;
        }

        @Nullable
        public String getHintText() {
            return hintText;
        }
    }

    public static class TooltipDetails {

        public static final TooltipDetails EMPTY = new TooltipDetails(
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList());

        private final List<ItemStack> blockCandidates;
        private final List<StructureLibHatchDescriptionLine> hatchDescriptionLines;
        private final List<ItemStack> hatchCandidates;

        public TooltipDetails(List<ItemStack> blockCandidates,
            List<StructureLibHatchDescriptionLine> hatchDescriptionLines, List<ItemStack> hatchCandidates) {
            this.blockCandidates = blockCandidates;
            this.hatchDescriptionLines = hatchDescriptionLines;
            this.hatchCandidates = hatchCandidates;
        }

        public static TooltipDetails empty() {
            return EMPTY;
        }

        public List<ItemStack> getBlockCandidates() {
            return blockCandidates;
        }

        public List<StructureLibHatchDescriptionLine> getHatchDescriptionLines() {
            return hatchDescriptionLines;
        }

        public List<ItemStack> getHatchCandidates() {
            return hatchCandidates;
        }
    }

    public static class HatchLeafMatch {

        private final IStructureElement<Object> element;
        private final HatchDetails details;

        public HatchLeafMatch(IStructureElement<Object> element, HatchDetails details) {
            this.element = element;
            this.details = details;
        }
    }

    public static class ReflectiveGregTechHatchSupport implements HatchSupport {

        public static final HatchSupport NO_OP = new HatchSupport() {

            @Override
            public HatchDetails inspectLeaf(Object constructable, IStructureElement<Object> element, World world, int x,
                int y, int z, ItemStack trigger, AutoPlaceEnvironment environment) {
                return null;
            }

            @Override
            public List<ItemStack> enumerateHatchCandidates(Object constructable, IStructureElement<Object> element,
                World world, int x, int y, int z, ItemStack trigger, AutoPlaceEnvironment environment) {
                return Collections.emptyList();
            }

            @Override
            public boolean isHatchItem(ItemStack stack) {
                return false;
            }
        };

        private final Class<?> hatchBuilderClass;
        private final Class<?> metaTileHatchClass;
        private final Field builderHintField;
        private final Field metaTileEntitiesField;
        private final Field blockMachinesField;
        private final Method itemMachinesGetter;

        public ReflectiveGregTechHatchSupport(Class<?> hatchBuilderClass, Class<?> metaTileHatchClass,
            Field builderHintField, Field metaTileEntitiesField, Field blockMachinesField, Method itemMachinesGetter) {
            this.hatchBuilderClass = hatchBuilderClass;
            this.metaTileHatchClass = metaTileHatchClass;
            this.builderHintField = builderHintField;
            this.metaTileEntitiesField = metaTileEntitiesField;
            this.blockMachinesField = blockMachinesField;
            this.itemMachinesGetter = itemMachinesGetter;
        }

        public static HatchSupport tryCreate() {
            try {
                Class<?> hatchBuilderClass = Class.forName("gregtech.api.util.HatchElementBuilder");
                Class<?> gregTechApiClass = Class.forName("gregtech.api.GregTechAPI");
                Class<?> metaTileHatchClass = Class.forName("gregtech.api.metatileentity.implementations.MTEHatch");
                Class<?> itemMachinesClass = Class.forName("gregtech.common.blocks.ItemMachines");
                Field builderHintField = hatchBuilderClass.getDeclaredField("mHint");
                builderHintField.setAccessible(true);
                Field metaTileEntitiesField = gregTechApiClass.getField("METATILEENTITIES");
                Field blockMachinesField = gregTechApiClass.getField("sBlockMachines");
                Method itemMachinesGetter = itemMachinesClass.getMethod("getMetaTileEntity", ItemStack.class);
                return new ReflectiveGregTechHatchSupport(
                    hatchBuilderClass,
                    metaTileHatchClass,
                    builderHintField,
                    metaTileEntitiesField,
                    blockMachinesField,
                    itemMachinesGetter);
            } catch (ReflectiveOperationException ignored) {
                return NO_OP;
            }
        }

        @Override
        public HatchDetails inspectLeaf(Object constructable, IStructureElement<Object> element, World world, int x,
            int y, int z, ItemStack trigger, AutoPlaceEnvironment environment) {
            Object builder = getBuilder(element);
            if (builder == null) {
                return null;
            }
            try {
                int hintDot = builderHintField.getInt(builder);
                Method getHintMethod = element.getClass()
                    .getDeclaredMethod("getHint");
                getHintMethod.setAccessible(true);
                Object hintText = getHintMethod.invoke(element);
                return hintDot > 0 ? new HatchDetails(hintDot, hintText instanceof String ? (String) hintText : null)
                    : null;
            } catch (ReflectiveOperationException ignored) {
                return null;
            }
        }

        @Override
        public List<ItemStack> enumerateHatchCandidates(Object constructable, IStructureElement<Object> element,
            World world, int x, int y, int z, ItemStack trigger, AutoPlaceEnvironment environment) {
            IStructureElement.BlocksToPlace blocksToPlace = element
                .getBlocksToPlace(constructable, world, x, y, z, trigger, environment);
            if (blocksToPlace == null) {
                return Collections.emptyList();
            }
            Predicate<ItemStack> predicate = blocksToPlace.getPredicate();
            if (predicate == null) {
                return Collections.emptyList();
            }
            try {
                Object metaTileEntities = metaTileEntitiesField.get(null);
                if (!(metaTileEntities instanceof Object[]metaTileArray)) {
                    return Collections.emptyList();
                }
                Object blockObject = blockMachinesField.get(null);
                if (!(blockObject instanceof Block blockMachines)) {
                    return Collections.emptyList();
                }
                List<ItemStack> candidates = new ArrayList<>();
                for (int meta = 1; meta < metaTileArray.length; meta++) {
                    if (metaTileArray[meta] == null) {
                        continue;
                    }
                    ItemStack stack = new ItemStack(blockMachines, 1, meta);
                    if (predicate.test(stack)) {
                        ItemStack copied = stack.copy();
                        copied.stackSize = 1;
                        candidates.add(copied);
                    }
                }
                return candidates;
            } catch (IllegalAccessException ignored) {
                return Collections.emptyList();
            }
        }

        @Override
        public boolean isHatchItem(ItemStack stack) {
            if (stack == null || stack.getItem() == null) {
                return false;
            }
            try {
                Object metaTileEntity = itemMachinesGetter.invoke(null, stack);
                return metaTileEntity != null && metaTileHatchClass.isInstance(metaTileEntity);
            } catch (ReflectiveOperationException ignored) {
                return false;
            }
        }

        @Nullable
        private Object getBuilder(IStructureElement<Object> element) {
            for (Field field : element.getClass()
                .getDeclaredFields()) {
                if (!hatchBuilderClass.isAssignableFrom(field.getType())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object builder = field.get(element);
                    if (builder != null) {
                        return builder;
                    }
                } catch (IllegalAccessException ignored) {}
            }
            return null;
        }
    }
}
