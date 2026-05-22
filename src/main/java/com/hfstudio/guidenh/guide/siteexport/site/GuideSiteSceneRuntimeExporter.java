package com.hfstudio.guidenh.guide.siteexport.site;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import com.google.flatbuffers.FlatBufferBuilder;
import com.hfstudio.guidenh.guide.color.LightDarkMode;
import com.hfstudio.guidenh.guide.internal.editor.io.SceneEditorOffscreenFramebuffer;
import com.hfstudio.guidenh.guide.internal.resource.GuideResourceAccess;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.GuidebookLevelRenderer;
import com.hfstudio.guidenh.guide.scene.GuidebookSceneLayerSelection;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;

import cpw.mods.fml.common.FMLLog;
import guideme.flatbuffers.scene.ExpAnimatedTexturePart;
import guideme.flatbuffers.scene.ExpAnimatedTexturePartFrame;
import guideme.flatbuffers.scene.ExpCameraSettings;
import guideme.flatbuffers.scene.ExpMaterial;
import guideme.flatbuffers.scene.ExpMesh;
import guideme.flatbuffers.scene.ExpSampler;
import guideme.flatbuffers.scene.ExpScene;
import guideme.flatbuffers.scene.ExpVertexElementType;
import guideme.flatbuffers.scene.ExpVertexElementUsage;
import guideme.flatbuffers.scene.ExpVertexFormat;
import guideme.flatbuffers.scene.ExpVertexFormatElement;

public class GuideSiteSceneRuntimeExporter {

    private static final int PLACEHOLDER_SCALE = 2;
    private static final ResourceLocation RAIN_TEXTURE = new ResourceLocation(
        "minecraft",
        "textures/environment/rain.png");
    private static final ResourceLocation SNOW_TEXTURE = new ResourceLocation(
        "minecraft",
        "textures/environment/snow.png");
    private static final String RAIN_ANIMATED_TEXTURE_ID = "guidenh-weather-rain";
    private static final String SNOW_ANIMATED_TEXTURE_ID = "guidenh-weather-snow";

    private final GuideSiteAssetRegistry assets;

    public GuideSiteSceneRuntimeExporter(GuideSiteAssetRegistry assets) {
        this.assets = assets;
    }

    public GuideSiteExportedScene exportScene(LytGuidebookScene scene) throws Exception {
        byte[] placeholderBytes = renderPlaceholder(scene);
        byte[] sceneBytes = exportScenePayload(scene);

        GuideSiteSceneExporter exporter = new GuideSiteSceneExporter(assets, () -> placeholderBytes, () -> sceneBytes);
        GuideSiteSceneExporter.SceneFiles files = exporter.writeSceneAssets();
        return new GuideSiteExportedScene(
            files.placeholderPath(),
            files.scenePath(),
            scene.getSceneWidth(),
            scene.getSceneHeight());
    }

    private byte[] renderPlaceholder(LytGuidebookScene scene) throws Exception {
        int originalBackground = scene.getSceneBackgroundColor();
        int originalBorder = scene.getSceneBorderColor();
        int originalWidth = scene.getSceneWidth();
        int originalHeight = scene.getSceneHeight();
        boolean originalSceneButtonsVisible = scene.isSceneButtonsVisible();
        boolean originalBottomControlsVisible = scene.isBottomControlsVisible();
        boolean originalReserveBottomControlArea = scene.isReserveBottomControlArea();

        int logicalWidth = Math.max(16, originalWidth);
        int logicalHeight = Math.max(16, originalHeight);
        int renderWidth = logicalWidth * PLACEHOLDER_SCALE;
        int renderHeight = logicalHeight * PLACEHOLDER_SCALE;

        try {
            scene.setSceneBackgroundColor(0x00000000);
            scene.setSceneBorderColor(0x00000000);
            scene.setSceneButtonsVisible(false);
            scene.setBottomControlsVisible(false);
            scene.setReserveBottomControlArea(false);
            scene.setSceneSize(renderWidth, renderHeight);
            scene.setCameraViewportOverride(logicalWidth, logicalHeight);

            BufferedImage image;
            try (SceneEditorOffscreenFramebuffer framebuffer = new SceneEditorOffscreenFramebuffer(
                renderWidth,
                renderHeight)) {
                image = framebuffer.render(scene);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } finally {
            scene.setSceneBackgroundColor(originalBackground);
            scene.setSceneBorderColor(originalBorder);
            scene.setSceneButtonsVisible(originalSceneButtonsVisible);
            scene.setBottomControlsVisible(originalBottomControlsVisible);
            scene.setReserveBottomControlArea(originalReserveBottomControlArea);
            scene.setSceneSize(originalWidth, originalHeight);
            scene.clearCameraViewportOverride();
        }
    }

    private byte[] exportScenePayload(LytGuidebookScene scene) throws Exception {
        Matrix4f inverseViewMatrix = new Matrix4f(
            scene.getCamera()
                .getViewMatrix()).invert();
        GuideSiteSceneTessellatorCapture recorder = new GuideSiteSceneTessellatorCapture(assets, inverseViewMatrix);
        int width = Math.max(16, scene.getSceneWidth());
        int height = Math.max(16, scene.getSceneHeight());

        try {
            GuideSiteSceneTessellatorCapture.activate(recorder);
            captureSceneMeshes(scene, width, height);
        } finally {
            GuideSiteSceneTessellatorCapture.deactivate();
        }

        GuideSiteSceneTessellatorCapture.RecordingResult result = recorder.finish();
        if (result.meshes.isEmpty()) {
            FMLLog.getLogger()
                .warn(
                    "Scene site export captured no tessellated meshes for a {}x{} scene; exported 3D preview will be blank.",
                    width,
                    height);
        }
        return encodeScene(scene.getCamera(), result);
    }

    private void captureSceneMeshes(LytGuidebookScene scene, int width, int height) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.gameSettings == null) {
            throw new IllegalStateException("Minecraft client is not ready for scene export.");
        }

