package com.hfstudio.guidenh.guide.render.emoji;

import net.minecraft.util.ResourceLocation;

public record GuideEmojiGlyph(ResourceLocation texture, int width, int height, int drawWidth, int drawHeight,
    int baselineOffset, boolean supported) {}
