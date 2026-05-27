package com.hfstudio.structurelibexport;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.IAlignment;
import com.gtnewhorizon.structurelib.alignment.IAlignmentLimits;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.alignment.enumerable.Flip;
import com.gtnewhorizon.structurelib.alignment.enumerable.Rotation;
import com.hfstudio.guidenh.integration.structurelib.StructureLibOrientationHelper;
import com.hfstudio.guidenh.integration.structurelib.StructureLibRuntimeFacade;
import com.hfstudio.guidenh.integration.structurelib.StructureLibRuntimeFacade.BuildContext;
import com.hfstudio.guidenh.integration.structurelib.StructureLibRuntimeFacade.ResolvedController;

public class StructureLibAlignmentResolver {

    public List<StructureLibOrientationSpec> resolveOrientations(StructureLibControllerSpec controller,
        List<StructureLibOrientationSpec> requestedOrientations, List<String> warnings) throws CommandException {
        if (requestedOrientations == null || requestedOrientations.isEmpty()) {
            ArrayList<StructureLibOrientationSpec> defaults = new ArrayList<>();
            defaults.add(StructureLibOrientationSpec.DEFAULT);
            return defaults;
        }
        if (!hasExplicitOrientation(requestedOrientations)) {
            return new ArrayList<>(requestedOrientations);
        }
        IAlignmentLimits limits = resolveLimits(controller, warnings);
        ArrayList<StructureLibOrientationSpec> accepted = new ArrayList<>();
        for (StructureLibOrientationSpec requestedOrientation : requestedOrientations) {
            if (requestedOrientation == null) {
                continue;
            }
            if (requestedOrientation.isDefaultOrientation() || isAllowed(limits, requestedOrientation, warnings)) {
                accepted.add(requestedOrientation);
            } else {
                warnings.add(
                    "Skipped unsupported StructureLib orientation " + requestedOrientation.asSuffix()
                        + " for "
                        + controller.getControllerArgument()
                        + ".");
            }
        }
        if (accepted.isEmpty()) {
            throw new CommandException(
                "No requested StructureLib orientations are valid for " + controller.getControllerArgument() + ".");
        }
        return accepted;
    }

    private boolean hasExplicitOrientation(List<StructureLibOrientationSpec> requestedOrientations) {
        for (StructureLibOrientationSpec orientation : requestedOrientations) {
            if (orientation != null && !orientation.isDefaultOrientation()) {
                return true;
            }
        }
        return false;
    }

    private IAlignmentLimits resolveLimits(StructureLibControllerSpec controller, List<String> warnings) {
        BuildContext context = new BuildContext();
        try {
            ResolvedController resolvedController = new ResolvedController(
                controller.getBlockId(),
                controller.getBlock(),
                controller.getMeta());
            TileEntity tile = StructureLibRuntimeFacade
                .placeControllerDirectly(context.getLevel(), context.getWorld(), resolvedController, warnings);
            IAlignment alignment = StructureLibRuntimeFacade.resolveAlignment(tile);
            if (alignment != null) {
                IAlignmentLimits limits = alignment.getAlignmentLimits();
                return limits != null ? limits : alignment;
            }
        } catch (Throwable t) {
            warnings.add(
                "Could not inspect StructureLib alignment limits for " + controller.getControllerArgument()
                    + "; requested orientations will be validated during import.");
        } finally {
            context.clear();
        }
        return null;
    }

    private boolean isAllowed(IAlignmentLimits limits, StructureLibOrientationSpec orientation, List<String> warnings) {
        if (limits == null) {
            return true;
        }
        ForgeDirection direction = StructureLibOrientationHelper.parseDirection(orientation.getFacing(), warnings);
        Rotation rotation = StructureLibOrientationHelper.parseRotation(orientation.getRotation(), warnings);
        Flip flip = StructureLibOrientationHelper.parseFlip(orientation.getFlip(), warnings);
        return limits.isNewExtendedFacingValid(ExtendedFacing.of(direction, rotation, flip));
    }
}
