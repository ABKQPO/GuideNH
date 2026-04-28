package com.hfstudio.guidenh.guide.scene;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

import com.hfstudio.guidenh.guide.document.LytSize;

public class CameraSettings {

    private float zoom = 1f;

    private final Vector4f viewport = new Vector4f();

    private float rotationX;
    private float rotationY;
    private float rotationZ;

    private final Vector3f rotationCenter = new Vector3f();

    private float offsetX;
    private float offsetY;

    private LytSize viewportSize = LytSize.empty();

    public CameraSettings() {
        setPerspectivePreset(PerspectivePreset.ISOMETRIC_NORTH_EAST);
    }

    public void setViewportSize(LytSize size) {
        setViewportSize(size.width(), size.height());
    }

    // Int overload to avoid LytSize allocation on per-frame hot paths.
    public void setViewportSize(int width, int height) {
        if (viewportSize.width() != width || viewportSize.height() != height) {
            this.viewportSize = new LytSize(width, height);
            var halfWidth = width / 2f;
            var halfHeight = height / 2f;
            viewport.set(-halfWidth, -halfHeight, halfWidth, halfHeight);
            markProjectionDirty();
        }
    }

    public LytSize getViewportSize() {
        return viewportSize;
    }

    public void setPerspectivePreset(PerspectivePreset preset) {
        switch (preset) {
            case ISOMETRIC_NORTH_EAST:
                setIsometricYawPitchRoll(225, 30, 0);
                break;
            case ISOMETRIC_NORTH_WEST:
                setIsometricYawPitchRoll(135, 30, 0);
                break;
            case UP:
                setIsometricYawPitchRoll(120, 0, 45);
                break;
            default:
                break;
        }
    }

    public void setIsometricYawPitchRoll(float yawDeg, float pitchDeg, float rollDeg) {
        if (rotationY != yawDeg || rotationX != pitchDeg || rotationZ != rollDeg) {
            rotationY = yawDeg;
            rotationX = pitchDeg;
            rotationZ = rollDeg;
            markViewDirty();
        }
    }

    public float getZoom() {
        return zoom;
    }

    public void setZoom(float zoom) {
        if (this.zoom != zoom) {
            this.zoom = zoom;
            markViewDirty();
        }
    }

    public float getRotationX() {
        return rotationX;
    }

    public void setRotationX(float rotationX) {
        if (this.rotationX != rotationX) {
            this.rotationX = rotationX;
            markViewDirty();
        }
    }

    public float getRotationY() {
        return rotationY;
    }

    public void setRotationY(float rotationY) {
        if (this.rotationY != rotationY) {
            this.rotationY = rotationY;
            markViewDirty();
        }
    }

    public float getRotationZ() {
        return rotationZ;
    }

    public void setRotationZ(float rotationZ) {
        if (this.rotationZ != rotationZ) {
            this.rotationZ = rotationZ;
            markViewDirty();
        }
    }

    public void setRotationCenter(Vector3fc rotationCenter) {
        setRotationCenter(rotationCenter.x(), rotationCenter.y(), rotationCenter.z());
    }

    public void setRotationCenter(float x, float y, float z) {
        if (rotationCenter.x != x || rotationCenter.y != y || rotationCenter.z != z) {
            this.rotationCenter.set(x, y, z);
            markViewDirty();
        }
    }

    public Vector3fc getRotationCenter() {
        return rotationCenter;
    }

    public float getOffsetX() {
        return offsetX;
    }

    public void setOffsetX(float offsetX) {
        if (this.offsetX != offsetX) {
            this.offsetX = offsetX;
            markViewDirty();
        }
    }

    public float getOffsetY() {
        return offsetY;
    }

    public void setOffsetY(float offsetY) {
        if (this.offsetY != offsetY) {
            this.offsetY = offsetY;
            markViewDirty();
        }
    }

    public static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    // Cached matrices: callers (renderer, worldToScreen, screenToWorldRay) consume the
    // result immediately or copy before mutating, so a single reusable instance per
    // kind is safe. Avoids two Matrix4f allocations per frame.
    private final Matrix4f reusableView = new Matrix4f();
    private final Matrix4f reusableProjection = new Matrix4f();
    private final Matrix4f reusableCombined = new Matrix4f();
    private final Matrix4f reusableInverted = new Matrix4f();
    private final Vector4f reusableWorldToScreen = new Vector4f();
    private final Vector4f reusableNear = new Vector4f();
    private final Vector4f reusableFar = new Vector4f();
    private boolean viewDirty = true;
    private boolean projectionDirty = true;
    private boolean combinedDirty = true;
    private boolean invertedDirty = true;

