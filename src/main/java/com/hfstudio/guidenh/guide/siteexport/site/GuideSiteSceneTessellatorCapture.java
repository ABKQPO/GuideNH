package com.hfstudio.guidenh.guide.siteexport.site;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.imageio.ImageIO;

import net.minecraft.client.renderer.OpenGlHelper;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

import guideme.flatbuffers.scene.ExpDepthTest;
import guideme.flatbuffers.scene.ExpIndexElementType;
import guideme.flatbuffers.scene.ExpPrimitiveType;
import guideme.flatbuffers.scene.ExpTransparency;

public class GuideSiteSceneTessellatorCapture {

    // Scene export capture is only expected to run on the client render thread, so a
    // direct active reference is cheaper than paying a ThreadLocal lookup on every vertex.
    private static volatile @Nullable GuideSiteSceneTessellatorCapture ACTIVE;

    private final GuideSiteAssetRegistry assets;
    private final Matrix4f inverseViewMatrix;
    private final Matrix4f currentWorldMatrix = new Matrix4f();
    private final Matrix4f modelViewMatrix = new Matrix4f();
    private final Matrix3f currentNormalMatrix = new Matrix3f();
    private final VertexDecodeScratch decodeScratch = new VertexDecodeScratch();
    private final FloatBuffer modelViewBuffer = BufferUtils.createFloatBuffer(16);
    private final List<CapturedMesh> meshes = new ArrayList<>();
    private final Map<Integer, TextureExport> textures = new LinkedHashMap<>();
    private static final int RAW_VERTEX_STRIDE = 8;
    private static final byte[] EMPTY_VERTEX_BYTES = new byte[0];

    private boolean drawing;
    private int drawMode;
    private boolean hasTexture;
    private boolean hasBrightness;
    private boolean hasNormals;
    private byte[] currentVertexBytes = EMPTY_VERTEX_BYTES;
    private int currentVertexCount;
    @Nullable
    private String currentSourceTextureId;

    public GuideSiteSceneTessellatorCapture(GuideSiteAssetRegistry assets, Matrix4f inverseViewMatrix) {
        this.assets = assets;
        this.inverseViewMatrix = new Matrix4f(inverseViewMatrix);
    }

    public static void activate(GuideSiteSceneTessellatorCapture capture) {
        if (capture == null) {
            throw new IllegalArgumentException("capture");
        }
        GuideSiteSceneTessellatorCapture previous = ACTIVE;
        if (previous != null && previous != capture) {
            throw new IllegalStateException("Another scene tessellator capture is already active.");
        }
        ACTIVE = capture;
    }

    public static void deactivate() {
        ACTIVE = null;
    }

    @Nullable
    public static GuideSiteSceneTessellatorCapture getActive() {
        return ACTIVE;
    }

    public RecordingResult finish() {
        ArrayList<ExportedTexture> exportedTextures = new ArrayList<>(textures.size());
        for (TextureExport texture : textures.values()) {
            exportedTextures.add(
                new ExportedTexture(
                    texture.textureId,
                    texture.texturePath,
                    texture.sourceTextureId,
                    texture.linearFiltering,
                    texture.useMipmaps));
        }
        return new RecordingResult(new ArrayList<>(meshes), exportedTextures);
    }

    public void setCurrentSourceTextureId(@Nullable String currentSourceTextureId) {
        this.currentSourceTextureId = currentSourceTextureId;
    }

    public void startDrawing(int drawMode) {
        if (drawing) {
            // A previous batch was not properly closed, so drop it before recording a new one.
            GuideDebugLog.warnAlways(
                "Scene capture startDrawing called while already drawing (mode={}); discarding previous unclosed batch",
                drawMode);
            drawing = false;
            currentVertexBytes = EMPTY_VERTEX_BYTES;
            currentVertexCount = 0;
        }
        drawing = true;
        currentVertexBytes = EMPTY_VERTEX_BYTES;
        currentVertexCount = 0;
        this.drawMode = drawMode;
        hasTexture = false;
        hasBrightness = false;
        hasNormals = false;
        captureCurrentWorldTransform();
    }

    public int draw() {
        if (!drawing) {
            // draw() called without a paired startDrawing(), so skip capture silently.
            return 0;
        }
        drawing = false;
        int vertexCount = currentVertexCount;
        try {
            if (vertexCount > 0) {
                captureCurrentMesh();
            }
        } catch (Throwable e) {
            GuideDebugLog.warnAlways("Scene capture mesh export failed ({} vertices)", vertexCount, e);
        } finally {
            currentVertexBytes = EMPTY_VERTEX_BYTES;
            currentVertexCount = 0;
        }
        return vertexCount * 32;
    }

