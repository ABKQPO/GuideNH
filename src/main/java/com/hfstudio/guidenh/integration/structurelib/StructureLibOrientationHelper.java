package com.hfstudio.guidenh.integration.structurelib;

import java.util.List;
import java.util.Locale;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.structurelib.alignment.IAlignment;
import com.gtnewhorizon.structurelib.alignment.IAlignmentProvider;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.alignment.enumerable.Flip;
import com.gtnewhorizon.structurelib.alignment.enumerable.Rotation;

public class StructureLibOrientationHelper {

    protected StructureLibOrientationHelper() {}

    public static void applyRequestedAlignment(@Nullable TileEntity controllerTile, StructureLibImportRequest request,
        List<String> warnings) {
        if (request == null
            || (request.getFacing() == null && request.getRotation() == null && request.getFlip() == null)) {
            return;
        }
        IAlignment alignment = resolveAlignment(controllerTile);
        if (alignment == null) {
            warnings
                .add("Controller does not expose StructureLib alignment controls; preview used the default facing.");
            return;
        }

        ForgeDirection direction = parseDirection(request.getFacing(), warnings);
        Rotation rotation = parseRotation(request.getRotation(), warnings);
        Flip flip = parseFlip(request.getFlip(), warnings);
        ExtendedFacing requestedFacing = ExtendedFacing.of(direction, rotation, flip);
        if (!alignment.checkedSetExtendedFacing(requestedFacing)) {
            applyDefaultAlignment(controllerTile);
            warnings.add(
                "Requested StructureLib facing/rotation/flip is not valid for this controller; preview used the default alignment.");
        }
    }

    public static void applyDefaultAlignment(@Nullable TileEntity controllerTile) {
        IAlignment alignment = resolveAlignment(controllerTile);
        if (alignment == null) {
            return;
        }
        ExtendedFacing currentFacing = alignment.getExtendedFacing();
        if (currentFacing != null && alignment.getAlignmentLimits()
            .isNewExtendedFacingValid(currentFacing)) {
            return;
        }
        for (ExtendedFacing facing : ExtendedFacing.VALUES) {
            if (alignment.checkedSetExtendedFacing(facing)) {
                return;
            }
        }
    }

    public static ForgeDirection resolveControllerFacing(@Nullable TileEntity controllerTile) {
        IAlignment alignment = resolveAlignment(controllerTile);
        if (alignment != null) {
            ExtendedFacing extendedFacing = alignment.getExtendedFacing();
            if (extendedFacing != null) {
                ForgeDirection backFacing = extendedFacing.getRelativeBackInWorld();
                if (backFacing != null && backFacing != ForgeDirection.UNKNOWN) {
                    return backFacing;
                }
                ForgeDirection direction = extendedFacing.getDirection();
                if (direction != null && direction != ForgeDirection.UNKNOWN) {
                    return direction.getOpposite();
                }
            }
        }
        return ForgeDirection.NORTH;
    }

    @Nullable
    public static IAlignment resolveAlignment(@Nullable TileEntity controllerTile) {
        if (controllerTile instanceof IAlignment alignment) {
            return alignment;
        }
        if (controllerTile instanceof IAlignmentProvider provider) {
            return provider.getAlignment();
        }
        return null;
    }

    public static ForgeDirection parseDirection(@Nullable String rawFacing, List<String> warnings) {
        if (rawFacing == null || rawFacing.trim()
            .isEmpty()) {
            return ForgeDirection.NORTH;
        }
        String normalized = rawFacing.trim()
            .toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "down" -> ForgeDirection.DOWN;
            case "up" -> ForgeDirection.UP;
            case "north" -> ForgeDirection.NORTH;
            case "south" -> ForgeDirection.SOUTH;
            case "west" -> ForgeDirection.WEST;
            case "east" -> ForgeDirection.EAST;
            default -> {
                warnings.add("Unsupported StructureLib facing '" + rawFacing + "'; preview used north.");
                yield ForgeDirection.NORTH;
            }
        };
    }

    public static Rotation parseRotation(@Nullable String rawRotation, List<String> warnings) {
        if (rawRotation == null || rawRotation.trim()
            .isEmpty()) {
            return Rotation.NORMAL;
        }
        Rotation rotation = Rotation.byName(normalizeRotation(rawRotation));
        if (rotation != null) {
            return rotation;
        }
        warnings.add("Unsupported StructureLib rotation '" + rawRotation + "'; preview used normal rotation.");
        return Rotation.NORMAL;
    }

    public static Flip parseFlip(@Nullable String rawFlip, List<String> warnings) {
        if (rawFlip == null || rawFlip.trim()
            .isEmpty()) {
            return Flip.NONE;
        }
        Flip flip = Flip.byName(normalizeFlip(rawFlip));
        if (flip != null) {
            return flip;
        }
        warnings.add("Unsupported StructureLib flip '" + rawFlip + "'; preview used no flip.");
        return Flip.NONE;
    }

    public static String normalizeRotation(String rawRotation) {
        String normalized = rawRotation.trim()
            .toLowerCase(Locale.ROOT)
            .replace('_', ' ')
            .replace('-', ' ');
        return switch (normalized) {
            case "90", "clockwise 90" -> "clockwise";
            case "180", "upside down 180" -> "upside down";
            case "270", "counter clockwise 90", "counterclockwise 90" -> "counter clockwise";
            default -> normalized;
        };
    }

    public static String normalizeFlip(String rawFlip) {
        String normalized = rawFlip.trim()
            .toLowerCase(Locale.ROOT)
            .replace('_', ' ')
            .replace('-', ' ');
        return switch (normalized) {
            case "mirror left right", "left right", "x" -> "horizontal";
            case "mirror front back", "front back", "z", "y" -> "vertical";
            default -> normalized;
        };
    }
}
