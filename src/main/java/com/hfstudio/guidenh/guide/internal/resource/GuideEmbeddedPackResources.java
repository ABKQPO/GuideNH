package com.hfstudio.guidenh.guide.internal.resource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.Nullable;

import net.minecraft.util.ResourceLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GuideEmbeddedPackResources {

    private static final Logger LOG = LoggerFactory.getLogger(GuideEmbeddedPackResources.class);
    private static final String PACK_RESOURCE_PATH =
        "assets/guidenh/resourcepacks/guidenh_tutorial_guide_resource_pack.zip";

    private static volatile boolean loaded;
    private static volatile Map<String, byte[]> entries = Collections.emptyMap();

    private GuideEmbeddedPackResources() {}

    public static void clear() {
        loaded = false;
        entries = Collections.emptyMap();
    }

    public static @Nullable byte[] read(ResourceLocation id) {
        ensureLoaded();
        return entries.get(toPackPath(id));
    }

    private static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }

        entries = loadEntries();
        loaded = true;
    }

    private static Map<String, byte[]> loadEntries() {
        var loadedEntries = new HashMap<String, byte[]>();

        try (var raw = GuideEmbeddedPackResources.class.getClassLoader().getResourceAsStream(PACK_RESOURCE_PATH)) {
            if (raw == null) {
                LOG.warn("Embedded guide pack {} was not found on the classpath", PACK_RESOURCE_PATH);
                return Collections.emptyMap();
            }

            try (var zip = new ZipInputStream(raw)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if (!entry.isDirectory()) {
                        loadedEntries.put(entry.getName(), readFully(zip));
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("Failed to read embedded guide pack {}", PACK_RESOURCE_PATH, e);
            return Collections.emptyMap();
        }

        return Collections.unmodifiableMap(loadedEntries);
    }

    private static String toPackPath(ResourceLocation id) {
        return "assets/" + id.getResourceDomain() + "/" + id.getResourcePath();
    }

    private static byte[] readFully(InputStream input) throws IOException {
        var out = new ByteArrayOutputStream();
        var buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) >= 0) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }
}
