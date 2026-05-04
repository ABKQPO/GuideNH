package com.hfstudio.guidenh.compat;

import java.util.Locale;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.util.data.IMod;
import com.gtnewhorizon.gtnhmixins.builders.ITargetMod;
import com.gtnewhorizon.gtnhmixins.builders.TargetModBuilder;

import cpw.mods.fml.common.Loader;

public enum Mods implements IMod, ITargetMod {

    // spotless:off
    NotEnoughItems("NotEnoughItems"),
    NeiCustomDiagram("neicustomdiagram"),
    BetterQuesting("betterquesting"),
    StructureLib("structurelib"),
    AE2("appliedenergistics2"),
    GregTech("gregtech"),
    CarpentersBlocks("CarpentersBlocks"),
    ForgeMultipart("ForgeMultipart"),
    BuildCraftTransport("BuildCraft|Transport"),
    DistantHorizons("distanthorizons"),
    EtFuturum("etfuturum"),
    SimpleSkinBackport("simpleskinbackport"),
    ;
    // spotless:on

    public final String modid;
    public final String resourceDomain;
    private final Supplier<Boolean> supplier;
    private final TargetModBuilder targetBuilder;
    private Boolean loaded;

    Mods(String modid) {
        this(modid, null, null);
    }

    Mods(Supplier<Boolean> supplier) {
        this(null, supplier, null);
    }

    Mods(String modid, Supplier<Boolean> supplier, String coreModClass) {
        this.modid = modid;
        this.resourceDomain = modid != null ? modid.toLowerCase(Locale.ENGLISH) : null;
        this.supplier = supplier;
        this.targetBuilder = new TargetModBuilder().setModId(modid)
            .setCoreModClass(coreModClass);
    }

    @NotNull
    @Override
    public TargetModBuilder getBuilder() {
        return targetBuilder;
    }

    @Override
    public boolean isModLoaded() {
        if (loaded == null) {
            if (supplier != null) {
                loaded = supplier.get();
            } else if (modid != null) {
                loaded = Loader.isModLoaded(modid);
            } else loaded = false;
        }
        return loaded;
    }

    @Override
    public String getID() {
        return modid;
    }

    @Override
    public String getResourceLocation() {
        return resourceDomain;
    }
}
