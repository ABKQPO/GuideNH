package com.hfstudio.guidenh.guide.scene;

import java.util.List;
import java.util.Locale;
import java.util.Random;

import net.minecraft.util.ResourceLocation;

/**
 * Factory helpers for guidebook scene particles and reusable particle presets.
 */
public class GuidebookSceneParticleFactory {

    public interface ParticleAllocator {

        GuidebookSceneParticle acquire();
    }

    public static final String DEFAULT_PARTICLE_NAME = "billboard";
    public static final int MAX_EXPLOSION_PARTICLE_COUNT = 256;
    private static final int HUGE_EXPLOSION_FLASH_TICKS = 8;
    public static final ResourceLocation PARTICLE_SHEET_TEXTURE = new ResourceLocation(
        "textures/particle/particles.png");
    public static final ResourceLocation EXPLOSION_TEXTURE = new ResourceLocation("textures/entity/explosion.png");

    public static GuidebookSceneParticle createStaticSceneParticle(String particleName, float x, float y, float z,
        float size) {
        return createStaticSceneParticle(new GuidebookSceneParticle(), particleName, x, y, z, size);
    }

    public static GuidebookSceneParticle createStaticSceneParticle(GuidebookSceneParticle particle, String particleName,
        float x, float y, float z, float size) {
        String normalizedName = normalizeParticleName(particleName);
        return switch (normalizedName) {
            case "smoke" -> createStaticSmokeParticle(particle, x, y, z, size);
            case "largesmoke" -> createStaticSmokeParticle(particle, x, y, z, size * 1.6f);
            case "explode" -> createStaticExplodeParticle(particle, x, y, z, size);
            case "flash", "largeexplode" -> createStaticFlashParticle(particle, x, y, z, size);
            case "hugeexplosion" -> createStaticFlashParticle(particle, x, y, z, size * 1.5f);
            default -> createStaticBillboardParticle(particle, x, y, z, size);
        };
    }

    public static GuidebookSceneParticle createRuntimeParticle(String particleName, float x, float y, float z, float vx,
        float vy, float vz, int lifetimeTicks, float size, Random rng) {
        return createRuntimeParticle(
            new GuidebookSceneParticle(),
            particleName,
            x,
            y,
            z,
            vx,
            vy,
            vz,
            lifetimeTicks,
            size,
            rng);
    }

    public static GuidebookSceneParticle createRuntimeParticle(GuidebookSceneParticle particle, String particleName,
        float x, float y, float z, float vx, float vy, float vz, int lifetimeTicks, float size, Random rng) {
        String normalizedName = normalizeParticleName(particleName);
        return switch (normalizedName) {
            case "smoke" -> createSmokeParticle(particle, x, y, z, vx, vy, vz, lifetimeTicks, size, rng);
            case "largesmoke" -> createSmokeParticle(particle, x, y, z, vx, vy, vz, lifetimeTicks, size * 1.6f, rng);
            case "explode" -> createExplodeParticle(particle, x, y, z, vx, vy, vz, lifetimeTicks, size, rng);
            case "flash", "largeexplode" -> createFlashParticle(particle, x, y, z, lifetimeTicks, size, rng);
            case "hugeexplosion" -> createFlashParticle(particle, x, y, z, lifetimeTicks, size * 1.5f, rng);
            default -> createBillboardParticle(particle, x, y, z, vx, vy, vz, lifetimeTicks, size);
        };
    }

    public static boolean isSupportedParticleName(String particleName) {
        return switch (normalizeParticleName(particleName)) {
            case "billboard", "particle", "quad", "sheet", "smoke", "largesmoke", "explode", "flash", "largeexplode", "hugeexplosion" -> true;
            default -> false;
        };
    }

    private static GuidebookSceneParticle createStaticBillboardParticle(GuidebookSceneParticle particle, float x,
        float y, float z, float size) {
        return particle.reset(x, y, z, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0.95f, 0.95f, 0.95f, -1, size)
            .withTexture(PARTICLE_SHEET_TEXTURE)
            .withPhysics(0f, 1f)
            .withAlphaRange(0.9f, 0.9f)
            .withAnimatedTexture(16, 16, 7, 1, 1, false)
            .withBrightness(240);
    }

