package com.hfstudio.guidenh.guide.mediawiki;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.minecraft.client.resources.IResourcePack;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.compiler.IdUtils;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.indices.CategoryIndex;
import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.internal.datadriven.DataDrivenGuideLoader;
import com.hfstudio.guidenh.guide.internal.datadriven.GuidePageResourceSelector;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;
import com.hfstudio.guidenh.integration.betterquesting.QuestIndex;
import com.hfstudio.guidenh.libs.unist.UnistPoint;

import cpw.mods.fml.common.FMLLog;

public class MediaWikiSpecialDataIndexer {

    private static final Pattern EXTERNAL_LINK_PATTERN = Pattern.compile("https?://[^\\s)>\\]]+");
    private static final Pattern ATTRIBUTE_RESOURCE_REFERENCE_PATTERN = Pattern
        .compile("(?:src|texture|image|csv|sound|file)\\s*=\\s*[\"']([^\"'#?][^\"']*)[\"']");
    private static final Pattern MARKDOWN_RESOURCE_REFERENCE_PATTERN = Pattern
        .compile("!?\\[[^\\]]*\\]\\(([^)#?\\s][^)]*)\\)");
    private static final String[] ASSET_EXTENSIONS = { ".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg", ".csv",
        ".json", ".mcmeta", ".txt", ".lang", ".ogg", ".mp3", ".wav", ".mermaid" };

    public MediaWikiSpecialDataIndex build(Guide guide, Collection<ParsedGuidePage> pages,
        CategoryIndex categoryIndex) {
        long totalStartNanos = System.nanoTime();
        LinkedHashMap<ResourceLocation, ParsedGuidePage> normalPages = new LinkedHashMap<>();
        if (pages != null) {
            for (ParsedGuidePage page : pages) {
                if (page == null || MediaWikiPageIds.isSyntheticPage(page.getId())) {
                    continue;
                }
                normalPages.put(page.getId(), page);
            }
        }
        long assetStartNanos = System.nanoTime();
        Map<ResourceLocation, Long> assetSizesById = buildAssetSizes(guide);
        Map<ResourceLocation, Set<ResourceLocation>> assetVariantsByReference = buildAssetVariantsByReference(
            assetSizesById.keySet());
        long assetElapsedNanos = System.nanoTime() - assetStartNanos;
        long usageStartNanos = System.nanoTime();
        Map<String, List<ResourceLocation>> fileUsageByPath = buildFileUsage(
            normalPages.values(),
            assetVariantsByReference);
        long usageElapsedNanos = System.nanoTime() - usageStartNanos;

        long metadataStartNanos = System.nanoTime();
        Map<ResourceLocation, List<ResourceLocation>> translations = buildTranslations(normalPages.values());
        Map<ResourceLocation, Set<String>> pageProperties = buildPageProperties(normalPages.values());
        Map<ResourceLocation, List<String>> externalLinks = buildExternalLinks(normalPages.values());
        Map<ResourceLocation, Long> pageSizes = buildPageSizes(normalPages.values());
        Map<ResourceLocation, List<MediaWikiSpecialLintIssue>> lintIssues = buildLintIssues(
            guide,
            normalPages.values(),
            assetVariantsByReference);
        Map<String, List<ResourceLocation>> ambiguousBindings = buildAmbiguousItemBindings(normalPages.values());
        Map<ResourceLocation, List<MediaWikiSpecialOverrideEntry>> overrides = buildOverrides(
            guide,
            normalPages.values());
        Set<String> unusedFiles = buildUnusedFiles(assetSizesById.keySet(), fileUsageByPath.keySet());
        long metadataElapsedNanos = System.nanoTime() - metadataStartNanos;

        MediaWikiSpecialDataIndex dataIndex = new MediaWikiSpecialDataIndex(
            Map.copyOf(normalPages),
            Map.copyOf(translations),
            Map.copyOf(pageProperties),
            Map.copyOf(externalLinks),
            Map.copyOf(pageSizes),
            Map.copyOf(assetSizesById),
            Map.copyOf(fileUsageByPath),
            Map.copyOf(lintIssues),
            Map.copyOf(ambiguousBindings),
            Map.copyOf(overrides),
            Set.copyOf(unusedFiles));
        long totalElapsedNanos = System.nanoTime() - totalStartNanos;
        FMLLog.getLogger()
            .info(
                "[GuideNH] [MediaWikiSpecialDataIndexer] Built special data index for {} pages in {} ms (assets: {} ms, usage: {} ms, metadata: {} ms)",
                normalPages.size(),
                nanosToMillis(totalElapsedNanos),
                nanosToMillis(assetElapsedNanos),
                nanosToMillis(usageElapsedNanos),
                nanosToMillis(metadataElapsedNanos));
        FMLLog.getLogger()
            .info(
                "[GuideNH] [MediaWikiSpecialDataIndexer] Details assets={}, assetAliases={}, fileUsageKeys={}, translations={}, propertyPages={}, externalLinkPages={}, pageSizes={}, lintPages={}, lintIssues={}, ambiguousBindings={}, overrides={}, unusedFiles={}",
                assetSizesById.size(),
                assetVariantsByReference.size(),
                fileUsageByPath.size(),
                translations.size(),
                pageProperties.size(),
                externalLinks.size(),
                pageSizes.size(),
                lintIssues.size(),
                countLintIssues(lintIssues),
                ambiguousBindings.size(),
                overrides.size(),
                unusedFiles.size());
        return dataIndex;
    }

