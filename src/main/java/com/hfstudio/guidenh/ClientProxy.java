package com.hfstudio.guidenh;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;

import com.hfstudio.guidenh.bridge.GuideNhRuntimeBridge;
import com.hfstudio.guidenh.bridge.GuideNhRuntimeBridgeSettings;
import com.hfstudio.guidenh.client.RegionWandRenderer;
import com.hfstudio.guidenh.client.command.GuideNhClientBridgeController;
import com.hfstudio.guidenh.client.command.GuideNhClientCommand;
import com.hfstudio.guidenh.client.hotkey.CycleRegionWandModeHotkey;
import com.hfstudio.guidenh.client.hotkey.GuidePageHistoryHotkey;
import com.hfstudio.guidenh.client.hotkey.OpenGuideHomeHotkey;
import com.hfstudio.guidenh.client.hotkey.OpenGuideHotkey;
import com.hfstudio.guidenh.client.hotkey.OpenSceneEditorHotkey;
import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.internal.GuideDevelopmentResourcePackWatcher;
import com.hfstudio.guidenh.guide.internal.GuideME;
import com.hfstudio.guidenh.guide.internal.GuideOnStartup;
import com.hfstudio.guidenh.guide.internal.GuideReloadListener;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.TagAttributeRegistry;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.AnchorProvider;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.AttributeNameProvider;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.AttributePresetValueProvider;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.AutocompleteProviders;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.BlockIdProvider;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.BooleanValueProvider;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.ColorProvider;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.CommandProvider;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.DomainProvider;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.EntityNameProvider;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.EnumValueProvider;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.ExpressionProvider;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.FencedBlockLanguageProvider;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.FormatPatternProvider;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.FrontmatterKeyProvider;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.FrontmatterValueProvider;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.ImagePathProvider;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.ItemIdProvider;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.KeyBindProvider;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.NumericValueProvider;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.OreDictProvider;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.PageReferenceProvider;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.TagNameProvider;
import com.hfstudio.guidenh.guide.internal.host.LytHost;
import com.hfstudio.guidenh.guide.internal.host.LytHostWorkItem;
import com.hfstudio.guidenh.guide.internal.host.scripts.BlockImageScript;
import com.hfstudio.guidenh.guide.internal.host.scripts.CategoryScript;
import com.hfstudio.guidenh.guide.internal.host.scripts.CommandLinkScript;
import com.hfstudio.guidenh.guide.internal.host.scripts.CsvTableScript;
import com.hfstudio.guidenh.guide.internal.host.scripts.FloatingImageScript;
import com.hfstudio.guidenh.guide.internal.host.scripts.ImageScript;
import com.hfstudio.guidenh.guide.internal.host.scripts.ItemGridScript;
import com.hfstudio.guidenh.guide.internal.host.scripts.ItemImageScript;
import com.hfstudio.guidenh.guide.internal.host.scripts.ItemLinkScript;
import com.hfstudio.guidenh.guide.internal.host.scripts.KeyBindScript;
import com.hfstudio.guidenh.guide.internal.host.scripts.MermaidScript;
import com.hfstudio.guidenh.guide.internal.host.scripts.PlayerNameScript;
import com.hfstudio.guidenh.guide.internal.host.scripts.QuestCardScript;
import com.hfstudio.guidenh.guide.internal.host.scripts.QuestLinkScript;
import com.hfstudio.guidenh.guide.internal.host.scripts.RecipeScript;
import com.hfstudio.guidenh.guide.internal.host.scripts.SceneScript;
import com.hfstudio.guidenh.guide.internal.host.scripts.SoundLinkScript;
import com.hfstudio.guidenh.guide.internal.host.scripts.SpecialScript;
import com.hfstudio.guidenh.guide.internal.host.scripts.StructureScript;
import com.hfstudio.guidenh.guide.internal.host.scripts.SubPagesScript;
import com.hfstudio.guidenh.guide.internal.host.scripts.TooltipScript;
import com.hfstudio.guidenh.guide.internal.scheduler.DevWatchWorkItem;
import com.hfstudio.guidenh.guide.internal.scheduler.LytHostPreheatItem;
import com.hfstudio.guidenh.guide.internal.scheduler.MasterScheduler;
import com.hfstudio.guidenh.guide.internal.scheduler.SearchIndexWorkItem;
import com.hfstudio.guidenh.guide.scene.level.GuidebookFakeWorld;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.integration.GuideNhClientIntegrationBootstrap;
import com.hfstudio.guidenh.integration.Mods;
import com.hfstudio.guidenh.integration.ae2.network.Ae2NetworkRegistration;
import com.hfstudio.guidenh.integration.nei.GuideScreenNeiBridge;
import com.hfstudio.guidenh.network.GuideNhClientBridgeHandler;
import com.hfstudio.guidenh.network.GuideNhClientBridgeMessage;
import com.hfstudio.guidenh.network.GuideNhNetwork;
import com.hfstudio.guidenh.network.GuideNhRegionExportClientHandler;
import com.hfstudio.guidenh.network.GuideNhRegionExportReplyMessage;
import com.hfstudio.structurelibexport.StructureExportBootstrap;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.relauncher.Side;

