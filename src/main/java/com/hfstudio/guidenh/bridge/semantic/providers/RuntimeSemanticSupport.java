package com.hfstudio.guidenh.bridge.semantic.providers;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
import net.minecraft.command.CommandClearInventory;
import net.minecraft.command.CommandDebug;
import net.minecraft.command.CommandDefaultGameMode;
import net.minecraft.command.CommandDifficulty;
import net.minecraft.command.CommandEffect;
import net.minecraft.command.CommandEnchant;
import net.minecraft.command.CommandGameMode;
import net.minecraft.command.CommandGameRule;
import net.minecraft.command.CommandGive;
import net.minecraft.command.CommandHandler;
import net.minecraft.command.CommandHelp;
import net.minecraft.command.CommandKill;
import net.minecraft.command.CommandPlaySound;
import net.minecraft.command.CommandServerKick;
import net.minecraft.command.CommandSetPlayerTimeout;
import net.minecraft.command.CommandSetSpawnpoint;
import net.minecraft.command.CommandShowSeed;
import net.minecraft.command.CommandSpreadPlayers;
import net.minecraft.command.CommandTime;
import net.minecraft.command.CommandToggleDownfall;
import net.minecraft.command.CommandWeather;
import net.minecraft.command.CommandXP;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandManager;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.server.CommandAchievement;
import net.minecraft.command.server.CommandBanIp;
import net.minecraft.command.server.CommandBanPlayer;
import net.minecraft.command.server.CommandBroadcast;
import net.minecraft.command.server.CommandDeOp;
import net.minecraft.command.server.CommandEmote;
import net.minecraft.command.server.CommandListBans;
import net.minecraft.command.server.CommandListPlayers;
import net.minecraft.command.server.CommandMessage;
import net.minecraft.command.server.CommandMessageRaw;
import net.minecraft.command.server.CommandNetstat;
import net.minecraft.command.server.CommandOp;
import net.minecraft.command.server.CommandPardonIp;
import net.minecraft.command.server.CommandPardonPlayer;
import net.minecraft.command.server.CommandPublishLocalServer;
import net.minecraft.command.server.CommandSaveAll;
import net.minecraft.command.server.CommandSaveOff;
import net.minecraft.command.server.CommandSaveOn;
import net.minecraft.command.server.CommandScoreboard;
import net.minecraft.command.server.CommandSetBlock;
import net.minecraft.command.server.CommandSetDefaultSpawnpoint;
import net.minecraft.command.server.CommandStop;
import net.minecraft.command.server.CommandSummon;
import net.minecraft.command.server.CommandTeleport;
import net.minecraft.command.server.CommandTestFor;
import net.minecraft.command.server.CommandTestForBlock;
import net.minecraft.command.server.CommandWhitelist;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.oredict.OreDictionary;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.util.data.ItemId;
import com.hfstudio.guidenh.client.command.GuideNhClientCommand;
import com.hfstudio.guidenh.guide.compiler.FrontmatterNavigation;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.compiler.tags.KeyBindTagCompiler;
import com.hfstudio.guidenh.guide.indices.ItemIndex;
import com.hfstudio.guidenh.guide.internal.GuideCommand;
import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.scene.element.GuidebookSceneEntityLoader;
import com.hfstudio.structurelibexport.StructureExportCommand;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;

public class RuntimeSemanticSupport {

    private RuntimeSemanticSupport() {}

