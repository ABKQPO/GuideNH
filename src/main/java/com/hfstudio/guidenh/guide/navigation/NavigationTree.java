package com.hfstudio.guidenh.guide.navigation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import net.minecraft.util.ResourceLocation;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.PageCollection;
import com.hfstudio.guidenh.guide.compiler.FrontmatterNavigation;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.internal.util.NavigationUtil;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Loader;

public class NavigationTree {

    private final Map<ResourceLocation, NavigationNode> nodeIndex;
    private final Map<ResourceLocation, ResourceLocation> parentIndex;

    private final List<NavigationNode> rootNodes;

    public NavigationTree(Map<ResourceLocation, NavigationNode> nodeIndex,
        Map<ResourceLocation, ResourceLocation> parentIndex, List<NavigationNode> rootNodes) {
        this.nodeIndex = nodeIndex;
        this.parentIndex = parentIndex;
        this.rootNodes = rootNodes;
    }

    public NavigationTree() {
        this.nodeIndex = Collections.emptyMap();
        this.parentIndex = Collections.emptyMap();
        this.rootNodes = Collections.emptyList();
    }

    public List<NavigationNode> getRootNodes() {
        return rootNodes;
    }

    @Nullable
    public NavigationNode getNodeById(ResourceLocation pageId) {
        return nodeIndex.get(pageId);
    }

    @Nullable
    public ResourceLocation getParentId(ResourceLocation pageId) {
        return parentIndex.get(pageId);
    }

    public List<NavigationNode> getPathTo(ResourceLocation pageId) {
        var path = new ArrayList<NavigationNode>();
        var currentPageId = pageId;
        while (currentPageId != null) {
            var node = nodeIndex.get(currentPageId);
            if (node == null) {
                break;
            }
            path.add(node);
            currentPageId = parentIndex.get(currentPageId);
        }
        Collections.reverse(path);
        return path;
    }

    public static NavigationTree build(Collection<ParsedGuidePage> pages) {
        return build(null, pages);
    }

    public static NavigationTree build(@Nullable PageCollection pageCollection, Collection<ParsedGuidePage> pages) {
        var pagesWithChildren = new HashMap<ResourceLocation, Pair<ParsedGuidePage, List<ParsedGuidePage>>>();

        // First pass, build a map of pages and their children
        for (var page : pages) {
            var navigationEntry = page.getFrontmatter()
                .navigationEntry();
            if (navigationEntry == null) {
                continue;
            }
            if (!areModRequirementsMet(navigationEntry)) {
                continue;
            }

            // Create an entry for this page to collect any children it might have
            pagesWithChildren.compute(
                page.getId(),
                (identifier, previousPair) -> previousPair != null ? Pair.of(page, previousPair.getRight())
                    : Pair.of(page, new ArrayList<>()));

            // Add this page to the collected children of the parent page (if any)
            var parentId = navigationEntry.parent();
            if (parentId != null) {
                pagesWithChildren.compute(parentId, (identifier, prevPage) -> {
                    if (prevPage != null) {
                        prevPage.getRight()
                            .add(page);
                        return prevPage;
                    } else {
                        var children = new ArrayList<ParsedGuidePage>();
                        children.add(page);
                        return Pair.of(null, children);
                    }
                });
            }
        }

        var nodeIndex = new HashMap<ResourceLocation, NavigationNode>(pages.size());
        var parentIndex = new HashMap<ResourceLocation, ResourceLocation>(pages.size());
        var rootNodes = new ArrayList<NavigationNode>();

        for (var entry : pagesWithChildren.entrySet()) {
            createNode(
                nodeIndex,
                parentIndex,
                rootNodes,
                pagesWithChildren,
                pageCollection,
                entry.getKey(),
                entry.getValue(),
                new HashSet<>());
        }

        // Sort root nodes
        rootNodes.sort(NODE_COMPARATOR);

        return new NavigationTree(
            Collections.unmodifiableMap(new HashMap<>(nodeIndex)),
            Collections.unmodifiableMap(new HashMap<>(parentIndex)),
            Collections.unmodifiableList(new ArrayList<>(rootNodes)));
    }

