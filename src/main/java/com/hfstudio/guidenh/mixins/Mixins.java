package com.hfstudio.guidenh.mixins;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.ITargetMod;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;
import com.hfstudio.guidenh.integration.Mods;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Mixins implements IMixins {

    EARLY(Side.CLIENT, "forge.AccessorForgeHooksClient", "forge.AccessorGuiIngameForge", "fml.AccessorFMLClientHandler",
        "minecraft.AccessorAbstractResourcePack", "forge.AccessorShapedOreRecipe", "forge.AccessorShapelessOreRecipe",
        "minecraft.MixinModelRendererSceneExportCapture", "minecraft.MixinTessellatorSceneExportCapture"),

    BQ_PANEL_HOVER(Side.CLIENT, Phase.LATE, Mods.BetterQuesting, "compat.MixinPanelButtonQuest"),

    GREGTECH_HATCH_BUILDER(Side.CLIENT, Phase.LATE, Mods.GregTech, "compat.gregtech.AccessorHatchElementBuilder"),

    FMP_BLOCK_MICRO_MATERIAL(Side.CLIENT, Phase.LATE, Mods.ForgeMultipart,
        "compat.forgemultipart.AccessorBlockMicroMaterial"),

    BC_TILE_GENERIC_PIPE(Side.CLIENT, Phase.LATE, Mods.BuildCraftTransport,
        "compat.buildcraft.AccessorTileGenericPipe"),

    LP_TILE_GENERIC_PIPE(Side.CLIENT, Phase.LATE, Mods.LogisticsPipes,
        "compat.logisticspipes.AccessorLogisticsTileGenericPipe"),

    WR_CBE_UNLOADED_JAM_STATE(Side.CLIENT, Phase.LATE, Mods.WirelessRedstoneCore,
        "compat.wirelessredstone.MixinRedstoneEther"),

    ;

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

    @Override
    public MixinBuilder getBuilder() {
        return builder;
    }
}
