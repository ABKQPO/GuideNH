package com.hfstudio.guidenh.guide.document.interaction;

import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.flow.LytSpoilerSpan;

@Desugar
public record DocumentInteractionSnapshot(@Nullable LytNode node, FlowInteractionPath flowPath,
    @Nullable LytFlowContent primaryHoverTarget, @Nullable LytFlowContent primaryClickTarget,
    List<LytFlowContent> tooltipTargets, List<LytFlowContent> revealTargets, @Nullable LytSpoilerSpan activeSpoiler) {

    private static final DocumentInteractionSnapshot EMPTY = new DocumentInteractionSnapshot(
        null,
        FlowInteractionPath.empty(),
        null,
        null,
        List.of(),
        List.of(),
        null);

    public DocumentInteractionSnapshot {
        flowPath = flowPath != null ? flowPath : FlowInteractionPath.empty();
        tooltipTargets = List.copyOf(tooltipTargets != null ? tooltipTargets : List.of());
        revealTargets = List.copyOf(revealTargets != null ? revealTargets : List.of());
        if (primaryHoverTarget == null) {
            primaryHoverTarget = flowPath.primary();
        }
        if (primaryClickTarget == null) {
            primaryClickTarget = primaryHoverTarget;
        }
        if (activeSpoiler == null) {
            activeSpoiler = flowPath.firstSpoiler();
        }
    }

    public static DocumentInteractionSnapshot empty() {
        return EMPTY;
    }

    public boolean isEmpty() {
        return node == null && flowPath.isEmpty();
    }

    public boolean containsHover(LytFlowContent content) {
        return flowPath.containsOrAncestors(content);
    }

    public boolean containsReveal(LytFlowContent content) {
        if (content == null) {
            return false;
        }
        for (var revealTarget : revealTargets) {
            if (revealTarget != null && revealTarget.isInclusiveAncestor(content)) {
                return true;
            }
        }
        return false;
    }

    public List<LytFlowContent> interactiveFlowTargets() {
        if (flowPath.isEmpty()) {
            return List.of();
        }
        List<LytFlowContent> interactiveTargets = new java.util.ArrayList<>(
            flowPath.targets()
                .size());
        for (var target : flowPath.targets()) {
            if (target instanceof InteractiveElement) {
                interactiveTargets.add(target);
            }
        }
        return interactiveTargets.isEmpty() ? List.of() : List.copyOf(interactiveTargets);
    }

    public @Nullable LytFlowContent firstInteractiveFlowTarget() {
        for (var target : flowPath.targets()) {
            if (target instanceof InteractiveElement) {
                return target;
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof DocumentInteractionSnapshot snapshot && Objects.equals(node, snapshot.node)
            && Objects.equals(flowPath, snapshot.flowPath)
            && Objects.equals(primaryHoverTarget, snapshot.primaryHoverTarget)
            && Objects.equals(primaryClickTarget, snapshot.primaryClickTarget)
            && Objects.equals(tooltipTargets, snapshot.tooltipTargets)
            && Objects.equals(revealTargets, snapshot.revealTargets)
            && Objects.equals(activeSpoiler, snapshot.activeSpoiler);
    }

    @Override
    public int hashCode() {
        return Objects
            .hash(node, flowPath, primaryHoverTarget, primaryClickTarget, tooltipTargets, revealTargets, activeSpoiler);
    }
}
