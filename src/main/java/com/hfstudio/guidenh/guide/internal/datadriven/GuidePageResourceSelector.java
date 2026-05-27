package com.hfstudio.guidenh.guide.internal.datadriven;

import java.nio.charset.StandardCharsets;
import java.util.List;

import net.minecraft.client.resources.IResourcePack;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;

public class GuidePageResourceSelector {

    private GuidePageResourceSelector() {}

    public static @Nullable SelectedPageResource select(ResourceLocation sourceId) {
        return select(sourceId, DataDrivenGuideLoader.getActiveResourcePacks());
    }

    public static @Nullable SelectedPageResource select(ResourceLocation sourceId,
        Iterable<? extends IResourcePack> resourcePacks) {
        SelectedPageResource winner = null;
        int winnerPriority = 0;
        boolean winnerPriorityResolved = false;
        int order = 0;
        for (IResourcePack resourcePack : resourcePacks) {
            byte[] bytes = DataDrivenGuideLoader.readBytes(resourcePack, sourceId);
            if (bytes == null) {
                continue;
            }
            int candidateOrder = order++;
            if (winner == null) {
                winner = new SelectedPageResource(sourceId, resourcePack, bytes, 0, candidateOrder);
                continue;
            }
            if (!winnerPriorityResolved) {
                winnerPriority = readLoadPriority(winner.sourceId(), winner.bytes());
                winner = winner.withLoadPriority(winnerPriority);
                winnerPriorityResolved = true;
            }
            int candidatePriority = readLoadPriority(sourceId, bytes);
            SelectedPageResource candidate = new SelectedPageResource(
                sourceId,
                resourcePack,
                bytes,
                candidatePriority,
                candidateOrder);
            if (candidate.shouldReplace(winner)) {
                winner = candidate;
                winnerPriority = candidatePriority;
                winnerPriorityResolved = true;
            }
        }
        if (winner != null && !winnerPriorityResolved) {
            winner = winner.withLoadPriority(readLoadPriority(winner.sourceId(), winner.bytes()));
        }
        return winner;
    }

    public static @Nullable SelectedPageResource selectFirstPresent(Iterable<? extends IResourcePack> resourcePacks,
        ResourceLocation... sourceIds) {
        if (sourceIds == null || sourceIds.length == 0) {
            return null;
        }
        var winners = new SelectedPageResource[sourceIds.length];
        var winnerPriorities = new int[sourceIds.length];
        var winnerPriorityResolved = new boolean[sourceIds.length];
        var orders = new int[sourceIds.length];
        for (IResourcePack resourcePack : resourcePacks) {
            for (int i = 0; i < sourceIds.length; i++) {
                ResourceLocation sourceId = sourceIds[i];
                if (sourceId == null) {
                    continue;
                }
                byte[] bytes = DataDrivenGuideLoader.readBytes(resourcePack, sourceId);
                if (bytes == null) {
                    continue;
                }
                int candidateOrder = orders[i]++;
                if (winners[i] == null) {
                    winners[i] = new SelectedPageResource(sourceId, resourcePack, bytes, 0, candidateOrder);
                    continue;
                }
                if (!winnerPriorityResolved[i]) {
                    winnerPriorities[i] = readLoadPriority(winners[i].sourceId(), winners[i].bytes());
                    winners[i] = winners[i].withLoadPriority(winnerPriorities[i]);
                    winnerPriorityResolved[i] = true;
                }
                int candidatePriority = readLoadPriority(sourceId, bytes);
                SelectedPageResource candidate = new SelectedPageResource(
                    sourceId,
                    resourcePack,
                    bytes,
                    candidatePriority,
                    candidateOrder);
                if (candidate.shouldReplace(winners[i])) {
                    winners[i] = candidate;
                    winnerPriorities[i] = candidatePriority;
                    winnerPriorityResolved[i] = true;
                }
            }
        }
        for (int i = 0; i < winners.length; i++) {
            SelectedPageResource winner = winners[i];
            if (winner != null) {
                if (!winnerPriorityResolved[i]) {
                    winner = winner.withLoadPriority(readLoadPriority(winner.sourceId(), winner.bytes()));
                    winners[i] = winner;
                }
                return winner;
            }
        }
        return null;
    }

    public static @Nullable SelectedPageResource selectFirstPresent(List<IResourcePack> resourcePacks,
        ResourceLocation... sourceIds) {
        return selectFirstPresent((Iterable<? extends IResourcePack>) resourcePacks, sourceIds);
    }

    public static int readLoadPriority(ResourceLocation sourceId, byte[] bytes) {
        String source = new String(bytes, StandardCharsets.UTF_8);
        var frontmatter = PageCompiler.parseFrontmatterFromSource(sourceId, PageCompiler.normalizeLineEndings(source));
        var navigation = frontmatter.navigationEntry();
        return navigation != null ? navigation.loadPriority() : 0;
    }

    @Desugar
    public record SelectedPageResource(ResourceLocation sourceId, IResourcePack resourcePack, byte[] bytes,
        int loadPriority, int order) {

        public boolean shouldReplace(SelectedPageResource previous) {
            return loadPriority > previous.loadPriority()
                || loadPriority == previous.loadPriority() && order > previous.order();
        }

        public SelectedPageResource withLoadPriority(int resolvedLoadPriority) {
            return loadPriority == resolvedLoadPriority ? this
                : new SelectedPageResource(sourceId, resourcePack, bytes, resolvedLoadPriority, order);
        }
    }
}
