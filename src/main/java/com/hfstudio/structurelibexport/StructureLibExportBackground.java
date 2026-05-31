package com.hfstudio.structurelibexport;

import java.util.Locale;

import net.minecraft.command.CommandException;

import lombok.Getter;

@Getter
public class StructureLibExportBackground {

    public static final int DARK_ARGB = 0xFF121216;

    private final int argb;

    public StructureLibExportBackground(int argb) {
        this.argb = argb;
    }

    public static StructureLibExportBackground transparent() {
        return new StructureLibExportBackground(0x00000000);
    }

    public static StructureLibExportBackground parse(String raw) throws CommandException {
        if (raw == null || raw.trim()
            .isEmpty()) {
            return transparent();
        }
        String normalized = raw.trim()
            .toLowerCase(Locale.ROOT);
        if ("transparent".equals(normalized)) {
            return transparent();
        }
        if ("dark".equals(normalized)) {
            return new StructureLibExportBackground(DARK_ARGB);
        }
        if (normalized.startsWith("#")) {
            String hex = normalized.substring(1);
            try {
                if (hex.length() == 6) {
                    return new StructureLibExportBackground((int) (0xFF000000L | Long.parseLong(hex, 16)));
                }
                if (hex.length() == 8) {
                    return new StructureLibExportBackground((int) Long.parseLong(hex, 16));
                }
            } catch (NumberFormatException e) {
                throw new CommandException("Invalid background color: " + raw);
            }
        }
        throw new CommandException("Invalid background. Use transparent, dark, #RRGGBB, or #AARRGGBB.");
    }

    public boolean isTransparent() {
        return (argb >>> 24) == 0;
    }
}
