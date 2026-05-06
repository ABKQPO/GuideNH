package com.hfstudio.guidenh.client.command;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentTranslation;

import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.internal.editor.io.SceneEditorStructureCache;
import com.hfstudio.guidenh.guide.internal.editor.io.SceneEditorStructureImportService;
import com.hfstudio.guidenh.guide.internal.structure.GuideNhStructureRuntime;
import com.hfstudio.guidenh.guide.internal.structure.GuideStructureFileStore;
import com.hfstudio.guidenh.network.GuideNhNetwork;
import com.hfstudio.guidenh.network.GuideNhStructureRequestMessage;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuideNhClientBridgeController {

    public static final GuideNhClientBridgeController INSTANCE = new GuideNhClientBridgeController();

    private final SceneEditorStructureImportService structureImportService;
    private final GuideStructureFileStore structureFileStore;

    private CompletableFuture<SceneEditorStructureImportService.ImportResult> pendingImport;
    private PendingImportRequest pendingImportRequest;

    private GuideNhClientBridgeController() {
        this.structureImportService = new SceneEditorStructureImportService(SceneEditorStructureCache.createDefault());
        this.structureFileStore = GuideStructureFileStore.createDefault();
    }

    public static GuideNhClientBridgeController getInstance() {
        return INSTANCE;
    }

    public static void init() {
        FMLCommonHandler.instance()
            .bus()
            .register(INSTANCE);
    }

    public boolean isServerStructureCommandsAvailable() {
        return GuideNhStructureRuntime.isServerStructureCommandsAvailable();
    }

    public Path exportStructureToFile(String prefix, String structureText) throws Exception {
        var entry = GuideNhStructureRuntime.getClientMemoryStore()
            .remember(prefix, structureText);
        syncEntryToServerIfAvailable(entry);
        return structureFileStore.saveExport(prefix, structureText);
    }

    public void beginImportStructure(int x, int y, int z, String filePath) {
        if (!isServerStructureCommandsAvailable()) {
            sendClient(GuidebookText.CommandStructureServerRequired);
            return;
        }
        if (pendingImport != null) {
            sendClient(GuidebookText.CommandStructureImportPending);
            return;
        }
        pendingImportRequest = new PendingImportRequest(x, y, z);
        pendingImport = structureImportService.importFromPathAsync(java.nio.file.Paths.get(filePath));
    }

    public void placeAllStructures(int x, int y, int z) {
        if (!isServerStructureCommandsAvailable()) {
            sendClient(GuidebookText.CommandStructureServerRequired);
            return;
        }
        GuideNhNetwork.channel()
            .sendToServer(GuideNhStructureRequestMessage.placeAll(x, y, z));
    }

    public void rememberScene(String label, String structureText) {
        try {
            var entry = GuideNhStructureRuntime.getClientMemoryStore()
                .remember(label, structureText);
            syncEntryToServerIfAvailable(entry);
        } catch (Exception e) {
            // Silently ignore parse failures for auto-registered scenes
        }
    }

    public void onServerHello() {
        GuideNhStructureRuntime.setServerStructureCommandsAvailable(true);
        GuideNhStructureRuntime.setClientStructureSyncNeeded(true);
    }

    public void onServerDisconnected() {
        GuideNhStructureRuntime.setServerStructureCommandsAvailable(false);
        GuideNhStructureRuntime.setClientStructureSyncNeeded(false);
        pendingImport = null;
        pendingImportRequest = null;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (GuideNhStructureRuntime.isClientStructureSyncNeeded()) {
            GuideNhStructureRuntime.setClientStructureSyncNeeded(false);
            syncAllClientStructuresToServer();
        }
        if (pendingImport == null || !pendingImport.isDone()) {
            return;
        }

        CompletableFuture<SceneEditorStructureImportService.ImportResult> future = pendingImport;
        PendingImportRequest request = pendingImportRequest;
        pendingImport = null;
        pendingImportRequest = null;
        if (request == null) {
            return;
        }

        try {
            SceneEditorStructureImportService.ImportResult result = future.join();
            if (result == null) {
                sendClient(GuidebookText.CommandStructureImportCanceled);
                return;
            }
            var entry = GuideNhStructureRuntime.getClientMemoryStore()
                .remember(result.getDisplayPath(), result.getStructureText());
            GuideNhNetwork.channel()
                .sendToServer(
                    GuideNhStructureRequestMessage
                        .importAndPlace(request.x, request.y, request.z, entry.getStructureText()));
        } catch (CompletionException e) {
            sendClient(
                GuidebookText.CommandStructureImportFailure,
                getErrorMessage(e.getCause() != null ? e.getCause() : e));
        } catch (Exception e) {
            sendClient(GuidebookText.CommandStructureImportFailure, getErrorMessage(e));
        }
    }

    @SubscribeEvent
    public void onClientDisconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        onServerDisconnected();
    }

    private void syncAllClientStructuresToServer() {
        var entries = GuideNhStructureRuntime.getClientMemoryStore()
            .snapshotEntries();
        if (entries.isEmpty()) {
            return;
        }
        for (var entry : entries) {
            syncEntryToServerIfAvailable(entry);
        }
    }

    private void syncEntryToServerIfAvailable(
        com.hfstudio.guidenh.guide.internal.structure.GuideStructureMemoryStore.Entry entry) {
        if (!isServerStructureCommandsAvailable()) {
            return;
        }
        GuideNhNetwork.channel()
            .sendToServer(GuideNhStructureRequestMessage.cache(entry.getStructureText()));
    }

    private void sendClient(GuidebookText key, Object... args) {
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.thePlayer;
        if (player != null) {
            player.addChatMessage(new ChatComponentTranslation(key.getTranslationKey(), args));
        }
    }

    public static String getErrorMessage(Throwable throwable) {
        return throwable.getMessage() != null ? throwable.getMessage()
            : throwable.getClass()
                .getSimpleName();
    }

    public static class PendingImportRequest {

        private final int x;
        private final int y;
        private final int z;

        private PendingImportRequest(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
