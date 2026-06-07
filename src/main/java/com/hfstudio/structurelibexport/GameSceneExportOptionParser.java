package com.hfstudio.structurelibexport;

import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import net.minecraft.command.CommandException;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class GameSceneExportOptionParser {

    public static GameSceneExportOptions parse(String[] args) throws CommandException {
        Builder builder = new Builder();
        String[] effectiveArgs = args != null ? args : new String[0];
        for (int i = 0; i < effectiveArgs.length; i++) {
            String arg = effectiveArgs[i];
            if (arg == null || arg.trim()
                .isEmpty()) {
                continue;
            }
            if (arg.startsWith("@")) {
                loadConfig(builder, arg.substring(1));
                continue;
            }
            if (!arg.startsWith("--")) {
                throw new CommandException("gameScene export does not accept positional arguments: " + arg);
            }
            String flag = arg.toLowerCase(Locale.ROOT);
            switch (flag) {
                case "--config" -> loadConfig(builder, requireValue(effectiveArgs, ++i, arg));
                case "--out" -> builder.outDir = Paths.get(requireValue(effectiveArgs, ++i, arg));
                case "--pixelsperblock" -> builder.pixelsPerBlock = parseInt(
                    requireValue(effectiveArgs, ++i, arg),
                    arg);
                case "--scale" -> builder.scale = parseFloat(requireValue(effectiveArgs, ++i, arg), arg);
                case "--layers" -> parseLayers(builder, requireValue(effectiveArgs, ++i, arg));
                case "--view" -> builder.viewName = requireValue(effectiveArgs, ++i, arg);
                case "--yaw" -> builder.yaw = parseFloat(requireValue(effectiveArgs, ++i, arg), arg);
                case "--pitch" -> builder.pitch = parseFloat(requireValue(effectiveArgs, ++i, arg), arg);
                case "--roll" -> builder.roll = parseFloat(requireValue(effectiveArgs, ++i, arg), arg);
                case "--rotatex" -> builder.rotateX = parseFloat(requireValue(effectiveArgs, ++i, arg), arg);
                case "--rotatey" -> builder.rotateY = parseFloat(requireValue(effectiveArgs, ++i, arg), arg);
                case "--rotatez" -> builder.rotateZ = parseFloat(requireValue(effectiveArgs, ++i, arg), arg);
                case "--background" -> builder.background = StructureLibExportBackground
                    .parse(requireValue(effectiveArgs, ++i, arg));
                case "--maxpixels" -> builder.maxPixels = parseLong(requireValue(effectiveArgs, ++i, arg), arg);
                case "--batchsize" -> builder.batchSize = parseInt(requireValue(effectiveArgs, ++i, arg), arg);
                case "--show-annotations" -> builder.showAnnotations = true;
                case "--show-grid" -> builder.showGrid = true;
                case "--force" -> builder.force = true;
                case "--dry-run" -> builder.dryRun = true;
                default -> throw new CommandException("Unknown exportStructure gameScene option: " + arg);
            }
        }
        return builder.build();
    }

    private static void loadConfig(Builder builder, String rawPath) throws CommandException {
        Path path = resolveConfigPath(rawPath);
        try (Reader reader = new FileReader(path.toFile())) {
            JsonElement rootElement = new JsonParser().parse(reader);
            if (!rootElement.isJsonObject()) {
                throw new CommandException("GameScene export config root must be an object.");
            }
            applyJson(builder, rootElement.getAsJsonObject());
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            throw new CommandException("Failed to read GameScene export config: " + e.getMessage());
        }
    }

    private static Path resolveConfigPath(String rawPath) throws CommandException {
        if (rawPath == null || rawPath.trim()
            .isEmpty()) {
            throw new CommandException("Missing config path.");
        }
        Path requested = Paths.get(rawPath.trim());
        if (requested.isAbsolute() && requested.toFile()
            .isFile()) {
            return requested;
        }
        if (requested.toFile()
            .isFile()) {
            return requested;
        }
        Path configRelative = Paths.get("config", "guidenh", "structure_exports", rawPath.trim());
        if (configRelative.toFile()
            .isFile()) {
            return configRelative;
        }
        throw new CommandException("GameScene export config not found: " + rawPath);
    }

    private static void applyJson(Builder builder, JsonObject root) throws CommandException {
        if (root.has("out")) {
            builder.outDir = Paths.get(readString(root, "out"));
        }
        if (root.has("pixelsPerBlock")) {
            builder.pixelsPerBlock = root.get("pixelsPerBlock")
                .getAsInt();
        }
        if (root.has("scale")) {
            builder.scale = root.get("scale")
                .getAsFloat();
        }
        if (root.has("layers")) {
            parseLayers(
                builder,
                root.get("layers")
                    .getAsString());
        }
        if (root.has("view")) {
            builder.viewName = readString(root, "view");
        }
        if (root.has("yaw")) {
            builder.yaw = root.get("yaw")
                .getAsFloat();
        }
        if (root.has("pitch")) {
            builder.pitch = root.get("pitch")
                .getAsFloat();
        }
        if (root.has("roll")) {
            builder.roll = root.get("roll")
                .getAsFloat();
        }
        if (root.has("rotateX")) {
            builder.rotateX = root.get("rotateX")
                .getAsFloat();
        }
        if (root.has("rotateY")) {
            builder.rotateY = root.get("rotateY")
                .getAsFloat();
        }
        if (root.has("rotateZ")) {
            builder.rotateZ = root.get("rotateZ")
                .getAsFloat();
        }
        if (root.has("background")) {
            builder.background = StructureLibExportBackground.parse(readString(root, "background"));
        }
        if (root.has("maxPixels")) {
            builder.maxPixels = root.get("maxPixels")
                .getAsLong();
        }
        if (root.has("batchSize")) {
            builder.batchSize = root.get("batchSize")
                .getAsInt();
        }
        if (root.has("showAnnotations")) {
            builder.showAnnotations = root.get("showAnnotations")
                .getAsBoolean();
        }
        if (root.has("showGrid")) {
            builder.showGrid = root.get("showGrid")
                .getAsBoolean();
        }
        if (root.has("force")) {
            builder.force = root.get("force")
                .getAsBoolean();
        }
        if (root.has("dryRun")) {
            builder.dryRun = root.get("dryRun")
                .getAsBoolean();
        }
    }

    private static String requireValue(String[] args, int index, String flag) throws CommandException {
        if (index >= args.length || args[index].startsWith("--")) {
            throw new CommandException("Missing value for " + flag);
        }
        return args[index];
    }

    private static int parseInt(String raw, String flag) throws CommandException {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new CommandException("Invalid integer for " + flag + ": " + raw);
        }
    }

    private static long parseLong(String raw, String flag) throws CommandException {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new CommandException("Invalid long integer for " + flag + ": " + raw);
        }
    }

    private static float parseFloat(String raw, String flag) throws CommandException {
        try {
            float value = Float.parseFloat(raw);
            if (Float.isNaN(value) || Float.isInfinite(value)) {
                throw new CommandException("Invalid number for " + flag + ": " + raw);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new CommandException("Invalid number for " + flag + ": " + raw);
        }
    }

    private static void parseLayers(Builder builder, String raw) {
        String trimmed = raw != null ? raw.trim() : "all";
        builder.layersEach = "each".equalsIgnoreCase(trimmed);
        builder.layerExpression = builder.layersEach ? "each" : trimmed;
    }

    private static String readString(JsonObject object, String name) {
        return object.get(name)
            .getAsString();
    }

    public static class Builder {

        @Nullable
        private Path outDir;
        private int pixelsPerBlock = GameSceneExportRunner.DEFAULT_GAME_SCENE_PIXELS_PER_BLOCK;
        private float scale = 1f;
        private String layerExpression = "all";
        private boolean layersEach;
        @Nullable
        private String viewName;
        @Nullable
        private Float yaw;
        @Nullable
        private Float pitch;
        @Nullable
        private Float roll;
        @Nullable
        private Float rotateX;
        @Nullable
        private Float rotateY;
        @Nullable
        private Float rotateZ;
        private StructureLibExportBackground background = StructureLibExportBackground.transparent();
        private long maxPixels = StructureLibExportImageLimits.DEFAULT_MAX_PIXELS;
        private int batchSize = 16;
        private boolean showAnnotations;
        private boolean showGrid;
        private boolean force;
        private boolean dryRun;

        private GameSceneExportOptions build() throws CommandException {
            StructureLibExportView view = StructureLibExportView.parse(viewName)
                .withOverrides(yaw, pitch, roll, rotateX, rotateY, rotateZ);
            return new GameSceneExportOptions(
                outDir,
                pixelsPerBlock,
                scale,
                layerExpression,
                layersEach,
                view,
                background,
                maxPixels,
                batchSize,
                showAnnotations,
                showGrid,
                force,
                dryRun);
        }
    }
}
