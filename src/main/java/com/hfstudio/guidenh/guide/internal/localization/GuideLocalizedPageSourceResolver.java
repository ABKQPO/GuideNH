package com.hfstudio.guidenh.guide.internal.localization;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;

public class GuideLocalizedPageSourceResolver {

    private static final String PAGE_LANG_KEY_PREFIX = "guidenh.page.";

    private GuideLocalizedPageSourceResolver() {}

    public static ParsedGuidePage parse(String sourcePack, String language, String contentRootFolder,
        ResourceLocation pageId, byte[] fileBytes) {
        return parse(sourcePack, language, contentRootFolder, pageId, fileBytes, null);
    }

    public static ParsedGuidePage parse(String sourcePack, String language, String contentRootFolder,
        ResourceLocation pageId, byte[] fileBytes, @Nullable String localizedSourceOverride) {
        return PageCompiler.parse(
            sourcePack,
            language,
            pageId,
            resolve(language, contentRootFolder, pageId, fileBytes, localizedSourceOverride).source());
    }

    /**
     * Lightweight variant that extracts only frontmatter during reload.
     * Full Micromark AST parse is deferred to first {@link ParsedGuidePage#getAstRoot()}.
     */
    public static ParsedGuidePage parseFrontmatterOnly(String sourcePack, String language, String contentRootFolder,
        ResourceLocation pageId, byte[] fileBytes) {
        return PageCompiler.parseFrontmatterOnly(
            sourcePack,
            language,
            pageId,
            resolve(language, contentRootFolder, pageId, fileBytes, null).source());
    }

    public static ParsedGuidePage parse(String sourcePack, String language, ResourceLocation pageId,
        ResolvedGuidePageSource resolvedSource) {
        return PageCompiler.parse(sourcePack, language, pageId, resolvedSource.source());
    }

    public static ResolvedGuidePageSource resolve(String language, String contentRootFolder, ResourceLocation pageId,
        byte[] fileBytes) {
        return resolve(language, contentRootFolder, pageId, fileBytes, null);
    }

    public static ResolvedGuidePageSource resolve(String language, String contentRootFolder, ResourceLocation pageId,
        byte[] fileBytes, @Nullable String localizedSourceOverride) {
        String langKey = buildLangKey(contentRootFolder, pageId);
        String localizedSource = hasText(localizedSourceOverride) ? decodeNewlines(localizedSourceOverride)
            : findLocalizedPageSource(langKey, language);
        if (localizedSource == null || localizedSource.isEmpty()) {
            return new ResolvedGuidePageSource(new String(fileBytes, StandardCharsets.UTF_8), false, null);
        }
        return new ResolvedGuidePageSource(localizedSource, true, langKey);
    }

    public static String buildLangKey(String contentRootFolder, ResourceLocation pageId) {
        String pagePath = stripMarkdownExtension(pageId.getResourcePath());
        StringBuilder builder = new StringBuilder(
            PAGE_LANG_KEY_PREFIX.length() + pageId.getResourceDomain()
                .length() + contentRootFolder.length() + pagePath.length() * 2 + 8);
        builder.append(PAGE_LANG_KEY_PREFIX);
        appendEscapedSegment(builder, pageId.getResourceDomain());
        builder.append('.');
        appendEscapedPath(builder, contentRootFolder);
        builder.append('.');
        appendEscapedPath(builder, pagePath);
        return builder.toString();
    }

    private static @Nullable String findLocalizedPageSource(String langKey, String language) {
        String normalizedLanguage = LangUtil.normalizeLanguage(language);
        String localized = GuidePageLanguageIndex.getValue(normalizedLanguage, langKey);
        if (hasText(localized)) {
            return decodeNewlines(localized);
        }
        return null;
    }

    private static boolean hasText(@Nullable String text) {
        return text != null && !text.isEmpty();
    }

    private static String stripMarkdownExtension(String pagePath) {
        return pagePath.endsWith(".md") ? pagePath.substring(0, pagePath.length() - 3) : pagePath;
    }

    private static void appendEscapedPath(StringBuilder builder, String path) {
        int segmentStart = 0;
        boolean firstSegment = true;
        while (segmentStart <= path.length()) {
            int separator = path.indexOf('/', segmentStart);
            int segmentEnd = separator >= 0 ? separator : path.length();
            if (segmentEnd > segmentStart) {
                if (!firstSegment) {
                    builder.append('.');
                }
                appendEscapedSegment(builder, path.substring(segmentStart, segmentEnd));
                firstSegment = false;
            }
            if (separator < 0) {
                return;
            }
            segmentStart = separator + 1;
        }
    }

    private static void appendEscapedSegment(StringBuilder builder, String segment) {
        for (int i = 0; i < segment.length(); i++) {
            char ch = segment.charAt(i);
            if (isSafeKeyCharacter(ch)) {
                builder.append(ch);
            } else {
                builder.append("_x")
                    .append(
                        Integer.toHexString(ch)
                            .toLowerCase(Locale.ROOT))
                    .append('_');
            }
        }
    }

    private static boolean isSafeKeyCharacter(char ch) {
        return ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' || ch >= '0' && ch <= '9' || ch == '-';
    }

    private static String decodeNewlines(String source) {
        int slashIndex = source.indexOf('\\');
        if (slashIndex < 0) {
            return source;
        }

        StringBuilder builder = new StringBuilder(source.length());
        builder.append(source, 0, slashIndex);
        boolean escaping = false;
        for (int i = slashIndex; i < source.length(); i++) {
            char ch = source.charAt(i);
            if (!escaping) {
                if (ch == '\\') {
                    escaping = true;
                    continue;
                }
                builder.append(ch);
                continue;
            }

            switch (ch) {
                case 'n':
                    builder.append('\n');
                    break;
                case 'r':
                    builder.append('\r');
                    break;
                case '\\':
                    builder.append('\\');
                    break;
                default:
                    builder.append('\\')
                        .append(ch);
                    break;
            }
            escaping = false;
        }
        if (escaping) {
            builder.append('\\');
        }
        return builder.toString();
    }

    @Desugar
    public record ResolvedGuidePageSource(String source, boolean localized, @Nullable String langKey) {}
}
