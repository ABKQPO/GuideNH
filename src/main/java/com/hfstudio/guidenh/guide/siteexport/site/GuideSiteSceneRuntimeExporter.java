package com.hfstudio.guidenh.guide.siteexport.site;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.shader.Framebuffer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.google.flatbuffers.FlatBufferBuilder;
import com.hfstudio.guidenh.guide.internal.editor.io.SceneEditorOffscreenFramebuffer;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.GuidebookLevelRenderer;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;

import guideme.flatbuffers.scene.ExpCameraSettings;
import guideme.flatbuffers.scene.ExpDepthTest;
import guideme.flatbuffers.scene.ExpIndexElementType;
import guideme.flatbuffers.scene.ExpMaterial;
import guideme.flatbuffers.scene.ExpMesh;
import guideme.flatbuffers.scene.ExpPrimitiveType;
import guideme.flatbuffers.scene.ExpSampler;
import guideme.flatbuffers.scene.ExpScene;
import guideme.flatbuffers.scene.ExpTransparency;
import guideme.flatbuffers.scene.ExpVertexElementType;
import guideme.flatbuffers.scene.ExpVertexElementUsage;
import guideme.flatbuffers.scene.ExpVertexFormat;
import guideme.flatbuffers.scene.ExpVertexFormatElement;

public class GuideSiteSceneRuntimeExporter {

    private static final int PLACEHOLDER_SCALE = 2;
    private static final Logger LOGGER = LogManager.getLogger("GuideNH/SiteExportScene");

    private final GuideSiteAssetRegistry assets;

    public GuideSiteSceneRuntimeExporter(GuideSiteAssetRegistry assets) {
        this.assets = assets;
    }

