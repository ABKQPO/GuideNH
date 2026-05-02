package com.hfstudio.guidenh.mixins;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.ITargetMod;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;
import com.hfstudio.guidenh.compat.Mods;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Mixins implements IMixins {

    EARLY(Side.CLIENT, "forge.AccessorForgeHooksClient", "fml.AccessorFMLClientHandler",
        "minecraft.AccessorAbstractResourcePack", "forge.AccessorShapedOreRecipe", "forge.AccessorShapelessOreRecipe",
        "minecraft.MixinModelRendererSceneExportCapture", "minecraft.MixinTessellatorSceneExportCapture"),

    // Captures the BetterQuesting quest currently under the mouse cursor in the BQ quest line
    // GUI so the open-guide hotkey can navigate to the corresponding wiki page. Only applied
    // when BetterQuesting is loaded.
    BQ_PANEL_HOVER(Side.CLIENT, Phase.LATE, Mods.BetterQuesting, "compat.MixinPanelButtonQuest"),

    // Exposes the private node/data fields of AE2's AENetworkProxy so the AE2 guide preview
    // helper can synthesize grid nodes without reflective field access. Only applied when
    // AE2 is loaded.
    AE2_NETWORK_PROXY(Side.CLIENT, Phase.LATE, Mods.AE2, "compat.ae2.AccessorAENetworkProxy"),

    GREGTECH_HATCH_BUILDER(Side.CLIENT, Phase.LATE, Mods.GregTech, "compat.gregtech.AccessorHatchElementBuilder"),

    ;

    @Getter
    private final MixinBuilder builder;

    Mixins(Side side, String... mixins) {
        this.builder = new MixinBuilder().addSidedMixins(side, mixins)
            .setPhase(Phase.EARLY);
    }

    Mixins(Side side, Phase phase, ITargetMod requiredMod, String... mixins) {
        this.builder = new MixinBuilder().addSidedMixins(side, mixins)
            .setPhase(phase)
            .addRequiredMod(requiredMod);
    }
}