    private long nanosToMillis(long nanos) {
        return nanos / 1_000_000L;
    }

    private Map<ResourceLocation, List<ResourceLocation>> buildTranslations(Collection<ParsedGuidePage> pages) {
        LinkedHashMap<ResourceLocation, List<ResourceLocation>> bySource = new LinkedHashMap<>();
        for (ParsedGuidePage page : pages) {
            if (page == null) {
                continue;
            }
            ResourceLocation sourcePageId = LangUtil.stripLangFromPageId(page.getId());
            bySource.computeIfAbsent(sourcePageId, ignored -> new ArrayList<>())
                .add(page.getId());
        }
        return bySource;
    }

    private Map<ResourceLocation, Set<String>> buildPageProperties(Collection<ParsedGuidePage> pages) {
        LinkedHashMap<ResourceLocation, Set<String>> byPage = new LinkedHashMap<>();
        for (ParsedGuidePage page : pages) {
            if (page == null || page.getFrontmatter() == null) {
                continue;
            }
            byPage.put(
                page.getId(),
                new LinkedHashSet<>(
                    page.getFrontmatter()
                        .additionalProperties()
                        .keySet()));
        }
        return byPage;
    }

    private Map<ResourceLocation, List<String>> buildExternalLinks(Collection<ParsedGuidePage> pages) {
        LinkedHashMap<ResourceLocation, List<String>> linksByPage = new LinkedHashMap<>();
        for (ParsedGuidePage page : pages) {
            if (page == null || page.getSource() == null
                || page.getSource()
                    .isEmpty()) {
                continue;
            }
            Matcher matcher = EXTERNAL_LINK_PATTERN.matcher(page.getSource());
            LinkedHashSet<String> links = new LinkedHashSet<>();
            while (matcher.find()) {
                links.add(matcher.group());
            }
            if (!links.isEmpty()) {
                linksByPage.put(page.getId(), new ArrayList<>(links));
            }
        }
        return linksByPage;
    }

    private Map<ResourceLocation, Long> buildPageSizes(Collection<ParsedGuidePage> pages) {
        LinkedHashMap<ResourceLocation, Long> sizes = new LinkedHashMap<>();
        for (ParsedGuidePage page : pages) {
            if (page != null) {
                sizes.put(
                    page.getId(),
                    page.getSource() != null ? (long) page.getSource()
                        .length() : 0L);
            }
        }
        return sizes;
    }

