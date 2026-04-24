package com.hfstudio.guidenh.guide.internal;

import java.util.stream.Stream;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;

class GuideMEServerProxy implements GuideMEProxy {

    @Override
    public @Nullable String getGuideDisplayName(ResourceLocation guideId) {
        var guide = GuideRegistry.getById(guideId);
        if (guide == null) return null;
        var settings = guide.getItemSettings();
        return settings.displayName()
            .orElse(null);
    }

    @Override
    public boolean openGuide(EntityPlayer player, ResourceLocation guideId, @Nullable PageAnchor anchor) {
        return false;
    }

    @Override
    public Stream<ResourceLocation> getAvailableGuides() {
        return GuideRegistry.getAll()
            .stream()
            .map(MutableGuide::getId);
    }

    @Override
    public Stream<ResourceLocation> getAvailablePages(ResourceLocation guideId) {
        var guide = GuideRegistry.getById(guideId);
        if (guide == null) return Stream.empty();
        return guide.getPages()
            .stream()
            .map(ParsedGuidePage::getId);
    }
}