    public static NavigationTree buildMerged(Collection<MutableGuide> guides) {
        if (guides == null || guides.isEmpty()) {
            return new NavigationTree();
        }

        var pagesWithChildren = new HashMap<ResourceLocation, Pair<ParsedGuidePage, List<ParsedGuidePage>>>();
        var pageCollectionsById = new HashMap<ResourceLocation, PageCollection>();
        for (var guide : guides) {
            if (guide == null) {
                continue;
            }
            for (var page : guide.getPages()) {
                var navigationEntry = page.getFrontmatter()
                    .navigationEntry();
                if (navigationEntry == null || !areModRequirementsMet(navigationEntry)) {
                    continue;
                }

                pageCollectionsById.put(page.getId(), guide);
                pagesWithChildren.compute(
                    page.getId(),
                    (identifier, previousPair) -> previousPair != null ? Pair.of(page, previousPair.getRight())
                        : Pair.of(page, new ArrayList<>()));

                var parentId = navigationEntry.parent();
                if (parentId != null) {
                    pagesWithChildren.compute(parentId, (identifier, prevPage) -> {
                        if (prevPage != null) {
                            prevPage.getRight()
                                .add(page);
                            return prevPage;
                        }
                        var children = new ArrayList<ParsedGuidePage>();
                        children.add(page);
                        return Pair.of(null, children);
                    });
                }
            }
        }

        var nodeIndex = new HashMap<ResourceLocation, NavigationNode>(pagesWithChildren.size());
        var parentIndex = new HashMap<ResourceLocation, ResourceLocation>(pagesWithChildren.size());
        var rootNodes = new ArrayList<NavigationNode>();
        for (var entry : pagesWithChildren.entrySet()) {
            createMergedNode(
                nodeIndex,
                parentIndex,
                rootNodes,
                pagesWithChildren,
                pageCollectionsById,
                entry.getKey(),
                entry.getValue(),
                new HashSet<>());
        }

        rootNodes.sort(NODE_COMPARATOR);
        return new NavigationTree(
            Collections.unmodifiableMap(new HashMap<>(nodeIndex)),
            Collections.unmodifiableMap(new HashMap<>(parentIndex)),
            Collections.unmodifiableList(new ArrayList<>(rootNodes)));
    }

    public static NavigationTree buildMergedPages(
        Map<ResourceLocation, ? extends PageCollection> pageCollectionsByPageId, Collection<ParsedGuidePage> pages) {
        if (pageCollectionsByPageId == null || pageCollectionsByPageId.isEmpty() || pages == null || pages.isEmpty()) {
            return new NavigationTree();
        }

        var pagesWithChildren = new HashMap<ResourceLocation, Pair<ParsedGuidePage, List<ParsedGuidePage>>>();
        for (var page : pages) {
            if (page == null) {
                continue;
            }

            var navigationEntry = page.getFrontmatter()
                .navigationEntry();
            if (navigationEntry == null || !areModRequirementsMet(navigationEntry)) {
                continue;
            }

            pagesWithChildren.compute(
                page.getId(),
                (identifier, previousPair) -> previousPair != null ? Pair.of(page, previousPair.getRight())
                    : Pair.of(page, new ArrayList<>()));

            var parentId = navigationEntry.parent();
            if (parentId != null) {
                pagesWithChildren.compute(parentId, (identifier, prevPage) -> {
                    if (prevPage != null) {
                        prevPage.getRight()
                            .add(page);
                        return prevPage;
                    }
                    var children = new ArrayList<ParsedGuidePage>();
                    children.add(page);
                    return Pair.of(null, children);
                });
            }
        }

        var nodeIndex = new HashMap<ResourceLocation, NavigationNode>(pagesWithChildren.size());
        var parentIndex = new HashMap<ResourceLocation, ResourceLocation>(pagesWithChildren.size());
        var rootNodes = new ArrayList<NavigationNode>();
        var resolvedPageCollections = new HashMap<ResourceLocation, PageCollection>(pageCollectionsByPageId.size());
        resolvedPageCollections.putAll(pageCollectionsByPageId);
        for (var entry : pagesWithChildren.entrySet()) {
            createMergedNode(
                nodeIndex,
                parentIndex,
                rootNodes,
                pagesWithChildren,
                resolvedPageCollections,
                entry.getKey(),
                entry.getValue(),
                new HashSet<>());
        }

        rootNodes.sort(NODE_COMPARATOR);
        return new NavigationTree(
            Collections.unmodifiableMap(new HashMap<>(nodeIndex)),
            Collections.unmodifiableMap(new HashMap<>(parentIndex)),
            Collections.unmodifiableList(new ArrayList<>(rootNodes)));
    }

