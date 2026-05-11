package com.hfstudio.structurelibexport;

import java.util.Locale;

import net.minecraft.command.CommandException;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.PerspectivePreset;

public class StructureLibExportView {

    private final String name;
    private final float yaw;
    private final float pitch;
    private final float roll;

    public StructureLibExportView(String name, float yaw, float pitch, float roll) {
        this.name = name;
        this.yaw = yaw;
        this.pitch = pitch;
        this.roll = roll;
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
            case "isometric-south-east", "se", "default" -> defaultView();
            case "isometric-north-east", "ne" -> new StructureLibExportView("isometric-north-east", 225f, 30f, 0f);
            case "isometric-north-west", "nw" -> new StructureLibExportView("isometric-north-west", 135f, 30f, 0f);
            case "top", "up" -> new StructureLibExportView("top", 120f, 0f, 45f);
            default -> throw new CommandException("Unknown StructureLib export view: " + raw);
        };
    }

    public StructureLibExportView withOverrides(@Nullable Float yaw, @Nullable Float pitch, @Nullable Float roll,
        @Nullable Float rotateX, @Nullable Float rotateY, @Nullable Float rotateZ) {
        float nextYaw = yaw != null ? yaw : rotateY != null ? rotateY : this.yaw;
        float nextPitch = pitch != null ? pitch : rotateX != null ? rotateX : this.pitch;
        float nextRoll = roll != null ? roll : rotateZ != null ? rotateZ : this.roll;
        if (nextYaw == this.yaw && nextPitch == this.pitch && nextRoll == this.roll) {
            return this;
        }
        return new StructureLibExportView(name + "-custom", nextYaw, nextPitch, nextRoll);
    }

    public void apply(CameraSettings camera) {
        camera.setIsometricYawPitchRoll(yaw, pitch, roll);
    }

    public String getName() {
        return name;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public float getRoll() {
        return roll;
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
