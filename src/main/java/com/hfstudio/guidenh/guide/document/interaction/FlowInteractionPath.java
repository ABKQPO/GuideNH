package com.hfstudio.guidenh.guide.document.interaction;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowLink;
import com.hfstudio.guidenh.guide.document.flow.LytSpoilerSpan;

@Desugar
public record FlowInteractionPath(@Nullable LytFlowContent primary, List<LytFlowContent> targets) {

    private static final FlowInteractionPath EMPTY = new FlowInteractionPath(null, List.of());

    public FlowInteractionPath {
        Set<LytFlowContent> deduplicatedTargets = new LinkedHashSet<>();
        if (targets != null) {
            for (var target : targets) {
                if (target != null) {
                    deduplicatedTargets.add(target);
                }
            }
        }
        if (primary != null) {
            deduplicatedTargets.remove(primary);
        }
        List<LytFlowContent> normalizedTargets = new ArrayList<>(
            deduplicatedTargets.size() + (primary != null ? 1 : 0));
        if (primary != null) {
            normalizedTargets.add(primary);
        }
        normalizedTargets.addAll(deduplicatedTargets);
        targets = List.copyOf(normalizedTargets);
        if (primary == null && !targets.isEmpty()) {
            primary = targets.getFirst();
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
        return new FlowInteractionPath(resolvePreferredPrimary(targets, content), targets);
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
            && Objects.equals(targets, path.targets);
    }

    @Override
    public int hashCode() {
        return Objects.hash(primary, targets);
    }
}