public class ClientProxy extends CommonProxy {

    private static final LytHost lytHost = new LytHost();

    public static LytHost getLytHost() {
        return lytHost;
    }

    private final GuideNhRuntimeBridge runtimeBridge = new GuideNhRuntimeBridge();

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        GuidebookLevel.setPreviewWorldFactory(GuidebookFakeWorld::new);
        GuideNhClientIntegrationBootstrap.preInitClient();
        GuideME.initClientProxy();
        GuideNhNetwork.channel()
            .registerMessage(GuideNhClientBridgeHandler.class, GuideNhClientBridgeMessage.class, 2, Side.CLIENT);
        GuideNhNetwork.channel()
            .registerMessage(
                GuideNhRegionExportClientHandler.class,
                GuideNhRegionExportReplyMessage.class,
                8,
                Side.CLIENT);
        Ae2NetworkRegistration.registerClientMessages();
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        ((IReloadableResourceManager) Minecraft.getMinecraft()
            .getResourceManager()).registerReloadListener(new GuideReloadListener());
        ClientCommandHandler.instance.registerCommand(new GuideNhClientCommand());
        StructureExportBootstrap.registerClientCommands();
        GuideNhClientBridgeController.init();
        if (Mods.NotEnoughItems.isModLoaded()) {
            GuideScreenNeiBridge.init();
        }
        OpenGuideHomeHotkey.init();
        OpenGuideHotkey.init();
        GuidePageHistoryHotkey.init();
        OpenSceneEditorHotkey.init();
        AutocompleteProviders.register(new ItemIdProvider());
        TagAttributeRegistry.initialize();
        AutocompleteProviders.register(new AttributeNameProvider());

        AutocompleteProviders.register(new ColorProvider());
        AutocompleteProviders.register(new OreDictProvider());
        AutocompleteProviders.register(new BlockIdProvider());
        AutocompleteProviders.register(new EntityNameProvider());
        AutocompleteProviders.register(new KeyBindProvider());
        AutocompleteProviders.register(new PageReferenceProvider());
        AutocompleteProviders.register(new AnchorProvider());
        AutocompleteProviders.register(new CommandProvider());
        AutocompleteProviders.register(new AttributePresetValueProvider());
        AutocompleteProviders.register(new NumericValueProvider());
        AutocompleteProviders.register(new BooleanValueProvider());
        AutocompleteProviders.register(new EnumValueProvider());
        AutocompleteProviders.register(new ExpressionProvider());
        AutocompleteProviders.register(new DomainProvider());
        AutocompleteProviders.register(new FormatPatternProvider());
        AutocompleteProviders.register(new TagNameProvider());
        AutocompleteProviders.register(new FencedBlockLanguageProvider());
        AutocompleteProviders.register(new FrontmatterKeyProvider());
        AutocompleteProviders.register(new FrontmatterValueProvider());

