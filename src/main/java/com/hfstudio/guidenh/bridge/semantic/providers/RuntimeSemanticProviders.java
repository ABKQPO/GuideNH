package com.hfstudio.guidenh.bridge.semantic.providers;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.oredict.OreDictionary;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.util.data.ItemId;
import com.gtnewhorizon.structurelib.alignment.IAlignment;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.hfstudio.guidenh.bridge.semantic.SemanticCapability;
import com.hfstudio.guidenh.bridge.semantic.SemanticProvider;
import com.hfstudio.guidenh.bridge.semantic.SemanticProviderRegistry;
import com.hfstudio.guidenh.bridge.semantic.SemanticQuery;
import com.hfstudio.guidenh.bridge.semantic.SemanticQueryResult;
import com.hfstudio.guidenh.client.command.GuideNhClientCommand;
import com.hfstudio.guidenh.guide.compiler.FrontmatterNavigation;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.compiler.tags.KeyBindTagCompiler;
import com.hfstudio.guidenh.guide.indices.ItemIndex;
import com.hfstudio.guidenh.guide.internal.GuideCommand;
import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.integration.structurelib.StructureLibImportRequest;
import com.hfstudio.guidenh.integration.structurelib.StructureLibRuntimeFacade;
import com.hfstudio.structurelibexport.StructureExportCommand;
import com.hfstudio.structurelibexport.StructureLibControllerDiscovery;
import com.hfstudio.structurelibexport.StructureLibControllerSpec;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;

public class RuntimeSemanticProviders {

    public static void registerBaseline(SemanticProviderRegistry registry) {
        SemanticProvider itemsProvider = createItemsProvider();
        registry.register(itemsProvider);
        registry.register(new AliasSemanticProvider(SemanticCapability.RECIPES, itemsProvider));
        registry.register(createPagesProvider());
        registry.register(createOresProvider());
        registry.register(createCategoriesProvider());
        registry.register(createModsProvider());
        registry.register(createCommandsProvider());
        registry.register(createSoundsProvider());
        registry.register(createKeybindsProvider());
        registry.register(createQuestsProvider());
        registry.register(createStructureLibProvider());
    }

    private static SemanticProvider createItemsProvider() {
        return new AbstractCollectionSemanticProvider(SemanticCapability.ITEMS) {

            @Override
            protected List<Map<String, String>> loadEntries() {
                List<Map<String, String>> entries = new ArrayList<>();
                addItemEntries(entries);
                addBlockOnlyEntries(entries);
                return entries;
            }
        };
    }

    private static SemanticProvider createPagesProvider() {
        return new AbstractCollectionSemanticProvider(SemanticCapability.PAGES) {

            @Override
            protected List<Map<String, String>> loadEntries() {
                Map<String, Map<String, String>> entriesById = new LinkedHashMap<>();
                for (ParsedGuidePage page : getAllParsedPages()) {
                    String pagePath = page.getId()
                        .getResourcePath();
                    String title = resolvePageTitle(page);
                    String detail = resolvePageDetail(page);
                    entriesById.putIfAbsent(pagePath, createEntry(pagePath, title, detail));
                }
                return new ArrayList<>(entriesById.values());
            }
        };
    }

    private static SemanticProvider createOresProvider() {
        return new AbstractCollectionSemanticProvider(SemanticCapability.ORES) {

            @Override
            protected List<Map<String, String>> loadEntries() {
                List<Map<String, String>> entries = new ArrayList<>();
                String[] oreNames = OreDictionary.getOreNames();
                for (String oreName : oreNames) {
                    if (oreName == null || oreName.trim()
                        .isEmpty()) {
                        continue;
                    }
                    int stackCount = OreDictionary.getOres(oreName)
                        .size();
                    entries.add(createEntry(oreName, "Ore Dictionary", "Variants: " + stackCount));
                }
                return entries;
            }
        };
    }

