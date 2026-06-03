package com.hfstudio.guidenh.guide.document.interaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowLink;
import com.hfstudio.guidenh.guide.document.flow.LytSpoilerSpan;

@Desugar
public record FlowInteractionPath(@Nullable LytFlowContent primary, List<LytFlowContent> targets,
    @Nullable LytSpoilerSpan activeSpoiler) {

    private static final FlowInteractionPath EMPTY = new FlowInteractionPath(null, List.of(), null);

    public FlowInteractionPath {
        List<LytFlowContent> normalizedTargets = new ArrayList<>(
            targets != null ? targets.size() + (primary != null ? 1 : 0) : primary != null ? 1 : 0);
        if (primary != null) {
            normalizedTargets.add(primary);
        }
        if (targets != null) {
            for (var target : targets) {
                if (target != null && target != primary && !normalizedTargets.contains(target)) {
                    normalizedTargets.add(target);
                }
            }
        }
        targets = List.copyOf(normalizedTargets);
        if (primary == null && !targets.isEmpty()) {
            primary = targets.getFirst();
        }
        if (activeSpoiler == null) {
            activeSpoiler = findFirstSpoiler(targets);
        }
    }

    public static FlowInteractionPath empty() {
        return EMPTY;
    }

    public boolean isEmpty() {
        return targets.isEmpty();
    }

    public boolean contains(LytFlowContent candidate) {
        return candidate != null && targets.contains(candidate);
    }

    public boolean containsOrAncestors(LytFlowContent candidate) {
        if (candidate == null) {
            return false;
        }
        for (var target : targets) {
            if (target.isInclusiveAncestor(candidate)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsPrimaryOrDescendant(LytFlowContent candidate) {
        return candidate != null && primary != null && primary.isInclusiveAncestor(candidate);
    }

    public @Nullable LytSpoilerSpan firstSpoiler() {
        return activeSpoiler;
    }

    private static @Nullable LytSpoilerSpan findFirstSpoiler(List<LytFlowContent> targets) {
        for (var target : targets) {
            var spoiler = target.findAncestor(LytSpoilerSpan.class);
            if (spoiler != null) {
                return spoiler;
            }
        }
        return null;
    }

    public static FlowInteractionPath fromPrimary(@Nullable LytFlowContent content) {
        if (content == null) {
            return empty();
        }
        List<LytFlowContent> targets = new ArrayList<>(4);
        for (var current = content; current != null; current = current.getFlowParent()) {
            targets.add(current);
        }
        return new FlowInteractionPath(resolvePreferredPrimary(targets, content), targets, findFirstSpoiler(targets));
    }

    private static LytFlowContent resolvePreferredPrimary(List<LytFlowContent> targets, LytFlowContent fallback) {
        for (var target : targets) {
            if (target instanceof LytFlowLink) {
                return target;
            }
        }
        return fallback;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FlowInteractionPath path && Objects.equals(primary, path.primary)
            && Objects.equals(targets, path.targets)
            && Objects.equals(activeSpoiler, path.activeSpoiler);
    }

    @Override
    public int hashCode() {
        return Objects.hash(primary, targets, activeSpoiler);
    }
}
