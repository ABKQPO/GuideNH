package com.hfstudio.guidenh.guide.internal.host.scripts;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;

import com.hfstudio.guidenh.guide.compiler.GuideItemReferenceResolver;
import com.hfstudio.guidenh.guide.compiler.IdUtils;
import com.hfstudio.guidenh.guide.compiler.IdUtils.ParsedItemRef;
import com.hfstudio.guidenh.guide.compiler.tags.BlockImageCompiler.BlockImagePlaceholder;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.internal.host.EventType;
import com.hfstudio.guidenh.guide.internal.host.LytEvent;
import com.hfstudio.guidenh.guide.internal.host.LytScript;
import com.hfstudio.guidenh.guide.internal.host.ScriptContext;
import com.hfstudio.guidenh.guide.internal.host.ScriptType;
import com.hfstudio.guidenh.guide.internal.structure.GuideTextNbtCodec;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.PerspectivePreset;
import com.hfstudio.guidenh.guide.scene.element.BlockElementCompiler;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.level.GuidebookPreviewBlockPlacer;
import com.hfstudio.guidenh.guide.scene.SceneViewportMetrics;
import com.hfstudio.guidenh.guide.scene.ponder.PonderNbtPath;

public class BlockImageScript implements LytScript {

    @Override
    public ScriptType type() { return ScriptType.JAVA; }

    @Override
    public String styleClass() { return "BlockImage"; }

    @Override
    public boolean isAsync() { return true; }

    @Override
    @SuppressWarnings("deprecation")
    public void onEvent(Object node, LytEvent event, ScriptContext ctx) {
        if (event.type() != EventType.MOUNT) return;

        BlockImagePlaceholder ph = LytFlowInlineBlock.unwrapPlaceholder(node, BlockImagePlaceholder.class);
        if (ph == null) return;

        Block block = null;
        int meta = ph.meta;
        NBTTagCompound tileTag = null;

        if (ph.ore != null && !ph.ore.isEmpty()) {
            ItemStack oreStack = GuideItemReferenceResolver.resolveOreDictionaryStack(ph.ore);
            if (oreStack != null && oreStack.getItem() != null) {
                block = Block.getBlockFromItem(oreStack.getItem());
                meta = oreStack.getItemDamage();
            }
        } else if (ph.id != null) {
            // Handle inline NBT: id="minecraft:stone{BlockEntityTag:{...}}"
            ParsedItemRef ref = IdUtils.parseItemRef(ph.id.contains(":") ? ph.id : "minecraft:" + ph.id, "minecraft");
            Item item = (Item) Item.itemRegistry.getObject(ref.rawKey());
            if (item != null) block = Block.getBlockFromItem(item);
            if (ref.nbt() != null && tileTag == null) {
                tileTag = (NBTTagCompound) ref.nbt().copy();
            }
        }

        if (block == null) {
            ctx.replace(
                LytParagraph.error("[BlockImage] Block not found: " + (ph.ore != null ? ph.ore : ph.id)));
            return;
        }

        if (ph.nbt != null && !ph.nbt.trim().isEmpty()) {
            try {
                NBTTagCompound explicitTag = GuideTextNbtCodec.readTextSafeCompound(ph.nbt.trim());
                if (tileTag != null) {
                    for (Object key : explicitTag.func_150296_c()) {
                        tileTag.setTag((String) key, explicitTag.getTag((String) key));
                    }
                } else {
                    tileTag = explicitTag;
                }
            } catch (Exception ignored) {}
        }

        PerspectivePreset perspective = PerspectivePreset.ISOMETRIC_NORTH_EAST;
        if (ph.perspective != null && !ph.perspective.trim().isEmpty()) {
            perspective = PerspectivePreset.fromSerializedName(ph.perspective.trim());
        }

        int defaultMeta = meta == Integer.MIN_VALUE ? BlockElementCompiler.defaultMetaFor(block, null) : meta;
        GuidebookLevel level = new GuidebookLevel();
        String registryId = ph.id != null ? (ph.id.contains(":") ? ph.id : "minecraft:" + ph.id) : "";
        GuidebookPreviewBlockPlacer.place(level, 0, 0, 0, block, defaultMeta, tileTag, registryId);

        if (level.isEmpty()) {
            ctx.replace(
                LytParagraph.error("[BlockImage] Failed to create block preview"));
            return;
        }

        int width = ph.width > 0 ? ph.width : 128;
        int height = ph.height > 0 ? ph.height : 128;
        float zoom = clampZoom(ph.scale);

        CameraSettings camera = new CameraSettings();
        camera.setPerspectivePreset(perspective);
        camera.setZoom(zoom);
        camera.setViewportSize(width, height);

        var scene = new LytGuidebookScene();
        scene.setLevel(level);
        scene.setCamera(camera);
        scene.setSceneSize(width, height);
        scene.setInteractive(false);
        scene.setSceneButtonsVisible(false);
        scene.setBottomControlsVisible(false);
        scene.setReserveBottomControlArea(false);
        scene.setVisibleLayerSliderEnabled(false);
        scene.setGridButtonEnabled(false);
        scene.setGridVisible(false);
        scene.setAnnotationsVisible(false);
        scene.setShowBackground(false);
        camera.setViewportSize(width, height);
        scene.snapshotInitialCamera();

        float[] center = level.getCenter();
        camera.setRotationCenter(center[0], center[1], center[2]);

        int[] bounds = level.getBounds();
        SceneViewportMetrics metrics = SceneViewportMetrics.measure(camera, bounds);
        int autoW = SceneViewportMetrics.clampDimension(metrics.spanX());
        int autoH = SceneViewportMetrics.clampDimension(metrics.spanY());
        scene.setSceneSize(autoW, autoH);
        camera.setViewportSize(autoW, autoH);

        var pc = camera.worldToScreen(center[0], center[1], center[2]);
        camera.setOffsetX(-pc.x);
        camera.setOffsetY(pc.y);
        scene.snapshotInitialCamera();

        ctx.replace(scene);
    }

    private static float clampZoom(float zoom) {
        return Math.max(LytGuidebookScene.MIN_ZOOM, Math.min(LytGuidebookScene.MAX_ZOOM, zoom <= 0 ? 1f : zoom));
    }

}