    private Map<ResourceLocation, Long> buildAssetSizes(Guide guide) {
        LinkedHashMap<ResourceLocation, Long> sizes = new LinkedHashMap<>();
        List<Guide> guides = resolveGuides(guide);
        for (IResourcePack resourcePack : DataDrivenGuideLoader.getActiveResourcePacks()) {
            File resourcePackFile = DataDrivenGuideLoader.getResourcePackFile(resourcePack);
            if (resourcePackFile == null || !resourcePackFile.exists()) {
                continue;
            }
            for (Guide sourceGuide : guides) {
                if (resourcePackFile.isDirectory()) {
                    collectAssetSizesFromDirectory(sourceGuide, resourcePackFile, sizes);
                } else {
                    collectAssetSizesFromZip(sourceGuide, resourcePackFile, sizes);
                }
            }
        }
        return sizes;
    }

    private void collectAssetSizesFromDirectory(Guide guide, File resourcePackRoot, Map<ResourceLocation, Long> sizes) {
        File guideRoot = new File(
            new File(new File(resourcePackRoot, "assets"), guide.getDefaultNamespace()),
            guide.getContentRootFolder());
        if (!guideRoot.isDirectory()) {
            return;
        }
        collectAssetSizesRecursively(guide, guideRoot, guideRoot, sizes);
    }