        Framebuffer framebuffer = new Framebuffer(width, height, true);
        framebuffer.setFramebufferColor(0f, 0f, 0f, 0f);

        int previousDisplayWidth = minecraft.displayWidth;
        int previousDisplayHeight = minecraft.displayHeight;
        int previousGuiScale = minecraft.gameSettings.guiScale;

        try {
            minecraft.displayWidth = width;
            minecraft.displayHeight = height;
            minecraft.gameSettings.guiScale = 1;

            framebuffer.bindFramebuffer(true);
            GL11.glClearColor(0f, 0f, 0f, 0f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

            Integer visibleLayerY = resolveVisibleLayerY(scene);
            GuidebookSceneLayerSelection layerSelection = GuidebookSceneLayerSelection.fromVisibleLayer(visibleLayerY);
            GuidebookLevelRenderer.getInstance()
                .render(
                    scene.getLevel(),
                    scene.getCamera(),
                    0,
                    0,
                    width,
                    height,
                    0,
                    0,
                    width,
                    height,
                    0.0f,
                    Collections.emptyList(),
                    LightDarkMode.LIGHT_MODE,
                    layerSelection,
                    scene.getRenderableParticlesForExport(),
                    scene.getRenderableWeatherEffectsForExport(),
                    scene.getSceneAnimationTickForRender());
        } finally {
            framebuffer.unbindFramebuffer();
            framebuffer.deleteFramebuffer();
            minecraft.displayWidth = previousDisplayWidth;
            minecraft.displayHeight = previousDisplayHeight;
            minecraft.gameSettings.guiScale = previousGuiScale;
            GL11.glViewport(0, 0, previousDisplayWidth, previousDisplayHeight);
        }
    }

    @Nullable
    private Integer resolveVisibleLayerY(LytGuidebookScene scene) {
        return scene != null ? scene.getVisibleLayerYForExport() : null;
    }

