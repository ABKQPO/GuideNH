package com.hfstudio.guidenh.guide.scene.ponder;

import java.nio.charset.StandardCharsets;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hfstudio.guidenh.guide.compiler.IdUtils;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;

/**
 * Loads a {@link PonderSceneData} from a JSON asset referenced by a resource-pack path.
 * The file is resolved relative to the current page, the same way as {@code ImportStructure}.
 */
public class PonderJsonLoader {

    private static final Gson GSON = new GsonBuilder().create();

    private PonderJsonLoader() {}

    /**
     * Loads and parses the Ponder JSON at {@code src} (relative to the current page).
     *
     * @param compiler the page compiler (provides asset loading and page-relative path resolution)
     * @param src      the path to the JSON file (e.g. {@code "my_scene.json"})
     * @param errorOut a single-element {@code String[]} used to return an error message on failure;
     *                 the element is set to {@code null} on success
     * @return the parsed data, or {@code null} if the file could not be loaded or parsed
     */
    @Nullable
    public static PonderSceneData load(PageCompiler compiler, String src, String[] errorOut) {
        if (errorOut == null || errorOut.length < 1) {
            throw new IllegalArgumentException("errorOut must have length >= 1");
        }
        errorOut[0] = null;

        ResourceLocation absId;
        try {
            absId = IdUtils.resolveLink(src, compiler.getPageId());
        } catch (IllegalArgumentException e) {
            errorOut[0] = "Invalid ponder path: " + src;
            return null;
        }

        byte[] data = compiler.loadAsset(absId);
        if (data == null) {
            errorOut[0] = "Missing ponder file: " + absId;
            return null;
        }

        try {
            String json = new String(data, StandardCharsets.UTF_8);
            PonderSceneData result = GSON.fromJson(json, PonderSceneData.class);
            if (result == null) {
                errorOut[0] = "Ponder JSON is empty: " + absId;
                return null;
            }
            if (result.getTotalTime() < 1) {
                errorOut[0] = "Ponder totalTime must be >= 1: " + absId;
                return null;
            }
            PonderLocalizationResolver.localize(result, compiler.getLanguage());
            return result;
        } catch (Exception e) {
            errorOut[0] = "Failed to parse ponder JSON (" + absId + "): " + e.getMessage();
            return null;
        }
    }
}
