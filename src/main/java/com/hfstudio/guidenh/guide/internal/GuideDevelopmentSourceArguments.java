package com.hfstudio.guidenh.guide.internal;

import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class GuideDevelopmentSourceArguments {

    public static final String RESOURCE_PACK_SOURCES_PROPERTY = "guideme.resourcePacks.sources";
    public static final String RESOURCE_PACK_SOURCE_PROPERTY = "guideme.resourcePack.sources";

    private GuideDevelopmentSourceArguments() {}

    public static List<Path> parseConfiguredResourcePackRoots() {
        return parseConfiguredResourcePackRoots(
            ManagementFactory.getRuntimeMXBean()
                .getInputArguments());
    }

    public static List<Path> parseConfiguredResourcePackRoots(List<String> inputArguments) {
        var roots = new LinkedHashSet<Path>();
        addRoots(inputArguments, roots, RESOURCE_PACK_SOURCES_PROPERTY, RESOURCE_PACK_SOURCE_PROPERTY);
        return new ArrayList<>(roots);
    }

    public static List<Path> parseDevelopmentSourceFolders(List<String> inputArguments, String propertyName) {
        var roots = new LinkedHashSet<Path>();
        addRoots(inputArguments, roots, propertyName);
        return new ArrayList<>(roots);
    }

    private static void addRoots(List<String> inputArguments, LinkedHashSet<Path> roots, String... propertyNames) {
        for (String argument : inputArguments) {
            String rawPath = getPropertyValue(argument, propertyNames);
            if (rawPath == null) {
                continue;
            }
            rawPath = rawPath.trim();
            if (rawPath.isEmpty()) {
                continue;
            }
            roots.add(Paths.get(rawPath));
        }
    }

    private static String getPropertyValue(String argument, String... propertyNames) {
        if (argument == null) {
            return null;
        }
        for (String propertyName : propertyNames) {
            String prefix = "-D" + propertyName + "=";
            if (argument.startsWith(prefix)) {
                return argument.substring(prefix.length());
            }
        }
        return null;
    }
}