        AutocompleteProviders.register(new ImagePathProvider());
        CycleRegionWandModeHotkey.init();
        MinecraftForge.EVENT_BUS.register(new RegionWandRenderer());
        MasterScheduler.init();
        MasterScheduler.getInstance()
            .submit(new LytHostWorkItem(lytHost));
        MasterScheduler.getInstance()
            .submit(new LytHostPreheatItem(lytHost));
        MasterScheduler.getInstance()
            .submit(new SearchIndexWorkItem());

        // Phase 3: LytScript registrations
        lytHost.registerScript("CommandLink", new CommandLinkScript());
        lytHost.registerScript("Img", new ImageScript());
        lytHost.registerScript("FloatingImage", new FloatingImageScript());
        lytHost.registerScript("PlayerName", new PlayerNameScript());
        lytHost.registerScript("KeyBind", new KeyBindScript());
        lytHost.registerScript("SoundLink", new SoundLinkScript());
        lytHost.registerScript("Structure", new StructureScript());
        lytHost.registerScript("SubPages", new SubPagesScript());
        lytHost.registerScript("Tooltip", new TooltipScript());
        lytHost.registerScript("ItemGrid", new ItemGridScript());
        lytHost.registerScript("ItemImage", new ItemImageScript());
        lytHost.registerScript("ItemLink", new ItemLinkScript());
        lytHost.registerScript("Category", new CategoryScript());
        lytHost.registerScript("Special", new SpecialScript());
        lytHost.registerScript("BlockImage", new BlockImageScript());
        lytHost.registerScript("CsvTable", new CsvTableScript());
        lytHost.registerScript("Mermaid", new MermaidScript());
        lytHost.registerScript("QuestLink", new QuestLinkScript());
        lytHost.registerScript("QuestCard", new QuestCardScript());
        // Phase 3: SceneScript handles Scene and GameScene
        SceneScript sceneScript = new SceneScript();
        lytHost.registerScript("Scene", sceneScript);
        lytHost.registerScript("GameScene", sceneScript);
        // Phase 3: RecipeScript handles Recipe, RecipeFor, RecipeUsage, RecipesFor
        RecipeScript recipeScript = new RecipeScript();
        lytHost.registerScript("Recipe", recipeScript);
        lytHost.registerScript("RecipeFor", recipeScript);
        lytHost.registerScript("RecipeUsage", recipeScript);
        lytHost.registerScript("RecipesFor", recipeScript);

        MinecraftForge.EVENT_BUS.register(this);
        GuideDebugLog.infoAlways(
            "GuideNH runtime bridge configuration loaded. enabled={}, hostConfigured={}, port={}, tokenConfigured={}",
            ModConfig.runtimeBridge.enabled,
            ModConfig.runtimeBridge.host != null && !ModConfig.runtimeBridge.host.trim()
                .isEmpty(),
            ModConfig.runtimeBridge.port,
            ModConfig.runtimeBridge.token != null && !ModConfig.runtimeBridge.token.isEmpty());
        runtimeBridge.start(
            new GuideNhRuntimeBridgeSettings(
                ModConfig.runtimeBridge.enabled,
                ModConfig.runtimeBridge.host,
                ModConfig.runtimeBridge.port,
                ModConfig.runtimeBridge.token,
                ModConfig.runtimeBridge.maxMessageBytes,
                ModConfig.runtimeBridge.maxPageSize,
                ModConfig.runtimeBridge.maxSubscriptions,
                ModConfig.runtimeBridge.maxConnections,
                ModConfig.runtimeBridge.maxDeltaEntries));
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
    }

    @Override
    public void completeInit(FMLLoadCompleteEvent event) {
        super.completeInit(event);
        GuideDevelopmentResourcePackWatcher.init();
        MasterScheduler.getInstance()
            .submit(new DevWatchWorkItem());
        GuideOnStartup.init();
    }

    @SubscribeEvent
    public void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        GuideDebugLog.infoAlways("Minecraft client disconnected. Stopping GuideNH runtime bridge session state");
        runtimeBridge.stop();
        GuideME.closeSearch();
        lytHost.getNavigation()
            .clear();
    }
}
