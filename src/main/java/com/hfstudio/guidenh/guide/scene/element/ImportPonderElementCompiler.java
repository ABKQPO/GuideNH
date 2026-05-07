package com.hfstudio.guidenh.guide.scene.element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.annotation.DiamondAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldBlockFaceOverlayAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldBoxAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldLineAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.PonderInputAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.PonderTextAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.SceneAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.compiler.AnnotationTagCompiler;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.ponder.PonderJsonLoader;
import com.hfstudio.guidenh.guide.scene.ponder.PonderKeyframe;
import com.hfstudio.guidenh.guide.scene.ponder.PonderKeyframeAnnotation;
import com.hfstudio.guidenh.guide.scene.ponder.PonderSceneData;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

/**
 * Compiles the {@code <ImportPonder src="scene.json" />} tag inside a {@code <GameScene>} block.
 *
 * <p>
 * The JSON file must be provided as an external asset in the resource pack.
 * The structure data is still provided by {@code <ImportStructure>}; this tag only carries the
 * animation timeline, camera keyframes, and per-keyframe annotations.
 *
 * <p>
 * Example MDX:
 * 
 * <pre>
 * {@code
 * <GameScene>
 *   <ImportStructure src="my_machine.snbt" />
 *   <ImportPonder src="my_machine.json" />
 * </GameScene>
 * }
 * </pre>
 */