    @Nullable
    public static NavigationNode createNode(Map<ResourceLocation, NavigationNode> nodeIndex,
        Map<ResourceLocation, ResourceLocation> parentIndex, List<NavigationNode> rootNodes,
        Map<ResourceLocation, Pair<ParsedGuidePage, List<ParsedGuidePage>>> pagesWithChildren,
        @Nullable PageCollection pageCollection, ResourceLocation pageId,
        Pair<ParsedGuidePage, List<ParsedGuidePage>> entry, Set<ResourceLocation> parents) {
        var existingNode = nodeIndex.get(pageId);
        if (existingNode != null) {
            return existingNode;
        }

        if (!parents.add(pageId)) {
            FMLLog.getLogger()
                .error(
                    "[GuideNH] [NavigationTree] Detected a cycle in the navigation tree parent-child relationship for page {}",
                    pageId);
            return null;
        }

        var page = entry.getKey();
        var children = entry.getRight();

        if (page == null) {
            // These children had a parent that doesn't exist
            FMLLog.getLogger()
                .error("[GuideNH] [NavigationTree] Pages {} had unknown navigation parent {}", children, pageId);
            return null;
        }

        var navigationEntry = Objects.requireNonNull(
            page.getFrontmatter()
                .navigationEntry(),
            "navigation frontmatter");

        // Construct the icon if set
        var icon = NavigationUtil.createNavigationIcon(page, pageCollection);

        var childNodes = new ArrayList<NavigationNode>(children.size());
        for (var childPage : children) {
            var childPageEntry = pagesWithChildren.get(childPage.getId());

            var childNode = createNode(
                nodeIndex,
                parentIndex,
                rootNodes,
                pagesWithChildren,
                pageCollection,
                childPage.getId(),
                childPageEntry,
                parents);
            if (childNode != null) {
                childNodes.add(childNode);
            }
        }
        childNodes.sort(NODE_COMPARATOR);

        var node = new NavigationNode(
            pageCollection != null ? pageCollection.getId() : null,
            page.getId(),
            navigationEntry.title(),
            icon,
            childNodes,
            navigationEntry.position(),
            true);
        nodeIndex.put(page.getId(), node);
        if (navigationEntry.parent() == null) {
            rootNodes.add(node);
        } else {
            parentIndex.put(page.getId(), navigationEntry.parent());
        }
        return node;
    }

    public static final Comparator<NavigationNode> NODE_COMPARATOR = Comparator.comparingInt(NavigationNode::position)
        .reversed()
        .thenComparing(NavigationNode::title);

    @Nullable
    private static NavigationNode createMergedNode(Map<ResourceLocation, NavigationNode> nodeIndex,
        Map<ResourceLocation, ResourceLocation> parentIndex, List<NavigationNode> rootNodes,
        Map<ResourceLocation, Pair<ParsedGuidePage, List<ParsedGuidePage>>> pagesWithChildren,
        Map<ResourceLocation, PageCollection> pageCollectionsByPageId, ResourceLocation pageId,
        Pair<ParsedGuidePage, List<ParsedGuidePage>> entry, Set<ResourceLocation> parents) {
        var existingNode = nodeIndex.get(pageId);
        if (existingNode != null) {
            return existingNode;
        }

        if (!parents.add(pageId)) {
            FMLLog.getLogger()
                .error(
                    "[GuideNH] [NavigationTree] Detected a cycle in the merged navigation tree parent-child relationship for page {}",
                    pageId);
            return null;
        }

        var page = entry.getKey();
        var children = entry.getRight();
        if (page == null) {
            FMLLog.getLogger()
                .error("[GuideNH] [NavigationTree] Pages {} had unknown merged navigation parent {}", children, pageId);
            return null;
        }

        var navigationEntry = Objects.requireNonNull(
            page.getFrontmatter()
                .navigationEntry(),
            "navigation frontmatter");
        var pageCollection = pageCollectionsByPageId.get(page.getId());
        var icon = NavigationUtil.createNavigationIcon(page, pageCollection);

        var childNodes = new ArrayList<NavigationNode>(children.size());
        for (var childPage : children) {
            var childPageEntry = pagesWithChildren.get(childPage.getId());
            var childNode = createMergedNode(
                nodeIndex,
                parentIndex,
                rootNodes,
                pagesWithChildren,
                pageCollectionsByPageId,
                childPage.getId(),
                childPageEntry,
                parents);
            if (childNode != null) {
                childNodes.add(childNode);
            }
        }
        childNodes.sort(NODE_COMPARATOR);

        var node = new NavigationNode(
            pageCollection != null ? pageCollection.getId() : null,
            page.getId(),
            navigationEntry.title(),
            icon,
            childNodes,
            navigationEntry.position(),
            true);
        nodeIndex.put(page.getId(), node);
        if (navigationEntry.parent() == null) {
            rootNodes.add(node);
        } else {
            parentIndex.put(page.getId(), navigationEntry.parent());
        }
        return node;
    }

    /**
     * Returns true when all mods listed in the navigation entry's {@code requiredMods} are currently
     * loaded, or when no requirements are declared. Returns false when at least one required mod is
     * absent, meaning the page should be excluded from the navigation tree and all page indices.
     */
    public static boolean areModRequirementsMet(@Nullable FrontmatterNavigation nav) {
        if (nav == null) {
            return true;
        }
        var required = nav.requiredMods();
        if (required == null || required.isEmpty()) {
            return true;
        }
        for (var modId : required) {
            if (!Loader.isModLoaded(modId)) {
                return false;
            }
        }
        return true;
    }

}
