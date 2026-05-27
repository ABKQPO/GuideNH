package com.hfstudio.guidenh.guide.scene;

import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;

/**
 * A single billboard particle rendered in the guidebook scene's 3D coordinate space.
 * Simulates the visual appearance of block-breaking or block-placing particles, using a
 * small sub-region of the block's texture atlas icon.
 *
 * <p>
 * Particles are owned by {@link LytGuidebookScene} and advanced one tick per
 * {@link LytGuidebookScene#ponderTick()} call. They are frozen while the ponder animation is
 * paused, and cleared whenever the scene is restarted or an out-of-order seek occurs.
 */
public class GuidebookSceneParticle {

    public static final int NO_BRIGHTNESS_OVERRIDE = -1;

    public float prevX;
    public float prevY;
    public float prevZ;

    /** Current world-space X position. */
    public float x;
    /** Current world-space Y position. */
    public float y;
    /** Current world-space Z position. */
    public float z;

    /** Velocity along the X axis (blocks per tick). */
    public float vx;
    /** Velocity along the Y axis (blocks per tick). */
    public float vy;
    /** Velocity along the Z axis (blocks per tick). */
    public float vz;

    /** Backing texture for this particle. */
    public ResourceLocation texture = TextureMap.locationBlocksTexture;

    /** Texture atlas minimum U coordinate. */
    public float u0;
    /** Texture atlas minimum V coordinate. */
    public float v0;
    /** Texture atlas maximum U coordinate. */
    public float u1;
    /** Texture atlas maximum V coordinate. */
    public float v1;

    /** Red color channel multiplier. */
    public float red;
    /** Green color channel multiplier. */
    public float green;
    /** Blue color channel multiplier. */
    public float blue;

    /** Current age in ticks. */
    public int age;
    /** Delay before the particle becomes active. */
    public int delayTicks;
    /** Age at which the particle is considered dead and removed. */
    public int maxAge;
    /** Half-side length of the billboard quad, in block units. */
    public float sizeStart;
    /** Half-side length at the end of the particle lifetime. */
    public float sizeEnd;
    /** Starting alpha multiplier. */
    public float alphaStart = 1f;
    /** Ending alpha multiplier. */
    public float alphaEnd = 1f;
    /** Gravity applied each tick before drag. */
    public float gravityPerTick = 0.04f;
    /** Velocity multiplier applied each tick after gravity. */
    public float dragPerTick = 0.98f;
    /** Optional brightness override, or {@link #NO_BRIGHTNESS_OVERRIDE}. */
    public int brightness = NO_BRIGHTNESS_OVERRIDE;
    /** Equal-width animated texture grid columns. */
    public int animationColumns;
    /** Equal-height animated texture grid rows. */
    public int animationRows;
    /** Animation start frame index. */
    public int animationStartFrame;
    /** Animation frame count. */
    public int animationFrameCount;
    /** Animation frame step. */
    public int animationFrameStep = 1;
    /** Whether the animation should loop when the particle is immortal. */
    public boolean animationLoop;

    public GuidebookSceneParticle() {
        reset(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 1f, 1f, 1, 0.01f);
    }

    public GuidebookSceneParticle(float x, float y, float z, float vx, float vy, float vz, float u0, float v0, float u1,
        float v1, float red, float green, float blue, int maxAge, float size) {
        reset(x, y, z, vx, vy, vz, u0, v0, u1, v1, red, green, blue, maxAge, size);
    }

    public GuidebookSceneParticle reset(float x, float y, float z, float vx, float vy, float vz, float u0, float v0,
        float u1, float v1, float red, float green, float blue, int maxAge, float size) {
        this.prevX = x;
        this.prevY = y;
        this.prevZ = z;
        this.x = x;
        this.y = y;
        this.z = z;
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
        this.texture = TextureMap.locationBlocksTexture;
        this.u0 = u0;
        this.v0 = v0;
        this.u1 = u1;
        this.v1 = v1;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.age = 0;
        this.delayTicks = 0;
        this.maxAge = maxAge;
        this.sizeStart = size;
        this.sizeEnd = size;
        this.alphaStart = 1f;
        this.alphaEnd = 1f;
        this.gravityPerTick = 0.04f;
        this.dragPerTick = 0.98f;
        this.brightness = NO_BRIGHTNESS_OVERRIDE;
        this.animationColumns = 0;
        this.animationRows = 0;
        this.animationStartFrame = 0;
        this.animationFrameCount = 0;
        this.animationFrameStep = 1;
        this.animationLoop = false;
        return this;
    }

    /** Advances the particle by one tick: applies velocity, gravity, and friction. */
    public void tick() {
        if (delayTicks > 0) {
            delayTicks--;
            return;
        }
        prevX = x;
        prevY = y;
        prevZ = z;
        x += vx;
        y += vy;
        z += vz;
        vy -= gravityPerTick;
        vx *= dragPerTick;
        vy *= dragPerTick;
        vz *= dragPerTick;
        age++;
        updateAnimatedTextureFrame(age);
    }