    public Matrix4f getViewMatrix() {
        if (viewDirty) {
            viewDirty = false;
            var result = reusableView.identity();
            result.translate(offsetX, offsetY, 0f);
            result.translate(rotationCenter.x, rotationCenter.y, rotationCenter.z);
            result.rotateZ(DEG_TO_RAD * rotationZ);
            result.rotateX(DEG_TO_RAD * rotationX);
            result.rotateY(DEG_TO_RAD * rotationY);
            result.translate(-rotationCenter.x, -rotationCenter.y, -rotationCenter.z);
        }
        return reusableView;
    }

    public Matrix4f getProjectionMatrix() {
        if (projectionDirty) {
            projectionDirty = false;
            float s = 0.625f * 16f * zoom;
            reusableProjection.identity()
                .setOrtho(viewport.x(), viewport.z(), viewport.y(), viewport.w(), -1000f, 3000f)
                // Keep zoom out of the model-view matrix so fixed-function lighting is not
                // skewed by our orthographic preview scale.
                .translate(offsetX, offsetY, 0f)
                .scale(s, s, 1f)
                .translate(-offsetX, -offsetY, 0f);
        }
        return reusableProjection;
    }

    public Matrix4f getCombinedMatrix() {
        if (combinedDirty) {
            combinedDirty = false;
            reusableCombined.set(getProjectionMatrix())
                .mul(getViewMatrix());
        }
        return reusableCombined;
    }

    public Vector3f worldToScreen(float worldX, float worldY, float worldZ) {
        return worldToScreen(worldX, worldY, worldZ, new Vector3f());
    }

    public Vector3f worldToScreen(float worldX, float worldY, float worldZ, Vector3f dest) {
        var v = reusableWorldToScreen.set(worldX, worldY, worldZ, 1f);
        getCombinedMatrix().transform(v);
        if (v.w != 0f) {
            v.x /= v.w;
            v.y /= v.w;
            v.z /= v.w;
        }
        float halfW = viewportSize.width() * 0.5f;
        float halfH = viewportSize.height() * 0.5f;
        return dest.set(v.x * halfW, -v.y * halfH, v.z);
    }

    public float[] screenToWorldRay(float screenX, float screenY) {
        return screenToWorldRay(screenX, screenY, new float[6]);
    }

    public float[] screenToWorldRay(float screenX, float screenY, float[] dest) {
        float halfW = viewportSize.width() * 0.5f;
        float halfH = viewportSize.height() * 0.5f;
        float ndcX = halfW == 0f ? 0f : screenX / halfW;
        float ndcY = halfH == 0f ? 0f : -screenY / halfH;

        var invMat = getInvertedCombinedMatrix();
        var near = reusableNear.set(ndcX, ndcY, -1f, 1f);
        var far = reusableFar.set(ndcX, ndcY, 1f, 1f);
        invMat.transform(near);
        invMat.transform(far);
        if (near.w != 0) {
            near.x /= near.w;
            near.y /= near.w;
            near.z /= near.w;
        }
        if (far.w != 0) {
            far.x /= far.w;
            far.y /= far.w;
            far.z /= far.w;
        }
        float dx = far.x - near.x;
        float dy = far.y - near.y;
        float dz = far.z - near.z;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len > 1e-6f) {
            dx /= len;
            dy /= len;
            dz /= len;
        }
        dest[0] = near.x;
        dest[1] = near.y;
        dest[2] = near.z;
        dest[3] = dx;
        dest[4] = dy;
        dest[5] = dz;
        return dest;
    }

    public SavedCameraSettings save() {
        return new SavedCameraSettings(rotationX, rotationY, rotationZ, offsetX, offsetY, zoom);
    }

    public void restore(SavedCameraSettings settings) {
        rotationX = settings.rotationX();
        rotationY = settings.rotationY();
        rotationZ = settings.rotationZ();
        offsetX = settings.offsetX();
        offsetY = settings.offsetY();
        zoom = settings.zoom();
        markViewDirty();
    }

    private Matrix4f getInvertedCombinedMatrix() {
        if (invertedDirty) {
            invertedDirty = false;
            reusableInverted.set(getCombinedMatrix())
                .invert();
        }
        return reusableInverted;
    }

    private void markViewDirty() {
        viewDirty = true;
        projectionDirty = true;
        combinedDirty = true;
        invertedDirty = true;
    }

    private void markProjectionDirty() {
        projectionDirty = true;
        combinedDirty = true;
        invertedDirty = true;
    }
}
