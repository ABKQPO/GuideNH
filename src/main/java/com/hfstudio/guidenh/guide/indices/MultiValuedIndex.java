package com.hfstudio.guidenh.guide.indices;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import net.minecraft.util.ResourceLocation;

import org.apache.commons.lang3.tuple.Pair;

import com.github.bsideup.jabel.Desugar;
import com.google.gson.stream.JsonWriter;
import com.hfstudio.guidenh.guide.GuidePageChange;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;

/**
 * A convenient index base-class for indices that map keys to multiple pages.
 */
public class MultiValuedIndex<K, V> implements PageIndex {

    private final Map<K, List<Record<V>>> index = new HashMap<>();

    private final String name;
    private final EntryFunction<K, V> entryFunction;
    private final JsonSerializer<K> keySerializer;
    private final JsonSerializer<V> valueSerializer;

    public MultiValuedIndex(String name, EntryFunction<K, V> entryFunction, JsonSerializer<K> keySerializer,
        JsonSerializer<V> valueSerializer) {
        this.name = name;
        this.entryFunction = entryFunction;
        this.keySerializer = keySerializer;
        this.valueSerializer = valueSerializer;
    }

    @Override
    public String getName() {
        return name;
    }

    public List<V> get(K key) {
        var entries = index.get(key);
        if (entries != null) {
            var result = new ArrayList<V>(entries.size());
            for (var entry : entries) {
                result.add(entry.value());
            }
            return result;
        }
        return Collections.emptyList();
    }

    @Override
    public boolean supportsUpdate() {
        return true;
    }

    @Override
    public void rebuild(List<ParsedGuidePage> pages) {
        index.clear();

        for (var page : pages) {
            addToIndex(page);
        }
    }

    @Override
    public void update(List<ParsedGuidePage> allPages, List<GuidePageChange> changes) {
        // Clean up all index entries associated with changed pages
        var idsToRemove = new HashSet<ResourceLocation>(changes.size());
        for (var change : changes) {
            idsToRemove.add(change.pageId());
        }
        var it = index.values()
            .iterator();
        while (it.hasNext()) {
            var entries = it.next();
            entries.removeIf(p -> idsToRemove.contains(p.pageId));
            if (entries.isEmpty()) {
                it.remove();
            }
        }

        // Then re-add new or changed pages
        for (var change : changes) {
            var newPage = change.newPage();
            if (newPage != null) {
                addToIndex(newPage);
            }
        }
    }

    @Override
    public void export(JsonWriter writer) throws IOException {
        writer.beginArray();
        for (var entry : index.entrySet()) {
            keySerializer.write(writer, entry.getKey());
            writer.beginArray();
            for (var vRecord : entry.getValue()) {
                valueSerializer.write(writer, vRecord.value());
            }
            writer.endArray();
        }
        writer.endArray();
    }

    private void addToIndex(ParsedGuidePage page) {
        for (var entry : entryFunction.getEntry(page)) {
            var key = entry.getKey();
            var value = entry.getValue();
            var entries = index.computeIfAbsent(key, k -> new ArrayList<>());
            entries.add(new Record<>(page.getId(), value));
        }
    }

    @FunctionalInterface
    public interface EntryFunction<K, V> {

        Iterable<Pair<K, V>> getEntry(ParsedGuidePage page);
    }

    @Desugar
    private record Record<V> (ResourceLocation pageId, V value) {}
}