    private static GuidebookSceneParticle createStaticSmokeParticle(GuidebookSceneParticle particle, float x, float y,
        float z, float size) {
        float gray = 0.2f;
        return particle.reset(x, y, z, 0f, 0f, 0f, 0f, 0f, 0f, 0f, gray, gray, gray, -1, Math.max(0.01f, size))
            .withTexture(PARTICLE_SHEET_TEXTURE)
            .withSizeRange(size, size)
            .withAlphaRange(0.9f, 0.9f)
            .withPhysics(0f, 1f)
            .withAnimatedTexture(16, 16, 0, 1, 1, false)
            .withBrightness(240);
    }

    private static GuidebookSceneParticle createStaticExplodeParticle(GuidebookSceneParticle particle, float x, float y,
        float z, float size) {
        return particle.reset(x, y, z, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0.85f, 0.85f, 0.85f, -1, Math.max(0.01f, size))
            .withTexture(PARTICLE_SHEET_TEXTURE)
            .withSizeRange(size, size)
            .withAlphaRange(0.95f, 0.95f)
            .withPhysics(0f, 1f)
            .withAnimatedTexture(16, 16, 7, 1, 1, false)
            .withBrightness(240);
    }

    private static GuidebookSceneParticle createStaticFlashParticle(GuidebookSceneParticle particle, float x, float y,
        float z, float size) {
        return particle.reset(x, y, z, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0.9f, 0.9f, 0.9f, -1, Math.max(0.01f, size))
            .withTexture(EXPLOSION_TEXTURE)
            .withSizeRange(size, size)
            .withAlphaRange(0.92f, 0.92f)
            .withPhysics(0f, 1f)
            .withAnimatedTexture(4, 4, 0, 1, 1, false)
            .withBrightness(240);
    }

    private static GuidebookSceneParticle createBillboardParticle(GuidebookSceneParticle particle, float x, float y,
        float z, float vx, float vy, float vz, int lifetimeTicks, float size) {
        return particle
            .reset(
                x,
                y,
                z,
                vx,
                vy,
                vz,
                0f,
                0f,
                0f,
                0f,
                0.95f,
                0.95f,
                0.95f,
                Math.max(1, lifetimeTicks),
                Math.max(0.01f, size))
            .withTexture(PARTICLE_SHEET_TEXTURE)
            .withSizeRange(size, size)
            .withAlphaRange(0.95f, 0.1f)
            .withPhysics(0f, 0.98f)
            .withAnimatedTexture(16, 16, 7, 1, 1, false)
            .withBrightness(240);
    }

    public static void appendExplosionPreset(List<GuidebookSceneParticle> out, Random rng, float x, float y, float z,
        int durationTicks, float power, int particleCount) {
        appendExplosionPreset(out, rng, x, y, z, durationTicks, power, particleCount, GuidebookSceneParticle::new);
    }

    public static void appendExplosionPreset(List<GuidebookSceneParticle> out, Random rng, float x, float y, float z,
        int durationTicks, float power, int particleCount, ParticleAllocator allocator) {
        if (out == null || rng == null) {
            return;
        }
        ParticleAllocator resolvedAllocator = allocator != null ? allocator : GuidebookSceneParticle::new;
        int resolvedDuration = Math.max(1, durationTicks);
        float resolvedPower = Math.max(0.1f, power);
        int resolvedCount = Math.max(1, particleCount);
        appendVanillaExplosionFlash(out, resolvedAllocator, rng, x, y, z, resolvedPower, resolvedDuration);

        int effectCount = Math.min(MAX_EXPLOSION_PARTICLE_COUNT, Math.max(12, resolvedCount));
        for (int i = 0; i < effectCount; i++) {
            double directionX;
            double directionY;
            double directionZ;
            double directionLengthSquared;
            do {
                directionX = rng.nextDouble() * 2.0D - 1.0D;
                directionY = rng.nextDouble() * 2.0D - 1.0D;
                directionZ = rng.nextDouble() * 2.0D - 1.0D;
                directionLengthSquared = directionX * directionX + directionY * directionY + directionZ * directionZ;
            } while (directionLengthSquared < 1.0E-4D || directionLengthSquared > 1.0D);

            double inverseDirectionLength = 1.0D / Math.sqrt(directionLengthSquared);
            directionX *= inverseDirectionLength;
            directionY *= inverseDirectionLength;
            directionZ *= inverseDirectionLength;

            double sampleDistance = resolvedPower * (0.55D + rng.nextDouble() * 0.45D);
            float sampleX = (float) (x + directionX * sampleDistance);
            float sampleY = (float) (y + directionY * sampleDistance * 0.85D);
            float sampleZ = (float) (z + directionZ * sampleDistance);
            double velocityScale = 0.5D / (sampleDistance / resolvedPower + 0.1D);
            velocityScale *= rng.nextFloat() * rng.nextFloat() + 0.3F;
            float velocityX = (float) (directionX * velocityScale);
            float velocityY = (float) (directionY * velocityScale);
            float velocityZ = (float) (directionZ * velocityScale);

            out.add(
                createVanillaExplodeParticle(
                    resolvedAllocator.acquire(),
                    (sampleX + x) * 0.5f,
                    (sampleY + y) * 0.5f,
                    (sampleZ + z) * 0.5f,
                    velocityX,
                    velocityY,
                    velocityZ,
                    rng));
            out.add(
                createVanillaSmokeParticle(
                    resolvedAllocator.acquire(),
                    sampleX,
                    sampleY,
                    sampleZ,
                    velocityX,
                    velocityY,
                    velocityZ,
                    resolvedDuration,
                    resolvedPower,
                    rng));
        }
    }

