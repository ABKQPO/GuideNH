package com.hfstudio.structurelibexport;

import java.util.Locale;

import net.minecraft.command.CommandException;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.PerspectivePreset;

import lombok.Getter;

@Getter
public class StructureLibExportView {

    private final String name;
    private final float yaw;
    private final float pitch;
    private final float roll;
    private final boolean explicit;

    public StructureLibExportView(String name, float yaw, float pitch, float roll) {
        this(name, yaw, pitch, roll, false);
    }

    public StructureLibExportView(String name, float yaw, float pitch, float roll, boolean explicit) {
        this.name = name;
        this.yaw = yaw;
        this.pitch = pitch;
        this.roll = roll;
        this.explicit = explicit;
    }

    public static StructureLibExportView defaultView() {
        return new StructureLibExportView("isometric-south-east", 315f, 30f, 0f);
    }

    public static StructureLibExportView parse(@Nullable String raw) throws CommandException {
        if (raw == null || raw.trim()
            .isEmpty()) {
            return defaultView();
        }
        String normalized = raw.trim()
            .toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "isometric-south-east", "se", "default" -> new StructureLibExportView(
                "isometric-south-east",
                315f,
                30f,
                0f,
                true);
            case "isometric-north-east", "ne" -> new StructureLibExportView(
                "isometric-north-east",
                225f,
                30f,
                0f,
                true);
            case "isometric-north-west", "nw" -> new StructureLibExportView(
                "isometric-north-west",
                135f,
                30f,
                0f,
                true);
            case "top", "up" -> new StructureLibExportView("top", 120f, 0f, 45f, true);
            default -> throw new CommandException("Unknown StructureLib export view: " + raw);
        };
    }

    public StructureLibExportView withOverrides(@Nullable Float yaw, @Nullable Float pitch, @Nullable Float roll,
        @Nullable Float rotateX, @Nullable Float rotateY, @Nullable Float rotateZ) {
        boolean hasOverrides = yaw != null || pitch != null
            || roll != null
            || rotateX != null
            || rotateY != null
            || rotateZ != null;
        float nextYaw = yaw != null ? yaw : rotateY != null ? rotateY : this.yaw;
        float nextPitch = pitch != null ? pitch : rotateX != null ? rotateX : this.pitch;
        float nextRoll = roll != null ? roll : rotateZ != null ? rotateZ : this.roll;
        if (!hasOverrides && nextYaw == this.yaw && nextPitch == this.pitch && nextRoll == this.roll) {
            return this;
        }
        return new StructureLibExportView(name + "-custom", nextYaw, nextPitch, nextRoll, true);
    }

    public void apply(CameraSettings camera) {
        camera.setIsometricYawPitchRoll(yaw, pitch, roll);
    }

    public PerspectivePreset asPerspectivePreset() {
        if ("isometric-north-west".equals(name)) {
            return PerspectivePreset.ISOMETRIC_NORTH_WEST;
        }
        if ("top".equals(name)) {
            return PerspectivePreset.UP;
        }
        if ("isometric-south-east".equals(name)) {
            return PerspectivePreset.ISOMETRIC_NORTH_EAST;
        }
        return PerspectivePreset.ISOMETRIC_NORTH_EAST;
    }
}
