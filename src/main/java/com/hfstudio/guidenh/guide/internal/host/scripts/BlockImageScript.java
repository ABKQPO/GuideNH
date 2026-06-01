package com.hfstudio.guidenh.guide.internal.host.scripts;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;

import com.hfstudio.guidenh.guide.compiler.GuideItemReferenceResolver;
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

        BlockImagePlaceholder ph;
        boolean isWrapped = node instanceof LytFlowInlineBlock w
            && w.getBlock() instanceof BlockImagePlaceholder p;
        if (isWrapped) {
            ph = (BlockImagePlaceholder) ((LytFlowInlineBlock) node).getBlock();
        } else if (node instanceof BlockImagePlaceholder p) {
            ph = p;
        } else {
            return;
        }

        Block block = null;
        int meta = ph.meta;

        if (ph.ore != null && !ph.ore.isEmpty()) {
            ItemStack oreStack = GuideItemReferenceResolver.resolveOreDictionaryStack(ph.ore);
            if (oreStack != null && oreStack.getItem() != null) {
                block = Block.getBlockFromItem(oreStack.getItem());
                meta = oreStack.getItemDamage();
            }
        } else if (ph.id != null) {
            Item item = (Item) Item.itemRegistry.getObject(ph.id.contains(":") ? ph.id : "minecraft:" + ph.id);
            if (item != null) {
                block = Block.getBlockFromItem(item);
            }
            if (block == null) {
                block = (Block) Block.blockRegistry.getObject(ph.id.contains(":") ? ph.id : "minecraft:" + ph.id);
            }
        }

        if (block == null) {
            ctx.replace(
                LytParagraph.error("[BlockImage] Block not found: " + (ph.ore != null ? ph.ore : ph.id)));
            return;
        }

        NBTTagCompound tileTag = null;
        if (ph.nbt != null && !ph.nbt.trim().isEmpty()) {
            try {
                tileTag = GuideTextNbtCodec.readTextSafeCompound(ph.nbt.trim());
            } catch (Exception ignored) {}
        }

        PerspectivePreset perspective = PerspectivePreset.ISOMETRIC_NORTH_EAST;
        if (ph.perspective != null && !ph.perspective.trim().isEmpty()) {
            perspective = PerspectivePreset.fromSerializedName(ph.perspective.trim());
        }

        int defaultMeta = meta == Integer.MIN_VALUE ? BlockElementCompiler.defaultMetaFor(block, null) : meta;
        GuidebookLevel level = new GuidebookLevel();
        GuidebookPreviewBlockPlacer.place(level, 0, 0, 0, block, defaultMeta, tileTag);

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
        float minX = bounds[0];
        float minY = bounds[1];
        float minZ = bounds[2];
        float maxX = bounds[3] + 1f;
        float maxY = bounds[4] + 1f;
        float maxZ = bounds[5] + 1f;

        float minScreenX = Float.MAX_VALUE, maxScreenX = -Float.MAX_VALUE;
        float minScreenY = Float.MAX_VALUE, maxScreenY = -Float.MAX_VALUE;
        for (int corner = 0; corner < 8; corner++) {
            float wx = (corner & 1) == 0 ? minX : maxX;
            float wy = (corner & 2) == 0 ? minY : maxY;
            float wz = (corner & 4) == 0 ? minZ : maxZ;
            var sp = camera.worldToScreen(wx, wy, wz);
            if (sp.x < minScreenX) minScreenX = sp.x;
            if (sp.x > maxScreenX) maxScreenX = sp.x;
            if (sp.y < minScreenY) minScreenY = sp.y;
            if (sp.y > maxScreenY) maxScreenY = sp.y;
        }

        int autoW = clampDim((int) Math.ceil(maxScreenX - minScreenX) + 16);
        int autoH = clampDim((int) Math.ceil(maxScreenY - minScreenY) + 16);
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

    private static int clampDim(int d) {
        return Math.max(64, Math.min(256, d));
    }

}
