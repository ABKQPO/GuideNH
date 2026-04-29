package com.hfstudio.guidenh.guide.scene.support;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;

import cpw.mods.fml.common.registry.GameRegistry;

public class GuideBlockMatcher {

    private final String blockId;
    @Nullable
    private final Integer meta;

    private GuideBlockMatcher(String blockId, @Nullable Integer meta) {
        this.blockId = blockId;
        this.meta = meta;
    }

    public static GuideBlockMatcher parse(String literal) {
        if (literal == null) {
            throw new IllegalArgumentException("Block matcher cannot be null");
        }

        String trimmed = literal.trim();
        String[] parts = trimmed.split(":");
        if (parts.length < 2 || parts.length > 3 || parts[0].isEmpty() || parts[1].isEmpty()) {
            throw new IllegalArgumentException("Invalid block matcher: " + literal);
        }

        Integer meta = null;
        if (parts.length == 3) {
            try {
                meta = Integer.valueOf(parts[2]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid block matcher meta: " + literal, e);
            }
            if (meta < 0) {
                throw new IllegalArgumentException("Block matcher meta must be non-negative: " + literal);
            }
        }

        return new GuideBlockMatcher(parts[0] + ":" + parts[1], meta);
    }

    public String getBlockId() {
        return blockId;
    }

    @Nullable
    public Integer getMeta() {
        return meta;
    }

    public boolean matches(@Nullable Block block, int meta) {
        if (block == null) {
            return false;
        }

        if (!matchesBlockId(block)) {
            return false;
        }

        return this.meta == null || this.meta == meta;
    }

    public boolean matchesResolvedBlockId(@Nullable String resolvedBlockId, int meta) {
        return matchesCandidate(resolvedBlockId) && (this.meta == null || this.meta == meta);
    }

    private boolean matchesBlockId(Block block) {
        String uniqueIdentifier = resolveUniqueIdentifier(block);
        if (matchesCandidate(uniqueIdentifier)) {
            return true;
        }

        Object registryName = Block.blockRegistry.getNameForObject(block);
        if (registryName != null && matchesCandidate(registryName.toString())) {
            return true;
        }

        return matchesCandidate(block.getUnlocalizedName());
    }

    private boolean matchesCandidate(@Nullable String candidate) {
        String normalizedCandidate = normalizeResolvedBlockId(candidate);
        return normalizedCandidate != null && blockId.equals(normalizedCandidate);
    }

    @Nullable
    public static String resolveUniqueIdentifier(Block block) {
        try {
            GameRegistry.UniqueIdentifier uniqueIdentifier = GameRegistry.findUniqueIdentifierFor(block);
            if (uniqueIdentifier != null) {
                return uniqueIdentifier.toString();
            }
        } catch (RuntimeException ignored) {
            // Unregistered synthetic blocks used in tests/editor tooling can reach this path.
        }
        return null;
    }

    @Nullable
    public static String normalizeResolvedBlockId(@Nullable String candidate) {
        if (candidate == null) {
            return null;
        }

        String trimmed = candidate.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        if (trimmed.startsWith("tile.")) {
            return "minecraft:" + trimmed.substring(5);
        }

        int tileNamespaceIndex = trimmed.indexOf(":tile.");
        if (tileNamespaceIndex >= 0) {
            return trimmed.substring(0, tileNamespaceIndex + 1) + trimmed.substring(tileNamespaceIndex + 6);
        }

        String normalizedRegistryName = normalizeRegistryName(trimmed);
        if (normalizedRegistryName != null) {
            return normalizedRegistryName;
        }

        return normalizeUnlocalizedName(trimmed);
    }

    @Nullable
    public static String normalizeRegistryName(@Nullable String registryName) {
        if (registryName == null) {
            return null;
        }

        String trimmed = registryName.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        return trimmed.indexOf(':') >= 0 ? trimmed : "minecraft:" + trimmed;
    }

    @Nullable
    public static String normalizeUnlocalizedName(@Nullable String unlocalizedName) {
        if (unlocalizedName == null || !unlocalizedName.startsWith("tile.") || unlocalizedName.length() <= 5) {
            return null;
        }

        return "minecraft:" + unlocalizedName.substring(5);
    }
}
