package com.hfstudio.guidenh.guide.siteexport.site;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import guideme.flatbuffers.scene.ExpDepthTest;
import guideme.flatbuffers.scene.ExpIndexElementType;
import guideme.flatbuffers.scene.ExpPrimitiveType;
import guideme.flatbuffers.scene.ExpTransparency;

public final class GuideSiteSceneTessellatorCapture {

    private static final ThreadLocal<GuideSiteSceneTessellatorCapture> ACTIVE = new ThreadLocal<GuideSiteSceneTessellatorCapture>();

    private final GuideSiteAssetRegistry assets;
    private final Matrix4f inverseViewMatrix;
    private final Matrix4f currentWorldMatrix = new Matrix4f();
    private final Matrix3f currentNormalMatrix = new Matrix3f();
    private final FloatBuffer modelViewBuffer = BufferUtils.createFloatBuffer(16);
    private final Vector3f transformedPosition = new Vector3f();
    private final Vector3f transformedNormal = new Vector3f();
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
    private float normalX;
    private float normalY;
    private float normalZ;
    private float textureU;
    private float textureV;
    private int color = 0xFFFFFFFF;
    private int brightness;
    private double xOffset;
    private double yOffset;
    private double zOffset;

    public GuideSiteSceneTessellatorCapture(GuideSiteAssetRegistry assets, Matrix4f inverseViewMatrix) {
        this.assets = assets;
        this.inverseViewMatrix = new Matrix4f(inverseViewMatrix);
    }

    public static void activate(GuideSiteSceneTessellatorCapture capture) {
        if (capture == null) {
            throw new IllegalArgumentException("capture");
        }
        GuideSiteSceneTessellatorCapture previous = ACTIVE.get();
        if (previous != null && previous != capture) {
            throw new IllegalStateException("Another scene tessellator capture is already active.");
        }
        ACTIVE.set(capture);
    }

    public static void deactivate() {
        ACTIVE.remove();
    }

    @Nullable
    public static GuideSiteSceneTessellatorCapture getActive() {
        return ACTIVE.get();
    }

    public RecordingResult finish() {
        return new RecordingResult(new ArrayList<CapturedMesh>(meshes));
    }

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
        normalX = 0.0f;
        normalY = 0.0f;
        normalZ = 0.0f;
        xOffset = 0.0D;
        yOffset = 0.0D;
        zOffset = 0.0D;
        captureCurrentWorldTransform();
    }

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

    public void setTextureUV(double u, double v) {
        hasTexture = true;
        textureU = (float) u;
        textureV = (float) v;
    }

    public void setBrightness(int brightness) {
        hasBrightness = true;
        this.brightness = brightness;
    }

    public void setColorOpaque_F(float red, float green, float blue) {
        setColorOpaque((int) (red * 255.0F), (int) (green * 255.0F), (int) (blue * 255.0F));
    }

    public void setColorRGBA_F(float red, float green, float blue, float alpha) {
        setColorRGBA((int) (red * 255.0F), (int) (green * 255.0F), (int) (blue * 255.0F), (int) (alpha * 255.0F));
    }

    public void setColorOpaque(int red, int green, int blue) {
        setColorRGBA(red, green, blue, 255);
    }

    public void setColorRGBA(int red, int green, int blue, int alpha) {
        if (colorDisabled) {
            return;
        }
        hasColor = true;
        color = clamp(alpha) << 24 | clamp(blue) << 16 | clamp(green) << 8 | clamp(red);
    }

    public void func_154352_a(byte red, byte green, byte blue) {
        setColorOpaque(red & 255, green & 255, blue & 255);
    }

    public void setColorOpaque_I(int color) {
        setColorOpaque(color >> 16 & 255, color >> 8 & 255, color & 255);
    }

    public void setColorRGBA_I(int color, int alpha) {
        setColorRGBA(color >> 16 & 255, color >> 8 & 255, color & 255, alpha);
    }

    public void disableColor() {
        colorDisabled = true;
    }

    public void setNormal(float x, float y, float z) {
        hasNormals = true;
        normalX = x;
        normalY = y;
        normalZ = z;
    }

    public void setTranslation(double x, double y, double z) {
        xOffset = x;
        yOffset = y;
        zOffset = z;
    }

    public void addTranslation(float x, float y, float z) {
        xOffset += x;
        yOffset += y;
        zOffset += z;
    }

    public void addVertex(double x, double y, double z) {
        int rgba = hasColor ? color : 0xFFFFFFFF;
        float px = (float) (x + xOffset);
        float py = (float) (y + yOffset);
        float pz = (float) (z + zOffset);

        transformedPosition.set(px, py, pz);
        currentWorldMatrix.transformPosition(transformedPosition);

        byte nx = 0;
        byte ny = 0;
        byte nz = 0;
        if (hasNormals) {
            transformedNormal.set(normalX, normalY, normalZ);
            currentNormalMatrix.transform(transformedNormal);
            if (transformedNormal.lengthSquared() > 1.0e-12f) {
                transformedNormal.normalize();
            }
            nx = packNormalComponent(transformedNormal.x);
            ny = packNormalComponent(transformedNormal.y);
            nz = packNormalComponent(transformedNormal.z);
        }

        currentVertices.add(
            new RecordedVertex(
                transformedPosition.x,
                transformedPosition.y,
                transformedPosition.z,
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

    private void captureCurrentWorldTransform() {
        modelViewBuffer.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelViewBuffer);
        modelViewBuffer.flip();

        Matrix4f modelViewMatrix = new Matrix4f().set(modelViewBuffer);
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
        boolean useMipmaps = minFilter == GL11.GL_NEAREST_MIPMAP_NEAREST || minFilter == GL11.GL_LINEAR_MIPMAP_NEAREST
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

    private static byte packNormalComponent(float value) {
        int packed = Math.round(Math.max(-1.0f, Math.min(1.0f, value)) * 127.0f);
        if (packed < -128) {
            packed = -128;
        } else if (packed > 127) {
            packed = 127;
        }
        return (byte) packed;
    }

    private static byte[] toByteArray(ByteBuffer buffer) {
        byte[] out = new byte[buffer.remaining()];
        buffer.get(out);
        return out;
    }

    static final class RecordingResult {

        final List<CapturedMesh> meshes;

        RecordingResult(List<CapturedMesh> meshes) {
            this.meshes = meshes;
        }
    }

    static final class CapturedMesh {

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

    static final class VertexFormatKey {

        final boolean hasUv;
        final boolean hasNormal;

        VertexFormatKey(boolean hasUv, boolean hasNormal) {
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

    static final class MaterialKey {

        final String name;
        final String shaderName;
        @Nullable
        final String textureId;
        @Nullable
        final String texturePath;
        final boolean linearFiltering;
        final boolean useMipmaps;
        final boolean disableCulling;
        final int transparency;
        final int depthTest;

        MaterialKey(String name, String shaderName, @Nullable String textureId, @Nullable String texturePath,
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

    static final class TextureExport {

        final String textureId;
        final String texturePath;
        final boolean linearFiltering;
        final boolean useMipmaps;

        TextureExport(String textureId, String texturePath, boolean linearFiltering, boolean useMipmaps) {
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
