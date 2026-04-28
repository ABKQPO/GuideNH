package com.hfstudio.guidenh.mixins;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Mixins implements IMixins {

    FORGE_HOOKS_CLIENT(Side.CLIENT, "forge.AccessorForgeHooksClient"),

    ;

    private final MixinBuilder builder;

    Mixins(Side side, String... mixins) {
        this.builder = new MixinBuilder().addSidedMixins(side, mixins)
            .setPhase(Phase.EARLY);
    }

    @Override
    @NotNull
    public MixinBuilder getBuilder() {
        return builder;
    }
}