    public static void addItemEntries(List<Map<String, String>> entries) {
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
                entries.add(createEntry(insertId, label, detail));
            }
        }
    }

    public static void addBlockOnlyEntries(List<Map<String, String>> entries) {
        for (Object rawBlock : Block.blockRegistry) {
            if (!(rawBlock instanceof Block block) || Item.getItemFromBlock(block) != null) {
                continue;
            }
            String baseId = resolveRegistryName(Block.blockRegistry.getNameForObject(block));
            if (baseId == null) {
                continue;
            }
            String label = resolveBlockLabel(block, baseId);
            entries.add(createEntry(baseId, label, baseId + ":0"));
        }
    }

    public static void addOreEntries(List<Map<String, String>> entries) {
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
    }

    public static void addCategoryEntries(List<Map<String, String>> entries) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, String> firstPageByCategory = new LinkedHashMap<>();
        for (ParsedGuidePage page : getAllParsedPages()) {
            for (String category : readStringList(page, "categories")) {
                counts.put(category, counts.getOrDefault(category, 0) + 1);
                firstPageByCategory.putIfAbsent(
                    category,
                    page.getId()
                        .getResourcePath());
            }
        }

        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            String category = entry.getKey();
            Integer count = entry.getValue();
            String detail = firstPageByCategory.get(category);
            entries.add(createEntry(category, "Referenced by " + count + " page(s)", detail));
        }
    }

    public static void addModEntries(List<Map<String, String>> entries) {
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
    }

    public static void addGuideCommandEntries(List<Map<String, String>> entries) {
        String root = "/" + new GuideCommand().getCommandName();
        entries.add(createEntry(root, "Open guide command", "Guide command root"));
        entries.add(createEntry(root + " list", "List guides", "Lists registered guides"));
        entries.add(createEntry(root + " open", "Open a guide", "Open a guide by id"));
        entries.add(createEntry(root + " reload", "Reload guides", "Reload guide resources"));
        entries.add(createEntry(root + " search", "Search guides", "Search guide content"));
        addGuideOpenEntries(entries, root + " open", "Open guide");
    }

    public static void addGuideNhClientCommandEntries(List<Map<String, String>> entries) {
        String root = "/" + new GuideNhClientCommand().getCommandName();
        entries.add(createEntry(root, "Open client guide command", "GuideNH client command root"));
        for (String subCommand : GuideNhClientCommand.ROOT_SUB_COMMANDS) {
            String command = root + " " + subCommand;
            entries.add(createEntry(command, formatCommandLabel(subCommand), "GuideNH client command"));
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

    public static void addStructureExportCommandEntries(List<Map<String, String>> entries) {
        String root = "/" + new StructureExportCommand().getCommandName();
        entries.add(createEntry(root, "Export structure", "Structure export command root"));
        for (String subCommand : StructureExportCommand.SUBCOMMANDS) {
            String command = root + " " + subCommand;
            entries.add(createEntry(command, formatCommandLabel(subCommand), "Structure export subcommand"));
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

    public static void addGlobalCommandEntries(List<Map<String, String>> entries) {
        addCommandHandlerEntries(entries, ClientCommandHandler.instance, "Client command");
        MinecraftServer minecraftServer = MinecraftServer.getServer();
        if (minecraftServer == null) {
            addFallbackServerCommandEntries(entries);
            return;
        }
        ICommandManager commandManager = minecraftServer.getCommandManager();
        if (!(commandManager instanceof CommandHandler commandHandler)) {
            addFallbackServerCommandEntries(entries);
            return;
        }
        addCommandHandlerEntries(entries, commandHandler, "Server command");
    }

    public static void addSoundEntries(List<Map<String, String>> entries) {
        SoundHandler soundHandler = resolveSoundHandler();
        if (soundHandler == null) {
            return;
        }

        Set<String> soundIds = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        collectSoundIds(soundHandler, soundIds);
        for (String soundId : soundIds) {
            entries.add(createEntry(soundId, "Registered sound", soundId));
        }
    }

    public static void addKeybindEntries(List<Map<String, String>> entries) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.gameSettings == null || minecraft.gameSettings.keyBindings == null) {
            return;
        }

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
    }

    public static void addQuestEntries(List<Map<String, String>> entries) {
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
    }

    public static void addPageEntries(List<Map<String, String>> entries) {
        Map<String, Map<String, String>> entriesById = new LinkedHashMap<>();
        for (ParsedGuidePage page : getAllParsedPages()) {
            String pagePath = page.getId()
                .getResourcePath();
            String title = resolvePageTitle(page);
            String detail = resolvePageDetail(page);
            entriesById.putIfAbsent(pagePath, createEntry(pagePath, title, detail));
        }
        entries.addAll(entriesById.values());
    }

    public static void addEntityEntries(List<Map<String, String>> entries) {
        LinkedHashSet<String> entityIds = new LinkedHashSet<>();
        for (Object rawId : net.minecraft.entity.EntityList.stringToClassMapping.keySet()) {
            if (rawId instanceof String entityId) {
                GuidebookSceneEntityLoader.addCandidateForms(entityIds, entityId);
            }
        }
        for (String previewPlayerId : GuidebookSceneEntityLoader.PREVIEW_PLAYER_IDS) {
            GuidebookSceneEntityLoader.addCandidateForms(entityIds, previewPlayerId);
        }

        for (String entityId : entityIds) {
            String simpleToken = GuidebookSceneEntityLoader.extractSimpleEntityToken(entityId);
            String label = simpleToken == null ? entityId : formatEntityLabel(simpleToken);
            entries.add(createEntry(entityId, label, entityId));
        }
    }

    public static List<ParsedGuidePage> getAllParsedPages() {
        List<ParsedGuidePage> pages = new ArrayList<>();
        for (MutableGuide guide : GuideRegistry.getAll()) {
            try {
                pages.addAll(guide.getPages());
            } catch (IllegalStateException ignored) {}
        }
        return pages;
    }

    public static List<String> readStringList(ParsedGuidePage page, String key) {
        Object value = page.getFrontmatter()
            .additionalProperties()
            .get(key);
        if (!(value instanceof List<?>values)) {
            return List.of();
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

    public static String resolvePageTitle(ParsedGuidePage page) {
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

    public static String resolvePageDetail(ParsedGuidePage page) {
        return page.getLanguage() + " - " + page.getSourcePack();
    }

    public static Map<String, String> createEntry(String id, @Nullable String label, @Nullable String detail) {
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

    public static @Nullable String trimToNull(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static void addGuideOpenEntries(List<Map<String, String>> entries, String prefix, String labelPrefix) {
        for (MutableGuide guide : GuideRegistry.getAll()) {
            ResourceLocation guideId = guide.getId();
            if (guideId == null) {
                continue;
            }
            String id = guideId.toString();
            entries.add(createEntry(prefix + " " + id, labelPrefix + ": " + id, "Guide id"));
        }
    }

    private static void addCommandOptionEntries(List<Map<String, String>> entries, String prefix, String[] options,
        String detail) {
        for (String option : options) {
            if (option == null || option.trim()
                .isEmpty()) {
                continue;
            }
            entries.add(createEntry(prefix + " " + option, formatCommandLabel(option), detail));
        }
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

        switch (value) {
            case ResourceLocation resourceLocation -> {
                soundIds.add(resourceLocation.toString());
                return;
            }
            case Map<?, ?> map -> {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    collectSoundIdsFromValue(entry.getKey(), soundIds, depth - 1);
                    collectSoundIdsFromValue(entry.getValue(), soundIds, depth - 1);
                }
                return;
            }
            case Iterable<?> iterable -> {
                for (Object element : iterable) {
                    collectSoundIdsFromValue(element, soundIds, depth - 1);
                }
                return;
            }
            default -> {
            }
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

    private static void addCommandHandlerEntries(List<Map<String, String>> entries, CommandHandler commandHandler,
        String sourceLabel) {
        Map<String, ICommand> commands = commandHandler.getCommands();
        if (commands == null || commands.isEmpty()) {
            return;
        }
        ICommandSender commandSender = resolveCommandSender();
        for (Map.Entry<String, ICommand> entry : commands.entrySet()) {
            String commandName = trimToNull(entry.getKey());
            ICommand command = entry.getValue();
            if (commandName == null || command == null) {
                continue;
            }
            String commandId = "/" + commandName;
            entries.add(
                createEntry(
                    commandId,
                    resolveCommandEntryLabel(command, commandName, sourceLabel),
                    resolveCommandEntryDetail(command, commandSender, sourceLabel)));
        }
    }

    private static void addFallbackServerCommandEntries(List<Map<String, String>> entries) {
        for (ICommand command : createFallbackServerCommands()) {
            String commandName = trimToNull(command.getCommandName());
            if (commandName == null) {
                continue;
            }
            entries.add(
                createEntry(
                    "/" + commandName,
                    resolveCommandEntryLabel(command, commandName, "Builtin server command"),
                    "Builtin server command"));
            List<String> aliases = command.getCommandAliases();
            if (aliases == null) {
                continue;
            }
            for (String rawAlias : aliases) {
                String alias = trimToNull(rawAlias);
                if (alias == null) {
                    continue;
                }
                entries.add(
                    createEntry(
                        "/" + alias,
                        resolveCommandEntryLabel(command, alias, "Builtin server alias"),
                        "Builtin server alias"));
            }
        }
    }

    private static List<ICommand> createFallbackServerCommands() {
        List<ICommand> commands = new ArrayList<>();
        commands.add(new CommandTime());
        commands.add(new CommandGameMode());
        commands.add(new CommandDifficulty());
        commands.add(new CommandDefaultGameMode());
        commands.add(new CommandKill());
        commands.add(new CommandToggleDownfall());
        commands.add(new CommandWeather());
        commands.add(new CommandXP());
        commands.add(new CommandTeleport());
        commands.add(new CommandGive());
        commands.add(new CommandEffect());
        commands.add(new CommandEnchant());
        commands.add(new CommandEmote());
        commands.add(new CommandShowSeed());
        commands.add(new CommandHelp());
        commands.add(new CommandDebug());
        commands.add(new CommandMessage());
        commands.add(new CommandBroadcast());
        commands.add(new CommandSetSpawnpoint());
        commands.add(new CommandSetDefaultSpawnpoint());
        commands.add(new CommandGameRule());
        commands.add(new CommandClearInventory());
        commands.add(new CommandTestFor());
        commands.add(new CommandSpreadPlayers());
        commands.add(new CommandPlaySound());
        commands.add(new CommandScoreboard());
        commands.add(new CommandAchievement());
        commands.add(new CommandSummon());
        commands.add(new CommandSetBlock());
        commands.add(new CommandTestForBlock());
        commands.add(new CommandMessageRaw());
        commands.add(new CommandPublishLocalServer());
        commands.add(new CommandOp());
        commands.add(new CommandDeOp());
        commands.add(new CommandStop());
        commands.add(new CommandSaveAll());
        commands.add(new CommandSaveOff());
        commands.add(new CommandSaveOn());
        commands.add(new CommandBanIp());
        commands.add(new CommandPardonIp());
        commands.add(new CommandBanPlayer());
        commands.add(new CommandListBans());
        commands.add(new CommandPardonPlayer());
        commands.add(new CommandServerKick());
        commands.add(new CommandListPlayers());
        commands.add(new CommandWhitelist());
        commands.add(new CommandSetPlayerTimeout());
        commands.add(new CommandNetstat());
        return commands;
    }

    private static ICommandSender resolveCommandSender() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft != null && minecraft.thePlayer != null) {
            return minecraft.thePlayer;
        }
        return MinecraftServer.getServer();
    }

    private static String resolveCommandEntryLabel(ICommand command, String commandName, String sourceLabel) {
        String translatedName = formatCommandLabel(commandName);
        String commandLabel = trimToNull(command.getCommandName());
        if (commandLabel != null && !commandLabel.equalsIgnoreCase(commandName)) {
            return translatedName + " (" + commandLabel + ")";
        }
        return translatedName + " - " + sourceLabel;
    }

    private static String resolveCommandEntryDetail(ICommand command, ICommandSender sender, String sourceLabel) {
        if (sender != null) {
            try {
                String usage = trimToNull(command.getCommandUsage(sender));
                if (usage != null) {
                    return usage;
                }
            } catch (Throwable ignored) {}
        }
        return sourceLabel;
    }

    private static String formatEntityLabel(String simpleToken) {
        if (simpleToken == null || simpleToken.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        char previous = 0;
        for (int index = 0; index < simpleToken.length(); index++) {
            char current = simpleToken.charAt(index);
            if (index > 0 && Character.isUpperCase(current) && Character.isLowerCase(previous)) {
                builder.append(' ');
            }
            if (current == '_' || current == '-') {
                builder.append(' ');
                previous = current;
                continue;
            }
            builder.append(current);
            previous = current;
        }
        if (builder.length() == 0) {
            return simpleToken;
        }
        builder.setCharAt(0, Character.toUpperCase(builder.charAt(0)));
        return builder.toString();
    }
}
