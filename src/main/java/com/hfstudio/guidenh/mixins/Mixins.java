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

    BQ_PANEL_HOVER(Side.CLIENT, Phase.LATE, Mods.BetterQuesting, "compat.MixinPanelButtonQuest"),

    AE2_NETWORK_PROXY(Side.CLIENT, Phase.LATE, Mods.AE2, "compat.ae2.AccessorAENetworkProxy"),

    GREGTECH_HATCH_BUILDER(Side.CLIENT, Phase.LATE, Mods.GregTech, "compat.gregtech.AccessorHatchElementBuilder"),

    FMP_BLOCK_MICRO_MATERIAL(Side.CLIENT, Phase.LATE, Mods.ForgeMultipart,
        "compat.forgemultipart.AccessorBlockMicroMaterial"),

    BC_TILE_GENERIC_PIPE(Side.CLIENT, Phase.LATE, Mods.BuildCraftTransport,
        "compat.buildcraft.AccessorTileGenericPipe"),

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
