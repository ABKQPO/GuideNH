package com.hfstudio.guidenh.guide.scene.ponder;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.structure.GuideTextNbtCodec;

/**
 * Applies simple dotted NBT paths such as {@code InputTanks[0].TankContent.Amount}.
 */
public class PonderNbtPath {

    private static final String WRAPPED_VALUE_KEY = "value";

    protected PonderNbtPath() {}

    public static NBTTagCompound parseCompound(String snbt) throws Exception {
        return GuideTextNbtCodec.readTextSafeCompound(snbt);
    }

    public static NBTBase parseValue(String snbt) throws Exception {
        NBTTagCompound wrapper = GuideTextNbtCodec.readTextSafeCompound("{" + WRAPPED_VALUE_KEY + ":" + snbt + "}");
        NBTBase value = wrapper.getTag(WRAPPED_VALUE_KEY);
        if (value == null) {
            throw new IllegalArgumentException("Missing wrapped NBT value");
        }
        return value;
    }

    public static void mergeCompound(NBTTagCompound target, NBTTagCompound patch) {
        for (String key : patch.func_150296_c()) {
            NBTBase patchValue = patch.getTag(key);
            NBTBase targetValue = target.getTag(key);
            if (targetValue instanceof NBTTagCompound targetCompound
                && patchValue instanceof NBTTagCompound patchCompound) {
                mergeCompound(targetCompound, patchCompound);
            } else if (patchValue != null) {
                target.setTag(key, patchValue.copy());
            }
        }
    }

    public static boolean set(NBTTagCompound root, String path, NBTBase value) {
        List<PathSegment> segments = parse(path);
        if (segments.isEmpty()) {
            return false;
        }
        NBTBase parent = root;
        for (int i = 0; i < segments.size(); i++) {
            PathSegment segment = segments.get(i);
            if (i == segments.size() - 1) {
                return setSegment(parent, segment, value);
            }
            parent = resolveSegment(parent, segment);
            if (parent == null) {
                return false;
            }
        }
        return false;
    }

    public static boolean remove(NBTTagCompound root, String path) {
        List<PathSegment> segments = parse(path);
        if (segments.isEmpty()) {
            return false;
        }
        NBTBase parent = root;
        for (int i = 0; i < segments.size(); i++) {
            PathSegment segment = segments.get(i);
            if (i == segments.size() - 1) {
                return removeSegment(parent, segment);
            }
            parent = resolveSegment(parent, segment);
            if (parent == null) {
                return false;
            }
        }
        return false;
    }

    @Nullable
    private static NBTBase resolveSegment(NBTBase parent, PathSegment segment) {
        NBTBase current;
        if (segment.key.isEmpty()) {
            current = parent;
        } else if (parent instanceof NBTTagCompound compound) {
            current = compound.getTag(segment.key);
        } else {
            return null;
        }

        for (int index : segment.indexes) {
            if (!(current instanceof NBTTagList list) || index < 0 || index >= list.tagCount()) {
                return null;
            }
            current = list.getCompoundTagAt(index);
        }
        return current;
    }

    private static boolean setSegment(NBTBase parent, PathSegment segment, NBTBase value) {
        if (segment.indexes.isEmpty()) {
            if (!(parent instanceof NBTTagCompound compound) || segment.key.isEmpty()) {
                return false;
            }
            compound.setTag(segment.key, value.copy());
            return true;
        }

        NBTTagList list = resolveFinalList(parent, segment);
        if (list == null) {
            return false;
        }
        int index = segment.indexes.getLast();
        if (index < 0 || index >= list.tagCount()) {
            return false;
        }
        list.func_150304_a(index, value.copy());
        return true;
    }

    private static boolean removeSegment(NBTBase parent, PathSegment segment) {
        if (segment.indexes.isEmpty()) {
            if (!(parent instanceof NBTTagCompound compound) || segment.key.isEmpty()) {
                return false;
            }
            compound.removeTag(segment.key);
            return true;
        }

        NBTTagList list = resolveFinalList(parent, segment);
        if (list == null) {
            return false;
        }
        int index = segment.indexes.getLast();
        if (index < 0 || index >= list.tagCount()) {
            return false;
        }
        list.removeTag(index);
        return true;
    }

    @Nullable
    private static NBTTagList resolveFinalList(NBTBase parent, PathSegment segment) {
        NBTBase current;
        if (segment.key.isEmpty()) {
            current = parent;
        } else if (parent instanceof NBTTagCompound compound) {
            current = compound.getTag(segment.key);
        } else {
            return null;
        }

        for (int i = 0; i < segment.indexes.size() - 1; i++) {
            int index = segment.indexes.get(i);
            if (!(current instanceof NBTTagList list) || index < 0 || index >= list.tagCount()) {
                return null;
            }
            current = list.getCompoundTagAt(index);
        }
        return current instanceof NBTTagList list ? list : null;
    }

    private static List<PathSegment> parse(String path) {
        List<PathSegment> segments = new ArrayList<>();
        String trimmedPath = trimToNull(path);
        if (trimmedPath == null) {
            return segments;
        }

        int start = 0;
        while (start <= trimmedPath.length()) {
            int separator = trimmedPath.indexOf('.', start);
            int end = separator >= 0 ? separator : trimmedPath.length();
            String rawSegment = trimToNull(trimmedPath.substring(start, end));
            PathSegment segment = rawSegment != null ? parseSegment(rawSegment) : null;
            if (segment == null) {
                return new ArrayList<>();
            }
            segments.add(segment);
            if (separator < 0) {
                break;
            }
            start = separator + 1;
        }
        return segments;
    }

    @Nullable
    private static PathSegment parseSegment(String raw) {
        if (raw.isEmpty()) {
            return null;
        }
        int bracket = raw.indexOf('[');
        String key = bracket >= 0 ? raw.substring(0, bracket) : raw;
        List<Integer> indexes = new ArrayList<>();
        int cursor = bracket;
        while (cursor >= 0) {
            int close = raw.indexOf(']', cursor + 1);
            if (close < 0) {
                return null;
            }
            String indexText = trimToNull(raw.substring(cursor + 1, close));
            if (indexText == null) {
                return null;
            }
            try {
                indexes.add(Integer.parseInt(indexText));
            } catch (NumberFormatException e) {
                return null;
            }
            cursor = close + 1;
            if (cursor >= raw.length()) {
                break;
            }
            if (raw.charAt(cursor) != '[') {
                return null;
            }
        }
        return new PathSegment(key, indexes);
    }

    private static class PathSegment {

        private final String key;
        private final List<Integer> indexes;

        private PathSegment(String key, List<Integer> indexes) {
            this.key = key;
            this.indexes = indexes;
        }
    }

    @Nullable
    private static String trimToNull(@Nullable String value) {
        if (value == null) {
            return null;
        }
        int start = 0;
        int end = value.length();
        while (start < end && value.charAt(start) <= ' ') {
            start++;
        }
        while (end > start && value.charAt(end - 1) <= ' ') {
            end--;
        }
        if (start == end) {
            return null;
        }
        if (start == 0 && end == value.length()) {
            return value;
        }
        return value.substring(start, end);
    }
}