    public GuideSiteExportedScene exportScene(LytGuidebookScene scene) throws Exception {
        byte[] placeholderBytes = renderPlaceholder(scene);
        byte[] sceneBytes = exportScenePayload(scene);

        GuideSiteSceneExporter exporter = new GuideSiteSceneExporter(
            assets,
            new GuideSiteSceneExporter.BytesProducer() {

                @Override
                public byte[] produce() {
                    return placeholderBytes;
                }
            },
            new GuideSiteSceneExporter.BytesProducer() {

                @Override
                public byte[] produce() {
                    return sceneBytes;
                }
            });
        GuideSiteSceneExporter.SceneFiles files = exporter.writeSceneAssets();
        return new GuideSiteExportedScene(files.placeholderPath(), files.scenePath());
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
        GuideSiteSceneTessellatorCapture recorder = new GuideSiteSceneTessellatorCapture(assets);
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
            LOGGER.warn(
                "Scene site export captured no tessellated meshes for a {}x{} scene; exported 3D preview will be blank.",
                Integer.valueOf(width),
                Integer.valueOf(height));
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

            GuidebookLevelRenderer.getInstance()
                .render(scene.getLevel(), scene.getCamera(), 0, 0, width, height, 0.0f);
        } finally {
            framebuffer.unbindFramebuffer();
            framebuffer.deleteFramebuffer();
            minecraft.displayWidth = previousDisplayWidth;
            minecraft.displayHeight = previousDisplayHeight;
            minecraft.gameSettings.guiScale = previousGuiScale;
            GL11.glViewport(0, 0, previousDisplayWidth, previousDisplayHeight);
        }
    }

    private byte[] encodeScene(CameraSettings camera, GuideSiteSceneTessellatorCapture.RecordingResult result)
        throws Exception {
        FlatBufferBuilder builder = new FlatBufferBuilder(1024);

        Map<GuideSiteSceneTessellatorCapture.VertexFormatKey, Integer> vertexFormats =
            new LinkedHashMap<GuideSiteSceneTessellatorCapture.VertexFormatKey, Integer>();
        Map<GuideSiteSceneTessellatorCapture.MaterialKey, Integer> materials =
            new LinkedHashMap<GuideSiteSceneTessellatorCapture.MaterialKey, Integer>();
        List<Integer> meshOffsets = new ArrayList<Integer>(result.meshes.size());

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
            ExpMesh.addMaterial(builder, materialOffset.intValue());
            ExpMesh.addVertexFormat(builder, vertexFormatOffset.intValue());
            ExpMesh.addPrimitiveType(builder, mesh.primitiveType);
            ExpMesh.addIndexBuffer(builder, indexBufferOffset);
            ExpMesh.addIndexType(builder, mesh.indexType);
            ExpMesh.addIndexCount(builder, mesh.indexCount);
            ExpMesh.addVertexBuffer(builder, vertexBufferOffset);
            meshOffsets.add(Integer.valueOf(ExpMesh.endExpMesh(builder)));
        }

        int meshesOffset = ExpScene.createMeshesVector(builder, toIntArray(meshOffsets));
        int animatedTexturesOffset = ExpScene.createAnimatedTexturesVector(builder, new int[0]);
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

    private int writeVertexFormat(FlatBufferBuilder builder, GuideSiteSceneTessellatorCapture.VertexFormatKey key) {
        int offset = 0;

        List<VertexFormatElementDef> elements = new ArrayList<VertexFormatElementDef>();
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
            int textureIdOffset = builder.createString(key.textureId);
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

    private static int[] toIntArray(List<Integer> values) {
        int[] result = new int[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i)
                .intValue();
        }
        return result;
    }

    private static Tessellator swapTessellator(Tessellator next) throws Exception {
        Field field = Tessellator.class.getDeclaredField("instance");
        field.setAccessible(true);

        try {
            Field modifiers = Field.class.getDeclaredField("modifiers");
            modifiers.setAccessible(true);
            modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        } catch (NoSuchFieldException ignored) {}

        Tessellator previous = (Tessellator) field.get(null);
        field.set(null, next);
        return previous;
    }

    private static final class RecordingResult {

        private final List<CapturedMesh> meshes;

        private RecordingResult(List<CapturedMesh> meshes) {
            this.meshes = meshes;
        }
    }

    private static final class CapturedMesh {

        private final byte[] vertexBuffer;
        private final byte[] indexBuffer;
        private final long indexCount;
        private final int indexType;
        private final int primitiveType;
        private final VertexFormatKey vertexFormatKey;
        private final MaterialKey materialKey;

        private CapturedMesh(byte[] vertexBuffer, byte[] indexBuffer, long indexCount, int indexType, int primitiveType,
            VertexFormatKey vertexFormatKey, MaterialKey materialKey) {
            this.vertexBuffer = vertexBuffer;
            this.indexBuffer = indexBuffer;
            this.indexCount = indexCount;
            this.indexType = indexType;
            this.primitiveType = primitiveType;
            this.vertexFormatKey = vertexFormatKey;
            this.materialKey = materialKey;
        }
    }

    private static final class VertexFormatKey {

        private final boolean hasUv;
        private final boolean hasNormal;

        private VertexFormatKey(boolean hasUv, boolean hasNormal) {
            this.hasUv = hasUv;
            this.hasNormal = hasNormal;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof VertexFormatKey)) {
                return false;
            }
            VertexFormatKey other = (VertexFormatKey) obj;
            return hasUv == other.hasUv && hasNormal == other.hasNormal;
        }

        @Override
        public int hashCode() {
            int result = hasUv ? 1 : 0;
            return 31 * result + (hasNormal ? 1 : 0);
        }
    }

    private static final class VertexFormatElementDef {

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

    private static final class MaterialKey {

        private final String name;
        private final String shaderName;
        @Nullable
        private final String textureId;
        @Nullable
        private final String texturePath;
        private final boolean linearFiltering;
        private final boolean useMipmaps;
        private final boolean disableCulling;
        private final int transparency;
        private final int depthTest;

        private MaterialKey(String name, String shaderName, @Nullable String textureId, @Nullable String texturePath,
            boolean linearFiltering, boolean useMipmaps, boolean disableCulling, int transparency, int depthTest) {
            this.name = name;
            this.shaderName = shaderName;
            this.textureId = textureId;
            this.texturePath = texturePath;
            this.linearFiltering = linearFiltering;
            this.useMipmaps = useMipmaps;
            this.disableCulling = disableCulling;
            this.transparency = transparency;
            this.depthTest = depthTest;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof MaterialKey)) {
                return false;
            }
            MaterialKey other = (MaterialKey) obj;
            if (linearFiltering != other.linearFiltering || useMipmaps != other.useMipmaps
                || disableCulling != other.disableCulling
                || transparency != other.transparency
                || depthTest != other.depthTest) {
                return false;
            }
            if (!name.equals(other.name) || !shaderName.equals(other.shaderName)) {
                return false;
            }
            if (textureId == null ? other.textureId != null : !textureId.equals(other.textureId)) {
                return false;
            }
            return texturePath == null ? other.texturePath == null : texturePath.equals(other.texturePath);
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + shaderName.hashCode();
            result = 31 * result + (textureId != null ? textureId.hashCode() : 0);
            result = 31 * result + (texturePath != null ? texturePath.hashCode() : 0);
            result = 31 * result + (linearFiltering ? 1 : 0);
            result = 31 * result + (useMipmaps ? 1 : 0);
            result = 31 * result + (disableCulling ? 1 : 0);
            result = 31 * result + transparency;
            result = 31 * result + depthTest;
            return result;
        }
    }

    private static final class TextureExport {

        private final String textureId;
        private final String texturePath;
        private final boolean linearFiltering;
        private final boolean useMipmaps;

        private TextureExport(String textureId, String texturePath, boolean linearFiltering, boolean useMipmaps) {
            this.textureId = textureId;
            this.texturePath = texturePath;
            this.linearFiltering = linearFiltering;
            this.useMipmaps = useMipmaps;
        }
    }

    private static final class RecordedVertex {

        private final float x;
        private final float y;
        private final float z;
        private final float u;
        private final float v;
        private final int r;
        private final int g;
        private final int b;
        private final int a;
        private final byte nx;
        private final byte ny;
        private final byte nz;

        private RecordedVertex(float x, float y, float z, float u, float v, int r, int g, int b, int a, byte nx,
            byte ny, byte nz) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.u = u;
            this.v = v;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.nx = nx;
            this.ny = ny;
            this.nz = nz;
        }
    }

    private static final class RecordingTessellator extends Tessellator {

        private final GuideSiteAssetRegistry assets;
        private final List<CapturedMesh> meshes = new ArrayList<CapturedMesh>();
        private final Map<Integer, TextureExport> textures = new LinkedHashMap<Integer, TextureExport>();

        private final ArrayList<RecordedVertex> currentVertices = new ArrayList<RecordedVertex>();

        private boolean drawing;
        private int drawMode;
        private boolean hasTexture;
        private boolean hasColor;
        private boolean hasBrightness;
        private boolean hasNormals;
        private boolean colorDisabled;
        private float textureU;
        private float textureV;
        private int color = 0xFFFFFFFF;
        private int brightness;
        private int normal;
        private double xOffset;
        private double yOffset;
        private double zOffset;

        private RecordingTessellator(GuideSiteAssetRegistry assets) {
            this.assets = assets;
            this.defaultTexture = true;
        }

        private RecordingResult finish() {
            return new RecordingResult(new ArrayList<CapturedMesh>(meshes));
        }

        @Override
        public void startDrawing(int drawMode) {
            if (drawing) {
                throw new IllegalStateException("Already tesselating!");
            }
            drawing = true;
            currentVertices.clear();
            this.drawMode = drawMode;
            hasTexture = false;
            hasColor = false;
            hasBrightness = false;
            hasNormals = false;
            colorDisabled = false;
            textureU = 0.0f;
            textureV = 0.0f;
            color = 0xFFFFFFFF;
            brightness = 0;
            normal = 0;
            xOffset = 0.0D;
            yOffset = 0.0D;
            zOffset = 0.0D;
        }

        @Override
        public int draw() {
            if (!drawing) {
                throw new IllegalStateException("Not tesselating!");
            }
            drawing = false;

            if (!currentVertices.isEmpty()) {
                try {
                    captureCurrentMesh();
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to capture tessellator mesh", e);
                }
            }

            int bytes = currentVertices.size() * 32;
            currentVertices.clear();
            return bytes;
        }

        @Override
        public void setTextureUV(double u, double v) {
            hasTexture = true;
            textureU = (float) u;
            textureV = (float) v;
        }

        @Override
        public void setBrightness(int brightness) {
            hasBrightness = true;
            this.brightness = brightness;
        }

        @Override
        public void setColorOpaque_F(float red, float green, float blue) {
            setColorOpaque((int) (red * 255.0F), (int) (green * 255.0F), (int) (blue * 255.0F));
        }

        @Override
        public void setColorRGBA_F(float red, float green, float blue, float alpha) {
            setColorRGBA((int) (red * 255.0F), (int) (green * 255.0F), (int) (blue * 255.0F), (int) (alpha * 255.0F));
        }

        @Override
        public void setColorOpaque(int red, int green, int blue) {
            setColorRGBA(red, green, blue, 255);
        }

        @Override
        public void setColorRGBA(int red, int green, int blue, int alpha) {
            if (colorDisabled) {
                return;
            }
            hasColor = true;
            color = clamp(alpha) << 24 | clamp(blue) << 16 | clamp(green) << 8 | clamp(red);
        }

        @Override
        public void func_154352_a(byte red, byte green, byte blue) {
            setColorOpaque(red & 255, green & 255, blue & 255);
        }

        @Override
        public void setColorOpaque_I(int color) {
            setColorOpaque(color >> 16 & 255, color >> 8 & 255, color & 255);
        }

        @Override
        public void setColorRGBA_I(int color, int alpha) {
            setColorRGBA(color >> 16 & 255, color >> 8 & 255, color & 255, alpha);
        }

        @Override
        public void disableColor() {
            colorDisabled = true;
        }

        @Override
        public void setNormal(float x, float y, float z) {
            hasNormals = true;
            byte nx = (byte) ((int) (x * 127.0F));
            byte ny = (byte) ((int) (y * 127.0F));
            byte nz = (byte) ((int) (z * 127.0F));
            normal = nx & 255 | (ny & 255) << 8 | (nz & 255) << 16;
        }

        @Override
        public void setTranslation(double x, double y, double z) {
            xOffset = x;
            yOffset = y;
            zOffset = z;
        }

        @Override
        public void addTranslation(float x, float y, float z) {
            xOffset += x;
            yOffset += y;
            zOffset += z;
        }

        @Override
        public void addVertex(double x, double y, double z) {
            int rgba = hasColor ? color : 0xFFFFFFFF;
            byte nx = hasNormals ? (byte) (normal & 255) : 0;
            byte ny = hasNormals ? (byte) ((normal >> 8) & 255) : 0;
            byte nz = hasNormals ? (byte) ((normal >> 16) & 255) : 0;
            currentVertices.add(
                new RecordedVertex(
                    (float) (x + xOffset),
                    (float) (y + yOffset),
                    (float) (z + zOffset),
                    hasTexture ? textureU : 0.0f,
                    hasTexture ? textureV : 0.0f,
                    rgba & 255,
                    rgba >> 8 & 255,
                    rgba >> 16 & 255,
                    rgba >> 24 & 255,
                    nx,
                    ny,
                    nz));
        }

        private void captureCurrentMesh() throws Exception {
            if (currentVertices.isEmpty()) {
                return;
            }

            TextureExport texture = hasTexture ? exportCurrentTexture() : null;
            MaterialKey material = createMaterialKey(texture);
            VertexFormatKey vertexFormat = new VertexFormatKey(hasTexture, hasNormals);

            ByteBuffer vertexBuffer = buildVertexBuffer(vertexFormat);
            IndexData indexData = buildIndexData();

            meshes.add(
                new CapturedMesh(
                    toByteArray(vertexBuffer),
                    toByteArray(indexData.buffer),
                    indexData.indexCount,
                    indexData.indexType,
                    indexData.primitiveType,
                    vertexFormat,
                    material));
        }

        @Nullable
        private TextureExport exportCurrentTexture() throws Exception {
            int textureId = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            if (textureId <= 0) {
                return null;
            }

            TextureExport existing = textures.get(Integer.valueOf(textureId));
            if (existing != null) {
                return existing;
            }

            int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
            int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
            if (width <= 0 || height <= 0) {
                return null;
            }

            ByteBuffer pixels = BufferUtils.createByteBuffer(width * height * 4);
            GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int index = (x + y * width) * 4;
                    int r = pixels.get(index) & 0xFF;
                    int g = pixels.get(index + 1) & 0xFF;
                    int b = pixels.get(index + 2) & 0xFF;
                    int a = pixels.get(index + 3) & 0xFF;
                    image.setRGB(x, y, a << 24 | r << 16 | g << 8 | b);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);

            String texturePath = assets.writeShared("scene-textures", ".png", out.toByteArray());

            int magFilter = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER);
            int minFilter = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER);
            boolean linearFiltering = magFilter == GL11.GL_LINEAR;
            boolean useMipmaps = minFilter == GL11.GL_NEAREST_MIPMAP_NEAREST
                || minFilter == GL11.GL_LINEAR_MIPMAP_NEAREST
                || minFilter == GL11.GL_NEAREST_MIPMAP_LINEAR
                || minFilter == GL11.GL_LINEAR_MIPMAP_LINEAR;

            TextureExport export = new TextureExport("gltex-" + textureId, texturePath, linearFiltering, useMipmaps);
            textures.put(Integer.valueOf(textureId), export);
            return export;
        }

        private MaterialKey createMaterialKey(@Nullable TextureExport texture) {
            boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
            boolean cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
            boolean depthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);

            int transparency = mapTransparency(blendEnabled);
            int depthTest = mapDepthTest(depthEnabled);

            String shaderName;
            if (texture == null) {
                shaderName = "position_color";
            } else if (blendEnabled) {
                shaderName = hasNormals && !hasBrightness ? "rendertype_entity_translucent_cull"
                    : "rendertype_translucent";
            } else if (hasNormals && !hasBrightness) {
                shaderName = "rendertype_entity_cutout";
            } else {
                shaderName = "rendertype_cutout";
            }

            return new MaterialKey(
                "scene-mesh",
                shaderName,
                texture != null ? texture.textureId : null,
                texture != null ? texture.texturePath : null,
                texture != null && texture.linearFiltering,
                texture != null && texture.useMipmaps,
                !cullEnabled,
                transparency,
                depthTest);
        }

        private int mapTransparency(boolean blendEnabled) {
            if (!blendEnabled) {
                return ExpTransparency.DISABLED;
            }

            int src = GL11.glGetInteger(GL11.GL_BLEND_SRC);
            int dst = GL11.glGetInteger(GL11.GL_BLEND_DST);

            if (src == GL11.GL_SRC_ALPHA && dst == GL11.GL_ONE_MINUS_SRC_ALPHA) {
                return ExpTransparency.TRANSLUCENT;
            }
            if (src == GL11.GL_SRC_ALPHA && dst == GL11.GL_ONE) {
                return ExpTransparency.LIGHTNING;
            }
            if (src == GL11.GL_ONE && dst == GL11.GL_ONE) {
                return ExpTransparency.ADDITIVE;
            }
            return ExpTransparency.TRANSLUCENT;
        }

        private int mapDepthTest(boolean depthEnabled) {
            if (!depthEnabled) {
                return ExpDepthTest.DISABLED;
            }
            int depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
            if (depthFunc == GL11.GL_EQUAL) {
                return ExpDepthTest.EQUAL;
            }
            if (depthFunc == GL11.GL_GEQUAL || depthFunc == GL11.GL_GREATER) {
                return ExpDepthTest.GREATER;
            }
            return ExpDepthTest.LEQUAL;
        }

        private ByteBuffer buildVertexBuffer(VertexFormatKey vertexFormat) {
            int stride = 12 + (vertexFormat.hasUv ? 8 : 0) + 4 + (vertexFormat.hasNormal ? 4 : 0);
            ByteBuffer buffer = ByteBuffer.allocate(currentVertices.size() * stride)
                .order(ByteOrder.LITTLE_ENDIAN);

            for (RecordedVertex vertex : currentVertices) {
                buffer.putFloat(vertex.x);
                buffer.putFloat(vertex.y);
                buffer.putFloat(vertex.z);
                if (vertexFormat.hasUv) {
                    buffer.putFloat(vertex.u);
                    buffer.putFloat(vertex.v);
                }
                buffer.put((byte) vertex.r);
                buffer.put((byte) vertex.g);
                buffer.put((byte) vertex.b);
                buffer.put((byte) vertex.a);
                if (vertexFormat.hasNormal) {
                    buffer.put(vertex.nx);
                    buffer.put(vertex.ny);
                    buffer.put(vertex.nz);
                    buffer.put((byte) 0);
                }
            }

            buffer.flip();
            return buffer;
        }

        private IndexData buildIndexData() {
            int primitiveType = mapPrimitiveType(drawMode);
            int[] indices = buildIndices(drawMode, currentVertices.size());
            boolean useUInt = currentVertices.size() > 0xFFFF;
            ByteBuffer buffer = ByteBuffer.allocate(indices.length * (useUInt ? 4 : 2))
                .order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < indices.length; i++) {
                if (useUInt) {
                    buffer.putInt(indices[i]);
                } else {
                    buffer.putShort((short) indices[i]);
                }
            }
            buffer.flip();
            return new IndexData(
                buffer,
                indices.length,
                useUInt ? ExpIndexElementType.UINT : ExpIndexElementType.USHORT,
                primitiveType);
        }

        private int[] buildIndices(int drawMode, int vertexCount) {
            if (drawMode == GL11.GL_QUADS) {
                int quadCount = vertexCount / 4;
                int[] indices = new int[quadCount * 6];
                int cursor = 0;
                for (int quad = 0; quad < quadCount; quad++) {
                    int base = quad * 4;
                    indices[cursor++] = base;
                    indices[cursor++] = base + 1;
                    indices[cursor++] = base + 2;
                    indices[cursor++] = base + 2;
                    indices[cursor++] = base + 3;
                    indices[cursor++] = base;
                }
                return indices;
            }

            int[] indices = new int[vertexCount];
            for (int i = 0; i < vertexCount; i++) {
                indices[i] = i;
            }
            return indices;
        }

        private int mapPrimitiveType(int drawMode) {
            if (drawMode == GL11.GL_LINES) {
                return ExpPrimitiveType.LINES;
            }
            if (drawMode == GL11.GL_LINE_STRIP) {
                return ExpPrimitiveType.LINE_STRIP;
            }
            if (drawMode == GL11.GL_TRIANGLE_STRIP) {
                return ExpPrimitiveType.TRIANGLE_STRIP;
            }
            if (drawMode == GL11.GL_TRIANGLE_FAN) {
                return ExpPrimitiveType.TRIANGLE_FAN;
            }
            if (drawMode == GL11.GL_POINTS) {
                return ExpPrimitiveType.POINTS;
            }
            return ExpPrimitiveType.TRIANGLES;
        }

        private static int clamp(int value) {
            if (value < 0) {
                return 0;
            }
            if (value > 255) {
                return 255;
            }
            return value;
        }

        private static byte[] toByteArray(ByteBuffer buffer) {
            byte[] out = new byte[buffer.remaining()];
            buffer.get(out);
            return out;
        }
    }

    private static final class IndexData {

        private final ByteBuffer buffer;
        private final long indexCount;
        private final int indexType;
        private final int primitiveType;

        private IndexData(ByteBuffer buffer, long indexCount, int indexType, int primitiveType) {
            this.buffer = buffer;
            this.indexCount = indexCount;
            this.indexType = indexType;
            this.primitiveType = primitiveType;
        }
    }
}
