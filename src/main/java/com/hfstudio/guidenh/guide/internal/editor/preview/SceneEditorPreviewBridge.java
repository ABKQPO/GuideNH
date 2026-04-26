package com.hfstudio.guidenh.guide.internal.editor.preview;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;

import org.joml.Vector3f;

import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.internal.editor.SceneEditorSession;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorElementModel;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorSceneModel;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.annotation.DiamondAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldBoxAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldLineAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.SceneAnnotation;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

public final class SceneEditorPreviewBridge {

    private final Path workingRoot;
    private final SceneEditorPreviewCameraController previewCameraController;

    public SceneEditorPreviewBridge(Path workingRoot) {
        this.workingRoot = workingRoot;
        this.previewCameraController = new SceneEditorPreviewCameraController();
    }

    public LytGuidebookScene buildScene(SceneEditorSession session) {
        SceneEditorSceneModel model = session.getSceneModel();
        LytGuidebookScene scene = new LytGuidebookScene();
        scene.setInteractive(true);
        scene.setSceneButtonsVisible(false);
        scene.setSceneSize(model.getPreviewWidth(), model.getPreviewHeight());
        applyExportCamera(scene.getCamera(), model);
        loadStructure(scene.getLevel(), session);
        for (SceneEditorElementModel element : model.getElements()) {
            if (!element.isVisible()) {
                continue;
            }
            scene.addAnnotation(toRuntimeAnnotation(element));
        }
        if (!scene.getLevel()
            .isEmpty()) {
            scene.getLevel()
                .rebindAllTileEntities();
        }
        scene.snapshotInitialCamera();
        return scene;
    }

    private void applyExportCamera(CameraSettings camera, SceneEditorSceneModel model) {
        previewCameraController.applyModelCamera(camera, model);
    }

    private void loadStructure(GuidebookLevel level, SceneEditorSession session) {
        String structureText = session.getImportedStructureSnbt();
        if (structureText == null || structureText.isEmpty()) {
            String structureSource = session.getSceneModel()
                .getStructureSource();
            if (structureSource == null || structureSource.isEmpty()) {
                return;
            }
            Path path = workingRoot.resolve(structureSource)
                .normalize();
            if (!Files.exists(path)) {
                return;
            }
            try {
                structureText = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            } catch (Exception ignored) {
                return;
            }
        }

        try {
            NBTTagCompound root = readStructureNbt(structureText.getBytes(StandardCharsets.UTF_8));
            loadStructureIntoLevel(level, root);
        } catch (Exception ignored) {}
    }

    private void loadStructureIntoLevel(GuidebookLevel level, NBTTagCompound root) {
        if (!root.hasKey("palette") || !root.hasKey("blocks")) {
            return;
        }
        NBTTagList paletteTag = root.getTagList("palette", 10);
        String[] palette = new String[paletteTag.tagCount()];
        for (int i = 0; i < paletteTag.tagCount(); i++) {
            palette[i] = paletteTag.getCompoundTagAt(i)
                .getString("Name");
        }

        NBTTagList blocksTag = root.getTagList("blocks", 10);
        for (int i = 0; i < blocksTag.tagCount(); i++) {
            NBTTagCompound blockTag = blocksTag.getCompoundTagAt(i);
            int state = blockTag.getInteger("state");
            if (state < 0 || state >= palette.length) {
                continue;
            }
            Block block = (Block) Block.blockRegistry.getObject(palette[state]);
            if (block == null || block == Blocks.air) {
                continue;
            }
            int[] pos = blockTag.getIntArray("pos");
            if (pos.length < 3) {
                continue;
            }
            int meta = blockTag.hasKey("meta") ? blockTag.getInteger("meta") : 0;
            TileEntity tileEntity = null;
            if (blockTag.hasKey("nbt", 10)) {
                try {
                    tileEntity = TileEntity.createAndLoadEntity(blockTag.getCompoundTag("nbt"));
                } catch (Exception ignored) {}
            }
            level.setBlock(pos[0], pos[1], pos[2], block, meta, tileEntity);
        }
    }

