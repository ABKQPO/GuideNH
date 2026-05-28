package com.hfstudio.guidenh.client.command;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentTranslation;

import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.internal.editor.io.SceneEditorStructureCache;
import com.hfstudio.guidenh.guide.internal.editor.io.SceneEditorStructureImportService;
import com.hfstudio.guidenh.guide.internal.structure.GuideNhStructureRuntime;
import com.hfstudio.guidenh.guide.internal.structure.GuideStructureData;
import com.hfstudio.guidenh.guide.internal.structure.GuideStructureFileStore;
import com.hfstudio.guidenh.network.GuideNhNetwork;
import com.hfstudio.guidenh.network.GuideNhRegionExportReplyMessage;
import com.hfstudio.guidenh.network.GuideNhRegionExportRequestMessage;
import com.hfstudio.guidenh.network.GuideNhStructureRequestMessage;
import com.hfstudio.guidenh.network.GuideNhStructureRequestSender;

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
    private final AtomicInteger nextRegionExportRequestId;
    private final Map<Integer, CompletableFuture<String>> pendingRegionExports;
    private final Map<Integer, RegionReplyAssembler> pendingRegionExportChunks;
    private final Set<String> rememberedSceneLabels;

    private CompletableFuture<SceneEditorStructureImportService.ImportResult> pendingImport;
    private PendingImportRequest pendingImportRequest;

    private GuideNhClientBridgeController() {
        this.structureImportService = new SceneEditorStructureImportService(SceneEditorStructureCache.createDefault());
        this.structureFileStore = GuideStructureFileStore.createDefault();
        this.nextRegionExportRequestId = new AtomicInteger(1);
        this.pendingRegionExports = new HashMap<>();
        this.pendingRegionExportChunks = new HashMap<>();
        this.rememberedSceneLabels = new HashSet<>();
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
        syncAllClientStructuresToServer();
        GuideNhStructureRequestSender.sendPlaceAll(GuideNhNetwork.channel(), x, y, z);
    }

    public void rememberScene(String label, String structureText) {
        try {
            GuideNhStructureRuntime.getClientMemoryStore()
                .remember(label, structureText);
        } catch (Exception e) {
            // Silently ignore parse failures for auto-registered scenes
        }
    }

    public void rememberScene(String label, GuideStructureData structureData) {
        if (structureData == null || label == null || label.isEmpty()) {
            return;
        }
        if (!rememberedSceneLabels.add(label)) {
            return;
        }
        GuideNhStructureRuntime.getClientMemoryStore()
            .remember(label, structureData);
    }

    public boolean hasRememberedScene(String label) {
        return label != null && rememberedSceneLabels.contains(label);
    }

    public void onServerHello() {
        GuideNhStructureRuntime.setServerStructureCommandsAvailable(true);
        GuideNhStructureRuntime.setClientStructureSyncNeeded(false);
    }

    public void onServerDisconnected() {
        GuideNhStructureRuntime.setServerStructureCommandsAvailable(false);
        GuideNhStructureRuntime.setClientStructureSyncNeeded(false);
        pendingImport = null;
        pendingImportRequest = null;
        rememberedSceneLabels.clear();
        for (CompletableFuture<String> future : pendingRegionExports.values()) {
            future.complete(null);
        }
        pendingRegionExports.clear();
        pendingRegionExportChunks.clear();
    }

    public CompletableFuture<String> requestRegionExport(int x, int y, int z, int sizeX, int sizeY, int sizeZ,
        boolean includeEntities) {
        if (!isServerStructureCommandsAvailable()) {
            return CompletableFuture.completedFuture(null);
        }
        int requestId = nextRegionExportRequestId.getAndIncrement();
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingRegionExports.put(requestId, future);
        GuideNhNetwork.channel()
            .sendToServer(
                new GuideNhRegionExportRequestMessage(requestId, x, y, z, sizeX, sizeY, sizeZ, includeEntities));
        return future;
    }

    public void handleRegionExportReply(GuideNhRegionExportReplyMessage message) {
        CompletableFuture<String> future = pendingRegionExports.get(message.getRequestId());
        if (future == null) {
            return;
        }
        if (message.getAction() == GuideNhRegionExportReplyMessage.ACTION_ERROR) {
            pendingRegionExports.remove(message.getRequestId());
            pendingRegionExportChunks.remove(message.getRequestId());
            future.complete(null);
            return;
        }
        if (message.getAction() == GuideNhRegionExportReplyMessage.ACTION_COMPLETE) {
            pendingRegionExports.remove(message.getRequestId());
            pendingRegionExportChunks.remove(message.getRequestId());
            future.complete(message.getPayloadText());
            return;
        }
        if (message.getAction() == GuideNhRegionExportReplyMessage.ACTION_CHUNK) {
            RegionReplyAssembler assembler = pendingRegionExportChunks
                .computeIfAbsent(message.getRequestId(), ignored -> new RegionReplyAssembler(message.getChunkCount()));
            String completed = assembler.accept(message);
            if (completed != null) {
                pendingRegionExports.remove(message.getRequestId());
                pendingRegionExportChunks.remove(message.getRequestId());
                future.complete(completed);
            }
        }
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
            GuideNhStructureRequestSender.sendImportAndPlace(
                GuideNhNetwork.channel(),
                request.x,
                request.y,
                request.z,
                entry.getStructureText());
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
        GuideNhStructureRequestSender.sendCache(GuideNhNetwork.channel(), entry.getStructureText());
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

    private static final class RegionReplyAssembler {

        private final byte[][] chunks;
        private int received;
        private int totalBytes;

        private RegionReplyAssembler(int chunkCount) {
            if (chunkCount <= 0 || chunkCount > GuideNhStructureRequestMessage.MAX_CHUNKS_PER_STRUCTURE) {
                throw new IllegalArgumentException("Invalid region export chunk count: " + chunkCount);
            }
            this.chunks = new byte[chunkCount][];
        }

        private synchronized String accept(GuideNhRegionExportReplyMessage message) {
            int index = message.getChunkIndex();
            if (message.getChunkCount() != chunks.length || index < 0 || index >= chunks.length) {
                return null;
            }
            byte[] bytes = message.getPayloadBytes();
            if (chunks[index] == null) {
                chunks[index] = bytes;
                received++;
                totalBytes += bytes.length;
            }
            if (received != chunks.length) {
                return null;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream(totalBytes);
            for (byte[] chunk : chunks) {
                if (chunk == null) {
                    return null;
                }
                out.write(chunk, 0, chunk.length);
            }
            return out.toString(StandardCharsets.UTF_8);
        }
    }
}
