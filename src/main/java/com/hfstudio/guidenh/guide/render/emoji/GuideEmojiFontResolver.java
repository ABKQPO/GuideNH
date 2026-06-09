package com.hfstudio.guidenh.guide.render.emoji;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.hfstudio.guidenh.config.ModConfig;

public class GuideEmojiFontResolver {

    public static final int DEFAULT_FONT_SIZE = 32;
    private static final String SAMPLE_EMOJI = "\uD83D\uDE00";
    private static final String[] WINDOWS_FONTS = { "Segoe UI Emoji", "Segoe UI Symbol" };
    private static final String[] MACOS_FONTS = { "Apple Color Emoji" };
    private static final String[] LINUX_FONTS = { "Noto Color Emoji", "Noto Emoji", "EmojiOne Color",
        "Twitter Color Emoji" };
    private static final String[] FALLBACK_FONTS = { "Noto Color Emoji", "Segoe UI Emoji", "Apple Color Emoji",
        "Noto Emoji", "Segoe UI Symbol", "Symbola", "Dialog" };

    private String cachedManualName;
    private Font cachedFont;
    private Set<String> cachedAvailableNames = Set.of();

    public Optional<Font> resolveFont() {
        if (!ModConfig.ui.systemEmojiRendering) {
            return Optional.empty();
        }

        String manualName = normalizeManualName(ModConfig.ui.systemEmojiFontName);
        if (Objects.equals(cachedManualName, manualName) && cachedFont != null) {
            return Optional.of(cachedFont);
        }

        cachedManualName = manualName;
        cachedAvailableNames = availableFontNames();
        cachedFont = resolveConfiguredFont(manualName).or(this::resolvePlatformFont)
            .or(this::resolveFallbackFont)
            .orElse(null);
        return Optional.ofNullable(cachedFont);
    }

    public boolean canDisplay(Font font, String emoji) {
        return font != null && emoji != null && !emoji.isEmpty() && font.canDisplayUpTo(emoji) < 0;
    }

    public String describeCurrentFont() {
        return resolveFont().map(Font::getFontName)
            .orElse("");
    }

    private Optional<Font> resolveConfiguredFont(String manualName) {
        if (manualName.isEmpty()) {
            return Optional.empty();
        }
        return findFont(manualName);
    }

    private Optional<Font> resolvePlatformFont() {
        String osName = System.getProperty("os.name", "")
            .toLowerCase(Locale.ROOT);
        if (osName.contains("win")) {
            return findFirstAvailable(WINDOWS_FONTS);
        }
        if (osName.contains("mac") || osName.contains("darwin")) {
            return findFirstAvailable(MACOS_FONTS);
        }
        return findFirstAvailable(LINUX_FONTS);
    }

    private Optional<Font> resolveFallbackFont() {
        return findFirstAvailable(FALLBACK_FONTS);
    }

    private Optional<Font> findFirstAvailable(String[] names) {
        for (var name : names) {
            var font = findFont(name);
            if (font.isPresent() && canDisplay(font.get(), SAMPLE_EMOJI)) {
                return font;
            }
        }
        return Optional.empty();
    }

    private Optional<Font> findFont(String name) {
        if (GraphicsEnvironment.isHeadless() || name == null || name.isBlank()) {
            return Optional.empty();
        }
        String normalized = normalizeFontName(name);
        if (!cachedAvailableNames.contains(normalized)) {
            return Optional.empty();
        }
        Font font = new Font(name, Font.PLAIN, DEFAULT_FONT_SIZE);
        if (!Objects.equals(normalizeFontName(font.getFontName()), normalized)
            && !Objects.equals(normalizeFontName(font.getFamily()), normalized)) {
            return Optional.empty();
        }
        return Optional.of(font);
    }

    private static Set<String> availableFontNames() {
        if (GraphicsEnvironment.isHeadless()) {
            return Set.of();
        }
        var names = new LinkedHashSet<String>();
        for (var font : GraphicsEnvironment.getLocalGraphicsEnvironment()
            .getAllFonts()) {
            names.add(normalizeFontName(font.getFontName()));
            names.add(normalizeFontName(font.getFamily()));
            names.add(normalizeFontName(font.getName()));
        }
        return Set.copyOf(names);
    }

    private static String normalizeManualName(String name) {
        return name == null ? "" : name.trim();
    }

    private static String normalizeFontName(String name) {
        return name == null ? ""
            : name.trim()
                .toLowerCase(Locale.ROOT);
    }
}