    private SceneAnnotation toRuntimeAnnotation(SceneEditorElementModel element) {
        ConstantColor color = parseColor(element.getColorLiteral());
        switch (element.getType()) {
            case BLOCK: {
                Vector3f min = new Vector3f(element.getPrimaryX(), element.getPrimaryY(), element.getPrimaryZ());
                Vector3f max = new Vector3f(
                    element.getPrimaryX() + 1f,
                    element.getPrimaryY() + 1f,
                    element.getPrimaryZ() + 1f);
                InWorldBoxAnnotation annotation = new InWorldBoxAnnotation(min, max, color, element.getThickness());
                annotation.setAlwaysOnTop(element.isAlwaysOnTop());
                if (!element.getTooltipMarkdown()
                    .isEmpty()) {
                    annotation.setTooltipText(element.getTooltipMarkdown());
                }
                return annotation;
            }
            case BOX: {
                Vector3f min = new Vector3f(element.getPrimaryX(), element.getPrimaryY(), element.getPrimaryZ());
                Vector3f max = new Vector3f(element.getSecondaryX(), element.getSecondaryY(), element.getSecondaryZ());
                normalizeBounds(min, max);
                InWorldBoxAnnotation annotation = new InWorldBoxAnnotation(min, max, color, element.getThickness());
                annotation.setAlwaysOnTop(element.isAlwaysOnTop());
                if (!element.getTooltipMarkdown()
                    .isEmpty()) {
                    annotation.setTooltipText(element.getTooltipMarkdown());
                }
                return annotation;
            }
            case LINE: {
                InWorldLineAnnotation annotation = new InWorldLineAnnotation(
                    new Vector3f(element.getPrimaryX(), element.getPrimaryY(), element.getPrimaryZ()),
                    new Vector3f(element.getSecondaryX(), element.getSecondaryY(), element.getSecondaryZ()),
                    color,
                    element.getThickness());
                annotation.setAlwaysOnTop(element.isAlwaysOnTop());
                if (!element.getTooltipMarkdown()
                    .isEmpty()) {
                    annotation.setTooltipText(element.getTooltipMarkdown());
                }
                return annotation;
            }
            case DIAMOND:
            default: {
                DiamondAnnotation annotation = new DiamondAnnotation(
                    new Vector3f(element.getPrimaryX(), element.getPrimaryY(), element.getPrimaryZ()),
                    color);
                annotation.setAlwaysOnTop(element.isAlwaysOnTop());
                if (!element.getTooltipMarkdown()
                    .isEmpty()) {
                    annotation.setTooltipText(element.getTooltipMarkdown());
                }
                return annotation;
            }
        }
    }

    private void normalizeBounds(Vector3f min, Vector3f max) {
        if (min.x > max.x) {
            float swap = min.x;
            min.x = max.x;
            max.x = swap;
        }
        if (min.y > max.y) {
            float swap = min.y;
            min.y = max.y;
            max.y = swap;
        }
        if (min.z > max.z) {
            float swap = min.z;
            min.z = max.z;
            max.z = swap;
        }
    }

    private ConstantColor parseColor(String colorLiteral) {
        if (colorLiteral == null || colorLiteral.isEmpty()) {
            return ConstantColor.WHITE;
        }
        if ("transparent".equalsIgnoreCase(colorLiteral)) {
            return ConstantColor.TRANSPARENT;
        }
        String normalized = colorLiteral.startsWith("#") ? colorLiteral.substring(1) : colorLiteral;
        if (normalized.length() == 6) {
            normalized = "FF" + normalized;
        }
        int color = (int) Long.parseLong(normalized.toUpperCase(Locale.ROOT), 16);
        return new ConstantColor(color);
    }

    private static NBTTagCompound readStructureNbt(byte[] data) throws Exception {
        if (looksLikeText(data)) {
            String text = new String(data, StandardCharsets.UTF_8);
            if (!text.isEmpty() && text.charAt(0) == '\uFEFF') {
                text = text.substring(1);
            }
            NBTBase parsed = JsonToNBT.func_150315_a(text);
            if (parsed instanceof NBTTagCompound compound) {
                return compound;
            }
            throw new IllegalStateException("SNBT root must be a Compound");
        }
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(data));
            DataInputStream input = new DataInputStream(gzip)) {
            return CompressedStreamTools.read(input);
        } catch (Exception ignored) {
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(data))) {
                return CompressedStreamTools.read(input);
            }
        }
    }

    private static boolean looksLikeText(byte[] data) {
        int index = 0;
        if (data.length >= 3 && (data[0] & 0xFF) == 0xEF && (data[1] & 0xFF) == 0xBB && (data[2] & 0xFF) == 0xBF) {
            index = 3;
        }
        while (index < data.length) {
            byte b = data[index];
            if (b == ' ' || b == '\t' || b == '\r' || b == '\n') {
                index++;
                continue;
            }
            return b == '{';
        }
        return false;
    }
}