    private byte[] encodeScene(CameraSettings camera, GuideSiteSceneTessellatorCapture.RecordingResult result)
        throws Exception {
        FlatBufferBuilder builder = new FlatBufferBuilder(1024);

        Map<GuideSiteSceneTessellatorCapture.VertexFormatKey, Integer> vertexFormats = new LinkedHashMap<>();
        Map<GuideSiteSceneTessellatorCapture.MaterialKey, Integer> materials = new LinkedHashMap<>();
        List<Integer> meshOffsets = new ArrayList<>(result.meshes.size());

        for (GuideSiteSceneTessellatorCapture.CapturedMesh mesh : result.meshes) {
            Integer vertexFormatOffset = vertexFormats.get(mesh.vertexFormatKey);
            if (vertexFormatOffset == null) {
                vertexFormatOffset = writeVertexFormat(builder, mesh.vertexFormatKey);
                vertexFormats.put(mesh.vertexFormatKey, vertexFormatOffset);
            }

            Integer materialOffset = materials.get(mesh.materialKey);
            if (materialOffset == null) {
                materialOffset = writeMaterial(builder, mesh.materialKey);
                materials.put(mesh.materialKey, materialOffset);
            }

            int vertexBufferOffset = ExpMesh.createVertexBufferVector(builder, mesh.vertexBuffer);
            int indexBufferOffset = ExpMesh.createIndexBufferVector(builder, mesh.indexBuffer);

            ExpMesh.startExpMesh(builder);
            ExpMesh.addMaterial(builder, materialOffset);
            ExpMesh.addVertexFormat(builder, vertexFormatOffset);
            ExpMesh.addPrimitiveType(builder, mesh.primitiveType);
            ExpMesh.addIndexBuffer(builder, indexBufferOffset);
            ExpMesh.addIndexType(builder, mesh.indexType);
            ExpMesh.addIndexCount(builder, mesh.indexCount);
            ExpMesh.addVertexBuffer(builder, vertexBufferOffset);
            meshOffsets.add(ExpMesh.endExpMesh(builder));
        }

        int meshesOffset = ExpScene.createMeshesVector(builder, toIntArray(meshOffsets));
        int animatedTexturesOffset = ExpScene
            .createAnimatedTexturesVector(builder, writeAnimatedTextures(builder, result.textures));
        int cameraOffset = ExpCameraSettings.createExpCameraSettings(
            builder,
            camera.getRotationY(),
            camera.getRotationX(),
            camera.getRotationZ(),
            camera.getZoom());

        ExpScene.startExpScene(builder);
        ExpScene.addCamera(builder, cameraOffset);
        ExpScene.addMeshes(builder, meshesOffset);
        ExpScene.addAnimatedTextures(builder, animatedTexturesOffset);
        ExpScene.finishExpSceneBuffer(builder, ExpScene.endExpScene(builder));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(out);
        gzip.write(builder.sizedByteArray());
        gzip.finish();
        gzip.close();
        return out.toByteArray();
    }

