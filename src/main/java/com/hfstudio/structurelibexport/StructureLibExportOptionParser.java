package com.hfstudio.structurelibexport;

import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.command.CommandException;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class StructureLibExportOptionParser {

    public static StructureLibExportOptions parse(String[] args) throws CommandException {
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
                if (builder.controller != null) {
                    throw new CommandException("Only one StructureLib controller can be specified.");
                }
                builder.controller = arg;
                continue;
            }
            String flag = arg.toLowerCase(Locale.ROOT);
            switch (flag) {
                case "--config" -> loadConfig(builder, requireValue(effectiveArgs, ++i, arg));
                case "--out" -> builder.outDir = Paths.get(requireValue(effectiveArgs, ++i, arg));
                case "--pixelsperblock" -> builder.pixelsPerBlock = parseInt(
                    requireValue(effectiveArgs, ++i, arg),
                    arg);
                case "--scale" -> builder.scale = parseFloat(requireValue(effectiveArgs, ++i, arg), arg);
                case "--tier" -> {
                    builder.tiers = parseValues(requireValue(effectiveArgs, ++i, arg), 1, 50);
                    builder.tierExplicit = true;
                }
                case "--channel" -> {
                    parseChannel(builder.channels, requireValue(effectiveArgs, ++i, arg));
                    builder.channelsExplicit = true;
                }
                case "--layers" -> parseLayers(builder, requireValue(effectiveArgs, ++i, arg));
                case "--facing" -> builder.facings = parseList(requireValue(effectiveArgs, ++i, arg));
                case "--rotation" -> builder.rotations = parseList(requireValue(effectiveArgs, ++i, arg));
                case "--flip" -> builder.flips = parseList(requireValue(effectiveArgs, ++i, arg));
                case "--orientation" -> builder.explicitOrientations
                    .addAll(parseOrientations(requireValue(effectiveArgs, ++i, arg)));
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
                case "--gt-active-controller" -> builder.gtActiveController = true;
                case "--gt-place-hatches" -> builder.gtPlaceHatches = true;
                case "--force" -> builder.force = true;
                case "--dry-run" -> builder.dryRun = true;
                default -> throw new CommandException("Unknown exportStructure structureLib option: " + arg);
            }
        }
        return builder.build();
    }

    private static void loadConfig(Builder builder, String rawPath) throws CommandException {
        Path path = resolveConfigPath(rawPath);
        try (Reader reader = new FileReader(path.toFile())) {
            JsonElement rootElement = new JsonParser().parse(reader);
            if (!rootElement.isJsonObject()) {
                throw new CommandException("StructureLib export config root must be an object.");
            }
            applyJson(builder, rootElement.getAsJsonObject());
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            throw new CommandException("Failed to read StructureLib export config: " + e.getMessage());
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
        throw new CommandException("StructureLib export config not found: " + rawPath);
    }

    private static void applyJson(Builder builder, JsonObject root) throws CommandException {
        if (root.has("controller")) {
            builder.controller = readString(root, "controller");
        }
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
        if (root.has("tier")) {
            builder.tiers = parseValues(readExpression(root.get("tier")), 1, 50);
            builder.tierExplicit = true;
        }
        if (root.has("channels")) {
            JsonObject channels = root.getAsJsonObject("channels");
            for (Map.Entry<String, JsonElement> entry : channels.entrySet()) {
                builder.channels.put(entry.getKey(), parseValues(readExpression(entry.getValue()), 1, 50));
            }
            builder.channelsExplicit = true;
        }
        if (root.has("layers")) {
            parseLayers(builder, readExpression(root.get("layers")));
        }
        if (root.has("facing")) {
            builder.facings = readStringList(root.get("facing"));
        }
        if (root.has("rotation")) {
            builder.rotations = readStringList(root.get("rotation"));
        }
        if (root.has("flip")) {
            builder.flips = readStringList(root.get("flip"));
        }
        if (root.has("orientation")) {
            builder.explicitOrientations.addAll(parseOrientations(readExpression(root.get("orientation"))));
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
        if (root.has("gtActiveController")) {
            builder.gtActiveController = root.get("gtActiveController")
                .getAsBoolean();
        }
        if (root.has("gtPlaceHatches")) {
            builder.gtPlaceHatches = root.get("gtPlaceHatches")
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
            throw new CommandException("Invalid long integer for " + flag + ": " + raw);
        }
    }

    private static long parseLong(String raw, String flag) throws CommandException {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new CommandException("Invalid integer for " + flag + ": " + raw);
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

    private static List<Integer> parseValues(String expression, int min, int max) throws CommandException {
        return new ArrayList<>(
            StructureLibNumericFilter.parse(expression)
                .resolveWithin(min, max));
    }

    private static void parseChannel(Map<String, List<Integer>> channels, String raw) throws CommandException {
        int equals = raw.indexOf('=');
        if (equals <= 0 || equals == raw.length() - 1) {
            throw new CommandException("Channel must use name=values syntax.");
        }
        String channel = raw.substring(0, equals)
            .trim();
        String expression = raw.substring(equals + 1)
            .trim();
        if (channel.isEmpty()) {
            throw new CommandException("Channel name cannot be empty.");
        }
        channels.put(channel, parseValues(expression, 1, 50));
    }

    private static void parseLayers(Builder builder, String raw) {
        String trimmed = raw != null ? raw.trim() : "all";
        builder.layersEach = "each".equalsIgnoreCase(trimmed);
        builder.layerExpression = builder.layersEach ? "each" : trimmed;
    }

    private static List<String> parseList(String raw) {
        ArrayList<String> values = new ArrayList<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                values.add(trimmed);
            }
        }
        return values;
    }

    private static List<StructureLibOrientationSpec> parseOrientations(String raw) throws CommandException {
        ArrayList<StructureLibOrientationSpec> orientations = new ArrayList<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] pieces = trimmed.split(":");
            if (pieces.length != 3) {
                throw new CommandException("Orientation must use facing:rotation:flip syntax: " + trimmed);
            }
            orientations.add(StructureLibOrientationSpec.of(pieces[0], pieces[1], pieces[2]));
        }
        return orientations;
    }

    private static String readString(JsonObject object, String name) {
        return object.get(name)
            .getAsString();
    }

    private static String readExpression(JsonElement element) {
        if (element.isJsonArray()) {
            StringBuilder builder = new StringBuilder();
            JsonArray array = element.getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                if (i > 0) {
                    builder.append(',');
                }
                builder.append(
                    array.get(i)
                        .getAsString());
            }
            return builder.toString();
        }
        return element.getAsString();
    }

    private static List<String> readStringList(JsonElement element) {
        if (!element.isJsonArray()) {
            return parseList(element.getAsString());
        }
        ArrayList<String> values = new ArrayList<>();
        JsonArray array = element.getAsJsonArray();
        for (JsonElement value : array) {
            values.add(value.getAsString());
        }
        return values;
    }

    public static class Builder {

        @Nullable
        private String controller;
        @Nullable
        private Path outDir;
        private int pixelsPerBlock = 128;
        private float scale = 1f;
        private List<Integer> tiers = new ArrayList<>();
        private Map<String, List<Integer>> channels = new LinkedHashMap<>();
        private boolean tierExplicit;
        private boolean channelsExplicit;
        private String layerExpression = "all";
        private boolean layersEach;
        private List<String> facings = new ArrayList<>();
        private List<String> rotations = new ArrayList<>();
        private List<String> flips = new ArrayList<>();
        private List<StructureLibOrientationSpec> explicitOrientations = new ArrayList<>();
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
        private boolean gtActiveController;
        private boolean gtPlaceHatches;
        private boolean force;
        private boolean dryRun;

        private StructureLibExportOptions build() throws CommandException {
            StructureLibExportView view = StructureLibExportView.parse(viewName)
                .withOverrides(yaw, pitch, roll, rotateX, rotateY, rotateZ);
            return new StructureLibExportOptions(
                controller,
                outDir,
                pixelsPerBlock,
                scale,
                tiers,
                channels,
                tierExplicit,
                channelsExplicit,
                layerExpression,
                layersEach,
                buildOrientations(),
                view,
                background,
                maxPixels,
                batchSize,
                gtActiveController,
                gtPlaceHatches,
                force,
                dryRun);
        }

        private List<StructureLibOrientationSpec> buildOrientations() {
            ArrayList<StructureLibOrientationSpec> orientations = new ArrayList<>(explicitOrientations);
            if (!facings.isEmpty() || !rotations.isEmpty() || !flips.isEmpty()) {
                List<String> effectiveFacings = facings.isEmpty() ? singleton("north") : facings;
                List<String> effectiveRotations = rotations.isEmpty() ? singleton("normal") : rotations;
                List<String> effectiveFlips = flips.isEmpty() ? singleton("none") : flips;
                for (String facing : effectiveFacings) {
                    for (String rotation : effectiveRotations) {
                        for (String flip : effectiveFlips) {
                            orientations.add(StructureLibOrientationSpec.of(facing, rotation, flip));
                        }
                    }
                }
            }
            return orientations;
        }

        private static List<String> singleton(String value) {
            ArrayList<String> values = new ArrayList<>();
            values.add(value);
            return values;
        }
    }
}