    private static SemanticProvider createCategoriesProvider() {
        return new AbstractCollectionSemanticProvider(SemanticCapability.CATEGORIES) {

            @Override
            protected List<Map<String, String>> loadEntries() {
                Map<String, Integer> counts = new LinkedHashMap<>();
                Map<String, String> firstPageByCategory = new LinkedHashMap<>();
                for (ParsedGuidePage page : getAllParsedPages()) {
                    for (String category : readStringList(page, "categories")) {
                        counts.put(category, counts.getOrDefault(category, Integer.valueOf(0)) + 1);
                        firstPageByCategory.putIfAbsent(
                            category,
                            page.getId()
                                .getResourcePath());
                    }
                }

                List<Map<String, String>> entries = new ArrayList<>();
                for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                    String category = entry.getKey();
                    Integer count = entry.getValue();
                    String detail = firstPageByCategory.get(category);
                    entries.add(createEntry(category, "Referenced by " + count + " page(s)", detail));
                }
                return entries;
            }
        };
    }

    private static SemanticProvider createModsProvider() {
        return new AbstractCollectionSemanticProvider(SemanticCapability.MODS) {

            @Override
            protected List<Map<String, String>> loadEntries() {
                List<Map<String, String>> entries = new ArrayList<>();
                Map<String, ModContainer> indexedMods = Loader.instance()
                    .getIndexedModList();
                for (Map.Entry<String, ModContainer> entry : indexedMods.entrySet()) {
                    String modId = entry.getKey();
                    ModContainer mod = entry.getValue();
                    if (modId == null || modId.trim()
                        .isEmpty() || mod == null) {
                        continue;
                    }
                    String version = trimToNull(mod.getVersion());
                    String detail = version != null ? version : modId;
                    entries.add(createEntry(modId, trimToNull(mod.getName()), detail));
                }
                return entries;
            }
        };
    }

    private static SemanticProvider createCommandsProvider() {
        return new AbstractCollectionSemanticProvider(SemanticCapability.COMMANDS) {

            @Override
            protected List<Map<String, String>> loadEntries() {
                List<Map<String, String>> entries = new ArrayList<>();
                addGuideCommandEntries(entries);
                addGuideNhClientCommandEntries(entries);
                addStructureExportCommandEntries(entries);
                return entries;
            }
        };
    }

    private static SemanticProvider createSoundsProvider() {
        return new AbstractCollectionSemanticProvider(SemanticCapability.SOUNDS) {

            @Override
            protected List<Map<String, String>> loadEntries() {
                SoundHandler soundHandler = resolveSoundHandler();
                if (soundHandler == null) {
                    return emptyEntries();
                }

                Set<String> soundIds = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                collectSoundIds(soundHandler, soundIds);

                List<Map<String, String>> entries = new ArrayList<>();
                for (String soundId : soundIds) {
                    entries.add(createEntry(soundId, "Registered sound", soundId));
                }
                return entries;
            }
        };
    }

    private static SemanticProvider createKeybindsProvider() {
        return new AbstractCollectionSemanticProvider(SemanticCapability.KEYBINDS) {

            @Override
            protected List<Map<String, String>> loadEntries() {
                Minecraft minecraft = Minecraft.getMinecraft();
                if (minecraft == null || minecraft.gameSettings == null || minecraft.gameSettings.keyBindings == null) {
                    return emptyEntries();
                }

                List<Map<String, String>> entries = new ArrayList<>();
                for (KeyBinding keyBinding : minecraft.gameSettings.keyBindings) {
                    if (keyBinding == null) {
                        continue;
                    }
                    String id = trimToNull(keyBinding.getKeyDescription());
                    if (id == null) {
                        continue;
                    }

                    String actionName = localizeKey(id);
                    String bindingName = trimToNull(KeyBindTagCompiler.describeMapping(keyBinding));
                    String categoryName = localizeKey(keyBinding.getKeyCategory());
                    String label = actionName;
                    if (bindingName != null) {
                        label = actionName + " - " + bindingName;
                    }
                    entries.add(createEntry(id, label, categoryName));
                }
                return entries;
            }
        };
    }

    private static SemanticProvider createQuestsProvider() {
        return new AbstractCollectionSemanticProvider(SemanticCapability.QUESTS) {

            @Override
            protected List<Map<String, String>> loadEntries() {
                List<Map<String, String>> entries = new ArrayList<>();
                for (ParsedGuidePage page : getAllParsedPages()) {
                    String pageTitle = resolvePageTitle(page);
                    String pagePath = page.getId()
                        .getResourcePath();
                    for (String questId : readStringList(page, "quest_ids")) {
                        if (!isUuidLike(questId)) {
                            continue;
                        }
                        entries.add(createEntry(questId, pageTitle, pagePath));
                    }
                }
                return entries;
            }
        };
    }

    private static SemanticProvider createStructureLibProvider() {
        return new SemanticProvider() {

            @Override
            public String getCapability() {
                return SemanticCapability.STRUCTURELIB;
            }

            @Override
            public SemanticQueryResult query(SemanticQuery query) {
                List<Map<String, String>> entries = loadStructureLibEntries(query);
                List<Map<String, String>> filteredEntries = filterStructureLibEntries(entries, query.getPrefix());
                int cursor = parseStructureLibCursor(query.getCursor(), filteredEntries.size());
                int limit = query.getLimit() > 0 ? query.getLimit() : filteredEntries.size();
                int end = Math.min(filteredEntries.size(), cursor + limit);
                String nextCursor = end < filteredEntries.size() ? Integer.toString(end) : null;
                return new SemanticQueryResult(
                    SemanticCapability.STRUCTURELIB,
                    computeStructureLibVersion(entries),
                    new ArrayList<>(filteredEntries.subList(cursor, end)),
                    nextCursor);
            }
        };
    }

    private static List<Map<String, String>> loadStructureLibEntries(SemanticQuery query) {
        Map<String, String> filters = query.getFilters();
        String attribute = normalizeStructureLibValue(filters.get("attribute"));
        if ("channel".equals(attribute)) {
            return loadStructureLibChannelEntries(filters);
        }
        if (isStructureLibOrientationAttribute(attribute)) {
            return loadStructureLibOrientationEntries(filters, attribute);
        }
        return loadStructureLibControllerEntries();
    }

    private static List<Map<String, String>> loadStructureLibControllerEntries() {
        List<Map<String, String>> entries = new ArrayList<>();
        for (StructureLibControllerSpec controller : new StructureLibControllerDiscovery().discoverAllControllers()) {
            String id = normalizeStructureLibValue(controller.getControllerArgument());
            if (id == null) {
                continue;
            }

            String label = normalizeStructureLibValue(controller.getDisplayName());
            String detail = controller.getBlockId() + ":" + controller.getMeta();
            entries.add(createStructureLibEntry(id, label, detail));
        }
        return normalizeStructureLibEntries(entries);
    }

    private static List<Map<String, String>> loadStructureLibChannelEntries(Map<String, String> filters) {
        String controller = normalizeStructureLibValue(filters.get("controller"));
        if (controller == null) {
            return Collections.emptyList();
        }
        try {
            StructureLibImportRequest request = new StructureLibImportRequest(controller, null, null, null, null, null);
            StructureLibRuntimeFacade.ResolvedController resolvedController = StructureLibRuntimeFacade
                .resolveController(request);
            StructureLibRuntimeFacade.ControlAnalysis analysis = StructureLibRuntimeFacade
                .analyzeControls(request, resolvedController);
            int maxTier = analysis.getMaxTotalTier();
            if (maxTier <= 0) {
                return Collections.emptyList();
            }
            List<Map<String, String>> entries = new ArrayList<>();
            String detail = describeStructureLibTierRange(controller, analysis);
            for (int value = 1; value <= maxTier; value++) {
                entries.add(createStructureLibEntry(Integer.toString(value), "StructureLib preview tier", detail));
            }
            return normalizeStructureLibEntries(entries);
        } catch (IllegalArgumentException ignored) {
            return Collections.emptyList();
        } catch (Throwable ignored) {
            return Collections.emptyList();
        }
    }

    private static List<Map<String, String>> loadStructureLibOrientationEntries(Map<String, String> filters,
        String attribute) {
        String controller = normalizeStructureLibValue(filters.get("controller"));
        if (controller == null || attribute == null) {
            return Collections.emptyList();
        }
        try {
            StructureLibControllerSpec controllerSpec = StructureLibControllerSpec.parse(controller);
            List<ExtendedFacing> allowedFacings = findStructureLibAllowedFacings(controllerSpec);
            if (allowedFacings.isEmpty()) {
                return Collections.emptyList();
            }
            List<Map<String, String>> entries = switch (attribute) {
                case "facing" -> createStructureLibFacingEntries(controllerSpec, allowedFacings, filters);
                case "rotation" -> createStructureLibRotationEntries(controllerSpec, allowedFacings, filters);
                case "flip" -> createStructureLibFlipEntries(controllerSpec, allowedFacings, filters);
                default -> Collections.emptyList();
            };
            return normalizeStructureLibEntries(entries);
        } catch (IllegalArgumentException ignored) {
            return Collections.emptyList();
        } catch (Throwable ignored) {
            return Collections.emptyList();
        }
    }

    private static List<Map<String, String>> createStructureLibFacingEntries(StructureLibControllerSpec controller,
        List<ExtendedFacing> allowedFacings, Map<String, String> filters) {
        List<Map<String, String>> entries = new ArrayList<>();
        for (ExtendedFacing facing : allowedFacings) {
            if (matchesStructureLibOrientationFilters(facing, filters, "facing")) {
                String value = facing.getDirection()
                    .name()
                    .toLowerCase(Locale.ROOT);
                entries.add(
                    createStructureLibEntry(value, "StructureLib facing", describeStructureLibOrientation(controller)));
            }
        }
        return entries;
    }

    private static List<Map<String, String>> createStructureLibRotationEntries(StructureLibControllerSpec controller,
        List<ExtendedFacing> allowedFacings, Map<String, String> filters) {
        List<Map<String, String>> entries = new ArrayList<>();
        for (ExtendedFacing facing : allowedFacings) {
            if (matchesStructureLibOrientationFilters(facing, filters, "rotation")) {
                String value = facing.getRotation()
                    .getName();
                entries.add(
                    createStructureLibEntry(
                        value,
                        "StructureLib rotation",
                        describeStructureLibOrientation(controller)));
            }
        }
        return entries;
    }

    private static List<Map<String, String>> createStructureLibFlipEntries(StructureLibControllerSpec controller,
        List<ExtendedFacing> allowedFacings, Map<String, String> filters) {
        List<Map<String, String>> entries = new ArrayList<>();
        for (ExtendedFacing facing : allowedFacings) {
            if (matchesStructureLibOrientationFilters(facing, filters, "flip")) {
                String value = facing.getFlip()
                    .getName();
                entries.add(
                    createStructureLibEntry(value, "StructureLib flip", describeStructureLibOrientation(controller)));
            }
        }
        return entries;
    }

    private static List<ExtendedFacing> findStructureLibAllowedFacings(StructureLibControllerSpec controller) {
        List<ExtendedFacing> allowedFacings = new ArrayList<>();
        StructureLibRuntimeFacade.BuildContext context = new StructureLibRuntimeFacade.BuildContext();
        try {
            StructureLibRuntimeFacade.ResolvedController resolvedController = new StructureLibRuntimeFacade.ResolvedController(
                controller.getBlockId(),
                controller.getBlock(),
                controller.getMeta());
            TileEntity tile = StructureLibRuntimeFacade
                .placeControllerDirectly(context.getLevel(), context.getWorld(), resolvedController, new ArrayList<>());
            if (tile == null) {
                return Collections.emptyList();
            }
            IAlignment alignment = StructureLibRuntimeFacade.resolveAlignment(tile);
            if (alignment == null) {
                return Collections.emptyList();
            }
            for (ExtendedFacing facing : ExtendedFacing.VALUES) {
                if (alignment.getAlignmentLimits() != null ? alignment.getAlignmentLimits()
                    .isNewExtendedFacingValid(facing) : alignment.checkedSetExtendedFacing(facing)) {
                    allowedFacings.add(facing);
                }
            }
            return allowedFacings;
        } catch (Throwable ignored) {
            return Collections.emptyList();
        } finally {
            context.clear();
        }
    }

    private static boolean matchesStructureLibOrientationFilters(ExtendedFacing facing, Map<String, String> filters,
        String targetAttribute) {
        return matchesStructureLibOrientationFilterValue(
            facing.getDirection()
                .name()
                .toLowerCase(Locale.ROOT),
            normalizeStructureLibValue(filters.get("facing")),
            targetAttribute,
            "facing")
            && matchesStructureLibOrientationFilterValue(
                facing.getRotation()
                    .getName(),
                normalizeStructureLibValue(filters.get("rotation")),
                targetAttribute,
                "rotation")
            && matchesStructureLibOrientationFilterValue(
                facing.getFlip()
                    .getName(),
                normalizeStructureLibValue(filters.get("flip")),
                targetAttribute,
                "flip");
    }

    private static boolean matchesStructureLibOrientationFilterValue(String actualValue,
        @Nullable String requestedValue, String targetAttribute, String attributeName) {
        if (targetAttribute.equals(attributeName) || requestedValue == null) {
            return true;
        }
        return actualValue.equalsIgnoreCase(requestedValue);
    }

    private static String describeStructureLibOrientation(StructureLibControllerSpec controller) {
        return "Allowed orientation for " + controller.getControllerArgument();
    }

    private static boolean isStructureLibOrientationAttribute(@Nullable String attribute) {
        return "facing".equals(attribute) || "rotation".equals(attribute) || "flip".equals(attribute);
    }

    private static String describeStructureLibTierRange(String controller,
        StructureLibRuntimeFacade.ControlAnalysis analysis) {
        StringBuilder detail = new StringBuilder();
        detail.append("Preview tier for ")
            .append(controller)
            .append(" (max ")
            .append(analysis.getMaxTotalTier())
            .append(')');
        if (analysis.getChannelMaxTierMap()
            .isEmpty()) {
            return detail.toString();
        }

        detail.append(" | Channel caps: ");
        boolean first = true;
        for (Map.Entry<String, Integer> entry : analysis.getChannelMaxTierMap()
            .entrySet()) {
            String channelId = normalizeStructureLibValue(entry.getKey());
            Integer maxValue = entry.getValue();
            if (channelId == null || maxValue == null || maxValue <= 0) {
                continue;
            }
            if (!first) {
                detail.append(", ");
            }
            detail.append(channelId)
                .append('=')
                .append(maxValue);
            first = false;
        }
        return detail.toString();
    }

    private static List<Map<String, String>> normalizeStructureLibEntries(List<Map<String, String>> entries) {
        Map<String, Map<String, String>> deduplicated = new LinkedHashMap<>();
        for (Map<String, String> entry : entries) {
            if (entry == null) {
                continue;
            }
            String id = normalizeStructureLibValue(entry.get("id"));
            if (id == null) {
                continue;
            }
            Map<String, String> normalized = new LinkedHashMap<>();
            normalized.put("id", id);
            String label = normalizeStructureLibValue(entry.get("label"));
            if (label != null) {
                normalized.put("label", label);
            }
            String detail = normalizeStructureLibValue(entry.get("detail"));
            if (detail != null) {
                normalized.put("detail", detail);
            }
            deduplicated.putIfAbsent(id.toLowerCase(Locale.ROOT), normalized);
        }
        List<Map<String, String>> normalizedEntries = new ArrayList<>(deduplicated.values());
        normalizedEntries.sort(
            (left, right) -> left.get("id")
                .compareToIgnoreCase(right.get("id")));
        return normalizedEntries;
    }

    private static List<Map<String, String>> filterStructureLibEntries(List<Map<String, String>> entries,
        String prefix) {
        String normalizedPrefix = prefix == null ? ""
            : prefix.trim()
                .toLowerCase(Locale.ROOT);
        if (normalizedPrefix.isEmpty()) {
            return entries;
        }
        List<Map<String, String>> filteredEntries = new ArrayList<>();
        for (Map<String, String> entry : entries) {
            if (startsWithIgnoreCase(entry.get("id"), normalizedPrefix)
                || startsWithIgnoreCase(entry.get("label"), normalizedPrefix)
                || startsWithIgnoreCase(entry.get("detail"), normalizedPrefix)) {
                filteredEntries.add(entry);
            }
        }
        return filteredEntries;
    }

    private static int parseStructureLibCursor(String cursor, int size) {
        if (cursor == null || cursor.isEmpty()) {
            return 0;
        }
        try {
            return Math.max(0, Math.min(Integer.parseInt(cursor), size));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static int computeStructureLibVersion(List<Map<String, String>> entries) {
        int hash = entries.hashCode();
        if (hash == Integer.MIN_VALUE) {
            return Integer.MAX_VALUE;
        }
        return Math.abs(hash) + 1;
    }

    private static boolean startsWithIgnoreCase(@Nullable String value, String prefix) {
        return value != null && value.toLowerCase(Locale.ROOT)
            .startsWith(prefix);
    }

    private static @Nullable String normalizeStructureLibValue(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static Map<String, String> createStructureLibEntry(String id, @Nullable String label,
        @Nullable String detail) {
        Map<String, String> entry = new LinkedHashMap<>();
        entry.put("id", id);
        if (label != null) {
            entry.put("label", label);
        }
        if (detail != null) {
            entry.put("detail", detail);
        }
        return entry;
    }

    private static void addItemEntries(List<Map<String, String>> entries) {
        for (Object rawItem : Item.itemRegistry) {
            if (!(rawItem instanceof Item item)) {
                continue;
            }
            String baseId = resolveRegistryName(Item.itemRegistry.getNameForObject(item));
            if (baseId == null) {
                continue;
            }

            List<ItemStack> variants = collectItemVariants(item);
            if (variants.isEmpty()) {
                variants.add(new ItemStack(item, 1, 0));
            }

            for (ItemStack stack : variants) {
                if (stack == null || stack.getItem() == null) {
                    continue;
                }
                String insertId = buildInsertId(baseId, stack.getItemDamage());
                String detail = buildDisplayId(baseId, stack.getItemDamage());
                String label = resolveItemLabel(stack, baseId);
                entries.add(entry(insertId, label, detail));
            }
        }
    }

    private static void addBlockOnlyEntries(List<Map<String, String>> entries) {
        for (Object rawBlock : Block.blockRegistry) {
            if (!(rawBlock instanceof Block block) || Item.getItemFromBlock(block) != null) {
                continue;
            }
            String baseId = resolveRegistryName(Block.blockRegistry.getNameForObject(block));
            if (baseId == null) {
                continue;
            }
            String label = resolveBlockLabel(block, baseId);
            entries.add(entry(baseId, label, baseId + ":0"));
        }
    }

    private static void addGuideCommandEntries(List<Map<String, String>> entries) {
        String root = "/" + new GuideCommand().getCommandName();
        entries.add(entry(root, "Open guide command", "Guide command root"));
        entries.add(entry(root + " list", "List guides", "Lists registered guides"));
        entries.add(entry(root + " open", "Open a guide", "Open a guide by id"));
        entries.add(entry(root + " reload", "Reload guides", "Reload guide resources"));
        entries.add(entry(root + " search", "Search guides", "Search guide content"));
        addGuideOpenEntries(entries, root + " open", "Open guide");
    }

    private static void addGuideNhClientCommandEntries(List<Map<String, String>> entries) {
        String root = "/" + new GuideNhClientCommand().getCommandName();
        entries.add(entry(root, "Open client guide command", "GuideNH client command root"));
        for (String subCommand : GuideNhClientCommand.ROOT_SUB_COMMANDS) {
            String command = root + " " + subCommand;
            entries.add(entry(command, formatCommandLabel(subCommand), "GuideNH client command"));
        }
        addGuideOpenEntries(entries, root + " open", "Open guide");
        addGuideOpenEntries(entries, root + " export", "Export guide");
        addCommandOptionEntries(
            entries,
            root + " exportsite",
            GuideNhClientCommand.EXPORT_SITE_FLAGS,
            "Export site option");
        addCommandOptionEntries(
            entries,
            root + " exportstructure",
            GuideNhClientCommand.EXPORT_STRUCTURE_FLAGS,
            "Export structure option");
    }

    private static void addStructureExportCommandEntries(List<Map<String, String>> entries) {
        String root = "/" + new StructureExportCommand().getCommandName();
        entries.add(entry(root, "Export structure", "Structure export command root"));
        for (String subCommand : StructureExportCommand.SUBCOMMANDS) {
            String command = root + " " + subCommand;
            entries.add(entry(command, formatCommandLabel(subCommand), "Structure export subcommand"));
        }
        addCommandOptionEntries(
            entries,
            root + " " + StructureExportCommand.SUBCOMMAND_STRUCTURE_LIB,
            StructureExportCommand.STRUCTURE_LIB_OPTIONS,
            "StructureLib export option");
        addCommandOptionEntries(
            entries,
            root + " " + StructureExportCommand.SUBCOMMAND_GAME_SCENE,
            StructureExportCommand.GAME_SCENE_OPTIONS,
            "Game scene export option");
    }

    private static String buildInsertId(String baseId, int meta) {
        return meta > 0 ? baseId + ":" + meta : baseId;
    }

    private static String buildDisplayId(String baseId, int meta) {
        return baseId + ":" + Math.max(meta, 0);
    }

    private static List<ItemStack> collectItemVariants(Item item) {
        List<ItemStack> variants = new ArrayList<>();
        try {
            item.getSubItems(item, CreativeTabs.tabAllSearch, variants);
        } catch (Throwable ignored) {}

        if (variants.isEmpty()) {
            return variants;
        }

        Map<String, ItemStack> uniqueVariants = new LinkedHashMap<>();
        for (ItemStack variant : variants) {
            if (variant == null || variant.getItem() == null) {
                continue;
            }
            String key = ItemIndex.formatKey(ItemId.createNoCopy(variant.getItem(), variant.getItemDamage(), null));
            uniqueVariants.putIfAbsent(key, variant);
        }
        return new ArrayList<>(uniqueVariants.values());
    }

    private static @Nullable String resolveRegistryName(Object registryName) {
        if (registryName == null) {
            return null;
        }
        String value = registryName.toString();
        return value.isEmpty() ? null : value;
    }

    private static String resolveItemLabel(ItemStack stack, String fallback) {
        try {
            String displayName = stack.getDisplayName();
            if (displayName != null && !displayName.trim()
                .isEmpty()) {
                return displayName;
            }
        } catch (Throwable ignored) {}
        return fallback;
    }

    private static String resolveBlockLabel(Block block, String fallback) {
        try {
            String localizedName = block.getLocalizedName();
            if (localizedName != null && !localizedName.trim()
                .isEmpty()) {
                return localizedName;
            }
        } catch (Throwable ignored) {}
        return fallback;
    }

    private static List<ParsedGuidePage> getAllParsedPages() {
        List<ParsedGuidePage> pages = new ArrayList<>();
        for (MutableGuide guide : GuideRegistry.getAll()) {
            try {
                pages.addAll(guide.getPages());
            } catch (IllegalStateException ignored) {}
        }
        return pages;
    }

    private static void addGuideOpenEntries(List<Map<String, String>> entries, String prefix, String labelPrefix) {
        for (MutableGuide guide : GuideRegistry.getAll()) {
            ResourceLocation guideId = guide.getId();
            if (guideId == null) {
                continue;
            }
            String id = guideId.toString();
            entries.add(entry(prefix + " " + id, labelPrefix + ": " + id, "Guide id"));
        }
    }

    private static void addCommandOptionEntries(List<Map<String, String>> entries, String prefix, String[] options,
        String detail) {
        for (String option : options) {
            if (option == null || option.trim()
                .isEmpty()) {
                continue;
            }
            entries.add(entry(prefix + " " + option, formatCommandLabel(option), detail));
        }
    }

    private static List<String> readStringList(ParsedGuidePage page, String key) {
        Object value = page.getFrontmatter()
            .additionalProperties()
            .get(key);
        if (!(value instanceof List<?>values)) {
            return Collections.emptyList();
        }

        List<String> strings = new ArrayList<>();
        for (Object rawValue : values) {
            if (rawValue instanceof String stringValue) {
                String trimmed = stringValue.trim();
                if (!trimmed.isEmpty()) {
                    strings.add(trimmed);
                }
            }
        }
        return strings;
    }

    private static String resolvePageTitle(ParsedGuidePage page) {
        FrontmatterNavigation navigation = page.getFrontmatter()
            .navigationEntry();
        if (navigation != null && navigation.title() != null
            && !navigation.title()
                .trim()
                .isEmpty()) {
            return navigation.title();
        }
        return page.getId()
            .getResourcePath();
    }

    private static String resolvePageDetail(ParsedGuidePage page) {
        return page.getLanguage() + " - " + page.getSourcePack();
    }

    private static @Nullable SoundHandler resolveSoundHandler() {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft != null ? minecraft.getSoundHandler() : null;
    }

    private static void collectSoundIds(SoundHandler soundHandler, Set<String> soundIds) {
        try {
            for (Field field : getAllFields(soundHandler.getClass())) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                field.setAccessible(true);
                Object value = field.get(soundHandler);
                collectSoundIdsFromValue(value, soundIds, 2);
            }
        } catch (IllegalAccessException ignored) {}
    }

    private static void collectSoundIdsFromValue(@Nullable Object value, Set<String> soundIds, int depth) {
        if (value == null || depth < 0) {
            return;
        }

        if (value instanceof ResourceLocation resourceLocation) {
            soundIds.add(resourceLocation.toString());
            return;
        }

        if (value instanceof Map<?, ?>map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                collectSoundIdsFromValue(entry.getKey(), soundIds, depth - 1);
                collectSoundIdsFromValue(entry.getValue(), soundIds, depth - 1);
            }
            return;
        }

        if (value instanceof Iterable<?>iterable) {
            for (Object element : iterable) {
                collectSoundIdsFromValue(element, soundIds, depth - 1);
            }
            return;
        }

        Class<?> type = value.getClass();
        if (type.isArray()) {
            int length = Array.getLength(value);
            for (int index = 0; index < length; index++) {
                collectSoundIdsFromValue(Array.get(value, index), soundIds, depth - 1);
            }
            return;
        }

        String typeName = type.getName()
            .toLowerCase(Locale.ROOT);
        if (!typeName.contains("sound")) {
            return;
        }

        for (Field field : getAllFields(type)) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            try {
                field.setAccessible(true);
                collectSoundIdsFromValue(field.get(value), soundIds, depth - 1);
            } catch (IllegalAccessException ignored) {}
        }
    }

    private static List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            Collections.addAll(fields, current.getDeclaredFields());
        }
        return fields;
    }

    private static String localizeKey(@Nullable String translationKey) {
        if (translationKey == null || translationKey.trim()
            .isEmpty()) {
            return "";
        }

        try {
            String localized = StatCollector.translateToLocal(translationKey);
            if (localized != null && !localized.equals(translationKey)) {
                return localized;
            }
        } catch (Throwable ignored) {}

        try {
            String localized = I18n.format(translationKey);
            if (localized != null && !localized.equals(translationKey)) {
                return localized;
            }
        } catch (Throwable ignored) {}

        return translationKey;
    }

    private static boolean isUuidLike(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static String formatCommandLabel(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (value.startsWith("--")) {
            return value;
        }

        StringBuilder builder = new StringBuilder();
        char previous = 0;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (index > 0 && Character.isUpperCase(current) && Character.isLowerCase(previous)) {
                builder.append(' ');
            } else if (current == '-' || current == '_') {
                builder.append(' ');
                previous = current;
                continue;
            }
            builder.append(current);
            previous = current;
        }
        if (builder.length() == 0) {
            return value;
        }
        builder.setCharAt(0, Character.toUpperCase(builder.charAt(0)));
        return builder.toString();
    }

    private static Map<String, String> entry(String id, String label, String detail) {
        Map<String, String> entry = new LinkedHashMap<>();
        entry.put("id", id);
        if (label != null && !label.isEmpty()) {
            entry.put("label", label);
        }
        if (detail != null && !detail.isEmpty()) {
            entry.put("detail", detail);
        }
        return entry;
    }
}
