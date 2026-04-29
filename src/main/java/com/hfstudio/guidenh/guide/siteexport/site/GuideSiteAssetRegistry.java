package com.hfstudio.guidenh.guide.siteexport.site;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;

public class GuideSiteAssetRegistry {

    private final Path outDir;

    public GuideSiteAssetRegistry(Path outDir) {
        this.outDir = outDir;
    }

    public String writeShared(String bucket, String extension, byte[] content) throws Exception {
        String hash = sha256(content);
        Path relative = Paths.get("_res", bucket, hash + extension);
        Path absolute = outDir.resolve(relative);
        Files.createDirectories(absolute.getParent());
        if (!Files.exists(absolute)) {
            Files.write(absolute, content);
        }
        return relative.toString().replace('\\', '/');
    }

    private String sha256(byte[] content) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
            .digest(content);
        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            hex.append(Character.forDigit((b >> 4) & 0xF, 16));
            hex.append(Character.forDigit(b & 0xF, 16));
        }
        return hex.toString();
    }
}