    private void collectAssetSizesRecursively(Guide guide, File guideRoot, File current,
        Map<ResourceLocation, Long> sizes) {
        File[] children = current.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                collectAssetSizesRecursively(guide, guideRoot, child, sizes);
                continue;
            }
            String relativePath = guideRoot.toPath()
                .relativize(child.toPath())
                .toString()
                .replace(File.separatorChar, '/');
            if (shouldIndexAsset(relativePath)) {
                sizes.putIfAbsent(new ResourceLocation(guide.getDefaultNamespace(), relativePath), child.length());
            }
        }
    }

    private void collectAssetSizesFromZip(Guide guide, File resourcePackFile, Map<ResourceLocation, Long> sizes) {
        String prefix = "assets/" + guide.getDefaultNamespace() + "/" + guide.getContentRootFolder() + "/";
        try (ZipFile zip = new ZipFile(resourcePackFile)) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String path = entry.getName();
                if (!path.startsWith(prefix)) {
                    continue;
                }
                String relativePath = path.substring(prefix.length());
                if (shouldIndexAsset(relativePath)) {
                    sizes.putIfAbsent(new ResourceLocation(guide.getDefaultNamespace(), relativePath), entry.getSize());
                }
            }
        } catch (IOException ignored) {}
    }

    private Map<ResourceLocation, Set<ResourceLocation>> buildAssetVariantsByReference(
        Set<ResourceLocation> knownAssets) {
        LinkedHashMap<ResourceLocation, Set<ResourceLocation>> variantsByReference = new LinkedHashMap<>();
        for (ResourceLocation assetId : knownAssets) {
            if (assetId == null) {
                continue;
            }
            registerAssetVariant(variantsByReference, assetId, assetId);
            ResourceLocation strippedAssetId = LangUtil.stripLangFromPageId(assetId);
            if (!assetId.equals(strippedAssetId)) {
                registerAssetVariant(variantsByReference, strippedAssetId, assetId);
            }
        }
        return variantsByReference;
    }

    private void registerAssetVariant(Map<ResourceLocation, Set<ResourceLocation>> variantsByReference,
        ResourceLocation referenceId, ResourceLocation assetId) {
        variantsByReference.computeIfAbsent(referenceId, ignored -> new LinkedHashSet<>())
            .add(assetId);
    }

    private Map<ResourceLocation, List<MediaWikiSpecialLintIssue>> buildLintIssues(Guide guide,
        Collection<ParsedGuidePage> pages, Map<ResourceLocation, Set<ResourceLocation>> assetVariantsByReference) {
        LinkedHashMap<ResourceLocation, List<MediaWikiSpecialLintIssue>> issues = new LinkedHashMap<>();
        for (ParsedGuidePage page : pages) {
            if (page != null && page.hasParseFailure()) {
                issues.put(
                    page.getId(),
                    List.of(
                        new MediaWikiSpecialLintIssue(
                            page.getParseFailureMessage(),
                            resolveLineNumber(page.getParseFailureFrom()))));
            }
            appendAssetReferenceIssues(issues, page, assetVariantsByReference);
        }
        if (guide instanceof MutableGuide mutableGuide) {
            appendCompileFailures(issues, mutableGuide);
        }
        return issues;
    }

    private void appendCompileFailures(Map<ResourceLocation, List<MediaWikiSpecialLintIssue>> issues,
        MutableGuide mutableGuide) {
        for (Map.Entry<ResourceLocation, MutableGuide.GuidePageFailureView> entry : mutableGuide.getPageFailureViews()
            .entrySet()) {
            MutableGuide.GuidePageFailureView failure = entry.getValue();
            if (failure == null || failure.parseFailure()) {
                continue;
            }
            issues.computeIfAbsent(entry.getKey(), ignored -> new ArrayList<>())
                .add(new MediaWikiSpecialLintIssue(failure.headingText() + ": " + failure.errorText(), null));
        }
    }

    private Integer resolveLineNumber(UnistPoint point) {
        if (point == null || point.line() <= 0) {
            return null;
        }
        return point.line();
    }

    private Map<String, List<ResourceLocation>> buildAmbiguousItemBindings(Collection<ParsedGuidePage> pages) {
        LinkedHashMap<String, List<ResourceLocation>> bindings = new LinkedHashMap<>();
        for (ParsedGuidePage page : pages) {
            if (page == null) {
                continue;
            }
            appendBindings(bindings, page, "item:", readStringValues(page, "item_ids", "item_id"));
            appendBindings(bindings, page, "ore:", readStringValues(page, "ore_ids", "ore_id"));
            appendBindings(bindings, page, "quest:", readStringValues(page, "quest_ids", "quest_id"));
            appendQuestBindings(bindings, page);
        }

        bindings.entrySet()
            .removeIf(
                entry -> entry.getValue() == null || entry.getValue()
                    .size() < 2);
        return bindings;
    }

    private Map<String, List<ResourceLocation>> buildFileUsage(Collection<ParsedGuidePage> pages,
        Map<ResourceLocation, Set<ResourceLocation>> assetVariantsByReference) {
        LinkedHashMap<String, List<ResourceLocation>> usage = new LinkedHashMap<>();
        for (ParsedGuidePage page : pages) {
            if (page == null) {
                continue;
            }
            LinkedHashSet<String> pageAssets = new LinkedHashSet<>();
            for (AssetReference reference : collectAssetReferences(page)) {
                if (reference.assetId() != null) {
                    for (ResourceLocation resolvedAssetId : resolveAssetTargets(
                        reference.assetId(),
                        assetVariantsByReference)) {
                        pageAssets.add(resolvedAssetId.toString());
                    }
                }
            }
            for (String assetKey : pageAssets) {
                usage.computeIfAbsent(assetKey, ignored -> new ArrayList<>())
                    .add(page.getId());
            }
        }
        return usage;
    }

    private Map<ResourceLocation, List<MediaWikiSpecialOverrideEntry>> buildOverrides(Guide guide,
        Collection<ParsedGuidePage> pages) {
        LinkedHashMap<ResourceLocation, List<MediaWikiSpecialOverrideEntry>> overridesByPage = new LinkedHashMap<>();
        List<IResourcePack> activeResourcePacks = DataDrivenGuideLoader.getActiveResourcePacks();
        LinkedHashMap<ResourceLocation, List<PageSourceCandidate>> candidateCache = new LinkedHashMap<>();
        for (ParsedGuidePage page : pages) {
            if (page == null) {
                continue;
            }
            Guide ownerGuide = resolveOwnerGuide(guide, page);
            String defaultLanguage = resolveDefaultLanguage(ownerGuide);
            PageSourceSelection selection = resolveSelectedPageSource(
                ownerGuide,
                page,
                defaultLanguage,
                activeResourcePacks,
                candidateCache);
            if (selection == null || selection.candidates()
                .size() < 2) {
                continue;
            }
            ArrayList<MediaWikiSpecialOverrideEntry> descriptions = new ArrayList<>(
                selection.candidates()
                    .size());
            for (PageSourceCandidate candidate : selection.candidates()) {
                descriptions.add(
                    new MediaWikiSpecialOverrideEntry(
                        candidate.priority(),
                        candidate.sourcePack(),
                        selection.sourceLanguage(),
                        selection.sourceId()
                            .toString()));
            }
            overridesByPage.put(page.getId(), descriptions);
        }
        return overridesByPage;
    }

    private List<Guide> resolveGuides(Guide guide) {
        if (guide instanceof MediaWikiGuideAggregator aggregator) {
            return new ArrayList<Guide>(aggregator.getComponentGuides());
        }
        return List.of(guide);
    }

    private Guide resolveOwnerGuide(Guide guide, ParsedGuidePage page) {
        if (guide instanceof MediaWikiGuideAggregator aggregator && page != null) {
            MutableGuide ownerGuide = aggregator.findOwnerGuide(page.getId());
            if (ownerGuide != null) {
                return ownerGuide;
            }
        }
        return guide;
    }

    private @Nullable PageSourceSelection resolveSelectedPageSource(Guide guide, ParsedGuidePage page,
        String defaultLanguage, List<IResourcePack> resourcePacks,
        Map<ResourceLocation, List<PageSourceCandidate>> candidateCache) {
        if (guide == null || page == null) {
            return null;
        }
        String requestedLanguage = page.getLanguage() != null && !page.getLanguage()
            .trim()
            .isEmpty() ? page.getLanguage() : defaultLanguage;
        for (PageSourceLookup lookup : buildPageSourceLookups(guide, page, requestedLanguage, defaultLanguage)) {
            List<PageSourceCandidate> candidates = candidateCache
                .computeIfAbsent(lookup.sourceId(), sourceId -> collectPageSourceCandidates(sourceId, resourcePacks));
            if (!candidates.isEmpty()) {
                return new PageSourceSelection(lookup.sourceId(), lookup.sourceLanguage(), candidates);
            }
        }
        return null;
    }

    private List<PageSourceLookup> buildPageSourceLookups(Guide guide, ParsedGuidePage page, String requestedLanguage,
        String defaultLanguage) {
        ArrayList<PageSourceLookup> lookups = new ArrayList<>(3);
        String pagePath = page.getId()
            .getResourcePath();
        String namespace = page.getId()
            .getResourceDomain();
        String folder = guide.getContentRootFolder();
        appendPageSourceLookup(lookups, namespace, folder, requestedLanguage, pagePath);
        if (defaultLanguage != null && !defaultLanguage.isEmpty()
            && !defaultLanguage.equalsIgnoreCase(requestedLanguage)) {
            appendPageSourceLookup(lookups, namespace, folder, defaultLanguage, pagePath);
        }
        lookups.add(new PageSourceLookup(new ResourceLocation(namespace, folder + "/" + pagePath), "raw"));
        return lookups;
    }

    private void appendPageSourceLookup(List<PageSourceLookup> lookups, String namespace, String folder,
        String language, String pagePath) {
        if (language == null || language.trim()
            .isEmpty()) {
            return;
        }
        lookups.add(
            new PageSourceLookup(new ResourceLocation(namespace, folder + "/_" + language + "/" + pagePath), language));
    }

    private List<PageSourceCandidate> collectPageSourceCandidates(ResourceLocation sourceId,
        List<IResourcePack> resourcePacks) {
        ArrayList<PageSourceCandidate> candidates = new ArrayList<>();
        int order = 0;
        for (IResourcePack resourcePack : resourcePacks) {
            byte[] bytes = DataDrivenGuideLoader.readBytes(resourcePack, sourceId);
            if (bytes == null) {
                continue;
            }
            candidates.add(
                new PageSourceCandidate(
                    resourcePack.getPackName(),
                    GuidePageResourceSelector.readLoadPriority(sourceId, bytes),
                    order++));
        }
        candidates.sort(
            (left, right) -> left.priority() != right.priority() ? Integer.compare(right.priority(), left.priority())
                : Integer.compare(right.order(), left.order()));
        return candidates;
    }

    private String resolveDefaultLanguage(Guide guide) {
        if (guide instanceof MutableGuide mutableGuide) {
            return mutableGuide.getDefaultLanguage();
        }
        return "en_us";
    }

    private Set<String> buildUnusedFiles(Set<ResourceLocation> assets, Set<String> usedAssetKeys) {
        LinkedHashSet<String> unused = new LinkedHashSet<>();
        for (ResourceLocation asset : assets) {
            if (asset == null) {
                continue;
            }
            String key = asset.toString();
            if (!usedAssetKeys.contains(key)) {
                unused.add(key);
            }
        }
        return unused;
    }

    private void appendBindings(Map<String, List<ResourceLocation>> bindings, ParsedGuidePage page, String prefix,
        List<String> ids) {
        for (String id : ids) {
            if (id == null || id.trim()
                .isEmpty()) {
                continue;
            }
            bindings.computeIfAbsent(prefix + id.trim(), ignored -> new ArrayList<>())
                .add(page.getId());
        }
    }

    private void appendQuestBindings(Map<String, List<ResourceLocation>> bindings, ParsedGuidePage page) {
        for (var questAnchor : QuestIndex.getQuestAnchors(page)) {
            bindings.computeIfAbsent("quest:" + questAnchor.getLeft(), ignored -> new ArrayList<>())
                .add(
                    questAnchor.getRight()
                        .pageId());
        }
    }

    private List<String> readStringValues(ParsedGuidePage page, String... keys) {
        if (page.getFrontmatter() == null || keys == null || keys.length == 0) {
            return List.of();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String key : keys) {
            if (key == null || key.trim()
                .isEmpty()) {
                continue;
            }
            Object value = page.getFrontmatter()
                .additionalProperties()
                .get(key);
            if (value instanceof List<?>values) {
                for (Object entry : values) {
                    if (entry instanceof String text && !text.trim()
                        .isEmpty()) {
                        result.add(text.trim());
                    }
                }
                continue;
            }
            if (value instanceof String text && !text.trim()
                .isEmpty()) {
                result.add(text.trim());
            }
        }
        return result.isEmpty() ? List.of() : new ArrayList<>(result);
    }

    private @Nullable ResourceLocation tryResolveAssetId(ParsedGuidePage page, String rawPath) {
        if (rawPath == null) {
            return null;
        }
        String trimmed = rawPath.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("http://")
            || trimmed.startsWith("https://")
            || trimmed.startsWith("#")) {
            return null;
        }
        if (!shouldIndexAsset(trimmed)) {
            return null;
        }
        return IdUtils.resolveLink(trimmed, page.getId());
    }

    private boolean shouldIndexAsset(String path) {
        String lowered = path.toLowerCase();
        for (String extension : ASSET_EXTENSIONS) {
            if (lowered.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    private void appendAssetReferenceIssues(Map<ResourceLocation, List<MediaWikiSpecialLintIssue>> issues,
        ParsedGuidePage page, Map<ResourceLocation, Set<ResourceLocation>> assetVariantsByReference) {
        if (page == null || assetVariantsByReference == null) {
            return;
        }
        LinkedHashSet<String> reported = new LinkedHashSet<>();
        for (AssetReference reference : collectAssetReferences(page)) {
            if (!reported.add(reference.uniqueKey())) {
                continue;
            }
            if (reference.assetId() == null) {
                appendLintIssue(
                    issues,
                    page.getId(),
                    GuidebookText.MediaWikiLintInvalidAssetReference.text(reference.rawPath()),
                    reference.lineNumber());
                continue;
            }
            if (resolveAssetTargets(reference.assetId(), assetVariantsByReference).isEmpty()) {
                appendLintIssue(
                    issues,
                    page.getId(),
                    GuidebookText.MediaWikiLintMissingAssetFile.text(reference.assetId()),
                    reference.lineNumber());
            }
        }
    }

    private void appendLintIssue(Map<ResourceLocation, List<MediaWikiSpecialLintIssue>> issues, ResourceLocation pageId,
        String message, @Nullable Integer lineNumber) {
        issues.computeIfAbsent(pageId, ignored -> new ArrayList<>())
            .add(new MediaWikiSpecialLintIssue(message, lineNumber));
    }

    private Set<ResourceLocation> resolveAssetTargets(@Nullable ResourceLocation referenceAssetId,
        Map<ResourceLocation, Set<ResourceLocation>> assetVariantsByReference) {
        if (referenceAssetId == null || assetVariantsByReference == null) {
            return Set.of();
        }
        Set<ResourceLocation> matches = assetVariantsByReference.get(referenceAssetId);
        return matches != null ? matches : Set.of();
    }

    private List<AssetReference> collectAssetReferences(ParsedGuidePage page) {
        if (page == null || page.getSource() == null
            || page.getSource()
                .isEmpty()) {
            return List.of();
        }
        ArrayList<AssetReference> references = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        int[] lineStartOffsets = buildLineStartOffsets(page.getSource());
        collectAssetReferences(
            page,
            page.getSource(),
            lineStartOffsets,
            ATTRIBUTE_RESOURCE_REFERENCE_PATTERN,
            references,
            seen);
        collectAssetReferences(
            page,
            page.getSource(),
            lineStartOffsets,
            MARKDOWN_RESOURCE_REFERENCE_PATTERN,
            references,
            seen);
        return references;
    }

    private void collectAssetReferences(ParsedGuidePage page, String source, int[] lineStartOffsets, Pattern pattern,
        List<AssetReference> references, Set<String> seen) {
        Matcher matcher = pattern.matcher(source);
        while (matcher.find()) {
            String rawPath = matcher.group(1);
            if (rawPath == null) {
                continue;
            }
            String trimmed = rawPath.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("http://")
                || trimmed.startsWith("https://")
                || trimmed.startsWith("#")
                || !shouldIndexAsset(trimmed)) {
                continue;
            }
            Integer lineNumber = resolveLineNumber(lineStartOffsets, matcher.start(1));
            ResourceLocation assetId = null;
            boolean valid = true;
            try {
                assetId = tryResolveAssetId(page, trimmed);
            } catch (IllegalArgumentException ignored) {
                valid = false;
            }
            String dedupeKey = (assetId != null ? assetId.toString() : trimmed) + "@" + lineNumber;
            if (seen.add(dedupeKey)) {
                references.add(new AssetReference(trimmed, assetId, lineNumber, valid));
            }
        }
    }

    private int[] buildLineStartOffsets(String source) {
        int lineCount = 1;
        for (int i = 0; i < source.length(); i++) {
            if (source.charAt(i) == '\n') {
                lineCount++;
            }
        }

        int[] offsets = new int[lineCount];
        offsets[0] = 0;
        int lineIndex = 1;
        for (int i = 0; i < source.length(); i++) {
            if (source.charAt(i) == '\n') {
                offsets[lineIndex++] = i + 1;
            }
        }
        return offsets;
    }

    private @Nullable Integer resolveLineNumber(int[] lineStartOffsets, int offset) {
        if (lineStartOffsets == null || offset < 0) {
            return null;
        }
        int lineIndex = Arrays.binarySearch(lineStartOffsets, offset);
        if (lineIndex >= 0) {
            return lineIndex + 1;
        }
        int insertionPoint = -lineIndex - 1;
        return insertionPoint > 0 ? insertionPoint : 1;
    }

    private int countLintIssues(Map<ResourceLocation, List<MediaWikiSpecialLintIssue>> lintIssues) {
        int total = 0;
        for (List<MediaWikiSpecialLintIssue> issues : lintIssues.values()) {
            if (issues != null) {
                total += issues.size();
            }
        }
        return total;
    }

    @Desugar
    private record PageSourceLookup(ResourceLocation sourceId, String sourceLanguage) {}

    @Desugar
    private record PageSourceCandidate(String sourcePack, int priority, int order) {}

    @Desugar
    private record PageSourceSelection(ResourceLocation sourceId, String sourceLanguage,
        List<PageSourceCandidate> candidates) {}

    @Desugar
    private record AssetReference(String rawPath, @Nullable ResourceLocation assetId, @Nullable Integer lineNumber,
        boolean valid) {

        private String uniqueKey() {
            return (assetId != null ? assetId.toString() : rawPath) + "#" + lineNumber + "#" + valid;
        }
    }
}
