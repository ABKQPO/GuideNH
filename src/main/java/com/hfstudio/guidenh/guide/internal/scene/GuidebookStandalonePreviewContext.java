package com.hfstudio.guidenh.guide.internal.scene;

import java.lang.reflect.Field;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.NetworkManager;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Creates a lightweight client networking context for guidebook previews when the client is still on the title screen.
 */
@SideOnly(Side.CLIENT)
public final class GuidebookStandalonePreviewContext {

    @Nullable
    private static NetHandlerPlayClient standaloneNetHandler;
    @Nullable
    private static Field netHandlerWorldField;
    private static volatile boolean netHandlerWorldFieldResolved;

    private GuidebookStandalonePreviewContext() {}

    public static NetHandlerPlayClient resolveNetHandler(Minecraft minecraft) {
        return resolveNetHandler(
            minecraft != null ? minecraft.getNetHandler() : null,
            () -> createStandaloneNetHandler(minecraft));
    }

    public static NetHandlerPlayClient resolveNetHandler(Minecraft minecraft, @Nullable WorldClient previewWorld) {
        NetHandlerPlayClient activeNetHandler = minecraft != null ? minecraft.getNetHandler() : null;
        return resolvePreviewNetHandler(activeNetHandler, () -> createStandaloneNetHandler(minecraft), previewWorld);
    }

    public static synchronized NetHandlerPlayClient resolveNetHandler(@Nullable NetHandlerPlayClient activeNetHandler,
        Supplier<NetHandlerPlayClient> standaloneFactory) {
        if (activeNetHandler != null) {
            return activeNetHandler;
        }
        if (standaloneNetHandler == null) {
            standaloneNetHandler = standaloneFactory.get();
        }
        if (standaloneNetHandler == null) {
            throw new IllegalStateException("Guidebook preview requires an active client world");
        }
        return standaloneNetHandler;
    }

    static synchronized NetHandlerPlayClient resolvePreviewNetHandler(@Nullable NetHandlerPlayClient activeNetHandler,
        Supplier<NetHandlerPlayClient> standaloneFactory, @Nullable WorldClient previewWorld) {
        NetHandlerPlayClient resolved = resolveNetHandler(activeNetHandler, standaloneFactory);
        if (previewWorld != null && (activeNetHandler == null || activeNetHandler == standaloneNetHandler)) {
            bindPreviewWorld(resolved, previewWorld);
        }
        return resolved;
    }

    private static NetHandlerPlayClient createStandaloneNetHandler(@Nullable Minecraft minecraft) {
        if (minecraft == null) {
            throw new IllegalStateException("Guidebook preview requires an active client world");
        }

        NetworkManager networkManager = new NetworkManager(true);
        NetHandlerPlayClient netHandler = new NetHandlerPlayClient(minecraft, minecraft.currentScreen, networkManager);
        networkManager.setNetHandler(netHandler);
        return netHandler;
    }

    public static void bindPreviewWorld(@Nullable NetHandlerPlayClient netHandler, @Nullable WorldClient previewWorld) {
        if (netHandler == null || previewWorld == null) {
            return;
        }
        Field worldField = resolveNetHandlerWorldField();
        if (worldField == null) {
            return;
        }
        try {
            worldField.set(netHandler, previewWorld);
        } catch (IllegalAccessException ignored) {}
    }

    @Nullable
    public static WorldClient getBoundPreviewWorld(@Nullable NetHandlerPlayClient netHandler) {
        if (netHandler == null) {
            return null;
        }
        Field worldField = resolveNetHandlerWorldField();
        if (worldField == null) {
            return null;
        }
        try {
            Object boundWorld = worldField.get(netHandler);
            return boundWorld instanceof WorldClient ? (WorldClient) boundWorld : null;
        } catch (IllegalAccessException ignored) {
            return null;
        }
    }

    @Nullable
    private static Field resolveNetHandlerWorldField() {
        if (netHandlerWorldFieldResolved) {
            return netHandlerWorldField;
        }
        netHandlerWorldFieldResolved = true;
        for (String fieldName : new String[] { "clientWorldController", "field_147300_g" }) {
            try {
                Field field = NetHandlerPlayClient.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                netHandlerWorldField = field;
                return netHandlerWorldField;
            } catch (ReflectiveOperationException ignored) {}
        }
        for (Field field : NetHandlerPlayClient.class.getDeclaredFields()) {
            if (!WorldClient.class.isAssignableFrom(field.getType())) {
                continue;
            }
            try {
                field.setAccessible(true);
                netHandlerWorldField = field;
                return netHandlerWorldField;
            } catch (RuntimeException ignored) {}
        }
        netHandlerWorldField = null;
        return netHandlerWorldField;
    }

    public static synchronized void resetForTests() {
        standaloneNetHandler = null;
        netHandlerWorldField = null;
        netHandlerWorldFieldResolved = false;
    }
}
