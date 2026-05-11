package com.hfstudio.guidenh.guide.scene.support;

import net.minecraft.block.Block;

import org.jetbrains.annotations.Nullable;

import cpw.mods.fml.common.registry.GameRegistry;

public class GuideBlockMatcher {

    private final String blockId;
    @Nullable
    private final Integer meta;

    protected GuideBlockMatcher(String blockId, @Nullable Integer meta) {
        this.blockId = blockId;
        this.meta = meta;
    }

    public static GuideBlockMatcher parse(String literal) {
        if (literal == null) {
            throw new IllegalArgumentException("Block matcher cannot be null");
        }

        String trimmed = trimToNull(literal);
        if (trimmed == null) {
            throw new IllegalArgumentException("Invalid block matcher: " + literal);
        }
        int effectiveEnd = stripTrailingSeparators(trimmed);
        int firstSeparator = trimmed.indexOf(':');
        if (firstSeparator <= 0 || firstSeparator >= effectiveEnd - 1) {
            throw new IllegalArgumentException("Invalid block matcher: " + literal);
        }

        int secondSeparator = trimmed.indexOf(':', firstSeparator + 1);
        if (secondSeparator == firstSeparator + 1) {
            throw new IllegalArgumentException("Invalid block matcher: " + literal);
        }
        if (secondSeparator > 0 && secondSeparator < effectiveEnd - 1
            && trimmed.indexOf(':', secondSeparator + 1) >= 0) {
            throw new IllegalArgumentException("Invalid block matcher: " + literal);
        }

        Integer meta = null;
        int blockIdEnd = effectiveEnd;
        if (secondSeparator > 0 && secondSeparator < effectiveEnd) {
            try {
                meta = Integer.valueOf(trimmed.substring(secondSeparator + 1, effectiveEnd));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid block matcher meta: " + literal, e);
            }
            if (meta < 0) {
                throw new IllegalArgumentException("Block matcher meta must be non-negative: " + literal);
            }
            blockIdEnd = secondSeparator;
        }

        return new GuideBlockMatcher(trimmed.substring(0, blockIdEnd), meta);
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
        String trimmed = trimToNull(candidate);
        if (trimmed == null) {
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
        String trimmed = trimToNull(registryName);
        if (trimmed == null) {
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

    private static int stripTrailingSeparators(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == ':') {
            end--;
        }
        return end;
    }

    @Nullable
    private static String trimToNull(@Nullable String value) {
        if (value == null) {
            return null;
        }

        int start = 0;
        int end = value.length();
        while (start < end && value.charAt(start) <= ' ') {
            start++;
        }
        while (end > start && value.charAt(end - 1) <= ' ') {
            end--;
        }
        if (start == end) {
            return null;
        }
        if (start == 0 && end == value.length()) {
            return value;
        }
        return value.substring(start, end);
    }
}