    public void advanceBy(int ticks) {
        int remainingTicks = Math.max(0, ticks);
        if (remainingTicks == 0 || isDead()) {
            return;
        }
        if (delayTicks > 0) {
            int skippedDelay = Math.min(delayTicks, remainingTicks);
            delayTicks -= skippedDelay;
            remainingTicks -= skippedDelay;
            if (remainingTicks == 0 || isDead()) {
                return;
            }
        }
        for (int i = 0; i < remainingTicks && !isDead(); i++) {
            tick();
        }
    }

    /** Returns {@code true} when the particle has exceeded its maximum age. */
    public boolean isDead() {
        return delayTicks <= 0 && maxAge >= 0 && age >= maxAge;
    }

    public boolean isReadyToRender() {
        return delayTicks <= 0;
    }

    public float getRenderX(float partialTicks) {
        return interpolate(prevX, x, partialTicks);
    }

    public float getRenderY(float partialTicks) {
        return interpolate(prevY, y, partialTicks);
    }

    public float getRenderZ(float partialTicks) {
        return interpolate(prevZ, z, partialTicks);
    }

    public float getAlpha(float partialTicks) {
        return interpolate(alphaStart, alphaEnd, getLifeProgress(partialTicks));
    }

    public float getSize(float partialTicks) {
        return interpolate(sizeStart, sizeEnd, getLifeProgress(partialTicks));
    }

    public int getBrightness(float partialTicks) {
        return brightness;
    }

    public GuidebookSceneParticle withTexture(ResourceLocation texture) {
        this.texture = texture != null ? texture : TextureMap.locationBlocksTexture;
        return this;
    }

    public GuidebookSceneParticle withSizeRange(float start, float end) {
        this.sizeStart = start;
        this.sizeEnd = end;
        return this;
    }

    public GuidebookSceneParticle withAlphaRange(float start, float end) {
        this.alphaStart = start;
        this.alphaEnd = end;
        return this;
    }

    public GuidebookSceneParticle withPhysics(float gravityPerTick, float dragPerTick) {
        this.gravityPerTick = gravityPerTick;
        this.dragPerTick = dragPerTick;
        return this;
    }

    public GuidebookSceneParticle withBrightness(int brightness) {
        this.brightness = brightness;
        return this;
    }

    public GuidebookSceneParticle withAnimatedTexture(int columns, int rows, int startFrame, int frameCount,
        int frameStep, boolean loop) {
        this.animationColumns = Math.max(0, columns);
        this.animationRows = Math.max(0, rows);
        this.animationStartFrame = Math.max(0, startFrame);
        this.animationFrameCount = Math.max(0, frameCount);
        this.animationFrameStep = frameStep == 0 ? 1 : frameStep;
        this.animationLoop = loop;
        updateAnimatedTextureFrame(age);
        return this;
    }

    public GuidebookSceneParticle withDelay(int delayTicks) {
        this.delayTicks = Math.max(0, delayTicks);
        return this;
    }

    public float getLifeProgress(float partialTicks) {
        if (delayTicks > 0) {
            return 0f;
        }
        if (maxAge <= 0) {
            return 0f;
        }
        float ageProgress = age + clampPartialTicks(partialTicks);
        float progress = ageProgress / (float) maxAge;
        if (progress <= 0f) {
            return 0f;
        }
        if (progress >= 1f) {
            return 1f;
        }
        return progress;
    }

    private void updateAnimatedTextureFrame(int currentAge) {
        if (animationColumns <= 0 || animationRows <= 0 || animationFrameCount <= 0) {
            return;
        }
        int frame = resolveAnimationFrame(currentAge);
        float invColumns = 1f / animationColumns;
        float invRows = 1f / animationRows;
        int column = Math.max(0, frame % animationColumns);
        int row = Math.max(0, frame / animationColumns);
        u0 = column * invColumns;
        v0 = row * invRows;
        u1 = u0 + invColumns;
        v1 = v0 + invRows;
    }

    private int resolveAnimationFrame(int currentAge) {
        int frameOffset;
        if (maxAge > 0) {
            int clampedAge = Math.max(0, Math.min(maxAge - 1, currentAge));
            frameOffset = Math.max(0, Math.min(animationFrameCount - 1, clampedAge * animationFrameCount / maxAge));
        } else if (animationLoop) {
            frameOffset = Math.floorMod(currentAge, animationFrameCount);
        } else {
            frameOffset = 0;
        }
        return Math.max(0, animationStartFrame + animationFrameStep * frameOffset);
    }

    private static float interpolate(float previous, float current, float partialTicks) {
        float t = clampPartialTicks(partialTicks);
        return previous + (current - previous) * t;
    }

    private static float clampPartialTicks(float partialTicks) {
        if (partialTicks <= 0f) {
            return 0f;
        }
        if (partialTicks >= 1f) {
            return 1f;
        }
        return partialTicks;
    }
}