    public static int defaultExplosionParticleCount(float power) {
        return Math.min(MAX_EXPLOSION_PARTICLE_COUNT, Math.max(16, Math.round(Math.max(0.1f, power) * 32f)));
    }

    private static GuidebookSceneParticle createSmokeParticle(GuidebookSceneParticle particle, float x, float y,
        float z, float vx, float vy, float vz, int lifetimeTicks, float size, Random rng) {
        float gray = rng.nextFloat() * 0.3f;
        return particle
            .reset(
                x,
                y,
                z,
                vx,
                vy,
                vz,
                0f,
                0f,
                0f,
                0f,
                gray,
                gray,
                gray,
                Math.max(1, lifetimeTicks),
                Math.max(0.01f, size))
            .withTexture(PARTICLE_SHEET_TEXTURE)
            .withSizeRange(size * 0.2f, size)
            .withAlphaRange(0.95f, 0f)
            .withPhysics(-0.004f, 0.96f)
            .withAnimatedTexture(16, 16, 7, 8, -1, false)
            .withBrightness(240);
    }

    private static GuidebookSceneParticle createExplodeParticle(GuidebookSceneParticle particle, float x, float y,
        float z, float vx, float vy, float vz, int lifetimeTicks, float size, Random rng) {
        float tint = rng.nextFloat() * 0.3f + 0.7f;
        float jitterX = (rng.nextFloat() * 2f - 1f) * 0.05f;
        float jitterY = (rng.nextFloat() * 2f - 1f) * 0.05f;
        float jitterZ = (rng.nextFloat() * 2f - 1f) * 0.05f;
        return particle
            .reset(
                x,
                y,
                z,
                vx + jitterX,
                vy + jitterY,
                vz + jitterZ,
                0f,
                0f,
                0f,
                0f,
                tint,
                tint,
                tint,
                Math.max(1, lifetimeTicks),
                Math.max(0.01f, size))
            .withTexture(PARTICLE_SHEET_TEXTURE)
            .withSizeRange(size * 0.35f, size)
            .withAlphaRange(1f, 0.15f)
            .withPhysics(-0.004f, 0.9f)
            .withAnimatedTexture(16, 16, 7, 8, -1, false)
            .withBrightness(240);
    }

    private static GuidebookSceneParticle createFlashParticle(GuidebookSceneParticle particle, float x, float y,
        float z, int lifetimeTicks, float size, Random rng) {
        float tint = rng.nextFloat() * 0.6f + 0.4f;
        return particle
            .reset(
                x,
                y,
                z,
                0f,
                0f,
                0f,
                0f,
                0f,
                0f,
                0f,
                tint,
                tint,
                tint,
                Math.max(1, lifetimeTicks),
                Math.max(0.01f, size))
            .withTexture(EXPLOSION_TEXTURE)
            .withSizeRange(size, size * 1.08f)
            .withAlphaRange(1f, 0.2f)
            .withPhysics(0f, 1f)
            .withAnimatedTexture(4, 4, 0, 16, 1, false)
            .withBrightness(240);
    }