public class ImportPonderElementCompiler implements SceneElementTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("ImportPonder");
    }

    @Override
    public void compile(GuidebookLevel level, CameraSettings camera, PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el) {
        LytGuidebookScene scene = AnnotationTagCompiler.CURRENT_SCENE.get();
        if (scene == null) {
            errorSink.appendError(compiler, "ImportPonder used outside <GameScene>", el);
            return;
        }

        String src = MdxAttrs.getString(compiler, errorSink, el, "src", null);
        if (src == null || src.trim()
            .isEmpty()) {
            errorSink.appendError(compiler, "Missing src attribute on <ImportPonder>", el);
            return;
        }

        String[] errorOut = new String[1];
        PonderSceneData data = PonderJsonLoader.load(compiler, src, errorOut);
        if (data == null) {
            errorSink.appendError(compiler, errorOut[0] != null ? errorOut[0] : "Failed to load ponder JSON", el);
            return;
        }

        List<List<SceneAnnotation>> annotationsByKeyframe = resolveAnnotations(data, compiler);
        scene.attachPonderData(data, annotationsByKeyframe);
    }

    private static List<List<SceneAnnotation>> resolveAnnotations(PonderSceneData data, PageCompiler compiler) {
        List<List<SceneAnnotation>> result = new ArrayList<>();
        for (PonderKeyframe kf : data.getKeyframes()) {
            List<PonderKeyframeAnnotation> rawList = kf.getAnnotations();
            if (rawList.isEmpty()) {
                result.add(Collections.emptyList());
                continue;
            }
            List<SceneAnnotation> resolved = new ArrayList<>(rawList.size());
            for (PonderKeyframeAnnotation raw : rawList) {
                resolveAndAdd(raw, resolved, compiler);
            }
            result.add(resolved.isEmpty() ? Collections.emptyList() : resolved);
        }
        return result;
    }

    private static void resolveAndAdd(PonderKeyframeAnnotation raw, List<SceneAnnotation> out, PageCompiler compiler) {
        SceneAnnotation ann = resolveAnnotation(raw, compiler);
        if (ann != null) {
            if (raw.getTooltip() != null && !raw.getTooltip()
                .isEmpty()) {
                ann.setTooltipText(raw.getTooltip());
            }
            out.add(ann);
        }
        if ("text".equals(raw.getType()) && raw.hasHighlight()) {
            var min = new Vector3f(raw.getHlMinX(0f), raw.getHlMinY(0f), raw.getHlMinZ(0f));
            var max = new Vector3f(raw.getHlMaxX(1f), raw.getHlMaxY(1f), raw.getHlMaxZ(1f));
            int hlArgb = raw.parseHighlightColor(0x8000FFAA);
            var box = new InWorldBoxAnnotation(
                min,
                max,
                new ConstantColor(hlArgb),
                InWorldBoxAnnotation.DEFAULT_THICKNESS);
            box.setAlwaysOnTop(raw.isAlwaysOnTop());
            out.add(box);
        }
    }

    @Nullable
    private static SceneAnnotation resolveAnnotation(PonderKeyframeAnnotation raw, PageCompiler compiler) {
        switch (raw.getType()) {
            case "diamond": {
                var pos = new Vector3f(raw.getX(0f), raw.getY(0f), raw.getZ(0f));
                int argb = raw.parseColor(0xFF00E000);
                var ann = new DiamondAnnotation(pos, new ConstantColor(argb));
                ann.setAlwaysOnTop(raw.isAlwaysOnTop());
                return ann;
            }
            case "box": {
                var min = new Vector3f(raw.getMinX(0f), raw.getMinY(0f), raw.getMinZ(0f));
                var max = new Vector3f(raw.getMaxX(1f), raw.getMaxY(1f), raw.getMaxZ(1f));
                int argb = raw.parseColor(0xFFFFFFFF);
                float lw = raw.getLineWidth(InWorldBoxAnnotation.DEFAULT_THICKNESS);
                var ann = new InWorldBoxAnnotation(min, max, new ConstantColor(argb), lw);
                ann.setAlwaysOnTop(raw.isAlwaysOnTop());
                return ann;
            }
            case "line": {
                var from = new Vector3f(raw.getFromX(0f), raw.getFromY(0f), raw.getFromZ(0f));
                var to = new Vector3f(raw.getToX(1f), raw.getToY(1f), raw.getToZ(1f));
                int argb = raw.parseColor(0xFFFFFFFF);
                float lw = raw.getLineWidth(InWorldLineAnnotation.DEFAULT_THICKNESS);
                var ann = new InWorldLineAnnotation(from, to, new ConstantColor(argb), lw);
                ann.setAlwaysOnTop(raw.isAlwaysOnTop());
                return ann;
            }
            case "blockface": {
                int bx = raw.getBlockX(0);
                int by = raw.getBlockY(0);
                int bz = raw.getBlockZ(0);
                int argb = raw.parseColor(0x80FFFFFF);
                var ann = new InWorldBlockFaceOverlayAnnotation(
                    bx,
                    by,
                    bz,
                    new ConstantColor(argb),
                    Collections.emptySet());
                ann.setAlwaysOnTop(raw.isAlwaysOnTop());
                return ann;
            }
            case "text": {
                var pos = new Vector3f(raw.getX(0f), raw.getY(0f), raw.getZ(0f));
                String msg = raw.getText();
                if (msg == null || msg.isEmpty()) return null;
                int borderArgb = raw.parseColor(0xFFAAAAAA);
                int maxW = raw.getMaxWidth(0);
                PonderTextAnnotation ann;
                if (raw.isIndependent()) {
                    ann = new PonderTextAnnotation(msg, borderArgb, (float) raw.getYOffset(0), maxW);
                } else {
                    ann = new PonderTextAnnotation(pos, msg, borderArgb, maxW);
                }
                if (compiler != null) {
                    var para = new LytParagraph();
                    compiler.compileInlineMarkdown(msg, para);
                    ann.setRichContent(para);
                }
                return ann;
            }
            case "input": {
                var pos = new Vector3f(raw.getX(0f), raw.getY(0f), raw.getZ(0f));
                PonderInputAnnotation.InputType inputType = resolveInputType(raw.getInputType());
                PonderInputAnnotation ann = new PonderInputAnnotation(pos, inputType);
                String mod = raw.getModifier();
                if (mod != null && !mod.isEmpty()) {
                    ann.setModifier(mod);
                }
                String itemId = raw.getItem();
                if (itemId != null && !itemId.isEmpty()) {
                    ItemStack stack = resolveItemStack(itemId);
                    if (stack != null) ann.setItemStack(stack);
                }
                return ann;
            }
            default:
                return null;
        }
    }

    private static PonderInputAnnotation.InputType resolveInputType(@Nullable String raw) {
        if (raw == null) return PonderInputAnnotation.InputType.LMB;
        switch (raw.toLowerCase()) {
            case "rmb":
                return PonderInputAnnotation.InputType.RMB;
            case "scroll":
                return PonderInputAnnotation.InputType.SCROLL;
            default:
                return PonderInputAnnotation.InputType.LMB;
        }
    }

    @Nullable
    private static ItemStack resolveItemStack(String itemId) {
        if (itemId == null || itemId.isEmpty()) return null;
        String[] parts = itemId.split(":");
        String registryId;
        int meta = 0;
        if (parts.length >= 3) {
            try {
                meta = Integer.parseInt(parts[parts.length - 1]);
                registryId = parts[0] + ":" + parts[1];
            } catch (NumberFormatException ignored) {
                registryId = itemId;
            }
        } else {
            registryId = itemId;
        }
        Item found = (Item) Item.itemRegistry.getObject(registryId);
        if (found == null) return null;
        return new ItemStack(found, 1, meta);
    }
}