    private int[] writeAnimatedTextures(FlatBufferBuilder builder,
        List<GuideSiteSceneTessellatorCapture.ExportedTexture> textures) throws Exception {
        if (textures == null || textures.isEmpty()) {
            return new int[0];
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        IResourceManager resourceManager = minecraft != null ? minecraft.getResourceManager() : null;
        if (resourceManager == null) {
            return new int[0];
        }
        ArrayList<Integer> animatedTextureOffsets = new ArrayList<>(2);
        appendWeatherAnimatedTexture(
            builder,
            animatedTextureOffsets,
            textures,
            resourceManager,
            RAIN_TEXTURE,
            RAIN_ANIMATED_TEXTURE_ID);
        appendWeatherAnimatedTexture(
            builder,
            animatedTextureOffsets,
            textures,
            resourceManager,
            SNOW_TEXTURE,
            SNOW_ANIMATED_TEXTURE_ID);
        return toIntArray(animatedTextureOffsets);
    }

    private void appendWeatherAnimatedTexture(FlatBufferBuilder builder, List<Integer> out,
        List<GuideSiteSceneTessellatorCapture.ExportedTexture> textures, IResourceManager resourceManager,
        ResourceLocation sourceTexture, String animatedTextureId) throws Exception {
        GuideSiteSceneTessellatorCapture.ExportedTexture exportedTexture = findTextureBySourceId(
            textures,
            sourceTexture);
        if (exportedTexture == null) {
            return;
        }
        byte[] sourceBytes = GuideResourceAccess.readBytes(resourceManager, sourceTexture);
        if (sourceBytes == null || sourceBytes.length <= 0) {
            return;
        }
        int animatedTextureOffset = writeAnimatedTexturePart(
            builder,
            sourceBytes,
            exportedTexture.texturePath,
            animatedTextureId);
        if (animatedTextureOffset != 0) {
            out.add(animatedTextureOffset);
        }
    }

    @Nullable
    private GuideSiteSceneTessellatorCapture.ExportedTexture findTextureBySourceId(
        List<GuideSiteSceneTessellatorCapture.ExportedTexture> textures, ResourceLocation sourceTexture) {
        String sourceTextureId = sourceTexture.toString();
        for (GuideSiteSceneTessellatorCapture.ExportedTexture texture : textures) {
            if (texture == null || texture.sourceTextureId == null) {
                continue;
            }
            if (sourceTextureId.equals(texture.sourceTextureId)) {
                return texture;
            }
        }
        return null;
    }

    private int writeAnimatedTexturePart(FlatBufferBuilder builder, byte[] sourceBytes, String framesPath,
        String animatedTextureId) throws Exception {
        BufferedImage sourceImage = ImageIO.read(new ByteArrayInputStream(sourceBytes));
        if (sourceImage == null) {
            return 0;
        }
        int frameWidth = sourceImage.getWidth();
        int frameHeight = sourceImage.getWidth();
        if (frameWidth <= 0 || frameHeight <= 0 || sourceImage.getHeight() < frameHeight) {
            return 0;
        }
        int frameCount = Math.max(1, sourceImage.getHeight() / frameHeight);

        ExpAnimatedTexturePart.startFramesVector(builder, frameCount);
        for (int frameIndex = frameCount - 1; frameIndex >= 0; frameIndex--) {
            ExpAnimatedTexturePartFrame.createExpAnimatedTexturePartFrame(builder, frameIndex, 1);
        }
        int framesOffset = builder.endVector();
        int textureIdOffset = builder.createString(animatedTextureId);
        int framesPathOffset = builder.createString(framesPath);
        return ExpAnimatedTexturePart.createExpAnimatedTexturePart(
            builder,
            textureIdOffset,
            0,
            0,
            frameWidth,
            frameHeight,
            framesPathOffset,
            frameCount,
            1,
            framesOffset);
    }

    private int writeVertexFormat(FlatBufferBuilder builder, GuideSiteSceneTessellatorCapture.VertexFormatKey key) {
        int offset = 0;

        List<VertexFormatElementDef> elements = new ArrayList<>();
        elements.add(
            new VertexFormatElementDef(
                0,
                ExpVertexElementType.FLOAT,
                ExpVertexElementUsage.POSITION,
                3,
                offset,
                12,
                false));
        offset += 12;

        if (key.hasUv) {
            elements.add(
                new VertexFormatElementDef(
                    0,
                    ExpVertexElementType.FLOAT,
                    ExpVertexElementUsage.UV,
                    2,
                    offset,
                    8,
                    false));
            offset += 8;
        }

        elements.add(
            new VertexFormatElementDef(0, ExpVertexElementType.UBYTE, ExpVertexElementUsage.COLOR, 4, offset, 4, true));
        offset += 4;

        if (key.hasNormal) {
            elements.add(
                new VertexFormatElementDef(
                    0,
                    ExpVertexElementType.BYTE,
                    ExpVertexElementUsage.NORMAL,
                    3,
                    offset,
                    4,
                    true));
            offset += 4;
        }

        ExpVertexFormat.startElementsVector(builder, elements.size());
        for (int i = elements.size() - 1; i >= 0; i--) {
            VertexFormatElementDef element = elements.get(i);
            ExpVertexFormatElement.createExpVertexFormatElement(
                builder,
                element.index,
                element.type,
                element.usage,
                element.count,
                element.offset,
                element.byteSize,
                element.normalized);
        }
        int elementsOffset = builder.endVector();
        ExpVertexFormat.startExpVertexFormat(builder);
        ExpVertexFormat.addElements(builder, elementsOffset);
        ExpVertexFormat.addVertexSize(builder, offset);
        return ExpVertexFormat.endExpVertexFormat(builder);
    }

    private int writeMaterial(FlatBufferBuilder builder, GuideSiteSceneTessellatorCapture.MaterialKey key) {
        int nameOffset = builder.createString(key.name);
        int shaderNameOffset = builder.createString(key.shaderName);

        int samplersOffset = 0;
        if (key.texturePath != null) {
            int textureIdOffset = builder.createString(resolveTextureId(key));
            int texturePathOffset = builder.createString(key.texturePath);
            int samplerOffset = ExpSampler
                .createExpSampler(builder, textureIdOffset, texturePathOffset, key.linearFiltering, key.useMipmaps);
            samplersOffset = ExpMaterial.createSamplersVector(builder, new int[] { samplerOffset });
        }

        return ExpMaterial.createExpMaterial(
            builder,
            nameOffset,
            shaderNameOffset,
            key.disableCulling,
            key.transparency,
            key.depthTest,
            samplersOffset);
    }

    private String resolveTextureId(GuideSiteSceneTessellatorCapture.MaterialKey key) {
        if (key.sourceTextureId != null) {
            if (key.sourceTextureId.equals(RAIN_TEXTURE.toString())) {
                return RAIN_ANIMATED_TEXTURE_ID;
            }
            if (key.sourceTextureId.equals(SNOW_TEXTURE.toString())) {
                return SNOW_ANIMATED_TEXTURE_ID;
            }
        }
        return key.textureId;
    }

    private static int[] toIntArray(List<Integer> values) {
        int[] result = new int[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i);
        }
        return result;
    }

    private static class VertexFormatElementDef {

        private final int index;
        private final int type;
        private final int usage;
        private final int count;
        private final int offset;
        private final int byteSize;
        private final boolean normalized;

        private VertexFormatElementDef(int index, int type, int usage, int count, int offset, int byteSize,
            boolean normalized) {
            this.index = index;
            this.type = type;
            this.usage = usage;
            this.count = count;
            this.offset = offset;
            this.byteSize = byteSize;
            this.normalized = normalized;
        }
    }
}