    private static void appendVanillaExplosionFlash(List<GuidebookSceneParticle> out, ParticleAllocator allocator,
        Random rng, float x, float y, float z, float power, int durationTicks) {
        if (power >= 2.0f) {
            int flashTicks = HUGE_EXPLOSION_FLASH_TICKS;
            for (int tick = 0; tick < flashTicks; tick++) {
                float progress = tick / (float) HUGE_EXPLOSION_FLASH_TICKS;
                for (int burst = 0; burst < 6; burst++) {
                    float sampleX = x + (rng.nextFloat() - rng.nextFloat()) * 4.0f;
                    float sampleY = y + (rng.nextFloat() - rng.nextFloat()) * 4.0f;
                    float sampleZ = z + (rng.nextFloat() - rng.nextFloat()) * 4.0f;
                    out.add(
                        createVanillaLargeExplosionParticle(
                            allocator.acquire(),
                            sampleX,
                            sampleY,
                            sampleZ,
                            progress,
                            rng).withDelay(tick));
                }
            }
            return;
        }

        out.add(createVanillaLargeExplosionParticle(allocator.acquire(), x, y, z, 1.0f, rng));
    }

    private static GuidebookSceneParticle createVanillaExplodeParticle(GuidebookSceneParticle particle, float x,
        float y, float z, float vx, float vy, float vz, Random rng) {
        float motionX = vx + (rng.nextFloat() * 2.0f - 1.0f) * 0.05f;
        float motionY = vy + (rng.nextFloat() * 2.0f - 1.0f) * 0.05f;
        float motionZ = vz + (rng.nextFloat() * 2.0f - 1.0f) * 0.05f;
        float tint = rng.nextFloat() * 0.3f + 0.7f;
        float renderSize = (rng.nextFloat() * rng.nextFloat() * 6.0f + 1.0f) * 0.06f;
        int lifetimeTicks = (int) (16.0d / (rng.nextFloat() * 0.8d + 0.2d)) + 2;
        return particle
            .reset(
                x,
                y,
                z,
                motionX,
                motionY,
                motionZ,
                0f,
                0f,
                0f,
                0f,
                tint,
                tint,
                tint,
                Math.max(1, lifetimeTicks),
                Math.max(0.01f, renderSize))
            .withTexture(PARTICLE_SHEET_TEXTURE)
            .withSizeRange(renderSize, renderSize)
            .withAlphaRange(1f, 1f)
            .withPhysics(-0.004f, 0.9f)
            .withAnimatedTexture(16, 16, 7, 8, -1, false)
            .withBrightness(240);
    }

    private static GuidebookSceneParticle createVanillaSmokeParticle(GuidebookSceneParticle particle, float x, float y,
        float z, float vx, float vy, float vz, int durationTicks, float power, Random rng) {
        float gray = rng.nextFloat() * 0.15f + 0.15f;
        float renderSize = (0.08f + power * 0.035f) * (0.85f + rng.nextFloat() * 0.35f);
        int lifetimeTicks = Math.max(10, durationTicks + 8 + rng.nextInt(5));
        return particle
            .reset(x, y, z, vx, vy, vz, 0f, 0f, 0f, 0f, gray, gray, gray, lifetimeTicks, Math.max(0.01f, renderSize))
            .withTexture(PARTICLE_SHEET_TEXTURE)
            .withSizeRange(renderSize * 0.35f, renderSize)
            .withAlphaRange(0.95f, 0f)
            .withPhysics(-0.004f, 0.96f)
            .withAnimatedTexture(16, 16, 7, 8, -1, false)
            .withBrightness(240);
    }

    private static GuidebookSceneParticle createVanillaLargeExplosionParticle(GuidebookSceneParticle particle, float x,
        float y, float z, float progress, Random rng) {
        float tint = rng.nextFloat() * 0.6f + 0.4f;
        float renderSize = 2.0f * (1.0f - progress * 0.5f);
        int lifetimeTicks = 6 + rng.nextInt(4);
        return particle.reset(x, y, z, 0f, 0f, 0f, 0f, 0f, 0f, 0f, tint, tint, tint, lifetimeTicks, renderSize)
            .withTexture(EXPLOSION_TEXTURE)
            .withSizeRange(renderSize, renderSize)
            .withAlphaRange(1f, 1f)
            .withPhysics(0f, 1f)
            .withAnimatedTexture(4, 4, 0, 16, 1, false)
            .withBrightness(240);
    }

    private static String normalizeParticleName(String particleName) {
        if (particleName == null) {
            return DEFAULT_PARTICLE_NAME;
        }
        String normalized = particleName.trim()
            .toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return DEFAULT_PARTICLE_NAME;
        }
        return switch (normalized) {
            case "particle", "quad", "sheet" -> DEFAULT_PARTICLE_NAME;
            default -> normalized;
        };
    }

    private static String normalize(String value) {
        return value == null ? null
            : value.trim()
                .toLowerCase(Locale.ROOT);
    }
}
