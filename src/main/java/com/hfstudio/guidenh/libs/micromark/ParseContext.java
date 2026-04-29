package com.hfstudio.guidenh.libs.micromark;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.libs.unist.UnistPoint;

/**
 * A context object that helps w/ parsing markdown.
 */
public class ParseContext {

    public Extension constructs;
    Create content;
    Create document;
    Create flow;
    Create string;
    Create text;

    /**
     * List of defined identifiers
     */
    public List<String> defined = new ArrayList<>();

    /**
     * Map of line numbers to whether they are lazy (as opposed to the line before them).
     * Take for example:
     * 
     * <pre>
     *   > a
     *   b
     * </pre>
     * 
     * L1 here is not lazy, L2 is.
     */
    public Map<Integer, Boolean> lazy = new HashMap<>();

    public boolean isLazyLine(int line) {
        return lazy.getOrDefault(line, false);
    }

    public Create get(ContentType contentType) {
        return switch (contentType) {
            case DOCUMENT -> document;
            case FLOW -> flow;
            case CONTENT -> content;
            case TEXT -> text;
            case STRING -> string;
        };
    }

    @FunctionalInterface
    public interface Create {

        default TokenizeContext create() {
            return create(null);
        }

        TokenizeContext create(@Nullable UnistPoint from);
    }
}
