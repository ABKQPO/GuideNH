package com.hfstudio.guidenh.guide.siteexport.site;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public final class GuideSiteLocalServerJarWriter {

    private GuideSiteLocalServerJarWriter() {}

    public static void writeTo(Path target) throws Exception {
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Manifest manifest = new Manifest();
        Attributes mainAttributes = manifest.getMainAttributes();
        mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        mainAttributes.put(Attributes.Name.MAIN_CLASS, GuideSiteLocalServer.class.getName());

        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(target), manifest)) {
            writeClassEntry(out, GuideSiteLocalServer.class);
        }
    }

    private static void writeClassEntry(JarOutputStream out, Class<?> type) throws Exception {
        String resourceName = type.getName()
            .replace('.', '/') + ".class";
        try (InputStream in = type.getClassLoader()
            .getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new IllegalStateException("Missing bundled class bytes for " + resourceName);
            }
            out.putNextEntry(new JarEntry(resourceName));
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
            }
            out.closeEntry();
        }
    }
}