    public void captureRawBuffer(int[] rawBuffer, int vertexCount, boolean hasTexture, boolean hasColor,
        boolean hasBrightness, boolean hasNormals) {
        this.hasTexture = hasTexture;
        this.hasBrightness = hasBrightness;
        this.hasNormals = hasNormals;
        currentVertexCount = sanitizeVertexCount(rawBuffer, vertexCount);
        if (currentVertexCount <= 0) {
            currentVertexBytes = EMPTY_VERTEX_BYTES;
            return;
        }
        currentVertexBytes = new byte[currentVertexCount * exportVertexStride(hasTexture, hasNormals)];
        appendCapturedVertices(
            rawBuffer,
            currentVertexCount,
            hasTexture,
            hasColor,
            hasNormals,
            currentWorldMatrix,
            currentNormalMatrix,
            decodeScratch,
            currentVertexBytes,
            0);
    }

    private void captureCurrentWorldTransform() {
        modelViewBuffer.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelViewBuffer);
        modelViewBuffer.flip();

        modelViewMatrix.set(modelViewBuffer);
        currentWorldMatrix.set(inverseViewMatrix)
            .mul(modelViewMatrix);

        try {
            currentNormalMatrix.set(currentWorldMatrix)
                .invert()
                .transpose();
        } catch (ArithmeticException ignored) {
            currentNormalMatrix.identity();
        }
    }

    private void captureCurrentMesh() throws Exception {
        if (currentVertexCount <= 0) {
            return;
        }

        TextureExport texture = hasTexture ? exportCurrentTexture() : null;
        MaterialKey material = createMaterialKey(texture);
        VertexFormatKey vertexFormat = new VertexFormatKey(hasTexture, hasNormals);
        EncodedIndexData indexData = buildIndexData(currentVertexCount);

        meshes.add(
            new CapturedMesh(
                currentVertexBytes,
                indexData.indexBuffer(),
                indexData.indexCount,
                indexData.indexType,
                indexData.primitiveType,
                vertexFormat,
                material));
    }

    /**
     * Maximum texture dimension for site export. Very large textures (e.g., GTNH's block atlas can be 8192x8192+)
     * are down-sampled via available mip levels to keep memory usage reasonable and export speed acceptable.
     */
    private static final int MAX_EXPORT_TEXTURE_SIZE = 8192;

    @Nullable
    private TextureExport exportCurrentTexture() throws Exception {
        int savedActiveUnit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        if (savedActiveUnit != OpenGlHelper.defaultTexUnit) {
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        }
        try {
            int textureId = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            if (textureId <= 0) {
                return null;
            }

            TextureExport existing = textures.get(textureId);
            if (existing != null) {
                return existing;
            }

            int level0Width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
            int level0Height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
            if (level0Width <= 0 || level0Height <= 0) {
                GuideDebugLog.warnAlways(
                    "exportCurrentTexture: bound texture id={} has invalid level-0 dimensions {}x{}; skipping",
                    textureId,
                    level0Width,
                    level0Height);
                return null;
            }

            // For large textures (e.g. the block atlas in GTNH can exceed 8192x8192), find the
            // smallest mip level that still fits within MAX_EXPORT_TEXTURE_SIZE. This avoids
            // allocating hundreds of MB just to download the atlas for a web preview.
            int exportMipLevel = 0;
            int exportWidth = level0Width;
            int exportHeight = level0Height;

            if (level0Width > MAX_EXPORT_TEXTURE_SIZE || level0Height > MAX_EXPORT_TEXTURE_SIZE) {
                for (int lv = 1; lv <= 12; lv++) {
                    int lw = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, lv, GL11.GL_TEXTURE_WIDTH);
                    int lh = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, lv, GL11.GL_TEXTURE_HEIGHT);
                    if (lw <= 0 || lh <= 0) break; // No more mip levels available
                    exportMipLevel = lv;
                    exportWidth = lw;
                    exportHeight = lh;
                    if (lw <= MAX_EXPORT_TEXTURE_SIZE && lh <= MAX_EXPORT_TEXTURE_SIZE) break;
                }
                GuideDebugLog.debugAlways(
                    "exportCurrentTexture: texture id={} is {}x{} - using mip level {} ({}x{}) for site export",
                    textureId,
                    level0Width,
                    level0Height,
                    exportMipLevel,
                    exportWidth,
                    exportHeight);
            } else {
                GuideDebugLog.debugAlways(
                    "exportCurrentTexture: exporting texture id={} ({}x{})",
                    textureId,
                    exportWidth,
                    exportHeight);
            }

            ByteBuffer pixels = BufferUtils.createByteBuffer(exportWidth * exportHeight * 4);
            GL11.glGetTexImage(GL11.GL_TEXTURE_2D, exportMipLevel, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);

            BufferedImage image = new BufferedImage(exportWidth, exportHeight, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < exportHeight; y++) {
                for (int x = 0; x < exportWidth; x++) {
                    int index = (x + y * exportWidth) * 4;
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

            TextureExport export = new TextureExport(
                "gltex-" + textureId,
                texturePath,
                currentSourceTextureId,
                linearFiltering,
                useMipmaps);
            textures.put(textureId, export);
            return export;
        } finally {
            if (savedActiveUnit != OpenGlHelper.defaultTexUnit) {
                OpenGlHelper.setActiveTexture(savedActiveUnit);
            }
        }
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
            shaderName = hasNormals && !hasBrightness ? "rendertype_entity_translucent_cull" : "rendertype_translucent";
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
            texture != null ? texture.sourceTextureId : null,
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

    private EncodedIndexData buildIndexData(int vertexCount) {
        return encodeIndexData(drawMode, vertexCount);
    }

    private static int mapPrimitiveType(int drawMode) {
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

    static int exportVertexStride(boolean hasUv, boolean hasNormal) {
        return 12 + (hasUv ? 8 : 0) + 4 + (hasNormal ? 4 : 0);
    }

    static EncodedIndexData encodeIndexData(int drawMode, int vertexCount) {
        int primitiveType = mapPrimitiveType(drawMode);
        boolean useUInt = vertexCount > 0xFFFF;
        int bytesPerIndex = useUInt ? Integer.BYTES : Short.BYTES;
        int indexCount = drawMode == GL11.GL_QUADS ? vertexCount / 4 * 6 : vertexCount;
        byte[] encoded = new byte[indexCount * bytesPerIndex];
        int cursor = 0;

        if (drawMode == GL11.GL_QUADS) {
            int quadCount = vertexCount / 4;
            for (int quad = 0; quad < quadCount; quad++) {
                int base = quad * 4;
                cursor = writeIndexLE(encoded, cursor, useUInt, base);
                cursor = writeIndexLE(encoded, cursor, useUInt, base + 1);
                cursor = writeIndexLE(encoded, cursor, useUInt, base + 2);
                cursor = writeIndexLE(encoded, cursor, useUInt, base + 2);
                cursor = writeIndexLE(encoded, cursor, useUInt, base + 3);
                cursor = writeIndexLE(encoded, cursor, useUInt, base);
            }
        } else {
            for (int index = 0; index < vertexCount; index++) {
                cursor = writeIndexLE(encoded, cursor, useUInt, index);
            }
        }

        return new EncodedIndexData(
            encoded,
            indexCount,
            useUInt ? ExpIndexElementType.UINT : ExpIndexElementType.USHORT,
            primitiveType);
    }

    static void appendCapturedVertices(int[] rawBuffer, int vertexCount, boolean hasTexture, boolean hasColor,
        boolean hasNormals, Matrix4f worldMatrix, Matrix3f normalMatrix, ByteBuffer target) {
        appendCapturedVertices(
            rawBuffer,
            vertexCount,
            hasTexture,
            hasColor,
            hasNormals,
            worldMatrix,
            normalMatrix,
            new VertexDecodeScratch(),
            target);
    }

    static void appendCapturedVertices(int[] rawBuffer, int vertexCount, boolean hasTexture, boolean hasColor,
        boolean hasNormals, Matrix4f worldMatrix, Matrix3f normalMatrix, VertexDecodeScratch scratch,
        ByteBuffer target) {
        int start = target.position();
        if (target.hasArray()) {
            int written = appendCapturedVertices(
                rawBuffer,
                vertexCount,
                hasTexture,
                hasColor,
                hasNormals,
                worldMatrix,
                normalMatrix,
                scratch,
                target.array(),
                target.arrayOffset() + start);
            target.position(start + written);
            return;
        }

        int sanitizedVertexCount = sanitizeVertexCount(rawBuffer, vertexCount);
        int written = sanitizedVertexCount * exportVertexStride(hasTexture, hasNormals);
        if (written <= 0) {
            return;
        }
        byte[] temp = new byte[written];
        appendCapturedVertices(
            rawBuffer,
            sanitizedVertexCount,
            hasTexture,
            hasColor,
            hasNormals,
            worldMatrix,
            normalMatrix,
            scratch,
            temp,
            0);
        target.put(temp, 0, written);
    }

    static int appendCapturedVertices(int[] rawBuffer, int vertexCount, boolean hasTexture, boolean hasColor,
        boolean hasNormals, Matrix4f worldMatrix, Matrix3f normalMatrix, VertexDecodeScratch scratch, byte[] target,
        int targetOffset) {
        if (rawBuffer == null || vertexCount <= 0) {
            return 0;
        }

        int stride = exportVertexStride(hasTexture, hasNormals);
        int sanitizedVertexCount = sanitizeVertexCount(rawBuffer, vertexCount);
        int cursor = targetOffset;
        for (int vertexIndex = 0; vertexIndex < sanitizedVertexCount; vertexIndex++) {
            int base = vertexIndex * RAW_VERTEX_STRIDE;
            scratch.transformedPosition.set(
                Float.intBitsToFloat(rawBuffer[base]),
                Float.intBitsToFloat(rawBuffer[base + 1]),
                Float.intBitsToFloat(rawBuffer[base + 2]));
            worldMatrix.transformPosition(scratch.transformedPosition);
            writeFloatLE(target, cursor, scratch.transformedPosition.x);
            cursor += Float.BYTES;
            writeFloatLE(target, cursor, scratch.transformedPosition.y);
            cursor += Float.BYTES;
            writeFloatLE(target, cursor, scratch.transformedPosition.z);
            cursor += Float.BYTES;

            if (hasTexture) {
                writeFloatLE(target, cursor, Float.intBitsToFloat(rawBuffer[base + 3]));
                cursor += Float.BYTES;
                writeFloatLE(target, cursor, Float.intBitsToFloat(rawBuffer[base + 4]));
                cursor += Float.BYTES;
            }

            int rgba = hasColor ? rawBuffer[base + 5] : 0xFFFFFFFF;
            target[cursor++] = (byte) (rgba & 255);
            target[cursor++] = (byte) (rgba >> 8 & 255);
            target[cursor++] = (byte) (rgba >> 16 & 255);
            target[cursor++] = (byte) (rgba >> 24 & 255);

            if (hasNormals) {
                int packedNormal = rawBuffer[base + 6];
                scratch.transformedNormal.set(
                    unpackNormalComponent(packedNormal),
                    unpackNormalComponent(packedNormal >> 8),
                    unpackNormalComponent(packedNormal >> 16));
                normalMatrix.transform(scratch.transformedNormal);
                if (scratch.transformedNormal.lengthSquared() > 1.0e-12f) {
                    scratch.transformedNormal.normalize();
                }
                target[cursor++] = packNormalComponent(scratch.transformedNormal.x);
                target[cursor++] = packNormalComponent(scratch.transformedNormal.y);
                target[cursor++] = packNormalComponent(scratch.transformedNormal.z);
                target[cursor++] = 0;
            }
        }
        return sanitizedVertexCount * stride;
    }

    private static int sanitizeVertexCount(int[] rawBuffer, int vertexCount) {
        if (rawBuffer == null || vertexCount <= 0) {
            return 0;
        }
        return Math.min(vertexCount, rawBuffer.length / RAW_VERTEX_STRIDE);
    }

    private static byte packNormalComponent(float value) {
        int packed = Math.round(Math.clamp(value, -1.0f, 1.0f) * 127.0f);
        if (packed < -128) {
            packed = -128;
        } else if (packed > 127) {
            packed = 127;
        }
        return (byte) packed;
    }

    private static float unpackNormalComponent(int packedByte) {
        return (byte) (packedByte & 0xFF) / 127.0f;
    }

    private static void writeFloatLE(byte[] target, int offset, float value) {
        int bits = Float.floatToRawIntBits(value);
        target[offset] = (byte) bits;
        target[offset + 1] = (byte) (bits >> 8);
        target[offset + 2] = (byte) (bits >> 16);
        target[offset + 3] = (byte) (bits >> 24);
    }

    private static int writeIndexLE(byte[] target, int offset, boolean useUInt, int index) {
        if (useUInt) {
            target[offset] = (byte) index;
            target[offset + 1] = (byte) (index >> 8);
            target[offset + 2] = (byte) (index >> 16);
            target[offset + 3] = (byte) (index >> 24);
            return offset + Integer.BYTES;
        }
        target[offset] = (byte) index;
        target[offset + 1] = (byte) (index >> 8);
        return offset + Short.BYTES;
    }

    public static class RecordingResult {

        public final List<CapturedMesh> meshes;
        public final List<ExportedTexture> textures;

        RecordingResult(List<CapturedMesh> meshes, List<ExportedTexture> textures) {
            this.meshes = meshes;
            this.textures = textures;
        }
    }

    public static class ExportedTexture {

        public final String textureId;
        public final String texturePath;
        @Nullable
        public final String sourceTextureId;
        public final boolean linearFiltering;
        public final boolean useMipmaps;

        ExportedTexture(String textureId, String texturePath, @Nullable String sourceTextureId, boolean linearFiltering,
            boolean useMipmaps) {
            this.textureId = textureId;
            this.texturePath = texturePath;
            this.sourceTextureId = sourceTextureId;
            this.linearFiltering = linearFiltering;
            this.useMipmaps = useMipmaps;
        }
    }

    static final class VertexDecodeScratch {

        final Vector3f transformedPosition = new Vector3f();
        final Vector3f transformedNormal = new Vector3f();
    }

    record EncodedIndexData(byte[] indexBuffer, long indexCount, int indexType, int primitiveType) {}

    public static class CapturedMesh {

        final byte[] vertexBuffer;
        final byte[] indexBuffer;
        final long indexCount;
        final int indexType;
        final int primitiveType;
        final VertexFormatKey vertexFormatKey;
        final MaterialKey materialKey;

        CapturedMesh(byte[] vertexBuffer, byte[] indexBuffer, long indexCount, int indexType, int primitiveType,
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

    public static class VertexFormatKey {

        public final boolean hasUv;
        public final boolean hasNormal;

        public VertexFormatKey(boolean hasUv, boolean hasNormal) {
            this.hasUv = hasUv;
            this.hasNormal = hasNormal;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof VertexFormatKey other)) {
                return false;
            }
            return hasUv == other.hasUv && hasNormal == other.hasNormal;
        }

        @Override
        public int hashCode() {
            int result = hasUv ? 1 : 0;
            return 31 * result + (hasNormal ? 1 : 0);
        }
    }

    public static class MaterialKey {

        public final String name;
        public final String shaderName;
        @Nullable
        public final String textureId;
        @Nullable
        public final String texturePath;
        @Nullable
        public final String sourceTextureId;
        public final boolean linearFiltering;
        public final boolean useMipmaps;
        public final boolean disableCulling;
        public final int transparency;
        public final int depthTest;

        public MaterialKey(String name, String shaderName, @Nullable String textureId, @Nullable String texturePath,
            @Nullable String sourceTextureId, boolean linearFiltering, boolean useMipmaps, boolean disableCulling,
            int transparency, int depthTest) {
            this.name = name;
            this.shaderName = shaderName;
            this.textureId = textureId;
            this.texturePath = texturePath;
            this.sourceTextureId = sourceTextureId;
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
            if (!(obj instanceof MaterialKey other)) {
                return false;
            }
            if (linearFiltering != other.linearFiltering || useMipmaps != other.useMipmaps
                || disableCulling != other.disableCulling
                || transparency != other.transparency
                || depthTest != other.depthTest) {
                return false;
            }
            if (!name.equals(other.name) || !shaderName.equals(other.shaderName)) {
                return false;
            }
            if (!Objects.equals(textureId, other.textureId)) {
                return false;
            }
            if (!Objects.equals(texturePath, other.texturePath)) {
                return false;
            }
            return Objects.equals(sourceTextureId, other.sourceTextureId);
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + shaderName.hashCode();
            result = 31 * result + (textureId != null ? textureId.hashCode() : 0);
            result = 31 * result + (texturePath != null ? texturePath.hashCode() : 0);
            result = 31 * result + (sourceTextureId != null ? sourceTextureId.hashCode() : 0);
            result = 31 * result + (linearFiltering ? 1 : 0);
            result = 31 * result + (useMipmaps ? 1 : 0);
            result = 31 * result + (disableCulling ? 1 : 0);
            result = 31 * result + transparency;
            result = 31 * result + depthTest;
            return result;
        }
    }

    private static class TextureExport {

        final String textureId;
        final String texturePath;
        @Nullable
        final String sourceTextureId;
        final boolean linearFiltering;
        final boolean useMipmaps;

        TextureExport(String textureId, String texturePath, @Nullable String sourceTextureId, boolean linearFiltering,
            boolean useMipmaps) {
            this.textureId = textureId;
            this.texturePath = texturePath;
            this.sourceTextureId = sourceTextureId;
            this.linearFiltering = linearFiltering;
            this.useMipmaps = useMipmaps;
        }
    }

}
